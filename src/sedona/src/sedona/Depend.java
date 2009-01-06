//
// Original Work:
//   Copyright (c) 2006, Brian Frank and Andy Frank
// 
// Derivative Work:
//   Copyright (c) 2007 Tridium, Inc.
//   Licensed under the Academic Free License version 3.0
//
// History:
//   12 Sep 07  Brian Frank  Creation
//

package sedona;

import java.util.*;
import sedona.xml.*;
import sedona.util.*;

/**
 * Depend models a dependency as a kit name and a version
 * constraint.  Convention for Sedona kits is a four part
 * version format of ' major.minor.build.patch'.
 *
 * The string format for Depend:
 *
 * <pre>
 *   depend        := name space* constraints
 *   constraints   := constraint [space* "," space* constraint]*
 *   constraint    := versionSimple | versionPlus | versionRange | checksum
 *   versionSimple := version
 *   versionPlus   := version space* "+"
 *   versionRange  := version space* "-" space* version
 *   version       := digit ["." digit]*                                   
 *   checksum      := "0x" 8 hex digits
 *   digit         := "0" - "9"
 * </pre>
 *
 * Note a simple version constraint such as "foo 1.2" really means
 * "1.2.*" - it  will match all build numbers and patch numbers
 * within "1.2".  Likewise "foo 1.2.64" will match all patch numbers
 * within the "1.2.64" build.  The "+" plus sign is used to specify a
 * given version and anything greater.  The "-" dash is used to
 * specify an inclusive range.  When using a range, then end version
 * is matched using the same rules as a simple version - for example
 * "4", "4.2", and "4.0.99 " are all matches for "foo 1.2-4".  You may
 * specify a list of potential constraints separated by commas.  Multiple
 * version dependencies are evaluated using a logical OR - any one match
 * is considered an overall match.  A version constraint and a checksum
 * constraint are evaluated using a logical AND. 
 *
 * <pre>
 * Examples:
 *    "foo 1.2"        Any version of foo 1.2 with any build or patch number
 *    "foo 1.2.64"     Any version of foo 1.2.64 with any patch number
 *    "foo 0+"         Any version of foo - version wildcard
 *    "foo 1.2+"       Any version of foo 1.2 or greater
 *    "foo 1.2-1.4"    Any version between 1.2 and 1.4 inclusive
 *    "foo 1.2,1.4"    Any version of 1.2 or 1.4
 *    "foo 0x1b02d4fc" Any version of foo with a checksum of 0x1b02d4fc
 *    "foo 1.0, 0x1b02d4fc" Any version of foo 1.0 and a checksum of 0x1b02d4fc
 * </pre>
 */
public class Depend
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public static Depend parse(String s)
  {
    try
    {
      return new Parser(s).parse();
    }
    catch (Throwable e)
    {
      throw new IllegalArgumentException("Invalid Depend: '" + s + "'");
    }  
  }

  public static Depend makeChecksum(String name, int checksum)
  {        
    Constraint c = new Constraint();
    c.checksum = checksum;
    return new Depend(name, new Constraint[] { c });
  }

  private Depend(String name, Constraint[] constraints)
  {
    this.name = name;
    this.constraints = constraints;
  }

//////////////////////////////////////////////////////////////////////////
// Parser
//////////////////////////////////////////////////////////////////////////

  static class Parser
  {
    Parser(String str)
    {
      this.str = str;
      this.len = str.length();
      consume();
    }

    Depend parse()
    {
      name = name();
      constraints.add(constraint());
      while (cur == ',')
      {
        consume();
        consumeSpaces();
        constraints.add(constraint());
      }
      if (pos <= len) throw new RuntimeException();
      return new Depend(name, (Constraint[])constraints.toArray(new Constraint[constraints.size()]));
    }

    private String name()
    {
      StringBuffer s = new StringBuffer();
      while (cur != ' ')
      {
        if (cur < 0) throw new RuntimeException();
        s.append((char)cur);
        consume();
      }
      consumeSpaces();
      if (s.length() == 0) throw new RuntimeException();
      return s.toString();
    }

    private Constraint constraint()
    {
      Constraint c = new Constraint();     
      if (cur == '0' && peek() == 'x')
      {
        c.checksum = checksum();
        return c;
      }   
      
      c.version = version();
      consumeSpaces();
      if (cur == '+')
      {
        c.isPlus = true;
        consume();
        consumeSpaces();
      }
      else if (cur == '-')
      {
        consume();
        consumeSpaces();
        c.endVersion = version();
        consumeSpaces();
      }
      return c;
    }

    private Version version()
    {                 
      int[] segs = new int[8];
      int seg = consumeDigit(); 
      int n = 0;
      while (true)
      {
        if ('0' <= cur && cur <= '9')
        {
          seg = seg*10 + consumeDigit();
        }
        else
        {
          segs[n++] = seg;
          seg = 0;
          if (cur != '.') break;
          else consume();
        }
      }
      return new Version(segs, n);
    }

    private int checksum()
    {
      consume();  // 0
      consume();  // x
      int v = 0;  
      while (true)
      {                              
        int digit = consumeHexDigit();
        if (digit < 0) break;
        v = (v << 4) | digit;
      }
      return v;
    }

    private int consumeDigit()
    {
      if ('0' <= cur && cur <= '9')
      {
        int digit = cur - '0';
        consume();
        return digit;
      }
      throw new RuntimeException();
    }

    private int consumeHexDigit()
    {
      if ('0' <= cur && cur <= '9')
      {
        int digit = cur - '0';
        consume();
        return digit;
      } 
      
      if ('a' <= cur && cur <= 'f')
      {
        int digit = cur - 'a' + 10;
        consume();
        return digit;
      }
      
      if ('A' <= cur && cur <= 'F')
      {
        int digit = cur - 'A' + 10;
        consume();
        return digit;
      }
      
      return -1;
    }

    private void consumeSpaces()
    {
      while (cur == ' ') consume();
    }           
    
    private int peek()
    {
      if (pos >= len) return -1;
      return str.charAt(pos);
    }

    private void consume()
    {
      if (pos < len)
      {
        cur = str.charAt(pos++);
      }
      else
      {
        cur = -1;
        pos = len+1;
      }
    }

    int cur;
    int pos;
    int len;
    String str;
    String name;
    ArrayList constraints = new ArrayList(4);
  }  
//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Get the kit name of this dependency.
   */
  public String name()
  {                              
    return name;
  }

  /**
   * Hash is based on toString representation.
   */
  public int hashCode()
  {
    return toString().hashCode();
  }

  /**
   * Equality is based on toString representation.
   */
  public boolean equals(Object obj)
  {
    if (obj instanceof Depend)
      return obj.toString().equals(toString());
    else
      return false;
  }
    
  /**
   * Get the normalized string format of this dependency.  Normalized
   * dependency strings do not contain any optional spaces.  See class
   * header for specification of the format.
   */
  public String toString()
  {
    StringBuffer s = new StringBuffer();
    s.append(name).append(' ');
    for (int i=0; i<constraints.length; ++i)
    {
      if (i > 0) s.append(',');
      Constraint c = constraints[i];
      if (c.version == null) 
      {
        s.append("0x").append(Integer.toHexString(c.checksum));
      }
      else
      {
        s.append(c.version);
        if (c.isPlus) s.append('+');
        if (c.endVersion != null) s.append('-').append(c.endVersion);
      }
    }
    return s.toString();
  }  

  /**
   * Get the number of version constraints.  There is always
   * at least one constraint.
   */
  public int size()
  {
    return constraints.length;
  }

  /**
   * Get the version constraint at specified index:
   *   - versionSimple: returns the version
   *   - versionPlus:   returns the version
   *   - versionRange:  returns the start version
   *   - checksum:      returns null
   */
  public Version version(int index)
  {
    return constraints[index].version;
  }

  /**
   * Return if the constraint at the specified index is a versionPlus:
   *   - versionSimple: returns false
   *   - versionPlus:   returns true
   *   - versionRange:  returns false
   *   - checksum:      returns false
   */
  public boolean isPlus(int index)
  {
    return constraints[index].isPlus;
  }

  /**
   * Return if the constraint at the specified index is a versionRange:
   *   - versionSimple: returns false
   *   - versionPlus:   returns false
   *   - versionRange:  returns true
   *   - checksum:      returns false
   */
  public boolean isRange(int index)
  {
    return constraints[index].endVersion != null;
  }

  /**
   * Return the ending version if versionRange:
   *   - versionSimple: returns null
   *   - versionPlus:   returns null
   *   - versionRange:  returns end version
   *   - checksum:      returns null
   */
  public Version endVersion(int index)
  {
    return constraints[index].endVersion;
  }

  /**
   * Get the checksum constraint at specified index:
   *   - versionSimple: returns -1
   *   - versionPlus:   returns -1
   *   - versionRange:  returns -1
   *   - checksum:      returns the checksum
   */
  public int checksum(int index)
  {
    return constraints[index].checksum;
  }
  
  /**
   * Convenience for <code>match(v, -1)</code>.
   */
  public boolean match(Version v)
  {                             
    return match(v, -1);
  }

  /**
   * Convenience for <code>match(null, checksum)</code>.
   */
  public boolean match(int checksum)
  {
    return match(null, checksum);
  }

  /**
   * Return if the specified version is a match against
   * this dependencies constraints.  See class header for
   * matching rules.
   */
  public boolean match(Version v, int checksum)
  {                          
    boolean verTest       = false;
    boolean checksumTest  = false;
    boolean verMatch      = false;
    boolean checksumMatch = false;
    
    for (int i=0; i<constraints.length; ++i)
    {
      Constraint c = constraints[i];    
      
      // keep track of what kind of tests are passed in 
      // and what kind of constrainsts we have
      if (c.checksum != -1)
        checksumTest = checksum != -1;
      else
        verTest = v != null;
              
      // match checksum
      if (checksum != -1)
        checksumMatch |= matchChecksum(c, checksum);
      
      // match version
      if (v != null)
        verMatch |= matchVersion(c, v);
    }    
    
    if (verTest)
    {
      if (checksumTest)
        return verMatch & checksumMatch;
      else
        return verMatch;
    }      
    else
    {
      return checksumMatch;
    }
  }

  private boolean matchChecksum(Constraint c, int checksum)
  {
    return c.checksum == checksum;
  }

  private boolean matchVersion(Constraint c, Version v)
  {      
    if (c.version == null)
    {
      return false;
    }                                             
    else if (c.isPlus)
    {
      // versionPlus
      return c.version.compareTo(v) <= 0;
    }
    else if (c.endVersion != null)
    {
      // versionRange
      return (c.version.compareTo(v) <= 0 &&
              (c.endVersion.compareTo(v) >= 0 || doMatch(c.endVersion, v)));
    }
    else
    {
      // versionSimple
      return doMatch(c.version, v);
    }
  }

  private static boolean doMatch(Version a, Version b)
  {
    if (a.size() > b.size()) return false;
    for (int i=0; i<a.size(); ++i)
      if (a.get(i) != b.get(i))
        return false;
    return true;
  }  
  
//////////////////////////////////////////////////////////////////////////
// XML
//////////////////////////////////////////////////////////////////////////

  /**
   * Encode this dependency into a XML document.
   */
  public void encodeXml(XWriter out)
  {
    out.w("  <depend ").attr("on", toString()).w("/>\n");
  }

  /**
   * Decode this dependency information from a XML document.
   */
  public static Depend decodeXml(XElem xml)
  {
    return parse(xml.get("on"));
  }

//////////////////////////////////////////////////////////////////////////
// Constraint
//////////////////////////////////////////////////////////////////////////

  static class Constraint
  {
    Version version;
    boolean isPlus;
    Version endVersion;
    int checksum = -1;
  }
  
//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private final String name;
  private final Constraint[] constraints;
  
}
