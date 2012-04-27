//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   14 Mar 07  Brian Frank  Creation
//

package sedonac.ir;

import java.util.ArrayList;

import sedonac.namespace.ArrayType;
import sedonac.namespace.Namespace;


/**
 * IrFlat is a flatten view of all the kits/types/slots
 * being compiled into a scode image.
 */
public class IrFlat
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public IrFlat(Namespace ns, IrKit[] kits)
  {
    ArrayList types = new ArrayList();

    // primitive types
    primitives = new IrPrimitive[ns.primitiveTypes.length];
    for (int i=0; i<primitives.length; ++i)
      primitives[i] = new IrPrimitive(ns.primitiveTypes[i]);
    for (int i=0; i<primitives.length; ++i)
      if (primitives[i].type.id != i) throw new IllegalStateException();

    // kits and types
    for (int i=0; i<kits.length; ++i)
    {
      for (int j=0; j<kits[i].types.length; ++j)
      {
        IrType type = kits[i].types[j];
        types.add(type);
      }
    }

    this.kits = kits;
    this.types = (IrType[])types.toArray(new IrType[types.size()]);
  }

  public void preResolve()
  {
    ArrayList fields = new ArrayList();
    ArrayList staticFields = new ArrayList();
    ArrayList methods = new ArrayList();
    ArrayList staticInits = new ArrayList();
    ArrayList reflectiveSlots = new ArrayList();

    for (int i=0; i<types.length; ++i)
    {
      IrType type = types[i];
      IrSlot[] slots = type.declared;
      for (int j=0; j<slots.length; ++j)
      {
        IrSlot slot = slots[j];
        if (slot.isProperty() || slot.isAction())
        {
          reflectiveSlots.add(slot);
        }

        if (slot instanceof IrMethod)
        {
          IrMethod m = (IrMethod)slot;
          methods.add(m);
          if (m.isStaticInit()) staticInits.add(m);
        }
        else
        {
          IrField f = (IrField)slot;
          fields.add(f);
          if (f.isStatic() && !f.isDefine() && !f.isConst()) staticFields.add(f);
        }
      }
    }

    this.fields = (IrField[])fields.toArray(new IrField[fields.size()]);
    this.staticFields = (IrField[])staticFields.toArray(new IrField[staticFields.size()]);
    this.methods = (IrMethod[])methods.toArray(new IrMethod[methods.size()]);
    this.staticInits = (IrMethod[])staticInits.toArray(new IrMethod[staticInits.size()]);
    this.reflectiveSlots = (IrSlot[])reflectiveSlots.toArray(new IrSlot[reflectiveSlots.size()]);
  }

  public void postResolve()
  {
    ArrayList compTypes = new ArrayList();
    ArrayList virtTypes = new ArrayList();
    ArrayList reflectiveTypes = new ArrayList();
    ArrayList logDefines  = new ArrayList();

    for (int i=0; i<types.length; ++i)
    {
      IrType type = types[i];
      if (type.isaComponent()) compTypes.add(type);
      if (type.isaVirtual())   virtTypes.add(type);
      if (type.isReflective()) reflectiveTypes.add(type);
      
      IrSlot[] slots = type.declared;
      for (int j=0; j<slots.length; ++j)
      {
        IrSlot slot = slots[j];
        if (slot.isField())
        {                     
          IrField f = (IrField)slot;
          if (f.isDefine() && f.type.isLog()) logDefines.add(f);
        }
      }
    }
    
    this.compTypes = (IrType[])compTypes.toArray(new IrType[compTypes.size()]);
    this.virtTypes = (IrType[])virtTypes.toArray(new IrType[virtTypes.size()]);
    this.reflectiveTypes = (IrType[])reflectiveTypes.toArray(new IrType[reflectiveTypes.size()]);
    this.logDefines = (IrField[])logDefines.toArray(new IrField[logDefines.size()]);
    
    for (int i=0; i<fields.length; ++i)
    {
      if (fields[i].qname.equals("sys::Sys.logLevels"))
      {
        ((ArrayType)fields[i].type).len = new ArrayType.LiteralLen(this.logDefines.length);
        break;
      }
    }
  }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  public void dumpTypes()
  {
    System.out.println("-- IrFlat.dumpTypes ---");
    for (int i=0; i<types.length; ++i)
      System.out.println("  " + i + ":  " + types[i].qname);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  // kits
  public IrKit[] kits;

  // types
  public IrPrimitive[] primitives;
  public IrType[] types;            // doesn't include primitives
  public IrType[] reflectiveTypes;  // doesn't include primitives
  public IrType[] virtTypes;
  public IrType[] compTypes;

  // slots
  public IrSlot[] reflectiveSlots;

  // fields
  public IrField[] fields;
  public IrField[] staticFields;  // does not include defines, or consts
  public IrField[] logDefines;

  // methods
  public IrMethod[] methods;
  public IrMethod[] staticInits;

}
