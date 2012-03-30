//
// This code licensed to public domain
//
// History:
//   6 Apr 02  Brian Frank  Creation
//

package sedona.xml;

import java.io.File;

import sedona.Depend;
import sedona.util.Abstime;
import sedona.util.Version;

/**
 * XElem models a XML Element construct.
 */
public class XElem
  extends XContent
{

////////////////////////////////////////////////////////////////
// Constructors
////////////////////////////////////////////////////////////////

  /**
   * Create a new element and define a new namespace
   * attribute for the element using the specified
   * prefix and uri.
   */
  public XElem(String nsPrefix, String nsUri, String name)
  {
    this.ns = defineNs(nsPrefix, nsUri);
    this.name = name;
  }

  /**
   * Create a new element with the specified namespace and local name.
   */
  public XElem(XNs ns, String name)
  {
    this.ns = ns;
    this.name = name;
  }

  /**
   * Convenience for <code>XElem(null, name)</code>.
   */
  public XElem(String name)
  {
    this(null, name);
  }

  /**
   * Convenience for <code>XElem(null, "unnamed")</code>.
   */
  public XElem()
  {
    this(null, "unnamed");
  }

////////////////////////////////////////////////////////////////
// Name
////////////////////////////////////////////////////////////////

  /**
   * Get the namespace for this element.  If no namespace
   * has been defined for this element then return null.
   */
  public final XNs ns()
  {
    return ns;
  }

  /**
   * Return <code>ns().prefix()</code> or null if no
   * namespace is defined for this element.
   */
  public final String prefix()
  {
    if (ns == null) return null;
    return ns.prefix;
  }

  /**
   * Return <code>ns().uri()</code> or null if no
   * namespace is defined for this element.
   */
  public final String uri()
  {
    if (ns == null) return null;
    return ns.uri;
  }

  /**
   * Get the local name of the element (without prefix).
   */
  public final String name()
  {
    return name;
  }

  /**
   * Get the qualified name of the element.  If this element
   * has no namespace or is in the default namespace return
   * the local name.  Otherwise return "<prefix>:<name>".
   */
  public final String qname()
  {
    if (ns == null || ns.prefix == "") return name;
    return new StringBuffer(ns.prefix)
      .append(':').append(name).toString();
  }

////////////////////////////////////////////////////////////////
// Name Modification
////////////////////////////////////////////////////////////////

  /**
   * Check is this element defines a namespace for the
   * specified uri.  If we find one, then return an
   * XNs instance for it, otherwise return null.
   */
  public final XNs findNs(String uri)
  {
    if (ns != null && ns.uri.equals(uri))
      return ns;

    int attrSize = attrSize();
    for(int i=0; i<attrSize; ++i)
    {
      if (uri.equals(attrValue(i)))
      {
        String name = attrName(i);
        String prefix;
        if (name.equals("xmlns"))
          prefix = "";
        else if (name.startsWith("xmlns:"))
          prefix = name.substring(6);
        else
          throw new XException("Invalid xmlns: " + name, this);
        return new XNs(prefix, uri);
      }
    }

    return null;
  }

  /**
   * Resolve the specified prefix into a XNs if it is
   * defined by this node or one of its ancestors.
   */
  public final XNs resolveNsPrefix(String prefix)
  {
    // try parent first
    if (parent != null)
    {
      XNs x = parent.resolveNsPrefix(prefix);
      if (x != null) return x;
    }

    int attrSize = attrSize();
    for(int i=0; i<attrSize; ++i)
    {
      String name = attrName(i);
      if (name.startsWith("xmlns:"))
      {
        if (prefix.equals(name.substring(6)))
          return new XNs(prefix, attrValue(i));
      }
    }

    return null;
  }

  /**
   * Set this element's namespace.
   */
  public final XNs setNs(XNs ns)
  {
    this.ns = ns;
    return ns;
  }

  /**
   * Set this element's local name.
   */
  public final void setName(String name)
  {
    if (name == null) throw new NullPointerException();
    this.name = name;
  }

  /**
   * Define the default namespace for this element and
   * add the "xmlns" attribute.  Return the XNs instance
   * used to identify the namespace.  Note this does not put
   * this XElem in the namespace unless setNs() is called.
   */
  public XNs defineDefaultNs(String uri)
  {
    return defineNs("", uri);
  }

  /**
   * Define the a new namespace for this element and
   * add the "xmlns:<prefix>" attribute.  Return the XNs
   * instance used to identify the namespace.  Note this
   * does not put this XElem in the namespace unless setNs()
   * is called.
   */
  public XNs defineNs(String prefix, String uri)
  {
    return defineNs(new XNs(prefix, uri));
  }

  /**
   * Define a namespace for this element by adding the
   * "xmlns:<prefix>" attribute.  Return the ns instance
   * passed as the parameter.  Note this does not put this
   * XElem in the namespace unless setNs() is called.
   */
  public XNs defineNs(XNs ns)
  {
    String name = ns.prefix.equals("") ?
                  "xmlns" :
                  "xmlns:" + ns.prefix;

    setAttr(name, ns.uri);

    return ns;
  }

////////////////////////////////////////////////////////////////
// Attribute Access
////////////////////////////////////////////////////////////////

  /**
   * Get the number of attributes in this element.
   */
  public final int attrSize()
  {
    return attrSize;
  }

  /**
   * Get the attribute namespace for the specified index.
   * Return null if no namespace is defined for the attribute.
   * Note that the special prefixes "xml" and "xmlns" return
   * null for their namespace, and are combined into the name.
   * @throws ArrayIndexOutOfBoundsException if index >= attrSize()
   */
  public final XNs attrNs(int index)
  {
    if (index >= attrSize) throw new ArrayIndexOutOfBoundsException(index);
    return (XNs)attr[index*3+1];
  }

  /**
   * Get the attribute name for the specified index.
   * Note that the special prefixes "xml" and "xmlns" return
   * null for their namespace, and are combined into the name.
   * @throws ArrayIndexOutOfBoundsException if index >= attrSize()
   */
  public final String attrName(int index)
  {
    if (index >= attrSize) throw new ArrayIndexOutOfBoundsException(index);
    return (String)attr[index*3];
  }

  /**
   * Get the attribute value for the specified index.
   * @throws ArrayIndexOutOfBoundsException if index >= attrSize()
   */
  public final String attrValue(int index)
  {
    if (index >= attrSize) throw new ArrayIndexOutOfBoundsException(index);
    return (String)attr[index*3+2];
  }

  /**
   * Get the index of the first attribute with the
   * specified namespace and local name or return -1
   * if no match.
   */
  public final int attrIndex(XNs ns, String name)
  {
    int len = attrSize*3;
    for(int i=0; i<len; i+=3)
      if (attr[i].equals(name) && XNs.equals(ns, attr[i+1]))
        return i/3;
    return -1;
  }

  /**
   * Get the index of the first attribute with the
   * specified name not in an explicit namespace.
   * Return -1 if no match.
   */
  public final int attrIndex(String name)
  {
    int len = attrSize*3;
    for(int i=0; i<len; i+=3)
      if (attr[i].equals(name) && attr[i+1] == null)
        return i/3;
    return -1;
  }

  /**
   * Get an attribute by the specified namespace and
   * local name.  If not found then throw XException.
   * Note that the specific prefixes "xml" and "xmlns"
   * are always combined into the name.
   */
  public final String get(XNs ns, String name)
  {
    int len = attrSize*3;
    for(int i=0; i<len; i+=3)
      if (attr[i].equals(name) && XNs.equals(ns, attr[i+1]))
        return (String)attr[i+2];
    throw new XException("Missing attr '" + name + "'", this);
  }

  /**
   * Get an attribute by the specified name not in an
   * explicit namespace.  If not found then throw XException.
   * Note that the specific prefixes "xml" and "xmlns"
   * are always combined into the name.
   */
  public final String get(String name)
  {
    int len = attrSize*3;
    for(int i=0; i<len; i+=3)
      if (attr[i].equals(name) && attr[i+1] == null)
        return (String)attr[i+2];
    throw new XException("Missing attr '" + name + "'", this);
  }

  /**
   * Get an attribute by the specified namespace and local
   * name.  If not found then return the given def value.
   */
  public final String get(XNs ns, String name, String def)
  {
    int len = attrSize*3;
    for(int i=0; i<len; i+=3)
      if (attr[i].equals(name) && XNs.equals(ns, attr[i+1]))
        return (String)attr[i+2];
    return def;
  }

  /**
   * Get an attribute by the specified name.  If not
   * found then return the given def value.
   */
  public final String get(String name, String def)
  {
    int len = attrSize*3;
    for(int i=0; i<len; i+=3)
      if (attr[i].equals(name) && attr[i+1] == null)
        return (String)attr[i+2];
    return def;
  }

  /**
   * Get a boolean attribute by the specified name.  If
   * not found then return throw an XException.  If the
   * value cannot be parsed as "true" or "false" then
   * throw an XException.
   */
  public final boolean getb(String name)
  {
    String v = get(name);
    if (v.equals("true")) return true;
    if (v.equals("false")) return false;
    throw new XException("Invalid boolean attr " + name + "='" + v + "'", this);
  }

  /**
   * Get a boolean attribute by the specified name.  If
   * not found then return the given def value.  If the
   * value cannot be parsed as "true" or "false" then
   * throw an XException.
   */
  public final boolean getb(String name, boolean def)
  {
    String v = get(name, null);
    if (v == null) return def;
    if (v.equals("true")) return true;
    if (v.equals("false")) return false;
    throw new XException("Invalid boolean attr " + name + "='" + v + "'", this);
  }

  /**
   * Get an int attribute by the specified name.  If
   * not found then return throw an XException.  If the
   * value cannot be parsed then throw an XException.
   */
  public final int geti(String name)
  {
    String v = get(name);
    try
    {
      return Integer.parseInt(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid int attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get an int attribute by the specified name.  If
   * not found then return the given def value.  If the
   * value cannot be parsed then throw an XException.
   */
  public final int geti(String name, int def)
  {
    String v = get(name, null);
    if (v == null) return def;
    try
    {
      return Integer.parseInt(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid int attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a long attribute by the specified name.  If
   * not found then return throw an XException.  If the
   * value cannot be parsed then throw an XException.
   */
  public final long getl(String name)
  {
    String v = get(name);
    try
    {
      return Long.parseLong(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid long attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a long attribute by the specified name.  If
   * not found then return the given def value.  If the
   * value cannot be parsed then throw an XException.
   */
  public final long getl(String name, long def)
  {
    String v = get(name, null);
    if (v == null) return def;
    try
    {
      return Long.parseLong(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid long attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a float attribute by the specified name.  If
   * not found then return throw an XException.  If the
   * value cannot be parsed then throw an XException.
   */
  public final float getf(String name)
  {
    String v = get(name);
    try
    {
      return Float.parseFloat(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid float attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a float attribute by the specified name.  If
   * not found then return the given def value.  If the
   * value cannot be parsed then throw an XException.
   */
  public final float getf(String name, float def)
  {
    String v = get(name, null);
    if (v == null) return def;
    try
    {
      return Float.parseFloat(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid float attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a double attribute by the specified name.  If
   * not found then return throw an XException.  If the
   * value cannot be parsed then throw an XException.
   */
  public final double getd(String name)
  {
    String v = get(name);
    try
    {
      return Double.parseDouble(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid double attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a double attribute by the specified name.  If
   * not found then return the given def value.  If the
   * value cannot be parsed then throw an XException.
   */
  public final double getd(String name, double def)
  {
    String v = get(name, null);
    if (v == null) return def;
    try
    {
      return Double.parseDouble(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid double attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a version attribute by the specified name.  If
   * not found then return throw an XException.  If the
   * value cannot be parsed then throw an XException.
   */
  public final Version getVersion(String name)
  {
    String v = get(name);
    try
    {
      return new Version(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid version attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a version attribute by the specified name.  If
   * not found then return def.  If the value cannot be
   * parsed then throw an XException.
   */
  public final Version getVersion(String name, Version def)
  {
    String v = get(name, null);
    if (v == null) return def;
    try
    {
      return new Version(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid version attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a depend attribute by the specified name.  If
   * not found then return throw an XException.  If the
   * value cannot be parsed then throw an XException.
   */
  public final Depend getDepend(String name)
  {
    String v = get(name);
    try
    {
      return Depend.parse(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid depend attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a abstime attribute by the specified name.  If
   * not found then return throw an XException.  If the
   * value cannot be parsed then throw an XException.
   */
  public final Abstime getAbstime(String name)
  {
    String v = get(name);
    try
    {
      return Abstime.parse(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid abstime attr " + name + "='" + v + "'", this);
    }
  }

  /**
   * Get a abstime attribute by the specified name.  If
   * not found then return def.  If the value cannot be
   * parsed then throw an XException.
   */
  public final Abstime getAbstime(String name, Abstime def)
  {
    String v = get(name, null);
    if (v == null) return def;
    try
    {
      return Abstime.parse(v);
    }
    catch(Exception e)
    {
      throw new XException("Invalid abstime attr " + name + "='" + v + "'", this);
    }
  }

////////////////////////////////////////////////////////////////
// Attribute Modification
////////////////////////////////////////////////////////////////

  /**
   * Set the attribute value for the first attribute
   * found with the specified namespace and local name.
   * If no attribute is found with the name then add it.
   */
  public final void setAttr(XNs ns, String name, String value)
  {
    if (name == null || value == null)
      throw new NullPointerException();

    int index = attrIndex(ns, name);
    if (index != -1) attr[index*3+2] = value;
    else addAttr(ns, name, value);
  }

  /**
   * Set the attribute value for the first attribute
   * found with the specified name.  If no attribute
   * is found with the name then add it.
   */
  public final void setAttr(String name, String value)
  {
    if (name == null || value == null)
      throw new NullPointerException();

    int index = attrIndex(name);
    if (index != -1) attr[index*3+2] = value;
    else addAttr(name, value);
  }

  /**
   * Set the attribute value at the specified index.
   */
  public final void setAttr(int index, String value)
  {
    if (value == null)
      throw new NullPointerException();

    if (index >= attrSize) throw new ArrayIndexOutOfBoundsException(index);
    attr[index*3+2] = value;
  }

  /**
   * Add the specified attribute name value pair using
   * the null namespace.
   * @return this
   */
  public final XElem addAttr(String name, String value)
  {
    return addAttrImpl(null, name, value);
  }

  /**
   * Add the specified attribute name value pair.
   * @return this
   */
  public final XElem addAttr(XNs ns, String name, String value)
  {
    return addAttrImpl(ns, name, value);
  }

  /**
   * This is used by the public addAttr() APIs and by
   * XParser to add an attribute where ns is just a prefix
   * string since we might not have resolved it yet.
   */
  final XElem addAttrImpl(Object ns, String name, String value)
  {
    if (name == null || value == null)
      throw new NullPointerException();

    // insure capacity
    if (attrSize >= attr.length/3)
    {
      int resize = attr.length*6;
      if (resize == 0) resize = 12;
      Object[] temp = new Object[resize];
      System.arraycopy(attr, 0, temp, 0, attr.length);
      attr = temp;
    }

    int offset = attrSize*3;
    attr[offset+0] = name;
    attr[offset+1] = ns;
    attr[offset+2] = value;
    attrSize++;
    return this;
  }

  /**
   * Remove the first attribute with the specified namespace
   * and local name.
   */
  public final void removeAttr(XNs ns, String name)
  {
    int index = attrIndex(ns, name);
    if (index != -1) removeAttr(index);
  }

  /**
   * Remove the first attribute with the specified
   * name in the null namespace.
   */
  public final void removeAttr(String name)
  {
    int index = attrIndex(name);
    if (index != -1) removeAttr(index);
  }

  /**
   * Remove the attribute at the specified index.
   */
  public final void removeAttr(int index)
  {
    if (index >= attrSize) throw new ArrayIndexOutOfBoundsException(index);

    int shift = attrSize-index-1;
    if (shift > 0)
      System.arraycopy(attr, (index+1)*3, attr, index*3, shift*3);
    attrSize--;
  }

  /**
   * Clear the attribute list to a count of 0.
   */
  public final void clearAttr()
  {
    attrSize = 0;
  }

////////////////////////////////////////////////////////////////
// Content
////////////////////////////////////////////////////////////////

  /**
   * Get the number of XContent children of this element.
   */
  public final int contentSize()
  {
    return contentSize;
  }

  /**
   * Get the XContent child at the specified index.
   * @throws ArrayIndexOutOfBoundsException if index >= contentSize()
   */
  public final XContent content(int index)
  {
    if (index >= contentSize) throw new ArrayIndexOutOfBoundsException(index);
    return content[index];
  }

  /**
   * Get the array of XContent children for this element.
   */
  public final XContent[] content()
  {
    XContent[] r = new XContent[contentSize];
    System.arraycopy(content, 0, r, 0, r.length);
    return r;
  }

  /**
   * Get the index of the specified content instance (using
   * == operator) or return -1 if the specified content is
   * not a child of this element.
   */
  public int contentIndex(XContent child)
  {
    int len = this.contentSize;
    XContent[] content = this.content;
    for(int i=0; i<len; ++i)
      if (content[i] == child)
        return i;
    return -1;
  }

////////////////////////////////////////////////////////////////
// Elem Content
////////////////////////////////////////////////////////////////

  /**
   * Get the XContent at the specified index cast to a XElem.
   * @throws ArrayIndexOutOfBoundsException if index >= contentSize()
   */
  public final XElem elem(int index)
  {
    if (index >= contentSize) throw new ArrayIndexOutOfBoundsException(index);
    return (XElem)content[index];
  }

  /**
   * Get the array of XElem children for this element.
   */
  public final XElem[] elems()
  {
    int len = contentSize;
    if (len == 0) return noElem;
    XContent[] content = this.content;

    int n = 0;
    XElem[] temp = new XElem[len];
    for(int i=0; i<len; ++i)
      if (content[i] instanceof XElem)
        temp[n++] = (XElem)content[i];

    if (n == temp.length) return temp;

    XElem[] r = new XElem[n];
    System.arraycopy(temp, 0, r, 0, n);
    return r;
  }

  /**
   * Get all the children elements in the specified
   * namespace and with the specified local name.
   */
  public final XElem[] elems(XNs ns, String name)
  {
    int len = contentSize;
    if (len == 0) return noElem;
    XContent[] content = this.content;

    int n = 0;
    XElem[] temp = new XElem[len];
    for(int i=0; i<len; ++i)
    {
      if (content[i] instanceof XElem)
      {
        XElem kid = (XElem)content[i];
        if (XNs.equals(ns, kid.ns) && kid.name.equals(name))
          temp[n++] = kid;
      }
    }

    if (n == temp.length) return temp;

    XElem[] r = new XElem[n];
    System.arraycopy(temp, 0, r, 0, n);
    return r;
  }

  /**
   * Get all the children elements in the specified namespace.
   */
  public final XElem[] elems(XNs ns)
  {
    int len = contentSize;
    if (len == 0) return noElem;
    XContent[] content = this.content;

    int n = 0;
    XElem[] temp = new XElem[len];
    for(int i=0; i<len; ++i)
    {
      if (content[i] instanceof XElem)
      {
        XElem kid = (XElem)content[i];
        if (XNs.equals(ns, kid.ns))
          temp[n++] = kid;
      }
    }

    if (n == temp.length) return temp;

    XElem[] r = new XElem[n];
    System.arraycopy(temp, 0, r, 0, n);
    return r;
  }

  /**
   * Get all the children elements with the specified
   * local name regardless of namespace.
   */
  public final XElem[] elems(String name)
  {
    int len = contentSize;
    if (len == 0) return noElem;
    XContent[] content = this.content;

    int n = 0;
    XElem[] temp = new XElem[len];
    for(int i=0; i<len; ++i)
    {
      if (content[i] instanceof XElem)
      {
        XElem kid = (XElem)content[i];
        if (kid.name.equals(name))
          temp[n++] = kid;
      }
    }

    if (n == temp.length) return temp;

    XElem[] r = new XElem[n];
    System.arraycopy(temp, 0, r, 0, n);
    return r;
  }

  /**
   * Get the first child element in the specified
   * namespace and with the specified local name.  If
   * not found then return null.
   */
  public final XElem elem(XNs ns, String name)
  {
    int len = contentSize;
    XContent[] content = this.content;
    for(int i=0; i<len; ++i)
    {
      if (content[i] instanceof XElem)
      {
        XElem kid = (XElem)content[i];
        if (XNs.equals(ns, kid.ns) && kid.name.equals(name))
          return kid;
      }
    }
    return null;
  }

  /**
   * Get the first child element the specified local name
   * regardless of namespace.  If not found then return null.
   */
  public final XElem elem(String name)
  {
    return elem(name, false);
  }

  /**
   * Get the first child element the specified local name
   * regardless of namespace.  If not found and required
   * is false return null, otherwise throw XException.
   */
  public final XElem elem(String name, boolean required)
  {
    int len = contentSize;
    XContent[] content = this.content;
    for(int i=0; i<len; ++i)
    {
      if (content[i] instanceof XElem)
      {
        XElem kid = (XElem)content[i];
        if (kid.name.equals(name))
          return kid;
      }
    }
    if (required) throw new XException("Missing element <" + name + ">", this);
    return null;
  }

////////////////////////////////////////////////////////////////
// Text Content
////////////////////////////////////////////////////////////////

  /**
   * Creates and adds a text descendent.
   * @return this
   */
  public final XElem addText(String txt)
  {
    return addContent(new XText(txt));
  }

  /**
   * If the first content child is a XText instance, then
   * return it.  Otherwise return null.
   */
  public XText text()
  {
    if (contentSize > 0 && content[0] instanceof XText)
      return (XText)content[0];
    return null;
  }

  /**
   * Get the XContent at the specified index cast to a XText.
   * @throws ArrayIndexOutOfBoundsException if index >= contentSize()
   */
  public final XText text(int index)
  {
    if (index >= contentSize) throw new ArrayIndexOutOfBoundsException(index);
    return (XText)content[index];
  }

  /**
   * If the first content child is a XText instance, then
   * return the result of calling XText.string().  Otherwise
   * return null.
   */
  public String string()
  {
    if (contentSize > 0 && content[0] instanceof XText)
      return ((XText)content[0]).string();
    return null;
  }

////////////////////////////////////////////////////////////////
// Content Modification
////////////////////////////////////////////////////////////////

  /**
   * Add the specified content to the end of the content list.
   * @return this
   */
  public final XElem addContent(XContent child)
  {
    return addContent(contentSize, child);
  }

  /**
   * Insert the content instance as a child of this
   * element at the specified index.
   * @return this
   * @throws ArrayIndexOutOfBoundsException if index > contentSize()
   */
  public final XElem addContent(int index, XContent child)
  {
    if (child.parent != null)
      throw new IllegalArgumentException("Content already parented");
    if (index > contentSize)
      throw new ArrayIndexOutOfBoundsException(index);

    // insure capacity
    if (contentSize >= content.length)
    {
      int resize = content.length*2;
      if (resize == 0) resize = 4;
      XContent[] temp = new XContent[resize];
      System.arraycopy(content, 0, temp, 0, content.length);
      content = temp;
    }

    // shift up if necessary
    int shift = contentSize-index;
    if (shift > 0)
      System.arraycopy(content, index, content, index+1, shift);

    content[index] = child;
    contentSize++;
    child.parent = this;
    return this;
  }

  /**
   * Replace the content instance at the specified index
   * with the new content child.
   * @throws ArrayIndexOutOfBoundsException if index >= contentSize()
   */
  public final void replaceContent(int index, XContent child)
  {
    if (index >= contentSize) throw new ArrayIndexOutOfBoundsException(index);
    content[index].parent = null;
    content[index] = child;
    child.parent = this;
  }

  /**
   * Remove the specified content instance (using == operator).
   */
  public final void removeContent(XContent child)
  {
    int index = contentIndex(child);
    if (index != -1) removeContent(index);
  }

  /**
   * Remove the content child at the specified index.
   * @throws ArrayIndexOutOfBoundsException if index >= contentSize()
   */
  public final void removeContent(int index)
  {
    if (index >= contentSize) throw new ArrayIndexOutOfBoundsException(index);

    content[index].parent = null;

    int shift = contentSize-index-1;
    if (shift > 0)
      System.arraycopy(content, index+1, content, index, shift);
    contentSize--;
  }

  /**
   * Get the content children and set content count to 0.
   */
  public final void clearContent()
  {
    int len = contentSize;
    for(int i=0; i<len; ++i)
      content[i].parent = null;
    contentSize = 0;
  }

////////////////////////////////////////////////////////////////
// Formatting
////////////////////////////////////////////////////////////////

  /**
   * Dump to standard out.
   */
  public void dump()
  {
    XWriter out = new XWriter(System.out);
    write(out, 0);
    out.flush();
  }

  /**
   * Write to the specified File.
   */
  public void write(File file)
    throws Exception
  {
    XWriter out = new XWriter(file);
    write(out);
    out.close();
  }

  /**
   * Write to the specified XWriter stream with indent of 0.
   */
  public void write(XWriter out)
  {
    write(out, 0);
  }

  /**
   * Write to the specified XWriter stream.
   */
  public void write(XWriter out, int indent)
  {
    out.indent(indent);
    out.w('<');
    if (ns != null && ns.prefix != "") out.w(ns.prefix).w(':');
    out.w(name);

    int attrSize = this.attrSize;
    if (attrSize > 0)
    {
      Object[] attr = this.attr;
      for(int i=0; i<attrSize; ++i)
      {
       String attrName = (String)attr[i*3+0];
       XNs attrNs      = (XNs)attr[i*3+1];
       String attrVal  = (String)attr[i*3+2];
       out.w(' ');
       if (attrNs != null) out.w(attrNs.prefix).w(':');
       out.w(attrName).w('=').w('"').safe(attrVal).w('"');
      }
    }

    int contentSize = this.contentSize;
    if (contentSize == 0)
    {
      out.w('/').w('>').nl();
      return;
    }

    XContent[] content = this.content;
    if (contentSize == 1 && content[0] instanceof XText)
    {
      out.w('>');
      ((XText)content[0]).write(out);
    }
    else
    {
      out.w('>').nl();
      for(int i=0; i<contentSize; ++i)
      {
        XContent c = content[i];
        if (c instanceof XElem)
          ((XElem)c).write(out, indent+1);
        else
          ((XText)c).write(out);
      }
      out.indent(indent);
    }

    out.w('<').w('/');
    if (ns != null && ns.prefix != "") out.w(ns.prefix).w(':');
    out.w(name);
    out.w('>').nl();
  }

////////////////////////////////////////////////////////////////
// Misc
////////////////////////////////////////////////////////////////

  /**
   * Get compiler Location instance.
   */
  public final XLocation location()
  {
    return new XLocation(filename, line);
  }

  /**
   * Get the line number of this element in the
   * document or 0 if unknown.
   */
  public final int line()
  {
    return line;
  }

  /**
   * Make a new cloned copy of this XElem instance.
   */
  public final XElem copy(XElem copy)
  {
    copy.ns = ns;
    copy.name = name;
    copy.filename = filename;
    copy.line = line;

    if (attrSize > 0)
    {
      copy.attrSize = attrSize;
      copy.attr = new Object[attrSize*3];
      System.arraycopy(attr, 0, copy.attr, 0, copy.attr.length);
    }

    if (contentSize > 0)
    {
      copy.contentSize = contentSize;
      copy.content = new XContent[contentSize];
      System.arraycopy(content, 0, copy.content, 0, copy.content.length);
    }

    return copy;
  }

  /**
   * To string returns the start tag.
   */
  public String toString()
  {
    StringBuffer s = new StringBuffer();
    s.append('<');
    if (ns != null && ns.prefix != "") s.append(ns.prefix).append(':');
    s.append(name);

    for(int i=0; i<attrSize; ++i)
    {
     String attrName = (String)attr[i*3+0];
     XNs attrNs      = (XNs)attr[i*3+1];
     String attrVal  = (String)attr[i*3+2];
     s.append(' ');
     if (attrNs != null) s.append(attrNs.prefix).append(':');
     s.append(attrName).append('=').append('\'').append(attrVal).append('\'');
    }

    s.append('>');
    return s.toString();
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  static Object[] noAttr = new Object[0];
  static XContent[] noContent = new XContent[0];
  static XElem[] noElem = new XElem[0];

  String filename;
  XNs ns;
  String name;
  Object[] attr = noAttr;   // 0=name, 1=ns, 2=value
  int attrSize;
  XContent[] content = noContent;
  int contentSize;
  int line;

}
