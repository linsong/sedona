//
// Copyright (c) 2000  Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 00  Brian Frank  Creation
//
package sedonac.jasm;

import java.util.*;

/**
 * MemberInfo
 */
public abstract class MemberInfo
{  

////////////////////////////////////////////////////////////////
// Constructors
////////////////////////////////////////////////////////////////

  MemberInfo(Assembler asm, int name, int type, int accessFlags)
  {
    this.asm = asm;
    this.cp = asm.cp;
    this.name = name;
    this.type = type;
    this.accessFlags = accessFlags;
  }

  MemberInfo(Assembler asm, String name, String type, int accessFlags)
  {
    this.asm = asm;
    this.cp = asm.cp;
    this.name = cp.utf(name);
    this.type = cp.utf(type);
    this.accessFlags = accessFlags;
  }

  MemberInfo(Assembler asm, int name, String type, int accessFlags)
  {
    this.asm = asm;
    this.cp = asm.cp;
    this.name = name;
    this.type = cp.utf(type);
    this.accessFlags = accessFlags;
  }

////////////////////////////////////////////////////////////////
// Attributes
////////////////////////////////////////////////////////////////  

  public void addAttribute(AttributeInfo ai)
  {
    if (attributes == null) attributes = new ArrayList(5);
    attributes.add(ai);
  }

////////////////////////////////////////////////////////////////
// Compile
////////////////////////////////////////////////////////////////  

  void compile(Buffer buf)
  {
    int attrLen = attributes == null ? 0 : attributes.size();
    
    buf.u2(accessFlags);
    buf.u2(name);
    buf.u2(type);
    buf.u2(attrLen);
    for(int i=0; i<attrLen; ++i)
      ((AttributeInfo)attributes.get(i)).compile(buf);
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public final Assembler asm;
  public final ConstantPool cp;
  public final int name;
  public final int type;
  public final int accessFlags;
  ArrayList attributes;
}
