//
// Copyright (c) 1997  Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   25 Jun 97  Brian Frank  Creation
//
package sedonac.jasm;

/**
 * Jvm contains constants for VM classfiles.
 */
public final class Jvm
  implements OpCodes
{

//////////////////////////////////////////////////////////////
// Header Constants
//////////////////////////////////////////////////////////////

  public static final int MAGIC = 0xCAFEBABE;
  public static final int MINOR_VERSION = 3;
  public static final int MAJOR_VERSION = 45;

//////////////////////////////////////////////////////////////
// Access Flags Constants
//////////////////////////////////////////////////////////////

  public static final int ACC_PUBLIC       = 0x0001;
  public static final int ACC_PRIVATE      = 0x0002;
  public static final int ACC_PROTECTED    = 0x0004;
  public static final int ACC_STATIC       = 0x0008;
  public static final int ACC_FINAL        = 0x0010;
  public static final int ACC_SUPER        = 0x0020;   // reused
  public static final int ACC_SYNCHRONIZED = 0x0020;   //
  public static final int ACC_VOLATILE     = 0x0040;
  public static final int ACC_TRANSIENT    = 0x0080;
  public static final int ACC_NATIVE       = 0x0100;
  public static final int ACC_INTERFACE    = 0x0200;
  public static final int ACC_ABSTRACT     = 0x0400;

//////////////////////////////////////////////////////////////
// Constant Pool Constants
//////////////////////////////////////////////////////////////

  public static final int CONSTANT_Utf8               = 1;
  public static final int CONSTANT_Integer            = 3;
  public static final int CONSTANT_Float              = 4;
  public static final int CONSTANT_Long               = 5;
  public static final int CONSTANT_Double             = 6;
  public static final int CONSTANT_Class              = 7;
  public static final int CONSTANT_String             = 8;
  public static final int CONSTANT_Fieldref           = 9;
  public static final int CONSTANT_Methodref          = 10;
  public static final int CONSTANT_InterfaceMethodref = 11;
  public static final int CONSTANT_NameAndType        = 12;

//////////////////////////////////////////////////////////////
// Attribute Keyword Constants
//////////////////////////////////////////////////////////////

  public static final String ATTR_SOURCE_FILE          = "SourceFile";
  public static final String ATTR_CONSTANT_VALUE       = "ConstantValue";
  public static final String ATTR_CODE                 = "Code";
  public static final String ATTR_EXCEPTIONS           = "Exceptions";
  public static final String ATTR_LINE_NUMBER_TABLE    = "LineNumberTable";
  public static final String ATTR_LOCAL_VARIABLE_TABLE = "LocalVariableTable";
  public static final String ATTR_INNER_CLASSES        = "InnerClasses";
  public static final String ATTR_SYNTHETIC            = "Synthetic";
  public static final String ATTR_DEPRECATED           = "Deprecated";

//////////////////////////////////////////////////////////////
// Array Type Constants
//////////////////////////////////////////////////////////////

  public static final int T_BOOLEAN = 4;
  public static final int T_CHAR    = 5;
  public static final int T_FLOAT   = 6;
  public static final int T_DOUBLE  = 7;
  public static final int T_BYTE    = 8;
  public static final int T_SHORT   = 9;
  public static final int T_INT     = 10;
  public static final int T_LONG    = 11;

  /**
   * Return a field type descriptor based
   * on a class type.
   * FieldType:
   *   BaseType
   *   ObjectType
   *   ArrayType
   * BaseType:
   *   B   byte
   *   C   char
   *   D   double
   *   F   float
   *   I   int
   *   J   long
   *   S   short
   *   Z   boolean
   *   V   void
   * ObjectType:
   *   L <classname> ;
   * ArrayType
   *   [ FieldType
   */
  public static String fieldDescriptor(Class cls)
  {
    // handle base types
    if (cls.isPrimitive())
    {
      if (cls.equals(byte.class))    return "B";
      if (cls.equals(char.class))    return "C";
      if (cls.equals(double.class))  return "D";
      if (cls.equals(float.class))   return "F";
      if (cls.equals(int.class))     return "I";
      if (cls.equals(long.class))    return "J";
      if (cls.equals(short.class))   return "S";
      if (cls.equals(boolean.class)) return "Z";
      if (cls.equals(void.class))    return "V";
      throw new IllegalStateException();
    }
    else
    {
      // handle array type
      if (cls.isArray())
        return "[" + fieldDescriptor(cls.getComponentType());

      // must be a object type
      return "L" + cls.getName().replace('.', '/') + ";";
    }
  }

  /**
   * Return a descriptor string used to describe
   * the signature of a method.  This descriptor
   * takes the format:
   * MethodDescriptor
   *   ( ParameterDescritor* ) ReturnDescriptor
   * ParameterDescriptor
   *   FieldType (excluding void)
   * ReturnDescriptor
   *   FieldType (including void)
   *
   * Example:
   *     Object myMethod(int i, double d, Thread t)
   *   evaluates to
   *     (IDLjava/lang/Thread;)Ljava/lang/Object;
   *
   * @param paramTypes array of parameter
   *    types, you may pass null instead of
   *    an array of length 0, if there are no
   *    parameters to the method
   * @param returnType class of return type
   *    or void.class if no return type.
   */
  public static String methodDescriptor(Class[] paramTypes, Class returnType)
  {
    StringBuffer s = new StringBuffer("(");

    if (paramTypes != null)
      for(int i=0; i<paramTypes.length; ++i)
        s.append(fieldDescriptor(paramTypes[i]));

    s.append(")").append(fieldDescriptor(returnType));
    return s.toString();
  }

////////////////////////////////////////////////////////////////
// Opcode Arguments
////////////////////////////////////////////////////////////////

  static final byte NONE = 0;
  static final byte U1   = 1;
  static final byte U2   = 2;
  static final byte B2   = 3;
  static final byte B4   = 4;
  static final byte SPECIAL = 5;
  
  static final byte[] OPCODE_ARGS =
  {
    NONE,       // NOP         = 0x00
    NONE,       // ACONST_NULL = 0x01
    NONE,       // ICONST_M1   = 0x02
    NONE,       // ICONST_0    = 0x03
    NONE,       // ICONST_1    = 0x04
    NONE,       // ICONST_2    = 0x05
    NONE,       // ICONST_3    = 0x06
    NONE,       // ICONST_4    = 0x07
    NONE,       // ICONST_5    = 0x08
    NONE,       // LCONST_0    = 0x09
    NONE,       // LCONST_1    = 0x0A
    NONE,       // FCONST_0    = 0x0B
    NONE,       // FCONST_1    = 0x0C
    NONE,       // FCONST_2    = 0x0D
    NONE,       // DCONST_0    = 0x0E
    NONE,       // DCONST_1    = 0x0F
    U1,         // BIPUSH      = 0x10
    U2,         // SIPUSH      = 0x11
    U1,         // LDC         = 0x12
    U2,         // LDC_W       = 0x13
    U2,         // LDC2_W      = 0x14
    U1,         // ILOAD       = 0x15
    U1,         // LLOAD       = 0x16
    U1,         // FLOAD       = 0x17
    U1,         // DLOAD       = 0x18
    U1,         // ALOAD       = 0x19
    NONE,       // ILOAD_0     = 0x1A
    NONE,       // ILOAD_1     = 0x1B
    NONE,       // ILOAD_2     = 0x1C
    NONE,       // ILOAD_3     = 0x1D
    NONE,       // LLOAD_0     = 0x1E
    NONE,       // LLOAD_1     = 0x1F
    NONE,       // LLOAD_2     = 0x20
    NONE,       // LLOAD_3     = 0x21
    NONE,       // FLOAD_0     = 0x22
    NONE,       // FLOAD_1     = 0x23
    NONE,       // FLOAD_2     = 0x24
    NONE,       // FLOAD_3     = 0x25
    NONE,       // DLOAD_0     = 0x26
    NONE,       // DLOAD_1     = 0x27
    NONE,       // DLOAD_2     = 0x28
    NONE,       // DLOAD_3     = 0x29
    NONE,       // ALOAD_0     = 0x2A
    NONE,       // ALOAD_1     = 0x2B
    NONE,       // ALOAD_2     = 0x2C
    NONE,       // ALOAD_3     = 0x2D
    NONE,       // IALOAD      = 0x2E
    NONE,       // LALOAD      = 0x2F
    NONE,       // FALOAD      = 0x30
    NONE,       // DALOAD      = 0x31
    NONE,       // AALOAD      = 0x32
    NONE,       // BALOAD      = 0x33
    NONE,       // CALOAD      = 0x34
    NONE,       // SALOAD      = 0x35
    U1,         // ISTORE      = 0x36
    U1,         // LSTORE      = 0x37

    U1,         // FSTORE      = 0x38
    U1,         // DSTORE      = 0x39
    U1,         // ASTORE      = 0x3A
    NONE,       // ISTORE_0    = 0x3B
    NONE,       // ISTORE_1    = 0x3C
    NONE,       // ISTORE_2    = 0x3D
    NONE,       // ISTORE_3    = 0x3E
    NONE,       // LSTORE_0    = 0x3F
    NONE,       // LSTORE_1    = 0x40
    NONE,       // LSTORE_2    = 0x41
    NONE,       // LSTORE_3    = 0x42
    NONE,       // FSTORE_0    = 0x43
    NONE,       // FSTORE_1    = 0x44
    NONE,       // FSTORE_2    = 0x45
    NONE,       // FSTORE_3    = 0x46
    NONE,       // DSTORE_0    = 0x47
    NONE,       // DSTORE_1    = 0x48
    NONE,       // DSTORE_2    = 0x49
    NONE,       // DSTORE_3    = 0x4A
    NONE,       // ASTORE_0    = 0x4B
    NONE,       // ASTORE_1    = 0x4C
    NONE,       // ASTORE_2    = 0x4D
    NONE,       // ASTORE_3    = 0x4E
    NONE,       // IASTORE     = 0x4F
    NONE,       // LASTORE     = 0x50
    NONE,       // FASTORE     = 0x51
    NONE,       // DASTORE     = 0x52
    NONE,       // AASTORE     = 0x53
    NONE,       // BASTORE     = 0x54
    NONE,       // CASTORE     = 0x55
    NONE,       // SASTORE     = 0x56
    NONE,       // POP         = 0x57
    NONE,       // POP2        = 0x58
    NONE,       // DUP         = 0x59
    NONE,       // DUP_X1      = 0x5A
    NONE,       // DUP_X2      = 0x5B
    NONE,       // DUP2        = 0x5C
    NONE,       // DUP2_X1     = 0x5D
    NONE,       // DUP2_X2     = 0x5E
    NONE,       // SWAP        = 0x5F
    NONE,       // IADD        = 0x60

    NONE,       // LADD        = 0x61
    NONE,       // FADD        = 0x62
    NONE,       // DADD        = 0x63
    NONE,       // ISUB        = 0x64
    NONE,       // LSUB        = 0x65
    NONE,       // FSUB        = 0x66
    NONE,       // DSUB        = 0x67
    NONE,       // IMUL        = 0x68
    NONE,       // LMUL        = 0x69
    NONE,       // FMUL        = 0x6A
    NONE,       // DMUL        = 0x6B
    NONE,       // IDIV        = 0x6C
    NONE,       // LDIV        = 0x6D
    NONE,       // FDIV        = 0x6E
    NONE,       // DDIV        = 0x6F
    NONE,       // IREM        = 0x70
    NONE,       // LREM        = 0x71
    NONE,       // FREM        = 0x72
    NONE,       // DREM        = 0x73
    NONE,       // INEG        = 0x74
    NONE,       // LNEG        = 0x75
    NONE,       // FNEG        = 0x76
    NONE,       // DNEG        = 0x77
    NONE,       // ISHL        = 0x78
    NONE,       // LSHL        = 0x79
    NONE,       // ISHR        = 0x7A
    NONE,       // LSHR        = 0x7B
    NONE,       // IUSHR       = 0x7C
    NONE,       // LUSHR       = 0x7D
    NONE,       // IAND        = 0x7E
    NONE,       // LAND        = 0x7F
    NONE,       // IOR         = 0x80
    NONE,       // LOR         = 0x81
    NONE,       // IXOR        = 0x82
    NONE,       // LXOR        = 0x83
    U2,         // IINC        = 0x84
    NONE,       // I2L         = 0x85
    NONE,       // I2F         = 0x86
    NONE,       // I2D         = 0x87
    NONE,       // L2I         = 0x88
    NONE,       // L2F         = 0x89

    NONE,       // L2D         = 0x8A
    NONE,       // F2I         = 0x8B
    NONE,       // F2L         = 0x8C
    NONE,       // F2D         = 0x8D
    NONE,       // D2I         = 0x8E
    NONE,       // D2L         = 0x8F
    NONE,       // D2F         = 0x90
    NONE,       // I2B         = 0x91
    NONE,       // I2C         = 0x92
    NONE,       // I2S         = 0x93
    NONE,       // LCMP        = 0x94
    NONE,       // FCMPL       = 0x95
    NONE,       // FCMPG       = 0x96
    NONE,       // DCMPL       = 0x97
    NONE,       // DCMPG       = 0x98
    B2,         // IFEQ        = 0x99
    B2,         // IFNE        = 0x9A
    B2,         // IFLT        = 0x9B
    B2,         // IFGE        = 0x9C
    B2,         // IFGT        = 0x9D
    B2,         // IFLE        = 0x9E
    B2,         // IF_ICMPEQ   = 0x9F
    B2,         // IF_ICMPNE   = 0xA0
    B2,         // IF_ICMPLT   = 0xA1
    B2,         // IF_ICMPGE   = 0xA2
    B2,         // IF_ICMPGT   = 0xA3
    B2,         // IF_ICMPLE   = 0xA4
    B2,         // IF_ACMPEQ   = 0xA5
    B2,         // IF_ACMPNU   = 0xA6
    B2,         // GOTO        = 0xA7
    B2,         // JSR         = 0xA8
    U1,         // RET         = 0xA9
    NONE,       // TABLESWITCH = 0xAA
    NONE,       // LOOKUPSWITCH = 0xAB
    NONE,       // IRETURN     = 0xAC
    NONE,       // LRETURN     = 0xAD
    NONE,       // FRETURN     = 0xAE
    NONE,       // DRETURN     = 0xAF
    NONE,       // ARETURN     = 0xB0
    NONE,       // RETURN      = 0xB1
    U2,         // GETSTATIC   = 0xB2

    U2,         // PUTSTATIC      = 0xB3
    U2,         // GETFIELD       = 0xB4
    U2,         // PUTFIELD       = 0xB5
    U2,         // INVOKEVIRTUAL  = 0xB6
    U2,         // INVOKESPECIAL  = 0xB7
    U2,         // INVOKESTATIC   = 0xB8
    SPECIAL,    // INVOKEINTERFACE= 0xB9
    NONE,       // XXX_UNUSED_XXX = 0xBA
    U2,         // NEW            = 0xBB
    U1,         // NEWARRAY       = 0xBC
    U2,         // ANEWARRAY      = 0xBD
    NONE,       // ARRAYLENGTH    = 0xBE
    NONE,       // ATHROW         = 0xBF
    U2,         // CHECKCAST      = 0xC0
    U2,         // INSTANCEOF     = 0xC1
    NONE,       // MONITORENTER   = 0xC2
    NONE,       // MONITOREXIT    = 0xC3
    NONE,       // WIDE           = 0xC4
    NONE,       // MULTIANEWARRAY = 0xC5
    B2,         // IFNULL         = 0xC6
    B2,         // IFNONNULL      = 0xC7
    B4,         // GOTO_W         = 0xC8
    B4          // JSR_W          = 0xC9
  };

}

