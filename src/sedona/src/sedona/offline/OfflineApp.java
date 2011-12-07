//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   17 May 07  Brian Frank  Creation
//

package sedona.offline;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import sedona.Buf;
import sedona.Component;
import sedona.Schema;
import sedona.util.FileUtil;
import sedona.xml.XElem;
import sedona.xml.XException;
import sedona.xml.XParser;
import sedona.xml.XWriter;

/**
 * OfflineApp models a complete Sedona application for working
 * offline and handles encoding and decoding from the standard
 * XML and binary formats.
 */
public class OfflineApp
  extends OfflineComponent
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public OfflineApp(Schema schema)
  {
    super(schema.type("sys::App"));
    this.app    = this;
    this.id     = 0;
    this.name   = "app";
    this.schema = schema;
    this.lookup = new OfflineComponent[256];
    this.lookup[0] = this;
    this.links  = new ArrayList();
  }

//////////////////////////////////////////////////////////////////////////
// Id Management
//////////////////////////////////////////////////////////////////////////

  /**
   * Lookup a component by its id.
   */
  public OfflineComponent lookup(int id)
  {
    if (id < 0 || id >= lookup.length) return null;
    return lookup[id];
  }

  /**
   * Lookup a component by path.
   */
  public OfflineComponent lookup(String path)
  {
    OfflineComponent p = this;
    StringTokenizer st = new StringTokenizer(path, "/");
    while (st.hasMoreTokens())
    {
      String name = st.nextToken();
      p = (OfflineComponent)p.child(name);
      if (p == null) return null;
    }
    return p;
  }

  /**
   * Get the max component id used by the application.
   */
  public int maxId()
  {
    for (int i=lookup.length-1; i>=0; --i)
      if (lookup[i] != null) return i;
    return 0;
  }

  /**
   * Generate a new unique id.
   */
  private int generateId()
  {
    for (int i=lastId; i<lookup.length; ++i)
      if (lookup[i] == null)
        return lastId = i;
    return lastId = lookup.length;
  }

  /**
   * Recursively assign an id to any component which
   * doesn't have one yet.
   */
  public void assignIds() { assignIds(this); }
  private void assignIds(OfflineComponent c)
  {
    if (c.id < 0)
    {
      c.id = generateId();
      addToLookupTable(c);
    }

    OfflineComponent[] kids = c.children();
    for (int i=0; i<kids.length; ++i)
      assignIds(kids[i]);
  }

  /**
   * Add to the lookup table, resize if necessary.
   */
  private void addToLookupTable(OfflineComponent c)
  {
    if (c.id >= lookup.length)
    {
      OfflineComponent[] temp = new OfflineComponent[Math.max(lookup.length*2, c.id+32)];
      System.arraycopy(lookup, 0, temp, 0, lookup.length);
      lookup = temp;
    }

    lookup[c.id] = c;
  }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /**
   * Check that this OfflineApp and another are exactly the same.
   */
  public boolean equivalent(OfflineApp that)
  {
    if (!super.equivalent(that)) return false;

    // check schema
    if (!this.schema.equivalent(that.schema)) return false;

    // check links
    if (this.links.size() != that.links.size()) return false;
    for (int i=0; i<links.size(); ++i)
    {
      OfflineLink thisLink = (OfflineLink)this.links.get(i);
      OfflineLink thatLink = (OfflineLink)that.links.get(i);
      if (!thisLink.equivalent(thatLink)) return false;
    }

    return true;
  }

//////////////////////////////////////////////////////////////////////////
// Component Management
//////////////////////////////////////////////////////////////////////////

  /**
   * Add a child component.  Return child.
   */
  public OfflineComponent add(OfflineComponent parent, OfflineComponent kid)
  {
    // error checking
    if (lookup(kid.id) != null)
      throw new IllegalStateException("Duplicate id: " + kid.id);
    if (parent == null)
      throw new IllegalStateException("Parent is null");
    if (parent.app != this)
      throw new IllegalStateException("Parent not mounted under app: " + parent);
    if (kid.parent != null)
      throw new IllegalStateException("Kid already mounted: " + kid);
    if (parent.child(kid.name) != null)
      throw new IllegalStateException("Duplicate name: " + kid.name);
    if (parent.kids != null && parent.kids.size() >= Component.maxChildren)
      throw new IllegalArgumentException("Too many children under component: " + parent);
    if (kid.type.isAbstract())
      throw new IllegalArgumentException("Cannot add component with abstract type to app: " + kid);
    if (!kid.type.isPublic())
      throw new IllegalArgumentException("Cannot add component with non-public type to app: " + kid);

    // if we've assigned an id, then add it to my lookup tables
    // immediately, otherwise we will assign the id after the whole
    // app has been decoded (and we know which ids have been used)
    if (kid.id > 0)
      addToLookupTable(kid);

    // lazily create kid structures on parent
    if (parent.kids == null)
    {
      parent.kids = new ArrayList();
      parent.kidsByName = new HashMap();
    }

    // mount it
    kid.app = this;
    kid.parent = parent;
    parent.kids.add(kid);
    parent.kidsByName.put(kid.name, kid);
    return kid;
  }

  /**
   * Remove a child component. All children of this component will be
   * recursively removed first.
   */
  public void remove(OfflineComponent kid)
  {
    // error checking
    if (kid == this)
      throw new IllegalStateException("Can't delete app itself!");
    if (kid.app != this || lookup(kid.id) != kid)
      throw new IllegalStateException("Not in this app: " + kid);

    // remove component tree - from the bottom-up
    OfflineComponent[] kidKids = kid.children();
    for (int i=0; i<kidKids.length; ++i)
      remove(kidKids[i]);

    // remove any links that reference it
    OfflineLink[] links = getLinks();
    for (int i=0; i<links.length; i++)
    {
      OfflineLink z = links[i];
      if (kid.id == z.fromComp.id || kid.id == z.toComp.id)
        removeLink(z);
    }

    // remove it from my lookup tables
    lookup[kid.id] = null;

    // remove it from parent
    OfflineComponent parent = kid.parent;
    kid.app = null;
    kid.parent = null;
    parent.kids.remove(kid);
    parent.kidsByName.remove(kid.name);
  }

  /**
   * Reorder component children.
   */
  public void reorder(OfflineComponent parent, int[] childrenIds)
  {
    // error checking
    if (parent == null)
      throw new IllegalStateException("Parent is null");
    if (parent.app != this || lookup(parent.id) != parent)
      throw new IllegalStateException("Not in this app: " + parent);

    // get safe copy
    int[] ids = (int[])childrenIds.clone();
    OfflineComponent[] kids = parent.children();
    if (kids.length != ids.length)
      throw new IllegalArgumentException("childrenIds.length wrong");

    // reorder kids
    ArrayList newList = new ArrayList(kids.length);
    for (int i=0; i<ids.length; i++)
    {
      // find component
      OfflineComponent kid = null;
      for (int j=0; j<kids.length; j++)
        if (kids[j].id == ids[i])
        {
          kid = kids[j];
          break;
        }

      // make sure id exists
      if (kid == null)
        throw new IllegalArgumentException("childrenId not found: " + ids[i]);

      // add new child
      newList.add(kid);
    }

    // commit new list to comp
    parent.kids = newList;
  }

//////////////////////////////////////////////////////////////////////////
// Link Management
//////////////////////////////////////////////////////////////////////////

  public void addLink(OfflineLink link)
  {
    // never add duplicates
    OfflineLink z = findLink(link);
    if (z == null) links.add(link);
  }

  public void removeLink(OfflineLink link)
  {
    links.remove(findLink(link));
  }

  public OfflineLink[] getLinks()
  {
    return (OfflineLink[])links.toArray(new OfflineLink[links.size()]);
  }

  OfflineLink findLink(OfflineLink link)
  {
    for (int i=0; i<links.size(); i++)
    {
      OfflineLink z = (OfflineLink)links.get(i);
      if (link.equivalent(z)) return z;
    }
    return null;
  }


////////////////////////////////////////////////////////////////
// Memory Management
////////////////////////////////////////////////////////////////

  /**
   * Estimate the number of bytes in RAM required for this app.
   * This number is based upon the following assumptions:
   *   - pointer size of 32-bits
   *   - heap alignment on 4 bytes boundaries
   *   - manifests sizeof is accurate (things were
   *     compiled together)
   */
  public int ramSize()
  {
    int mem = 0;

    // components
    for (int i=0; i<lookup.length; ++i)
    {
      if (lookup[i] != null)
        mem += align(lookup[i].type.manifest.sizeof);
    }

    // links
    mem += links.size() * 16;

    return mem;
  }

  static int align(int size)
  {
    int rem = size % 4;
    if (rem == 0) return size;
    return size + (4-rem);
  }

  /**
   * Estimate the number of bytes required to persist this
   * application to FLASH.
   */
  public int flashSize()
  {
    return encodeAppBinary().size;
  }

//////////////////////////////////////////////////////////////////////////
// XML Encode
//////////////////////////////////////////////////////////////////////////

  public void encodeAppXml(File file)
    throws Exception
  {
    encodeAppXml(file, false);
  }

  public void encodeAppXml(File file, boolean nochk)
    throws Exception
  {
    XWriter out = new XWriter(file);
    try
    {
      encodeAppXml(out,nochk);
    }
    finally
    {
      out.close();
    }
  }

  public void encodeAppXml(OutputStream out)
    throws Exception
  {
    encodeAppXml(out,false);
  }

  public void encodeAppXml(OutputStream out,boolean nochk)
    throws Exception
  {
    encodeAppXml(new XWriter(out),nochk);
  }

  public void encodeAppXml(XWriter out)
  {
    encodeAppXml(out,false);
  }

  public void encodeAppXml(XWriter out,boolean nochk)
  {
    out.w("<?xml version='1.0'?>\n");
    out.w("<sedonaApp>\n");
    schema.encodeXml(out, nochk);
    out.w("<app>\n");
    encodeXmlProps(out, 2, true);
    encodeXmlChildren(out, 2, true);
    out.w("</app>\n");
    out.w("<links>\n");
    for (int i=0; i<links.size(); ++i)
      ((OfflineLink)links.get(i)).encodeXml(out);
    out.w("</links>\n");
    out.w("</sedonaApp>\n");
    out.flush();
  }

  public void dump()
  {
    XWriter out = new XWriter(System.out);
    encodeAppXml(out, false);
    out.flush();
  }

//////////////////////////////////////////////////////////////////////////
// Decode
//////////////////////////////////////////////////////////////////////////

  /**
   * If the file extension ends with '.sax' route to
   * decodeAppXml(), otherwise route to decodeAppBinary().
   */
  public static OfflineApp decodeApp(File file)
    throws Exception
  {
    if (file.getName().endsWith(".sax"))
      return decodeAppXml(file);
    else
      return decodeAppBinary(file);
  }

//////////////////////////////////////////////////////////////////////////
// XML Decode
//////////////////////////////////////////////////////////////////////////

  public static OfflineApp decodeAppXml(File file)
    throws Exception
  {
    return decodeAppXml(XParser.make(file).parse());
  }

  public static OfflineApp decodeAppXml(String filename, InputStream in)
    throws Exception
  {
    return decodeAppXml(XParser.make(filename, in).parse());
  }

  public static OfflineApp decodeAppXml(XElem xml)
    throws Exception
  {
    // check root
    if (!xml.name().equals("sedonaApp"))
      throw new XException("Root element must be <sedonaApp>", xml);

    // decode schema
    Schema schema = Schema.decodeXml(xml.elem("schema", true));

    // decode App component
    OfflineApp app = new OfflineApp(schema);
    XElem appXml = xml.elem("app", true);
    app.decodeXmlProps(appXml);
    app.decodeXmlChildren(appXml);

    // finish assigning any ids
    app.assignIds(app);

    // decode links
    XElem[] xlinks = xml.elem("links", true).elems("link");
    app.links = new ArrayList(Math.max(32, xlinks .length));
    for (int i=0; i<xlinks .length; ++i)
      app.links.add(OfflineLink.decodeXml(app, xlinks[i]));

    return app;
  }

//////////////////////////////////////////////////////////////////////////
// Binary Encode
//////////////////////////////////////////////////////////////////////////

  public void encodeAppBinary(File file)
    throws Exception
  {
    FileOutputStream out = new FileOutputStream(file);
    try
    {
      encodeAppBinary().writeTo(out);
    }
    finally
    {
      out.close();
    }
  }

  public Buf encodeAppBinary()
  {
    // header
    Buf out = new Buf(1024);
    out.i4(0x73617070);       // "sapp"
    out.i4(0x0003);           // version 0.3
    schema.encodeBinary(out); // schema
    out.u2(maxId());          // maxId

    // components
    for (int i=0; i<lookup.length; ++i)
      if (lookup[i] != null)
        lookup[i].encodeBinary(out);
    out.u2(0xffff);

    // links
    for (int i=0; i<links.size(); ++i)
      ((OfflineLink)links.get(i)).encodeBinary(out);
    out.u2(0xffff);

    // end marker
    out.u1('.');
    return out;
  }

//////////////////////////////////////////////////////////////////////////
// Binary Decode
//////////////////////////////////////////////////////////////////////////

  public static OfflineApp decodeAppBinary(File file)
    throws Exception
  {
    FileInputStream in = new FileInputStream(file);
    try
    {
      return decodeAppBinary(in);
    }
    finally
    {
      in.close();
    }
  }

  public static OfflineApp decodeAppBinary(InputStream in)
    throws Exception
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    FileUtil.pipe(in, out);
    return decodeAppBinary(new Buf(out.toByteArray()));
  }

  public static OfflineApp decodeAppBinary(Buf in)
    throws Exception
  {
    // header
    int magic = in.i4();
    int version = in.i4();
    if (magic != 0x73617070) throw new IOException("Invalid magic 0x" + Integer.toHexString(magic));
    if (version != 0x0003) throw new IOException("Invalid version 0x" + Integer.toHexString(version));
    Schema schema = Schema.decodeBinary(in);
    int maxId = in.u2();

    // create app
    OfflineApp app = new OfflineApp(schema);

    // read components into a list of Decoded instances
    HashMap decoded = new HashMap();
    while (true)
    {
      // read next id
      int id = in.u2();
      if (id == 0xffff) break;

      // decode component
      Decoded d = OfflineComponent.decodeBinary(app, in, id);
      decoded.put(new Integer(d.comp.id), d);

      // add to lookup table
      if (d.comp != app)
      {
        if (app.lookup(d.comp.id) != null)
          throw new IOException("Duplicate id: " + d.comp.id);
        app.addToLookupTable(d.comp);
      }
    }

    // now match up the parent/child relationships
    Iterator it = decoded.values().iterator();
    while (it.hasNext())
      app.finishDecode(decoded, (Decoded)it.next());

    // decode links
    while (true)
    {
      int id = in.u2();
      if (id == 0xffff) break;
      app.links.add(OfflineLink.decodeBinary(app, id, in));
    }

    if (in.u1() != '.') throw new IOException("Invalid app end marker");

    return app;
  }

  private void finishDecode(HashMap decoded, Decoded d)
    throws IOException
  {
    OfflineComponent comp = d.comp;

    if (d.parentId != 0xffff)
    {
      comp.parent = lookup(d.parentId);
      if (comp.parent == null) throw new IOException("Missing parent " + d.parentId + " for " + comp);
    }

    if (d.childrenId != 0xffff)
    {
      comp.kids = new ArrayList();
      comp.kidsByName = new HashMap();

      OfflineComponent kid = lookup(d.childrenId);
      if (kid == null) throw new IOException("Missing child " + d.childrenId + " for " + comp);
      while (true)
      {
        // add to parent
        comp.kids.add(kid);
        comp.kidsByName.put(kid.name, kid);

        // lookup sibling
        Decoded dKid = (Decoded)decoded.get(new Integer(kid.id));
        if (dKid.nextSiblingId == 0xffff) break;

        kid = lookup(dKid.nextSiblingId);
        if (kid == null) throw new IOException("Missing nextSibling " + d.nextSiblingId + " for " + dKid.comp);
      }
    }
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final Schema schema;
  private OfflineComponent[] lookup;   // indexed by component id
  private ArrayList links;
  private int lastId;

}

