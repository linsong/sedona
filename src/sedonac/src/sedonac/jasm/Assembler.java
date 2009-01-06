//
// Copyright (c) 2000 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 00  Brian Frank  Creation
//
package sedonac.jasm;

import java.util.ArrayList;

/**
 * Assembler
 */
public class Assembler
{  

  public Assembler(String thisClass, String superClass, 
                   int accessFlags, String[] interfaces)
  {
    this.thisClass = cp.cls(thisClass);    
    
    this.superClass = (superClass == null) ?
      cp.cls("java/lang/Object") :
      cp.cls(superClass);
    
    this.accessFlags = accessFlags;
    this.interfaces  = new int[interfaces == null ? 0 : interfaces.length];
    for(int i=0; i<this.interfaces.length; ++i)
      this.interfaces[i] = cp.cls(interfaces[i]);
  }
  
  public FieldInfo addField(FieldInfo f) { fields.add(f); return f; }  
  public MethodInfo addMethod(MethodInfo m) { methods.add(m); return m; }  
  public AttributeInfo addAttribute(AttributeInfo a) { attributes.add(a); return a; }
  
  public Buffer compile()
  {
    Buffer buf = new Buffer();
    
    buf.u4(Jvm.MAGIC);          // magic
    buf.u2(Jvm.MINOR_VERSION);  // minor version
    buf.u2(Jvm.MAJOR_VERSION);  // major version
    buf.u2(cp.count+1);         // constant pool count
    buf.append(cp.buf);         // contant pool
    buf.u2(accessFlags);        // access flags
    buf.u2(thisClass);          // this class
    buf.u2(superClass);         // super class
    
    // interfaces
    buf.u2(interfaces.length); 
    for(int i=0; i<interfaces.length; ++i) 
      buf.u2(interfaces[i]);
      
    // fields  
    buf.u2(fields.size());
    for(int i=0; i<fields.size(); ++i)
      ((FieldInfo)fields.get(i)).compile(buf);
    
    // methods
    buf.u2(methods.size()); 
    for(int i=0; i<methods.size(); ++i)
      ((MethodInfo)methods.get(i)).compile(buf);
    
    // attributes  
    buf.u2(attributes.size());
    for(int i=0; i<attributes.size(); ++i)
      ((AttributeInfo)attributes.get(i)).compile(buf);
    
    return buf;
  }

  public final int thisClass;
  public final int superClass;
  public final int[] interfaces;
  public final int accessFlags;
  public final ConstantPool cp = new ConstantPool();
  
  private ArrayList fields = new ArrayList();
  private ArrayList methods = new ArrayList();
  private ArrayList attributes = new ArrayList();
  
}
