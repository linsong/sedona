//
// Copyright (c) 2000 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 00  Brian Frank  Creation
//
package sedonac.jasm;

import java.lang.reflect.*;

/**
 * Code
 */
public class Code
  extends AttributeInfo
{  

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  public Code(Assembler asm)
  {
    super(asm, Jvm.ATTR_CODE);
    this.cp = asm.cp;
  }

////////////////////////////////////////////////////////////////
// Add
////////////////////////////////////////////////////////////////
  
  public int add(int opcode)
  {
    if (Jvm.OPCODE_ARGS[opcode] != Jvm.NONE) 
      throw new IllegalStateException("Opcode requires arguments: " + opcode);
    return code.u1(opcode);
  }

  public int add(int opcode, int arg)
  {
    int ref = code.count;
     
    // auto widen LDC opcode if necessary
    if (opcode == Jvm.LDC)
    {
      if (arg < 255) { code.u1(Jvm.LDC); code.u1(arg); }
      else { code.u1(Jvm.LDC_W); code.u2(arg); }
      return ref;
    }
    
    int argType = Jvm.OPCODE_ARGS[opcode];
    code.u1(opcode);
    if (argType == Jvm.U1) code.u1(arg);
    else if (argType == Jvm.U2 || argType == Jvm.B2) code.u2(arg);
    else throw new IllegalStateException("Opcode does not take u1 or u2 args: " + opcode);
    return ref;
  }
    
  public int addIntConst(int v)
  {
    // if the constant is between -1 to 5,
    // we can use the iconst_x operations
    // for maximum efficiency
    switch(v)
    {
      case -1: return add( Jvm.ICONST_M1 );
      case 0:  return add( Jvm.ICONST_0 );
      case 1:  return add( Jvm.ICONST_1 );
      case 2:  return add( Jvm.ICONST_2 );
      case 3:  return add( Jvm.ICONST_3 );
      case 4:  return add( Jvm.ICONST_4 );
      case 5:  return add( Jvm.ICONST_5 );
    }

    // if the constant fits in a byte use bipush
    if (Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
    {
      return add( Jvm.BIPUSH, v );
    }

    // if the constant fits in two bytes use sipush
    if (Short.MIN_VALUE <= v && v <= Short.MAX_VALUE)
    {
      return add( Jvm.SIPUSH, v );
    }

    // else we have to load from the constant pool
    return add( Jvm.LDC, cp.intConst(v) );
  }

  public int addLongConst(long v)
  {
    if (v == 0L) return add(Jvm.LCONST_0);             
    if (v == 1L) return add(Jvm.LCONST_1);             
    return add(Jvm.LDC2_W, cp.longConst(v));
  }

  public int addFloatConst(float v)
  {                             
    if (v == 0.0f) return add(Jvm.FCONST_0);             
    if (v == 1.0f) return add(Jvm.FCONST_1);             
    return add(Jvm.LDC, cp.floatConst(v));
  }

  public int addDoubleConst(double v)
  {                
    if (v == 0.0) return add(Jvm.DCONST_0);             
    if (v == 1.0) return add(Jvm.DCONST_1);             
    return add(Jvm.LDC2_W, cp.doubleConst(v));
  }
  
  public int addPad(int opcode)
  {
    int ref = code.u1(opcode);
    int pad = 3 - ref % 4;
    for(int i=0; i<pad; ++i) code.u1(0);
    return ref;
  }                               

  
////////////////////////////////////////////////////////////////
// Load
////////////////////////////////////////////////////////////////

  public void aload(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.ALOAD_0); break;
      case 1:  add(Jvm.ALOAD_1); break;
      case 2:  add(Jvm.ALOAD_2); break;
      case 3:  add(Jvm.ALOAD_3); break;
      default: add(Jvm.ALOAD, reg); break;
    }
  }

  public void iload(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.ILOAD_0); break;
      case 1:  add(Jvm.ILOAD_1); break;
      case 2:  add(Jvm.ILOAD_2); break;
      case 3:  add(Jvm.ILOAD_3); break;
      default: add(Jvm.ILOAD, reg); break;
    }
  }

  public void lload(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.LLOAD_0); break;
      case 1:  add(Jvm.LLOAD_1); break;
      case 2:  add(Jvm.LLOAD_2); break;
      case 3:  add(Jvm.LLOAD_3); break;
      default: add(Jvm.LLOAD, reg); break;
    }
  }

  public void fload(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.FLOAD_0); break;
      case 1:  add(Jvm.FLOAD_1); break;
      case 2:  add(Jvm.FLOAD_2); break;
      case 3:  add(Jvm.FLOAD_3); break;
      default: add(Jvm.FLOAD, reg); break;
    }
  }

  public void dload(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.DLOAD_0); break;
      case 1:  add(Jvm.DLOAD_1); break;
      case 2:  add(Jvm.DLOAD_2); break;
      case 3:  add(Jvm.DLOAD_3); break;
      default: add(Jvm.DLOAD, reg); break;
    }
  }
  
////////////////////////////////////////////////////////////////
// Store
////////////////////////////////////////////////////////////////

  public void astore(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.ASTORE_0); break;
      case 1:  add(Jvm.ASTORE_1); break;
      case 2:  add(Jvm.ASTORE_2); break;
      case 3:  add(Jvm.ASTORE_3); break;
      default: add(Jvm.ASTORE, reg); break;
    }
  }

  public void istore(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.ISTORE_0); break;
      case 1:  add(Jvm.ISTORE_1); break;
      case 2:  add(Jvm.ISTORE_2); break;
      case 3:  add(Jvm.ISTORE_3); break;
      default: add(Jvm.ISTORE, reg); break;
    }
  }

  public void lstore(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.LSTORE_0); break;
      case 1:  add(Jvm.LSTORE_1); break;
      case 2:  add(Jvm.LSTORE_2); break;
      case 3:  add(Jvm.LSTORE_3); break;
      default: add(Jvm.LSTORE, reg); break;
    }
  }

  public void fstore(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.FSTORE_0); break;
      case 1:  add(Jvm.FSTORE_1); break;
      case 2:  add(Jvm.FSTORE_2); break;
      case 3:  add(Jvm.FSTORE_3); break;
      default: add(Jvm.FSTORE, reg); break;
    }
  }

  public void dstore(int reg)
  {
    switch (reg)
    {
      case 0:  add(Jvm.DSTORE_0); break;
      case 1:  add(Jvm.DSTORE_1); break;
      case 2:  add(Jvm.DSTORE_2); break;
      case 3:  add(Jvm.DSTORE_3); break;
      default: add(Jvm.DSTORE, reg); break;
    }
  }


////////////////////////////////////////////////////////////////
// Branching
////////////////////////////////////////////////////////////////

  public int branch(int opcode) 
  { 
    return add(opcode, 0xffff) + 1;
  }

  public int branch(int opcode, int dest) 
  { 
    return add(opcode, dest-code.count);
  }

  public void patch(int codeOffset) 
  {                       
    patch(codeOffset, code.count);
  }

  public void patch(int codeOffset, int location) 
  {        
    code.u2(codeOffset, location-codeOffset+1);          
  }              
  
////////////////////////////////////////////////////////////////
// Invoke Utils
////////////////////////////////////////////////////////////////

  public int invoke(Method m)
  {
    int method = cp.method(m);
    int flags = m.getModifiers();
    
    if (Modifier.isInterface(flags)) 
      return invokeInterface(method, m.getParameterTypes().length+1);
    else if (Modifier.isStatic(flags)) 
      return add(Jvm.INVOKESTATIC, method);
    else 
      return add(Jvm.INVOKEVIRTUAL, method);
  }

  public int invokeInterface(int method, int nargs)
  {
    int ref = code.count;
    code.u1(Jvm.INVOKEINTERFACE);
    code.u2(method);
    code.u1(nargs);
    code.u1(0);
    return ref;
  }

////////////////////////////////////////////////////////////////
// Compiler
////////////////////////////////////////////////////////////////

  void compile(Buffer buf)
  {                      
    if (maxStack < 0 || maxLocals < 0)
      throw new IllegalStateException("maxStack or maxLocals not set");
      
    buf.u2(name);          // attribute name        
    int len = buf.u4(-1);  // attribute length (to backpatch)
    buf.u2(maxStack);      // max stack
    buf.u2(maxLocals);     // max locals
    buf.u4(code.count);    // code length
    buf.append(code);      // code
    buf.u2(0);             // exceptions not supported
    buf.u2(attributes.length);
    for (int i=0; i<attributes.length; ++i)
      attributes[i].compile(buf); 
    buf.u4(len, buf.count-len-4); // backpatch length (doesn't include name/length)
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public final ConstantPool cp;
  public int maxStack = -1;
  public int maxLocals = -1;
  public Buffer code = new Buffer(512); 
  public AttributeInfo[] attributes = new AttributeInfo[0];
  
}
