//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Oct 08  Brian Frank  Creation
//

package sedona.vm;

import java.lang.Byte;
import java.lang.Float;
import java.lang.Double;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import sedona.*;
import sedona.Type;
import sedona.kit.*;
import sedona.util.*;

/**
 * ReflectUtil attempts to make working with Java 
 * reflection slighly less painful.
 */
public class ReflectUtil
{                       

  public static Object get(Class cls, String field)    
    throws Exception
  {
    return field(cls, field).get(null);
  }

  public static Object get(Object self, String field)    
    throws Exception
  {
    return field(self.getClass(), field).get(self);
  }

  public static boolean getb(Object self, String field)    
    throws Exception
  {
    return field(self.getClass(), field).getByte(self) != 0;
  }

  public static int geti(Object self, String field)    
    throws Exception
  {
    return field(self.getClass(), field).getInt(self);
  }
  
  public static void set(Class cls, String field, Object val)
    throws Exception
  {
    field(cls, field).set(null, val);
  }

  public static void set(Object self, String field, Object val)
    throws Exception
  {
    field(self.getClass(), field).set(self, val);
  }                     

  public static void seti(Object self, String field, int val)
    throws Exception
  {
    field(self.getClass(), field).setInt(self, val);
  }                     
  
  public static Field field(Class cls, String name)
    throws Exception
  {            
    return cls.getField(name);
  }                                     
  
}

