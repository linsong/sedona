//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.ast;

import java.io.*;
import sedona.util.*;

/**
 * AstWriter
 */
public class AstWriter
  extends PrintWriter
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public AstWriter(Writer out) { super(out); }

  public AstWriter(OutputStream out) { super(out); }

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  public AstWriter w(Object s)
  {
    print(s);
    return this;
  }

  public AstWriter w(boolean b)
  {
    print(b);
    return this;
  }

  public AstWriter indent()
  {
    return w(TextUtil.getSpaces(indent*2));
  }

  public AstWriter nl()
  {
    return w("\n");
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public int indent;

}
