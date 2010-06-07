//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Mar 08  Brian Frank  Creation
//

package sedonac.util;

import java.io.*;
import java.util.*;
import sedona.*;
import sedona.util.*;
import sedonac.*;
import sedonac.scode.*;

/**
 * SCodeParser parses the schema information from a binary scode file.
 */
public class SCodeParser
{

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  public KitPart[] parse(File file)
    throws IOException
  {                             
    return parse(Buf.readFrom(file));
  }
  
  public KitPart[] parse(Buf buf)
    throws IOException
  { 
    // read magic to config endian
    this.buf = buf;
    buf.pos = 0;
    if (buf.i4() != SCode.vmMagic)
    {
      buf.pos = 0;                
      buf.bigEndian = this.bigEndian = false;
      if (buf.i4() != SCode.vmMagic)
        throw new IOException("Invalid magic for scode");
    }                   

    // header  
    majorVer   = buf.u1();       //  4 major version
    minorVer   = buf.u1();       //  5 minor version
    blockSize  = buf.u1();       //  6 sedona block size in bytes
    refSize    = buf.u1();       //  7 pointer size in bytes
    imageSize  = buf.i4();       //  8 spacer for image size in bytes
    dataSize   = buf.i4();       // 12 data size in bytes
    mainMethod = buf.u2();       // 16 spacer for main method block index
    buf.u2();                    // 18 test block index
    int biKits = buf.u2();       // 20 spacer for kits block index
    int num    = buf.u1();       // 22 num kits
    scodeFlags = buf.u1();       // 23 scode flags
    
    // verify version
    if (majorVer != SCode.vmMajorVer || minorVer != SCode.vmMinorVer)
      throw new IOException("Unsupported scode version " + majorVer + "." + minorVer);
        
    // traverse the pointers for each kit
    KitPart[] parts = new KitPart[num];
    for (int i=0; i<num; ++i) 
    {
      buf.pos = biKits*blockSize + i*2;
      parts[i] = parseKit(buf.u2());
    }
        
    return this.kits = parts;
  }                          
  
  private KitPart parseKit(int bi)
    throws IOException
  {                    
    buf.pos = bi * blockSize; 
    int id       = buf.u1();
    int typesLen = buf.u1();
    String name  = parseStr(buf.u2());
    String ver   = parseStr(buf.u2());
    int pad      = buf.u2();
    int checksum = buf.i4(); 
    return new KitPart(name, checksum, Version.parse(ver));
  }

  private String parseStr(int bi)
    throws IOException
  {                               
    int off = bi * blockSize;
    StringBuffer s = new StringBuffer();
    for (int i=0; buf.bytes[off+i] != 0; ++i)
      s.append((char)buf.bytes[off+i]);
    return s.toString(); 
  }                     
  
//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  public void dump()
  {
    PrintWriter out = new PrintWriter(System.out);
    dump(out);
    out.flush();
  }

  public void dump(PrintWriter out)
  {
    out.println("  majorVer:   " + majorVer);
    out.println("  minorVer :  " + minorVer);
    out.println("  endian:     " + (bigEndian ? "big" : "little"));
    out.println("  blockSize:  " + blockSize);
    out.println("  refSize:    " + refSize);
    out.println("  imageSize:  " + imageSize);
    out.println("  dataSize:   " + dataSize);
    out.println("  scodeFlags: 0x" + Integer.toHexString(scodeFlags));
    out.println("    debug:    " + ((scodeFlags & SCode.scodeDebug) != 0));
    out.println("    test:     " + ((scodeFlags & SCode.scodeTest) != 0));
    out.println("  kits:");
    for (int i=0; i<kits.length; ++i)
      out.println("    " + kits[i] + " " + kits[i].version);
  }

  public static void main(String[] args)
    throws Exception
  {
    if (args.length == 0) { System.out.println("usage: SCodeParser <file>"); return; }
    SCodeParser p = new SCodeParser();         
    p.parse(new File(args[0]));
    p.dump();
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  Buf buf;  
  public boolean bigEndian = true;    
  public int majorVer;
  public int minorVer;
  public int blockSize;
  public int refSize; 
  public int imageSize;
  public int dataSize; 
  public int mainMethod;
  public int scodeFlags;  // see SCode.scodeXXX (test, debug)
  public KitPart[] kits;
}
