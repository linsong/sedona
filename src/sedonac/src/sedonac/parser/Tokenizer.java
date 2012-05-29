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

import sedona.Buf;
import sedona.util.TextUtil;
import sedonac.Compiler;
import sedonac.CompilerException;
import sedonac.CompilerSupport;
import sedonac.Location;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Tokenizer for sedona program parser
 */
public class Tokenizer
  extends CompilerSupport
{

////////////////////////////////////////////////////////////////
// Read File
////////////////////////////////////////////////////////////////

  /**
   * Read a file into a normalized char array
   * with all newlines represented as '\n'
   */
  public static char[] readFile(File f)
  {
    Location loc = new Location(f);
    try
    {
      return readFile(loc, new FileInputStream(f));
    }
    catch (FileNotFoundException e)
    {
      throw new CompilerException("Source file not found", loc, e);
    }
  }

  /**
   * Read a file into a normalized char array
   * with all newlines represented as '\n'
   */
  private static final CharArrayWriter chars = new CharArrayWriter(1024);
  private static final char[] charBuf = new char[1024];
  public static char[] readFile(Location loc, InputStream inputStream)
  {
    BufferedReader in = null;
    try
    {
      chars.reset();
      in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
      int n;
      while ((n = in.read(charBuf)) != -1)
        chars.write(charBuf, 0, n);
      return chars.toCharArray();
    }
    catch(IOException e)
    {
      throw new CompilerException("Cannot read source file", loc, e);
    }
    finally
    {
      try { if (in != null) in.close(); } catch (Exception e) {}
    }
  }

////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  /**
   * Constructor.  The character buffer be normalized
   * to that all newlines are represented as \n
   */
  public Tokenizer(Compiler compiler, String filename, char[] buf)
  {
    super(compiler);     
    this.filename = filename;
    this.buf = buf;
    this.pos = 0;
    this.line = 1;
    this.col  = -1;
    this.doc  = compiler != null ? compiler.doc : false;
    consume();
    consume();
  }

////////////////////////////////////////////////////////////////
// Main
////////////////////////////////////////////////////////////////

  /**
   * Tokenize the whole buffer.
   */
  public Token[] tokenize()
  {
    ArrayList acc = new ArrayList(1024);
    while (true)
    {
      Token tok = next(acc);
      if (tok != null) 
      {
        acc.add(tok);
        if (tok.type == Token.EOF) break;
      }
    }
    return (Token[])acc.toArray(new Token[acc.size()]);
  }

  /**
   * Read one token and return it, or read a series
   * of tokens and append them to the acc list and return
   * null.
   */
  private Token next(ArrayList acc)
  {
    while (true)
    {
      // whitespace
      if (isSpace(cur)) { consume(); continue; }

      // comments
      if (cur == '/' && peek == '/') { lineComment(); continue; }
      if (cur == '/' && peek == '*') { blockComment(); continue; }
      if (cur == '*' && peek == '*')
      {
        if (doc) return docComment();
        else lineComment(); continue;
      }

      // identifier
      if (isIdStart(cur)) return idOrKeyword();

      // number
      if (isDigit(cur)) return numberLiteral();

      // char literal
      if (cur == '\'') return charLiteral();

      // str literal
      if (cur == '"') { strLiteral(acc); return null; }

      // symbol
      Token symbol = symbol();
      if (symbol != null) return symbol;

      // eof of file
      if (cur < 0) return new Token(location(), Token.EOF);

      throw err("Unexpected token '" + (char)cur + "'");
    }
  }              
  
////////////////////////////////////////////////////////////////
// Productions
////////////////////////////////////////////////////////////////

  /**
   * Return identifier token.
   */
  private Token idOrKeyword()
  {
    Location loc = location();
    StringBuffer s = new StringBuffer();
    s.append((char)cur);
    consume();
    while (isIdChar(cur)) s.append((char)consume());
    String str = s.toString();

    int keyword = Token.fromKeyword(str);
    if (keyword > 0)
      return new Token(loc, keyword);
    else
      return new Token(loc, Token.ID, str);
  }

  /**
   * Return number literal.
   */
  private Token numberLiteral()
  {
    Location loc    = location();
    boolean isFloat = false;
    boolean isHex   = false; 
    boolean isWide  = false; // L/long or D/double

    // check hex
    if (cur == '0' && peek == 'x')
    {
      consume();
      consume();       
      if (cur == '[') return bufLiteral(loc);
      isHex = true;
      if (!isDigit(cur) && !isHex(cur)) 
        throw err("Invalid hex literal");
    }

    // read number into string buffer
    StringBuffer s  = new StringBuffer();
    while (isDigit(cur) || cur == '.' || cur == '_' || 
        cur == 'E' || cur == 'e' || (isHex && isHex(cur)))
    {
      if (cur == '_') { consume(); continue; }
      if (cur == '.')
      {
        if (isHex) throw err("Invalid hex literal");
        isFloat = true;
      }
      if (!isHex && (cur == 'E' || cur == 'e'))
      {
        isFloat = true;
        s.append((char)consume()).append(exponentialPart());
        break;
      }
      s.append((char)consume());
    }
    String str = s.toString();

    long time = -1;
    if (!isHex)
    {
      if (cur == 'n' && peek == 's') { consume(); consume(); time = 1L; }
      else if (cur == 'm' && peek == 's') { consume(); consume(); time = 1000000L; }
      else if (cur == 's' && peek == 'e') { consume(); consume(); if (cur != 'c') throw err("Expected 'sec' in Time literal"); consume(); time = 1000000000L; }
      else if (cur == 'm' && peek == 'i') { consume(); consume(); if (cur != 'n') throw err("Expected 'min' in Time literal"); consume(); time = 60000000000L; }
      else if (cur == 'h' && peek == 'r') { consume(); consume(); time = 3600000000000L; }
      else if (cur == 'd' && peek == 'a') { consume(); consume(); if (cur != 'y' || peek != 's') throw err("Expected 'days' in Time literal"); consume(); consume(); time = 86400000000000L; }
    }
    switch (cur)
    {
      case 'f':
      case 'F':
        consume();
        isFloat = true;
        break;
      case 'd':
      case 'D':
        consume();
        isFloat = true;
        isWide  = true;
        break;
      case 'l':
      case 'L':
        consume();
        isWide = true;
        if (isFloat) throw err("Type mismatch. Cannot convert from floating point number to long: " + str + "L", location());
        break;
    }

    // parse into int or float
    try
    {
      // if time
      if (time >= 0)
      {
        if (isFloat)
        {
          time = (long)((double)time * Double.parseDouble(str));
          return new Token(loc, Token.TIME_LITERAL, new Long(time));
        }
        else
        {
          time *= Long.parseLong(str);
          return new Token(loc, Token.TIME_LITERAL, new Long(time));
        }
      }

      // float/double literal
      if (isFloat)   
      {
        if (isWide)
          return new Token(loc, Token.DOUBLE_LITERAL, new Double(str));
        else
          return new Token(loc, Token.FLOAT_LITERAL, new Float(str));
      }

      // parse as long and check range
      long x;
      if (isHex)
      {
        if (str.length() > 16 || (!isWide && str.length() > 8)) 
          throw err("Invalid range for int/long: 0x" + str, location());
        x = TextUtil.parseHexLong(str);
        if (!isWide && x > 0xffffffffL)
          throw err("Invalid range for int: 0x" + str, location());
      }
      else
      {
        x = Long.parseLong(str);
        if (!isWide && (x < Integer.MIN_VALUE || Integer.MAX_VALUE+1L < x))
          throw err("Invalid range for int: " + str, location());
      }

      // int/long literal                
      if (isWide)
        return new Token(loc, Token.LONG_LITERAL, new Long(x));
      else      
        return new Token(loc, Token.INT_LITERAL, new Integer((int)x));
    }
    catch (CompilerException e)
    {              
      throw e;
    }
    catch (Exception e)
    {                               
      throw err("Invalid number: " + str, location());
    }
  }
  
  /**
   * Parse the Exponential part of a number that is in scientific notation.
   * Assumes the 'E' or 'e' has already been consumed.
   * 
   * @return a String containing the exponential part of the number.
   */
  private String exponentialPart()
  {
    StringBuffer sb = new StringBuffer();
    
    while (cur == '_') consume();
    if (cur == '+' || cur == '-') sb.append((char)consume());
    while (cur == '_' || isDigit(cur))
    {
      if (cur == '_' ) { consume(); continue; }
      sb.append((char)consume());
    }
    
    return sb.toString();
  }

  /**
   * Return string literal.
   */
  private Token charLiteral()
  {
    Location loc = location();
    consume();      
    
    int c;
    if (cur == '\\') 
    {
      c = escape();
    }
    else
    {
      c = cur;
      consume();
    }
    if (cur != '\'') throw err("Invalid character literal " + cur);
    consume();
    return new Token(loc, Token.INT_LITERAL, new Integer(c));
  }

  /**
   * Return string literal.
   */
  private void strLiteral(ArrayList acc)
  {
    Location loc = location();
    consume();
    StringBuffer s = new StringBuffer(); 
    boolean interpolated = false;
    while (true)
    {
      if (cur == '"') { consume(); break; }
      if (cur == '\n') throw err("Unexpected end of string literal");
      if (cur == '$')
      {                 
        // if we have detected an interpolated string, then
        // insert opening paren to treat whole string atomically
        if (!interpolated)
        {
          interpolated = true;
          acc.add(new Token(location(), Token.LPAREN));
        }
   
        // process interpolated string, it returns null
        // if at end of string literal
        if (!strInterpolation(loc, acc, s.toString()))
        {
          acc.add(new Token(location(), Token.RPAREN));
          return;
        }
   
        s.setLength(0);
        loc = location();
      }
      else if (cur == '\\') 
      {
        s.append((char)escape());
      }
      else
      {
        s.append((char)cur);
        consume();
      }
    }      
                 
    acc.add(new Token(loc, Token.STR_LITERAL, s.toString()));
    
    // if interpolated then we add rparen to treat whole atomically
    if (interpolated)
      acc.add(new Token(location(), Token.RPAREN));
  }

  
  /**
   * When we hit a $ inside a string it indicates an embedded
   * expression.  We make this look like a stream of tokens
   * such that:
   *   "a ${b} c" -> ("a " + (b) + " c")
   * Return true if more in the string literal.
   */  
  private boolean strInterpolation(Location loc, ArrayList acc, String s)
  {
    consume(); // $
    acc.add(new Token(loc, Token.STR_LITERAL, s));
    acc.add(new Token(location(), Token.PLUS));
  
    // if { we allow an expression b/w {...}
    if (cur == '{')
    {
      acc.add(new Token(location(), Token.LPAREN));
      consume();
      while (true)
      {
        if (cur == '"') throw err("Unexpected end of string, missing }");
        Token tok = next(acc);
        if (tok.type == Token.RBRACE) break;
        acc.add(tok);         
      }
      acc.add(new Token(location(), Token.RPAREN));
    }
  
    // else also allow a single identifier with
    // dotted accessors x, x.y, x.y.z
    else
    {
      Token tok = next(acc);
      if (tok.type != Token.ID) throw err("Expected identifier after $");
      acc.add(tok);
      while (true)
      {
        if (cur != '.') break;
        acc.add(next(acc)); // dot
        tok = next(acc);
        if (tok.type != Token.ID) throw err("Expected identifier");
        acc.add(tok);
      }
    }
  
    // if at end of string, all done
    if (cur == '\"')
    {
      consume();
      return false;
    }
  
    // add plus and return true to keep chugging
    acc.add(new Token(location(), Token.PLUS));
    return true;
  }      
    
  /**
   * Parse escape sequence.
   */
  private int escape()
  {
    if (cur != '\\') throw new IllegalStateException();
    consume();
    switch (cur)
    {
      case '0':  consume(); return '\0';
      case 'n':  consume(); return '\n';
      case 'r':  consume(); return '\r';
      case 't':  consume(); return '\t';
      case '"':  consume(); return '"';
      case '\'': consume(); return '\'';
      case '\\': consume(); return '\\';
      case '$':  consume(); return '$';
      default:   throw err("Invalid escape sequence " + (char)cur);
    }
  }

  /**
   * Return bytes literal 0x[...] (we've already consumed 0x).
   */
  private Token bufLiteral(Location loc)
  {                             
    Buf buf = new Buf();
    if (consume() != '[') throw new IllegalStateException();
    while (cur != ']')
    {                 
      if (isSpace(cur)) { consume(); continue; }
      if (cur == '/' && peek == '/') { lineComment(); continue; }
      if (cur == '/' && peek == '*') { blockComment(); continue; }
      if (cur == '*' && peek == '*') { lineComment(); continue; }
            
      int b = (nibble() << 4) | nibble();
      buf.write(b);
    }
    consume();
    
    return new Token(loc, Token.BUF_LITERAL, buf);
  }   
  
  private int nibble()
  {                   
    if (isDigit(cur) || isHex(cur))   
      return TextUtil.hexCharToInt((char)consume());
    else
      throw err("Expected hex digit");
  }

  /**
   * Return symbol token or null.
   */
  private Token symbol()
  {
    Location loc = location();
    switch (cur)
    {

      case '.':
        consume();
        return new Token(loc, Token.DOT);
      case ',':
        consume();
        return new Token(loc, Token.COMMA);
      case ';':
        consume();
        return new Token(loc, Token.SEMICOLON);
      case ':':
        consume();
        if (cur == ':') { consume(); return new Token(loc, Token.DOUBLE_COLON); }
        if (cur == '=') { consume(); return new Token(loc, Token.PROP_ASSIGN); }
        return new Token(loc, Token.COLON);
      case '+':
        consume();
        if (cur == '+') { consume(); return new Token(loc, Token.INCREMENT); }
        if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_PLUS); }
        return new Token(loc, Token.PLUS);
      case '-':
        consume();
        if (cur == '-') { consume(); return new Token(loc, Token.DECREMENT); }
        if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_MINUS); }
        if (cur == '>') { consume(); return new Token(loc, Token.ARROW); }
        return new Token(loc, Token.MINUS);
      case '*':
        consume();
        if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_STAR); }
        return new Token(loc, Token.STAR);
      case '/':
        consume();
        if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_SLASH); }
        return new Token(loc, Token.SLASH);
      case '%':
        consume();
        if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_PERCENT); }
        return new Token(loc, Token.PERCENT);
      case '&':
        consume();
        if (cur == '&') { consume(); return new Token(loc, Token.DOUBLE_AMP); }
        if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_AMP); }
        return new Token(loc, Token.AMP);
      case '|':
        consume();
        if (cur == '|') { consume(); return new Token(loc, Token.DOUBLE_PIPE); }
        if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_PIPE); }
        return new Token(loc, Token.PIPE);
      case '^':
        consume();
        if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_CARET); }
        return new Token(loc, Token.CARET);
      case '?':
        consume();
        if (cur == '.') { consume(); return new Token(loc, Token.SAFE_NAV); }
        if (cur == ':') { consume(); return new Token(loc, Token.ELVIS); }
        return new Token(loc, Token.QUESTION);
      case '!':
        consume();
        if (cur == '=') { consume(); return new Token(loc, Token.NOT_EQ); }
        return new Token(loc, Token.BANG);
      case '~':
        consume();
        return new Token(loc, Token.TILDE);
      case '<':
        consume();
        if (cur == '<')
        {
          consume();
          if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_LSHIFT); }
          return new Token(loc, Token.LSHIFT);
        }
        if (cur == '=') { consume(); return new Token(loc, Token.LT_EQ); }
        return new Token(loc, Token.LT);
      case '>':
        consume();
        if (cur == '>')
        {
          consume();
          if (cur == '=') { consume(); return new Token(loc, Token.ASSIGN_RSHIFT); }
          return new Token(loc, Token.RSHIFT);
        }
        if (cur == '=') { consume(); return new Token(loc, Token.GT_EQ); }
        return new Token(loc, Token.GT);
      case '{':
        consume();
        return new Token(loc, Token.LBRACE);
      case '}':
        consume();
        return new Token(loc, Token.RBRACE);
      case '[':
        consume();
        return new Token(loc, Token.LBRACKET);
      case ']':
        consume();
        return new Token(loc, Token.RBRACKET);
      case '(':
        consume();
        return new Token(loc, Token.LPAREN);
      case ')':
        consume();
        return new Token(loc, Token.RPAREN);
      case '=':
        consume();
        if (cur == '=') { consume(); return new Token(loc, Token.EQ); }
        return new Token(loc, Token.ASSIGN);
      case '@':
        consume();
        return new Token(loc, Token.AT);
    }
    return null;
  }

  /**
   * Skip // line comment
   */
  private void lineComment()
  {
    consume();  // first slash
    consume();  // second slash
    while (cur != '\n' && cur > 0) consume();
    consume(); // newline
  }

  /**
   * Skip slash/start block comment
   */
  private void blockComment()
  {
    int depth = 1;
    consume();  // slash
    consume();  // star
    while (cur > 0)
    {
      if (cur == '*' && peek == '/')
      {
        depth--;
        if (depth <= 0) break;
      }
      if (cur == '/' && peek == '*') depth++;
      consume();
    }
    consume(); // star
    consume(); // slash
  }

  /**
   * Return a ** sedona doc comment.
   */
  private Token docComment()
  {
    Location loc = location();
    while (cur == '*') consume();
    if (cur == ' ') consume();

    // parse comment
    StringBuffer s = new StringBuffer();
    while (cur > 0)
    {
      // add to buffer and advance
      int c = cur;
      s.append((char)c);
      consume();

      // if not at newline, then loop
      if (c != '\n') continue;

      // we at a newline, check for leading whitespace(0+)/star(2+)/whitespace(1)
      while (cur == ' ' || cur == '\t') consume();
      if (cur != '*' || peek != '*') break;
      while (cur == '*') consume();
      if (cur == ' ' || cur == '\t') consume();
    }

    String doc = s.toString().trim();
    return new Token(loc, Token.DOC_COMMENT, doc);

  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  /**
   * Throw compiler error with current location.
   */
  public CompilerException err(String msg)
  {
    return super.err(msg, location());
  }

  /**
   * Current location
   */
  private Location location()
  {
    return new Location(filename, line, col);
  }

////////////////////////////////////////////////////////////////
// Read
////////////////////////////////////////////////////////////////

  /**
   * Consume next character and return it.
   */
  private int consume()
  {
    if (cur < 0) throw err("Unexpected end of file");
    int result = cur;
    cur = peek;
    peek = (pos < buf.length) ? buf[pos++] : -1;
    if (cur == '\n') { line++; col = 0; }
    else col++;
//System.out.println(" consume " + location() + " cur=" + (char)cur + " peek=" + (char)peek + " pos=" + pos);
    return result;
  }

////////////////////////////////////////////////////////////////
// Table
////////////////////////////////////////////////////////////////

  boolean isIdStart(int c) { return c > 0 && c < 128 ? (charMap[c] & ID_START) != 0 : false; }
  boolean isIdChar(int c)  { return c > 0 && c < 128 ? (charMap[c] & ID_CHAR) != 0 : false; }
  boolean isDigit(int c)   { return c > 0 && c < 128 ? (charMap[c] & DIGIT) != 0 : false; }
  boolean isHex(int c)     { return c > 0 && c < 128 ? (charMap[c] & HEX) != 0 : false; }
  boolean isSpace(int c)   { return c > 0 && c < 128 ? (charMap[c] & SPACE) != 0 : false; }

  static final byte[] charMap = new byte[128];
  static final int ID_START = 0x01;
  static final int ID_CHAR  = 0x02;
  static final int SPACE    = 0x04;
  static final int DIGIT    = 0x08;
  static final int HEX      = 0x10;
  static
  {
    for (int i='A'; i<='Z'; ++i) charMap[i] = ID_START | ID_CHAR;
    for (int i='a'; i<='z'; ++i) charMap[i] = ID_START | ID_CHAR;
    for (int i='0'; i<='9'; ++i) charMap[i] = ID_CHAR | DIGIT;
    for (int i='a'; i<='f'; ++i) charMap[i] |= HEX;
    for (int i='A'; i<='F'; ++i) charMap[i] |= HEX;
    charMap['_']  = ID_START | ID_CHAR;
    charMap[' ']  = SPACE;
    charMap['\t'] = SPACE;
    charMap['\n'] = SPACE;
    charMap['\r'] = SPACE;
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
      Tokenizer t = new Tokenizer(new Compiler(), f.getName(), readFile(f));
      Token[] toks = t.tokenize();
      for (int i=0; i<toks.length; ++i)
      {
        Token tok = toks[i];
        if (tok.type == Token.EOF) break;
        String s = tok.toString();
        if (tok.type == Token.ID) s = "@" + s;
        System.out.println(TextUtil.padRight(tok.loc + ": ", 20) + s);
      }
    }
    catch(CompilerException e)
    {
      System.out.println(e.toLogString());
    }
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  String filename;
  char[] buf;
  int pos;
  int cur;
  int peek;
  int line;
  int col;
  boolean doc;
  Token last = new Token(new Location(null, 0, 0), Token.EOF);

}
