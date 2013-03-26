//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Feb 07  Brian Frank  Creation
//

package sedonac.util;

import java.io.*;
import java.util.*;

/**
 * SCodeGen
 */
public class SCodeGen
{

//////////////////////////////////////////////////////////////////////////
// Main
//////////////////////////////////////////////////////////////////////////

  public static void main(String[] args)
    throws Exception
  {
    String lang = args[0];
    File dataFile = new File(args[1]);
    File inFile   = new File(args[2]);
    File outFile  = new File(args[3]);

    System.out.println();
    System.out.println("lang:  " + lang);
    System.out.println("data:  " + dataFile);
    System.out.println("in:    " + inFile);
    System.out.println("out:   " + outFile);

    readData(dataFile);

    if (lang.equals("java"))
      writeJava(inFile, outFile);
    else
      writeH(inFile, outFile);

    System.out.println("done!");
    System.out.println();
  }

//////////////////////////////////////////////////////////////////////////
// Write Java
//////////////////////////////////////////////////////////////////////////

  static void writeJava(File inFile, File outFile)
    throws Exception
  {
    String[] lines = readLines(inFile);
    PrintWriter out = new PrintWriter(new FileWriter(outFile));

    for (int i=0; i<lines.length; ++i)
    {
      String x = lines[i].trim();
      if (x.equals("$opcodes")) printJavaOps(out);
      else if (x.equals("$constants")) printJavaConstants(out);
      else  out.println(lines[i]);
    }

    out.close();
  }

  static void printJavaOps(PrintWriter out)
  {
    for (int i=0; i<ops.length; ++i)
    {
      Op op = ops[i];
      if (op.id < 0)
      {
        out.println();
        out.println("  " + op.comment);
      }
      else
      {
        out.println("  public static final int " + pad(op.name, 18) + " = " + pad(op.id+";", 6) +  op.comment);
      }
    }
    out.println();

    out.println("  // OpCodes by name");
    out.println("  public static final String[] names =");
    out.println("  {");
    for (int i=0; i<ops.length; ++i)
    {
      Op op = ops[i];
      if (op.id < 0) continue;
      out.println("    \"" + pad(op.name + "\",", 20) + " // " + op.id);
    }
    out.println("  };");
    out.println();

    out.println("  // OpCodes arguments");
    out.println("  public static int argType(int opcode)");
    out.println("  {");
    out.println("    switch(opcode)");
    out.println("    {");
    for (int i=0; i<ops.length; ++i)
    {
      Op op = ops[i];
      if (op.arg == null) continue;
      out.println("      case " + pad(op.name + ":", 20) + " return " + op.arg + "Arg;");
    }
    out.println("      default:                  return noArg;");
    out.println("    }");
    out.println("  }");
  }

  static void printJavaConstants(PrintWriter out)
  {
    for (int i=0; i<constants.length; ++i)
    {
      Constant c = constants[i];
      out.println("  public static final " + c.type + " " + pad(c.name, 14) +
        " = " + pad(c.value+";", 8) + " " + c.comment);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Write H
//////////////////////////////////////////////////////////////////////////

  static void writeH(File inFile, File outFile)
    throws Exception
  {
    String[] lines = readLines(inFile);
    PrintWriter out = new PrintWriter(new FileWriter(outFile));

    for (int i=0; i<lines.length; ++i)
    {
      String x = lines[i].trim();
      if (x.equals("$opcodes")) printHOps(out);
      else if (x.equals("$opcodeLabels")) printHOpLabels(out);
      else if (x.equals("$debug")) printHDebug(out);
      else if (x.equals("$constants")) printHConstants(out);
      else  out.println(lines[i]);
    }

    out.close();
  }

  static void printHOps(PrintWriter out)
  {
    for (int i=0; i<ops.length; ++i)
    {
      Op op = ops[i];
      if (op.id < 0)
      {
        out.println();
        out.println(op.comment);
      }
      else
      {
        out.println("#define " + pad(op.name, 18) + "  " + op.id);
      }
    }
    out.println();
    out.println("#define " + pad("NumOpcodes", 18) + "  " + nextOpcode);
  }

  static void printHOpLabels(PrintWriter out)
  {                      
    out.println("#define OpcodeLabelsArray \\");
    out.println("{ \\");
    int last = -1;
    for (int i=0; i<ops.length; ++i)
    {
      Op op = ops[i];
      if (op.id < 0) continue;
      if (op.id != last+1) throw new IllegalStateException("last=" + last + " id=" + op.id);
      out.println("&&" + op.name + ", \\");
      last = op.id;
    }
    out.print("}");
    out.println();
  }

  static void printHDebug(PrintWriter out)
  {
    out.println("#ifdef SCODE_DEBUG");
    out.println();
    out.println("// OpCodes by name");
    out.println("const char* OpcodeNames[] =");
    out.println("{");
    for (int i=0; i<ops.length; ++i)
    {
      Op op = ops[i];
      if (op.id < 0) continue;
      out.println("  \"" + pad(op.name + "\",", 20) + " // " + op.id);
    }
    out.println("};");
    out.println();
    out.println("// pointer offset used for null pointer check");
    out.println("const int8_t OpcodePointerOffsets[] =");
    out.println("{");
    for (int i=0; i<ops.length; ++i)
    {
      Op op = ops[i];
      if (op.id < 0) continue;
      out.println("  " + padLeft(op.offset + ",", 4) + "   // " + op.id + " " + op.name);
    }
    out.println("};");
    out.println();
    out.println("#endif");
  }

  static void printHConstants(PrintWriter out)
  {
    for (int i=0; i<constants.length; ++i)
    {
      Constant c = constants[i];
      out.println("#define " + pad(c.name, 14) + " " + c.value);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Read Data
//////////////////////////////////////////////////////////////////////////

  static void readData(File f)
    throws Exception
  {
    String[] lines = readLines(f);
    ArrayList opsAcc = new ArrayList();
    ArrayList constantsAcc = new ArrayList();

    String key = null;
    for (int i=0; i<lines.length; ++i)
    {
      String line = lines[i].trim();
      if (line.length() == 0) continue;
      if (line.startsWith("--")) continue;
      if (line.startsWith("$")) { key = line; continue; }
      if (key.equals("$opcodes"))
        opsAcc.add(parseOp(line));
      else if (key.equals("$constants"))
        constantsAcc.add(parseConstant(line));
      else
        throw new IllegalStateException("key = " + key);
    }

    ops = (Op[])opsAcc.toArray(new Op[opsAcc.size()]);
    constants = (Constant[])constantsAcc.toArray(new Constant[constantsAcc.size()]);
  }

  static Op parseOp(String line)
  {
    int slash = line.indexOf('/');

    // section comment
    if (slash == 0)
    {
      Op op = new Op();
      op.id      = -1;
      op.comment = line;
      return op;
    }

    String comment = line.substring(slash);      
    String[] toks = split(line.substring(0, slash));
    
    String name = toks[0];
    String arg = null;
    int offset = -1;
    if (toks.length == 2)
    { 
      try
      {
        offset = Integer.parseInt(toks[1]);
      }
      catch (Exception e)
      {                                 
        arg = toks[1];
      }      
    }
    else if (toks.length == 3)
    {
      arg = toks[1];
      offset = Integer.parseInt(toks[2]);
    }

    Op op = new Op();
    op.id      = nextOpcode++;
    op.name    = name;
    op.arg     = arg;
    op.offset  = offset;
    op.comment = comment;
    return op;
  }                     
  
  static String[] split(String s)
  {                                                      
    ArrayList acc = new ArrayList();
    StringTokenizer st = new StringTokenizer(s, " ");
    while (st.hasMoreTokens())
      acc.add(st.nextToken());
    return (String[])acc.toArray(new String[acc.size()]);
  }

  static Constant parseConstant(String line)
  {
    int slash = line.indexOf('/');
    String comment = line.substring(slash);
    String rest = line.substring(0, slash).trim();

    StringTokenizer st = new StringTokenizer(rest, " =");
    Constant c = new Constant();
    c.type  = st.nextToken().trim();
    c.name  = st.nextToken().trim();
    c.value = st.nextToken().trim();
    c.comment = comment;
    return c;
  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  public static String[] readLines(File f)
    throws Exception
  {
    BufferedReader in = new BufferedReader(new FileReader(f));
    ArrayList acc = new ArrayList();
    String line;
    while((line=in.readLine()) != null)
      acc.add(line);
    in.close();
    return (String[])acc.toArray(new String[acc.size()]);
  }

  public static String getSpaces(int num)
  {
    return SPACES[num];
  }

  private static String[] SPACES;
  static
  {
    SPACES = new String[50];
    SPACES[0] = "";
    for(int i=1; i<50; ++i)
      SPACES[i] = SPACES[i-1] + " ";
  }

  public static String pad(String s, int width)
  {
    if (s.length() >= width) return s;
    StringBuffer buf = new StringBuffer(width);
    buf.append(s).append(getSpaces(width-s.length()));
    return buf.toString();
  }

  public static String padLeft(String s, int width)
  {
    if (s.length() >= width) return s;
    StringBuffer buf = new StringBuffer(width);
    buf.append(getSpaces(width-s.length())).append(s);
    return buf.toString();
  }

//////////////////////////////////////////////////////////////////////////
// Inner Classes
//////////////////////////////////////////////////////////////////////////

  static class Op
  {
    int id;
    String name;
    String arg;     
    int offset;   // sp offset for null pointer checks
    String comment;
  }

  static class Constant
  {
    String type;
    String name;
    String value;
    String comment;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  static int nextOpcode = 0;
  static Op[] ops;
  static Constant[] constants;

}
