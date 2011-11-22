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
 * JavaKitAsm is used to compile a kits into Java bytecode classfiles.
 */
public class JavaKitAsm  
  extends CompilerSupport     
  implements OpCodes
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  public JavaKitAsm(Compiler c)
  {          
    super(c);    
    this.ir = compiler.ir;
    this.isSys = ir.name.equals("sys");
    this.jnameKitConst = "sedona/vm/" + ir.name + "/KitConst";
    this.jnameBootstrap = "sedona/vm/" + ir.name + "/JsvmBootstrap";
  }

////////////////////////////////////////////////////////////////
// Top
////////////////////////////////////////////////////////////////

  public JavaClass[] assemble()
  {                        
    assembleTypeClasses();
    assembleKitClass();
    assembleBootstrapClass();
    return (JavaClass[])classes.toArray(new JavaClass[classes.size()]);
  }
    
////////////////////////////////////////////////////////////////
// Type Classes
////////////////////////////////////////////////////////////////

  void assembleTypeClasses()
  { 
    for (int i=0; i<ir.types.length; ++i)            
      classes.add(new JavaClassAsm(this, ir.types[i]).assemble());
  }    

////////////////////////////////////////////////////////////////
// Kit$Const Classe
////////////////////////////////////////////////////////////////

  void assembleKitClass()
  {                   
    // assemble the Kit$Const class
    Assembler asm = new Assembler(jnameKitConst, "java/lang/Object", Jvm.ACC_PUBLIC, null);
    
    // initReflect()                    
    Code reflect = new Code(asm);
    assembleReflect(asm, reflect);
    reflect.add(RETURN);       
    reflect.maxLocals = 4;
    reflect.maxStack  = 4;    
    asm.addMethod(new MethodInfo(asm, "initReflect", "()V", Jvm.ACC_STATIC, reflect));

    // initTests()                    
    Code tests = new Code(asm);
    assembleTests(asm, tests);
    tests.add(RETURN);       
    tests.maxLocals = 4;
    tests.maxStack  = 4;    
    asm.addMethod(new MethodInfo(asm, "initTests", "()V", Jvm.ACC_STATIC, tests));
    
    // <clinit> constructor
    Code clinit = new Code(asm);             
    assembleStrConsts(asm, clinit);
    assembleBufConsts(asm, clinit);
    assembleLogConsts(asm, clinit);
    clinit.add(INVOKESTATIC, asm.cp.method(jnameKitConst, "initReflect", "()V"));
    clinit.add(INVOKESTATIC, asm.cp.method(jnameKitConst, "initTests", "()V"));
    clinit.add(RETURN);       
    clinit.maxLocals = 4;
    clinit.maxStack  = 4;    
    asm.addMethod(new MethodInfo(asm, "<clinit>", "()V", Jvm.ACC_PUBLIC|Jvm.ACC_STATIC, clinit));
    
    // <init> constructor
    Code init = new Code(asm);     
    init.add(ALOAD_0);
    init.add(INVOKESPECIAL, asm.cp.method("java/lang/Object", "<init>", "()V"));
    init.add(RETURN);       
    init.maxLocals = 1;
    init.maxStack  = 2;    
    asm.addMethod(new MethodInfo(asm, "<init>", "()V", Jvm.ACC_PUBLIC, init));

    // return classfile as JavaClass instance
    JavaClass cls = new JavaClass();              
    cls.kitName   = ir.name;
    cls.name      = "KitConst";
    cls.qname     = ir.name + "::KitConst";
    cls.classfile = asm.compile();
    classes.add(cls);
  }
  
////////////////////////////////////////////////////////////////
// Bootstrap
////////////////////////////////////////////////////////////////

  /**
   * Creates bootstrap class for the kit to invoke the static initializers of
   * every type that contains one. The JSVM must call {@code bootstrap()} for
   * every kit before launching {@code Sys.main}.
   * <p>
   * Note: calling the {@code _sInit()} method should <b>not</b> be done in the
   * class initializer {@code <clinit>} of a Type that contains one because the
   * Java VM is not required to invoke the class initializer until the class is
   * loaded - which may be some time into jsvm execution (or never). However,
   * the Sedona VM guarantees that all {@code _sInit()} will be invoked prior
   * to executing main, so we must maintain that behavior in a JSVM implementation.
   * 
   * <pre>
   * public class sedona.vm.&lt;kit&gt;.JsvmBootstrap
   * {
   *   public static void bootstrap()
   *   {
   *     &lt;type_1&gt;._sInit()
   *     ...
   *     &lt;type_n&gt;._sInit()
   *   }
   * }
   * </pre>
   * @see sedonac.gen.ImageGen
   */
  void assembleBootstrapClass()
  {
    Assembler asm = new Assembler(jnameBootstrap, "java/lang/Object", Jvm.ACC_PUBLIC, null);
    
    // public static void bootstrap()
    Code code = new Code(asm);
    code.maxLocals = 4;
    code.maxStack = 0;
    IrFlat flat = new IrFlat(compiler.ns, new IrKit[] { compiler.ir });
    flat.preResolve();
    IrMethod[] sInits = flat.staticInits;
    final int contextRef = asm.cp.field("sedona/vm/sys/Sys", "context", "Lsedona/vm/Context;");
    for (int i=0; i<sInits.length; ++i)
    {
      IrMethod m = sInits[i];
      String parent = JavaClassAsm.jname(m.parent(), false);
      String sig = JavaClassAsm.jsig(m);
      int cls = asm.cp.cls(parent);
      int nt  = asm.cp.nt(m.name(), sig);
      if (sig.indexOf("Lsedona/vm/Context;") != -1)
      {
        code.add(GETSTATIC, contextRef);
        code.maxStack++;
      }
      code.add(INVOKESTATIC, asm.cp.method(cls, nt));
    }
    code.add(RETURN);
    asm.addMethod(new MethodInfo(asm, "bootstrap", "()V", Jvm.ACC_PUBLIC|Jvm.ACC_STATIC, code));
    
    // <clinit> constructor
    Code clinit = new Code(asm);             
    clinit.add(RETURN);       
    clinit.maxLocals = 4;
    clinit.maxStack  = 4;    
    asm.addMethod(new MethodInfo(asm, "<clinit>", "()V", Jvm.ACC_PUBLIC|Jvm.ACC_STATIC, clinit));
    
    // <init> constructor
    Code init = new Code(asm);     
    init.add(ALOAD_0);
    init.add(INVOKESPECIAL, asm.cp.method("java/lang/Object", "<init>", "()V"));
    init.add(RETURN);       
    init.maxLocals = 1;
    init.maxStack  = 2;    
    asm.addMethod(new MethodInfo(asm, "<init>", "()V", Jvm.ACC_PUBLIC, init));
    
    // compile class
    JavaClass cls = new JavaClass();
    cls.kitName   = ir.name;
    cls.name      = "JsvmBootstrap";
    cls.qname     = ir.name + "::JsvmBootstrap";
    cls.classfile = asm.compile();

    classes.add(cls);
  }
  
////////////////////////////////////////////////////////////////
// Kit/Type/Slot Reflection Constants
////////////////////////////////////////////////////////////////
  
  /**
   * Generate Kit, Type, and Slot reflection constant instances:
   *   static Kit kit = ...
   *   static Type typeA = ...
   *   static Type typeB = ...
   *   ...
   *   static Type slotA_x = ...
   *   static Type slotA_y = ...
   *   ...
   */
  void assembleReflect(Assembler asm, Code code)
  {           
    // find the reflective types
    TypeDef[] types = findReflectiveTypes();        
    
    // Kit instance        
    assembleKit(asm, code, typesLen(types));                       
    
    // primitive types if compiling sys
    if (isSys)
    {
      for (int i=0; i<ns.primitiveTypes.length; ++i)
        assembleType(asm, code, ns.primitiveTypes[i]);
    }
    
    // Type instances 
    for (int i=0; i<types.length; ++i)
      assembleType(asm, code, types[i]);
    
    // Slot instances
    for (int i=0; i<types.length; ++i)                  
    {
      TypeDef t = types[i]; 
      if (t.reflectiveSlots == null) continue; 
      for (int j=0; j<t.reflectiveSlots.length; ++j)
        assembleSlot(asm, code, t.reflectiveSlots[j]);
    }
  }                     
  
  /**
   * Find all the component types which are reflective.
   */
  TypeDef[] findReflectiveTypes()
  {
    ArrayList acc = new ArrayList();
    for (int i=0; i<ast.types.length; ++i)
    {
      TypeDef t = ast.types[i];
      if (t.isReflective()) acc.add(t);
    }
    return (TypeDef[])acc.toArray(new TypeDef[acc.size()]);
  }                          
  
  /**
   * Define the typesLen based on the max id assigned 
   * by an earlier step.
   */
  int typesLen(TypeDef[] types)
  {        
    if (types.length == 0) return 0;
    int maxId = 0;
    for (int i=0; i<types.length; ++i)
      maxId = Math.max(maxId, types[i].id);
    return maxId + 1;
  }
  
  /**
   * Generate and initialize the Kit kit field.
   */
  void assembleKit(Assembler asm, Code code, int typesLen)
  {
    ConstantPool cp = asm.cp;
    int kitCls  = cp.cls("sedona/vm/sys/Kit");      
    int kitCtor = cp.method(kitCls, "<init>", "()V");   
    String field = "kit";
    
    // generate field
    int flags = Jvm.ACC_PUBLIC | Jvm.ACC_STATIC;
    asm.addField(new FieldInfo(asm, "kit", "Lsedona/vm/sys/Kit;", flags));    
  }
  
  /**
   * Generate and initialize the Type typeX field.
   */
  void assembleType(Assembler asm, Code code, Type t)
  {
    ConstantPool cp = asm.cp;
    int typeCls  = cp.cls("sedona/vm/sys/Type");      
    int typeCtor = cp.method(typeCls, "<init>", "()V");   
    String fieldName = "type" + t.name();
    int field = asm.cp.field(jnameKitConst, fieldName, "Lsedona/vm/sys/Type;");
            
    // generate field
    int flags = Jvm.ACC_PUBLIC | Jvm.ACC_STATIC;
    asm.addField(new FieldInfo(asm, fieldName, "Lsedona/vm/sys/Type;", flags)); 
  }

  /**
   * Generate and initialize the Slot slotX_y field.
   */
  void assembleSlot(Assembler asm, Code code, SlotDef s)
  {
    ConstantPool cp = asm.cp;
    int slotCls  = cp.cls("sedona/vm/sys/Slot");      
    int slotCtor = cp.method(slotCls, "<init>", "()V");   
    String fieldName = "slot" + s.parent.name + "_" + s.name;
    int field = asm.cp.field(jnameKitConst, fieldName, "Lsedona/vm/sys/Slot;");
    
    // generate accessor class
    JavaClass acc = new JavaSlotAccessorAsm(compiler, s).assemble();
    classes.add(acc);        
            
    // generate field
    int flags = Jvm.ACC_PUBLIC | Jvm.ACC_STATIC;
    asm.addField(new FieldInfo(asm, fieldName, "Lsedona/vm/sys/Slot;", flags)); 
  }                         
  
  /**
   * Get the Slot.type value - for fields it is the 
   * field type, for actions it is the argument type.
   */
  Type slotReflectionType(Slot s)
  {
    if (s.isField()) return ((FieldDef)s).type();
    MethodDef m = (MethodDef)s;
    if (m.params.length == 0) return ns.voidType;
    return m.params[0].type;
  }

////////////////////////////////////////////////////////////////
// Test List
////////////////////////////////////////////////////////////////

  /**
   * Generate a static field with the qualified slot names
   * of every test method:
   *   public static String[] tests
   */
  void assembleTests(Assembler asm, Code code)
  {                                
    // generate the field    
    int flags = Jvm.ACC_PUBLIC | Jvm.ACC_STATIC;
    asm.addField(new FieldInfo(asm, "tests", "[Ljava/lang/String;", flags)); 
    
    // get the test methods for this kit
    IrMethod[] tests = FindTestCases.findTestMethods(ir.types, true);
    
    // allocate the String[] and initialize
    code.addIntConst(tests.length);
    code.add(ANEWARRAY, asm.cp.cls("java/lang/String"));
    for (int i=0; i<tests.length; ++i)
    {
      code.add(DUP);
      code.addIntConst(i);
      code.add(LDC, asm.cp.string(tests[i].qname()));
      code.add(AASTORE);
    }                               
    code.add(PUTSTATIC, asm.cp.field(jnameKitConst, "tests", "[Ljava/lang/String;"));
  }            
  
////////////////////////////////////////////////////////////////
// Str Constants
////////////////////////////////////////////////////////////////
  
  /**
   * Add a static field for every Str constant and the
   * code to initialize it.
   */
  void assembleStrConsts(Assembler asm, Code clinit)
  {
    if (strings.size() == 0) return;
    
    int initStr = asm.cp.method("sedona/vm/VmUtil", "strConst", "(Ljava/lang/String;)Lsedona/vm/StrRef;");
    
    Iterator it = strings.keySet().iterator();
    while (it.hasNext())
    {
      String str   = (String)it.next();
      String field = (String)strings.get(str);       
      
      // generate field
      int flags = Jvm.ACC_PUBLIC | Jvm.ACC_STATIC;
      asm.addField(new FieldInfo(asm, field, "Lsedona/vm/StrRef;", flags)); 
      
      // generate initialization
      clinit.add(LDC, asm.cp.string(str));
      clinit.add(INVOKESTATIC, initStr);
      clinit.add(PUTSTATIC, asm.cp.field(jnameKitConst, field, "Lsedona/vm/StrRef;"));
    }                    
  }

////////////////////////////////////////////////////////////////
// Buf Constants
////////////////////////////////////////////////////////////////

  /**
   * Add a static field for every Buf constant and the
   * code to initialize it.
   */
  void assembleBufConsts(Assembler asm, Code clinit)
  {                 
    if (bufs.size() == 0) return;
    
    int initBuf  = asm.cp.method("sedona/vm/VmUtil", "bufConst", "(Ljava/lang/String;)[B");
    int bufCls   = asm.cp.cls("sedona/vm/sys/Buf");      
    int bufCtor  = asm.cp.method(bufCls, "<init>", "()V");
    int bufBytes = asm.cp.field(bufCls, "bytes", "[B");
    int bufSize  = asm.cp.field(bufCls, "size", "S");
    int bufBytesLen = asm.cp.field(bufCls, "bytesLen", "S");
    
    Iterator it = bufs.keySet().iterator();
    while (it.hasNext())
    {
      Buf buf = (Buf)it.next();
      String field = (String)bufs.get(buf);       
      
      // generate field
      int flags = Jvm.ACC_PUBLIC | Jvm.ACC_STATIC;
      asm.addField(new FieldInfo(asm, field, "Lsedona/vm/sys/Buf;", flags)); 
      
      // construct Buf
      clinit.add(NEW, bufCls);
      clinit.add(DUP);                                                 
      clinit.add(INVOKESPECIAL, bufCtor);
      
      // init bytes
      clinit.add(DUP);      
      clinit.add(LDC, asm.cp.string(buf.encodeString()));
      clinit.add(INVOKESTATIC, initBuf);
      clinit.add(PUTFIELD, bufBytes);

      // init length
      clinit.add(DUP);      
      clinit.addIntConst(buf.size);
      clinit.add(PUTFIELD, bufBytesLen);

      // init size
      clinit.add(DUP);      
      clinit.addIntConst(buf.size);
      clinit.add(PUTFIELD, bufSize);
      
      // store to field
      clinit.add(PUTSTATIC, asm.cp.field(jnameKitConst, field, "Lsedona/vm/sys/Buf;"));
    }                    
  }

////////////////////////////////////////////////////////////////
// Log Constants
////////////////////////////////////////////////////////////////

  /**
   * Add a static field for every Log constant and the code to 
   * initialize it.  Note that logs are defined as a static field 
   * on *both* KitConst *and* the original parent type.  We use 
   * reflection on KitConst during runtime to initialize all the 
   * logs. 
   */
  void assembleLogConsts(Assembler asm, Code clinit)
  {     
    int logCls  = asm.cp.cls("sedona/vm/sys/Log");      
    int logCtor = asm.cp.method(logCls, "<init>", "()V");
    for (int i=0; i<logs.size(); ++i)
    {
      IrField f = (IrField)logs.get(i);  
      
      // allocate the Log instance
      clinit.add(NEW, logCls);
      clinit.add(DUP);
      clinit.add(DUP);
      clinit.add(INVOKESPECIAL, logCtor);
      
      // store to definition field
      clinit.add(PUTSTATIC, JavaClassAsm.fieldRef(asm, f));
      
      // also store to KitConst.log_<type>_<slot>
      int flags = Jvm.ACC_PUBLIC | Jvm.ACC_STATIC;
      String myField = "log_" + f.parent().name() + "_" + f.name();
      asm.addField(new FieldInfo(asm, myField, "Lsedona/vm/sys/Log;", flags)); 
      clinit.add(PUTSTATIC, asm.cp.field(jnameKitConst, myField, "Lsedona/vm/sys/Log;"));
    }
  }
        
////////////////////////////////////////////////////////////////
// Load Constants
////////////////////////////////////////////////////////////////
  
  /**
   * Generate the code to load a Str constant and
   * put it our table for KitConst generation.
   */ 
  void loadStr(Code code, String str)
  {    
    if (str == null) throw new IllegalStateException();
           
    // map string const to kit field name             
    String fieldName = (String)strings.get(str);
    if (fieldName == null)
    {
      fieldName = "str" + strings.size();
      strings.put(str, fieldName);
    }                  
    
    code.add(GETSTATIC, code.cp.field(jnameKitConst, fieldName, "Lsedona/vm/StrRef;"));
  }

  /**
   * Generate the code to load a Buf constant and
   * put it our table for KitConst generation.
   */ 
  void loadBuf(Code code, Buf buf)
  {    
    if (buf == null) throw new IllegalStateException();
    
    // map string const to kit field name             
    String fieldName = (String)bufs.get(buf);
    if (fieldName == null)
    {
      fieldName = "buf" + bufs.size();
      bufs.put(buf, fieldName);
    }                  
    
    code.add(GETSTATIC, code.cp.field(jnameKitConst, fieldName, 
      "Lsedona/vm/sys/Buf;"));
  }

  /**
   * Generate the code to load a Type constant.
   */ 
  void loadType(Code code, Type t)
  {             
    if (t == null) throw new IllegalStateException();
    
    String kit = t.isPrimitive() ? "sys" : t.kit().name();
    String parent = "sedona/vm/" + kit + "/KitConst";
    String field = "type" + t.name();
    String of    = "Lsedona/vm/sys/Type;";
    code.add(GETSTATIC, code.cp.field(parent, field, of));
  }

  /**
   * Generate the code to load a Slot constant.
   */ 
  void loadSlot(Code code, Slot s)
  {                                                                   
    if (s == null) throw new IllegalStateException();
    
    String parent = "sedona/vm/" + s.parent().kit().name() + "/KitConst";
    String field = "slot" + s.parent().name() + "_" + s.name();
    String of = "Lsedona/vm/sys/Slot;";
    code.add(GETSTATIC, code.cp.field(parent, field, of));
  }
  
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  KitDef ast = compiler.ast;
  IrKit ir = compiler.ir;
  final String jnameKitConst;
  final String jnameBootstrap;
  boolean isSys;
  ArrayList classes = new ArrayList();
  HashMap strings = new HashMap();   // String const -> field name
  HashMap bufs    = new HashMap();   // Buf const -> field name
  ArrayList logs  = new ArrayList(); // IrFields which are logs

}

