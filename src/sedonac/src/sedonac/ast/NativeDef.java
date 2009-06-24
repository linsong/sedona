//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   29 Mar 07  Brian Frank  Creation
//

package sedonac.ast;

import java.io.*;
import java.util.*;
import sedonac.*;
import sedonac.namespace.*;

/**
 * NativeDef models one native mapping from kit.xml.
 */
public class NativeDef
  extends AstNode
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public NativeDef(Location loc, String qname, NativeId id)
  {
    super(loc);
    this.qname = qname;
    this.id    = id;
  }

//////////////////////////////////////////////////////////////////////////
// AstNode
//////////////////////////////////////////////////////////////////////////

  public String toString()
  {
    return "native " + qname + " = " + id;
  }

  public void write(AstWriter out)
  {
    out.w(toString()).nl();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public String qname;
  public NativeId id;

}
