//
// Copyright (c) 2000  Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 00  Brian Frank  Creation
//
package sedonac.jasm;

/**
 * MethodInfo
 */
public class MethodInfo
  extends MemberInfo
{  

  public MethodInfo(Assembler asm, int name, int type, int accessFlags)
  {
    super(asm, name, type, accessFlags);
  }

  public MethodInfo(Assembler asm, String name, String type, int accessFlags)
  {
    super(asm, name, type, accessFlags);
  }

  public MethodInfo(Assembler asm, int name, int type, int accessFlags, Code code)
  {
    super(asm, name, type, accessFlags);
    addAttribute(code);
  }

  public MethodInfo(Assembler asm, int name, String type, int accessFlags, Code code)
  {
    super(asm, name, type, accessFlags);
    addAttribute(code);
  }

  public MethodInfo(Assembler asm, String name, String type, int accessFlags, Code code)
  {
    super(asm, name, type, accessFlags);
    addAttribute(code);
  }
}
