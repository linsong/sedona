//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   26 Apr 07  Brian Frank  Creation
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
 * AssignSlotIds is used to order the reflective slots of
 * each component type by slot id.
 */
public class AssignSlotIds
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public AssignSlotIds(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    log.debug("  AssignSlotIds");

    for (int i=0; i<flat.compTypes.length; ++i)
      assignSlotIds(flat.compTypes[i]);
  }

  private void assignSlotIds(IrType t)
  {
    // get base class slots
    IrSlot[] base = t.isComponent() ? new IrSlot[0] : ((IrType)t.base).reflectiveSlots;
    if (base == null) throw new IllegalStateException(t.qname);
    ArrayList acc = new ArrayList();
    for (int i=0; i<base.length; ++i) acc.add(base[i]);

    // add my declared reflective slots
    for (int i=0; i<t.declared.length; ++i)
    {
      IrSlot slot = t.declared[i];
      if (slot.isAction() && slot.isOverride())
      {
        for (int j=0; j<acc.size(); ++j)
        {
          IrSlot inheritedAction = (IrSlot)acc.get(j);
          if (inheritedAction.isAction() && inheritedAction.name().equals(slot.name()))
          {
            // Action override - It should have same id as the action it
            // is overridding, but reference the declared slot.
            if (j != inheritedAction.id) throw new IllegalStateException();
            slot.id = inheritedAction.id;
            acc.set(j, slot);
            break;
          }
        }
        if (slot.id < 0) throw new IllegalStateException("Did not override action " + slot.qname());
      }
      else if (slot.isProperty() || slot.isAction())
      {
        slot.id = acc.size();
        acc.add(slot);
      }
    }

    // save reflective slots away
    t.reflectiveSlots = (IrSlot[])acc.toArray(new IrSlot[acc.size()]);
  }

}
