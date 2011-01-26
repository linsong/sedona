//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 Mar 07  Brian Frank  Creation
//

package sedonac.gen;

import java.util.*;

import sedona.Buf;
import sedonac.Compiler;
import sedonac.CompilerSupport;
import sedonac.ast.Expr;
import sedonac.ir.IrAddressable;
import sedonac.namespace.Slot;
import sedonac.namespace.Type;

/**
 * ConstPool manages the pool of addressable constants:
 *   - string literals
 *   - time literals
 *   - type qnames (references string literals)
 *   - slot qnames (references type qname and string literals)
 */
public class ConstPool
  extends CompilerSupport
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public ConstPool(ImageGen parent)
  {
    super(parent.compiler);
    this.parent = parent;
  }

//////////////////////////////////////////////////////////////////////////
// Integers
//////////////////////////////////////////////////////////////////////////

  IrInt toInt(Integer v)
  {
    IrInt x = (IrInt)ints.get(v);
    if (x == null)
    {
      x = new IrInt(v);
      ints.put(v, x);
    }
    return x;
  }

  void ints()
  {                   
    // pad to 4 bytes to align 32-bit values
    Buf code = parent.code;
    code.align(4);

    // sanity check to ensure we are both aligned on a
    // block index and on an 4-byte boundary
    if (code.size % parent.image.blockSize != 0) throw new IllegalStateException();
    if (code.size % 4 != 0) throw new IllegalStateException();
  
    Integer[] keys = (Integer[])ints.keySet().toArray(new Integer[ints.size()]);
    Arrays.sort(keys);
    for (int i=0; i<keys.length; ++i)
    {
      IrInt x = (IrInt)ints.get(keys[i]);
      x.blockIndex = blockIndex();
      int val = x.val.intValue();
      // System.out.println("-- Int [" + x.blockIndex + "] " + val);
      code.i4(val);
      blockAlign();
    }
  }

  static class IrInt
    implements IrAddressable
  {
    IrInt(Integer val) { this.val = val; }

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int i) { blockIndex = i; }

    public boolean alignBlockIndex() { return true; }

    final Integer val;
    int blockIndex;
  }

//////////////////////////////////////////////////////////////////////////
// Longs
//////////////////////////////////////////////////////////////////////////

  IrLong toLong(Long v)
  {
    IrLong x = (IrLong)longs.get(v);
    if (x == null)
    {
      x = new IrLong(v);
      longs.put(v, x);
    }
    return x;
  }

  void longs()
  {                   
    // pad to 8 bytes to align 64-bit values
    Buf code = parent.code;
    code.align(8);

    // sanity check to ensure we are both aligned on a
    // block index and on an 8-byte boundary
    if (code.size % parent.image.blockSize != 0) throw new IllegalStateException();
    if (code.size % 8 != 0) throw new IllegalStateException();
  
    Long[] keys = (Long[])longs.keySet().toArray(new Long[longs.size()]);
    Arrays.sort(keys);
    for (int i=0; i<keys.length; ++i)
    {
      IrLong x = (IrLong)longs.get(keys[i]);
      x.blockIndex = blockIndex();
      long val = x.val.longValue();
      // System.out.println("-- Long [" + x.blockIndex + "] " + val);
      code.i8(val);
      blockAlign();
    }
  }

  static class IrLong
    implements IrAddressable
  {
    IrLong(Long val) { this.val = val; }

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int i) { blockIndex = i; }

    public boolean alignBlockIndex() { return true; }

    final Long val;
    int blockIndex;
  }

//////////////////////////////////////////////////////////////////////////
// Floats
//////////////////////////////////////////////////////////////////////////

  IrFloat toFloat(Float v)
  {
    IrFloat x = (IrFloat)floats.get(v);
    if (x == null)
    {
      x = new IrFloat(v);
      floats.put(v, x);
    }
    return x;
  }

  void floats()
  {
    // pad to 4 bytes to align 32-bit floats
    Buf code = parent.code;
    code.align(4);

    // sanity check to ensure we are both aligned on a
    // block index and on an 4-byte boundary
    if (code.size % parent.image.blockSize != 0) throw new IllegalStateException();
    if (code.size % 4 != 0) throw new IllegalStateException();
    
    Float[] keys = (Float[])floats.keySet().toArray(new Float[floats.size()]);
    Arrays.sort(keys);
    for (int i=0; i<keys.length; ++i)
    {
      IrFloat x = (IrFloat)floats.get(keys[i]);
      x.blockIndex = blockIndex();
      float val = x.val.floatValue();
      // System.out.println("-- Float [" + x.blockIndex + "] " + val);
      code.f4(val);
      blockAlign();
    }
  }

  static class IrFloat
    implements IrAddressable
  {
    IrFloat(Float val) { this.val = val; }

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int i) { blockIndex = i; }

    public boolean alignBlockIndex() { return true; }

    final Float val;
    int blockIndex;
  }

//////////////////////////////////////////////////////////////////////////
// Doubles
//////////////////////////////////////////////////////////////////////////

  IrDouble toDouble(Double v)
  {
    IrDouble x = (IrDouble)doubles.get(v);
    if (x == null)
    {
      x = new IrDouble(v);
      doubles.put(v, x);
    }
    return x;
  }

  void doubles()
  {                          
    // pad to 8 bytes to align 64-bit doubles
    Buf code = parent.code;
    code.align(8);

    // sanity check to ensure we are both aligned on a
    // block index and on an 8-byte boundary
    if (code.size % parent.image.blockSize != 0) throw new IllegalStateException();
    if (code.size % 8 != 0) throw new IllegalStateException();
  
    Double[] keys = (Double[])doubles.keySet().toArray(new Double[doubles.size()]);
    Arrays.sort(keys);
    for (int i=0; i<keys.length; ++i)
    {
      IrDouble x = (IrDouble)doubles.get(keys[i]);
      x.blockIndex = blockIndex();
      double val = x.val.doubleValue();
      // System.out.println("-- Double [" + x.blockIndex + "] " + parent.image.armDouble + "  " + val);
      if (parent.image.armDouble)
      {      
        // ARM chips layout 64-bit doubles with byte level 
        // little endian and 32-bit word level big endian
        long bits = java.lang.Double.doubleToLongBits(val);
        code.i4((int)((bits >> 32) & 0xffffffffL));
        code.i4((int)(bits & 0xffffffffL));
      }
      else
      {
        code.f8(val);
      }
      blockAlign();
    }
  }

  static class IrDouble
    implements IrAddressable
  {
    IrDouble(Double val) { this.val = val; }

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int i) { blockIndex = i; }

    public boolean alignBlockIndex() { return true; }

    final Double val;
    int blockIndex;
  }

//////////////////////////////////////////////////////////////////////////
// Strings
//////////////////////////////////////////////////////////////////////////

  IrStr string(String val)
  {
    IrStr s = (IrStr)strings.get(val);
    if (s == null)
    {
      s = new IrStr(val);
      strings.put(val, s);
    }
    return s;
  }

  void strings()
  {                            
    Buf code = parent.code;
    String[] keys = (String[])strings.keySet().toArray(new String[strings.size()]);
    Arrays.sort(keys);
    for (int i=0; i<keys.length; ++i)
    {
      IrStr str = (IrStr)strings.get(keys[i]);
      str.blockIndex = blockIndex();
      writeStrLiteral(code, str.val);
    }
  }

  void writeStrLiteral(Buf code, String val)
  {
    for (int i=0; i<val.length(); ++i)
    {
      int ch = val.charAt(i);
      if (ch > 0xff) throw err("Invalid string literal: " + val);
      code.u1(ch);
    }             
    code.u1(0);   // null terminator
    blockAlign();
  }

  static class IrStr
    implements IrAddressable
  {
    IrStr(String val) { this.val = val; }

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int i) { blockIndex = i; }

    public boolean alignBlockIndex() { return true; }

    final String val;
    int blockIndex;
  }                        
  
//////////////////////////////////////////////////////////////////////////
// Bufs
//////////////////////////////////////////////////////////////////////////

  IrBuf buf(Buf val)
  {
    IrBuf x = (IrBuf)bufs.get(val);
    if (x == null)
    {
      x = new IrBuf(val);
      bufs.put(val, x);
    }
    return x;
  }

  void bufs()
  {                      
    // pad to 2 bytes to align 16-bit size fields
    Buf code = parent.code;
    code.align(2);

    // sanity check to ensure we are both aligned on a
    // block index and on an 2-byte boundary
    if (code.size % parent.image.blockSize != 0) throw new IllegalStateException();
    if (code.size % 2 != 0) throw new IllegalStateException();
  
    Buf[] keys = (Buf[])bufs.keySet().toArray(new Buf[bufs.size()]);
    Arrays.sort(keys);
    for (int i=0; i<keys.length; ++i)
    {
      IrBuf buf = (IrBuf)bufs.get(keys[i]);
      buf.blockIndex = blockIndex();
      Buf val = buf.val;
      // System.out.println("-- [" + buf.blockIndex + "] \"" + val);
      code.u2(val.size);
      code.u2(val.size);
      code.append(val);
      code.align(2);  // leave padding 
      blockAlign();
    }
  }

  static class IrBuf
    implements IrAddressable
  {
    IrBuf(Buf val) { this.val = val; }

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int i) { blockIndex = i; }

    public boolean alignBlockIndex() { return true; }

    final Buf val;
    int blockIndex;
  }

//////////////////////////////////////////////////////////////////////////
// Arrays
//////////////////////////////////////////////////////////////////////////

  IrArray array(Expr.Literal literal)
  {                                 
    Type of = literal.type.arrayOf();
    Object[] array = literal.asArray();
    IrArray key = new IrArray(of, array);
    IrArray x = (IrArray)arrays.get(key);
    if (x == null)
    {
      x = key;
      arrays.put(key, key);
    }           
    return x;
  }

  void arrays()
  {    
    Buf code = parent.code;
    IrArray[] arrays = (IrArray[])this.arrays.values().toArray(new IrArray[this.arrays.size()]);
    for (int i=0; i<arrays.length; ++i)
    {     
      IrArray a = arrays[i];
      Type of = a.of;
      Object[] array = a.array;
      
      // 1. align to nearest block boundary or primitive boundary
      // 2. save block index
      // 3. write array values
      blockAlign(); 
      if (of.isStr())
      {
        // first write the string values and save their block indices
        int[] strIndices = new int[array.length];            
        for (int j=0; j<array.length; ++j)        
        {
          strIndices[j] = blockIndex();
          writeStrLiteral(code, (String)array[j]);
        }                        
        // now write the actual array of block indices
        code.align(2);
        a.blockIndex = blockIndex();
        for (int j=0; j<array.length; ++j)        
          code.u2(strIndices[j]);
      }
      else switch (of.id())
      {
        case Type.byteId:
          a.blockIndex = blockIndex();
          for (int j=0; j<array.length; ++j)
            code.u1(((Integer)array[j]).intValue());
          break;

        case Type.shortId:            
          code.align(2);
          a.blockIndex = blockIndex();          
          for (int j=0; j<array.length; ++j)
            code.u2(((Integer)array[j]).intValue());
          break;

        case Type.intId:            
          code.align(4);
          a.blockIndex = blockIndex();          
          for (int j=0; j<array.length; ++j)
            code.i4(((Integer)array[j]).intValue());
          break;

        case Type.longId:            
          code.align(8);
          a.blockIndex = blockIndex();          
          for (int j=0; j<array.length; ++j)
            code.i8(((Long)array[j]).longValue());
          break;

        case Type.floatId:            
          code.align(4);
          a.blockIndex = blockIndex();          
          for (int j=0; j<array.length; ++j)
            code.f4(((Float)array[j]).floatValue());
          break;

        case Type.doubleId:            
          code.align(8);
          a.blockIndex = blockIndex();                
          if (parent.image.armDouble)
          {
            for (int j=0; j<array.length; ++j)
            {
              double val = ((Double)array[j]).doubleValue();      
              // ARM chips layout 64-bit doubles with byte level 
              // little endian and 32-bit word level big endian
              long bits = java.lang.Double.doubleToLongBits(val);
              code.i4((int)((bits >> 32) & 0xffffffffL));
              code.i4((int)(bits & 0xffffffffL));
            }
          }
          else
          {
            for (int j=0; j<array.length; ++j)
              code.f8(((Double)array[j]).doubleValue());
          }
          break;

        default:      
           throw new IllegalStateException("array literal: " + of);            
      }
    }
    blockAlign();
  }

  static class IrArray
    implements IrAddressable
  {
    IrArray(Type of, Object[] array) 
    { 
      this.of = of; 
      this.array = array;
      this.hash = of.name().hashCode() ^ array.length;
    }      
    
    public int hashCode()
    {
     return hash;
    }             
    
    public boolean equals(Object that)
    {
      if (this == that)
        return true;
      if (that == null)
        return false;
      if (!(that instanceof IrArray))
        return false;
      IrArray x = (IrArray)that;
      if (!of.equals(x.of))
        return false;
      if (!Arrays.equals(array, x.array))
        return false;
      return true;
    }

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int i) { blockIndex = i; }

    public boolean alignBlockIndex() { return true; }

    final Type of;
    final Object[] array;
    final int hash;
    int blockIndex;    
  }

//////////////////////////////////////////////////////////////////////////
// QnameType
//////////////////////////////////////////////////////////////////////////

  IrQnameType qnameType(Type type)
  {
    String qname = type.qname();
    IrQnameType t = (IrQnameType)qnameTypes.get(qname);
    if (t == null)
    {
      t = new IrQnameType();
      t.kitName  = string(type.kit().name());
      t.typeName = string(type.name());
      qnameTypes.put(qname, t);
    }
    return t;
  }

  void qnameTypes()
  {
    Buf code = parent.code;
    code.align(2);
    Iterator it = qnameTypes.values().iterator();
    while (it.hasNext())
    {
      IrQnameType t = (IrQnameType)it.next();
      t.blockIndex = blockIndex();

      if (t.kitName.blockIndex <= 0) throw new IllegalStateException();

      // System.out.println("-- [" + t.blockIndex + "] " + t.kitName.val + "::" + t.typeName.val);

      code.u2(t.kitName.blockIndex);
      code.u2(t.typeName.blockIndex);
      blockAlign();
    }
  }

  static class IrQnameType
    implements IrAddressable
  {
    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int i) { blockIndex = i; }

    public boolean alignBlockIndex() { return true; }

    int blockIndex;
    IrStr kitName;
    IrStr typeName;
  }

//////////////////////////////////////////////////////////////////////////
// QnameSlot
//////////////////////////////////////////////////////////////////////////

  IrQnameSlot qnameSlot(Slot slot)
  {
    String qname = slot.qname();
    IrQnameSlot s = (IrQnameSlot)qnameSlots.get(qname);
    if (s == null)
    {
      s = new IrQnameSlot();
      s.qnameType = qnameType(slot.parent());
      s.slotName  = string(slot.name());
      qnameSlots.put(qname, s);
    }
    return s;
  }

  void qnameSlots()
  {
    Buf code = parent.code;
    code.align(2);
    Iterator it = qnameSlots.values().iterator();
    while (it.hasNext())
    {
      IrQnameSlot s = (IrQnameSlot)it.next();
      s.blockIndex = blockIndex();

      if (s.qnameType.blockIndex <= 0 || s.slotName.blockIndex <= 0)
        throw new IllegalStateException();

      // System.out.println("-- [" + s.blockIndex + "] " + s.slotName.val);

      code.u2(s.qnameType.blockIndex);
      code.u2(s.slotName.blockIndex);
      blockAlign();
    }
  }

  static class IrQnameSlot
    implements IrAddressable
  {
    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int i) { blockIndex = i; }

    public boolean alignBlockIndex() { return true; }

    int blockIndex;
    IrQnameType qnameType;
    IrStr slotName;
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  public int blockIndex() { return parent.blockIndex(); }

  public void blockAlign() { parent.blockAlign(); }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  ImageGen parent;
  HashMap ints       = new HashMap();
  HashMap longs      = new HashMap();
  HashMap floats     = new HashMap();
  HashMap doubles    = new HashMap();
  HashMap strings    = new HashMap();
  HashMap bufs       = new HashMap();
  HashMap arrays     = new HashMap();
  HashMap qnameTypes = new HashMap();
  HashMap qnameSlots = new HashMap();

}
