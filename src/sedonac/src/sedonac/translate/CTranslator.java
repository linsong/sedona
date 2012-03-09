//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 07  Brian Frank  Creation
//

// this code is in a vegetative state

package sedonac.translate;

import java.io.*;
import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * CTranslator
 */
public class CTranslator
  extends AbstractTranslator
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public CTranslator(Compiler compiler, TypeDef type)
  {
    super(compiler, type);
  }

//////////////////////////////////////////////////////////////////////////
// Translate
//////////////////////////////////////////////////////////////////////////

  public File toFile()
  {
    return new File(outDir, qname(type) + ".c");
  }

  public void doTranslate()
  {
    w("#include \"").w(qname(type)).w(".h\"").nl();
    includes();
    nl();
w("extern bool approx(float a, float b);").nl();
w("extern void doAssert(bool cond, const char* file, int line);").nl();
w("#define assert(c) doAssert((c), __FILE__, __LINE__)").nl();
    nl();
    staticFields();
    nl();
    MethodDef[] methods = type.methodDefs();
    for (int i=0; i<methods.length; ++i)
      method(methods[i]);

if (type.qname.equals("sys::Obj"))
{
  nl();
  w("bool approx(float a, float b)").nl();
  w("{").nl();
  w("  return a < b ? b-a < 0.001f : a-b < 0.001f;").nl();
  w("}").nl();
  nl();
  w("static int successes = 0;").nl();
  w("static int failures = 0;").nl();
  w("void doAssert(bool cond, const char* file, int line)").nl();
  w("{").nl();
  w("  if (cond) successes++;").nl();
  w("  else failures++;").nl();
  w("}").nl();
  nl();
  w("int main(int argc, char** argv)").nl();
  w("{").nl();
  w("  sys_FieldTest__staticInit();").nl();
  w("  sys_Sys_main();").nl();
  w("  printf(\"-- successes = %d\\n\", successes);").nl();
  w("  printf(\"-- failures  = %d\\n\", failures);").nl();
  w("}").nl();
}
  }

//////////////////////////////////////////////////////////////////////////
// Includes
//////////////////////////////////////////////////////////////////////////

  public void includes()
  {
    Type[] includes = findIncludes();
    for (int i=0; i<includes.length; ++i)
      w("#include \"").w(qname(includes[i])).w(".h\"").nl();
  }

  public Type[] findIncludes()
  {
    HashMap acc = new HashMap();
    SlotDef[] slotDefs = type.slotDefs();
    for (int i=0; i<slotDefs.length; ++i)
    {
      SlotDef slot = slotDefs[i];
      if (slot.isField())
        findIncludes(acc, (FieldDef)slot);
      else
        findIncludes(acc, (MethodDef)slot);
    }
    return (Type[])acc.values().toArray(new Type[acc.size()]);
  }

  public void findIncludes(HashMap acc, FieldDef f)
  {
    addInclude(acc, f.type);
  }

  public void findIncludes(HashMap acc, MethodDef m)
  {
    addInclude(acc, m.ret);
    for (int i=0; i<m.params.length; ++i)
      addInclude(acc, m.params[i].type);
  }

  public void addInclude(HashMap acc, Type type)
  {
    if (type.isPrimitive()) return;
    if (type.isArray()) { addInclude(acc, type.arrayOf()); return; }
    if (type.qname().equals(this.type.qname)) return;
    acc.put(type.qname(), type);
  }

//////////////////////////////////////////////////////////////////////////
// Field
//////////////////////////////////////////////////////////////////////////

  public void staticFields()
  {
    FieldDef[] fields = type.fieldDefs();
    for (int i=0; i<fields.length; ++i)
    {
      FieldDef f = fields[i];
      if (f.isStatic())
      {
        fieldSig(f);
        /*
        w(" = ");
        if (f.type.isRef())
          w("NULL");
        else
          w("0");
        */
        w(";").nl();
      }
    }
  }

  public void fieldSig(FieldDef f)
  {
    w(toType(f.type, f.isInline()));
    w(" ").w(qname(f));
    if (f.type.isArray() && f.isInline())
      w("[").w(f.type.arrayLength()).w("]");
  }

//////////////////////////////////////////////////////////////////////////
// Method
//////////////////////////////////////////////////////////////////////////

  public void method(MethodDef m)
  {
    methodSig(m).nl();
    block(m.code);
    nl();
  }

  public CTranslator methodSig(MethodDef m)
  {
    wtype(m.ret).w(" ").w(qname(m)).w("(");
    for (int i=0; i<m.params.length; ++i)
    {
      ParamDef p = m.params[i];
      if (i > 0) w(", ");
      wtype(p.type).w(" ").w(p.name);
    }
    w(")");
    return this;
  }

//////////////////////////////////////////////////////////////////////////
// Block
//////////////////////////////////////////////////////////////////////////

  public void block(Block block)
  {
    indent().w("{").nl();
    ++indent;
    localDefs(block);
    stmts(block.stmts);
    --indent;
    indent().w("}").nl();
  }

  public void localDefs(Block block)
  {
    Stmt.LocalDef[] locals = findLocalDefs(block);
    for (int i=0; i<locals.length; ++i)
    {
      Stmt.LocalDef local = locals[i];
      indent().wtype(local.type).w(" ").w(local.name).w(";").nl();
    }
    if (locals.length > 0) nl();
  }

  public Stmt.LocalDef[] findLocalDefs(Block block)
  {
    ArrayList list = new ArrayList();
    HashMap dups = new HashMap();

    for (int i=0; i<block.stmts.size(); ++i)
    {
      Stmt stmt = (Stmt)block.stmts.get(i);

      // check if statement is or contains local def
      Stmt.LocalDef def = null;
      if (stmt instanceof Stmt.LocalDef)
      {
        def = (Stmt.LocalDef)stmt;
      }
      else if (stmt instanceof Stmt.For)
      {
        Stmt.For forStmt = (Stmt.For)stmt;
        if (forStmt.init instanceof Stmt.LocalDef)
          def = (Stmt.LocalDef)forStmt.init;
      }
      if (def == null) continue;

      // if local def add it to our list
      for (int n=1; dups.containsKey(def.name); ++n)
        def.name = def.name + n;
      list.add(def);
      dups.put(def.name, def);
    }

    return (Stmt.LocalDef[])list.toArray(new Stmt.LocalDef[list.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// Stmts
//////////////////////////////////////////////////////////////////////////

  public void localDef(Stmt.LocalDef stmt, boolean standAlone)
  {
    // we only output initializers, since we declare at top of block
    if (stmt.init != null)
    {
      if (standAlone) indent();
      w(stmt.name).w(" = "); expr(stmt.init, true);
      if (standAlone) w(";").nl();
    }
  }

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////

  public void trueLiteral() { w("TRUE"); }
  public void falseLiteral() { w("FALSE"); }
  public void nullLiteral() { w("NULL"); }

  public void binary(Expr.Binary expr, boolean top)
  {
    if (expr.id == Expr.EQ && expr.lhs.type.isFloat())
    {
      w("approx(");
      expr(expr.lhs, true);
      w(", ");
      expr(expr.rhs, true);
      w(")");
    }
    else
    {
      super.binary(expr, top);
    }
  }

  public void field(Expr.Field expr)
  {
    Field f = expr.field;
    w("(");
    if (f.isStatic())
    {
      if (f.isInline() && !f.type().isArray())
        w("(&").w(qname(f)).w(")");
      else
        w(qname(f));
    }
    else
    {
      expr(expr.target);
      w("->");
      w(expr.name);
    }
    w(")");
  }

  public void call(Expr.Call expr)
  {
    if (expr.target != null && expr.target.id != Expr.STATIC_TYPE)
      throw new IllegalStateException("Call targets not implemented: " + expr);

    w(qname(expr.method));
    callArgs(expr);
  }

  public void assignNarrow(Type lhs, Expr rhs)
  {
    if (lhs.isByte())
    {
      w("(uint8_t)(");
      expr(rhs);
      w(")");
    }
    else if (lhs.isShort())
    {
      w("(uint16_t)(");
      expr(rhs);
      w(")");
    }
    else
    {
      expr(rhs);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Type Utils
//////////////////////////////////////////////////////////////////////////

  public String qname(Type type)
  {
    return type.kit().name() + "_" + type.name();
  }

  public String qname(Slot s)
  {
    return qname(s.parent()) + "_" + s.name();
  }

  public String toType(Type type) { return toType(type, false); }
  public String toType(Type type, boolean inline)
  {
    String s = toInlineType(type);
    if (type.isRef() && !inline) s += "*";
    return s;
  }

  public String toInlineType(Type type)
  {
    if (type.isArray())
    {
      return toType(type.arrayOf());
    }
    else if (type.isPrimitive())
    {
      if (type.isBool()) return "bool";
      if (type.isByte()) return "uint8_t";
      if (type.isShort()) return "uint16_t";
      if (type.isInt()) return "int32_t";
      return type.signature();
    }
    else if (type instanceof StubType)
    {
      // shouldn't need this once I wrap up
      // bootstrap type resolve
      return "sys_" + type.name();
    }
    else
    {
      return qname(type);
    }
  }


}
