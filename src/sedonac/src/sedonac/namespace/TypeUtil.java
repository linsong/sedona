//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 07  Brian Frank  Creation
//

package sedonac.namespace;

import sedona.Buf;
import sedona.Env;
import sedona.Facets;
import sedona.util.TextUtil;
import sedonac.Location;
import sedonac.ast.*;
import sedonac.ir.IrType;

/**
 * TypeUtil deals with the fact that using Java interfaces suck.
 */
public class TypeUtil
{

  /**
   * Return a is an instance of b.
   */
  public static boolean is(Type a, Type b)
  {
    // if the same this is easy
    if (a == b) return true;

    // treat all integers the same
    if (a.isInteger() && b.isInteger()) return true;

    // if either if arrays
    if (a.isArray() || b.isArray())
    {
      // if only one is an array
      if (!a.isArray()) return false;
      if (!b.isArray()) return b.isObj();

      // if a[x] is b[x]
      Type aof = a.arrayOf();
      Type bof = b.arrayOf();
      if (!aof.is(bof)) return false;

      // array of primitives must be exact match
      // for example byte[] is not a int[]
      if (aof.isPrimitive() && aof != bof) return false;

      // const and non-const are non-compatible since const
      // stores block indexes and non-const stores pointers
      if (a.isConst() != b.isConst()) return false;

      // check bounds, if b is bounded,
      // then a must have the same bounds
      ArrayType.Len alen = a.arrayLength();
      ArrayType.Len blen = b.arrayLength();
      if (blen == null) return true;
      if (alen == null) return false;
      return alen.equals(blen);
    }

    // check signatures
    if (a.signature().equals(b.signature())) return true;

    // check inheritance chain
    Type base = a.base();
    while (base != null)
    {
      if (is(base, b)) return true;
      base = base.base();
    }

    return false;
  }

  /**
   * Return a is the same type as b.
   */
  public static boolean equals(Type a, Object o)
  {
    if (a == o) return true;
    if (o instanceof Type)
    {
      Type b = (Type)o;
      return a.signature().equals(b.signature());
    }
    return false;
  }

  /**
   * Is the component a subclass of Component.
   */
  public static boolean isaComponent(Type t)
  {
    if (t.isComponent()) return true;
    if (t.base() == null) return false;
    return isaComponent(t.base());
  }

  /**
   * Is the component a subclass of Virtual.
   */
  public static boolean isaVirtual(Type t)
  {
    if (t.isVirtual()) return true;
    if (t.base() == null) return false;
    return isaVirtual(t.base());
  }

  /**
   * Is the component a subclass of Test.
   */
  public static boolean isaTest(Type t)
  {
    if (t.qname().equals("sys::Test")) return true;
    if (t.base() == null) return false;
    return isaTest(t.base());
  }

  /**
   * If the specified qname is a predefined type,
   * then return it's id, otherwise return -1.
   */
  public static int predefinedId(String qname)
  {
    if (qname.equals("sys::Buf")) return Type.bufId;
    return -1;
  }

  /**
   * Is the type reflective (component or predefined).
   */
  public static boolean isReflective(Type t)
  {
    if (t.isaComponent()) return true;
    if (t.isPrimitive()) return true;
    if (predefinedId(t.qname()) > 0) return true;
    return false;
  }

  /**
   * Is the slot inherited into subclasses.
   */
  public static boolean isInherited(Slot slot, Type into)
  {
    if (slot.isPrivate()) return false;
    if (slot.isInternal())
    {
      String kit1 = slot.parent().kit().name();
      String kit2 = into.kit().name();
      if (!kit1.equals(kit2)) return false;
    }

    if (slot.isMethod())
    {
      Method m = (Method)slot;
      if (m.isInstanceInit() || m.isStaticInit())
        return false;
    }

    return true;
  }

  /**
   * Get the specified type instance as an IrType.
   */
  public static IrType ir(Type t)
  {                       
    if (t == null) return null;
                            
    if (t instanceof IrType) return (IrType)t;
    
    if (t instanceof TypeDef)
    {
      IrType ir = ((TypeDef)t).ir;
      if (ir == null) throw new IllegalStateException("TypeDef not assembled");
      return ir; 
    }                                                                          
    
    throw new IllegalStateException("Cannot map to IrType: " + t.getClass().getName());
  }

  /**
   * If the specified type contains an inline
   * unsized array field, then return it.
   */
  public static Field getUnsizedArrayField(Type t)
  {
    Slot[] slots = t.slots();
    for (int i=0; i<slots.length; ++i)
    {
      if (slots[i] instanceof Field)
      {
        Field f = (Field)slots[i];
        if (f.ctorLengthParam() > 0)
          return f;
      }
    }
    return null;
  }

  /**
   * Map a field qname to a log qname.
   */        
  public static String toLogName(Field f)
  {      
    String n = f.name();                             
    String q = f.qname();
    if (n.equals("log")) 
      return q.substring(0, q.length()-".log".length());
      
    if (n.length() >= 4 && n.endsWith("Log")) 
      return q.substring(0, q.length()-"Log".length());
    
    return q;
  }

  /**
   * Given a type, figure out which print method should
   * be used.
   */        
  public static Method toPrintMethod(Namespace ns, Type t)
  {
    if (t.isStr())     return ns.resolveMethod("sys::OutStream.print", true);
    if (t.isBool())    return ns.resolveMethod("sys::OutStream.printBool", true);
    if (t.isInteger()) return ns.resolveMethod("sys::OutStream.printInt", true);
    if (t.isLong())    return ns.resolveMethod("sys::OutStream.printLong", true);
    if (t.isFloat())   return ns.resolveMethod("sys::OutStream.printFloat", true);
    if (t.isDouble())  return ns.resolveMethod("sys::OutStream.printDouble", true);
    return null;
  }                  
  
  /**
   * Return if the testonly facet is set to true.
   */
  public static boolean isTestOnly(Type t)
  {
    return t.facets().getb("testonly", false);
  }

  /**
   * Map slot flags to a String.
   */
  public static String flagsToString(Type t)
  {
    StringBuffer s = new StringBuffer();

    if (t.isAbstract()) s.append("abstract ");
    if (t.isConst())    s.append("const ");
    if (t.isFinal())    s.append("final ");
    if (t.isInternal()) s.append("internal ");

    return s.toString();
  }

  /**
   * Map slot flags to a String.
   */
  public static String flagsToString(Slot slot)
  {
    StringBuffer s = new StringBuffer();

    if (!slot.isAction() && !slot.isProperty())
    {
      if (slot.isInternal())  s.append("internal ");
      if (slot.isProtected()) s.append("protected ");
      if (slot.isPrivate())   s.append("private ");
      if (slot.isPublic())    s.append("public ");
    }

    if (slot.isAbstract()) s.append("abstract ");
    else if (slot.isVirtual() && !slot.isAction()) s.append("virtual ");

    if (slot.isDefine()) s.append("define ");
    else if (slot.isStatic()) s.append("static ");

    if (slot.isProperty()) s.append("property ");
    else if (slot.isInline()) s.append("inline ");

    if (slot.isConst())    s.append("const ");
    if (slot.isAction())   s.append("action ");
    if (slot.isNative())   s.append("native ");
    if (slot.isOverride()) s.append("override ");

    return s.toString();
  }

  /**
   * Map a slot and facets to runtime flags.
   */
  public static int rtFlags(Slot slot, Facets facets)
  {
    int flags = 0;
    if (slot instanceof Method)  flags |= Slot.RT_ACTION;
    if (facets.getb("config"))   flags |= Slot.RT_CONFIG;
    if (facets.getb("asStr"))    flags |= Slot.RT_AS_STR;
    if (facets.getb("operator")) flags |= Slot.RT_OPERATOR;
    return flags;
  }

  /**
   * Get the number of parameters which takes into 
   * account the implicit this and wide parameters.
   */
  public static int numParams(Method m)
  {              
    Type[] params = m.paramTypes();
    int numParams = 0;
    for (int i=0; i<params.length; ++i)
      numParams += params[i].isWide() ? 2 : 1;
    if (!m.isStatic()) numParams++;
    return numParams;
  }            
  
  /**
   * Map a literal object to its code representation:
   *   - null
   *   - Boolean
   *   - Integer
   *   - Long
   *   - Float
   *   - Double
   *   - String 
   *   - Buf
   *   - Type
   *   - Slot
   */          
  public static String toCodeString(Object v)
  {      
    if (v == null)
      throw new IllegalStateException("Unexpected null value");
      
    if (v instanceof Boolean) 
      return v.toString();
      
    if (v instanceof Integer) 
      return v.toString();
      
    if (v instanceof Long)    
      return ((Long)v).longValue() == Long.MIN_VALUE ? 
             "0x8000_0000_0000_0000L" : v.toString() + "L";

    if (v instanceof Float)             
      return Env.floatFormat(((Float)v).floatValue()) + "F";

    if (v instanceof Double)             
      return Env.doubleFormat(((Double)v).doubleValue()) + "D";

    if (v instanceof String)             
      return '"' + TextUtil.toLiteral(v.toString()) + '"';

    if (v instanceof Buf)             
      return ((Buf)v).toString();

    if (v instanceof Type)             
      return ((Type)v).qname();

    if (v instanceof Slot)             
      return ((Slot)v).qname();

    if (v instanceof Object[])
    {
      Object[] array = (Object[])v;
      if (array.length == 0) return "{,}";
      StringBuffer s = new StringBuffer();
      s.append("{");
      for (int i=0; i<array.length; ++i)
      {
        if (i > 0) s.append(",");
        s.append(toCodeString(array[i]));
      }
      s.append("}");
      return s.toString();
    }
    
    throw new IllegalStateException("Unexpected literal type: " + v.getClass().getName());
  }                   
  
  /**
   * Get the value as a code string, but make it safe such that it
   * guaranteed no exception is thrown (for code reporting).
   */
  public static String toCodeStringSafe(Object v)
  {                                         
    try { return toCodeString(v); } catch (Exception e) {}
    try { String.valueOf(v); } catch (Exception e) {}
    return "???";
  }

}
