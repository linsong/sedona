//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   27 Mar 09  Matthew Giannini Creation
//
package sedonac.steps;

import java.util.*;

import sedona.*;
import sedonac.Compiler;
import sedonac.*;
import sedonac.ir.*;
import sedonac.namespace.*;

/**
 * Orders the static initializers such that if kit {@code K1} has a dependency
 * on kit {@code K2}, all of {@code K2}'s static initializers will be run
 * before {@code K1}'s.
 * <p>
 * There is no ordering of static initializers within a kit, except that
 * <code>sys::Sys_sInit()</code> is guaranteed to be the first static
 * initializer that is executed.
 * <p>
 */
public class OrderStaticInits extends CompilerStep
{

  public OrderStaticInits(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    // TODO: intra-kit sInit ordering.
    log.debug("  OrderStaticInits");
    Arrays.sort(compiler.flat.staticInits, new StaticInitComparator(graphDependencies()));
    quitIfErrors();
    
    if (log.isDebug())
    {
      for (int i=0; i< compiler.flat.staticInits.length; ++i)
        log.debug("    " + compiler.flat.staticInits[i]);
    }
  }
  
  private HashMap graphDependencies()
  {
    HashMap kitsByName = new HashMap(compiler.kits.length);
    for (int i=0; i<compiler.kits.length; ++i)
      kitsByName.put(compiler.kits[i].name(), compiler.kits[i]);
    
    HashMap graph = new HashMap(compiler.kits.length);
    for (int i=0; i<compiler.kits.length; ++i)
      graph.put(compiler.kits[i].name(), graphDependencies(kitsByName, graph, compiler.kits[i], compiler.kits[i].name()));
    return graph;
  }
  
  private HashSet graphDependencies(HashMap byName, HashMap graph, IrKit kit, String path)
  {
    HashSet kitDepends = new HashSet();
    for (int i=0; i<kit.manifest.depends.length; ++i)
    {
      Depend d = kit.manifest.depends[i];
      HashSet set = (HashSet)graph.get(d.name());
      if (set == null)
      {
        IrKit dependency = (IrKit)byName.get(d.name());
        if (dependency == null)
        {
          err("Dependency on '" + d.name() + "' required through '" + path + "'", compiler.input);
          continue;
        }
        graph.put(d.name(), set = graphDependencies(byName, graph, (IrKit)byName.get(d.name()), path+" -> "+d.name()));
      }
      kitDepends.add(d.name());
      kitDepends.addAll(set);
    }
    return kitDepends;
  }
  
  class StaticInitComparator implements Comparator
  {
    HashMap dependencies;
    Namespace ns;
    
    public StaticInitComparator(HashMap dependencies)
    {
      this.dependencies = dependencies;
      this.ns = OrderStaticInits.this.ns;
    }
    
    public int compare(Object o1, Object o2)
    {
      IrMethod s1 = (IrMethod)o1;
      IrMethod s2 = (IrMethod)o2;
      
      // force sys::Sys._sInit to have highest precedence
      if (s1.parent.is(ns.sysType)) return -1;
      else if (s2.parent.is(ns.sysType)) return 1;

      IrKit k1 = s1.parent.kit;
      IrKit k2 = s2.parent.kit;
      if (k1.name().equals(k2.name())) return 0;

      HashSet d1 = (HashSet)dependencies.get(k1.name());
      HashSet d2 = (HashSet)dependencies.get(k2.name());
      if (d1.contains(k2.name())) return 1;
      else if (d2.contains(k1.name())) return -1;
      else return 0;
    } 
  }

}
