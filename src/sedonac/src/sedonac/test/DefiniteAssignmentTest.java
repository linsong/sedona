//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.test;

import sedonac.CompilerException;

/**
 * Test definite assignment
 *
 * @author Matthew Giannini
 * @creation Nov 19, 2009
 *
 */
public class DefiniteAssignmentTest extends CompileTest
{  
  public void testUnary()
  {
    verify(
      "z=0; b1=\n" +
      "!b2;\n" + // err 3:2
      "--a;\n" + // err 4:3
      "++b;\n" + // err 5:3
      "c--;\n" + // err 6:1
      "d++;\n" + // err 7:1
      "z=~e;\n" + // err 8:4
      "",
      new String[]{"3:2:","4:3:","5:3:","6:1:","7:1:","8:4:"});
  }
  
  public void testOpEq()
  {
    verify(
      "z=0;\n" +
      "a |= 0x01\n" + // err 3:1
      "b &= 0x02\n" + // err 4:1
      "c ^= 0x04\n" + // err 5:1
      "d <<= 2\n"   + // err 6:1
      "e >>= 1\n"   + // err 7:1
      "",
      new String[]{"3:1:","4:1:","5:1:","6:1:","7:1:",});
    
    verify(
      "z=0\n" +
      "a += 1\n" + // err 3:1
      "b -= 1\n" + // err 4:1
      "c *= 1\n" + // err 5:1
      "d /= 1\n" + // err 6:1
      "e %= 1\n" + // err 7:1
      "",
      new String[]{"3:1:","4:1:","5:1:","6:1:","7:1:",});
  }
  
  public void testBool()
  {
    verify(
      "z=0; if (((a=1)==1) && (1<2)){x=0} a+=1\n" +
      "if (true && true && ((b=1) == 1)){x=0} b+=1\n" +
      "if (false || ((c=1)==1)){x=0} c+=1\n" +
      "if (false || false || ((d=1)==1)){x=0} d+=1\n" +
      
      "if ( (((e=0)==0) && (1<2)) && false){x=0} e+=1\n"
      );
    
    verify(
      "z=0; if (1<2 && ((a=0)==0)){x=0}\n" +
      "a+=1\n" + // err 3:1
      
      "if (false && ((b=1) == 1)){x=0}\n" +
      "b+=1\n" + // err 5:1
      
      "if (true && (1<2) && ((c=1)==1)){x=0}\n" +
      "c+=1\n" + // err 7:1
      
      "if (true || ((d=1)==1)){x=0}\n" +
      "d+=1\n" + // err 9:1
      
      "if ((1<2) || ((e=0)==0)){x=0}\n" +
      "e+=1\n" + // err 11:1
      
      "if ( ((1<2) && ((f=0)==0)) || ((f=0)==0)){x=0}\n" +
      "f+=1\n", // err 13:1
      
      new String[]{"3:1:","5:1:","7:1:","9:1:","11:1:","13:1:"});
  }
  
  public void testIfAndTernary()
  {
    verify(
      "z = 0; if (x<0) a = 1; else if (x<1) a = 2; else a = 3; a += 1\n" +
      "if (x<0) { if (x<1) b = 1; else b = 2 } else{ if (x<2) b = 3; else b = 4} b += 1\n" +
      "if (true) c = 1; c += 1\n" +
      "if (false) {x+=1} else {d=1} d+=1\n" +
      "z = (x<0) ? (e=0) : (f=0)\n");
    
    verify(
      "z=0; if (x<0) a = 0\n" +
      "a += 1\n" + // err 3:1:
      
      "if (true) {x+=1} else {b=1}\n" +
      "b += 1\n" + // err 5:1:
      
      "if (false) {c=1} else {x+=1}\n" +
      "c += 1\n" +   // err 7:1:
      
      "if (x<1) d = 0; else if (x<2) d = 1\n" +
      "d += 1\n",    // err 9:1:
      new String[]{"3:1:", "5:1:", "7:1:", "9:1:"});
    
    verify(
      "z = (x<0) ?\n"+
      "a :\n" +
      "b", new String[] {"3:1:","4:1:"});
    
    String[] line3col1 = new String[]{"3:1:"};
    verify(
      "z = ((a=0) == 0) ? a :\n" +
      "b", line3col1);
    
    verify(
      "z = (x==0) ? (a=0) :\n" +
      "b", line3col1);
    
    verify(
      "z = (x==0) ?\n" +
      "a : (a=0)", line3col1);
  }
  
  public void testFor()
  {
    verify(
      "z=0; for(a=0; a<10; ++a){}\n" +
      "for(int i=0; i<10; ++b){b=0}\n" +
      "for(int i=0; (c=i)<10; ++i){c+=1}\n" +
      "for(int i=(d=0); d<10; ++d){}\n" +
      "for(;;){e=0; break} e+=1\n");
    
    verify(
      "z=0; for(;\n" +
      "a<10; ++a){}\n" + // err 3:1
      
      "for(;x<10;\n" +
      "b++){}\n" + // err 5:1
      
      "for(;;){\n" + 
      "c++; break}\n" + // err 7:1
      
      "for(int i=0; i<10; ++i) d=0\n" +
      "d+=1\n",        // err 9:1
      new String[]{"3:1:","5:1:","7:1:","9:1:"});
  }
  
  public void testSwitch()
  {
    verify(
      "switch(z=x){case 0: default: a=0} a+=1\n" +
      "switch(x){default: b=0} b+=1\n");
    
    verify(
      "switch(z=x) {case 0: a=0}\n" +
      "a+=1\n" + // err 3:1
      
      "switch(x) {case 0: b=0; default: x=0}\n"+
      "b+=1\n" + // err 5:1
      
      "switch(x) {case 1: c=0; case 2: break; case 3: case 4: c=4; break; case 5: default: c = 6}\n" +
      "c+=1\n", // err 7:1
      new String[]{"3:1:", "5:1:", "7:1:"});
  }
  
  public void testWhile()
  {
    verify(
      "while((z=0) < 1){}\n");
    
    verify(
      "z=0; while(true) { if(1<2){break} a=0 }\n" +
      "a+=1\n" + // err 3:1
      
      "while(Sys.ticks()<1000L){b=0}\n" +
      "b+=1\n" + // err 5:1
      "",
      new String[]{"3:1:","5:1:"});
  }
  
  public void testDoWhile()
  {
    verify(
      "z=0; do{a=0}while(1<2); a+=1\n" +
      "do{continue}while((b=0)<1); b+=1\n"+
      "");
    
    verify(
      "z=0; do{ if(Sys.ticks()<1L){a=0}else{break} }while(true);\n" +
      "a+=1\n" + // err 3:1
      
      "do{ if(Sys.ticks()<1L){break} b=0 }while(true);\n" +
      "b+=1\n" + // err 5:1
      
      "do{ if(Sys.ticks()<1L){break} }while((c=0)<1);\n" +
      "c+=1\n" + // err 7:1
      "",
      new String[]{"3:1:","5:1:","7:1:"});
  }
  
  public void testReturn()
  {
    verify(
      "z=0;if (Sys.ticks()>1L){ if (Sys.ticks()>1L){a=0}else{return 1} }else{return 1}a+=1\n" +
      "while (true) { if(Sys.ticks()>1L) return 1; else if (Sys.ticks()>2L) return 2; else b=0 }b+=1\n" +
      "do{ if(Sys.ticks()>1L) return 0; else c=0; }while(1<2); c+=1\n"
      );
    
    // this fails because the the genClass returns "z" which must be assigned
    // by our test code
    verify("", new String[]{"3:8:"});
  }
  
  public void testMisc()
  {
    verify(
      "z =\n" +
      "s1.length() + \n" + // err 3:1 - test call
      "strings[0]?.length()\n" + // err 4:1 - array
      
      "Sys.intStr(\n" +
      "a)\n" +// err 6:1 - param to call
      
      "ints[\n" +
      "b] = 0\n" + // err 8:1 - index not assigned
      "",
      new String[]{"3:1:","4:1:","6:1:","8:1:"});
  }
  
  private void verify(String b) { verify(b, new String[0]); }
  private void verify(String b, String[] errs)
  {
    compile(genClass(b));
    CompilerException[] e = compiler.errors();
    verify(e.length == errs.length);
    {
      for (int i=0; i<e.length; ++i)
      {
        verify(e[i].toLogString().indexOf(errs[i]) >= 0);
      }
    }
  }
  
  /**
   * Method body is always started at line 2.  All tests must definitely assign
   * "z", or the test will fail
   */
  private String genClass(String methodBody)
  {
    return new StringBuffer()
    .append("class DefAss extends Component { static inline int[3] ints; int x = 0;")
    .append("int func(int param) {int a; int b; int c; int d; int e; int f; int g; int z;")
    .append("Str s1; Str[] strings; bool b1; bool b2;\n")
    .append(methodBody) // line 2
    .append("\nreturn z\n}")
    .append("\n}").toString();
  }

}
