//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Aug 06  Brian Frank  Creation
//

package sedonac.parser;

import java.lang.reflect.*;
import java.util.*;
import sedona.util.*;
import sedonac.*;
import sedonac.ast.*;
import sedonac.namespace.Type;
import sedonac.namespace.Namespace;

/**
 * Tokens for sedona parser
 */
public class Token
{

////////////////////////////////////////////////////////////////
// Token Types
////////////////////////////////////////////////////////////////

  public static final int EOF            = 0;
  public static final int ID             = 1;
  public static final int DOC_COMMENT    = 2;
  public static final int INT_LITERAL    = 3;
  public static final int LONG_LITERAL   = 4;
  public static final int FLOAT_LITERAL  = 5;
  public static final int DOUBLE_LITERAL = 6;
  public static final int STR_LITERAL    = 7;
  public static final int TIME_LITERAL   = 8;
  public static final int BUF_LITERAL    = 9;

  public static final int DOT            = 10;  //  .
  public static final int COMMA          = 11;  //  ,
  public static final int SEMICOLON      = 12;  //  ;
  public static final int COLON          = 13;  //  :
  public static final int PLUS           = 14;  //  +
  public static final int MINUS          = 15;  //  -
  public static final int STAR           = 16;  //  *
  public static final int SLASH          = 17;  //  /
  public static final int PERCENT        = 18;  //  %
  public static final int AMP            = 19;  //  &
  public static final int PIPE           = 20;  //  |
  public static final int DOUBLE_AMP     = 21;  //  &&
  public static final int DOUBLE_PIPE    = 22;  //  ||
  public static final int DOUBLE_COLON   = 23;  //  ::
  public static final int CARET          = 24;  //  ^
  public static final int QUESTION       = 25;  //  ?
  public static final int BANG           = 26;  //  !
  public static final int TILDE          = 27;  //  ~
  public static final int AT             = 28;  //  @
  public static final int LSHIFT         = 29;  //  <<
  public static final int RSHIFT         = 30;  //  >>
  public static final int EQ             = 31;  //  ==
  public static final int NOT_EQ         = 32;  //  !=
  public static final int GT             = 33;  //  >
  public static final int GT_EQ          = 34;  //  >=
  public static final int LT             = 35;  //  <
  public static final int LT_EQ          = 36;  //  <=
  public static final int INCREMENT      = 37;  //  ++
  public static final int DECREMENT      = 38;  //  --
  public static final int ARROW          = 39;  //  ->
  public static final int LBRACE         = 40;  //  {
  public static final int RBRACE         = 41;  //  }
  public static final int LBRACKET       = 42;  //  [
  public static final int RBRACKET       = 43;  //  ]
  public static final int LPAREN         = 44;  //  (
  public static final int RPAREN         = 45;  //  )
  public static final int ASSIGN         = 46;  //  =
  public static final int ASSIGN_PLUS    = 47;  //  +=
  public static final int ASSIGN_MINUS   = 48;  //  -=
  public static final int ASSIGN_STAR    = 49;  //  *=
  public static final int ASSIGN_SLASH   = 50;  //  /=
  public static final int ASSIGN_PERCENT = 51;  //  %=
  public static final int ASSIGN_AMP     = 52;  //  &=
  public static final int ASSIGN_PIPE    = 53;  //  |=
  public static final int ASSIGN_CARET   = 54;  //  ^=
  public static final int ASSIGN_LSHIFT  = 55;  //  <<=
  public static final int ASSIGN_RSHIFT  = 56;  //  >>=
  public static final int SAFE_NAV       = 57;  //  ?.
  public static final int ELVIS          = 58;  //  ?:
  public static final int PROP_ASSIGN    = 59;  //  :=

  public static final int ABSTRACT       = 100;
  public static final int ACTION         = 101;
  public static final int ASSERT         = 102;
  public static final int BOOL           = 103;
  public static final int BREAK          = 104;
  public static final int BYTE           = 105;
  public static final int CASE           = 106;
  public static final int CLASS          = 107;
  public static final int CONST          = 108;
  public static final int CONTINUE       = 109;
  public static final int DEFAULT        = 110;
  public static final int DEFINE         = 111;
  public static final int DELETE         = 112;
  public static final int DOUBLE         = 113;
  public static final int DO             = 114;
  public static final int ELSE           = 115;
  public static final int ENUM           = 116;
  public static final int EXTENDS        = 117;
  public static final int FALSE          = 118;
  public static final int FINAL          = 119;
  public static final int FLOAT          = 120;
  public static final int FOR            = 121;
  public static final int FOREACH        = 122;
  public static final int FUNCTION       = 123;
  public static final int GOTO           = 124;
  public static final int IF             = 125;
  public static final int INLINE         = 126;
  public static final int INT            = 127;
  public static final int INTERNAL       = 128;
  public static final int LONG           = 129;
  public static final int NATIVE         = 130;
  public static final int NEW            = 131;
  public static final int NULL           = 132;
  public static final int OVERRIDE       = 133;
  public static final int PRIVATE        = 134;
  public static final int PROPERTY       = 135;
  public static final int PROTECTED      = 136;
  public static final int PUBLIC         = 137;
  public static final int RETURN         = 138;
  public static final int SHORT          = 139;
  public static final int STATIC         = 140;
  public static final int SUPER          = 141;
  public static final int SWITCH         = 142;
  public static final int THIS           = 143;
  public static final int TRUE           = 144;
  public static final int VIRTUAL        = 145;
  public static final int VOID           = 146;
  public static final int WHILE          = 147;

////////////////////////////////////////////////////////////////
// Constructors
////////////////////////////////////////////////////////////////

  public Token(Location loc, int type)
  {
    this.loc  = loc;
    this.type = type;
  }

  public Token(Location loc, int type, Object value)
  {
    this.loc   = loc;
    this.type  = type;
    this.value = value;
  }

////////////////////////////////////////////////////////////////
// String
////////////////////////////////////////////////////////////////

  public static String toKeyword(int type)
  {
    return (String)idToKeyword.get(new Integer(type));
  }

  public static int fromKeyword(String keyword)
  {
    Integer type = (Integer)keywordToId.get(keyword);
    if (type != null) return type.intValue();
    return -1;
  }

  public boolean isId() { return type == ID; }

  public boolean isKeyword() { return toKeyword(type) != null; }

  public boolean isLiteral()
  {
    switch (type)
    {
      case INT_LITERAL:
      case LONG_LITERAL:
      case FLOAT_LITERAL:
      case DOUBLE_LITERAL:
      case STR_LITERAL:
      case TIME_LITERAL:
      case BUF_LITERAL:
      case FALSE:
      case TRUE:
      case NULL:
        return true;
      default:
        return false;
    }
  }

  public int toUnaryExprId(boolean postfix)
  {
    switch (type)
    {
      case MINUS:     return Expr.NEGATE;
      case BANG:      return Expr.COND_NOT;
      case TILDE:     return Expr.BIT_NOT;
      case INCREMENT: return postfix ? Expr.POST_INCR : Expr.PRE_INCR;
      case DECREMENT: return postfix ? Expr.POST_DECR : Expr.PRE_DECR;
      default:        throw new IllegalStateException(toString());
    }
  }

  public int toBinaryExprId()
  {
    switch (type)
    {
      case PIPE:           return Expr.BIT_OR;
      case CARET:          return Expr.BIT_XOR;
      case AMP:            return Expr.BIT_AND;
      case EQ:             return Expr.EQ;
      case NOT_EQ:         return Expr.NOT_EQ;
      case GT:             return Expr.GT;
      case GT_EQ:          return Expr.GT_EQ;
      case LT:             return Expr.LT;
      case LT_EQ:          return Expr.LT_EQ;
      case LSHIFT:         return Expr.LSHIFT;
      case RSHIFT:         return Expr.RSHIFT;
      case STAR:           return Expr.MUL;
      case SLASH:          return Expr.DIV;
      case PERCENT:        return Expr.MOD;
      case PLUS:           return Expr.ADD;
      case MINUS:          return Expr.SUB;
      case ASSIGN:         return Expr.ASSIGN;
      case ASSIGN_PLUS:    return Expr.ASSIGN_ADD;
      case ASSIGN_MINUS:   return Expr.ASSIGN_SUB;
      case ASSIGN_STAR:    return Expr.ASSIGN_MUL;
      case ASSIGN_SLASH:   return Expr.ASSIGN_DIV;
      case ASSIGN_PERCENT: return Expr.ASSIGN_MOD;
      case ASSIGN_AMP:     return Expr.ASSIGN_BIT_AND;
      case ASSIGN_PIPE:    return Expr.ASSIGN_BIT_OR;
      case ASSIGN_CARET:   return Expr.ASSIGN_BIT_XOR;
      case ASSIGN_LSHIFT:  return Expr.ASSIGN_LSHIFT;
      case ASSIGN_RSHIFT:  return Expr.ASSIGN_RSHIFT;
      case ELVIS:          return Expr.ELVIS;
      case PROP_ASSIGN:    return Expr.PROP_ASSIGN;
      default:             throw new IllegalStateException(toString());
    }
  }

  public boolean isShift()
  {
    return type == LSHIFT || type == RSHIFT;
  }

  public boolean isAssign()
  {
    switch (type)
    {
      case ASSIGN:
      case ASSIGN_PLUS:
      case ASSIGN_MINUS:
      case ASSIGN_STAR:
      case ASSIGN_SLASH:
      case ASSIGN_PERCENT:
      case ASSIGN_AMP:
      case ASSIGN_PIPE:
      case ASSIGN_CARET:
      case ASSIGN_LSHIFT:
      case ASSIGN_RSHIFT:
      case PROP_ASSIGN:
        return true;
      default:
        return false;
    }
  }

  public int assignToBinary()
  {
    switch (type)
    {
      case ASSIGN_PLUS:    return PLUS;
      case ASSIGN_MINUS:   return MINUS;
      case ASSIGN_STAR:    return STAR;
      case ASSIGN_SLASH:   return SLASH;
      case ASSIGN_PERCENT: return PERCENT;
      case ASSIGN_AMP:     return AMP;
      case ASSIGN_PIPE:    return PIPE;
      case ASSIGN_CARET:   return CARET;
      case ASSIGN_LSHIFT:  return LSHIFT;
      case ASSIGN_RSHIFT:  return RSHIFT;
    }                    
    throw new IllegalStateException(toString());
  }

  public Type typeKeyword(Namespace ns)
  {
    switch (type)
    {
      case BOOL:   return ns.boolType;
      case BYTE:   return ns.byteType;
      case SHORT:  return ns.shortType;
      case INT:    return ns.intType;
      case LONG:   return ns.longType;
      case FLOAT:  return ns.floatType;
      case DOUBLE: return ns.doubleType;
      case VOID:   return ns.voidType;
      default:     return null;
    }
  }

  public int    valueToInt()    { return ((Integer)value).intValue(); }
  public long   valueToLong()   { return ((Long)value).longValue(); }
  public float  valueToFloat()  { return ((Float)value).floatValue(); }
  public double valueToDouble() { return ((Double)value).doubleValue(); }

  public String toString()
  {
    switch (type)
    {
      case ID:             return value.toString();
      case DOC_COMMENT:    return "DocComment";
      case INT_LITERAL:    return value.toString();
      case LONG_LITERAL:   return value.toString() + "L";
      case FLOAT_LITERAL:  return value.toString() + "F";
      case DOUBLE_LITERAL: return value.toString() + "D";
      case TIME_LITERAL:   return value.toString() + "ns";
      case STR_LITERAL:    return '"' + TextUtil.toLiteral((String)value) + '"';
      case BUF_LITERAL:    return value.toString();
      default:             return toString(type);
    }
  }

  public static String toString(int type)
  {
    switch (type)
    {
      case EOF:            return "EOF";
      case ID:             return "Identifier";
      case DOC_COMMENT:    return "DocComment";
      case INT_LITERAL:    return "IntLiteral";
      case LONG_LITERAL:   return "LongLiteral";
      case FLOAT_LITERAL:  return "FloatLiteral";
      case DOUBLE_LITERAL: return "DoubleLiteral";
      case STR_LITERAL:    return "StrLiteral";
      case DOT:            return ".";
      case COMMA:          return ",";
      case SEMICOLON:      return ";";
      case COLON:          return ":";
      case PLUS:           return "+";
      case MINUS:          return "-";
      case STAR:           return "*";
      case SLASH:          return "/";
      case PERCENT:        return "%";
      case AMP:            return "&";
      case PIPE:           return "|";
      case DOUBLE_AMP:     return "&&";
      case DOUBLE_PIPE:    return "||";
      case DOUBLE_COLON:   return "::";
      case CARET:          return "^";
      case QUESTION:       return "?";
      case BANG:           return "!";
      case TILDE:          return "~";
      case AT:             return "@";
      case LSHIFT:         return "<<";
      case RSHIFT:         return ">>";
      case EQ:             return "==";
      case NOT_EQ:         return "!=";
      case GT:             return ">";
      case GT_EQ:          return ">=";
      case LT:             return "<";
      case LT_EQ:          return "<=";
      case INCREMENT:      return "++";
      case DECREMENT:      return "--";
      case ARROW:          return "->";
      case LBRACE:         return "{";
      case RBRACE:         return "}";
      case LBRACKET:       return "[";
      case RBRACKET:       return "]";
      case LPAREN:         return "(";
      case RPAREN:         return ")";
      case ASSIGN:         return "=";
      case ASSIGN_PLUS:    return "+=";
      case ASSIGN_MINUS:   return "-=";
      case ASSIGN_STAR:    return "*=";
      case ASSIGN_SLASH:   return "/=";
      case ASSIGN_PERCENT: return "%=";
      case ASSIGN_AMP:     return "&=";
      case ASSIGN_PIPE:    return "|=";
      case ASSIGN_CARET:   return "^=";
      case ASSIGN_LSHIFT:  return "<<=";
      case ASSIGN_RSHIFT:  return ">>=";
      case SAFE_NAV:       return "?.";
      case ELVIS:          return "?:";
      case PROP_ASSIGN:    return ":=";
      default:             return toKeyword(type);
    }
  }

////////////////////////////////////////////////////////////////
// Static Init
////////////////////////////////////////////////////////////////

  private static HashMap idToKeyword = new HashMap();
  private static HashMap keywordToId = new HashMap();
  static
  {
    try
    {
      Field[] fields = Token.class.getFields();
      for (int i=0; i<fields.length; ++i)
      {
        Field f     = fields[i];
        int mods    = f.getModifiers();
        if (Modifier.isStatic(mods) && f.getType() == int.class)
        {
          int x = f.getInt(null);
          if (x >= 100)
          {
            String name = TextUtil.toLowerCase(f.getName());
            Integer id = new Integer(x);
            if (idToKeyword.get(id) != null)
              System.out.println("ERROR: dup id: " + id + " / " + name);
            keywordToId.put(name, id);
            idToKeyword.put(id, name);
          }
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public Location loc;       // location of token
  public int type = -1;      // type code
  public Object value;       // Integer, Float, String, Long, Buf

}
