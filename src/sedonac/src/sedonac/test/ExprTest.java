//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Feb 07  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import sedonac.Compiler;

/**
 * ExprTest
 */
public class ExprTest
  extends CompileTest
{

/* TODO
  public void testInt()
  {
    verifyInt(6, "*", 3, 18);
    verifyInt(1000, "*", 33, 33000);
    verifyInt(6, "/", 3, 2);
    verifyInt(8, "%", 3, 2);
    verifyInt(2, "+", 3, 5);
    verifyInt(50, "-", 2, 48);
    verifyInt(0xa734, "|", 0x3fc5, 0xa734|0x3fc5, true);
    verifyInt(0xa734, "&", 0x3fc5, 0xa734&0x3fc5, true);
    verifyInt(0xa734, "^", 0x3fc5, 0xa734^0x3fc5, true);
    verifyInt(0xf0f0f0f0, "|", 0x12345678, 0xf0f0f0f0|0x12345678, true);
    verifyInt(0xf0f0f0f0, "^", 0x12345678, 0xf0f0f0f0^0x12345678, true);
    verifyInt(0xf0f0f0f0, "&", 0x12345678, 0xf0f0f0f0&0x12345678, true);
  }

  public void verifyInt(int a, String op, int b, int expected) { verifyInt(a, op, b, expected, false); }
  public void verifyInt(int a, String op, int b, int expected, boolean hex)
  {
    String as = hex ? "0x" + Integer.toHexString(a) : ""+a;
    String bs = hex ? "0x" + Integer.toHexString(b) : ""+b;
    String src =
      "class Foo\n" +
      "{\n" +
      "  int x() { return " + as + op + bs + "}\n" +
      "}\n";
    compile(src);

    VM vm = new VM(image);
    Int r = (Int)vm.main();
    verifyEq(r.val, expected);
  }
*/

}
