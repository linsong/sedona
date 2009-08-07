//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Feb 07  Brian Frank  Creation
//

package sedonac.test;

import sedona.Buf;
import sedonac.Compiler;
import sedonac.parser.*;

/**
 * TokenizerTest
 */
public class TokenizerTest
  extends Test
{

  public void test()
  {
    // id literals
    verifyId("a");
    verifyId("x010");

    // int literals
    verifyInt("0", 0);
    verifyInt("2", 2);
    verifyInt("123", 123);
    verifyInt("123456789", 123456789);
    verifyInt("0xab", 0xab);
    verifyInt("0x1234ABCD", 0x1234ABCD);
    verifyInt("0xffffffff", 0xffffffff);
    verifyInt("0xf0ffffff", 0xf0ffffff);
    verifyInt("0x7fffffff", 0x7fffffff);          
    verifyInt("2147483647", 2147483647);
    verifyInt("'x'", 'x');
    verifyInt("'!'", '!');
    verifyInt("'\n'", '\n');
    verifyInt("'\r'", '\r');
    verifyInt("'\t'", '\t');
    verifyInt("'\0'", '\0');
    verifyInt("'\\\\'", '\\');
    verifyInt("'\"'", '"');
    verifyInt("'\\''", '\'');     
    verifyBad("0x1aabbccdd");
    verifyBad("0xaabbccdd_00112233");
    verifyBad("2147483649");

    // long literals
    verifyLong("0L", 0);
    verifyLong("66l", 66);
    verifyLong("1234567890L", 1234567890L);
    verifyLong("1234567890_000L", 1234567890000L);
    verifyLong("0xaabbccddl", 0xaabbccddL);
    verifyLong("0xaabbccdd_00112233L", 0xaabbccdd00112233L);
    verifyBad("398387493938383983954943943L");
    verifyBad("0x1_aabbccdd_00112233L");

    // float literals
    verifyFloat("0f", 0.0f);
    verifyFloat("0e0", 0.0f);
    verifyFloat("0e10000", 0.0f);
    verifyFloat("2f", 2f);
    verifyFloat("0.0", 0.0f);
    verifyFloat("0.0e0", 0.0f);
    verifyFloat("0.2", 0.2f);
    verifyFloat("2e-1", 0.2f);
    verifyFloat("0.2e-1", 0.02f);
    verifyFloat("200f", 200f);
    verifyFloat("2e2", 200f);
    verifyFloat("2e+2", 200f);
    verifyFloat("12345.0088", 12345.0088f);
    verifyFloat("1_0e__1__", 100.0f);
    verifyFloat("1e2f", 100.0f);
    verifyFloat("1.2e1", 12.0f);

    // double literals
    verifyDouble("0d", 0.0d);
    verifyDouble("0e0d", 0.0d);
    verifyDouble("0e100d", 0.0d);
    verifyDouble("2D", 2d);
    verifyDouble("0.0d", 0.0d);
    verifyDouble("0.0e0d", 0.0d);
    verifyDouble("0.2d", 0.2d);
    verifyDouble("2e-1d", 0.2d);
    verifyDouble("200D", 200d);
    verifyDouble("2e2d", 200d);
    verifyDouble("2e+2d", 200d);
    verifyDouble("12345.0088D", 12345.0088d);
    verifyDouble("1.23450088e4D", 12345.0088d);

    // time literals
    verifyTime("0ns",        0);
    verifyTime("0min",       0);
    verifyTime("0ns",        0);
    verifyTime("5ns",        5);
    verifyTime("1ms",        1000L*1000L);
    verifyTime("1sec",       1000L*1000L*1000L);
    verifyTime("1min",       60L*1000L*1000L*1000L);
    verifyTime("1hr",        60L*60L*1000L*1000L*1000L);
    verifyTime("0.5ms",      500L*1000L);
    verifyTime("3.2ms",      3200L*1000L);
    verifyTime("0.001sec",   1000L*1000L);
    verifyTime("0.25min",    15L*1000L*1000L*1000L);
    verifyTime("24hr",       24L*60L*60L*1000L*1000L*1000L);
    verifyTime("876000hr",   876000L*60L*60L*1000L*1000L*1000L);  // 100yr
    verifyTime("1days",      24L*60L*60L*1000L*1000L*1000L); // 1day
    verifyTime("0.5days",    12L*60L*60L*1000L*1000L*1000L); // 1/2yr
    verifyTime("30days",     30L*24L*60L*60L*1000L*1000L*1000L); // 1day
    verifyTime("36500days",  876000L*60L*60L*1000L*1000L*1000L);  // 100yr

    // string literals
    verifyStr("", "");
    verifyStr("x", "x");
    verifyStr("\\\"hi\\\"", "\"hi\"");
    verifyStr("\\\\", "\\");
    verifyStr("hello world", "hello world");
    verifyStr("hello\\nworld", "hello\nworld");
    verifyStr("{\\t\\n\\r\\0}", "{\t\n\r\0}");

    // bytes literals 
    verifyBuf("0x[]", new byte[] {});
    verifyBuf("0x[ ]", new byte[] {});
    verifyBuf("0x[00]", new byte[] {0});
    verifyBuf("0x[0001]", new byte[] {0, 1});
    verifyBuf("0x[0001]", new byte[] {0, 1});
    verifyBuf("0x[cafe BABE]", new byte[] {(byte)0xca, (byte)0xfe, (byte)0xba, (byte)0xbe});
    verifyBuf("0x[0123 4567 89ab cd ef]", new byte[] {0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef});

    // symbols
    verifySymbol(Token.DOT, ".");
    verifySymbol(Token.COMMA, ",");
    verifySymbol(Token.SEMICOLON, ";");
    verifySymbol(Token.COLON, ":");
    verifySymbol(Token.PLUS, "+");
    verifySymbol(Token.MINUS, "-");
    verifySymbol(Token.STAR, "*");
    verifySymbol(Token.SLASH, "/");
    verifySymbol(Token.PERCENT, "%");
    verifySymbol(Token.AMP, "&");
    verifySymbol(Token.PIPE, "|");
    verifySymbol(Token.DOUBLE_AMP, "&&");
    verifySymbol(Token.DOUBLE_PIPE, "||");
    verifySymbol(Token.CARET, "^");
    verifySymbol(Token.QUESTION, "?");
    verifySymbol(Token.BANG, "!");
    verifySymbol(Token.TILDE, "~");
    verifySymbol(Token.LSHIFT, "<<");
    verifySymbol(Token.RSHIFT, ">>");
    verifySymbol(Token.EQ, "==");
    verifySymbol(Token.NOT_EQ, "!=");
    verifySymbol(Token.GT, ">");
    verifySymbol(Token.GT_EQ, ">=");
    verifySymbol(Token.LT, "<");
    verifySymbol(Token.LT_EQ, "<=");
    verifySymbol(Token.INCREMENT, "++");
    verifySymbol(Token.DECREMENT, "--");
    verifySymbol(Token.ARROW, "->");
    verifySymbol(Token.LBRACE, "{");
    verifySymbol(Token.RBRACE, "}");
    verifySymbol(Token.LBRACKET, "[");
    verifySymbol(Token.RBRACKET, "]");
    verifySymbol(Token.LPAREN, "(");
    verifySymbol(Token.RPAREN, ")");
    verifySymbol(Token.ASSIGN, "=");

    // keywords
    verifyEq(tokenize("class")[0].type, Token.CLASS);
    verifyEq(tokenize("int")[0].type,   Token.INT);
    verifyEq(tokenize("while")[0].type, Token.WHILE);

    // comments
    verifyTokens("8 /* 1 2 3 */ 6f", new int[] { Token.INT_LITERAL, Token.FLOAT_LITERAL });
    verifyTokens("hi // 1 2 \n \"3\"+u", new int[] { Token.ID, Token.STR_LITERAL, Token.PLUS, Token.ID});
  }

  void verifyBad(String s)
  {  
    Exception ex = null;            
    try
    {
      Token t = tokenize(s)[0];
      System.out.println("ERROR: " + t);
    }
    catch (Exception e)
    {
      // System.out.println("  " + e);
      ex = e;
    }                        
    verify(ex != null);
  }

  void verifyId(String s)
  {
    Token t = tokenize(s)[0];
    verifyEq(t.type, Token.ID);
    verifyEq(t.value, s);
  }

  void verifyStr(String s, String expected)
  {
    Token t = tokenize("\"" + s + "\"")[0];
    verifyEq(t.type, Token.STR_LITERAL);
    verifyEq(t.value, expected);
  }

  void verifyInt(String s, int expected)
  {
    Token t = tokenize(s)[0];
    verifyEq(t.type, Token.INT_LITERAL);
    verifyEq(t.value, new Integer(expected));
  }

  void verifyLong(String s, long expected)
  {
    Token t = tokenize(s)[0];
    verifyEq(t.type, Token.LONG_LITERAL);
    verifyEq(t.value, new Long(expected));
  }

  void verifyFloat(String s, float expected)
  {
    Token t = tokenize(s)[0];
    verifyEq(t.type, Token.FLOAT_LITERAL);
    verifyEq(t.value, new Float(expected));
  }

  void verifyDouble(String s, double expected)
  {
    Token t = tokenize(s)[0];
    verifyEq(t.type, Token.DOUBLE_LITERAL);
    verifyEq(t.value, new Double(expected));
  }

  void verifyTime(String s, long expected)
  {
    Token t = tokenize(s)[0];
    verifyEq(t.type, Token.TIME_LITERAL);
    verifyEq(t.value, new Long(expected));
  }

  void verifyBuf(String s, byte[] expected)
  {                  
    Token[] toks = tokenize(s);
    Token t = toks[0];
    verifyEq(t.type, Token.BUF_LITERAL);
    Buf actual = (Buf)t.value;
    verifyEq(actual.size, expected.length);
    for (int i=0; i<actual.size; ++i)
      verifyEq(actual.bytes[i], expected[i]);
  }

  void verifySymbol(int type, String s)
  {
    Token t = tokenize(s)[0];
    verifyEq(t.type, type);
    verifyEq(t.toString(), s);
  }

  void verifyTokens(String s, int[] ids)
  {
    Token[] toks = tokenize(s);
    verifyEq(toks.length-1, ids.length);
    for (int i=0; i<ids.length; ++i)
      verifyEq(toks[i].type, ids[i]);
    verifyEq(toks[toks.length-1].type, Token.EOF);
  }

  Token[] tokenize(String s)
  {
    return new Tokenizer(new Compiler(), "test", s.toCharArray()).tokenize();
  }

}
