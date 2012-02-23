//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 May 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedona.Env;
import sedona.Value;
import sedona.manifest.*;
import sedona.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * BuildManifest uses the compile representation of the Kit, Types, and Slots
 * to define a runtime representation using the sedona.* APIs.  This represents
 * the information we encode into an XML document for the kit's manifest file.
 */
public class BuildManifest
  extends CompilerStep
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  public BuildManifest(Compiler compiler)
  {
    super(compiler);
  }


////////////////////////////////////////////////////////////////
// Run
////////////////////////////////////////////////////////////////

  public void run()
  {
    log.debug("  BuildManifest");
    isSys = compiler.ast.name.equals("sys");
    findReflectiveTypes();
    compiler.manifest = toKitManifest(compiler.ast);
  }

//////////////////////////////////////////////////////////////////////////
// Find Reflective Types
//////////////////////////////////////////////////////////////////////////

  private void findReflectiveTypes()
  {
    ArrayList nontests = new ArrayList();

    if (isSys)
    {
      Type[] predefined = ns.predefined();
      for (int i=0; i<predefined.length; ++i)
      {
        Type p = predefined[i];
        nontests.add(p);
        if (p.id() != i) throw new IllegalStateException();
      }
    }
    
    ArrayList tests = new ArrayList();
    int manifestId = nontests.size();
    for (int i=0; i<compiler.ast.types.length; ++i)
    {
      TypeDef t = compiler.ast.types[i];
      if (t.isaComponent())
      {
        findReflectiveSlots(t);
        if (!t.facets().getb("testonly", false))
        {
          t.id = manifestId++;
          nontests.add(t);
        }
        else
        {
          // Tests ids get set after we have set ids for all nontests.
          tests.add(t);
        }
      }
    }
    
    // Assign ids for tests now
    Iterator iter = tests.iterator();
    while (iter.hasNext())
      ((TypeDef)iter.next()).id = manifestId++;
    
    // force tests to be at the end
    nontests.addAll(tests);
    compiler.ast.reflectiveTypes = (Type[])nontests.toArray(new Type[nontests.size()]);
  }

  private void findReflectiveSlots(TypeDef t)
  {
    ArrayList acc = new ArrayList();
    SlotDef[] slotDefs = t.slotDefs();
    for (int i=0; i<slotDefs.length; ++i)
    {
      SlotDef s = slotDefs[i];
      if (s.isReflective())
      {
        s.declaredId = acc.size();
        acc.add(s);
      }
    }
    t.reflectiveSlots = (SlotDef[])acc.toArray(new SlotDef[acc.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// AST -> Manifest
//////////////////////////////////////////////////////////////////////////

  private KitManifest toKitManifest(KitDef ast)
  {
    KitManifest manifest = new KitManifest(ast.name);    

    manifest.vendor      = ast.vendor;
    manifest.version     = ast.version;
    manifest.description = ast.description;
    manifest.hasNatives  = ast.natives.length > 0;
    manifest.doc         = ast.doc;
    manifest.buildHost   = Env.hostname;
    manifest.buildTime   = Abstime.now();
    
    manifest.depends = new sedona.Depend[ast.depends.length];
    for (int i=0; i<manifest.depends.length; ++i)
      manifest.depends[i] = ast.depends[i].depend;

    int len = ast.reflectiveTypes.length;
    manifest.types = new TypeManifest[len];
    for (int i=0; i<len; ++i)
      manifest.types[i] = toTypeManifest(manifest, ast.reflectiveTypes[i]);

    manifest.checksum = new KitChecksum().compute(manifest);

    return manifest;
  }

  private TypeManifest toTypeManifest(KitManifest manifestKit, Type t)
  {
    String base = null;
    if (t.base() != null && t.base().isaComponent())
      base = t.base().qname();
    TypeManifest manifest = new TypeManifest(manifestKit, t.id(), t.name(), t.facets(), base, t.sizeof(), t.flags());

    if (t instanceof TypeDef)
    {
      SlotDef[] reflectiveSlots = ((TypeDef)t).reflectiveSlots;
      int len = reflectiveSlots == null ? 0 : reflectiveSlots.length;
      manifest.slots = new SlotManifest[len];
      for (int i=0; i<len; ++i)
        manifest.slots[i] = toSlotManifest(manifest, reflectiveSlots[i]);
    }
    else
    {
      manifest.slots = new SlotManifest[0];
    }

    return manifest;
  }

  private SlotManifest toSlotManifest(TypeManifest parent, SlotDef ast)
  {
    Type type;
    Value def = null;
    if (ast.isField())
    {
      FieldDef f = (FieldDef)ast;
      type = f.type;

      if (f.init != null)
      {
        Expr.Literal init = f.init.toLiteral();
        if (init == null)
          throw err("Invalid slot default literal: " + f.init, f.init.loc);
        def = init.toValue();
      }
    }
    else
    {
      MethodDef m = (MethodDef)ast;
      if (m.params.length == 0)
        type = ns.voidType;
      else
        type = m.params[0].type;
    }

    return new SlotManifest(parent, ast.declaredId, ast.name, ast.facets(), type.qname(), ast.rtFlags(), def);
  }

  boolean isSys;
}
