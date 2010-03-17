//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   14 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.namespace.*;
import sedonac.scode.*;

/**
 * ResolveIR walks all the kits, types, and slots to resolve qnames
 * into their IrType and IrSlot references and ensures that we don't
 * have any unresolved references.
 */
public class ResolveIR
  extends ResolveTypes
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public ResolveIR(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    log.debug("  ResolveIR");

    // pre-resolve flattening
    IrFlat flat = compiler.flat;
    flat.preResolve();

    // walk base, fields and methods
    for (int i=0; i<flat.types.length; ++i) resolveBase(flat.types[i]);
    for (int i=0; i<flat.fields.length; ++i) resolveField(flat.fields[i]);
    for (int i=0; i<flat.methods.length; ++i) resolveMethod(flat.methods[i]);
    quitIfErrors();

    // post-resolve flattening
    flat.postResolve();
  }

//////////////////////////////////////////////////////////////////////////
// Slot Resolution
//////////////////////////////////////////////////////////////////////////

  private void resolveBase(IrType t)
  {
    if (t.base == null) return;
    t.base = resolveType(t.base);
  }

  private void resolveField(IrField f)
  {
    f.type = resolveType(f.type);
    if (f.isConst() && f.type.isArray() && f.type.arrayOf().isRef())
    {
      ArrayType arr = (ArrayType)f.type;
      f.type = new ArrayType(arr.loc, arr.of, arr.len, true);
    }

    if (f.ctorLengthArg instanceof Expr.Name)
    {
      String id = ((Expr.Name)f.ctorLengthArg).name;
      Field x = (Field)ns.resolveSlot(id);
      if (f == null)
        err("Unknown ctorLengthArg '" + id + "' for '" + f.qname + "'", f.ctorLengthArg.loc);
      else
        f.ctorLengthArg = new Expr.Field(f.ctorLengthArg.loc, null, x);
    }
  }

  private void resolveMethod(IrMethod m)
  {
    m.ret = resolveType(m.ret);
    for (int i=0; i<m.params.length; ++i)
      m.params[i] = resolveType(m.params[i]);
    resolveOps(m.code);
  }

  private void resolveOps(IrOp[] ops)
  {
    if (ops == null) return;
    for (int i=0; i<ops.length; ++i)
    {
      IrOp op = ops[i];
      switch (op.argType())
      {
        case SCode.typeArg:
          op.resolvedArg = resolveType(op.arg);
          break;
        case SCode.slotArg:
          op.resolvedArg = resolveSlot(op.arg, null);
          break;
        case SCode.methodArg:
          op.resolvedArg = resolveSlot(op.arg, IrMethod.class);
          break;
        case SCode.fieldArg:
          op.resolvedArg = resolveSlot(op.arg, IrField.class);
          break;
      }
    }
  }

//////////////////////////////////////////////////////////////////////////
// QName Resolve
//////////////////////////////////////////////////////////////////////////

  private Type resolveType(Type type)
  {
    if (type instanceof ArrayType)
    {
      ArrayType array = (ArrayType)type;
      array.of = resolveType(array.of);
      array.len = arrayLength(array.len, array.loc);
      return array;
    }

    if (type instanceof StubType)
    {
      StubType stub = (StubType)type;
      Type resolved = ns.resolveType(stub.qname);
      if (resolved == null) throw new IllegalStateException(stub.qname);
      return resolved;
    }

    if (type instanceof UnresolvedType)
    {
      String signature = ((UnresolvedType)type).name;
      Type resolved = ns.resolveType(signature);
      if (resolved != null) return resolved;
      if (unresolved.put(signature, signature) == null)
        err("Unresolved type \"" + signature + "\"");
    }

    return type;
  }

  private Type resolveType(String qname)
  {
    Type t = ns.resolveType(qname);
    if (t == null)
      err("Unresolved type \"" + qname + "\"");
    return t;
  }

  private IrSlot resolveSlot(String qname, Class expected)
  {
    Slot slot = ns.resolveSlot(qname);
    if (slot == null || (expected != null && slot.getClass() != expected))
    {
      if (unresolved.put(qname, qname) == null)
        err("Unresolved slot \"" + qname + "\"");
    }
    return (IrSlot)slot;
  }

  public CompilerException err(String msg)
  {
    return super.err(msg, loc);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  HashMap unresolved = new HashMap();
  Location loc;
}
