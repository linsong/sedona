//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   26 Mar 08  Elizabeth McKenney  Creation
//

package sedonac.util;

import java.io.*;
import java.util.*;
import sedona.*;
import sedona.util.*;
import sedonac.*;
import sedonac.scode.*;

/**
 * SCodeValidator disassembles a binary scode file.
 * Loosely based on SCodeParser.
 */
public class SCodeValidator
{

//////////////////////////////////////////////////////////////////////////
// parse   (2 versions)
//////////////////////////////////////////////////////////////////////////

  public void parse(File file)
    throws IOException
  {                             
    parse(Buf.readFrom(file));
  }
  

  public void parse(Buf buf)
    throws IOException
  { 
    this.buf = buf;

    // Parse the header & set up public fields
    System.out.println("\nParsing header fields....\n");
    hdr.parse(buf);
    hdr.print();

    System.out.println("\nParsing bootstrap....\n");
    parseBootstrap(buf);

//    System.out.println("\nParsing vtables....\n");
//    parseVtables(buf);

    System.out.println("\nParsing kit list....\n");
    parseKits();

    // DIAG
    System.out.println();
    System.out.println("endTypeBix = 0x" + tHS(endTypeBix));
    System.out.println("endSlotBix = 0x" + tHS(endSlotBix));

//    System.out.println("\nParsing type list....\n");
//    parseTypes();

//    System.out.println("\nParsing slot list....\n");
//    parseSlots();

    System.out.println("\nParsing log list....\n");
    parseLogs(endSlotBix);

    // DIAG
    System.out.println();
    System.out.println("endLogBix = 0x" + tHS(endLogBix));

    System.out.println("\nParsing method list....\n");
    parseMethods(endLogBix);

    if (hdr.testTabBix>0)
    {
      System.out.println("\nParsing test table....\n");
      parseTests();
    }
    else
      System.out.println("\nNo test table to parse.\n");

    return;
  }

//////////////////////////////////////////////////////////////////////////
// parseBootstrap 
//////////////////////////////////////////////////////////////////////////


  private void parseBootstrap(Buf buf)
    throws IOException
  {
    buf.pos = hdr.bootBix * hdr.blockSize;
    int numParams = buf.u1();
    int numLocals = buf.u1();

    System.out.println("Bootstrap: numParams = " + numParams + ", numLocals = " + numLocals);
    
    int callBix;
    int nextByte = buf.u1();

    // If debug is true, next bytes are MetaSlot and qname
    if (hdr.debugFlag)
    {
      while (nextByte==0) nextByte = buf.u1();    // skip pad byte(s) if any

      if (nextByte!=SCode.MetaSlot)
        throw new IOException("Found " + nextByte + " not MetaSlot at offset " + tHS(buf.pos));

      callBix = u2aligned(buf);
      System.out.println("Main bix = 0x" + tHS(callBix));

      nextByte = buf.u1();
    }

    while (nextByte==0) nextByte = buf.u1();    // skip pad byte(s) if any

    // If there are static inits they come next, as list of 
    // 1-byte Call opcode followed by 2-byte bix
    while (nextByte==SCode.Call)
    {
      callBix = u2aligned(buf);
      System.out.println("Static init call to bix = 0x" + tHS(callBix));
      nextByte = buf.u1();
      while (nextByte==0) nextByte = buf.u1();    // skip pad byte(s) if any
    }

    // Past loop, next opcode is LoadParam0
    if (nextByte!=SCode.LoadParam0)
      throw new IOException("Found " + nextByte + " not LoadParam0 at offset " + tHS(buf.pos));

    nextByte = buf.u1();
    if (nextByte!=SCode.LoadParam1)
      throw new IOException("Found " + nextByte + " not LoadParam1 at offset " + tHS(buf.pos));

    nextByte = buf.u1();
    if (nextByte==0) nextByte = buf.u1();    // skip pad byte if any

    if (nextByte!=SCode.Call)
      throw new IOException("Found " + nextByte + " not Call at offset " + tHS(buf.pos));

    callBix = u2aligned(buf);
    nextByte = buf.u1();
    if (nextByte!=SCode.ReturnPop)
      throw new IOException("Found " + nextByte + " not ReturnPop at offset " + tHS(buf.pos));

    System.out.println(" Main bix is " + tHS(callBix));
    System.out.println("Bootstrap section ended at scode offset " + tHS(buf.pos));
  }





//////////////////////////////////////////////////////////////////////////
// parseMethodStandalone 
//////////////////////////////////////////////////////////////////////////


  private void parseMethodStandalone(File infile, int codeBix)
    throws IOException
  {     
    this.buf = Buf.readFrom(infile);

    // Parse the header & set up public fields
    System.out.println("\nParsing header fields....");
    hdr.parse(buf);

    // Parse the specified method
    parseMethod(codeBix, true);
  }




//////////////////////////////////////////////////////////////////////////
// parseMethods 
//////////////////////////////////////////////////////////////////////////

  private void parseMethods(int methodTabBix)
    throws IOException
  {     
    // If methodTabBix<0 then buf.pos already at correct position
    if (methodTabBix>0) buf.pos = methodTabBix * hdr.blockSize;

    // Not sure how to tell we're done???
    while (buf.pos < hdr.imageSize)
    {
      parseMethod(-1, false);
    }
  }



//////////////////////////////////////////////////////////////////////////
// parseMethod 
//////////////////////////////////////////////////////////////////////////


  private void parseMethod(int codeBix, boolean bRestorePos)
    throws IOException
  {     
    int       opCode;
    int       opPos;
    String    argStr;
    ArrayList calledMethods = new ArrayList();

    int savepos = buf.pos;

    // If codeBix<0 then buf.pos already at correct position
    if (codeBix>0) buf.pos = codeBix * hdr.blockSize;
    else codeBix = buf.pos / hdr.blockSize;

    // This represents the maximum dest addr jumped to within the method
    // (so we know which Return opcode is really the end)
    int maxjmp = buf.pos; 

    // Print num params & locals like opcodes
    System.out.println("\nParsing method at bix = 0x" + tHS(codeBix));

    opPos  = buf.pos;
    opCode = buf.u1();
    System.out.println( opFormat(opPos, opCode, "Num params") + opCode );

    opPos  = buf.pos;
    opCode = buf.u1();
    System.out.println( opFormat(opPos, opCode, "Num locals") + opCode );


    // Stop at first Return type opcode, unless method includes a jump past that point
    boolean bDone = false;


    //
    // Step through the opcodes and args until end of method
    //
    while (!bDone) 
    {
      // Get next opcode
      opPos  = buf.pos;
      opCode = buf.u1();

      // Print opcode & name
      System.out.print( opFormat(opPos, opCode, SCode.name(opCode)) );

      argStr = getArgString(opCode);

      // Print arg if any
      if (argStr!=null) System.out.println(argStr);
      else System.out.println();


      // Quit when we reach the first return, unless there's a jump that
      // takes us past it (not sure this is sufficient test?)
      switch (opCode)
      {
        case SCode.Call:   calledMethods.add(argStr);
                           break;

        case SCode.Jump:
        case SCode.JumpNonZero:
        case SCode.JumpZero:      
        case SCode.JumpFar:
        case SCode.JumpFarNonZero:
        case SCode.JumpFarZero:   
        case SCode.JumpIntEq:
        case SCode.JumpIntNotEq:
        case SCode.JumpIntGt:
        case SCode.JumpIntGtEq:
        case SCode.JumpIntLt:
        case SCode.JumpIntLtEq:
        case SCode.JumpFarIntEq:
        case SCode.JumpFarIntNotEq:
        case SCode.JumpFarIntGt:
        case SCode.JumpFarIntGtEq:
        case SCode.JumpFarIntLt:
        case SCode.JumpFarIntLtEq:  // update maxjmp as needed
                           if (opPos+intval(argStr) > maxjmp)
                             maxjmp = opPos+intval(argStr);
                           break;

        case SCode.ReturnVoid:
        case SCode.ReturnPop:
        case SCode.ReturnPopWide:  if (buf.pos > maxjmp) bDone = true;  
                                   break;

        default:
      }

    }  // while (!bDone)
    

    //
    // JUST FOR TESTING - easy way to try parsing many different methods
    //

    // Now parse any methods called by this one...  (I may regret this)
//    if (!calledMethods.isEmpty())
//      for (int k=0; k<1; k++)  // calledMethods.size(); k++)
//      {
//        String hexstr = (String)calledMethods.get(k);
//        int methodBix = hexval( hexstr );
//        parseMethod(methodBix); 
//      }
    
    if (bRestorePos) buf.pos = savepos;
  }


//////////////////////////////////////////////////////////////////////////
// getArgString 
//////////////////////////////////////////////////////////////////////////

  private String getArgString(int op)
    throws IOException
  {     
    // Find width of opcode's arg and read it in.
    // Return string showing arg value (incl 0x prefix for hex),
    // or null if no arg.

    String argStr = null;

    // For now, keep it simple - just get hex value of arg bytes
    switch (SCode.argType(op))
    {
      // signed 1-byte arg
      case SCode.jmpArg    : argStr = String.valueOf(buf.s1());
                             break;
      // unsigned 1-byte arg
      case SCode.u1Arg     : argStr = "0x" + tHS(buf.u1());
                             break;
      // signed 2-byte arg 
      case SCode.jmpfarArg : argStr = String.valueOf(buf.s2());
                             break;
      // unsigned 2-byte arg 
      case SCode.strArg    :
      case SCode.bufArg    :
      case SCode.typeArg   :
      case SCode.slotArg   :
      case SCode.u2Arg     : argStr = "0x" + tHS(u2aligned(buf));
                             break;
      // unsigned 2-byte arg, not necessarily aligned
      case SCode.methodArg : argStr = "0x" + tHS(buf.u2());
                             break;
      // signed 4-byte arg
      case SCode.intArg    : argStr = String.valueOf(i4aligned(buf));
                             break;
      case SCode.floatArg  : argStr = String.valueOf(f4aligned(buf));
                             break;
      // unsigned 4-byte arg
      case SCode.s4Arg     : argStr = "0x" + tHS(i4aligned(buf));
                             break;

      // unsigned 8-byte arg
      case SCode.longArg   : argStr = "0x" + java.lang.Long.toHexString(i8aligned(buf));
                             break;
      // signed 8-byte arg
      case SCode.doubleArg : argStr = String.valueOf(f8aligned(buf));
                             break;

      // switch has multiple args, first is #args to follow
      case SCode.switchArg : int nargs = u2aligned(buf);
                             argStr = "(" + nargs + ") ";
                             for (int k=0; k<nargs; k++)
                             {
                               if (k>0) argStr += ", ";
                               argStr += "0x" + tHS(u2aligned(buf));
                             }
                             break;

      // field arg (varies with opcode, usually unsigned)
      case SCode.fieldArg  : if (SCode.name(op).endsWith("1"))
                               argStr = "0x" + tHS(buf.u1());
                             else if (SCode.name(op).endsWith("2"))
                               argStr = "0x" + tHS(u2aligned(buf));
                             else if (SCode.name(op).endsWith("4"))
                               argStr = "0x" + tHS(i4aligned(buf));
                             else 
                               argStr = "0x" + tHS(u2aligned(buf));  // width of "ConstStatic" arg?
                             break;

      // no arg
      case SCode.noArg     : 
      default              : return null;
    }

    return argStr;
  }



//////////////////////////////////////////////////////////////////////////
// parseLogs 
//////////////////////////////////////////////////////////////////////////

  private void parseLogs(int logTabBix)
    throws IOException
  {     
    int prev_id = -2;
    int id = -1;

    int savepos = buf.pos;

    // If logTabBix<0 then buf.pos already at correct position
    if (logTabBix>0) buf.pos = logTabBix * hdr.blockSize;

    // Stupid heuristic to find end of log table: when ids stop being consecutive
    // (assumes we start at 0)
    while (id == prev_id+1)
    {
      prev_id = id;

      id           = u2aligned(buf);
      int nameBix  = u2aligned(buf);
      String name  = parseStr(nameBix);

      if (id == prev_id+1)   // if consecutive, assume it's a valid log entry
      {
        System.out.print("\nLog " + id);
        System.out.println("\tName = " + name + "  NameBix = 0x" + tHS(nameBix));
      }
    } 

    endLogBix = (buf.pos + (hdr.blockSize-1)) / hdr.blockSize;

    buf.pos = savepos;
  }





//////////////////////////////////////////////////////////////////////////
// parseTests 
//////////////////////////////////////////////////////////////////////////

  private void parseTests()
    throws IOException
  {     
    buf.pos = hdr.testTabBix * hdr.blockSize;
    int numTests = u2aligned(buf);

    System.out.println("  # tests = " + numTests);

    // traverse the pointers for each test in table
    for (int i=0; i<numTests; ++i) 
    {
      int slotBix = u2aligned(buf);
      int codeBix = u2aligned(buf);
      System.out.print("\nTest " + i + ": qname bix = 0x" + tHS(slotBix));
      System.out.println(", code bix = 0x" + tHS(codeBix));
      System.out.println("  Name = " + parseQnameSlot(slotBix));
      System.out.println();

      parseMethod(codeBix, true);
    }
  }



//////////////////////////////////////////////////////////////////////////
// parseTest 
//////////////////////////////////////////////////////////////////////////

  private void parseTest(int bi)
    throws IOException
  {                    
    buf.pos = bi * hdr.blockSize; 
    int id       = buf.u1();
    int typesLen = buf.u1();
    int nameBix  = u2aligned(buf);
    int verBix   = u2aligned(buf);
    String name  = parseStr(nameBix);
    String ver   = parseStr(verBix);
    int pad      = u2aligned(buf);
    int csum     = i4aligned(buf); 

    return;
  }





//////////////////////////////////////////////////////////////////////////
// parseKits 
//////////////////////////////////////////////////////////////////////////


  private void parseKits()
    throws IOException
  {     
    // traverse the pointers for each kit
    for (int i=0; i<hdr.numKits; ++i) 
    {
      buf.pos = hdr.kitTabBix * hdr.blockSize + i*2;
      int kitBix = u2aligned(buf);
      parseKit(kitBix);
    }
  }


//////////////////////////////////////////////////////////////////////////
// parseKit 
//////////////////////////////////////////////////////////////////////////

  private KitPart parseKit(int bi)
    throws IOException
  {                    
    int savepos = buf.pos;
    buf.pos = bi * hdr.blockSize; 

    int id       = buf.u1();
    int typesLen = buf.u1();
    int nameBix  = u2aligned(buf);
    int verBix   = u2aligned(buf);
    String name  = parseStr(nameBix);

    if (name.equals("sys")) this.sysKitBix = bi;

    String ver   = parseStr(verBix);
    int pad      = u2aligned(buf);
    int csum     = i4aligned(buf); 

    System.out.println("\nKit " + id + " at bix 0x" + tHS(bi));
    System.out.println("  Name    = " + name + "-" + tHS(csum) + "  ver " + ver);
    System.out.println("  NameBix = 0x" + tHS(nameBix) + ", verBix = 0x" + tHS(verBix));
    System.out.println("  # types = " + typesLen);

    for (int j=0; j<typesLen; j++)
    {
      int tbix = u2aligned(buf);
      parseType(tbix);
    }

    buf.pos = savepos;

    return new KitPart(name, csum, Version.parse(ver));
  }




//////////////////////////////////////////////////////////////////////////
// parseType 
//////////////////////////////////////////////////////////////////////////


  private void parseType(int typeBix)
    throws IOException
  {     
    int savepos = buf.pos;

    // If typeBix<0 then buf.pos already at correct position
    if (typeBix>0) buf.pos = typeBix * hdr.blockSize; 
    else typeBix = buf.pos / hdr.blockSize;

    int id       = buf.u1();
    int slotsLen = buf.u1();
    int nameBix  = u2aligned(buf);
    int kitBix   = u2aligned(buf);
    String name  = parseStr(nameBix);

    System.out.println("\n\tType " + id + " at bix 0x" + tHS(typeBix));
    System.out.println("  \tName    = " + name + "  NameBix = 0x" + tHS(nameBix));
    System.out.println("  \tkitBix  = " + tHS(kitBix));

    // Stupid heuristic for identifying primitive types: in sys kit & 1st char is lower case 
    // NOTE: for this to work, sys kit bix must be known (ok if called from parseKit)
    if ((kitBix==this.sysKitBix) && name.substring(0,1).equals(name.substring(0,1).toLowerCase()))
    {
      // If primitive, stop here & return
      buf.pos = savepos;
      return;
    }

    int baseBix  = u2aligned(buf);      // base class typebix, 0 if not comp
    int size     = u2aligned(buf);
    int initBix  = u2aligned(buf);      // bix for init code

    System.out.println("  \tbaseBix = " + tHS(baseBix));
    System.out.println("  \tinitBix = " + tHS(initBix));
    System.out.println("  \tSize    = " + size + "  # slots = " + slotsLen);

    for (int j=0; j<slotsLen; j++)
    {
      int slotBix  = u2aligned(buf);      
      parseSlot(slotBix);
    }

    // Update pointer to end of section
    if (typeBix > endTypeBix)
      endTypeBix = (buf.pos + (hdr.blockSize-1)) / hdr.blockSize;   // round up to next bix

    buf.pos = savepos;
  }





//////////////////////////////////////////////////////////////////////////
// parseSlot 
//////////////////////////////////////////////////////////////////////////


  private void parseSlot(int slotBix)
    throws IOException
  {     
    int savepos = buf.pos;
    buf.pos = slotBix * hdr.blockSize; 

    int id       = buf.u1();
    int rtFlags  = buf.u1();
    int nameBix  = u2aligned(buf);
    String name  = parseStr(nameBix);
    int fpBix    = u2aligned(buf);    // bix for field type OR method param
    int codeBix  = u2aligned(buf);    // bix for code ONLY if method!!

    System.out.println("\n\t\tSlot " + id + " at bix 0x" + tHS(slotBix));
    System.out.println("  \t\tName    = " + name + "  NameBix = 0x" + tHS(nameBix));
    System.out.println("  \t\tfpBix   = " + tHS(fpBix) + "  rtFlags = " + rtFlags);
    System.out.println("  \t\tcodeBix = " + tHS(codeBix) + " ONLY if method!");

    // Update pointer to end of section
    if (slotBix > endSlotBix)
      endSlotBix = (buf.pos + (hdr.blockSize-1)) / hdr.blockSize;   // round up to next bix

    buf.pos = savepos;
  }






//////////////////////////////////////////////////////////////////////////
// Utility functions
//////////////////////////////////////////////////////////////////////////

  //
  // parseQnameSlot 
  //
  private String parseQnameSlot(int bi)
    throws IOException
  {                               
    int savepos = buf.pos;

    buf.pos = bi * hdr.blockSize;
    int qnameBix  = u2aligned(buf);
    int methodBix = u2aligned(buf);

    buf.pos = qnameBix * hdr.blockSize;
    int typeNameBix = u2aligned(buf);
    int slotNameBix = u2aligned(buf);
    String typeName = parseStr(typeNameBix);
    String slotName = parseStr(slotNameBix);

    String methodName = parseStr(methodBix);

    buf.pos = savepos;

    return (typeName + ":" + slotName + "." + methodName);
  }                     
  

  //
  // parseStr
  //
  private String parseStr(int bi)
    throws IOException
  {                               
    int off = bi * hdr.blockSize;
    StringBuffer s = new StringBuffer();
    for (int i=0; buf.bytes[off+i] != 0; ++i)
      s.append((char)buf.bytes[off+i]);
    return s.toString(); 
  }                     
  

  //
  // Get values from buf, checking alignment
  //
  private int u2aligned(Buf b)
    throws IOException
  {
    if (b.pos%2 != 0)
      throw new IOException("Alignment exception (2) at scode offset 0x" + tHS(b.pos));
    return b.u2();
  }


  private int i4aligned(Buf b)
    throws IOException
  {
    if (b.pos%4 != 0)
      throw new IOException("Alignment exception (4) at scode offset 0x" + tHS(b.pos));
    return b.i4();
  }


  private long i8aligned(Buf b)
    throws IOException
  {
    if (b.pos%8 != 0)
      throw new IOException("Alignment exception (8) at scode offset 0x" + tHS(b.pos));
    return b.i8();
  }


  private float f4aligned(Buf b)
    throws IOException
  {
    if (b.pos%4 != 0)
      throw new IOException("Alignment exception (4) at scode offset 0x" + tHS(b.pos));
    return b.f4();
  }


  private double f8aligned(Buf b)
    throws IOException
  {
    if (b.pos%8 != 0)
      throw new IOException("Alignment exception (8) at scode offset 0x" + tHS(b.pos));
    return b.f8();
  }


  // Print hex value (no 0x prefix)
  static String tHS(int val)  
  { 
    return(Integer.toHexString(val));
  }


  // Return integer value of string (handles exceptions here)
  static int intval(String intstr)  
  { 
    try { return Integer.valueOf(intstr).intValue(); }
    catch (NumberFormatException e)
    {
      System.out.println("Error: Could not extract integer from string " + intstr);
    }
    return -1;
  }


  // Return integer value of hex string (handles exceptions here)
  static int hexval(String hexstr)  
  { 
    try { return Integer.valueOf(hexstr.substring(2), 16).intValue(); }
    catch (NumberFormatException e)
    {
      System.out.println("Error: Could not extract integer from string " + hexstr);
    }
    return -1;
  }


  // Format opcode & description nicely
  static String opFormat(int pos, int op, String descrip)  
  { 
    String spacepool   = "                                                  ";  
    int    maxNameWidth = 25;    // max opcode name width, plus arg spacing, adj as desired
    String prefix;

    String padding = spacepool.substring(0, maxNameWidth-descrip.length());

    // Add leading 0 to opcode if needed
    if (op<16) prefix = "   0x0";
    else prefix = "   0x";

    // Return formatted string
    return tHS(pos) + "  " + prefix + tHS(op) + "   : " + descrip + padding;
  }


//////////////////////////////////////////////////////////////////////////
// Main
//////////////////////////////////////////////////////////////////////////

  public static void main(String[] args)
    throws Exception
  {
    if (args.length < 1) { System.out.println("usage: SCodeValidator <file> [bix]"); return; }
    if (args.length > 1) 
    { 
      System.out.println("Decoding method in file " + args[0] + " at bix " + args[1]); 
      int codeBix = Integer.parseInt(args[1], 16);
      new SCodeValidator().parseMethodStandalone(new File(args[0]), codeBix);
      return;
    }

    System.out.println("\n\nDecoding " + args[0] + ":\n");

    // Do the parse
    new SCodeValidator().parse(new File(args[0]));
  }




////////////////////////////////////////////////////////////////
// class SHeader
////////////////////////////////////////////////////////////////

  class SHeader
  {
    public boolean bigEndian = true;    
    public int vmMagic;

    public int majorVer;
    public int minorVer;
    public int blockSize;
    public int refSize;
  
    public int imageSize;
    public int dataSize;
    public int bootBix;
    public int testTabBix;
  
    public int kitTabBix;
    public int numKits;

    public int scodeFlags;
    public boolean debugFlag;
    public boolean testFlag;


    public void parse(Buf buf)
      throws IOException
    {                    
      // read magic to config endian
      buf.pos = 0;
      vmMagic = i4aligned(buf);
  
      // if magic doesn't match, change endianness & try again
      if (vmMagic != SCode.vmMagic)
      {
        buf.bigEndian = bigEndian = false;
        buf.pos = 0;                
        vmMagic = i4aligned(buf);
  
        // if magic doesn't match now, give up
        if (vmMagic != SCode.vmMagic)
          throw new IOException("Invalid magic for scode");
      }                   
    
      // Read in fields
      majorVer  = buf.u1();
      minorVer  = buf.u1();
      blockSize = buf.u1();
      refSize   = buf.u1();
      
      // Note: this prevents tool from analyzing older scode files
      if (majorVer != SCode.vmMajorVer || minorVer != SCode.vmMinorVer)
        throw new IOException("Unsupported scode version " + majorVer + "." + minorVer);
      
      imageSize  = i4aligned(buf);
      dataSize   = i4aligned(buf);
      bootBix    = u2aligned(buf);
      testTabBix = u2aligned(buf);
  
      // block index to kits is at offset 20, num at 22
      kitTabBix  = u2aligned(buf);
      numKits = buf.u1();      
  
      // scode flags
      scodeFlags  = buf.u1();
      debugFlag = (scodeFlags & SCode.scodeDebug)!=0;
      testFlag  = (scodeFlags & SCode.scodeTest)!=0;
    }

    public void print()
    {                    
      System.out.println("  Platform is " + (bigEndian?"big":"little") + " endian.");
      System.out.println("  vmMagic      = 0x" + tHS(vmMagic));
      System.out.println("  majorVer     = " + majorVer);
      System.out.println("  minorVer     = " + minorVer);
      System.out.println("  blockSize    = " + blockSize);
      System.out.println("  refSize      = " + refSize);
      System.out.println("  imageSize    = 0x" + tHS(imageSize) + "\t(" + imageSize + ")");
      System.out.println("  dataSize     = 0x" + tHS(dataSize) + "\t(" + dataSize + ")");
      System.out.println("  boot bix     = 0x" + tHS(bootBix));
      System.out.println("  test tab bix = 0x" + tHS(testTabBix));
      System.out.println("  kit tab bix  = 0x" + tHS(kitTabBix));
      System.out.println("  num kits     = " + numKits);

      System.out.print("  scode flags  = 0x" + tHS(scodeFlags));
      System.out.print("  (test=" + (testFlag ? "true" : "false"));
      System.out.print(", debug=" + (debugFlag ? "true" : "false"));
      System.out.println(")");

    }

  }



////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  static SCodeValidator decode;

  Buf buf;  
  SHeader hdr = new SHeader();
  Schema schema;

  int sysKitBix = -1;   // bix for sys kit

  // Use these to identify ends of type & slot segments
  int endTypeBix = -1;
  int endSlotBix = -1;
  int endLogBix  = -1;
}

