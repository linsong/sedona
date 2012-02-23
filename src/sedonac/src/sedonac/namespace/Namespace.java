//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Mar 07  Brian Frank  Creation
//

package sedonac.namespace;

import java.util.*;

/**
 * Namespace stores a map of types used by a compiler pipeline.
 */
public class Namespace
{

//////////////////////////////////////////////////////////////////////////
// Resolution
//////////////////////////////////////////////////////////////////////////

  public Kit resolveKit(String name)
  {
    return (Kit)kits.get(name);
  }

  public Type resolveType(String sig)
  {
    int colon = sig.indexOf(':');
    if (colon < 0 || sig.charAt(colon+1) != ':')
    {
      // check primitives
      Type primitive = (Type)primitivesByName.get(sig);
      if (primitive != null) return primitive;

      throw new IllegalStateException("Invalid type signature: " + sig);
    }
    String kitName  = sig.substring(0, colon);
    String typeName = sig.substring(colon+2);

    Kit kit = resolveKit(kitName);
    if (kit == null) return null;

    return kit.type(typeName);
  }

  public Type[] resolveTypeBySimpleName(String typeName)
  {
    ArrayList acc = new ArrayList();
    Iterator it = kits.values().iterator();
    while (it.hasNext())
    {
      Kit kit = (Kit)it.next();
      Type t = kit.type(typeName);
      if (t != null) acc.add(t);
    }
    return (Type[])acc.toArray(new Type[acc.size()]);
  }

  public Slot resolveSlot(String sig) { return resolveSlot(sig, false); }
  public Slot resolveSlot(String sig, boolean checked)
  {
    int dot = sig.indexOf('.');
    if (dot < 0) throw new IllegalStateException("Invalid method signature: " + sig);
    String typeSig = sig.substring(0, dot);
    String slotName = sig.substring(dot+1);

    Type type = resolveType(typeSig);
    if (type == null) 
    {
      if (checked) 
        throw new IllegalStateException("Slot not found:: " + sig);
      return null;
    }

    Slot slot = type.slot(slotName);
    if (slot == null && checked)
      throw new IllegalStateException("Slot not found:: " + sig);
    return slot;
  }

  public Field resolveField(String sig)
  {
    return (Field)resolveSlot(sig);
  }

  public Method resolveMethod(String sig)
  {
    return (Method)resolveSlot(sig);
  }

  public Method resolveMethod(String sig, boolean checked)
  {
    return (Method)resolveSlot(sig, checked);
  }

//////////////////////////////////////////////////////////////////////////
// Mount
//////////////////////////////////////////////////////////////////////////

  public void mount(Kit kit)
  {
    if (resolveKit(kit.name()) != null)
      throw new IllegalStateException("duplicate kit: " + kit.name());

    kits.put(kit.name(), kit);

    // if sys, then replace our stubs
    if (kit.name().equals("sys"))
    {
      objType  =  requiredType(kit, "Obj");
      bufType  =  requiredType(kit, "Buf");
      compType =  requiredType(kit, "Component");
      logType  =  requiredType(kit, "Log");
      slotType =  requiredType(kit, "Slot");
      strType  =  requiredType(kit, "Str");
      sysType  =  requiredType(kit, "Sys");
      typeType =  requiredType(kit, "Type");
    }
  }

  public Type objType  = new StubType(this, null, "Obj");
  public Type bufType  = new StubType(this, objType, "Buf");
  public Type compType = new StubType(this, objType, "Component");
  public Type logType  = new StubType(this, objType, "Log");
  public Type slotType = new StubType(this, objType, "Slot");
  public Type strType  = new StubType(this, objType, "Str");
  public Type sysType  = new StubType(this, objType,  "Sys");
  public Type typeType = new StubType(this, objType, "Type");

  private Type requiredType(Kit kit, String name)
  {
    Type t = kit.type(name);
    if (t == null) throw new IllegalStateException(name);
    return t;
  }

//////////////////////////////////////////////////////////////////////////
// Predefined
//////////////////////////////////////////////////////////////////////////

  // used when we error detect errors, but attempting to continue
  public static final Type error = new PrimitiveType("error", 0, 0);

  // predefined
  public Type[] predefined()
  {
    return new Type[]
    {
      voidType,   // 0
      boolType,   // 1
      byteType,   // 2
      shortType,  // 3
      intType,    // 4
      longType,   // 5
      floatType,  // 6
      doubleType, // 7
      bufType     // 8
    };
  }

  // primitives
  public final PrimitiveType voidType   = primitive("void",   Type.voidId,  0);
  public final PrimitiveType boolType   = primitive("bool",   Type.boolId,  1);
  public final PrimitiveType byteType   = primitive("byte",   Type.byteId,  1);
  public final PrimitiveType shortType  = primitive("short",  Type.shortId, 2);
  public final PrimitiveType intType    = primitive("int",    Type.intId,   4);
  public final PrimitiveType longType   = primitive("long",   Type.longId,  8);
  public final PrimitiveType floatType  = primitive("float",  Type.floatId, 4);
  public final PrimitiveType doubleType = primitive("double", Type.doubleId, 8);
  public final PrimitiveType[] primitiveTypes =
  {
    voidType,   // 0
    boolType,   // 1
    byteType,   // 2
    shortType,  // 3
    intType,    // 4
    longType,   // 5
    floatType,  // 6
    doubleType, // 7
  };


  private static HashMap primitivesByName = new HashMap();

  private PrimitiveType primitive(String name, int id, int sizeof)
  {
    PrimitiveType p = new PrimitiveType(name, id, sizeof);
    primitivesByName.put(name, p);
    return p;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  HashMap kits = new HashMap();
}
