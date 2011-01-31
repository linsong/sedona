//
// Copyright (c) 2000 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   24 May 00  Brian Frank  Creation
//

package sedona.util;

import java.util.*;

/**
 * Encapsulation of a decimal version string.
 * This version string must be a sequence of positive
 * decimal integers separated by "."'s and may have
 * leading zeros (per java.lang.Package specification).
 */
public class Version
  implements Comparable
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////
  
  /**
   * If s is null, then return null, otherwise 
   * return the parsed version.
   */
  public static Version parse(String s)
  {            
    if (s == null) return null;
    return new Version(s);
  }

  /**
   * Parse a version string.
   *
   * @throws IllegalArgumentException is version
   *    string is incorrectly formatted.
   */
  public Version(String s)
  {
    try
    {
      // version cannot be empty: ""
      if (s.length() == 0) throw new IllegalArgumentException();
      
      int[] buf = new int[16];
      int c = 0;

      StringTokenizer st = new StringTokenizer(s, ".");
      while(st.hasMoreTokens())
      {
        int x = Integer.parseInt(st.nextToken());
        if (x < 0)
          throw new IllegalArgumentException();
        buf[c++] = x;
      }

      versions = new int[c];
      System.arraycopy(buf, 0, versions, 0, c);
    }
    catch(Exception e)
    {
      throw new IllegalArgumentException("Invalid version string \"" + s + "\"");
    }
  }
  
  /**
   * Create version with specified segments.
   *
   * @throws IllegalArgumentException is version
   *    string is incorrectly formatted.
   */
  public Version(int[] segs, int n)
  {
    if (n == 0)
      throw new IllegalArgumentException("Cannot create empty version: n = " + n);
    versions = new int[n];
    System.arraycopy(segs, 0, versions, 0, n);
  }                 

////////////////////////////////////////////////////////////////
// Access
////////////////////////////////////////////////////////////////

  /**
   * Get version at index 0 or -1
   */
  public int major()
  {
    if (versions.length < 1) return -1;
    return versions[0];
  }

  /**
   * Get version at index 1 or -1
   */
  public int minor()
  {
    if (versions.length < 2) return -1;
    return versions[1];
  }

  /**
   * Get version at index 2 or -1
   */
  public int build()
  {
    if (versions.length < 3) return -1;
    return versions[2];
  }

  /**
   * Get version at index 3 or -1
   */
  public int patch()
  {
    if (versions.length < 4) return -1;
    return versions[3];
  }

  /**
   * Get the version at index.
   */
  public int get(int index)
  {
    return versions[index];
  }

  /**
   * Get the number of numbers in this Version.
   */
  public int size()
  {
    return versions.length;
  }

  /**
   * The null version is "0".
   */
  public boolean isNull()
  {
    if (versions.length == 1) return versions[0] == 0;
    return false;
  }

  /**
   * Return a negative integer, zero, or a positive
   * integer as this Version is less than, equal to,
   * or greater than the specified Version.  If the two
   * versions are equal in the number of digits they
   * contain, but one version has additional digits then
   * that one is considered greater (ie, 1.0.1 > 1.0).
   */
  public int compareTo(Object ver)
  {
    Version v = (Version)ver;

    // check digits we have
    int len = versions.length;
    int vLen = v.versions.length;
    for(int i=0; i<len && i<vLen; ++i)
    {
      if (versions[i] > v.versions[i]) return 1;
      if (versions[i] < v.versions[i]) return -1;
    }

    // if equal lengths they are equal
    if (len == vLen) return 0;
    if (len > vLen) return 1;
    return -1;
  }

  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + toString().hashCode();
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
    Version other = (Version)obj;
    if (!Arrays.equals(versions, other.versions))
      return false;
    return true;
  }

  /**
   * To string.
   */
  public String toString()
  {
    return toString(versions.length);
  }

  /**
   * To string using the first 'len' versions.
   */
  public String toString(int len)
  {
    StringBuffer s = new StringBuffer();
    for(int i=0; i<len && i<versions.length; ++i)
    {
      if (i>0) s.append('.');
      s.append(versions[i]);
    }
    return s.toString();
  }

  /** Version of "0" */
  public static Version NULL = new Version("0");

////////////////////////////////////////////////////////////////
// Attributes
////////////////////////////////////////////////////////////////

  private int[] versions;

}
