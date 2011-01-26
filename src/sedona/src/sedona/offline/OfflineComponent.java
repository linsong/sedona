//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Jun 07  Brian Frank  Creation
//

package sedona.offline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import sedona.Buf;
import sedona.Component;
import sedona.Kit;
import sedona.Link;
import sedona.Slot;
import sedona.Type;
import sedona.Value;
import sedona.xml.XElem;
import sedona.xml.XException;
import sedona.xml.XWriter;

/**
 * OfflineComponent models a Sedona component within an
 * OfflineApp when working with sax and sab application
 * files.
 */
public class OfflineComponent
  extends Component
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public OfflineComponent(Type type)
  {
    this(type, null);
  }

  public OfflineComponent(Type type, String name)
  {
    super(type);
    if (name != null) rename(name);
  }

  public OfflineComponent(Type type, String name, int id)
  {
    super(type);
    this.id = id;
    if (name != null) rename(name);
  }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /**
   * Get the OfflineApp instance this componet is mounted in.
   */
  public OfflineApp app()
  {
    return app;
  }

  /**
   * Get the id which identifies this component in its app.
   */
  public int id()
  {
    return id;
  }

  /**
   * Get the component name which is unique with its parent.
   */
  public String name()
  {
    return name;
  }

  /**
   * Rename this component.
   */
  public void rename(String newName)
  {
    assertName(newName);    
    if (parent != null && parent.kidsByName != null)
    {
      parent.kidsByName.remove(name);
      parent.kidsByName.put(newName, this);
    }              
    this.name = newName;
  }

  public Component getParent() { return parent(); }
  public Component[] getChildren() { return children(); }
  public Component getChild(String name) { return child(name); }

  /**
   * Get my parent Component.
   */
  public OfflineComponent parent()
  {
    return parent;
  }

  /**
   * Get list of this component's children.  If this is
   * a remote component, then the first call to this method
   * will block to perform one more network calls.
   */
  public OfflineComponent[] children()
  {
    if (kids == null) return none;
    return (OfflineComponent[])kids.toArray(new OfflineComponent[kids.size()]);
  }

  /**
   * Lookup a child by its simple name.
   */
  public OfflineComponent child(String name)
  {
    if (kidsByName == null) return null;
    return (OfflineComponent)kidsByName.get(name);
  }

  /**
   * Get the list of links into and out of this component.
   */
  public Link[] links()
  {
    if (app == null) return new Link[0];
    
    ArrayList links = new ArrayList();
    OfflineLink[] all = app.getLinks();
    for (int i=0; i<all.length; i++)
    {
      OfflineLink link = all[i];
      if (this == link.fromComp || this == link.toComp)
        links.add(new Link(link.fromComp, link.fromSlot, link.toComp, link.toSlot));
    }
    return (Link[])links.toArray(new Link[links.size()]);
  }

  /**
   * Check that this component and all its descendants
   * are exactly the same.
   */
  public boolean equivalent(OfflineComponent that)
  {
    if (this.getClass() != that.getClass()) return false;

    // meta-data
    if (!type.qname.equals(that.type.qname) ||
        id != that.id ||
        !name.equals(that.name))
      return false;

    // properties
    Slot[] thisSlots = this.type.slots;
    Slot[] thatSlots = that.type.slots;
    if (thisSlots.length != thatSlots.length) return false;
    for (int i=0; i<thisSlots.length; ++i)
    {
      Slot thisSlot = thisSlots[i];
      Slot thatSlot = thatSlots[i];
      if (thisSlot.isProp())
      {
        if (!thatSlot.isProp()) return false;
        if (!get(thisSlot).equals(that.get(thatSlot)))
          return false;
      }
    }

    // children
    OfflineComponent[] thisKids = this.children();
    OfflineComponent[] thatKids = that.children();
    if (thisKids.length != thatKids.length) return false;
    for (int i=0; i<thisKids.length; ++i)
      if (!thisKids[i].equivalent(thatKids[i]))
        return false;

    return true;
  }

//////////////////////////////////////////////////////////////////////////
// XML Encode
//////////////////////////////////////////////////////////////////////////

  public void encodeXml(XWriter out, int indent)
  {
    // start tag
    out.indent(indent).w("<!-- ").w(path()).w(" -->\n");
    out.indent(indent).w("<comp");
    out.attr(" name", name).attr(" id", String.valueOf(id)).attr(" type", type.qname);

    // child elements
    boolean startClosed = false;
    startClosed = encodeXmlProps(out, indent+2, startClosed);
    startClosed = encodeXmlChildren(out, indent+2, startClosed);

    // close tag
    if (!startClosed)
      out.w("/>\n");
    else
      out.indent(indent).w("</comp>\n");
  }

  boolean encodeXmlProps(XWriter out, int indent, boolean startClosed)
  {
    Slot[] slots = type.slots;
    for (int i=0; i<slots.length; ++i)
    {
      Slot slot = slots[i];
      if (!slot.isProp()) continue;
      Value value = get(slot);
      if (value.equals(slot.def())) continue;

      if (!startClosed) { out.w(">\n"); startClosed = true; }
      out.indent(indent).w("<prop").attr(" name", slot.name)
         .attr(" val", value.encodeString()).w("/>\n");
    }
    return startClosed;
  }

  boolean encodeXmlChildren(XWriter out, int indent, boolean startClosed)
  {
    OfflineComponent[] kids = children();
    if (kids.length == 0) return startClosed;

    if (!startClosed) { out.w(">\n"); startClosed = true; }

    for (int i=0; i<kids.length; ++i)
      kids[i].encodeXml(out, indent);

    return startClosed;
  }

//////////////////////////////////////////////////////////////////////////
// XML Decode
//////////////////////////////////////////////////////////////////////////

  void decodeXmlProps(XElem xml)
  {
    for (int i=0; i<xml.contentSize(); ++i)
      if (xml.elem(i).name().equals("prop"))
        decodeXmlProp(xml.elem(i));
  }

  void decodeXmlProp(XElem xml)
  {
    String name = xml.get("name");
    Slot slot = type.slot(name);
    if (slot == null)
    {
      System.out.println("WARNING: Unknown slot " + type + "." + name + " [" + xml.location() + "]");
      return;
    }
    
    // parse the value
    String valStr = xml.get("val");
    Value val = get(slot);
    try
    {
      val = val.decodeString(valStr);
    }
    catch (Exception e)
    {                                 
      throw new XException("Invalid " + val.getClass().getName() + " format: " + valStr, xml);
    }
    
    // set the value
    try
    {
      set(slot, val);
    }
    catch (Exception e)
    {
      throw new XException(e.getMessage(), xml);
    }
  }

  void decodeXmlChildren(XElem xml)
  {
    for (int i=0; i<xml.contentSize(); ++i)
    {
      XElem kidElem = xml.elem(i);
      if (kidElem.name().equals("comp"))
      {
        try
        {
          decodeXmlChild(kidElem);
        }
        catch (XException e)
        {
          throw e;
        }
        catch (Exception e)
        {
          //e.printStackTrace();
          throw new XException("Cannot decode component: " + e.toString(), kidElem);
        }
      }
    }
  }

  void decodeXmlChild(XElem xml)
  {
    // create instance from type
    String qname = xml.get("type");
    Type type = app.schema.type(qname);
    if (type == null)
      throw new XException("Unknown type in schema: " + qname, xml);
    OfflineComponent kid = new OfflineComponent(type);

    // decode id
    kid.id  = xml.geti("id", -1);
    
    // decode name 
    String name = xml.get("name");
    String err = checkName(name);
    if (err != null) throw new XException("Invalid name \"" + name + "\" (" + err + ")", xml);
    kid.rename(name);

    // add it to the application
    app.add(this, kid);

    // decode props and children
    kid.decodeXmlProps(xml);
    kid.decodeXmlChildren(xml);
  }

//////////////////////////////////////////////////////////////////////////
// Binary Encode
//////////////////////////////////////////////////////////////////////////

  public void encodeBinary(Buf out)
  {
    out.u2(id);
    out.u1(type.kit.id);
    out.u1(type.id);
    encodeBinaryMeta(out);
    encodeBinaryProps(out);
    out.u1(';');
  }

  void encodeBinaryMeta(Buf out)
  {
    // Java model uses arrays, but binary format
    // is based on linked list id pointers
    int parentId = -1;
    int childrenId = -1;
    int nextSiblingId = -1;

    if (parent != null)
    {
      parentId = parent.id;
      int myIndex = parent.kids.indexOf(this);
      if (myIndex+1 < parent.kids.size())
        nextSiblingId = ((OfflineComponent)parent.kids.get(myIndex+1)).id;
    }

    if (kids != null && kids.size() > 0)
      childrenId = ((OfflineComponent)kids.get(0)).id;

    out.str(name);
    out.u2(parentId);
    out.u2(childrenId);
    out.u2(nextSiblingId);
  }

  void encodeBinaryProps(Buf out)
  {
    Slot[] slots = type.slots;
    for (int i=0; i<slots.length; ++i)
    {
      Slot slot = slots[i];
      if (slot.isProp() && slot.isConfig())
        get(slot).encodeBinary(out);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Binary Decode
//////////////////////////////////////////////////////////////////////////

  static Decoded decodeBinary(OfflineApp app, Buf in, int id)
    throws Exception
  {
    // identity
    int kitId = in.u1();
    int typeId = in.u1();
    Kit kit = app.schema.kits[kitId];
    Type type = kit.types[typeId];

    // create component
    Decoded d = new Decoded();
    if (id == 0)
      d.comp = app;
    else
    {
      d.comp = new OfflineComponent(type);
      d.comp.app = app;
    }
    d.comp.id = id;

    // meta-data
    d.comp.name     = in.str();
    d.parentId      = in.u2();
    d.childrenId    = in.u2();
    d.nextSiblingId = in.u2();

    // props
    d.comp.decodeBinaryProps(in);

    // end of component magic
    if (in.u1() != ';')
      throw new IOException("Corrupted component " + id + " " + d.comp.name);

    return d;
  }

  void decodeBinaryProps(Buf in)
    throws IOException
  {
    Slot[] slots = type.slots;
    for (int i=0; i<slots.length; ++i)
    {
      Slot slot = slots[i];
      if (slot.isProp() && slot.isConfig())
        set(slot, get(slot).decodeBinary(in));
    }
  }

  static class Decoded
  {
    OfflineComponent comp;
    int parentId;
    int childrenId;
    int nextSiblingId;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  static OfflineComponent[] none = new OfflineComponent[0];

  OfflineApp app;
  int id = -1;
  String name;
  OfflineComponent parent;
  ArrayList kids;
  HashMap kidsByName;

}

