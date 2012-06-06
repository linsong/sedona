//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Dec 07  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import sedona.*;
import sedona.kit.*;
import sedona.offline.*;
import sedona.util.*;
import sedona.xml.*;

/**
 * KitDbTest
 */
public class KitDbTest
  extends Test
{

////////////////////////////////////////////////////////////////
// Depend Parse
////////////////////////////////////////////////////////////////

  public void testDependParse()
    throws Exception
  {
    Depend d;
    
    d = Depend.parse("foo 1.2.3");
    verifyEq(d.size(), 1);
    verifyDepend(d, 0, new Version("1.2.3"), false, false, null, -1);

    d = Depend.parse("foo 1.2.3+");
    verifyEq(d.size(), 1);
    verifyDepend(d, 0, new Version("1.2.3"), true, false, null, -1);
    
    d = Depend.parse("foo 1.2.3=");
    verifyEq(d.size(), 1);
    verifyDepend(d, 0, new Version("1.2.3"), false, true, null, -1);

    d = Depend.parse("foo 1.2.3-1.2.4");
    verifyEq(d.size(), 1);
    verifyDepend(d, 0, new Version("1.2.3"), false, false, new Version("1.2.4"), -1);
    
    d = Depend.makeChecksum("foo", 0xffeeddcc);
    verifyEq(d.size(), 1);
    verifyDepend(d, 0, null, false, false, null, 0xffeeddcc);
    
    d = Depend.parse("foo 10.20, 10.30");
    verifyEq(d.size(), 2);
    verifyDepend(d, 0, new Version("10.20"), false, false, null, -1);
    verifyDepend(d, 1, new Version("10.30"), false, false, null, -1);
    
    d = Depend.parse("foo 10+, 0x12345678, 20-30");
    verifyEq(d.size(), 3);
    verifyDepend(d, 0, new Version("10"), true, false, null, -1);
    verifyDepend(d, 1, null, false, false, null, 0x12345678);
    verifyDepend(d, 2, new Version("20"), false, false, new Version("30"), -1);
  }         
  
  void verifyDepend(Depend d, int index, Version ver, boolean plus, boolean exact, Version end, int checksum)
  {
    verifyEq(d.name(), "foo");
    verifyEq(d.version(index), ver);
    verifyEq(d.isPlus(index), plus);
    verifyEq(d.isExact(index), exact);
    verifyEq(d.endVersion(index), end);
    verifyEq(d.checksum(index), checksum);
    
    String s = d.toString();
    Depend x = Depend.parse(s);
    verifyEq(x.name(), "foo");
    verifyEq(x.version(index), ver);
    verifyEq(x.isPlus(index), plus);
    verifyEq(x.isExact(index), exact);
    verifyEq(x.endVersion(index), end);
    verifyEq(x.checksum(index), checksum);
  }                   

////////////////////////////////////////////////////////////////
// Depend Match
////////////////////////////////////////////////////////////////

  public void testDependMatch()
    throws Exception
  {
    Depend d;

    // simple    
    d = Depend.parse("foo 1.2");
    verifyEq(d.match(new Version("1")), false);                          
    verifyEq(d.match(new Version("1.2")), true);                          
    verifyEq(d.match(new Version("1.2.0")), true);                          
    verifyEq(d.match(new Version("1.2.20")), true);                          
    verifyEq(d.match(new Version("1.3")), false);                          
    verifyEq(d.match(new Version("2")), false);                          
    verifyEq(d.match(new Version("10.3")), false);                          
    verifyEq(d.match(0xaabbccdd), false);                          
    verifyEq(d.match(new Version("1.2"), 0xaabbccdd), true);                          
    verifyEq(d.match(new Version("1.3"), 0xaabbccdd), false);                          

    // plus    
    d = Depend.parse("foo 1.2+");
    verifyEq(d.match(new Version("1")), false);                          
    verifyEq(d.match(new Version("1.1")), false);                          
    verifyEq(d.match(new Version("1.1.3")), false);                          
    verifyEq(d.match(new Version("1.2")), true);                          
    verifyEq(d.match(new Version("1.2.0")), true);                          
    verifyEq(d.match(new Version("1.2.20.3")), true);                          
    verifyEq(d.match(new Version("1.3")), true);                          
    verifyEq(d.match(new Version("2")), true);                          
    verifyEq(d.match(new Version("10.3")), true);                          
    verifyEq(d.match(0xaabbccdd), false);                          
    verifyEq(d.match(new Version("1.2.3"), 0xaabbccdd), true);                          
    verifyEq(d.match(new Version("1.0"), 0xaabbccdd), false);      
    
    // exact
    d = Depend.parse("foo 1.2=");
    verifyEq(d.match(new Version("1")), false);
    verifyEq(d.match(new Version("1.2")), true);
    verifyEq(d.match(new Version("1.2.1")), false);
    verifyEq(d.match(new Version("1.2"), 0xaabbccdd), true);
    verifyEq(d.match(0xaabbccdd), false);
    verifyEq(d.match(new Version("1"), 0xaabbccdd), false);
    d = Depend.parse("foo 1.2=,0xaabbccdd");
    verifyEq(d.match(new Version("1.2"), 0xaabbccdd), true);
    verifyEq(d.match(new Version("1.2"), 0xffbbccdd), false);
    verifyEq(d.match(new Version("1"), 0xaabbccdd), false);

    // range    
    d = Depend.parse("foo 3.2-3.4");
    verifyEq(d.match(new Version("1.1")), false);                          
    verifyEq(d.match(new Version("3.1")), false);                          
    verifyEq(d.match(new Version("3.2")), true);                          
    verifyEq(d.match(new Version("3.2.10")), true);                          
    verifyEq(d.match(new Version("3.3.99")), true);                          
    verifyEq(d.match(new Version("3.4")), true);                          
    verifyEq(d.match(new Version("3.4.456")), true);                          
    verifyEq(d.match(new Version("3.5")), false);                              
    verifyEq(d.match(0xaabbccdd), false);                          
    verifyEq(d.match(new Version("3.2.11"), 0xaabbccdd), true);                          
    verifyEq(d.match(new Version("1.0"), 0xaabbccdd), false);                          

    // checksum    
    d = Depend.parse("foo 0xaabbccdd");
    verifyEq(d.match(new Version("1.1")), false);                          
    verifyEq(d.match(new Version("3.1")), false);                          
    verifyEq(d.match(0xaabbccdd), true);                          
    verifyEq(d.match(0xaabbccdf), false);                          
    verifyEq(d.match(new Version("1.1"), 0xaabbccdd), true);                          
    verifyEq(d.match(new Version("1.1"), 0xaabbccdf), false);                          

    // version checksum    
    d = Depend.parse("foo 1.2, 0xaabbccdd");
    verifyEq(d.match(new Version("1.1")), false);                          
    verifyEq(d.match(new Version("1.2")), true);                          
    verifyEq(d.match(0xaabbccdd), true);                          
    verifyEq(d.match(new Version("1.1"), 0xaabbccdd), false);                          
    verifyEq(d.match(new Version("1.2"), 0xaabbccdf), false);                          
    verifyEq(d.match(new Version("1.2"), 0xaabbccdd), true);                          
    
    // compound                                                                      
    d = Depend.parse("foo 3.2, 3.4+, 0xaabbccdd");
    verifyEq(d.match(new Version("1.1")), false);                          
    verifyEq(d.match(new Version("3.1")), false);                          
    verifyEq(d.match(new Version("3.2")), true);                          
    verifyEq(d.match(new Version("3.2.10")), true);                          
    verifyEq(d.match(new Version("3.3.99")), false);                          
    verifyEq(d.match(new Version("3.4")), true);                          
    verifyEq(d.match(new Version("3.4.456")), true);                          
    verifyEq(d.match(new Version("3.5")), true);                              
    verifyEq(d.match(0xaabbccdd), true);                          
    verifyEq(d.match(0xaabbccdf), false);                          
    verifyEq(d.match(new Version("1.0"),   0xaabbccdf), false);                          
    verifyEq(d.match(new Version("1.0"),   0xaabbccdd), false);                          
    verifyEq(d.match(new Version("3.2.1"), 0xaabbccdf), false);                          
    verifyEq(d.match(new Version("3.2.1"), 0xaabbccdd), true);                          
    verifyEq(d.match(new Version("3.4"),   0xaabbccdd), true);                          
  }

////////////////////////////////////////////////////////////////
// KitDb
////////////////////////////////////////////////////////////////

  public void testKitDb()
    throws Exception
  {         
    File dir = new File(KitDb.dir, "testKitDb");
    dir.mkdirs();
    try
    {          
      // build some dummy kit files
      makeKitFile(dir, 0x12345678, "1.0.3400");  
      makeKitFile(dir, 0x12345678, "1.0.3400.1");
      makeKitFile(dir, 0x12345678, "1.0.3401");  
      makeKitFile(dir, 0x12345678, "1.0.3402");  
      makeKitFile(dir, 0xaaaaaaaa, "1.0.3403");  
      makeKitFile(dir, 0xaaaaaaaa, "1.1.3500");  
      makeKitFile(dir, 0xbbbbbbbb, "1.1.3501");  
      makeKitFile(dir, 0xcccccccc, "1.2.3600");  
      
      // list
      KitFile[] kits;
      kits = KitDb.list("testKitDb");
      verifyEq(kits.length, 8);
      verifyKitFile(kits[0], 0x12345678, "1.0.3400.1");
      verifyKitFile(kits[1], 0x12345678, "1.0.3400");
      verifyKitFile(kits[2], 0x12345678, "1.0.3401");  
      verifyKitFile(kits[3], 0x12345678, "1.0.3402");  
      verifyKitFile(kits[4], 0xaaaaaaaa, "1.0.3403");  
      verifyKitFile(kits[5], 0xaaaaaaaa, "1.1.3500");  
      verifyKitFile(kits[6], 0xbbbbbbbb, "1.1.3501");  
      verifyKitFile(kits[7], 0xcccccccc, "1.2.3600");  
      
      // matchAll: 1.0
      kits = KitDb.matchAll(Depend.parse("testKitDb 1.0"));
      verifyEq(kits.length, 5);
      verifyKitFile(kits[0], 0x012345678, "1.0.3400.1");
      verifyKitFile(kits[1], 0x012345678, "1.0.3400");  
      verifyKitFile(kits[2], 0x012345678, "1.0.3401");  
      verifyKitFile(kits[3], 0x012345678, "1.0.3402");  
      verifyKitFile(kits[4], 0x0aaaaaaaa, "1.0.3403");              
      
      // matchAll: 1.1+
      kits = KitDb.matchAll(Depend.parse("testKitDb 1.1+"));
      verifyEq(kits.length, 3);
      verifyKitFile(kits[0], 0xaaaaaaaa, "1.1.3500");  
      verifyKitFile(kits[1], 0xbbbbbbbb, "1.1.3501");  
      verifyKitFile(kits[2], 0xcccccccc, "1.2.3600");  
      
      // matchAll: 0xaaaaaaaa
      kits = KitDb.matchAll(Depend.parse("testKitDb 0xaaaaaaaa"));
      verifyEq(kits.length, 2);
      verifyKitFile(kits[0], 0xaaaaaaaa, "1.0.3403");  
      verifyKitFile(kits[1], 0xaaaaaaaa, "1.1.3500");  

      // matchBest
      KitFile kit;
      kit = KitDb.matchBest("testKitDb");
      verifyKitFile(kit, 0xcccccccc, "1.2.3600");  
      
      // matchBest: 1.0
      kit = KitDb.matchBest(Depend.parse("testKitDb 1.0"));
      verifyKitFile(kit, 0x0aaaaaaaa, "1.0.3403");              
      
      // matchBest: 1.1
      kit = KitDb.matchBest(Depend.parse("testKitDb 1.1+"));
      verifyKitFile(kit, 0xcccccccc, "1.2.3600");  

      // matchBest: 0x12345678
      kit = KitDb.matchBest(Depend.parse("testKitDb 0x12345678"));
      verifyKitFile(kit, 0x12345678, "1.0.3402");  
      
      // matchBest: 1.3
      kit = KitDb.matchBest(Depend.parse("testKitDb 1.3"));
      verify(kit == null);        
      
      // matchBest: 1.0.3400=
      kit = KitDb.matchBest(Depend.parse("testKitDb 1.0.3400="));
      verifyKitFile(kit, 0x12345678, "1.0.3400");
      
      // matchBest: 1.0.3400.1=
      kit = KitDb.matchBest(Depend.parse("testKitDb 1.0.3400.1="));
      verifyKitFile(kit, 0x12345678, "1.0.3400.1");
      
      // matchBest: 1.0=
      kit = KitDb.matchBest(Depend.parse("testKitDb 1.0="));
      verify(kit == null);
    }
    finally
    {
      FileUtil.delete(dir, null);
    }                    
  }          

  void verifyKitFile(KitFile kitFile, int checksum, String version)
  {
    verifyEq(kitFile.name, "testKitDb");
    verifyEq(kitFile.checksum, checksum);
    verifyEq(kitFile.version.toString(), version);
    verifyEq(kitFile.exists(), true);
  }
  
  void makeKitFile(File dir, int checksum, String version)
    throws Exception
  {
    File f = new File(dir, dir.getName() + "-" + 
      Integer.toHexString(checksum) + "-" + version + ".kit");
    PrintWriter out = new PrintWriter( openFileWriter(f) );
    out.print("dummy test file");
    out.close();
  }  
  

}
