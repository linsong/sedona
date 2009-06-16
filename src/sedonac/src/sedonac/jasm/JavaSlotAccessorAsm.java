//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Oct 08  Brian Frank  Creation
//
package sedonac.jasm;
                   
import java.util.*;                   
import sedona.Buf;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.namespace.*;
import sedonac.steps.*;

/**          
 * JavaSlotAccessorAsm compiles a SlotAccessor subclass for
 * a specific SlotDef to reflectively get/set a property field
 * or call an action method.
 */
public class JavaSlotAccessorAsm  
  extends CompilerSupport     
  implements OpCodes
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  public JavaSlotAccessorAsm(Compiler c, SlotDef slot)
  {          
    super(c);                  
    this.ir = compiler.ir;
    this.slot = slot;
  }

////////////////////////////////////////////////////////////////
// Top
////////////////////////////////////////////////////////////////

  public JavaClass assemble()
  {                        
    // assemble the Acc_Type_slot class                                         
    String name = "Acc_" + slot.parent.name + "_" + slot.name;
    String classname = "sedona/vm/" + ir.name + "/" + name;
    Assembler asm = new Assembler(classname, "sedona/vm/SlotAccessor", Jvm.ACC_PUBLIC|Jvm.ACC_FINAL, null);
    
    // generate the methods on the SlotAccessor subclass
    assembleCtor(asm);        
    if (slot.isField())
    {              
      FieldDef field = (FieldDef)slot;
      switch (field.type.id())
      {       
        case Type.boolId:   assembleBoolProp(asm); break;
        case Type.byteId:   assembleByteProp(asm); break;
        case Type.shortId:  assembleShortProp(asm); break;
        case Type.intId:    assembleIntProp(asm); break;
        case Type.longId:   assembleLongProp(asm); break;
        case Type.floatId:  assembleFloatProp(asm); break;
        case Type.doubleId: assembleDoubleProp(asm); break;
        case Type.bufId:    assembleBufProp(asm); break;
        default: throw new IllegalStateException(field.type().toString());
      }
    }
    else
    {
      MethodDef method = (MethodDef)slot;
      switch (method.actionType(ns).id())
      {       
        case Type.voidId:   assembleVoidAction(asm); break;
        case Type.boolId:   assembleBoolAction(asm); break;
        case Type.intId:    assembleIntAction(asm); break;
        case Type.longId:   assembleLongAction(asm); break;
        case Type.floatId:  assembleFloatAction(asm); break;
        case Type.doubleId: assembleDoubleAction(asm); break;
        case Type.bufId:    assembleBufAction(asm); break;
        default: throw new IllegalStateException(method.actionType(ns).toString());
      }
    }
    
    // return classfile as JavaClass instance
    JavaClass cls = new JavaClass();              
    cls.kitName   = ir.name;
    cls.name      = name;
    cls.qname     = ir.name + "::" + name;
    cls.classfile = asm.compile();    
    return cls;
  }                         
  
////////////////////////////////////////////////////////////////
// Ctor
////////////////////////////////////////////////////////////////

  void assembleCtor(Assembler asm)
  {
    // <init> constructor
    Code init = new Code(asm);     
    init.add(ALOAD_0);
    init.add(INVOKESPECIAL, asm.cp.method("sedona/vm/SlotAccessor", "<init>", "()V"));
    init.add(RETURN);       
    init.maxLocals = 1;
    init.maxStack  = 2;    
    asm.addMethod(new MethodInfo(asm, "<init>", "()V", Jvm.ACC_PUBLIC, init));
  }

////////////////////////////////////////////////////////////////
// Properties
////////////////////////////////////////////////////////////////

  void assembleBoolProp(Assembler asm)
  {                     
    assembleGetter(asm, "getBool", "(Ljava/lang/Object;)B", IRETURN, 0);
    assembleSetter(asm, "setBool", "(Ljava/lang/Object;B)V", ILOAD_2);
  }

  void assembleByteProp(Assembler asm)
  {                     
    assembleGetter(asm, "getInt", "(Ljava/lang/Object;)I", IRETURN, 0xff);
    assembleSetter(asm, "setInt", "(Ljava/lang/Object;I)V", ILOAD_2);
  }

  void assembleShortProp(Assembler asm)
  {                     
    assembleGetter(asm, "getInt", "(Ljava/lang/Object;)I", IRETURN, 0xffff);
    assembleSetter(asm, "setInt", "(Ljava/lang/Object;I)V", ILOAD_2);
  }

  void assembleIntProp(Assembler asm)
  {                     
    assembleGetter(asm, "getInt", "(Ljava/lang/Object;)I", IRETURN, 0);
    assembleSetter(asm, "setInt", "(Ljava/lang/Object;I)V", ILOAD_2);
  }

  void assembleLongProp(Assembler asm)
  {                     
    assembleGetter(asm, "getLong", "(Ljava/lang/Object;)J", LRETURN, 0);
    assembleSetter(asm, "setLong", "(Ljava/lang/Object;J)V", LLOAD_2);
  }

  void assembleFloatProp(Assembler asm)
  {                     
    assembleGetter(asm, "getFloat", "(Ljava/lang/Object;)F", FRETURN, 0);
    assembleSetter(asm, "setFloat", "(Ljava/lang/Object;F)V", FLOAD_2);
  }

  void assembleDoubleProp(Assembler asm)
  {                     
    assembleGetter(asm, "getDouble", "(Ljava/lang/Object;)D", DRETURN, 0);
    assembleSetter(asm, "setDouble", "(Ljava/lang/Object;D)V", DLOAD_2);
  }

  void assembleBufProp(Assembler asm)
  {                     
    assembleGetter(asm, "getBuf", "(Ljava/lang/Object;)Ljava/lang/Object;", ARETURN, 0);
    // no setter
  }

  void assembleGetter(Assembler asm, String name, String sig, int ret, int mask)
  {                  
    // X getX(Object comp) { return ((Foo)comp).field; }              
    Code code = new Code(asm);     
    code.add(ALOAD_1);
    code.add(CHECKCAST, parentCls(asm));
    code.add(GETFIELD, fieldRef(asm));
    if (mask != 0)
    {
      code.addIntConst(mask);
      code.add(IAND);
    }
    code.add(ret);
    code.maxLocals = 2;
    code.maxStack  = 2;    
    asm.addMethod(new MethodInfo(asm, name, sig, Jvm.ACC_PUBLIC|Jvm.ACC_FINAL, code));
  }         

  void assembleSetter(Assembler asm, String name, String sig, int load)
  {                  
    // void setX(Object comp, X val) { ((Foo)comp).field = val; }              
    Code code = new Code(asm);     
    code.add(ALOAD_1);
    code.add(CHECKCAST, parentCls(asm));
    code.add(load);
    code.add(PUTFIELD, fieldRef(asm));
    code.add(RETURN);
    code.maxLocals = 4;
    code.maxStack  = 3;    
    asm.addMethod(new MethodInfo(asm, name, sig, Jvm.ACC_PUBLIC|Jvm.ACC_FINAL, code));
  }         

////////////////////////////////////////////////////////////////
// Actions
////////////////////////////////////////////////////////////////

  void assembleVoidAction(Assembler asm)
  {                     
    assembleInvoke(asm, "invokeVoid", "(Ljava/lang/Object;)V", 0);
  }

  void assembleBoolAction(Assembler asm)
  {                     
    assembleInvoke(asm, "invokeBool", "(Ljava/lang/Object;B)V", ILOAD_2);
  }

  void assembleIntAction(Assembler asm)
  {                     
    assembleInvoke(asm, "invokeInt", "(Ljava/lang/Object;I)V", ILOAD_2);
  }

  void assembleLongAction(Assembler asm)
  {                     
    assembleInvoke(asm, "invokeLong", "(Ljava/lang/Object;J)V", LLOAD_2);
  }

  void assembleFloatAction(Assembler asm)
  {                     
    assembleInvoke(asm, "invokeFloat", "(Ljava/lang/Object;F)V", FLOAD_2);
  }

  void assembleDoubleAction(Assembler asm)
  {                     
    assembleInvoke(asm, "invokeDouble", "(Ljava/lang/Object;D)V", DLOAD_2);
  }

  void assembleBufAction(Assembler asm)
  {                     
    assembleInvoke(asm, "invokeBuf", "(Ljava/lang/Object;Ljava/lang/Object;)V", ALOAD_2);
  }

  void assembleInvoke(Assembler asm, String name, String sig, int load)
  {                  
    // void invokeX(Object comp, X val) { ((Foo)comp).method(val); }              
    Code code = new Code(asm);     
    code.add(ALOAD_1);
    code.add(CHECKCAST, parentCls(asm));
    if (load != 0) code.add(load); 
    if (name.equals("invokeBuf")) code.add(CHECKCAST, asm.cp.cls("sedona/vm/sys/Buf"));
    code.add(INVOKEVIRTUAL, methodRef(asm));
    code.add(RETURN);
    code.maxLocals = 4;
    code.maxStack  = 4;    
    asm.addMethod(new MethodInfo(asm, name, sig, Jvm.ACC_PUBLIC|Jvm.ACC_FINAL, code));
  }         
  
////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  int parentCls(Assembler asm)
  {                
    if (parentCls == 0)
      parentCls = asm.cp.cls(JavaClassAsm.jname(slot.parent, false));
    return parentCls;
  }
  
  int fieldRef(Assembler asm)
  {                
    if (fieldRef == 0)
      fieldRef = asm.cp.field(parentCls(asm), slot.name, JavaClassAsm.jsig(((FieldDef)slot).type));
    return fieldRef;         
  }

  int methodRef(Assembler asm)
  {                
    if (fieldRef == 0)
      fieldRef = asm.cp.method(parentCls(asm), slot.name, JavaClassAsm.jsig((MethodDef)slot));
    return fieldRef;         
  }
    
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  IrKit ir;           
  SlotDef slot;
  int parentCls = 0;             
  int fieldRef = 0;

}

