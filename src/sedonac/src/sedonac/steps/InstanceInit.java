//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   21 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.parser.*;
import sedonac.namespace.*;

/**
 * InstanceInit ensures ever type has an instance initializer:
 */
public class InstanceInit
  extends CompilerStep
{

  public InstanceInit(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    log.debug("  InstanceInit");
    walkAst(WALK_TO_TYPES);
    quitIfErrors();
  }

  public void enterType(TypeDef t)
  {
    super.enterType(t);

    // force every type to have an instance initializer,
    // eventually it should get pruned via dead code
    // optimization if it isn't used
    MethodDef m = t.makeInstanceInit(ns);
    Location loc = m.loc;

    // if we have a base class (other than sys::Obj), then
    // call it's instanceInit
    if (t.base != null && !t.base.isObj())
    {
      Method superInit = (Method)t.base.slot(Method.INSTANCE_INIT);
      if (superInit == null) throw new IllegalStateException(t.base.qname());
      Expr.Call call = new Expr.Call(loc, new Expr.Super(loc), superInit, new Expr[0]);
      m.code.add(m.initStmtIndex++, call.toStmt());
    }

    // if this is a Virtual, then we need to initialize the vtable field
    if (t.isaVirtual())
      m.code.add(m.initStmtIndex++, new Expr.InitVirt(loc, t).toStmt());

    // if this is a Component, then we need to initialize the type field
    if (t.isaComponent())
      m.code.add(m.initStmtIndex++, new Expr.InitComp(loc, t).toStmt());
  }

}
