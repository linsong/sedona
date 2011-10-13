//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
package sedonac.test;

/**
 * Note: All the conditions in these tests are meaningless except for the 
 * boolean literals. If we ever optimize expressions to determine values
 * for boolean expressions (constant propagation) this test will need to
 * be re-worked.
 * 
 * @author Matthew Giannini
 * @creation Nov 19, 2009
 *
 */
public class DeadCodeTest extends CompileTest
{
  private static final String[] neverExits = {"never exits"};
  private static final String[] line3col1 = {"3:1:"};
  public void testBasic()
  {
    verify("");
    verify("return");
    verify("int i=0; i+= 1; return");
  }
  
  public void testIf()
  {  
    // Ok
    verify(
      "if (x<1) {x++}\n" +
      "if (false){}else{x+=1}\n" +
      "if (true) {x+=1}\n" +
      "if (x<1){x+=1}else{x+=2}\n" + 
      "if (x<1) x+=1; else if (x<2) return; else return; x+=100\n" +
      "if (x<1) return; else if (x<2) x+=2; else return; x+=100\n" +
      "if (x<1) return; else if (x<2) return; else x+=3; x+=100\n");
    
    // Warnings
    verify(
      "if (false)\n" +
      "x+=1; else x += 2", line3col1);
    
    verify(
      "if (true) x+=1; else\n" +
      "x+=2", line3col1);
    
    verify(
      "if (true) return\n" +
      "x+=1", line3col1);
    
    verify(
      "if (x<1) return; else return\n" +
      "x+=1", line3col1);
    
    verify(
      "if (x<1) return; else if (x<2) return; else return\n" +
      "x+=1", line3col1);
    
    // Nesting Test Start
    verify(
      "if (x<100){ if (x<1) return; else if (x<2) return; else if (x<3) return; }\n" +
      "else { return }\n" +
      "x+=1");
    
    verify(
      "if (false){\n" + 
      "if (x<1) return; else if (x<2) return; else if (x<3)\n" + //dead
      "return; }\n" + // dead
      "else { return }\n" +
      "x+=1", new String[]{"3:1 - 4:1", "6:1:"});    
      
    verify(
      "if (true){ if (x<1) return; else if (x<2) return; else if (x<3) return; } else\n" +
      "return;\n" +
      "x+=1", line3col1);
    // Nesting Test End
  }
  
  public void testWhile()
  {
    verify(
      "while(x<10){x+=1} x+=100\n" +
      "while(true){break} x+=100\n" +
      "while(true){if(x==0) break; x+=1} x+=100\n" +
      "while(false){} x+=100\n");
    
    verify(
      "while(true){break;\n" +
      "x+=1}\n" + // warn 3:1:
      
      "while(false){\n" +
      "if (x<1)\n" +  // warn 5:1 -
      "x+=1}\n" +     // 6:1
      
      "while(x<1){continue\n" +
      "x+=1}\n" + // warn 8:1:
      "x+=1",
      new String[]{"3:1:", "5:1 - 6:1", "8:1:"});
      
    verify(
      "while(true){}\n" +
      "x+=1", line3col1, neverExits);
  }
  
  public void testDoWhile()
  {
    verify(
      "do { x += 1 } while(x<0);\n" +
      "do { x += 1; continue } while(x<0);\n" +
      "do { if (x%2==0) break; x+=1 } while(x<0);\n");
    
    verify(
      "do { break }while(\n" +
      "x<10);\n" + // warn 3:1:
      
      "do { continue\n" +
      "x+=1}while(x<10);\n" + // warn 5:1:
      
      "do { break\n" +
      "x+=1}while(\n" +  // warn 7:1 -
      "x<10);\n",        // warn 8:1
      new String[]{"3:1:", "5:1:", "7:1 - 8:1"});
    
    verify(
      "do {} while(true);\n" +
      "x+=1", line3col1, neverExits);
  }
  
  public void testFor()
  {
    verify(
      "for(;;){break} x+=100\n" +
      "for(int i=0;;){break} x+=100\n" +
      "for(int i=0;;++i){if(i>0) break } x+=100\n" +
      "for(int i=0;i<10;++i) {if(i==1) continue} x+=100\n" +
      "for(int i=0;i<10;++i) {continue}\n" +
      "for(int i=0;i<10;++i){} x+=100\n" +
      "for(int i=0;i<10;++i){x+=1} x+=100\n");
    
    verify(
      "for(int i=0; i<10;\n" +
      "++i) { break }\n" + // warn 3:1:
      
      "for(int i=0; i<10;\n" +
      "++i) { break;\n" +  // warn 5:1
      "x+=100}\n",         // warn 6:1  
      new String[] {"3:1:","5:1:","6:1:"});
    
    verify(
      "for(;;){}\n" +
      "x+=100", line3col1, neverExits);
  }
  
  public void testSwitch()
  {
    verify(
      "switch(x){} x+=100\n" +
      "switch(x){case 0: break} x+=100\n" +
      "switch(x){default: x+=1} x+=100\n" +
      "switch(x){case 0: x+=1; case 1: case 2: x+=2; default: x+=1} x+=100\n" +
      "switch(x){case 0: break; default: x+=1} x+=100\n" +
      "switch(x){case 0: x+=0} x+=100\n");
    
    verify(
      "switch(x){ case 0: default: break;\n" +
      "x+=2}\n" +  // warn 3:1:
      
      "switch(x){ default: return }\n" +
      "x+=3\n",   // warn 5:1:
      new String[] {"3:1:","5:1:"});
  }
  
  public void testGoto()
  {
    verify(
      "goto A; A: goto C; B: x += 1; return; C: goto B\n");
    
    verify(
      "goto A\n" +
      "x += 1\n" + // warn 3:1 -
      "x += 2\n" + // 4:1
      "A: return\n",
      new String[] {"3:1 - 4:1"});
    
    verify(
      "goto A\n" +
      "x += 1\n" + // warn 3:1:
      "A: x += 2; goto A\n" +
      "x += 3\n",  // warn 5:1
      new String[]{"3:1:","5:1:"}, neverExits);
  }
  
  private void verify(String b) { verify(b, new String[0], new String[0]); }
  private void verify(String b, String[] warns) { verify(b, warns, new String[0]); }
  private void verify(String b, String[] warns, String[] errs)
  {
    compile(genClass(b));
    verify(compiler.warnings.size() == warns.length);
    // NOTE: if this ever causes problems because we change log format, we
    // can comment out.
    for (int i=0; i<warns.length; ++i)
    {
//      System.out.println("\nWARN: " + compiler.warnings.get(i));
      verify(compiler.warnings.get(i).toString().indexOf(warns[i]) >= 0);
    }
    
    verify(compiler.errors().length == errs.length);
    for (int i=0; i<errs.length; ++i)
      verify(compiler.errors()[i].toLogString().indexOf(errs[i]) >= 0);
  }
  
  /**
   * Method body is always started at line 2
   */
  private String genClass(String methodBody)
  {
    return new StringBuffer()
    .append("class DeadCode extends Component { int x = 0; void dead() {\n")
    .append(methodBody).append("\n}")
    .append("\n}").toString();
  }

}
