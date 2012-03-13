//
// Copyright (c) 2000 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Mar 00  Brian Frank  Creation
//
package sedonac.jasm;

/**
 * FieldInfo
 */
public class FieldInfo
  extends MemberInfo
{  

  public FieldInfo(Assembler asm, int name, int type, int accessFlags)
  {
    super(asm, name, type, accessFlags);
  }

  public FieldInfo(Assembler asm, String name, String type, int accessFlags)
  {
    super(asm, name, type, accessFlags);
  }

}
