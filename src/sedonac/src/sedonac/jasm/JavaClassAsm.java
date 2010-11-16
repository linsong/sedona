//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Oct 08  Brian Frank  Creation
//
package sedonac.jasm;

import sedona.Facets;
import sedonac.ir.*;
import sedonac.namespace.*;

/**          
 * JavaClassAsm generates a Java classfile for a Sedona class.
 */
public class JavaClassAsm  
  extends Assembler     
  implements OpCodes
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  JavaClassAsm(JavaKitAsm parent, IrType ir)
  {                          
    super(jname(ir, false), jname(ir.base(), false), toClassFlags(ir), toInterfaces(ir));
    this.parent = parent;
    this.ir = ir; 
  }                                               
  
  static int toClassFlags(IrType ir)
  {
    int flags = Jvm.ACC_PUBLIC;
    if (ir.isAbstract()) flags |= Jvm.ACC_ABSTRACT;
    return flags;
  }
  
  static String[] toInterfaces(IrType t)
  {                    
    if (t.qname().equals("sys::Kit")) 
      return new String[] { "sedona/vm/IKit" };

    if (t.qname().equals("sys::Type")) 
      return new String[] { "sedona/vm/IType" };

    if (t.qname().equals("sys::Slot")) 
      return new String[] { "sedona/vm/ISlot" };

    if (t.qname().equals("sys::Component")) 
      return new String[] { "sedona/vm/IComponent" };
      
    return null;
  }

////////////////////////////////////////////////////////////////
// Assemble
////////////////////////////////////////////////////////////////

  public JavaClass assemble()
  {                   
    // assemble all the fields
    for (int i=0; i<ir.declared.length; ++i)
    {
      IrSlot slot = ir.declared[i];
      if (slot instanceof IrMethod)
        assembleMethod((IrMethod)slot);
      else
        assembleField((IrField)slot);
    }                                   
    
    // assemble constructor and class initializer
    assembleConstructor();
    assembleClassInit();
    
    // special 
    if (ir.qname.equals("sys::Kit")) assembleKitSpecials();
    if (ir.qname.equals("sys::Type")) assembleTypeSpecials();
    if (ir.qname.equals("sys::Slot")) assembleSlotSpecials();
    if (ir.qname.equals("sys::Component")) assembleCompSpecials();
    if (ir.qname.equals("sys::Sys")) assembleSysSpecials();
    
    // if @javaPeer facet specified, generate a public
    // field for Java natives to store an Object
    if (ir.facets.getb("javaPeer", false))
      addField(new FieldInfo(this, "peer", "Ljava/lang/Object;", Jvm.ACC_PUBLIC));
    
    // attributes        
    String filename = ir.loc.toFileName();
    if (filename != null)
      addAttribute(new AttributeInfo(this, "SourceFile", filename));
  
    // return classfile as JavaClass instance
    JavaClass cls = new JavaClass();              
    cls.kitName   = ir.kit().name();
    cls.name      = ir.name();
    cls.qname     = ir.qname();
    cls.classfile = compile();   

    // dump classfile             
    /*
    try
    {                                
    File f = new File(cls.name + ".class");
    System.out.println("DUMP: " + f);
    OutputStream out = new FileOutputStream(f);
    out.write(cls.classfile.trim());
    out.close();
    }
    catch (Exception e) { e.printStackTrace(); }        
    */
    
    return cls;
  }   
  
  void assembleField(IrField ir)
  {
    addField(new FieldInfo(this, ir.name, jsig(ir.type), jflags(ir.flags, ir.facets)));
  }

  void assembleMethod(IrMethod ir)
  {                           
    if (ir.isNative()) return;
           
    String name = ir.name;
    if (ir.isStaticInit()) sInit = ir;
    
    int flags = jflags(ir.flags, ir.facets);  
    if (ir.isInstanceInit()) flags |= Jvm.ACC_PUBLIC;
    if (this.ir.isStr()) flags |= Jvm.ACC_STATIC;
    
    MethodInfo mi = addMethod(new MethodInfo(this, name, jsig(ir), flags));
    if (ir.code != null)     
      mi.addAttribute(new JavaMethodAsm(this, ir).assemble());
  }                      
  
  void assembleConstructor()
  {                        
    Code code = new Code(this);     
    code.add(ALOAD_0);
    code.add(INVOKESPECIAL, cp.method(superClass, "<init>", "()V"));
    assembleInitFields(code, false);
    code.add(RETURN);       
    code.maxLocals = 8;
    code.maxStack  = 8;
    
    MethodInfo mi = addMethod(new MethodInfo(this, "<init>", "()V", Jvm.ACC_PUBLIC));       
    mi.addAttribute(code);
  }          

  void assembleClassInit()
  {                     
    Code code = new Code(this);     
    code.maxLocals = 8;
    code.maxStack  = 8;
    
    assembleInitFields(code, true);
    
    // NOTE: do not call _sInit() here. See note on JavaKitAsm::assembleBootstrap()
    
    code.add(RETURN);
    
    MethodInfo mi = addMethod(new MethodInfo(this, "<clinit>", "()V", Jvm.ACC_PUBLIC|Jvm.ACC_STATIC));       
    mi.addAttribute(code);
  }        
  
  void assembleInitFields(Code code, boolean statics)
  {
    for (int i=0; i<ir.declared.length; ++i)
    {
      IrSlot slot = ir.declared[i];
      if (!slot.isField()) continue;
      IrField f = (IrField)slot;
      if (f.isStatic() != statics) continue;    
      assembleInitField(code, f);      
    }      
  }
  
  void assembleInitField(final Code code, final IrField f)
  {                                                   
    assembleInitField(code, f, false, new Runnable()
    {  
      public void run() { code.addIntConst(f.type().arrayLength().val()); }
    });
  }
  
  void assembleInitField(Code code, IrField f, boolean doUnsized, Runnable pushLength)
  {       
    // logs are stuck into the kit assembler list and 
    // generated  by the KitConst static initializer
    if (f.isDefine() && f.type().qname().equals("sys::Log"))
    {
      parent.logs.add(f);
      return;
    }
    
    // if this is an array literal define, handle special
    if (f.isDefine() && f.type.isArray())
    {        
      assembleArrayLiteral(code, f);
      return;
    }
                       
    // we only assemble initialization code for inline fields
    if (!f.isInline()) return;                         
    
    // if const
    if (f.isConst()) 
    {                                                
      // ignore these consts, they are initialized by runtime
      if (f.qname().equals("sys::Kit.types")) return;        
      if (f.qname().equals("sys::Type.slots")) return;        
      
      // just for sanity sake
      throw new IllegalStateException("Unexpected const field: " + f.qname() + ": " + f.type());
    }                

    // if this is an unsized array, check if we are doing
    // them now (they get done in JavaMethodAsm.onReturn)
    if (f.ctorLengthParam >= 0 && !doUnsized) return; 

    // put this if instance field
    if (!f.isStatic()) code.add(ALOAD_0);    
        
    // arrays
    Type type = f.type();
    if (type.isArray())
    { 
      Type of = type.arrayOf();
      if (of.isArray()) throw new IllegalStateException("multi-dimension arrays not supported");
      
      // push length onto stack
      pushLength.run();
            
      // create array     
      assembleNewArray(code, of);
      if (f.arrayInit)  arrayInit(code, of, pushLength);
    }

    // string
    else if (type.isStr())
    {                        
      // StrRef with size of buffer
      code.addIntConst(f.ctorLengthArg.toIntLiteral().intValue());
      code.add(INVOKESTATIC, cp.method("sedona/vm/StrRef", "make", "(I)Lsedona/vm/StrRef;"));
    }
    
    // object
    else
    {
      int cls = cp.cls(jname(type, false));
      code.add(NEW, cls);
      code.add(DUP);
      code.add(INVOKESPECIAL, cp.method(cls, "<init>", "()V"));
    }                          
    
    // store field    
    if (f.isStatic())
      code.add(PUTSTATIC, fieldRef(f));
    else
      code.add(PUTFIELD, fieldRef(f));
  }                   
  
  /** 
   * Add instr to create array; length must already push onto stack 
   */
  static void assembleNewArray(Code code, Type of)
  {    
    if (of.isRef()) 
    {      
      code.add(ANEWARRAY, code.cp.cls(jname(of, true)));
    }
    else switch (of.id())
    {
      case Type.boolId:   code.add(NEWARRAY, Jvm.T_BOOLEAN); break;
      case Type.byteId:   code.add(NEWARRAY, Jvm.T_BYTE); break;
      case Type.shortId:  code.add(NEWARRAY, Jvm.T_SHORT); break;
      case Type.intId:    code.add(NEWARRAY, Jvm.T_INT); break;
      case Type.longId:   code.add(NEWARRAY, Jvm.T_LONG); break;
      case Type.floatId:  code.add(NEWARRAY, Jvm.T_FLOAT); break;
      case Type.doubleId: code.add(NEWARRAY, Jvm.T_DOUBLE); break;
      default: throw new IllegalStateException("assembleInitField: " + of);
    }
  }
    
  void arrayInit(Code code, Type of, Runnable pushLength)
  {                            
    int cls = cp.cls(jname(of, false));
    int ctor = cp.method(cls, "<init>", "()V");
    
    code.add(ASTORE_2);            // store array
    pushLength.run();              // push length
    code.add(ISTORE_3);            // store length
    int mark = code.add(ILOAD_3);  // mark: load length
    code.add(ICONST_1);            // subtract
    code.add(ISUB);                //   one
    code.add(ISTORE_3);            // store new index
    code.add(ALOAD_2);             // load array
    code.add(ILOAD_3);             // load index
    
    code.add(NEW, cls);            // construct fresh instance
    code.add(DUP);
    code.add(INVOKESPECIAL, ctor);
    
    code.add(AASTORE);             // store to array
    code.add(ILOAD_3);             // load index
    code.branch(IFNE, mark);       // if not zero loop to mark
    code.add(ALOAD_2);             // restore array to stack
  }

  void assembleArrayLiteral(Code code, IrField f)
  { 
    Type of = f.type.arrayOf();
    Object[] array = f.define.asArray();
    
    // push length   
    code.addIntConst(array.length);                                            
    
    // create array  
    assembleNewArray(code, of);
    
    // set array values
    for (int i=0; i<array.length; ++i)
    {
      Object v = array[i];
      code.add(DUP);
      code.addIntConst(i);
      if (of.isStr())
      {    
        parent.loadStr(code, (String)v);
        code.add(AASTORE);
      }
      else switch (of.id())
      { 
        case Type.byteId:   code.addIntConst(((Integer)v).intValue()); code.add(BASTORE); break;
        case Type.shortId:  code.addIntConst(((Integer)v).intValue()); code.add(SASTORE); break;
        case Type.intId:    code.addIntConst(((Integer)v).intValue()); code.add(IASTORE); break;
        case Type.longId:   code.addLongConst(((Long)v).longValue()); code.add(LASTORE); break;
        case Type.floatId:  code.addFloatConst(((Float)v).floatValue()); code.add(FASTORE); break;
        case Type.doubleId: code.addDoubleConst(((Double)v).doubleValue()); code.add(DASTORE); break;
        default: throw new IllegalStateException(of.toString());
      }
    }
    
    // save to field
    code.add(PUTSTATIC, fieldRef(f));
  }

////////////////////////////////////////////////////////////////
// Kit Specials
////////////////////////////////////////////////////////////////

  void assembleKitSpecials()
  {    
    // int id() { return id }
    addGetter("id", "()I", "id", "B");
    
    // String name() { return _name }
    addGetter("name", "()Ljava/lang/String;", "_name", "Ljava/lang/String;");
    
    // return IType[] types() { return types }
    addGetter("types", "()[Lsedona/vm/IType;", "types", "[Lsedona/vm/sys/Type;");
    
    // String _name
    addField(new FieldInfo(this, "_name", "Ljava/lang/String;", Jvm.ACC_PUBLIC));
  }

////////////////////////////////////////////////////////////////
// Type Specials
////////////////////////////////////////////////////////////////

  void assembleTypeSpecials()
  {                            
    // IKit kit() { return kit }
    addGetter("kit", "()Lsedona/vm/IKit;", "kit", "Lsedona/vm/sys/Kit;");

    // int id() { return id }
    addGetter("id", "()I", "id", "B");

    // String name() { return _name }
    addGetter("name", "()Ljava/lang/String;", "_name", "Ljava/lang/String;");

    // IType base() { return base }
    addGetter("base", "()Lsedona/vm/IType;", "base", "Lsedona/vm/sys/Type;");

    // ISlot[] slots() { return slots }
    addGetter("slots", "()[Lsedona/vm/ISlot;", "slots", "[Lsedona/vm/sys/Slot;");
    
    // String _name
    addField(new FieldInfo(this, "_name", "Ljava/lang/String;", Jvm.ACC_PUBLIC));
  }        

////////////////////////////////////////////////////////////////
// Slot Specials
////////////////////////////////////////////////////////////////

  void assembleSlotSpecials()
  {    
    // int id() { return id }
    addGetter("id", "()I", "id", "B");

    // String name() { return _name }
    addGetter("name", "()Ljava/lang/String;", "_name", "Ljava/lang/String;");

    // IType type() { return type }
    addGetter("type", "()Lsedona/vm/IType;", "type", "Lsedona/vm/sys/Type;");

    // int flags() { return flags }
    addGetter("flags", "()I", "flags", "B");
        
    // String _name
    addField(new FieldInfo(this, "_name", "Ljava/lang/String;", Jvm.ACC_PUBLIC));
    
    // SlotAccessor accessor
    addField(new FieldInfo(this, "accessor", "Lsedona/vm/SlotAccessor;", Jvm.ACC_PUBLIC));
    
    // SlotAccessor accessor() { return accessor }
    addGetter("accessor", "()Lsedona/vm/SlotAccessor;", "accessor", "Lsedona/vm/SlotAccessor;");
  }        

////////////////////////////////////////////////////////////////
// Component Specials
////////////////////////////////////////////////////////////////

  void assembleCompSpecials()
  {    
    // IType IComponent.type()    
    Code code = new Code(this);     
    code.maxLocals = 2;
    code.maxStack  = 2;
    code.add(ALOAD_0);
    code.add(GETFIELD, cp.field("sedona/vm/sys/Component", "type", "Lsedona/vm/sys/Type;"));    
    code.add(ARETURN);
    addMethod(new MethodInfo(this, "type", "()Lsedona/vm/IType;", Jvm.ACC_PUBLIC|Jvm.ACC_FINAL, code));       
    
    // Object[] IComponent.slots()    
    code = new Code(this);     
    code.maxLocals = 2;
    code.maxStack  = 2;
    code.add(ALOAD_0);
    code.add(GETFIELD, cp.field("sedona/vm/sys/Component", "slots", "[Lsedona/Value;"));    
    code.add(ARETURN);
    addMethod(new MethodInfo(this, "slots", "()[Lsedona/Value;", Jvm.ACC_PUBLIC|Jvm.ACC_FINAL, code));       

    // void IComponent.slots(Object[])    
    code = new Code(this);     
    code.maxLocals = 2;
    code.maxStack  = 2;
    code.add(ALOAD_0);
    code.add(ALOAD_1);
    code.add(PUTFIELD, cp.field("sedona/vm/sys/Component", "slots", "[Lsedona/Value;"));    
    code.add(RETURN);
    addMethod(new MethodInfo(this, "slots", "([Lsedona/Value;)V", Jvm.ACC_PUBLIC|Jvm.ACC_FINAL, code));       

    // Object[] slots
    addField(new FieldInfo(this, "slots", "[Lsedona/Value;", Jvm.ACC_PRIVATE));
  }        

////////////////////////////////////////////////////////////////
// Sys Specials
////////////////////////////////////////////////////////////////

  void assembleSysSpecials()
  {               
    // Context context
    addField(new FieldInfo(this, "context", "Lsedona/vm/Context;", Jvm.ACC_PUBLIC|Jvm.ACC_STATIC));
  }

  public int contextRef()
  {
    if (contextRef == 0)
      contextRef = cp.field("sedona/vm/sys/Sys", "context", "Lsedona/vm/Context;");
    return contextRef;
  }
  
////////////////////////////////////////////////////////////////
// Method
////////////////////////////////////////////////////////////////

  void addGetter(String javaMethod, String javaSig, String sedonaField, String sedonaSig)
  {
    Code code = new Code(this);     
    code.maxLocals = 2;
    code.maxStack  = 2;
    code.add(ALOAD_0);
    code.add(GETFIELD, cp.field(thisClass, sedonaField, sedonaSig));    
    if (javaSig.equals("()I")) code.add(IRETURN);
    else code.add(ARETURN);
    addMethod(new MethodInfo(this, javaMethod, javaSig, Jvm.ACC_PUBLIC|Jvm.ACC_FINAL, code));       
  }

  public int assertRef()
  {
    if (assertRef == 0)
      assertRef = cp.method("sedona/vm/VmUtil", "assertOp", "(Z)V");
    return assertRef;
  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  public static String[] jnames(Type[] t)
  {                                      
    String[] jnames = new String[t.length];
    for (int i=0; i<t.length; ++i)
      jnames[i] = jname(t[i], false);
    return jnames;
  }

  public static String jname(Type t, boolean slotSig)
  {                      
    if (t == null) return null;
    
    if (t.isPrimitive())
    {
      switch (t.id())
      {
        case Type.voidId:   return "V";
        case Type.boolId:   return "B";
        case Type.byteId:   return "B";
        case Type.shortId:  return "S";
        case Type.intId:    return "I";
        case Type.longId:   return "J";
        case Type.floatId:  return "F";
        case Type.doubleId: return "D";
      }                   
    }      

    if (t.isObj()) return "java/lang/Object";
    
    if (t.isStr()) return slotSig ? "sedona/vm/StrRef" : "sedona/vm/sys/Str";
    
    if (t.isArray()) return "[" + jsig(t.arrayOf());
    
    return jname(t.kit().name(), t.name());
  }

  public static String jname(String kitName, String typeName)
  {
    return "sedona/vm/" + kitName + "/" + typeName;
  }

  public static String jsig(Type t) 
  {
    String s = jname(t, true);
    if (s.charAt(0) == '[' || s.length() < 5) return s;
    return "L" + s + ";";
  }

  public static String jsig(Method m) 
  {                              
    Type[] params = m.paramTypes();     
    Type parent = m.parent();
    boolean isStatic = m.isStatic();
    boolean isNative = isJavaNative(m);
    
    StringBuffer s = new StringBuffer();
    s.append('(');                                            

    // explicit self parameter   
    if ((parent.isStr() || isNative) && !isStatic)
      s.append(jsig(parent, isNative));
    
    // parameters
    for (int i=0; i<params.length; ++i)
      s.append(jsig(params[i], isNative));
   
    // if native add Context parameter
    if (isNative)
      s.append("Lsedona/vm/Context;");
      
    s.append(')');
    
    // return type
    s.append(jsig(m.returnType(), isNative));
    return s.toString();
  }                   
  
  private static String jsig(Type t, boolean forNative)
  {                      
    String sig = jsig(t);        
    if (forNative && sig.startsWith("L") && !t.isStr()) 
      return "Ljava/lang/Object;";
    return sig;
  }
  
  public int fieldRef(Field f) 
  {                        
    return fieldRef(this, f);       
  }

  public static int fieldRef(Assembler asm, Field f) 
  {                 
    int cls = asm.cp.cls(jname(f.parent(), false));
    int nt  = asm.cp.nt(f.name(), jsig(f.type()));
    return asm.cp.field(cls, nt);
  }

  public int methodRef(Method m) 
  {                      
    String parent = jname(m.parent(), false);                     
       
    if (isJavaNative(m))  parent += "_n";
    
    int cls = cp.cls(parent);
    int nt  = cp.nt(m.name(), jsig(m));
    return cp.method(cls, nt);
  }

  public static int jflags(int sflags, Facets facets) 
  {
    int jflags = Jvm.ACC_PUBLIC; 
    if ((sflags & Slot.ABSTRACT) != 0)  jflags |= Jvm.ACC_ABSTRACT;
    if ((sflags & Slot.STATIC) != 0)    jflags |= Jvm.ACC_STATIC;
    /*
    if (facets.getb("javaPublic", false))
    {
      jflags |= Jvm.ACC_PUBLIC;
    }
    else
    {
      if ((sflags & Slot.PUBLIC) != 0)    jflags |= ;
      if ((sflags & Slot.PROTECTED) != 0) jflags |= Jvm.ACC_PROTECTED;
      if ((sflags & Slot.PRIVATE) != 0)   jflags |= Jvm.ACC_PRIVATE;
    } 
    */
    return jflags;
  }               
  
  public static boolean isJavaNative(Method m)
  {
    return m.isNative() || 
           m.parent().isStr() || 
           m.facets().getb("javaNative");
  }
 
  public static void echo(Code code, String s)
  {  
    code.add(LDC, code.cp.string(s));    
    code.add(INVOKESTATIC, code.cp.method("sedona/vm/VmUtil", "echo", "(Ljava/lang/Object;)V"));
  }
     
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  JavaKitAsm parent;
  IrType ir;
  IrMethod sInit;
  int assertRef;      
  int contextRef;
  
}

