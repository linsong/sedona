//
// Copyright (c) 2000 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   24 May 00  Brian Frank  Creation
//

package sedonac.util;

import java.io.*;
import java.util.*;
import sedona.Env;
import sedonac.*;

/**
 * CStrGen translates a binary input file into a C
 * source file with as a C string literal.
 */
public class CStrGen
{

  public static void main(String[] args)
    throws Exception
  {
    if (args.length < 1)
    {
      System.out.println("usage: CStrGen <infile> ");
      return;
    }

    File inFile = new File(args[0]);
    int dot = inFile.getName().indexOf('.');
    boolean scode = false;
    
    String suffix;
    if (inFile.getName().endsWith(".scode"))
    {
      suffix = "_scode";
      scode = true;
    }  
    else if (inFile.getName().endsWith(".sab"))
      suffix = "_sab";
    else
    {        
      System.out.println("infile must by .scode or .sab");
      return;
    }      

    File outFile = new File(inFile.getParentFile(), inFile.getName().substring(0, dot) + suffix + ".c");
    
    System.out.println("in:  " + inFile);
    System.out.println("out: " + outFile);
    toCFile(inFile, outFile, scode ? "scode" : "sab");

  }
  public static void toCFile(final File inFile, final File outFile,
                             final String varPrefix) throws Exception
  {
    InputStream in = new BufferedInputStream(new FileInputStream(inFile));
    PrintWriter out = new PrintWriter(new FileWriter(outFile));

    int c;
    out.print("// auto generated sedonac " + Env.timestamp() + "\n");
    out.print("\n");
    out.print("#include \"sedona.h\"\n");
    out.print("\n");
    
    out.print("const unsigned int " + varPrefix + "Size = " + inFile.length() + ";\n");
    out.print("const uint8_t " + varPrefix + "Image[] = \n{\n");

    String line = "";
    while ((c = in.read()) >= 0)
    {
      if (line.length() > 75)
      {
        line += "\n";
        out.print(line);
        line = "";
      }

      line += "0x" + hex(c) + ",";
//      if (c == 0)
//        line += "\\0";
//      else if (c == '\\')
//        line += "\\\\";
//      else if (c == '"')
//        line += "\\\"";
//      else if (' ' <= c && c <= 127)
//        line += "" + (char)c;
//      else
//        line += "\\x" + hex(c);

    }
    line = line.substring(0, line.length()-1);
    line += "\n};\n";
    out.print(line);

    in.close();
    out.close();
  }

  private static String hex(int c)
  {
    if (c < 0x10) return "0" + Integer.toHexString(c);
    return Integer.toHexString(c);
  }

}
