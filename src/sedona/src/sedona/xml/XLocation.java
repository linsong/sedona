//
// This code licensed to public domain
//
// History:
//   30 May 06  Brian Frank  Creation
//

package sedona.xml;

/**
 * XLocation stores a filename and line number.
 */
public class XLocation
{

  public XLocation(String file, int line, int col)
  {
    this.file = file;
    this.line = line;
    this.col  = col;
  }

  public XLocation(String file, int line)
  {
    this.file = file;
    this.line = line;
  }

  public XLocation(String file)
  {
    this.file = file;
  }

  public XLocation()
  {
  }

  public String toString()
  {
    if (file == null)
    {
      if (line == 0)
        return "Unknown";
      else
        return "Line " + line;
    }
    else
    {
      if (line == 0)
        return file;
      else if (col == 0)
        return file + ":" + line;
      else
        return file + ":" + line + ":" + col;
    }
  }

  public String file;
  public int line;
  public int col;

}
