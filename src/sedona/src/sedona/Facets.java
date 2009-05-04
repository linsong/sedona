//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Jul 07  Brian Frank  Creation
//

package sedona;

import java.util.*;
import sedona.xml.*;
import sedona.util.*;

/**
 * Facets stores the name/value pairs used to annotate 
 * types and slots with additional meta-data.
 */
public class Facets
{

////////////////////////////////////////////////////////////////
// Methods
////////////////////////////////////////////////////////////////

  /**
   * Return if this Facets instances if locked 
   * into read-only mode.
   */
  public boolean isRO()
  {
    return ro;
  }

  /**
   * Lock this Facets instance into read-only mode.  Any
   * attempt to change read-only facets results in an exception.
   * Return this.
   */
  public Facets ro()
  {           
    this.ro = true;
    return this;
  }

  /**
   * Return if size is zero.
   */
  public boolean isEmpty()
  {
    return map.size() == 0;
  }

  /**
   * Return number of key/value pairs.
   */
  public int size()
  {
    return map.size();
  }

  /**
   * Return list of keys pairs.
   */
  public String[] keys()
  {
    return (String[])map.keySet().toArray(new String[map.size()]);
  }

  /**
   * To string.
   */
  public String toString()
  {            
    StringBuffer s = new StringBuffer();
    s.append('[');      
    Iterator it = map.keySet().iterator();
    while (it.hasNext())
    {                                                 
      if (s.length() > 1) s.append(", ");
      String name = (String)it.next();
      s.append(name);
      
      Value val   = (Value)map.get(name);
      if (val != Bool.TRUE)
        s.append('=').append(val.toCode());
    }
    s.append(']');
    return s.toString();
  }
  
////////////////////////////////////////////////////////////////
// Get
////////////////////////////////////////////////////////////////
  
  /**
   * Get the facet for the specified name, or 
   * if not mapped return def.
   */
  public Value get(String name, Value def)
  {                                      
    Value v = (Value)map.get(name);
    if (v == null) return def;
    return v;
  }                                  

  /**
   * Convenience for <code>get(name, null)</code>.
   */
  public Value get(String name) 
  { 
    return get(name, null); 
  }

  /**
   * Get a <code>Str</code> facet.
   */
  public String gets(String name, String def)
  {                                      
    Str v = (Str)map.get(name);
    if (v == null) return def;
    return v.val;
  }                                  

  /**
   * Convenience for <code>gets(name, null)</code>.
   */
  public String gets(String name) 
  { 
    return gets(name, null); 
  }

  /**
   * Get a <code>Bool</code> facet.
   */
  public boolean getb(String name, boolean def)
  {                                      
    Bool v = (Bool)map.get(name);
    if (v == null) return def;
    return v.val;
  }                                  

  /**
   * Convenience for <code>getb(name, false)</code>.
   */
  public boolean getb(String name)
  {
    return getb(name, false);
  }

  /**
   * Get an <code>Int</code> facet.
   */
  public int geti(String name, int def)
  {                                      
    Int v = (Int)map.get(name);
    if (v == null) return def;
    return v.val;
  }                                  

  /**
   * Convenience for <code>geti(name, 0)</code>.
   */
  public int geti(String name)
  {
    return geti(name, 0);
  }

  /**
   * Get a <code>Float</code> facet.
   */
  public float getf(String name, float def)
  {                                      
    Float v = (Float)map.get(name);
    if (v == null) return def;
    return v.val;
  }                                  

  /**
   * Convenience for <code>getf(name, 0)</code>.
   */
  public float getf(String name)
  {
    return getf(name, 0.0f);
  }

////////////////////////////////////////////////////////////////
// Set
////////////////////////////////////////////////////////////////
  
  /**
   * Set the specified name value pair.  If name is
   * already mapped, then the old value is overriden.
   * Otherwise we add it.  Return this.
   */
  public Facets set(String name, Value val)
  {               
    if (ro) throw new IllegalStateException("Facets are readonly");                       
    map.put(name, val);
    return this;
  }                                  

  /**
   * Convenience for <code>set(name, Str.make(val))</code>.
   */
  public Facets sets(String name, String val)
  {
    return set(name, Str.make(val));
  }

  /**
   * Convenience for <code>set(name, Bool.make(val))</code>.
   */
  public Facets setb(String name, boolean val)
  {
    return set(name, Bool.make(val));
  }

  /**
   * Convenience for <code>set(name, Int.make(val))</code>.
   */
  public Facets seti(String name, int val)
  {
    return set(name, Int.make(val));
  }

  /**
   * Convenience for <code>set(name, Float.make(val))</code>.
   */
  public Facets setf(String name, float val)
  {
    return set(name, Float.make(val));
  }
  
  /**
   * Remove the specified facet.
   */
  public Facets remove(String name)
  {
    if (ro) throw new IllegalStateException("Facets are readonly");                       
    map.remove(name);
    return this;
  }                                  

//////////////////////////////////////////////////////////////////////////
// XML
//////////////////////////////////////////////////////////////////////////

  /**
   * Encode this facets into a XML document.
   */
  public void encodeXml(XWriter out, int indent)
  {
    out.w(TextUtil.getSpaces(indent));
    out.w("<facets>\n");
    String[] keys = keys();
    for (int i=0; i<keys.length; ++i)
    {                               
      String name = keys[i];
      Value val   = get(name);      
      String type = Type.predefinedName(val.typeId()); 
      out.w(TextUtil.getSpaces(indent+2));
      out.w("<").w(type)
        .w(" ").attr("name", name)
        .w(" ").attr("val", val.encodeString()).w("/>\n");
    }
    out.w(TextUtil.getSpaces(indent));
    out.w("</facets>\n");   
  }

  /**
   * Decode this type information from a XML document.
   */
  public static Facets decodeXml(XElem xml)
  {                   
    if (xml == null) return empty;
    
    Facets facets = new Facets();

    XElem[] elems = xml.elems();
    for (int i=0; i<elems.length; ++i)
    {                                          
      XElem elem    = elems[i];
      String typeId = elem.name();
      String name   = elem.get("name");
      String valStr = elem.get("val");      
      Value val = Value.defaultForType(Type.predefinedId(typeId)).decodeString(valStr);
      facets.set(name, val);
    }

    return facets;
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public static final Facets empty = new Facets().ro();

  private HashMap map = new HashMap(7);
  private boolean ro = false;

}
 
 
