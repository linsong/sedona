//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 May 07  Brian Frank  Creation
//

package sedona;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import sedona.manifest.KitManifest;
import sedona.manifest.ManifestDb;
import sedona.util.Log;
import sedona.util.TextUtil;
import sedona.xml.XElem;
import sedona.xml.XException;
import sedona.xml.XWriter;


/**
 * Schema represents a list of kits with a specific schema
 * checksum which represent the exact schema used by an application
 * in an app file or to a connected device.
 */
public class Schema
{

//////////////////////////////////////////////////////////////////////////
// Load
//////////////////////////////////////////////////////////////////////////

  public static final Log log = new Log("schema");
  private static HashMap cache = new HashMap();

  /**
   * Create a schema for the specified list of kits
   * identified by a kit name and a kit checksum.
   */
  public static Schema load(KitPart[] parts)
    throws Exception
  {
    // first sort the parts according to standard schema
    // order (sys first, followed by others alphabetically)
    sortKits(parts);

    // create unique hash key for schema
    StringBuffer s = new StringBuffer();
    for (int i=0; i<parts.length; ++i)
      s.append(parts[i]).append(';');
    String key = s.substring(0, s.length()-1);

    // check cache for this schema
    Schema schema = (Schema)cache.get(key);
    if (schema != null) return schema;

    // try to load it
    log.debug("Loading... [" + key + "]");
    schema = new Schema(key, new Kit[parts.length]);
    ArrayList missingParts = new ArrayList();
    for (int i=0; i<parts.length; ++i)
    {
      // load part's manifest
      KitPart part = parts[i];
      KitManifest mf = ManifestDb.load(part);
      if (mf == null)
      {
        missingParts.add(part);
        continue;
      }

      // sanity check no duplicate kit names
      if (schema.kitsByName.get(mf.name) != null)
        throw new Exception("Duplicate kit name: " + mf.name);

      // add to schema data structures
      Kit kit = new Kit(schema, i, mf);
      schema.kits[i] = kit;
      schema.kitsByName.put(kit.name, kit);
    }
    
    if (missingParts.size() > 0)
      throw new MissingKitManifestException((KitPart[])missingParts.toArray(new KitPart[missingParts.size()]));

    // now resolve it
    schema.resolve();

    // stash away in cache and return
    cache.put(key, schema);
    return schema;
  }

  /**
   * Private constructor.
   */
  private Schema(String key, Kit[] kits)
  {
    this.key  = key;
    this.kits = kits;
    this.kitsByName = new HashMap(kits.length*3);
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  /**
   * Sort kits by name such that sys is always at index 0, and the
   * rest are sorted by name alphabetically.  This is the standard
   * order for all kit schemas.  The objects passed in must have a
   * public field called "name".
   */
  public static void sortKits(Object[] kits)
  {
    try
    {
      final java.lang.reflect.Field f = kits[0].getClass().getField("name");

      Arrays.sort(kits, new Comparator()
      {
        public int compare(Object a, Object b)
        {
          try
          {
            String an = (String)f.get(a);
            String bn = (String)f.get(b);
            if (an.equals("sys")) return -1;
            if (bn.equals("sys")) return +1;
            return an.compareTo(bn);
          }
          catch (Exception e)
          {
            throw new RuntimeException(e.toString());
          }
        }
      });

      // sanity that sys is always specified at at zero
      if (!f.get(kits[0]).equals("sys"))
        throw new IllegalStateException("sys kit must be in every schema");
    }
    catch (RuntimeException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new RuntimeException(e.toString());
    }
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /**
   * Lookup a kit by its id within this schema.
   */
  public Kit kit(int id)
  {
    if (0 <= id & id < kits.length)
      return kits[id];
    else
      return null;
  }

  /**
   * Lookup a kit by its globally unique kit
   * name or return null if not found.
   */
  public Kit kit(String name)
  {
    return (Kit)kitsByName.get(name);
  }

  /**
   * Lookup a type by its qualified name or return
   * null if not found.
   */
  public Type type(String qname)
  {
    int colon = qname.indexOf(':');
    if (colon < 0 || qname.charAt(colon+1) != ':')
    {
      // try primitive which is implied sys:: qualifier
      Type t = type("sys::" + qname);
      if (t != null) return t;

      throw new IllegalStateException("Invalid type signature: " + qname);
    }

    String kitName  = qname.substring(0, colon);
    String typeName = qname.substring(colon+2);

    Kit kit = kit(kitName);
    if (kit == null) return null;

    return kit.type(typeName);
  }

  /**
   * Find all the concrete (non-abstract) instances of the given type.
   */
  public Type[] allConcreteTypes(Type t)    
  {      
    ArrayList acc = new ArrayList();
    for (int i=0; i<kits.length; ++i)
    {
      Type[] kt = kits[i].types;
      for (int j=0; j<kt.length; ++j)
        if (!kt[j].isAbstract() && kt[j].is(t)) acc.add(kt[j]);
    }
    Type[] types = (Type[])acc.toArray(new Type[acc.size()]);
    Arrays.sort(types);
    return types;
  }

  /**
   * Check that this Schema and another are exactly the same.
   */
  public boolean equivalent(Schema that)
  {
    return key.equals(that.key);
  }

  /**
   * Return string key.
   */
  public String toString()
  {
    return key;
  }

//////////////////////////////////////////////////////////////////////////
// Resolve
//////////////////////////////////////////////////////////////////////////

  /**
   * Resolve completes the kit->type->slot data structures
   * for this specific schema.  This includes resolving base
   * types and assiging slot inherited slot ids.
   */
  void resolve()
    throws Exception
  {
    // resolve base types
    for (int i=0; i<kits.length; ++i)
    {
      Kit kit = kits[i];
      for (int j=0; j<kit.types.length; ++j)
        kit.types[j].resolveBase();
    }

    // resolve slots for each types
    for (int i=0; i<kits.length; ++i)
    {
      Kit kit = kits[i];
      for (int j=0; j<kit.types.length; ++j)
        kit.types[j].resolveSlots();
    }
  }

//////////////////////////////////////////////////////////////////////////
// XML IO
//////////////////////////////////////////////////////////////////////////

  public void encodeXml(XWriter out)
  {
    encodeXml(out, false);
  }
  
  /**
   * Encode this schema to XML format.
   */
  public void encodeXml(XWriter out, boolean nochk)
  {
    out.write("<schema>\n");
    for (int i=0; i<kits.length; ++i)
    {
      Kit kit = kits[i];
      out.w("  <kit ")
         .attr("name", kit.name).w(" ");
      if (!nochk)
        out.attr("checksum", TextUtil.intToHexString(kit.checksum));
      out.w(" />").nl();
    }
    out.write("</schema>\n");
  }

  /**
   * Decode and load an XML <schema> description according
   * to the application XML format.
   */
  public static Schema decodeXml(XElem xml)
    throws Exception
  {
    // parse kit elements into KitParts
    XElem[] xkits = xml.elems("kit");
    KitPart[] parts = new KitPart[xkits.length];
    for (int i=0; i<parts.length; ++i)
    {
      String name = xkits[i].get("name");
      int checksum = (int)java.lang.Long.parseLong(xkits[i].get("checksum", "0"), 16);
      KitPart part;
      if (checksum == 0)
      {
        part = KitPart.forLocalKit(name);
        if (part == null)
          throw new XException("Local kit '" + name + "' not found, must use explicit checksum", xkits[i]);
      }
      else
      {
        part = new KitPart(name, checksum);
      }
      parts[i] = part;
    }

    return load(parts);
  }

//////////////////////////////////////////////////////////////////////////
// Binary IO
//////////////////////////////////////////////////////////////////////////

  /**
   * Save to binary app format.
   */
  public void encodeBinary(Buf out)
  {
    out.u1(kits.length);
    for (int i=0; i<kits.length; ++i)
    {
      Kit kit = kits[i];
      out.str(kit.name);
      out.i4(kit.checksum);
    }
  }

  /**
   * Decode and load from the binary app format.
   */
  public static Schema decodeBinary(Buf in)
    throws Exception
  {
    KitPart[] parts = new KitPart[in.u1()];
    for (int i=0; i<parts.length; ++i)
      parts[i] = new KitPart(in.str(), in.i4());

    return load(parts);
  }

//////////////////////////////////////////////////////////////////////////
// MissingKitManifestException
//////////////////////////////////////////////////////////////////////////

  public static class MissingKitManifestException 
    extends Exception
  {
    public MissingKitManifestException(KitPart part)
    {
      this(new KitPart[]{part});
    }
    
    public MissingKitManifestException(KitPart[] parts)
    {
      this.parts = parts;
    }
    
    public String getMessage()
    {
      StringBuffer sb = new StringBuffer();
      sb.append("Missing kit manifest(s): ");
      for (int i=0; i<parts.length; ++i)
      {
        if (i > 0) sb.append(", ");
        sb.append(parts[i]);
      }
      return sb.append('.').toString();
    }
    
    public KitPart[] parts;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final String key;     // key based on kit parts
  public final Kit[] kits;     // by id (treat as readonly)
  final HashMap kitsByName;    // by name

}
