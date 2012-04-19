//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.parser.*;
import sedonac.namespace.*;
import sedonac.ir.*;

/**
 * Inherit walks through all the types and maps inherited slots
 * into the slot namespace of each type.  We also use this step to
 * check for invalid overrides or slot name conflicts.
 */
public class Inherit
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public Inherit(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    log.debug("  Inherit");

    // first process the IR types
    for (int i=0; i<compiler.kits.length; ++i)
    {
      IrType[] types = compiler.kits[i].types;
      for (int j=0; j<types.length; ++j)
        inherit(types[j]);
    }

    // then process the AST types
    if (compiler.ast != null)
      walkAst(WALK_TO_TYPES);

    quitIfErrors();
  }

  public void enterType(TypeDef t)
  {
    super.enterType(t);
    inherit(t);
  }

//////////////////////////////////////////////////////////////////////////
// Inherit
//////////////////////////////////////////////////////////////////////////

  private void inherit(Type t)
  {
    // if Ob, then nothing to inherit
    if (t.base() == null) return;

    // the types should be ordered by inheritance now, so that
    // means t's base type should already be fully inherited; so
    // now we inherit each of the base type's slots
    Slot[] slots = t.base().slots();
    for (int i=0; i<slots.length; ++i)
      inheritSlot(t, slots[i]);

    // check that everything I thought I was overriding was
    // actually overriding something found in base classes
    if (t instanceof TypeDef)
    {
      TypeDef def = (TypeDef)t;
      SlotDef[] defs = def.slotDefs();
      for (int i=0; i<defs.length; ++i)
        if (defs[i].isOverride() && defs[i].overrides == null)
          err("Override of unknown method '" + defs[i].name + "'", defs[i].loc);
    }
  }

  private void inheritSlot(Type t, Slot slot)
  {
    // we don't inherit special methods like initializers
    if (!slot.isInherited(t)) return;

    // check if we have a SlotDef with the same name
    String name = slot.name();
    String qname = slot.qname();
    Slot declared = t.slot(name);

    // if no duplicate, then add it and its inherited!
    if (declared == null)
    {
      t.addSlot(slot);
      return;
    }

    // if not a SlotDef, then we are inherited IR types - skip
    // the following error checking
    if (!(declared instanceof SlotDef)) return;
    SlotDef def = (SlotDef)declared;

    // check if the signatures don't match
    if (!doSignaturesMatch(slot, declared))
    {
      // if slot was marked override, then report mismatch signatures
      if (declared.isOverride())
      {
        err("Overridden method '" + name + "' has different signature than '" + qname + "'", def.loc);
        def.overrides = def;
        return;
      }

      // otherwise just report a name conflict
      else
      {
        err("Slot name '" + name + "' conflicts with inherited slot '" + qname + "'", def.loc);
        def.overrides = def;
        return;
      }
    }

    // check that I marked the method as override
    if (!def.isOverride())
    {
      err("Must use 'override' keyword to override '" + qname + "'", def.loc);
      def.overrides = def;
      return;
    }
    
    // cannot change from action to non-action, or vice-versa
    if (def.isAction() && !slot.isAction())
    {
      err("'" + def.qname + "' cannot be declared an action because it overrides non-action '" + qname + "'", def.loc);
      return;
    }
    else if (slot.isAction() && !def.isAction())
    {
      err("'" + def.qname + "' must be decalred as an action because it overrides action '" + qname + "'", def.loc);
      return;
    }

    if (!slot.isVirtual())
    {
      err("Cannot override non-virtual method '" + qname + "'", def.loc);
      def.overrides = def;
      return;
    }

    // it's overriden the exiting definition
    def.overrides = slot;
  }

  private boolean doSignaturesMatch(Slot as, Slot bs)
  {
    // fields are never matching overrides
    if (as.isField()) return false;
    if (bs.isField()) return false;

    Method a = (Method)as;
    Method b = (Method)bs;

    // check return types
    if (!a.returnType().equals(b.returnType()))
      return false;

    // check param count
    Type[] ap = a.paramTypes();
    Type[] bp = b.paramTypes();
    if (ap.length != bp.length) return false;

    // check param types
    for (int i=0; i<ap.length; ++i)
      if (!ap[i].equals(bp[i])) return false;

    // must be a match!
    return true;
  }

}
