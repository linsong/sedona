//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Mar 07  Brian Frank  Creation
//

package sedonac.ir;

import sedona.Buf;
import sedona.util.*;
import sedonac.*;
import sedonac.scode.*;
import sedonac.namespace.*;

/**
 * IrOp
 */
public class IrOp
{

//////////////////////////////////////////////////////////////////////////
// Constructors
//////////////////////////////////////////////////////////////////////////

  public IrOp(int opcode, String arg)
  {
    this.opcode = opcode;
    this.arg = arg;
  }

  public IrOp(int opcode, Type arg)
  {
    this.opcode = opcode;
    this.arg = arg.qname();
    this.resolvedArg = arg;
  }
  
  public IrOp(int opcode, Slot arg)
  {
    this.opcode = opcode;
    this.arg = arg.qname();
    this.resolvedArg = arg;
  }

  public IrOp(int opcode, int arg)
  {                     
    this.opcode = opcode;
    this.arg = String.valueOf(arg);
  }

  public IrOp(int opcode)
  {
    this.opcode = opcode;
  }

  public IrOp()
  {
  }

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  public int argType()
  {
    return SCode.argType(opcode);
  }

  public boolean isJump()
  {
    return argType() == SCode.jmpArg || argType() == SCode.jmpfarArg;
  }

  public boolean isFieldOp()
  {
    return argType() == SCode.fieldArg;
  }

  public int argToInt()
  {
    return Integer.parseInt(arg);
  }

  public long argToLong()
  {                     
    if (!arg.endsWith("L"))
      throw new IllegalStateException(this + " doesn't end with L");
    return Long.parseLong(arg.substring(0, arg.length()-1));
  }

  public float argToFloat()
  {
    if (arg.equals("null")) return Float.NaN;
    if (!arg.endsWith("F"))
      throw new IllegalStateException(this + " doesn't end with F");
    return Float.parseFloat(arg.substring(0, arg.length()-1));
  }

  public double argToDouble()
  {                        
    if (arg.equals("null")) return Double.NaN;
    if (!arg.endsWith("D"))
      throw new IllegalStateException(this + " doesn't end with D");
    return Double.parseDouble(arg.substring(0, arg.length()-1));
  }

  public String argToStr()
  {                                
    if (!arg.startsWith("\""))
      throw new IllegalStateException(this + " isn't quoted");
    return TextUtil.fromLiteral(arg.substring(1, arg.length()-1));
  }

  public Buf argToBuf()
  {            
    return Buf.fromString(arg);                    
  }

  public Type argToType()
  {
    return (Type)resolvedArg;
  }

  public IrType argToIrType()
  {
    return (IrType)resolvedArg;
  }

  public Slot argToSlot()
  {
    return (Slot)resolvedArg;
  }

  public IrSlot argToIrSlot()
  {
    return (IrSlot)resolvedArg;
  }

  public IrField argToField()
  {
    return (IrField)resolvedArg;
  }

  public IrMethod argToMethod()
  {
    return (IrMethod)resolvedArg;
  }
  
  public String opcodeName()
  {
    return SCode.name(opcode);
  }

  public String toString()
  {
    String s = opcodeName();
    if (arg != null) s += " " + arg;
    return s;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  // used to indicate instructions associated with assigning
  // to a static field - in Sedona we have an implicit static
  // base address which Java doesn't have
  public static final int STATIC_FIELD = 0x01;   
  
  // used to indicate instructions which should be 
  // ignored by the Java assembler
  public static final int IGNORE_JAVA = 0x02;
  
  public int index;           // logical index in IR list of opcodes
  public int opcode;          // opcode id
  public String arg;          // argument in IR String format
  public Object resolvedArg;  // if arg resolves to Type, IrSlot
  public int pos;             // position from beginning of bytecode/scode image
  public Location loc;        // available during assembly from AST
  public Type type;           // type carried thru during assembly from AST
  public int flags;           // bitmask to indicate special cases

}
