//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ir.*;
import sedonac.namespace.*;
import sedonac.scode.*;
import sedonac.util.*;

/**
 * VTableLayout builds each Component type's vtables with
 * all the virtual method references.
 */
public class VTableLayout
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public VTableLayout(Compiler compiler)
  {
    super(compiler);
    this.image = compiler.image;
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    log.debug("  VTableLayout");
    for (int i=0; i<flat.virtTypes.length; ++i)
      layout(flat.virtTypes[i]);
    quitIfErrors();
    // dump();
  }

//////////////////////////////////////////////////////////////////////////
// Layout
//////////////////////////////////////////////////////////////////////////

  private void layout(IrType t)
  {
    ArrayList vtable = new ArrayList();
    HashMap map = new HashMap();

    // get my parent's vtable which I inherit
    if (!t.isVirtual())
    {
      IrVTable inherit = ((IrType)t.base).vtable;
      if (inherit == null) throw new IllegalStateException(t.qname());
      for (int i=0; i<inherit.methods.length; ++i)
      {
        IrMethod m = inherit.methods[i];
        m.vindex = vtable.size();
        map.put(m.name, new Integer(m.vindex));
        vtable.add(m);
      }
    }

    // walk through my virtual/overriden methods
    IrSlot[] slots = t.declared;
    for (int i=0; i<slots.length; ++i)
    {
      // skip fields
      if (slots[i].isField()) continue;
      IrMethod m = (IrMethod)slots[i];

      // if override
      if (m.isOverride())
      {
        Integer index = (Integer)map.get(m.name);
        if (index == null) err("Unknown override '" + m.qname + "'");
        else
        {
          m.vindex = index.intValue();
          vtable.set(m.vindex, m);
        }
      }

      // if new virtual
      else if (m.isVirtual())
      {
        m.vindex = vtable.size();
        map.put(m.name, new Integer(m.vindex));
        vtable.add(m);
      }
    }

    // save away vtable
    t.vtable = new IrVTable();
    t.vtable.methods = (IrMethod[])vtable.toArray(new IrMethod[vtable.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// Dump
//////////////////////////////////////////////////////////////////////////

  private void dump()
  {
    for (int i=0; i<flat.types.length; ++i)
      dump(flat.types[i]);
  }

  private void dump(IrType t)
  {
    if (t.vtable == null) return;
    System.out.println("---- " + t.qname + " vtable ----");
    for (int i=0; i<t.vtable.methods.length; ++i)
    {
      IrMethod m = t.vtable.methods[i];
      String s = m == null ? "null" : m.qname;
      System.out.println("  " + i + ":  " + s);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  SCodeImage image;
  Location loc;

}
