//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 June 09  Matthew Giannini  Creation
//
package sedonac.util;

import java.util.*;

import sedona.Env;

/**
 * VarResolver resolves strings containing sedonac variable patterns.
 *
 * @author Matthew Giannini
 * @creation Jun 15, 2009
 *
 */
public class VarResolver
{
  public VarResolver()
  {
    this(new Properties());
  }
  
  public VarResolver(Properties vars)
  {
    this.vars = vars;
  }
  
  public String resolve(final String pattern) throws Exception
  {
    return resolve(pattern, new Properties());
  }
  
  public String resolve(final String pattern, Properties overrides)
    throws Exception
  {
    StringBuffer resolved = new StringBuffer();
    final int len = pattern.length();
    try
    {
      for (int i=0; i<len; ++i)
      {
        char c = pattern.charAt(i);
        
        if (c != '$') { resolved.append(c); continue; }
        
        c = pattern.charAt(++i);
        // handle $$ escape sequence
        if (c == '$') { resolved.append(c); continue; }
        else if (c != '{') { throw new Exception("Expected '{' after '" + pattern.substring(0, i) + "'"); }
        
        // parse variable name
        StringBuffer variable = new StringBuffer();
        c = pattern.charAt(++i);
        if (c == '}') { throw new Exception("Empty patterns not allowed '" + pattern.substring(0, i+1) + "'"); }
        while (c != '}')
        {
          variable.append(c);
          c = pattern.charAt(++i);
        }
        
        final String value = getValue(variable.toString(), overrides);
        if (value == null)
          throw new Exception("Could not resolve variable: ${" + variable.toString() + "}");
        resolved.append(value);
      }
    }
    catch (IndexOutOfBoundsException e)
    {
      throw new Exception("Unexpected end of pattern: '" + pattern + "'");
    }
    
    return resolved.toString();
  }
  
  protected String getValue(final String variable, Properties overrides)
  {
    if (variable.startsWith(OS))
      return System.getenv(variable.substring(OS.length()));
    else if (variable.startsWith(SEDONA))
    {
      // Special case to get version since it is not in Env properties
      if (variable.equals(SEDONA+"version"))
        return Env.version;
      return Env.getProperty(variable.substring(SEDONA.length()));
    }
    else if (overrides.containsKey(variable))
      return overrides.getProperty(variable);
    else
      return vars.getProperty(variable);
  }
  
  private static final String OS = "os.env.";
  private static final String SEDONA = "sedona.env.";
  
  protected Properties vars;

}
