//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Oct 08  Brian Frank  Creation
//
package sedonac.jasm;

import sedona.util.*;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.namespace.*;
import sedonac.scode.*;

/**          
 * JavaCodeAsm generates the code segment of a method.
 */
public class JavaMethodAsm  
  implements OpCodes
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  JavaMethodAsm(JavaClassAsm parent, IrMethod ir)
  {                             
    this.kitAsm = parent.parent;
    this.parent = parent;
    this.ir     = ir;      
    this.code   = new Code(parent);         
    this.localOffset = computeLocalOffset(); 
    this.maxLocals = ir.maxLocals * 2 + 10;  // TODO
    this.maxStack  = ir.maxStack;
  }           

  int computeLocalOffset()
  {                                
    // figure out the 
    int offset = ir.isStatic() ? 0 : 1;
    for (int i=0; i<ir.params.length; ++i)
      offset += ir.params[i].isWide() ? 2 : 1;
    return offset;
  }
                
////////////////////////////////////////////////////////////////
// Assemble
////////////////////////////////////////////////////////////////

  Code assemble()
  {                                  
    if (ir.code == null) return null;

    IrOp[] ops = ir.code;
    prevOp = null;
    for (cur=0; cur<ops.length; ++cur)
    {
      assemble(ops[cur]);
      prevOp = ops[cur];
    }
    
    backpatch();
    
    code.maxLocals = maxLocals;
    code.maxStack  = maxStack;
    code.attributes = new AttributeInfo[] { toLineNumTable() };

    return code;
  }                   
  
  void onReturn()
  {                                      
    if (!ir.isInstanceInit()) return;
    if (ir.parent.isStr()) return;
    
    // if we have unsized class, then we need to init its array field 
    IrField[] fields = parent.ir.instanceFields();
    for (int i=0; i<fields.length; ++i) 
    {                                       
      final IrField field = fields[i];
      if (fields[i].ctorLengthParam >= 0)  
      {
        parent.assembleInitField(code, field, true, new Runnable()                              
        {  
          public void run() { code.add(ILOAD, field.ctorLengthParam); }
        });
      }
    }
  }
  
  void assemble(IrOp op)
  {                     
    op.pos = code.code.count;    
    if ((op.flags & IrOp.IGNORE_JAVA) != 0) return;   
    switch (op.opcode)
    {
      case SCode.Nop:        break;
      case SCode.LoadIM1:    op(ICONST_M1); break;
      case SCode.LoadI0:     op(ICONST_0); break;
      case SCode.LoadI1:     op(ICONST_1); break; 
      case SCode.LoadI2:     op(ICONST_2); break;
      case SCode.LoadI3:     op(ICONST_3); break;
      case SCode.LoadI4:     op(ICONST_4); break;
      case SCode.LoadI5:     op(ICONST_5); break;
      case SCode.LoadIntU1:  loadInt(op.argToInt()); break;
      case SCode.LoadIntU2:  loadInt(op.argToInt()); break;
      case SCode.LoadL0:     op(LCONST_0); break;
      case SCode.LoadL1:     op(LCONST_1); break;
      case SCode.LoadF0:     op(FCONST_0); break;
      case SCode.LoadF1:     op(FCONST_1); break;
      case SCode.LoadD0:     op(DCONST_0); break;
      case SCode.LoadD1:     op(DCONST_1); break;
      case SCode.LoadNull:   op(ACONST_NULL); break;
      case SCode.LoadNullBool:   op(ICONST_2); break;
      case SCode.LoadNullFloat:  loadFloat(Float.NaN); break;
      case SCode.LoadNullDouble: loadDouble(Double.NaN); break;
      case SCode.LoadInt:    loadInt(op.argToInt()); break;
      case SCode.LoadFloat:  loadFloat(op.argToFloat()); break;
      case SCode.LoadLong:   loadLong(op.argToLong()); break; 
      case SCode.LoadDouble: loadDouble(op.argToDouble()); break;
      case SCode.LoadStr:    kitAsm.loadStr(code, op.argToStr()); break;
      case SCode.LoadBuf:    kitAsm.loadBuf(code, op.argToBuf()); break;
      case SCode.LoadType:   kitAsm.loadType(code, op.argToType()); break;
      case SCode.LoadSlot:   kitAsm.loadSlot(code, op.argToSlot()); break;
      case SCode.LoadDefine: loadDefine(op); break;

      // load params
      case SCode.LoadParam0:     loadParam(0, op); break;
      case SCode.LoadParam1:     loadParam(1, op); break;
      case SCode.LoadParam2:     loadParam(2, op); break;
      case SCode.LoadParam3:     loadParam(3, op); break;
      case SCode.LoadParam:      loadParam(op.argToInt(), op); break;
      case SCode.LoadParamWide:  loadParam(op.argToInt(), op); break;
  
      // store params
      case SCode.StoreParam:     storeParam(op.argToInt(), op); break;
      case SCode.StoreParamWide: storeParam(op.argToInt(), op); break;

      // load locals
      case SCode.LoadLocal0:     loadLocal(0, op); break;
      case SCode.LoadLocal1:     loadLocal(1, op); break; 
      case SCode.LoadLocal2:     loadLocal(2, op); break; 
      case SCode.LoadLocal3:     loadLocal(3, op); break; 
      case SCode.LoadLocal4:     loadLocal(4, op); break; 
      case SCode.LoadLocal5:     loadLocal(5, op); break; 
      case SCode.LoadLocal6:     loadLocal(6, op); break; 
      case SCode.LoadLocal7:     loadLocal(7, op); break; 
      case SCode.LoadLocal:      loadLocal(op.argToInt(), op); break; 
      case SCode.LoadLocalWide:  loadLocal(op.argToInt(), op); break; 

      // store locals
      case SCode.StoreLocal0:    storeLocal(0, op); break;
      case SCode.StoreLocal1:    storeLocal(1, op); break;
      case SCode.StoreLocal2:    storeLocal(2, op); break;
      case SCode.StoreLocal3:    storeLocal(3, op); break;
      case SCode.StoreLocal4:    storeLocal(4, op); break;
      case SCode.StoreLocal5:    storeLocal(5, op); break;
      case SCode.StoreLocal6:    storeLocal(6, op); break;
      case SCode.StoreLocal7:    storeLocal(7, op); break;
      case SCode.StoreLocal:     storeLocal(op.argToInt(), op); break;
      case SCode.StoreLocalWide: storeLocal(op.argToInt(), op); break;

      // int compare
      case SCode.IntEq:          compare(IF_ICMPEQ); break;
      case SCode.IntNotEq:       compare(IF_ICMPNE); break;
      case SCode.IntGt:          compare(IF_ICMPGT); break; 
      case SCode.IntGtEq:        compare(IF_ICMPGE); break;
      case SCode.IntLt:          compare(IF_ICMPLT); break;
      case SCode.IntLtEq:        compare(IF_ICMPLE); break;

      // int math
      case SCode.IntMul:         op(IMUL); break;
      case SCode.IntDiv:         op(IDIV); break;
      case SCode.IntMod:         op(IREM); break;
      case SCode.IntAdd:         op(IADD); break;
      case SCode.IntSub:         op(ISUB); break;
      case SCode.IntOr:          op(IOR); break;
      case SCode.IntXor:         op(IXOR); break;
      case SCode.IntAnd:         op(IAND); break;
      case SCode.IntNot:         op(ICONST_M1); op(IXOR); break;
      case SCode.IntNeg:         op(INEG); break;
      case SCode.IntShiftL:      op(ISHL); break;
      case SCode.IntShiftR:      op(ISHR); break;
      case SCode.IntInc:         op(ICONST_1);  op(IADD); break;
      case SCode.IntDec:         op(ICONST_M1); op(IADD); break;

      // long compare
      case SCode.LongEq:         compare(IFEQ, LCMP); break;
      case SCode.LongNotEq:      compare(IFNE, LCMP); break;
      case SCode.LongGt:         compare(IFGT, LCMP); break;
      case SCode.LongGtEq:       compare(IFGE, LCMP); break;
      case SCode.LongLt:         compare(IFLT, LCMP); break;
      case SCode.LongLtEq:       compare(IFLE, LCMP); break;

      // long math
      case SCode.LongMul:        op(LMUL); break;
      case SCode.LongDiv:        op(LDIV); break;
      case SCode.LongMod:        op(LREM); break;
      case SCode.LongAdd:        op(LADD); break;
      case SCode.LongSub:        op(LSUB); break;
      case SCode.LongOr:         op(LOR); break;
      case SCode.LongXor:        op(LXOR); break;
      case SCode.LongAnd:        op(LAND); break;
      case SCode.LongNot:        loadLong(-1L); op(LXOR); break;
      case SCode.LongNeg:        op(LNEG); break;
      case SCode.LongShiftL:     op(LSHL); break;
      case SCode.LongShiftR:     op(LSHR); break;

      // float compare
      case SCode.FloatEq:        code.add(INVOKESTATIC, parent.cp.method("sedona/vm/VmUtil", "floatEQ", "(FF)Z")); break;
      case SCode.FloatNotEq:     code.add(INVOKESTATIC, parent.cp.method("sedona/vm/VmUtil", "floatNE", "(FF)Z")); break;
      case SCode.FloatGt:        compare(IFGT, FCMPG); break;
      case SCode.FloatGtEq:      compare(IFGE, FCMPG); break;
      case SCode.FloatLt:        compare(IFLT, FCMPG); break;
      case SCode.FloatLtEq:      compare(IFLE, FCMPG); break;

      // float math
      case SCode.FloatMul:       op(FMUL); break;
      case SCode.FloatDiv:       op(FDIV); break;
      case SCode.FloatAdd:       op(FADD); break;
      case SCode.FloatSub:       op(FSUB); break;
      case SCode.FloatNeg:       op(FNEG); break;

      // double compare
      case SCode.DoubleEq:       code.add(INVOKESTATIC, parent.cp.method("sedona/vm/VmUtil", "doubleEQ", "(DD)Z")); break;
      case SCode.DoubleNotEq:    code.add(INVOKESTATIC, parent.cp.method("sedona/vm/VmUtil", "doubleNE", "(DD)Z")); break;
      case SCode.DoubleGt:       compare(IFGT, DCMPG); break;
      case SCode.DoubleGtEq:     compare(IFGE, DCMPG); break;
      case SCode.DoubleLt:       compare(IFLT, DCMPG); break;
      case SCode.DoubleLtEq:     compare(IFLE, DCMPG); break;

      // double math
      case SCode.DoubleMul:      op(DMUL); break;
      case SCode.DoubleDiv:      op(DDIV); break;
      case SCode.DoubleAdd:      op(DADD); break;
      case SCode.DoubleSub:      op(DSUB); break;
      case SCode.DoubleNeg:      op(DNEG); break;

      // casts
      case SCode.IntToFloat:     op(I2F); break;
      case SCode.IntToLong:      op(I2L); break;
      case SCode.IntToDouble:    op(I2D); break;
      case SCode.LongToInt:      op(L2I); break;
      case SCode.LongToFloat:    op(L2F); break;
      case SCode.LongToDouble:   op(L2D); break;
      case SCode.FloatToInt:     op(F2I); break;
      case SCode.FloatToLong:    op(F2L); break;
      case SCode.FloatToDouble:  op(F2D); break;
      case SCode.DoubleToInt:    op(D2I); break;
      case SCode.DoubleToLong:   op(D2L); break;
      case SCode.DoubleToFloat:  op(D2F); break;

      // object compare
      case SCode.ObjEq:          compare(IF_ACMPEQ); break;
      case SCode.ObjNotEq:       compare(IF_ACMPNE); break;
      
      // general purpose compare
      case SCode.EqZero:         not(); break; // only used for logical-not before optimization
      case SCode.NotEqZero:      throw new IllegalStateException();

      // stack manipulation
      case SCode.Pop:            op(POP);  break;
      case SCode.Pop2:           op(POP2); break;
      case SCode.Pop3:           break;  // only used for foreach which uses local vars
      case SCode.Dup:            op(DUP);  break; 
      case SCode.Dup2:           op(DUP2); break; 
      case SCode.DupDown2:       dupDown(op);  break;
      case SCode.DupDown3:       dupDown(op);  break;
      case SCode.Dup2Down2:      dupDown(op); break;
      case SCode.Dup2Down3:      dupDown(op); break;

      // branching    
      case SCode.Jump:           jump(GOTO, op); break; 
      case SCode.JumpNonZero:    jump(IFNE, op); break;
      case SCode.JumpZero:       jump(IFEQ, op); break;
      case SCode.Foreach:        foreach(op); break;
      case SCode.JumpFar:        throw new IllegalStateException();
      case SCode.JumpFarNonZero: throw new IllegalStateException();
      case SCode.JumpFarZero:    throw new IllegalStateException();
      case SCode.ForeachFar:     throw new IllegalStateException();

      // storage
      case SCode.LoadDataAddr:  skipLoadDataAddr(); break;

      // 8 bit storage (bytes, bools)
      case SCode.Load8BitFieldU1:   loadField(op); break;
      case SCode.Load8BitArray:     loadArray(op); break;
      case SCode.Store8BitFieldU1:  storeField(op); break;
      case SCode.Store8BitArray:    storeArray(op); break;
      case SCode.Add8BitArray:      addArray(op); break;

      // 16 bit storage (shorts)
      case SCode.Load16BitFieldU1:  loadField(op); break;
      case SCode.Load16BitArray:    loadArray(op); break;
      case SCode.Store16BitFieldU1: storeField(op); break;
      case SCode.Store16BitArray:   storeArray(op); break;
      case SCode.Add16BitArray:     addArray(op); break;

      // 32 bit storage (int/float)
      case SCode.Load32BitFieldU1:  loadField(op); break;
      case SCode.Load32BitArray:    loadArray(op); break;
      case SCode.Store32BitFieldU1: storeField(op); break;
      case SCode.Store32BitArray:   storeArray(op); break;
      case SCode.Add32BitArray:     addArray(op); break;

      // 64 bit storage (long/double)
      case SCode.Load64BitFieldU1:  loadField(op); break;
      case SCode.Load64BitArray:    loadArray(op); break;
      case SCode.Store64BitFieldU1: storeField(op); break;
      case SCode.Store64BitArray:   storeArray(op); break;
      case SCode.Add64BitArray:     addArray(op); break;

      // ref storage (pointers - variable width)
      case SCode.LoadRefFieldU1:    loadField(op); break;
      case SCode.LoadRefArray:      loadArray(op); break;
      case SCode.StoreRefFieldU1:   storeField(op); break;
      case SCode.StoreRefArray:     storeArray(op); break;
      case SCode.AddRefArray:       addArray(op); break;

      // const storage (block index)
      case SCode.LoadConstFieldU1:  loadField(op); break;
      case SCode.LoadConstStatic:   loadField(op); break;
      case SCode.LoadConstArray:    loadArray(op); break;

      // inline storage (pointer addition)
      case SCode.LoadInlineFieldU1: loadField(op); break;
      
      // call control
      case SCode.Call:
      case SCode.CallVirtual:
      case SCode.CallNative:
      case SCode.CallNativeWide:
      case SCode.CallNativeVoid:   call(op); break;
      case SCode.ReturnVoid:      
      case SCode.ReturnPop:
      case SCode.ReturnPopWide:    returnOp(); break;
      
      // misc
      case SCode.InitArray:  code.add(POP2); code.add(POP); break;  // just pop args
      case SCode.InitVirt:   code.add(POP); break;  // just pop this
      case SCode.InitComp:   initComp(); break;
      case SCode.SizeOf:     code.addIntConst(-999); break;
      case SCode.Assert:     assertOp(op); break;
      case SCode.Switch:     switchOp(op); break;
      case SCode.MetaSlot:   break; // ignore
      case SCode.Cast:       cast(op); break;
      default: throw new IllegalStateException(op + " " + op.loc);
    }
  }

////////////////////////////////////////////////////////////////
// Constants
////////////////////////////////////////////////////////////////

  void loadInt(int val)
  {             
    code.addIntConst(val);
  }

  void loadLong(long val)
  {
    code.addLongConst(val);
  }

  void loadFloat(float val)
  {
    code.addFloatConst(val);
  } 

  void loadDouble(double val)
  {
    code.addDoubleConst(val);
  }

////////////////////////////////////////////////////////////////
// Load Param/Local
////////////////////////////////////////////////////////////////

  public void loadParam(int reg, IrOp op)
  {                              
    load(reg, jstackType(op));
  }  

  public void loadLocal(int reg, IrOp op)
  {
    load(reg+localOffset, jstackType(op));
  }  

  public void load(int reg, int stackType)
  {                       
    switch (stackType)
    {
      case OBJ:    code.aload(reg); break;
      case INT:    code.iload(reg); break;
      case LONG:   code.lload(reg); break;
      case FLOAT:  code.fload(reg); break;
      case DOUBLE: code.dload(reg); break;
      default: throw new IllegalStateException(""+(char)stackType);
    }
  }


////////////////////////////////////////////////////////////////
// Store Param/Local
////////////////////////////////////////////////////////////////

  public void storeParam(int reg, IrOp op)
  {                 
    store(reg, jstackType(op));
  }  

  public void storeLocal(int reg, IrOp op)
  {
    store(reg+localOffset, jstackType(op));
  }                 
  
  public void store(int reg, int stackType)
  {                       
    switch (stackType)
    {
      case OBJ:    code.astore(reg); break;
      case INT:    code.istore(reg); break;
      case LONG:   code.lstore(reg); break;
      case FLOAT:  code.fstore(reg); break;
      case DOUBLE: code.dstore(reg); break;
      default: throw new IllegalStateException(""+(char)stackType);
    }
  }                     

////////////////////////////////////////////////////////////////
// Compare and Branching
////////////////////////////////////////////////////////////////

  // TODO: we can peek at the next opcode and combine
  // this into a jump (or potentially do some of the optimizations
  // from the next step before this)

  void compare(int opcode) { compare(opcode, -1); }
  void compare(int opcode, int aux)
  {                        
    if (aux > 0) op(aux);
    int mark1 = branch(opcode);
    op(ICONST_0);
    int mark2 = branch(GOTO);
    patch(mark1);
    op(ICONST_1);
    patch(mark2);    
  }          
  
  void jump(int opcode, IrOp op)
  {
    branchTo(opcode, ir.code[op.argToInt()]);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  void loadDefine(IrOp op)
  {
    Field f = (Field)op.resolvedArg;      
    if (f == null) 
      throw new IllegalStateException("ERROR: must create IrOp with resolved field: " + op);    
          
    Expr.Literal literal = f.define();
    if (literal != null && !f.parent().qname().equals("sys::Sys")) 
    {            
      switch (literal.id)
      {
        case Expr.TRUE_LITERAL:   op(ICONST_1); return;
        case Expr.FALSE_LITERAL:  op(ICONST_0); return;
        case Expr.INT_LITERAL:    loadInt(literal.asInt()); return;
        case Expr.LONG_LITERAL:   loadLong(literal.asLong()); return;
        case Expr.FLOAT_LITERAL:  loadFloat(literal.asFloat()); return;
        case Expr.DOUBLE_LITERAL: loadDouble(literal.asDouble()); return;
        case Expr.TIME_LITERAL:   loadLong(literal.asLong()); return;
        case Expr.STR_LITERAL:    kitAsm.loadStr(code, literal.asString()); return;
        case Expr.NULL_LITERAL:   op(ACONST_NULL); return;
      }
    }         
        
    loadField(op);
  }

  void loadField(IrOp op)
  {                 
    Field f = (Field)op.resolvedArg;      
    if (f == null) 
      throw new IllegalStateException("ERROR: must create IrOp with resolved field: " + op);    

    // if this is a field on Str
    if (f.qname().equals("sys::Str.buf")) 
    {
      code.add(GETFIELD, code.cp.field("sedona/vm/StrRef", "buf", "[B"));
      return;
    }
      
    int fref = parent.fieldRef(f);
    if (f.isStatic())
      code.add(GETSTATIC, fref);
    else
      code.add(GETFIELD, fref);   
    
    // byte and short are unsigned, so we need to load them as such  
    if (f.type().isByte())  { code.addIntConst(0xff);   code.add(IAND); }
    if (f.type().isShort()) { code.addIntConst(0xffff); code.add(IAND); }
  }                                       

  void storeField(IrOp op)
  {      
    Field f = (Field)op.resolvedArg;      
    if (f == null) 
      throw new IllegalStateException("ERROR: must create IrOp with resolved field: " + op);    
      
    int fref = parent.fieldRef(f);
    if (f.isStatic())
      code.add(PUTSTATIC, fref);
    else
      code.add(PUTFIELD, fref);
  }                                       

////////////////////////////////////////////////////////////////
// Arrays
////////////////////////////////////////////////////////////////

  void loadArray(IrOp op)
  {           
    int stackType = jstackType(op, false);                                                         
    String name, sig;
    switch (stackType)
    { 
      case BOOL:   code.add(BALOAD); break; 
      case BYTE:   code.add(BALOAD); code.addIntConst(0xff); code.add(IAND); break; 
      case SHORT:  code.add(SALOAD); code.addIntConst(0xffff); code.add(IAND); break; 
      case INT:    code.add(IALOAD); break; 
      case LONG:   code.add(LALOAD); break; 
      case FLOAT:  code.add(FALOAD); break; 
      case DOUBLE: code.add(DALOAD); break; 
      case OBJ:    code.add(AALOAD); break; 
      default:
        throw new IllegalStateException("loadArray " + (char)stackType);
    }
  }

  void storeArray(IrOp op)
  {              
    int stackType = jstackType(op.type, false);                                                         
    String name, sig;
    switch (stackType)
    { 
      case BOOL:   code.add(BASTORE); break; 
      case BYTE:   code.add(BASTORE); break; 
      case SHORT:  code.add(SASTORE); break; 
      case INT:    code.add(IASTORE); break; 
      case LONG:   code.add(LASTORE); break; 
      case FLOAT:  code.add(FASTORE); break; 
      case DOUBLE: code.add(DASTORE); break; 
      case OBJ:    code.add(AASTORE); break; 
      default:
        throw new IllegalStateException("storeArray " + (char)stackType);
    }                                     
  }

  void addArray(IrOp op)
  {       
    throw new IllegalStateException();
  }       

////////////////////////////////////////////////////////////////
// Call
////////////////////////////////////////////////////////////////

  void call(IrOp op)
  { 
    // get resolved method                                            
    Method m = (Method)op.resolvedArg;      
    if (m == null) 
      throw new IllegalStateException("ERROR: must create IrOp with resolved method: " + op);    
    boolean isNative = JavaClassAsm.isJavaNative(m);

    // methods handled specially
    String qname = m.qname();
    if (qname.equals("sys::Sys.malloc")) { malloc(op); return; }
    
    // figure out java opcode
    int javaCall;                    
    if (isNative)
    {
      javaCall = INVOKESTATIC;
    }
    else if (op.opcode == SCode.CallVirtual)
    {
      javaCall = INVOKEVIRTUAL;
    }
    else if (m.isStatic())
    { 
      javaCall = INVOKESTATIC;
    }
    else if (ir.isStaticInit() && m.isInstanceInit())
    {
      // Always invokevirtual when calling a static field's _iInit() method.
      javaCall = INVOKEVIRTUAL;
    }
    else if (ir.isInstanceInit() && m.isInstanceInit())
    {
      // Condition: We are in the class's _iInit() method and we are
      // calling _iInit() on either: 
      // 1) ourselves (effectively super._iInit()). We know this is the
      //    case because the previous opcode will be LoadParam0.
      //    In this case we need to use java opcode INVOKESPECIAL.
      // 2) a non-static, inline field. In this case we must use INVOKEVIRTUAL.
      //
      // NOTE: It is is a Sedona compiler error to have non-static, inline
      // fields of the same type as the declaring class.
      javaCall = (prevOp.opcode == SCode.LoadParam0) ? INVOKESPECIAL : INVOKEVIRTUAL;
    }
    else
    {     
      javaCall = INVOKEVIRTUAL;
      // see if we can use invokespecial (must use for super calls)      
      if (op.opcode == SCode.Call && parent.ir.is(m.parent()) && 
          (m.isVirtual() || m.isInstanceInit()))
      {
        javaCall = INVOKESPECIAL;
      }
    }                      
    
    // if native method we need to push Context onto the stack
    if (isNative) 
      code.add(GETSTATIC, parent.contextRef());
        
    // normal static/virtual call  
    int mref = parent.methodRef(m);
    code.add(javaCall, mref);
    
    // if we called a native method we need to cast java.lang.Object
    Type ret = m.returnType();
    if (m.isNative() && 
        ret.isRef() && !ret.isObj() && !ret.isArray() && !ret.isStr())
      code.add(CHECKCAST, code.cp.cls(JavaClassAsm.jname(ret, true)));
  }  
  
  void malloc(IrOp op)
  {
    Type type = op.type;
    if (type.isArray())
    {
      int of = code.cp.cls(JavaClassAsm.jname(type.arrayOf(), false));       
      code.add(ANEWARRAY, of);
    }
    else
    {
      int cls = code.cp.cls(JavaClassAsm.jname(type, false));       
      code.add(NEW, cls);
      code.add(DUP);
      code.add(INVOKESPECIAL, code.cp.method(cls, "<init>", "()V"));
    }
  }
    
////////////////////////////////////////////////////////////////
// Return
////////////////////////////////////////////////////////////////

  void returnOp()
  {                        
    code.add(returnOp(ir.ret));
  }                            
  
  int returnOp(Type t)
  {                   
    onReturn();       
    switch(jstackType(t))
    {
      case OBJ:    return ARETURN;
      case INT:    return IRETURN;
      case LONG:   return LRETURN;
      case FLOAT:  return FRETURN;
      case DOUBLE: return DRETURN;
      case VOID:   return RETURN;
      default: throw new IllegalStateException(t.toString());
    }
  }

////////////////////////////////////////////////////////////////
// Misc
////////////////////////////////////////////////////////////////

  void dupDown(IrOp op)
  {                               
    if ((op.flags & IrOp.STATIC_FIELD) != 0)
    {
      switch (op.opcode)
      {
        case SCode.DupDown2:  op(DUP);  break;
        case SCode.Dup2Down2: op(DUP2); break;
        default: throw new IllegalStateException(op.toString());
      }
      return;
    }               
    
    switch (op.opcode)
    {
      case SCode.DupDown2:  op(DUP_X1);  break;
      case SCode.DupDown3:  op(DUP_X2);  break;
      case SCode.Dup2Down2: op(DUP2_X1); break;
      case SCode.Dup2Down3: op(DUP2_X2); break;
      default: throw new IllegalStateException(op.toString());
    }
  }

  void foreach(IrOp op)
  {                         
    // at this point the array, length, and 
    // counter (initialized  to -1) is on the stack,
        
    int tempArray   = maxLocals; maxLocals += 1;    
    int tempLength  = maxLocals; maxLocals += 1;
    int tempCounter = maxLocals; maxLocals += 1;
    
    // store them all to locals
    store(tempCounter, INT);
    store(tempLength, INT);
    store(tempArray, OBJ);

    // now is the actual jump point to continue 
    // the foreach loop
    op.pos = code.code.count;        
    
    // increment our counter and jump out if lengt >= counter
    load(tempLength, INT);
    load(tempCounter, INT);
    code.add(ICONST_1);
    code.add(IADD);
    code.add(DUP);
    store(tempCounter, INT);
    jump(IF_ICMPLE, op);                             
    
    // at this point need to leave array, counter on stack
    load(tempArray, OBJ);
    load(tempCounter, INT);
  }                   

  void not()
  {
    code.add(ICONST_1);
    code.add(IXOR);
  }

  void skipLoadDataAddr()
  {         
    // skip LoadDataAddr since we don't need that
    // for loading static fields; but we might also
    // need to skip the next instruction if it is a dup
    int peek = peek().opcode;
    if (peek == SCode.Dup || peek == SCode.Dup2)
      cur++;
  }

  void initComp()
  {   
    // note: during sys bootstrap, this type field may be null                 
    kitAsm.loadType(code, parent.ir);
    code.add(PUTFIELD, code.cp.field("sedona/vm/sys/Component", 
      "type", "Lsedona/vm/sys/Type;"));
  }

  void switchOp(IrOp op)
  {                  
    String[] toks = TextUtil.split(op.arg, ',');
    int[] jumps = new int[toks.length];    
    for (int i=0; i<toks.length; ++i) jumps[i] = Integer.parseInt(toks[i]);
    
    int base = code.code.u1(TABLESWITCH);
    while (code.code.count % 4 != 0) code.code.u1(0);
    int def = code.code.u4(-1);   // def
    code.code.u4(0);              // low
    code.code.u4(jumps.length-1); // high
    for (int i=0; i<jumps.length; ++i)
    {
      Patch p = new Patch();
      p.baseOffset = base;
      p.offset = code.code.u4(-1);
      p.dest   = ir.code[jumps[i]];
      p.width  = 4;
      p.next   = patches;
      patches = p;
    }           
    code.code.u4(def, code.code.count-base);
  }

  void assertOp(IrOp op)
  {                  
    code.add(INVOKESTATIC, parent.assertRef());
  }

  void cast(IrOp op)
  {        
    Type type = op.argToType();        
    String jname = JavaClassAsm.jname(type, true);    
    code.add(CHECKCAST, code.cp.cls(jname));
  }

////////////////////////////////////////////////////////////////
// LineNumberTable
////////////////////////////////////////////////////////////////

  AttributeInfo toLineNumTable()
  {                        
    IrOp[] code = ir.code;
    Buffer buf = new Buffer(); 
    buf.u2(0xffff); // length
    int count = 0;
    int last = -1;
    for (int i=0; i<code.length; ++i)
    {                                       
      IrOp op = code[i];
      if (last == op.loc.line) continue;
      buf.u2(op.pos);
      buf.u2(op.loc.line);
      last = op.loc.line;
      count++;
    }             
    buf.u2(0, count);
    return new AttributeInfo(parent, "LineNumberTable", buf.trim());
  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  IrOp peek()
  { 
    if (cur+1 >= ir.code.length) return null;
    return ir.code[cur+1];
  }

  void op(int opcode) 
  { 
    code.add(opcode); 
  }

  void branchTo(int opcode, IrOp dest) 
  { 
    Patch p = new Patch();
    p.offset = branch(opcode);         
    p.baseOffset = p.offset-1;
    p.dest = dest;
    p.next = patches;
    patches = p;
  }

  int branch(int opcode) 
  { 
    return code.branch(opcode); 
  }

  void patch(int codeOffset) 
  {                       
    code.patch(codeOffset);
  }

  void patch(int codeOffset, int location) 
  {        
    code.patch(codeOffset, location);          
  }              
  
  void backpatch()
  {
    for (Patch p = patches; p != null; p = p.next)
    {
      if (p.width == 2)
        code.code.u2(p.offset, p.dest.pos-p.baseOffset);
      else 
        code.code.u4(p.offset, p.dest.pos-p.baseOffset);
    }
  }             
  
  public int jstackType(IrOp op) { return jstackType(op, true); }
  public int jstackType(IrOp op, boolean asInt)
  {
    if (op.type == null) 
      throw new IllegalStateException("IrOp not typed: " + op);
    return jstackType(op.type, asInt);
  }
  
  public int jstackType(Type t) { return jstackType(t, true); }
  public int jstackType(Type t, boolean asInt)
  {                        
    if (t.isRef()) return OBJ;
    switch (t.id())
    {
      case Type.voidId:   return VOID;
      case Type.boolId:   return asInt ? INT : BOOL;
      case Type.byteId:   return asInt ? INT : BYTE;
      case Type.shortId:  return asInt ? INT : SHORT;
      case Type.intId:    return INT;
      case Type.longId:   return LONG;
      case Type.floatId:  return FLOAT;
      case Type.doubleId: return DOUBLE;
      default: throw new IllegalStateException(t.toString());
    }    
  }

////////////////////////////////////////////////////////////////
// Patch
////////////////////////////////////////////////////////////////

  static class Patch
  {                                 
    int baseOffset; // offset from which to compute jump
    int offset;     // offset into code buffer to patch
    IrOp dest;      // destintation opcode  
    int width = 2;  // u2 or u4?
    Patch next;     // for linked list
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public static final int OBJ    = 'A';
  public static final int BOOL   = 'Z';
  public static final int INT    = 'I';
  public static final int BYTE   = 'B';
  public static final int SHORT  = 'S';
  public static final int FLOAT  = 'F';
  public static final int LONG   = 'J';
  public static final int DOUBLE = 'D';
  public static final int VOID   = 'V';
 
  JavaKitAsm kitAsm;
  JavaClassAsm parent;
  IrMethod ir;           
  IrOp prevOp;
  int cur;          // current index into ir.code  
  Code code;                 
  int localOffset;  // java register index of sedona local 0
  Patch patches;
  int maxLocals;
  int maxStack;
}

