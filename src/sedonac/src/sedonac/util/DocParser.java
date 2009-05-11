//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   9 Jun 07  Andy Frank  Creation
//

package sedonac.util;

import java.io.*;
import java.util.*;
import sedona.Env;
import sedona.util.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;

/**
 * DocParser parses a sedona doc.
 */
public class DocParser
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public DocParser(String text)
  {
    this.text = text.toCharArray();
  }

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  public DocNode[] parse()
  {
    while (next())
    {
      switch (curr())
      {
        case '\n': break;
        case ' ':  pre(); break;
        default:   para(); break;
      }
    }
    return (DocNode[])nodes.toArray(new DocNode[nodes.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// Node Parsers
//////////////////////////////////////////////////////////////////////////

  private void pre()
  {
    StringBuffer buf = new StringBuffer();
    do
    {
      char curr = curr();
      char peek = peek();
      if (curr == '\n')
      {
        if (peek == 0) break;
        if (peek != ' ') break;
      }
      buf.append(curr);
    }
    while (next());
    nodes.add(new PreNode(buf.toString()));
  }

  private void para()
  {
    StringBuffer buf = new StringBuffer();
    do
    {
      char curr = curr();
      char peek = peek();
      if (curr == '\n')
      {
        if (peek == 0) break;
        if (peek == '\n') break;
        if (peek == ' ') break;
      }
      buf.append(curr);
    }
    while (next());
    nodes.add(new ParaNode(buf.toString()));
  }

//////////////////////////////////////////////////////////////////////////
// Enumeration
//////////////////////////////////////////////////////////////////////////

  /**
   * Advance to the next character.  Return false if
   * we have moved passed the length of the string.
   */
  private boolean next()
  {
    if (off+1 >= text.length) return false;
    off++;
    return true;
  }

  /**
   * Return the character at the current position.
   */
  private char curr()
  {
    return text[off];
  }

  /**
   * Return the next character beyond the current position,
   * or 0 if its padded the end of the string.
   */
  private char peek()
  {
    return (off+1 < text.length) ? text[off+1] : 0;
  }

//////////////////////////////////////////////////////////////////////////
// DocNode
//////////////////////////////////////////////////////////////////////////

  public static abstract class DocNode
  {
    public abstract int id();
    public String text;

    public static final int PARA = 1;
    public static final int PRE  = 2;
  }

  public static class ParaNode extends DocNode
  {
    public ParaNode(String text) { this.text = text; }
    public int id() { return PARA; }
  }

  public static class PreNode extends DocNode
  {
    public PreNode(String text) { this.text = text; }
    public int id() { return PRE; }
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private ArrayList nodes = new ArrayList();

  private int off = -1;
  private char[] text;

}
