//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   9 Jun 07  Andy Frank  Creation
//

package sedonac.test;

import sedona.*;
import sedonac.util.*;

/**
 * DocParserTest
 */
public class DocParserTest
  extends Test
{

  public void testParser()
    throws Exception
  {
    verify("Foo", new DocParser.DocNode[] { p("Foo") });
    verify("Foo\nBar", new DocParser.DocNode[] { p("Foo\nBar") });
    verify("Foo\n\nBar", new DocParser.DocNode[] { p("Foo"), p("Bar") });
    verify("\nFoo\n\nBar\n", new DocParser.DocNode[] { p("Foo"), p("Bar") });
    verify("\n\n\nFoo\n\n\n\nBar\n\n\n", new DocParser.DocNode[] { p("Foo"), p("Bar") });

    verify("Foo\n Bar", new DocParser.DocNode[] { p("Foo"), pre(" Bar") });
    verify("Foo\n Bar\n Cool", new DocParser.DocNode[] { p("Foo"), pre(" Bar\n Cool") });
    verify("Foo\n Bar\n Cool\nFoo", new DocParser.DocNode[] { p("Foo"), pre(" Bar\n Cool"), p("Foo") });
  }

  private DocParser.ParaNode p(String s) { return new DocParser.ParaNode(s); }
  private DocParser.PreNode pre(String s) { return new DocParser.PreNode(s); }

  private void verify(String text, DocParser.DocNode[] nodes)
    throws Exception
  {
    DocParser.DocNode[] n = new DocParser(text).parse();
    try
    {
      verify(n.length == nodes.length);
      for (int i=0; i<n.length; i++)
      {
        verify(n[i].id() == nodes[i].id());
        verify(n[i].text.equals(nodes[i].text));
      }
    }
    catch (Exception e)
    {
      System.out.println("\nOriginal Text:");
      System.out.println(text);
      System.out.println("");
      for (int i=0; i<n.length; i++)
      {
        System.out.println("--- " + n[i] + "----");
        System.out.println(n[i].text);
      }
      throw e;
    }

  }

}
