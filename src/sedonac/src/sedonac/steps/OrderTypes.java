//
// Original Work:
//   Copyright (c) 2006, Brian Frank and Andy Frank
// 
// Derivative Work:
//   Copyright (c) 2007 Tridium, Inc.
//   Licensed under the Academic Free License version 3.0
//
// History:
//   20 Feb 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import sedonac.Compiler;
import sedonac.CompilerStep;
import sedonac.Location;
import sedonac.ast.TypeDef;
import sedonac.namespace.Type;

/**
 * OrderTypes orders the list of Types from top to bottom such that any
 * inherited types are guaranteed to be positioned first in the types list.
 * During this process we check for duplicate type names and cyclic inheritance.
 */
public abstract class OrderTypes
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public OrderTypes(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Order
//////////////////////////////////////////////////////////////////////////

  public void order(Type[] types)
  {
    log.debug("  OrderTypes");
    ordered = new ArrayList();
    processing = new HashMap();
    
    // Kit cksum is dependent on the order of the types. We must ensure that
    // the type order is the same across platforms, so we sort the types
    // before ordering them by inheritance.
    Arrays.sort(types, new Comparator()
    {
      public int compare(Object o1, Object o2)
      {
        return ((Type)o1).qname().compareTo(((Type)o2).qname());
      }
    });

    // create the todo map which is our working input,
    // check for duplicate type names in this loop
    todo = new HashMap();
    for (int i=0; i<types.length; ++i)
    {
      Type t = types[i];
      String qname = t.qname();
      if (todo.get(qname) != null)
        err("Duplicate type name: " + qname, toLoc(t));
      else
        todo.put(qname, t);
    }
    quitIfErrors();

    // process each type in order
    for (int i=0; i<types.length; ++i)
    {
      process(types[i]);
    }
    quitIfErrors();

    // use ordered types for rest of pipeline
    if (ordered.size() != types.length)
      throw new IllegalStateException();
    ordered.toArray(types);

    if (log.isDebug())
    {
      for (int i=0; i<types.length; ++i)
        log.debug("    " + types[i]);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Process
//////////////////////////////////////////////////////////////////////////

  private void process(Type t)
  {
    // check that this type is still in the todo
    // list, otherwise we've already processed it
    t = (Type)todo.get(t.qname());
    if (t == null) return;
    String qname = t.qname();

    // check if this guy is in the processing queue,
    // in which case we have cyclic inheritance
    if (processing.containsKey(qname))
    {
      err("Cyclic inheritance: " + qname, toLoc(t));
      return;
    }
    processing.put(qname, t);

    // process inheritance
    if (t.base() != null) process(t.base());

    // now that is has been processed, removed it the
    // todo map and add it to the ordered result list
    processing.remove(qname);
    todo.remove(qname);
    ordered.add(t);
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  public Location toLoc(Type t)
  {
    if (t instanceof TypeDef)
      return ((TypeDef)t).loc;
    else
      return new Location(t.qname());
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  HashMap processing;    // map of qname to typse being processed
  HashMap todo;          // map of qname to types left to process
  ArrayList ordered;     // ordered result list

}
