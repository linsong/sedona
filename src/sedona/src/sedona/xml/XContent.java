//
// This code licensed to public domain
//
// History:
//   6 Apr 02  Brian Frank  Creation
//

package sedona.xml;

/**
 * XContent is the super class of the various element content classes.
 */
public abstract class XContent
{

////////////////////////////////////////////////////////////////
// Access
////////////////////////////////////////////////////////////////

  /**
   * Get the parent element or null if not currently parented.
   */
  public final XElem parent()
  {
    return parent;
  }

  /**
   * XContent equality is defined by the == operator.
   */
  public final boolean equals(Object obj)
  {
    return this == obj;
  }
  
  /**
   * Return built-in <code>java.lang.Object.hashCode()</code>.
   */
  public final int hashCode()
  {
    return super.hashCode();
  }

  /**
   * Write to the XWriter.
   */
  public abstract void write(XWriter out);

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  XElem parent;

}
