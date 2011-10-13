//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 06  Brian Frank  Creation
//

package sedonac;

import java.io.*;
import java.util.zip.*;
import sedona.xml.*;

/**
 * Location stores a filename and line number.
 */
public class Location
  implements Comparable
{                  

  public static String toString(File f)
  {          
    if (f == null) return null;
    try
    {                     
      return f.getAbsolutePath();
    }
    catch (Exception e)
    {
      return f.toString();
    }
  }

  public Location(String file, int line, int col)
  {
    this.file = file;
    this.line = line;
    this.col  = col;
  }

  public Location(String file, int line)
  {
    this.file = file;
    this.line = line;
  }

  public Location(String file)
  {
    this.file = file;
  }

  public Location(File file)
  {
    this.file = toString(file);
  }

  public Location(File zipFile, ZipEntry entry)
  {
    this.file = toString(zipFile) + "|" + entry.getName();
  }

  public Location(XLocation xloc)
  {
    this(xloc.file, xloc.line, xloc.col);
  }

  public Location(XElem elem)
  {
    this(elem.location());
  }

  public Location()
  {
  }
  
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + col;
    result = prime * result + ((file == null) ? 0 : file.hashCode());
    result = prime * result + line;
    return result;
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Location other = (Location)obj;
    if (col != other.col)
      return false;
    if (file == null)
    {
      if (other.file != null)
        return false;
    }
    else if (!file.equals(other.file))
      return false;
    if (line != other.line)
      return false;
    return true;
  }
  
  public int compareTo(Object o)
  {
    Location other = (Location)o;
    int cmp = 0;
    if (file == null)
    {
      if (other.file != null)
        return -1;
    }
    else
    {
      if ((cmp = file.compareTo(other.file)) != 0)
        return cmp;
    }
    
    if ((cmp = line - other.line) != 0)
      return cmp;
    
    return col - other.col;
  }

  public String toFileName()
  {
    if (file == null) return null;
    String n = file;
    int slash = n.lastIndexOf('/');
    if (slash > 0) return n.substring(slash+1);
    slash = n.lastIndexOf('\\');
    if (slash > 0) return n.substring(slash+1);
    return n;
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
