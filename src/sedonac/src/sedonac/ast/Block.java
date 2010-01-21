//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.ast;

import java.util.*;
import sedonac.*;
import sedonac.parser.*;

/**
 * Block
 */
public class Block
  extends AstNode
  implements LocalScope
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public Block(Location loc)
  {
    super(loc);
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  public boolean isEmpty()
  {
    return stmts.size() == 0;
  }

  public Stmt[] stmts()
  {
    return (Stmt[])stmts.toArray(new Stmt[stmts.size()]);
  }

  public Stmt.LocalDef[] getLocals()
  {
    ArrayList acc = new ArrayList();
    for (int i=0; i<stmts.size(); ++i)
    {
      Stmt stmt = (Stmt)stmts.get(i);
      if (stmt instanceof Stmt.LocalDef)
        acc.add(stmt);
    }
    return (Stmt.LocalDef[])acc.toArray(new Stmt.LocalDef[acc.size()]);
  }

  public Stmt.LocalDef resolveLocal(String name)
  {
    for (int i=0; i<stmts.size(); ++i)
    {
      Stmt stmt = (Stmt)stmts.get(i);
      Stmt.LocalDef local = Stmt.LocalDef.isLocal(stmt, name);
      if (local != null) return local;
    }
    return null;
  }                    
  
  public int maxStack()
  {
    int stack = 0;
    for (int i=0; i<stmts.size(); ++i)
    {
      Stmt stmt = (Stmt)stmts.get(i);
      stack = Math.max(stack, stmt.maxStack());
    }
    return stack;
  }  

  public boolean isExit()
  {
    if (stmts.size() == 0) return false;
    return ((Stmt)stmts.get(stmts.size()-1)).isExit();
  }

  public void add(Stmt stmt)
  {
    stmts.add(stmt);
  }

  public void add(int index, Stmt stmt)
  {
    stmts.add(index, stmt);
  }

//////////////////////////////////////////////////////////////////////////
// AstNode
//////////////////////////////////////////////////////////////////////////

  public void walk(AstVisitor visitor)
  {
    visitor.enterBlock(this);
    for (int i=0; i<stmts.size(); ++i)
      ((Stmt)stmts.get(i)).walk(visitor);
    visitor.exitBlock(this);
  }

  public void write(AstWriter out) { write(out, true); }
  public void write(AstWriter out, boolean braces)
  {
    if (braces) out.indent().w("{").nl();
    out.indent++;
    Stmt[] stmts = stmts();
    for (int i=0; i<stmts.length; ++i)
    {
      Stmt stmt = stmts[i];
      if (stmt.label != null) out.indent().w(stmt.label).w(": ").nl();
      stmt.write(out);
    }
    out.indent--;
    if (braces) out.indent().w("}").nl();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public ArrayList stmts = new ArrayList();

}
