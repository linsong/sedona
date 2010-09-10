//
// This code licensed to public domain
//
// History:
//   6 Apr 02  Brian Frank  Creation
//

package sedona.xml;

/**
 * XException is used to indicate a problem parsing XML.
 */
public class XException
  extends RuntimeException
{

  public XException(String msg, XLocation location, Throwable cause)
  {
    super(msg);
    this.location = location;
    this.cause = cause;
  }

  public XException(String msg, XLocation location)
  {
    this(msg, location, null);
  }

  public XException(String msg, XElem elem)
  {
    this(msg, elem.location(), null);
  }

  public XLocation location;
  public Throwable cause;

}
