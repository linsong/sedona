//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Nov 08  Brian Frank  Creation
//                     

package sedona.vm.sys;

import java.lang.reflect.*;
import sedona.Type;
import sedona.vm.*;

/**
 * sys::Component public static methods
 */
public class Component_n
{                             
  
////////////////////////////////////////////////////////////////
// Invokes
////////////////////////////////////////////////////////////////
  
  public static void invokeVoid(Object self, Object slot, Context cx)    
    throws Exception
  {              
    ((ISlot)slot).accessor().invokeVoid(self);       
  }

  public static void invokeBool(Object self, Object slot, byte arg, Context cx)
    throws Exception
  {
    ((ISlot)slot).accessor().invokeBool(self, arg);       
  }

  public static void invokeInt(Object self, Object slot, int arg, Context cx)
    throws Exception
  {
    ((ISlot)slot).accessor().invokeInt(self, arg);       
  }

  public static void invokeLong(Object self, Object slot, long arg, Context cx)
    throws Exception
  {
    ((ISlot)slot).accessor().invokeLong(self, arg);       
  }

  public static void invokeFloat(Object self, Object slot, float arg, Context cx)
    throws Exception
  {
    ((ISlot)slot).accessor().invokeFloat(self, arg);       
  }

  public static void invokeDouble(Object self, Object slot, double arg, Context cx)
    throws Exception
  {
    ((ISlot)slot).accessor().invokeDouble(self, arg);       
  }

  public static void invokeBuf(Object self, Object slot, Object arg, Context cx)
    throws Exception
  {
    ((ISlot)slot).accessor().invokeBuf(self, arg);       
  }


////////////////////////////////////////////////////////////////
// Getters
////////////////////////////////////////////////////////////////

  public static byte getBool(Object self, Object slot, Context cx)
    throws Exception
  {                            
    return ((ISlot)slot).accessor().getBool(self);       
  }

  public static int getInt(Object self, Object slot, Context cx)
    throws Exception
  {
    return ((ISlot)slot).accessor().getInt(self);       
  }                                              
  
  public static long getLong(Object self, Object slot, Context cx)
    throws Exception
  {          
    return ((ISlot)slot).accessor().getLong(self);       
  }

  public static float getFloat(Object self, Object slot, Context cx)
    throws Exception
  {                            
    return ((ISlot)slot).accessor().getFloat(self);       
  }

  public static double getDouble(Object self, Object slot, Context cx)
    throws Exception
  {            
    return ((ISlot)slot).accessor().getDouble(self);       
  }

  public static Object getBuf(Object self, Object slot, Context cx)
    throws Exception
  {
    return ((ISlot)slot).accessor().getBuf(self);       
  }

////////////////////////////////////////////////////////////////
// Setters
////////////////////////////////////////////////////////////////

  public static byte doSetBool(Object self, Object slot, byte val, Context cx)
    throws Exception
  {    
    SlotAccessor acc = ((ISlot)slot).accessor();
    if (acc.getBool(self) == val) return 0;
    acc.setBool(self, val);
    return 1; 
  }
  
  public static byte doSetInt(Object self, Object slot, int val, Context cx)
    throws Exception
  {
    SlotAccessor acc = ((ISlot)slot).accessor();
    if (acc.getInt(self) == val) return 0;
    acc.setInt(self, val);
    return 1; 
  }
  
  public static byte doSetLong(Object self, Object slot, long val, Context cx)
    throws Exception
  {
    SlotAccessor acc = ((ISlot)slot).accessor();
    if (acc.getLong(self) == val) return 0;
    acc.setLong(self, val);
    return 1; 
  }
  
  public static byte doSetFloat(Object self, Object slot, float val, Context cx)
    throws Exception
  {                                              
    SlotAccessor acc = ((ISlot)slot).accessor();
    if (acc.getFloat(self) == val) return 0;
    acc.setFloat(self, val);
    return 1; 
  }
  
  public static byte doSetDouble(Object self, Object slot, double val, Context cx)
    throws Exception
  {          
    SlotAccessor acc = ((ISlot)slot).accessor();
    if (acc.getDouble(self) == val) return 0;
    acc.setDouble(self, val);
    return 1; 
  }           
    
}

