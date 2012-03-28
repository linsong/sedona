//
// This code licensed to public domain
//
// History:
//   6 Apr 02  Brian Frank  Creation
//

package sedona.xml;

/**
 * XNs models an XML namespace.  XNs are usually created as
 * attributes on XElems using the <code>XElem.defineNs()</code>
 * and <code>XElem.defineDefaultNs()</code> methods.  Two
 * XNs instances are equal if they have the same uri.
 */
public final class XNs
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Create a new XNs instance with the specified
   * prefix and uri.
   */
  public XNs(String prefix, String uri)
  {
    if (prefix == null || uri == null)
      throw new NullPointerException();

    this.prefix = prefix;
    this.uri = uri;
  }

////////////////////////////////////////////////////////////////
// Access
////////////////////////////////////////////////////////////////

  /**
   * Return if this a default XNs namespace which has a prefix of "".
   */
  public boolean isDefault()
  {
    return prefix.equals("");
  }

  /**
   * Get the prefix used to tag elements with this namespace.
   * If this is the default namespace then return "".
   */
  public final String prefix()
  {
    return prefix;
  }

  /**
   * Get the uri which defines a universally unique namespace.
   */
  public final String uri()
  {
    return uri;
  }

  /**
   * Return uri.
   */
  public String toString()
  {
    return uri;
  }

////////////////////////////////////////////////////////////////
// Identity
////////////////////////////////////////////////////////////////

  /**
   * Two instances of XNs are equal if they have the
   * exact same uri characters.
   */
  public final boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj instanceof XNs)
    {
      return uri.equals(((XNs)obj).uri);
    }
    return false;
  }
  
  public int hashCode()
  {
    return uri.hashCode();
  }

  /**
   * Two instances of XNs are equal if they have the
   * exact same uri characters.
   */
  static boolean equals(Object ns1, Object ns2)
  {
    if (ns1 == null) return ns2 == null;
    return ns1.equals(ns2);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  String prefix;
  String uri;

}
