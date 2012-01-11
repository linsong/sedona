//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Aug 06  Brian Frank  Creation
//

package sedonac.ir;

import java.io.*;
import java.util.*;
import sedona.Bool;
import sedona.Facets;
import sedona.Value;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;
import sedonac.ir.*;
import sedonac.parser.*;
import sedonac.scode.*;

/**
 * IrReader is parser for the IR text format written by IrWriter.
 */
public class IrReader
  extends Parser
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Constructor.
   */
  public IrReader(Compiler compiler, Location loc, InputStream in)
  {
    super(compiler, loc, in);
  }

//////////////////////////////////////////////////////////////////////////
// Parse
//////////////////////////////////////////////////////////////////////////

  public IrType readType(IrKit kit)
  {
    // read tokens into memory
    readTokens();

    // class header          
    Facets facets = readFacets();
    int flags = typeFlags();
    consume(Token.CLASS);
    String name = consumeId();

    // extends
    Type base = null;
    if (!kit.name.equals("sys") || !name.equals("Obj"))
    {
      consume(Token.EXTENDS);
      base = tryTypeBase();
    }

    IrType t = new IrType(kit, flags, name, facets);
    t.base = base;

    // slots
    HashMap slotsByName = new HashMap();
    ArrayList slots = new ArrayList();
    consume(Token.LBRACE);
    while (curt != Token.RBRACE)
    {
      IrSlot slot = readSlot(t);
      slots.add(slot);
      slotsByName.put(slot.name, slot);
    }
    consume(Token.RBRACE);
    t.declared = (IrSlot[])slots.toArray(new IrSlot[slots.size()]);
    t.slots = slots;
    t.slotsByName = slotsByName;

    return t;
  }

  private Facets readFacets()
  {
    if (curt != Token.AT) return new Facets();

    Facets facets = new Facets();
    while (curt == Token.AT)
    {
      consume();
      String name = consumeId();
      Value value = Bool.TRUE;
      if (curt == Token.ASSIGN)
      {
        consume(Token.ASSIGN);
        value = literal().toValue();
      }
      facets.set(name, value);
    }
    return facets;
  }


  private IrSlot readSlot(IrType parent)
  {
    Facets facets = readFacets();
    int flags = slotFlags();
    Type type = type();
    String name = consumeId();
    if (curt == Token.LPAREN)
      return readMethod(parent, flags, name, facets, type);
    else
      return readField(parent, flags, name, facets, type);
  }

  private IrSlot readField(IrType parent, int flags, String name, Facets facets, Type type)
  {
    Expr.Literal define = null;
    boolean arrayInit = false;
    if (curt == Token.ASSIGN)
    {
      consume();
      if ((flags & Field.DEFINE) != 0)
      {                  
        if (curt == Token.LBRACE)
          define = arrayLiteral(flags, type); 
        else
        {
          define = literal();
          define.isNullLiteral(type);       // coerce null value into correct type
        }
      }
      else
      {
        arrayInitializer();
        arrayInit = true;
      }
    }

    // reference property types are implied inline
    if (((flags & Slot.PROPERTY) != 0) && !type.isPrimitive())
      flags |= Slot.INLINE;

    IrField f = new IrField(parent, flags, name, facets, type);
    f.define = define;
    f.arrayInit = arrayInit;

    if (curt == Token.LBRACE)
    {
      consume(Token.LBRACE);
      readFieldDirectives(f);
      consume(Token.RBRACE);
    }
    return f;
  }

  private void readFieldDirectives(IrField f)
  {
    while (curt == Token.DOT)
    {
      consume();
      String key = consumeId();
      consume(Token.ASSIGN);

      if (key.equals("ctorLengthParam"))
        f.ctorLengthParam = Integer.parseInt(consume().toString());
      else if (key.equals("ctorLengthArg"))
        f.ctorLengthArg = literalOrDefine();
      else
        err("Unknown field directive: " + key);
    }
  }

  private IrMethod readMethod(IrType parent, int flags, String name, Facets facets, Type ret)
  {
    // params
    ArrayList acc = new ArrayList();
    consume(Token.LPAREN);
    while (curt != Token.RPAREN)
    {
      if (acc.size() > 0) consume(Token.COMMA);
      acc.add(type());
    }
    consume(Token.RPAREN);
    Type[] params = (Type[])acc.toArray(new Type[acc.size()]);

    IrMethod m = new IrMethod(parent, flags, name, facets, ret, params);

    // native
    if (curt == Token.ASSIGN)
    {
      consume(Token.ASSIGN);
      Location loc = this.loc;
      int kitId = consume().valueToInt();
      consume(Token.DOUBLE_COLON);
      int methodId = consume().valueToInt();
      m.nativeId = new NativeId(loc, kitId, methodId);
    }

    // code
    if ((flags & (Slot.ABSTRACT|Slot.NATIVE)) != 0)
    {
      if (curt == Token.LBRACE) throw err("Abstract and native methods cannot have method bodies");
      if ((flags & Slot.NATIVE) != 0 && m.nativeId == null)
        throw err("Native method missing native id");
    }
    else
    {
      if (curt != Token.LBRACE) throw err("Expected method body");
      consume(Token.LBRACE);
      readCodeDirectives(m);
      m.code = readCode();
      consume(Token.RBRACE);
    }

    return m;
  }

  private void readCodeDirectives(IrMethod m)
  {
    while (curt == Token.DOT)
    {
      consume();
      String key = consumeId();
      consume(Token.ASSIGN);
      String val = consume().toString();

      if (key.equals("maxLocals"))
        m.maxLocals = Integer.parseInt(val);
      else
        err("Unknown code directive: " + key);
    }
  }

  private IrOp[] readCode()
  {
    ArrayList acc = new ArrayList();
    while (curt != Token.RBRACE)
      acc.add(readOp());
    return (IrOp[])acc.toArray(new IrOp[acc.size()]);
  }

  private IrOp readOp()
  {
    IrOp op = new IrOp();

    // index
    op.index = consume(Token.INT_LITERAL).valueToInt();
    consume(Token.COLON);

    // opcode
    String opcodeName = consumeId();
    op.opcode = SCode.opcode(opcodeName);
    if (op.opcode < 0) throw err("Unknown opcode '" + opcodeName + "'");

    // argument
    if (!curIsNewline)
    {
      Token tok = consume();
      op.arg = tok.toString();
      while (!curIsNewline)
        op.arg += consume().toString();
    }

    return op;
  }

  private Expr literalOrDefine()
  {
    if (curt != Token.ID)
      return literal();

    Location loc = cur.loc;
    String id = consumeId();
    consume(Token.DOUBLE_COLON);
    id += "::" + consumeId();
    consume(Token.DOT);
    id += "." + consumeId();
    return new Expr.Name(loc, null, id);
  }

}
