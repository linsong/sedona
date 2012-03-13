//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Dec 08  Brian Frank  Creation
//

package sedona.vm;

/**
 * SlotAccessor is used to efficiently get/set properties and
 * call actions reflectively.
 */
public abstract class SlotAccessor
{                                                         
  public ISlot slot;

////////////////////////////////////////////////////////////////
// Getters
////////////////////////////////////////////////////////////////

  public byte getBool(Object comp)     { throw err(comp, "getBool"); }
  public int getInt(Object comp)       { throw err(comp, "getInt"); }
  public long getLong(Object comp)     { throw err(comp, "getLong"); }
  public float getFloat(Object comp)   { throw err(comp, "getFloat"); }
  public double getDouble(Object comp) { throw err(comp, "getDouble"); }
  public Object getBuf(Object comp)    { throw err(comp, "getInt"); }

////////////////////////////////////////////////////////////////
// Setters
////////////////////////////////////////////////////////////////

  public void setBool(Object comp, byte val)    { throw err(comp, "setBool"); }
  public void setInt(Object comp, int val)      { throw err(comp, "setInt"); }
  public void setLong(Object comp, long val)    { throw err(comp, "setLong"); }
  public void setFloat(Object comp, float val)  { throw err(comp, "setFloat"); }
  public void setDouble(Object comp, double val){ throw err(comp, "setDouble"); }

////////////////////////////////////////////////////////////////
// Actions
////////////////////////////////////////////////////////////////

  public void invokeVoid(Object comp)               { throw err(comp, "invokeVoid"); } 
  public void invokeBool(Object comp, byte arg)     { throw err(comp, "invokeByte"); } 
  public void invokeInt(Object comp, int arg)       { throw err(comp, "invokeInt"); } 
  public void invokeLong(Object comp, long arg)     { throw err(comp, "invokeLong"); } 
  public void invokeFloat(Object comp, float arg)   { throw err(comp, "invokeFloat"); } 
  public void invokeDouble(Object comp, double arg) { throw err(comp, "invokeDouble"); } 
  public void invokeBuf(Object comp, Object arg)    { throw err(comp, "invokeBuf"); } 

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////
  
  private RuntimeException err(Object comp, String s) 
  {                                         
    IComponent c = (IComponent)comp;
    String qname = c.type().kit().name() + "::" + c.type().name();
    return new IllegalStateException(s + " on " + qname + "." + slot.name() + ": " + slot.type().name()); 
  }
  
}

