//
// Copyright (c) 1997 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   29 Jun 97  Brian Frank  Creation
//
package sedonac.jasm;

/**
 * The OpCodes interface defines the opcode constants.
 */
public interface OpCodes
{

  public static final int NOP            = 0x00;
  public static final int ACONST_NULL    = 0x01;
  public static final int ICONST_M1      = 0x02;
  public static final int ICONST_0       = 0x03;
  public static final int ICONST_1       = 0x04;
  public static final int ICONST_2       = 0x05;
  public static final int ICONST_3       = 0x06;
  public static final int ICONST_4       = 0x07;
  public static final int ICONST_5       = 0x08;
  public static final int LCONST_0       = 0x09;
  public static final int LCONST_1       = 0x0A;
  public static final int FCONST_0       = 0x0B;
  public static final int FCONST_1       = 0x0C;
  public static final int FCONST_2       = 0x0D;
  public static final int DCONST_0       = 0x0E;
  public static final int DCONST_1       = 0x0F;
  public static final int BIPUSH         = 0x10;
  public static final int SIPUSH         = 0x11;
  public static final int LDC            = 0x12;
  public static final int LDC_W          = 0x13;
  public static final int LDC2_W         = 0x14;
  public static final int ILOAD          = 0x15;
  public static final int LLOAD          = 0x16;
  public static final int FLOAD          = 0x17;
  public static final int DLOAD          = 0x18;
  public static final int ALOAD          = 0x19;
  public static final int ILOAD_0        = 0x1A;
  public static final int ILOAD_1        = 0x1B;
  
  public static final int ILOAD_2        = 0x1C;
  public static final int ILOAD_3        = 0x1D;
  public static final int LLOAD_0        = 0x1E;
  public static final int LLOAD_1        = 0x1F;
  public static final int LLOAD_2        = 0x20;
  public static final int LLOAD_3        = 0x21;
  public static final int FLOAD_0        = 0x22;
  public static final int FLOAD_1        = 0x23;
  public static final int FLOAD_2        = 0x24;
  public static final int FLOAD_3        = 0x25;
  public static final int DLOAD_0        = 0x26;
  public static final int DLOAD_1        = 0x27;
  public static final int DLOAD_2        = 0x28;
  public static final int DLOAD_3        = 0x29;
  public static final int ALOAD_0        = 0x2A;
  public static final int ALOAD_1        = 0x2B;
  public static final int ALOAD_2        = 0x2C;
  public static final int ALOAD_3        = 0x2D;
  public static final int IALOAD         = 0x2E;
  public static final int LALOAD         = 0x2F;
  public static final int FALOAD         = 0x30;
  public static final int DALOAD         = 0x31;
  public static final int AALOAD         = 0x32;
  public static final int BALOAD         = 0x33;
  public static final int CALOAD         = 0x34;
  public static final int SALOAD         = 0x35;
  public static final int ISTORE         = 0x36;
  public static final int LSTORE         = 0x37;
                                         
  public static final int FSTORE         = 0x38;
  public static final int DSTORE         = 0x39;
  public static final int ASTORE         = 0x3A;
  public static final int ISTORE_0       = 0x3B;
  public static final int ISTORE_1       = 0x3C;
  public static final int ISTORE_2       = 0x3D;
  public static final int ISTORE_3       = 0x3E;
  public static final int LSTORE_0       = 0x3F;
  public static final int LSTORE_1       = 0x40;
  public static final int LSTORE_2       = 0x41;
  public static final int LSTORE_3       = 0x42;
  public static final int FSTORE_0       = 0x43;
  public static final int FSTORE_1       = 0x44;
  public static final int FSTORE_2       = 0x45;
  public static final int FSTORE_3       = 0x46;
  public static final int DSTORE_0       = 0x47;
  public static final int DSTORE_1       = 0x48;
  public static final int DSTORE_2       = 0x49;
  public static final int DSTORE_3       = 0x4A;
  public static final int ASTORE_0       = 0x4B;
  public static final int ASTORE_1       = 0x4C;
  public static final int ASTORE_2       = 0x4D;
  public static final int ASTORE_3       = 0x4E;
  public static final int IASTORE        = 0x4F;
  public static final int LASTORE        = 0x50;
  public static final int FASTORE        = 0x51;
  public static final int DASTORE        = 0x52;
  public static final int AASTORE        = 0x53;
  public static final int BASTORE        = 0x54;
  public static final int CASTORE        = 0x55;
  public static final int SASTORE        = 0x56;
  public static final int POP            = 0x57;
  public static final int POP2           = 0x58;
  public static final int DUP            = 0x59;
  public static final int DUP_X1         = 0x5A;
  public static final int DUP_X2         = 0x5B;
  public static final int DUP2           = 0x5C;
  public static final int DUP2_X1        = 0x5D;
  public static final int DUP2_X2        = 0x5E;
  public static final int SWAP           = 0x5F;
  public static final int IADD           = 0x60;

  public static final int LADD           = 0x61;
  public static final int FADD           = 0x62;
  public static final int DADD           = 0x63;
  public static final int ISUB           = 0x64;
  public static final int LSUB           = 0x65;
  public static final int FSUB           = 0x66;
  public static final int DSUB           = 0x67;
  public static final int IMUL           = 0x68;
  public static final int LMUL           = 0x69;
  public static final int FMUL           = 0x6A;
  public static final int DMUL           = 0x6B;
  public static final int IDIV           = 0x6C;
  public static final int LDIV           = 0x6D;
  public static final int FDIV           = 0x6E;
  public static final int DDIV           = 0x6F;
  public static final int IREM           = 0x70;
  public static final int LREM           = 0x71;
  public static final int FREM           = 0x72;
  public static final int DREM           = 0x73;
  public static final int INEG           = 0x74;
  public static final int LNEG           = 0x75;
  public static final int FNEG           = 0x76;
  public static final int DNEG           = 0x77;
  public static final int ISHL           = 0x78;
  public static final int LSHL           = 0x79;
  public static final int ISHR           = 0x7A;
  public static final int LSHR           = 0x7B;
  public static final int IUSHR          = 0x7C;
  public static final int LUSHR          = 0x7D;
  public static final int IAND           = 0x7E;
  public static final int LAND           = 0x7F;
  public static final int IOR            = 0x80;
  public static final int LOR            = 0x81;
  public static final int IXOR           = 0x82;
  public static final int LXOR           = 0x83;
  public static final int IINC           = 0x84;
  public static final int I2L            = 0x85;
  public static final int I2F            = 0x86;
  public static final int I2D            = 0x87;
  public static final int L2I            = 0x88;
  public static final int L2F            = 0x89;

  public static final int L2D            = 0x8A;
  public static final int F2I            = 0x8B;
  public static final int F2L            = 0x8C;
  public static final int F2D            = 0x8D;
  public static final int D2I            = 0x8E;
  public static final int D2L            = 0x8F;
  public static final int D2F            = 0x90;
  public static final int I2B            = 0x91;
  public static final int I2C            = 0x92;
  public static final int I2S            = 0x93;
  public static final int LCMP           = 0x94;
  public static final int FCMPL          = 0x95;
  public static final int FCMPG          = 0x96;
  public static final int DCMPL          = 0x97;
  public static final int DCMPG          = 0x98;
  public static final int IFEQ           = 0x99;
  public static final int IFNE           = 0x9A;
  public static final int IFLT           = 0x9B;
  public static final int IFGE           = 0x9C;
  public static final int IFGT           = 0x9D;
  public static final int IFLE           = 0x9E;
  public static final int IF_ICMPEQ      = 0x9F;
  public static final int IF_ICMPNE      = 0xA0;
  public static final int IF_ICMPLT      = 0xA1;
  public static final int IF_ICMPGE      = 0xA2;
  public static final int IF_ICMPGT      = 0xA3;
  public static final int IF_ICMPLE      = 0xA4;
  public static final int IF_ACMPEQ      = 0xA5;
  public static final int IF_ACMPNE      = 0xA6;
  public static final int GOTO           = 0xA7;
  public static final int JSR            = 0xA8;
  public static final int RET            = 0xA9;
  public static final int TABLESWITCH    = 0xAA;
  public static final int LOOKUPSWITCH   = 0xAB;
  public static final int IRETURN        = 0xAC;
  public static final int LRETURN        = 0xAD;
  public static final int FRETURN        = 0xAE;
  public static final int DRETURN        = 0xAF;
  public static final int ARETURN        = 0xB0;
  public static final int RETURN         = 0xB1;
  public static final int GETSTATIC      = 0xB2;

  public static final int PUTSTATIC      = 0xB3;
  public static final int GETFIELD       = 0xB4;
  public static final int PUTFIELD       = 0xB5;
  public static final int INVOKEVIRTUAL  = 0xB6;
  public static final int INVOKESPECIAL  = 0xB7;
  public static final int INVOKESTATIC   = 0xB8;
  public static final int INVOKEINTERFACE= 0xB9;
  public static final int XXX_UNUSED_XXX = 0xBA;
  public static final int NEW            = 0xBB;
  public static final int NEWARRAY       = 0xBC;
  public static final int ANEWARRAY      = 0xBD;
  public static final int ARRAYLENGTH    = 0xBE;
  public static final int ATHROW         = 0xBF;
  public static final int CHECKCAST      = 0xC0;
  public static final int INSTANCEOF     = 0xC1;
  public static final int MONITORENTER   = 0xC2;
  public static final int MONITOREXIT    = 0xC3;
  public static final int WIDE           = 0xC4;
  public static final int MULTIANEWARRAY = 0xC5;
  public static final int IFNULL         = 0xC6;
  public static final int IFNONNULL      = 0xC7;
  public static final int GOTO_W         = 0xC8;
  public static final int JSR_W          = 0xC9;

}                                        

