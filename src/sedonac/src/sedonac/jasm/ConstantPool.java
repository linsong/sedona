//
// Copyright (c) 2000 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 00  Brian Frank  Creation
//
package sedonac.jasm;

import java.lang.reflect.*;
import java.util.*;

/**
 * ConstantPool
 */
public class ConstantPool
{  

////////////////////////////////////////////////////////////////
// Index Access
////////////////////////////////////////////////////////////////

  public int utf(String str)
  {
    Integer ref = (Integer)utfTable.get(str);
    if (ref != null)
      return ref.intValue();

    buf.u1(Jvm.CONSTANT_Utf8);
    buf.utf(str);

    count++;
    utfTable.put(str, new Integer(count));
    return count;  
  }
  
  public int cls(String className)
  {
    Integer ref = (Integer)classTable.get(className);
    if (ref != null) return ref.intValue();
    
    int i = utf(className);  
    buf.u1(Jvm.CONSTANT_Class);
    buf.u2(i);
    
    count++;
    classTable.put(className, new Integer(count));
    return count;  
  }

  public int string(String str)
  {
    Integer ref = (Integer)stringTable.get(str);
    if (ref != null) return ref.intValue();
    
    int i = utf(str);
    buf.u1(Jvm.CONSTANT_String);
    buf.u2(i);
    
    count++;
    stringTable.put(str, new Integer(count));
    return count;  
  }

  public int intConst(int i)
  {
    Integer ref = (Integer)integerTable.get(i);
    if (ref != null) return ref.intValue();
    
    buf.u1(Jvm.CONSTANT_Integer);
    buf.u4(i);
    
    count++;
    integerTable.put(i, new Integer(count));
    return count;  
  }

  public int floatConst(float f)
  {
    Float key = new Float(f);
    Integer ref = (Integer)floatTable.get(key);
    if (ref != null) return ref.intValue();
    
    buf.u1(Jvm.CONSTANT_Float);
    buf.u4( Float.floatToIntBits(f) );
    
    count++;
    floatTable.put(key, new Integer(count));
    return count;  
  }

  public int doubleConst(double d)
  {
    Double key = new Double(d);
    Integer ref = (Integer)doubleTable.get(key);
    if (ref != null) return ref.intValue();
    
    buf.u1(Jvm.CONSTANT_Double);
    buf.u8( Double.doubleToLongBits(d) );
    
    count++;
    doubleTable.put(key, new Integer(count));
    count++; // double entries use two slots
    return count-1;  
  }

  public int longConst(long lng)
  {
    Long key = new Long(lng);
    Integer ref = (Integer)longTable.get(key);
    if (ref != null) return ref.intValue();
    
    buf.u1(Jvm.CONSTANT_Long);
    buf.u8(lng);
    
    count++;
    longTable.put(key, new Integer(count));
    count++; // long entries use two slots
    return count-1;  
  }

  public int nt(int name, int type)
  {
    int hash = name << 16 | type;
    Integer ref = (Integer)fieldTable.get(hash);
    if (ref != null) return ref.intValue();
    
    buf.u1(Jvm.CONSTANT_NameAndType);
    buf.u2(name);
    buf.u2(type);
    
    count++;
    ntTable.put(hash, new Integer(count));
    return count;
  }
  
  public int field(int cls, int nt)
  {
    int hash = cls << 16 | nt;
    Integer ref = (Integer)fieldTable.get(hash);
    if (ref != null) return ref.intValue();
    
    buf.u1(Jvm.CONSTANT_Fieldref);
    buf.u2(cls);
    buf.u2(nt);
    
    count++;
    fieldTable.put(hash, new Integer(count));
    return count;
  }

  public int method(int cls, int nt)
  {
    int hash = cls << 16 | nt;
    Integer ref = (Integer)methodTable.get(hash);
    if (ref != null) return ref.intValue();
    
    buf.u1(Jvm.CONSTANT_Methodref);
    buf.u2(cls);
    buf.u2(nt);
    
    count++;
    methodTable.put(hash, new Integer(count));
    return count;
  }

  public int iface(int cls, int nt)
  {
    int hash = cls << 16 | nt;
    Integer ref = (Integer)ifaceTable.get(hash);
    if (ref != null) return ref.intValue();
    
    buf.u1(Jvm.CONSTANT_InterfaceMethodref);
    buf.u2(cls);
    buf.u2(nt);
    
    count++;
    ifaceTable.put(hash, new Integer(count));
    return count;
  }

////////////////////////////////////////////////////////////////
// Convenience
////////////////////////////////////////////////////////////////  

  public int cls(Class c)
  {
    return cls( c.getName().replace('.', '/') );
  }

  public int nt(int name, String type)
  {
    return nt( name, utf(type) );
  }

  public int nt(String name, String type)
  {
    return nt( utf(name), utf(type) );
  }

  public int field(String cls, String name, String type)
  {
    return field(cls(cls), nt(name, type));
  }

  public int method(int cls, int name, String type)
  {
    return method(cls, nt(name, type));
  }

  public int method(int cls, String name, String type)
  {
    return method(cls, nt(name, type));
  }

  public int method(String cls, String name, String type)
  {
    return method(cls(cls), nt(name, type));
  }

  public int method(Method m)
  {
    int cls = cls(m.getDeclaringClass());
    int name = utf(m.getName());
    int type = utf(Jvm.methodDescriptor(m.getParameterTypes(), m.getReturnType()));
    return method(cls, nt(name, type));
  }

  public int iface(int cls, String name, String type)
  {
    return iface(cls, nt(name, type));
  }

  public int field(int cls, int name, String type)
  {
    return field(cls, nt(name, type));
  }

  public int field(int cls, String name, String type)
  {
    return field(cls, nt(name, type));
  }
  
  public int field(FieldInfo fi)
  {
    return field(fi.asm.thisClass, nt(fi.name, fi.type));
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  Buffer buf = new Buffer();
  int count = 0;
  
  Hashtable utfTable     = new Hashtable();
  Hashtable classTable   = new Hashtable();
  Hashtable stringTable  = new Hashtable();
  Hashtable floatTable  = new Hashtable();
  Hashtable doubleTable  = new Hashtable();
  Hashtable longTable  = new Hashtable();
  IntHashMap integerTable = new IntHashMap();
  IntHashMap ntTable      = new IntHashMap();
  IntHashMap fieldTable   = new IntHashMap();
  IntHashMap methodTable  = new IntHashMap();
  IntHashMap ifaceTable   = new IntHashMap();
}
