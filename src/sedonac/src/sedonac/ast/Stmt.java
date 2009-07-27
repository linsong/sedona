//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.ast;

import sedonac.*;
import sedonac.namespace.*;
import sedonac.ir.*;

/**
 * Stmt
 */
public abstract class Stmt
  extends AstNode
{

//////////////////////////////////////////////////////////////////////////
// Ids
//////////////////////////////////////////////////////////////////////////

  public static final int EXPR_STMT = 1;
  public static final int LOCAL_DEF = 2;
  public static final int RETURN    = 3;
  public static final int IF        = 4;
  public static final int FOR       = 5;
  public static final int FOREACH   = 6;
  public static final int WHILE     = 7;
  public static final int DO_WHILE  = 8;
  public static final int BREAK     = 9;
  public static final int CONTINUE  = 10;
  public static final int ASSERT    = 11;
  public static final int GOTO      = 12;
  public static final int SWITCH    = 13;

//////////////////////////////////////////////////////////////////////////
// Stmt
//////////////////////////////////////////////////////////////////////////

  Stmt(Location loc, int id)
  {
    super(loc);
    this.id = id;
  }

  public void walk(AstVisitor visitor)
  {
    visitor.enterStmt(this);
    doWalk(visitor);
    visitor.exitStmt(this);
  }

  public boolean isExit() { return false; }               
  
  public abstract int maxStack();
  static int max(int a, int b) { return Math.max(a, b); }
  static int max(int a, int b, int c) { return Math.max(Math.max(a, b), c); }
  static int max(int a, int b, int c, int d) { return Math.max(Math.max(a, b), Math.max(c, d)); }

  protected abstract void doWalk(AstVisitor visitor);

//////////////////////////////////////////////////////////////////////////
// ExprStmt
//////////////////////////////////////////////////////////////////////////

  public static class ExprStmt extends Stmt
  {
    public ExprStmt(Location loc, Expr expr)
    {
      super(loc, EXPR_STMT);
      this.expr = expr;
    }

    protected void doWalk(AstVisitor visitor)
    {
      expr = expr.walk(visitor);
    }                 
    
    public int maxStack() 
    { 
      return expr.maxStack(); 
    }

    public void write(AstWriter out)
    {
      out.indent().w(expr).nl();
    }

    public Expr expr;
  }

//////////////////////////////////////////////////////////////////////////
// LocalDef
//////////////////////////////////////////////////////////////////////////

  public static class LocalDef extends Stmt implements VarDef
  {
    public LocalDef(Location loc, Type type, String name, Expr init)
    {
      super(loc, LOCAL_DEF);
      this.type = type;
      this.name = name;
      this.init = init;
    }

    public LocalDef(Location loc, Type type, String name)
    {
      this(loc, type, name, null);
    }

    public int index()  { return index; }
    public Type type() { return type; }
    public String name() { return name; }
    public boolean isParam() { return false; }
    public boolean isLocal() { return true; }
    public String toString() { return type + " " + name; }

    public int maxStack() 
    { 
      return init == null ? 0 : init.maxStack(); 
    }

    protected void doWalk(AstVisitor visitor)
    {
      if (init != null)
        init = init.walk(visitor);
      type = visitor.type(type);
    }

    public void write(AstWriter out)
    {
      out.indent().w(type).w(" ").w(name);
      if (init != null) out.w(" = ").w(init);
      out.nl();
    }

    public static LocalDef isLocal(Stmt stmt, String name)
    {
      if (stmt instanceof LocalDef)
      {
        LocalDef local = (LocalDef)stmt;
        if (local.name.equals(name)) return local;
      }
      return null;
    }

    public int index;
    public Type type;
    public String name;
    public Expr init;
    public boolean declared;  // flag used in ResolveExpr
  }

//////////////////////////////////////////////////////////////////////////
// Return
//////////////////////////////////////////////////////////////////////////

  public static class Return extends Stmt
  {
    public Return(Location loc) { super(loc, RETURN); }

    protected void doWalk(AstVisitor visitor)
    {
      if (expr != null)
        expr = expr.walk(visitor);
    }

    public boolean isExit() { return true; }

    public int maxStack() 
    { 
      return expr == null ? 0 : expr.maxStack(); 
    }

    public void write(AstWriter out)
    {
      out.indent().w("return ");
      if (expr != null) out.w(expr);
      out.nl();
    }

    public Expr expr;
    public int foreachDepth;  // for stack balancing
  }

//////////////////////////////////////////////////////////////////////////
// If
//////////////////////////////////////////////////////////////////////////

  public static class If extends Stmt
  {
    public If(Location loc) { super(loc, IF); }

    public boolean isExit()
    {
      if (falseBlock == null) return false;
      return trueBlock.isExit() && falseBlock.isExit();
    }

    protected void doWalk(AstVisitor visitor)
    {
      cond = cond.walk(visitor);
      trueBlock.walk(visitor);
      if (falseBlock != null) falseBlock.walk(visitor);
    }

    public int maxStack() 
    {                     
      int c = cond.maxStack();
      int t = trueBlock.maxStack();
      int f = falseBlock == null ? 0 : falseBlock.maxStack();
      return max(c, t, f);
    }

    public void write(AstWriter out)
    {
      out.indent().w("if (").w(cond).w(")").nl();
      trueBlock.write(out);
      if (falseBlock != null)
      {
        out.indent().w("else").nl();
        falseBlock.write(out);
      }
    }

    public Expr cond;
    public Block trueBlock;
    public Block falseBlock;
  }

//////////////////////////////////////////////////////////////////////////
// For
//////////////////////////////////////////////////////////////////////////

  public static class For extends Stmt
    implements LocalScope
  {
    public For(Location loc) { super(loc, FOR); }

    public LocalDef[] getLocals()
    {
      if (init instanceof LocalDef)
        return new LocalDef[] { (LocalDef)init };
      else
        return new LocalDef[0];
    }

    public LocalDef resolveLocal(String name)
    {
      return Stmt.LocalDef.isLocal(init, name);
    }

    protected void doWalk(AstVisitor visitor)
    {
      if (init   != null) init.walk(visitor);
      if (cond   != null) cond   = cond.walk(visitor);
      if (update != null) update = update.walk(visitor);
      block.walk(visitor);
    }

    public int maxStack() 
    {                     
      int i = init    == null ? 0 : init.maxStack();
      int c = cond    == null ? 0 : cond.maxStack();
      int u = update  == null ? 0 : update.maxStack();              
      int b = block.maxStack();
      return max(i, c, u, b);
    }

    public void write(AstWriter out)
    {
      out.indent().w("for (");
      if (init != null) out.w(init);
      out.w("; ");
      if (cond != null) out.w(cond);
      out.w("; ");
      if (update != null) out.w(update);
      out.w(")").nl();
      block.write(out);
    }

    public Stmt init;
    public Expr cond;
    public Expr update;
    public Block block;
  }

//////////////////////////////////////////////////////////////////////////
// Foreach
//////////////////////////////////////////////////////////////////////////

  public static class Foreach extends Stmt
    implements LocalScope
  {
    public Foreach(Location loc) { super(loc, FOREACH); }

    public LocalDef[] getLocals()
    {
      return new LocalDef[] { local };
    }

    public LocalDef resolveLocal(String name)
    {
      return Stmt.LocalDef.isLocal(local, name);
    }

    protected void doWalk(AstVisitor visitor)
    {
      local.walk(visitor);
      array = array.walk(visitor);
      if (length != null) length = length.walk(visitor);
      block.walk(visitor);
    }

    public int maxStack() 
    {        
      int a = array.maxStack();
      int l = length == null ? 0 : length.maxStack();
      int b = block.maxStack();
      return max(a+l+2, b);                
    }

    public void write(AstWriter out)
    {
      out.indent().w("foreach (").w(local).w(" : ").w(array);
      if (length != null) out.w(", ").w(length);
      out.w(")").nl();
      block.write(out);
    }

    public Stmt.LocalDef local;
    public Expr array;
    public Expr length;
    public Block block;
  }

//////////////////////////////////////////////////////////////////////////
// While
//////////////////////////////////////////////////////////////////////////

  public static class While extends Stmt
  {
    public While(Location loc) { super(loc, WHILE); }

    protected void doWalk(AstVisitor visitor)
    {
      if (cond != null) cond = cond.walk(visitor);
      block.walk(visitor);
    }

    public int maxStack() 
    {                              
      int c = cond.maxStack();
      int b = block.maxStack();
      return max(c, b);        
    }

    public void write(AstWriter out)
    {
      out.indent().w("while (").w(cond).w(")").nl();
      block.write(out);
    }

    public Expr cond;
    public Block block;
  }

//////////////////////////////////////////////////////////////////////////
// DoWhile
//////////////////////////////////////////////////////////////////////////

  public static class DoWhile extends Stmt
  {
    public DoWhile(Location loc) { super(loc, DO_WHILE); }

    protected void doWalk(AstVisitor visitor)
    {
      if (cond != null) cond = cond.walk(visitor);
      block.walk(visitor);
    }

    public int maxStack() 
    {        
      int c = cond.maxStack();
      int b = block.maxStack();
      return max(c, b);        
    }

    public void write(AstWriter out)
    {
      out.indent().w("do").nl();
      block.write(out);
      out.indent().w("while (").w(cond).w(")").nl();
    }

    public Expr cond;
    public Block block;
  }

//////////////////////////////////////////////////////////////////////////
// Break
//////////////////////////////////////////////////////////////////////////

  public static class Break extends Stmt
  {
    public Break(Location loc) { super(loc, BREAK); }

    protected void doWalk(AstVisitor visitor) {}

    public int maxStack() { return 0; }

    public void write(AstWriter out)
    {
      out.indent().w("break").nl();
    }
  }

//////////////////////////////////////////////////////////////////////////
// Continue
//////////////////////////////////////////////////////////////////////////

  public static class Continue extends Stmt
  {
    public Continue(Location loc) { super(loc, CONTINUE); }

    protected void doWalk(AstVisitor visitor) {}

    public int maxStack() { return 0; }

    public void write(AstWriter out)
    {
      out.indent().w("continue").nl();
    }
  }

//////////////////////////////////////////////////////////////////////////
// Assert
//////////////////////////////////////////////////////////////////////////

  public static class Assert extends Stmt
  {
    public Assert(Location loc) { super(loc, ASSERT); }

    protected void doWalk(AstVisitor visitor)
    {
      cond = cond.walk(visitor);
    }                                             
    
    public int maxStack() 
    { 
      return cond.maxStack(); 
    }

    public void write(AstWriter out)
    {
      out.indent().w("assert (").w(cond).w(")").nl();
    }

    public Expr cond;
  }

//////////////////////////////////////////////////////////////////////////
// Goto
//////////////////////////////////////////////////////////////////////////

  public static class Goto extends Stmt
  {
    public Goto(Location loc) { super(loc, GOTO); }

    public boolean isExit() { return true; }

    public int maxStack() { return 0; }

    protected void doWalk(AstVisitor visitor) {}

    public void write(AstWriter out)
    {
      out.indent().w("goto ").w(destLabel).nl();
    }

    public String destLabel;
    public Stmt destStmt;    
    public IrOp op;
  }

//////////////////////////////////////////////////////////////////////////
// Switch
//////////////////////////////////////////////////////////////////////////

  public static class Switch extends Stmt
  {
    public Switch(Location loc) { super(loc, SWITCH); }

    public boolean isExit()
    {
      if (defaultBlock == null) return false;
      for (int i=0; i<cases.length; ++i)
        if (cases[i].block != null && !cases[i].block.isExit())
          return false;
      return defaultBlock.isExit();
    }

    protected void doWalk(AstVisitor visitor)
    {
      cond = cond.walk(visitor);
      for (int i=0; i<cases.length; ++i)
        cases[i].walk(visitor);
      if (defaultBlock != null)
        defaultBlock.walk(visitor);
    }

    public int maxStack() 
    { 
      int x = cond.maxStack(); 
      for (int i=0; i<cases.length; ++i)
      {
        x = max(x, cases[i].maxStack());
      }
      if (defaultBlock != null) x = max(x, defaultBlock.maxStack());
      return x;
    }

    public void write(AstWriter out)
    {
      out.indent().w("switch (").w(cond).w(")").nl();
      out.indent().w("{").nl();
      out.indent++;

      for  (int i=0; i<cases.length; ++i)
        cases[i].write(out);

      if (defaultBlock != null)
      {
        out.indent().w("default:").nl();
        defaultBlock.write(out, false);
      }

      out.indent--;
      out.indent().w("}").nl();
    }

    public Expr cond;
    public Case[] cases;
    public Block defaultBlock;
  }

  public static class Case extends AstNode
  {
    public Case(Location loc) { super(loc); }

    public void walk(AstVisitor visitor)
    {
      label = label.walk(visitor);
      if (block != null) block.walk(visitor);
    }                  
                     
    public int maxStack()
    {
      return block == null ? 0 : block.maxStack();
    }

    public void write(AstWriter out)
    {
      out.indent().w("case:").w(label).nl();
      if (block != null) block.write(out, false);
    }

    public Expr label;
    public Block block;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final int id;
  public String label;   // if labeled as goto destintation
  public int mark = -1;  // CodeAsm opcode index 
}
