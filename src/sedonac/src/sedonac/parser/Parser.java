//
// Original Work:
//   Copyright (c) 2006, Brian Frank and Andy Frank
// 
// Derivative Work:
//   Copyright (c) 2007 Tridium, Inc.
//   Licensed under the Academic Free License version 3.0
//
// History:
//   16 Aug 06  Brian Frank  Creation
//

package sedonac.parser;

import java.io.*;
import java.util.*;
import sedona.Facets;
import sedona.Value;
import sedona.Bool;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * Parser for sedona program parser
 */
public class Parser
  extends CompilerSupport
{

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * File constructor.
   */
  public Parser(Compiler compiler, File file)
  {
    super(compiler);
    this.filename = Location.toString(file);
    this.tokenizer = new Tokenizer(compiler, filename, Tokenizer.readFile(file));
  }

  /**
   * InputStream constructor.
   */
  public Parser(Compiler compiler, Location loc, InputStream in)
  {
    super(compiler);
    this.filename = loc.file;
    this.tokenizer = new Tokenizer(compiler, filename, Tokenizer.readFile(loc, in));
  }

  /**
   * Test constructor.
   */
  public Parser(Compiler compiler, String source)
  {
    super(compiler);
    this.filename = "test";
    this.tokenizer = new Tokenizer(compiler, "test", source.toCharArray());
  }

//////////////////////////////////////////////////////////////////////////
// Unit
//////////////////////////////////////////////////////////////////////////

  public TypeDef[] parse()
  {
    readTokens();
    ArrayList acc = new ArrayList();
    while (curt != Token.EOF)
      acc.add(typeDef());
    return (TypeDef[])acc.toArray(new TypeDef[acc.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// TypeDef
//////////////////////////////////////////////////////////////////////////

  public TypeDef typeDef()
  {
    Location loc = this.loc;

    // comment
    String doc = null;
    while (curt == Token.DOC_COMMENT)
      doc = (String)consume(Token.DOC_COMMENT).value;

    // header
    FacetDef[] facets = facets();
    int flags = typeFlags();
    consume(Token.CLASS);
    String name = consumeId();
    TypeDef def = new TypeDef(loc, compiler.ast, flags, name, facets);
    def.doc = doc;
    if (curt == Token.EXTENDS)
    {
      consume();
      if ((def.base = tryTypeBase()) == null)
        err("'extends' missing base class identifier", this.loc);
    }
    else
    {
      if (!def.qname.equals("sys::Obj"))
        def.base = ns.objType;
    }

    // body
    consume(Token.LBRACE);
    while (curt != Token.RBRACE)
    {
      // parse the field or method
      SlotDef slot = slotDef(def);

      // check for dup name, otherwise add it
      if (def.slot(slot.name()) != null)
        err("Duplicate slot name '" + slot.name + "'", slot.loc);
      else
        def.addSlot(slot);
    }
    consume(Token.RBRACE);
    return def;
  }

  protected int typeFlags()
  {
    int flags = 0;
    boolean protection = false;
    while (true)
    {
      if (curt == Token.ABSTRACT)       { consume(); flags |= Type.ABSTRACT; }
      else if (curt == Token.CONST)     { consume(); flags |= Type.CONST; }
      else if (curt == Token.FINAL)     { consume(); flags |= Type.FINAL; }
      else if (curt == Token.INTERNAL)  { consume(); flags |= Type.INTERNAL; protection = true; }
      else if (curt == Token.PUBLIC)    { consume(); flags |= Type.PUBLIC;   protection = true; }
      else break;
    }
    if (!protection) flags |= Type.PUBLIC;
    return flags;
  }

//////////////////////////////////////////////////////////////////////////
// SlotDef
//////////////////////////////////////////////////////////////////////////

  private SlotDef slotDef(TypeDef parent)
  {
    // comment
    String doc = null;
    while (curt == Token.DOC_COMMENT)
      doc = (String)consume(Token.DOC_COMMENT).value;

    // slot definition
    SlotDef slot = doSlotDef(parent);
    slot.doc = doc;
    return slot;
  }

  private SlotDef doSlotDef(TypeDef parent)
  {
    Location loc = this.loc;

    FacetDef[] facets = facets();
    int flags = slotFlags();

    // constructor
    int mark = pos;
    if (curt == Token.ID && cur.value.equals(parent.name) && peekt == Token.LPAREN)
    {
      consumeId();
      int lparen = pos;
      consume(Token.LPAREN);
      boolean isCtor = (curt == Token.RPAREN || (tryType() != null && cur.isId()));
      if (isCtor)
      {
        reset(lparen);
        return methodDef(loc, parent, flags, Method.INSTANCE_INIT, facets, ns.voidType);
      }
    }
    reset(mark);

    // type declaration
    Type type = tryType();
    if (type == null)
      throw err("Expected field or method definition, invalid type signature");

    // if parens then this an inline field with ctor arguments
    Expr[] ctorArgs = null;
    if (curt == Token.LPAREN)
    {
      if (type.isArray())
        err("Cannot call constructor on an array field", this.loc);
      ctorArgs = callArgs();
    }

    // field or method name
    String name = consumeId();

    if (curt == Token.LPAREN)
      return methodDef(loc, parent, flags, name, facets, type);
    else
      return fieldDef(loc, parent, flags, name, facets, type, ctorArgs);
  }

  protected int slotFlags()
  {
    // parse specified flags
    Location loc = this.loc;
    int flags = 0;
    boolean protection = false;
    while (true)
    {
      if (curt == Token.ABSTRACT)       { consume(); flags |= Slot.ABSTRACT; }
      else if (curt == Token.ACTION)    { consume(); flags |= Slot.ACTION; }
      else if (curt == Token.CONST)     { consume(); flags |= Slot.CONST; }
      else if (curt == Token.DEFINE)    { consume(); flags |= Slot.DEFINE; }
      else if (curt == Token.INLINE)    { consume(); flags |= Slot.INLINE; }
      else if (curt == Token.INTERNAL)  { consume(); flags |= Slot.INTERNAL;   protection = true; }
      else if (curt == Token.NATIVE)    { consume(); flags |= Slot.NATIVE; }
      else if (curt == Token.OVERRIDE)  { consume(); flags |= Slot.OVERRIDE; }
      else if (curt == Token.PRIVATE)   { consume(); flags |= Slot.PRIVATE;   protection = true; }
      else if (curt == Token.PROPERTY)  { consume(); flags |= Slot.PROPERTY; }
      else if (curt == Token.PROTECTED) { consume(); flags |= Slot.PROTECTED; protection = true; }
      else if (curt == Token.PUBLIC)    { consume(); flags |= Slot.PUBLIC;    protection = true; }
      else if (curt == Token.STATIC)    { consume(); flags |= Slot.STATIC; }
      else if (curt == Token.VIRTUAL)   { consume(); flags |= Slot.VIRTUAL; }
      else break;
    }

    // abstract implies virtual
    if ((flags & Slot.ABSTRACT) != 0)
    {
      if ((flags & Slot.VIRTUAL) != 0)
        err("The 'virtual' modifier is implied by 'abstract'", loc);
      flags |= Slot.VIRTUAL;
    }

    // define implies static
    if ((flags & Slot.DEFINE) != 0)
    {
      if ((flags & Slot.STATIC) != 0)
        err("The 'static' modifier is implied by 'define'", loc);
      flags |= Slot.STATIC;
    }

    // action implies public and virtual
    if ((flags & Slot.ACTION) != 0)
    {
      if (((flags & Slot.VIRTUAL) != 0) && ((flags & Slot.ABSTRACT) == 0))
        err("The 'virtual' modifier is implied by 'action'", loc);
      flags |= Slot.VIRTUAL;
      
      if ((flags & Slot.PUBLIC) != 0)
        err("The 'public' modifier is implied by 'action'", loc);
    }

    // property implies public
    if ((flags & Slot.PROPERTY) != 0)
    {
      if ((flags & Slot.PUBLIC) != 0)
        err("The 'public' modifier is implied by 'property'", loc);
    }

    // property implies inline
    if ((flags & Slot.PROPERTY) != 0)
    {
      if ((flags & Slot.INLINE) != 0)
        err("The 'inline' modifier is implied by 'property'", loc);
    }

    // public is implicits
    if (!protection) flags |= Slot.PUBLIC;

    return flags;
  }

//////////////////////////////////////////////////////////////////////////
// FieldDef
//////////////////////////////////////////////////////////////////////////

  private FieldDef fieldDef(Location loc, TypeDef parent, int flags, 
                            String name, FacetDef[] facets, Type type, 
                            Expr[] ctorArgs)
  {                      
    // reference property types are implied inline
    if (((flags & Slot.PROPERTY) != 0) && !type.isPrimitive()) 
      flags |= Slot.INLINE;  
  
    Expr init = null;
    boolean populateObjArray = false;
    if ((curt == Token.ASSIGN) || (curt == Token.PROP_ASSIGN))
    {
      consume();
      if (curt == Token.LBRACE)
      {                  
        if (peekt == Token.DOT)
          init = arrayInitializer();
        else                               
          init = arrayLiteral(flags, type);
      }
      else
      {
        init = expr();
        init.isNullLiteral(type);       // coerce null to correct type if necessary
      }
    }
    endOfStmt();

    // define implies const
    if ((flags & Slot.DEFINE) != 0)
    {                              
      if ((flags & Slot.CONST) != 0) err("Cannot use 'const' modifier on define", loc);
      flags |= Slot.CONST;
    }

    return new FieldDef(loc, parent, flags, name, facets, type, init, ctorArgs);
  }

  protected Expr.InitArray arrayInitializer()
  {
    Location loc = this.loc;
    String msg = "Expected object array initializer '{...}'";
    consume(Token.LBRACE, msg);
    consume(Token.DOT, msg);
    consume(Token.DOT, msg);
    consume(Token.DOT, msg);
    consume(Token.RBRACE, msg);
    return new Expr.InitArray(loc);
  }

  protected Expr.Literal arrayLiteral(int flags, Type type)
  {                          
    // verify this is a define                       
    Location loc = this.loc;
    if ((flags & Slot.DEFINE) == 0)
      throw err("Array literals only supported on define fields", loc);
      
    // verify this is an array                       
    if (!type.isArray())
      throw err("Cannot use array literal with non-array field", loc);
    
    // parse array
    ArrayList acc = new ArrayList();
    consume(Token.LBRACE);        
    if (curt == Token.COMMA) consume();
    else while (true)
    {
      acc.add(literal().value);
      if (curt != Token.COMMA) break;
      consume(Token.COMMA);
    }
    consume(Token.RBRACE);        
    return new Expr.Literal(loc, Expr.ARRAY_LITERAL, type, acc.toArray());
  }                     
  
//////////////////////////////////////////////////////////////////////////
// MethodDef
//////////////////////////////////////////////////////////////////////////

  private MethodDef methodDef(Location loc, TypeDef parent, int flags, 
                              String name, FacetDef[] facets, Type ret)
  {
    this.inVoid = ret.isVoid();
    boolean isStatic = (flags & Slot.STATIC) != 0;
    ParamDef[] params = params(isStatic);

    Block code;
    if ((flags & (Slot.ABSTRACT|Slot.NATIVE)) != 0)
    {
      if (curt == Token.LBRACE) throw err("Abstract and native methods cannot have method bodies");
      code = null;
      endOfStmt();
    }
    else
    {
      if (curt != Token.LBRACE) throw err("Expected method body");
      code = block();
    }

    return new MethodDef(loc, parent, flags, name, facets, ret, params, code);
  }

  private ParamDef[] params(boolean isStatic)
  {
    ArrayList acc = new ArrayList();
    consume(Token.LPAREN);       
    int index = isStatic ? 0 : 1;
    if (curt != Token.RPAREN)
    {
      while (true)
      {          
        ParamDef param = param(index);
        index += param.type.isWide() ? 2 : 1;
        acc.add(param);
        if (curt == Token.RPAREN) break;
        consume(Token.COMMA);
      }
    }
    consume(Token.RPAREN);
    return (ParamDef[])acc.toArray(new ParamDef[acc.size()]);
  }

  private ParamDef param(int index)
  {
    return new ParamDef(loc, index, type(), consumeId());
  }

//////////////////////////////////////////////////////////////////////////
// Statements
//////////////////////////////////////////////////////////////////////////

  public Block block()
  {
    verify(Token.LBRACE);
    return stmtOrBlock();
  }

  public Block stmtOrBlock( )
  {
    Block block = new Block(cur.loc);
    if (curt != Token.LBRACE)
    {
      block.stmts.add(stmt());
    }
    else
    {
      consume(Token.LBRACE);
      while (curt != Token.RBRACE)
        block.stmts.add(stmt());
      consume(Token.RBRACE);
    }
    return block;
  }

  private Stmt stmt()
  {   
    // label: stmt                 
    String label = null;
    if (curt == Token.ID && peekt == Token.COLON)
    {                   
      label = consumeId();
      consume(Token.COLON);
    }
    
    Stmt stmt = doStmt();
    stmt.label = label;
    return stmt;
  }
  
  private Stmt doStmt()
  {  
    switch (curt)
    {
      case Token.RETURN:   return returnStmt();
      case Token.IF:       return ifStmt();
      case Token.FOR:      return forStmt();
      case Token.FOREACH:  return foreachStmt();
      case Token.WHILE:    return whileStmt();
      case Token.DO:       return doWhileStmt();
      case Token.BREAK:    return breakStmt();
      case Token.CONTINUE: return continueStmt();
      case Token.ASSERT:   return assertStmt();
      case Token.GOTO:     return gotoStmt();
      case Token.SWITCH:   return switchStmt();
    }

    return exprOrLocalDef(true);
  }

  private Stmt exprOrLocalDef(boolean isEndOfStmt)
  {
    Location loc = this.loc;

    // check if we have a potential type followed
    // by a identifier which we assume to be a local
    // variable declaration
    int mark = pos;
    Type type = tryType();
    if (type != null && cur.isId())
      return localDefStmt(type, isEndOfStmt);
    reset(mark);

    // must be expr statement
    Stmt exprStmt = new Stmt.ExprStmt(loc, expr());
    if (isEndOfStmt) endOfStmt();
    return exprStmt;
  }

  private Stmt localDefStmt(Type type, boolean isEndOfStmt)
  {
    Location loc = this.loc;

    String id = consumeId();
    Expr init = null;
    if ((curt == Token.ASSIGN) || (curt == Token.PROP_ASSIGN)) { consume(); init = expr(); }
    if (isEndOfStmt) endOfStmt();
    return new Stmt.LocalDef(loc, type, id, init);
  }

  private Stmt returnStmt()
  {
    Stmt.Return stmt = new Stmt.Return(loc);
    consume(Token.RETURN);
    if (!inVoid) stmt.expr = expr();
    endOfStmt();
    return stmt;
  }

  private Stmt ifStmt()
  {
    Stmt.If stmt = new Stmt.If(loc);
    consume(Token.IF);
    consume(Token.LPAREN);
    stmt.cond = expr();
    consume(Token.RPAREN);
    stmt.trueBlock = stmtOrBlock();
    if (curt == Token.ELSE)
    {
      consume(Token.ELSE);
      stmt.falseBlock = stmtOrBlock();
    }
    return stmt;
  }

  private Stmt.While whileStmt()
  {
    Stmt.While stmt = new Stmt.While(loc);
    consume(Token.WHILE);
    consume(Token.LPAREN);
    stmt.cond = expr();
    consume(Token.RPAREN);

    if (curt == Token.SEMICOLON)
    {
      // no body
      stmt.block = new Block(loc);
      consume();
    }
    else
    {
      stmt.block = stmtOrBlock();
    }

    return stmt;
  }

  private Stmt.DoWhile doWhileStmt()
  {
    Stmt.DoWhile stmt = new Stmt.DoWhile(loc);
    consume(Token.DO);
    stmt.block = stmtOrBlock();
    consume(Token.WHILE);
    consume(Token.LPAREN);
    stmt.cond = expr();
    consume(Token.RPAREN);
    endOfStmt();
    return stmt;
  }

  private Stmt.For forStmt()
  {
    Stmt.For stmt = new Stmt.For(loc);
    consume(Token.FOR);
    consume(Token.LPAREN);

    Stmt init = null;
    if (curt != Token.SEMICOLON) stmt.init = exprOrLocalDef(false);
    consume(Token.SEMICOLON);

    if (curt != Token.SEMICOLON) stmt.cond = expr();
    consume(Token.SEMICOLON);

    if (curt != Token.RPAREN) stmt.update = expr();
    consume(Token.RPAREN);

    if (curt == Token.SEMICOLON)
    {
      // no body
      stmt.block = new Block(loc);
      consume();
    }
    else
    {
      stmt.block = stmtOrBlock();
    }

    return stmt;
  }

  private Stmt.Foreach foreachStmt()
  {
    Stmt.Foreach stmt = new Stmt.Foreach(loc);

    consume(Token.FOREACH);
    consume(Token.LPAREN);
    Location localLoc = this.loc;
    Type type = type();
    String name = consumeId();
    stmt.local = new Stmt.LocalDef(localLoc, type, name);
    consume(Token.COLON);
    stmt.array = expr();
    if (curt == Token.COMMA)
    {
      consume(Token.COMMA);
      stmt.length = expr();
    }
    consume(Token.RPAREN);

    stmt.block = stmtOrBlock();
    return stmt;
  }

  private Stmt.Break breakStmt()
  {
    Stmt.Break stmt = new Stmt.Break(loc);
    consume(Token.BREAK);
    endOfStmt();
    return stmt;
  }

  private Stmt.Continue continueStmt()
  {
    Stmt.Continue stmt = new Stmt.Continue(loc);
    consume(Token.CONTINUE);
    endOfStmt();
    return stmt;
  }

  private Stmt assertStmt()
  {
    Stmt.Assert stmt = new Stmt.Assert(loc);
    consume(Token.ASSERT);
    consume(Token.LPAREN);
    stmt.cond = expr();
    consume(Token.RPAREN);
    endOfStmt();
    return stmt;
  }

  private Stmt gotoStmt()
  {
    Stmt.Goto stmt = new Stmt.Goto(loc);
    consume(Token.GOTO);
    stmt.destLabel = consumeId();
    endOfStmt();
    return stmt;
  }

  private Stmt.Switch switchStmt()
  {                     
    Stmt.Switch stmt = new Stmt.Switch(loc);    
    
    // switch(cond)
    consume(Token.SWITCH);
    consume(Token.LPAREN);
    stmt.cond = expr();
    consume(Token.RPAREN);
    
    // opening {
    consume(Token.LBRACE); 
    
    // case blocks
    ArrayList cases = new ArrayList();
    while (curt == Token.CASE)
    {             
      Stmt.Case c = new Stmt.Case(loc);
      cases.add(c);
      
      // label
      consume(Token.CASE);
      c.label = expr();
      consume(Token.COLON);
      
      // statements
      Block block = new Block(loc);
      while (curt != Token.CASE && curt != Token.DEFAULT  && curt != Token.RBRACE)
        block.add(stmt());
      if (!block.isEmpty())
        c.block = block;
    }                                 
    stmt.cases = (Stmt.Case[])cases.toArray(new Stmt.Case[cases.size()]);
    
    // default
    if (curt == Token.DEFAULT)
    {
      consume(Token.DEFAULT);
      consume(Token.COLON);
      stmt.defaultBlock = new Block(loc);
      while (curt != Token.RBRACE)
        stmt.defaultBlock.add(stmt());
    }
    
    // closing }    
    consume(Token.RBRACE);
    return stmt;
  }

  void endOfStmt()
  {
    if (curt == Token.SEMICOLON) { consume(); return; }
    if (curIsNewline) return;
    if (curt == Token.RBRACE) return;
    throw err("Expected end of statement, not " + cur);
  }

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////

  public Expr expr()
  {
    return assignExpr();
  }

  private Expr assignExpr()
  {
    Expr expr = ternary();
    if (cur.isAssign())
      return new Expr.Binary(expr.loc, consume(), expr, assignExpr());
    return expr;
  }

  private Expr ternary()
  {
    Expr expr = condOrExpr();
    if (curt == Token.QUESTION)
    {
      Expr.Ternary ternary = new Expr.Ternary(expr.loc);
      ternary.cond = expr;
      consume(Token.QUESTION);
      ternary.trueExpr  = condOrExpr();
      consume(Token.COLON);
      ternary.falseExpr = condOrExpr();
      return ternary;
    }
    return expr;
  }

  private Expr condOrExpr()
  {
    Expr expr = condAndExpr();
    if (curt == Token.DOUBLE_PIPE)
    {
      Expr.Cond cond = new Expr.Cond(expr.loc, Expr.COND_OR, cur, expr);
      while (curt == Token.DOUBLE_PIPE)
      {
        consume();
        cond.operands.add(condAndExpr());
      }
      expr = cond;
    }
    return expr;
  }

  private Expr condAndExpr()
  {
    Expr expr = bitOrExpr();
    if (curt == Token.DOUBLE_AMP)
    {
      Expr.Cond cond = new Expr.Cond(expr.loc, Expr.COND_AND, cur, expr);
      while (curt == Token.DOUBLE_AMP)
      {
        consume();
        cond.operands.add(bitOrExpr());
      }
      expr = cond;
    }
    return expr;
  }

  private Expr bitOrExpr()
  {
    Expr expr = bitXorExpr();
    while (curt == Token.PIPE)
      expr = new Expr.Binary(expr.loc, consume(), expr, bitXorExpr());
    return expr;
  }

  private Expr bitXorExpr()
  {
    Expr expr = bitAndExpr();
    while (curt == Token.CARET)
      expr = new Expr.Binary(expr.loc, consume(), expr, bitAndExpr());
    return expr;
  }

  private Expr bitAndExpr()
  {
    Expr expr = equalityExpr();
    while (curt == Token.AMP)
      expr = new Expr.Binary(expr.loc, consume(), expr, equalityExpr());
    return expr;
  }

  private Expr equalityExpr()
  {
    Expr expr = relationalExpr();
    while (curt == Token.EQ || curt == Token.NOT_EQ)
      expr = new Expr.Binary(expr.loc, consume(), expr, relationalExpr());
    return expr;
  }

  private Expr relationalExpr()
  {
    Expr expr = elvis();
    while (curt == Token.LT || curt == Token.LT_EQ ||
           curt == Token.GT || curt == Token.GT_EQ)
      expr = new Expr.Binary(expr.loc, consume(), expr, elvis());
    return expr;
  }

  private Expr elvis()
  {
    Expr expr = shiftExpr();
    while (curt == Token.ELVIS)
      expr = new Expr.Binary(expr.loc, consume(), expr, shiftExpr());
    return expr;
  }

  private Expr shiftExpr()
  {
    Expr expr = addExpr();
    while (curt == Token.LSHIFT || curt == Token.RSHIFT)
      expr = new Expr.Binary(expr.loc, consume(), expr, addExpr());
    return expr;
  }

  private Expr addExpr()
  {
    Expr expr = multExpr();
    while (curt == Token.PLUS || curt == Token.MINUS)
      expr = new Expr.Binary(expr.loc, consume(), expr, multExpr());
    return expr;
  }

  private Expr multExpr()
  {
    Expr expr = parenExpr();
    while (curt == Token.STAR || curt == Token.SLASH || curt == Token.PERCENT)
      expr = new Expr.Binary(expr.loc, consume(), expr, parenExpr());
    return expr;
  }

  private Expr parenExpr()
  {
    if (curt != Token.LPAREN)
      return unaryExpr();

    Location loc = this.loc;
    consume(Token.LPAREN);

    Type cast = tryCastType();
    if (cast != null)
    {
      consume(Token.RPAREN);
      return new Expr.Cast(loc, cast, parenExpr());
    }

    Expr expr = expr();
    consume(Token.RPAREN);
    while (true)
    {
      Expr suffix = termSuffixExpr(expr);
      if (suffix == null) return expr;
      expr = suffix;
    }
  }

  private Expr unaryExpr()
  {
    // plus is ignored
    if (curt == Token.PLUS)  
    {
      consume();
      return parenExpr();       
    }
    
    // new/delete
    if (curt == Token.NEW) return newExpr();
    if (curt == Token.DELETE) return deleteExpr();

    // prefix unary operators
    if (curt == Token.BANG || curt == Token.MINUS || curt == Token.TILDE ||
        curt == Token.INCREMENT || curt == Token.DECREMENT)
      return new Expr.Unary(loc, consume(), parenExpr(), false);

    Expr expr = termExpr();

    // postfix operators
    if (curt == Token.INCREMENT || curt == Token.DECREMENT)
       return new Expr.Unary(loc, consume(), expr, true);

    return expr;
  }   
  
  private Expr newExpr()
  {
    // new keyword
    Expr.New expr = new Expr.New(loc);
    consume();    
    
    // type
    expr.of = tryTypeBase();
    if (expr.of == null)
      throw err("Expected type name");

    // optional [arrayLength]
    if (curt == Token.LBRACKET)
    {
      consume();
      expr.arrayLength = expr();
      consume(Token.RBRACKET);
    }                         
    
    // otherwise expect Type()
    else
    {
      consume(Token.LPAREN);
      consume(Token.RPAREN);
    }
    
    return expr;
  }

  private Expr deleteExpr()
  {
    // delete keyword
    Expr.Delete expr = new Expr.Delete(loc);
    consume();    
    expr.target = expr();
    return expr;
  }

  private Expr termExpr()
  {
    Expr expr = termBaseExpr();
    while (true)
    {
      Expr suffix = termSuffixExpr(expr);
      if (suffix == null) return expr;
      expr = suffix;
    }
  }

  private Expr termBaseExpr()
  {
    if (cur.isLiteral()) return literal();
    Location loc = this.loc;

    switch (curt)
    {
      case Token.ID:    return idExpr(null, false);
      case Token.THIS:  consume(); return new Expr.This(loc);
      case Token.SUPER: consume(); return new Expr.Super(loc);
    }

    Type keyword = cur.typeKeyword(ns);
    if (keyword != null && peekt == Token.DOT)
    {
      consume();
      consume();
      if (curt == Token.ID)
      {
        String id = consumeId();
        if (id.equals("type"))
          return new Expr.Literal(loc, ns, Expr.TYPE_LITERAL, keyword);
        if (id.equals("sizeof"))
          return new Expr.Literal(loc, ns, Expr.SIZE_OF, keyword);
      }
      throw err("Expected type literal expression, not '" + cur + "'");
    }

    throw err("Expected expression, not '" + cur + "'");
  }

  public Expr.Literal literal()
  {
    Location loc = this.loc;

    if (curt == Token.MINUS)
    {
      consume();
      Expr.Literal literal;
      switch (curt)
      {
        case Token.INT_LITERAL:    literal = new Expr.Literal(loc, ns, Expr.INT_LITERAL,    consume().value); break;
        case Token.LONG_LITERAL:   literal = new Expr.Literal(loc, ns, Expr.LONG_LITERAL,   consume().value); break;
        case Token.FLOAT_LITERAL:  literal = new Expr.Literal(loc, ns, Expr.FLOAT_LITERAL,  consume().value); break;
        case Token.DOUBLE_LITERAL: literal = new Expr.Literal(loc, ns, Expr.DOUBLE_LITERAL, consume().value); break;
        case Token.TIME_LITERAL:   literal = new Expr.Literal(loc, ns, Expr.TIME_LITERAL,   consume().value); break;
        default:  throw err("Expected int, long, float, double, or time literal, not '" + cur + "'");
      }
      return literal.negate();
    }

    switch (curt)
    {
      case Token.TRUE:             consume(); return new Expr.Literal(loc, ns, Expr.TRUE_LITERAL, Boolean.TRUE);
      case Token.FALSE:            consume(); return new Expr.Literal(loc, ns, Expr.FALSE_LITERAL, Boolean.FALSE);
      case Token.INT_LITERAL:      return new Expr.Literal(loc, ns, Expr.INT_LITERAL, consume().value);
      case Token.LONG_LITERAL:     return new Expr.Literal(loc, ns, Expr.LONG_LITERAL, consume().value);
      case Token.FLOAT_LITERAL:    return new Expr.Literal(loc, ns, Expr.FLOAT_LITERAL, consume().value);
      case Token.DOUBLE_LITERAL:   return new Expr.Literal(loc, ns, Expr.DOUBLE_LITERAL, consume().value);
      case Token.NULL:             consume(); return new Expr.Literal(loc, ns, Expr.NULL_LITERAL, null);
      case Token.TIME_LITERAL:     return new Expr.Literal(loc, ns, Expr.TIME_LITERAL, consume().value);
      case Token.STR_LITERAL:      return new Expr.Literal(loc, ns, Expr.STR_LITERAL, consume().value);
      case Token.BUF_LITERAL:      return new Expr.Literal(loc, ns, Expr.BUF_LITERAL, consume().value);
    }
    throw err("Expected literal, not '" + cur + "'");
  }

  private Expr idExpr(Expr target, boolean safeNav)
  {
    Location loc = this.loc;
    String name = consumeId();
    Expr.Name expr;
    if (curt == Token.LPAREN)
    {
      expr = new Expr.Call(loc, target, name, callArgs());
    }
    else
    {
      // Check for qname (kit::Type)
      if (curt == Token.DOUBLE_COLON)
      {
        consume(Token.DOUBLE_COLON);
        name += "::" + consumeId();
      }
      expr = new Expr.Name(loc, target, name);
    }       
    expr.safeNav = safeNav;
    return expr;
  }

  private Expr[] callArgs()
  {
    ArrayList acc = new ArrayList();
    consume(Token.LPAREN);
    if (curt != Token.RPAREN)
    {
      while (true)
      {
        acc.add(expr());
        if (curt == Token.RPAREN) break;
        consume(Token.COMMA);
      }
    }
    consume(Token.RPAREN);
    return (Expr[])acc.toArray(new Expr[acc.size()]);
  }

  private Expr termSuffixExpr(Expr target)
  {
    if (curt == Token.DOT)
    {
      consume(Token.DOT);
      return idExpr(target, false);
    }

    if (curt == Token.SAFE_NAV)
    {
      consume(Token.SAFE_NAV);
      return idExpr(target, true);
    }

    // TODO: need to deal with local variable
    // definitions such as Foo[] x and Foo[len]

    if (curt == Token.LBRACKET)
    {
      Location loc = this.loc;
      consume(Token.LBRACKET);
      if (curt == Token.RBRACKET)
        throw err("Local variable array types not supported yet");
      Expr index = expr();
      consume(Token.RBRACKET);
      return new Expr.Index(loc, target, index);
    }

    return null;
  }


////////////////////////////////////////////////////////////////
// Facets
////////////////////////////////////////////////////////////////

  private FacetDef[] facets()
  {
    if (curt != Token.AT) return FacetDef.empty;

    ArrayList acc = new ArrayList();
    while (curt == Token.AT)
    {
      consume(); 
      Location loc = this.loc;
      String name = consumeId();
      Expr value;
      if (curt == Token.ASSIGN)
      {
        consume(Token.ASSIGN);
        value = expr();
      }                
      else
      {             
        value = new Expr.Literal(loc, ns, Expr.TRUE_LITERAL, Boolean.TRUE);
      }
      acc.add(new FacetDef(loc, name, value));
    }
    return (FacetDef[])acc.toArray(new FacetDef[acc.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// Type
//////////////////////////////////////////////////////////////////////////

  /**
   * Parse a type production.
   */
  public Type type()
  {
    try
    {
      Type type = doTryType();
      if (type != null) return type;
      throw err("Expected type name", loc);
    }
    catch (ParseTypeException e)
    {
      throw err(e.getMessage(), e.loc);
    }
  }

  /**
   * Attempt to parse a type production or return null
   * if we know for sure we don't have a type.
   */
  public Type tryType()
  {
    try
    {
      return doTryType();
    }
    catch (ParseTypeException e)
    {
      return null;
    }
  }

  private Type doTryType()
  {
    Type type = tryTypeBase();
    if (type == null) return null;

    if (curt == Token.LBRACKET)
      type = arrayType(type);

    if (curt == Token.LBRACKET)
      throw new ParseTypeException("Multi-dimensional arrays not supported", cur.loc);

    return type;
  }

  public Type tryTypeBase()
  {
    Type keyword = cur.typeKeyword(ns);
    if (keyword != null)
    {
      consume();
      return keyword;
    }

    Location loc = this.loc;
    if (curt == Token.ID)
    {
      String id = consumeId();
      if (curt == Token.DOUBLE_COLON)
      {
        consume();
        id += "::" + consumeId();
      }
      return new UnresolvedType(loc, id);
    }

    return null;
  }

  private Type arrayType(Type type)
  {
    consume(Token.LBRACKET);
    Location loc = cur.loc;

    if (curt == Token.RBRACKET)
    {
      type = new ArrayType(loc, type, null);
    }
    else if (curt == Token.ID)
    {
      String id = defineId();
      type = new ArrayType(loc, type, new ArrayType.UnresolvedLen(id));
    }
    else if (curt == Token.INT_LITERAL)
    {
      int len = consume().valueToInt();
      type = new ArrayType(loc, type, new ArrayType.LiteralLen(len));
    }
    else
    {
      throw new ParseTypeException("Expected array length, not '" + cur + "'", loc);
    }

    if (curt != Token.RBRACKET)
      throw new ParseTypeException("Expected ']', not '" + cur + "'", loc);
    consume();
    return type;
  }

  /**
   * Try to parse a cast type production, and if the current sequence of
   * tokens is not a type production, then leave the tokens untouched
   * and return null.  We assume the open paren has been consumed
   * and if successful the cur token should be left on the close paren.
   * NOTE: this method must be kept in sync with type()
   */
  private Type tryCastType()
  {
    // In Sedona just like C/C#/Java, a paren could mean either a
    // cast or a parenthesized expression, to figure this out
    // we follow the C# ECMA spec (14.6.6 Cast expressions)...
    //
    // To resolve cast expression ambiguities, the following rule exists: a
    // sequence of one or more tokens enclosed in parentheses is considered
    // the start of a cast expression only if at least one of the following
    // are true:
    //  - the sequence of tokens is a correct grammar for a type, but not for
    //    an expression (for example a::b)
    //  - the sequence of tokens is a correct grammar for a type (for example
    //    a or a::b), and the token immediately following the closing parentheses
    //    is an identifier, ~, !, (, a literal, or any keyword (except as
    //    and is in C#)
    //  casts: (x)y  (x)(y) and (x)(-y)
    //  not casts: (x)-y   (as long as x is not a type keyword)

    // attempt to parse a type production
    int mark = pos;
    Type type = tryType();

    // ok at this point we must have a close paren for this to be a cast
    if (type != null && curt == Token.RPAREN)
    {
      // we can meet rule two by checking the next token
      if (peekt == Token.ID   || peekt == Token.TILDE  ||
          peekt == Token.BANG || peekt == Token.LPAREN ||
          peek.isLiteral()    || peek.isKeyword())
        return type;
    }

    // if we haven't returned by this point, it means we have consumed
    // tokens but discovered that this isn't a cast operation so reset
    // the parser back to the token we were on entering this method
    reset(mark);
    return null;
  }

  /**
   * Consume a define identifier which might be in one of the
   * following forms:
   *   - kit::Type.slot
   *   - Type.slot
   *   - slot
   */
  private String defineId()
  {
    String id = consumeId();
    if (curt == Token.DOUBLE_COLON)
    {
      consume();
      id += "::" + consumeId();
    }
    if (curt == Token.DOT)
    {
      consume();
      id += "." + consumeId();
    }
    return id;
  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  /**
   * Throw compiler error with current location.
   */
  public CompilerException err(String msg)
  {
    return super.err(msg, cur.loc);
  }

////////////////////////////////////////////////////////////////
// Read
////////////////////////////////////////////////////////////////

  /**
   * Consume current token and return it.
   */
  protected String consumeId()
  {
    return consume(Token.ID).value.toString();
  }

  /**
   * Consume expected token.
   */
  protected Token consume(int expected)
  {
    verify(expected);
    return consume();
  }

  /**
   * Consume expected token.
   */
  protected Token consume(int expected, String msg)
  {
    verify(expected, msg);
    return consume();
  }

  /**
   * Verify expected token.
   */
  protected void verify(int expected)
  {
    if (curt != expected)
      throw err("Expected '" + Token.toString(expected) + "', not '" + cur.toString() + "'");
  }

  /**
   * Verify expected token.
   */
  protected void verify(int expected, String msg)
  {
    if (curt != expected)
      throw err(msg);
  }

  /**
   * Consume current token and return it.
   */
  protected Token consume()
  {
    Token result = cur;

    // get the next token from the buffer, if pos is past numTokens,
    // then always use the last token which will be eof
    Token next;
    pos++;
    if (pos+1 < numTokens)
    {
      next = tokens[pos+1];  // next peek is cur+1
    }
    else
    {
      next = tokens[numTokens-1];
    }

    cur   = peek;
    peek  = next;
    curt  = cur.type;
    peekt = peek.type;
    loc   = cur.loc;
    curIsNewline = result.loc.line < cur.loc.line;

    return result;
  }

  /**
   * Read all the tokens into memory and store in the tokens field.
   */
  public void readTokens()
  {
    this.tokens = tokenizer.tokenize();
    this.numTokens = tokens.length;
    reset(0);
  }

  /**
   * Reset the current position to the specified tokens index.
   */
  protected void reset(int pos)
  {
    this.pos   = pos;
    this.cur   = tokens[pos];
    this.peek  = pos+1 < numTokens ? tokens[pos+1] : tokens[pos];
    this.curt  = cur.type;
    this.peekt = peek.type;
    this.loc   = cur.loc;
  }

////////////////////////////////////////////////////////////////
// ParseTypeException
////////////////////////////////////////////////////////////////

  static class ParseTypeException extends RuntimeException
  {
    ParseTypeException(String msg, Location loc)
    {
      super(msg);
      this.loc = loc;
    }

    Location loc;
  }

////////////////////////////////////////////////////////////////
// Main
////////////////////////////////////////////////////////////////

  public static void main(String args[])
    throws Exception
  {
    try
    {
      File f = new File(args[0]);
      Parser p = new Parser(null, f);
      p.parse()[0].dump();
    }
    catch(CompilerException e)
    {
      System.out.println(e.toLogString());
      e.printStackTrace();
    }
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  protected String filename;
  protected Tokenizer tokenizer;
  protected Location loc;
  protected Token[] tokens;
  protected int pos;
  protected int numTokens;
  protected Token cur;
  protected Token peek;
  protected int curt;
  protected int peekt;
  protected boolean curIsNewline;
  protected boolean inVoid;

}
