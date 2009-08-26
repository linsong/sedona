//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Mar 07  Brian Frank  Creation
//

package sedonac.ir;

import java.io.*;
import sedona.Facets;
import sedona.Value;
import sedona.util.*;
import sedonac.*;
import sedonac.ast.*;
import sedonac.namespace.*;
import sedonac.scode.*;

/**
 * IrWriter
 */
public class IrWriter
  extends PrintWriter
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public IrWriter(Writer out) { super(out); }

  public IrWriter(OutputStream out) { super(out); }

//////////////////////////////////////////////////////////////////////////
// Kit
//////////////////////////////////////////////////////////////////////////

  public void writeType(IrType t)
  {                              
    writeFacets(t.facets);
    w(TypeUtil.flagsToString(t));
    w("class ").w(t.name);
    if (t.base != null) w(" extends ").w(t.base);    
    nl();
    w("{").nl();
    indent++;
    IrSlot[] slots = t.declared;
    for (int i=0; i<slots.length; ++i)
    {
      nl();
      writeSlot(slots[i]);
    }
    indent--;
    nl();
    w("}").nl();
  }

  public void writeSlot(IrSlot slot)
  {
    if (slot instanceof IrMethod)
      writeMethod((IrMethod)slot);
    else
      writeField((IrField)slot);
  }

  public void writeFlags(IrSlot slot)
  {
    w(TypeUtil.flagsToString(slot));
  }

  public void writeField(IrField field)
  {
    writeFacets(field.facets);
    indent();
    writeFlags(field);                     
    w(field.type);
    w(" ").w(field.name);
    if (field.isDefine() && !field.type.isLog())
      w(" = ").w(field.define.toCodeString());
    if (field.arrayInit)
      w(" = {...}");
    nl();

    if (field.ctorLengthParam > 0 || field.ctorLengthArg != null)
    {             
      w("  {").nl();
      if (field.ctorLengthParam > 0)
      {
        w("    .ctorLengthParam = " + field.ctorLengthParam).nl();
      }
      if (field.ctorLengthArg != null)
      {
        Expr ctorLenArg = field.ctorLengthArg;
        String s = (ctorLenArg instanceof Expr.Literal) ?
          ((Expr.Literal)ctorLenArg).toCodeString() :
          ((Expr.Field)ctorLenArg).field.qname();
        w("    .ctorLengthArg = " + s).nl();
      }
      w("  }").nl();
    }
  }

  public void writeMethod(IrMethod method)
  {
    // signature
    Type[] params = method.params;
    writeFacets(method.facets);
    indent();
    writeFlags(method);
    w(method.ret).w(" ").w(method.name).w("(");
    for (int i=0; i<method.params.length; ++i)
    {
      if (i > 0) w(",");
      w(method.params[i]);
    }
    w(")");

    // native
    if (method.isNative())
      w(" = ").w(method.nativeId.toString());

    nl();

    // code
    IrOp[] code = method.code;
    if (code != null)
    {
      indent().w("{").nl();
      indent++;
      if (method.maxLocals > 0) indent().w(".maxLocals = " + method.maxLocals).nl();
      for (int i=0; i<code.length; ++i)
        writeOp(code[i]);
      indent--;
      indent().w("}").nl();
    }
  }

  public void writeOp(IrOp op)
  {
    String index = TextUtil.pad(op.index + ": ", 5);
    indent().w(index).w(SCode.name(op.opcode));
    if (op.arg != null) w(" ").w(op.arg);
    nl();
  }

  public IrWriter writeFacets(Facets facets)
  {                
    String[] keys = facets.keys();
    for (int i=0; i<keys.length; ++i)                                   
    {
      String key = keys[i];
      Value val = facets.get(key);
      indent().w("@").w(key);
      if (val != sedona.Bool.TRUE) w("=").w(val.toCode());
      nl();
    }             
    return this;
  }

//////////////////////////////////////////////////////////////////////////
// IO
//////////////////////////////////////////////////////////////////////////

  public IrWriter w(Type t)
  {
    String sig = t.signature();                       
    if (sig.startsWith("const ")) sig = sig.substring(6);
    print(sig);
    return this;
  }

  public IrWriter w(Object s)
  {
    print(s);
    return this;
  }

  public IrWriter w(int i)
  {
    print(i);
    return this;
  }

  public IrWriter indent()
  {
    return w(TextUtil.getSpaces(indent*2));
  }

  public IrWriter nl()
  {
    return w("\n");
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public int indent;
}
