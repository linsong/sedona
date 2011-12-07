//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Jun 07  Brian Frank  Creation
//

package sedonac.test;

import sedona.*;

/**
 * ManifestTest
 */
public class ManifestTest
  extends Test
{

  public void test()
    throws Exception
  {
    Schema schema = Schema.load(new KitPart[]
    {
      KitPart.forLocalKit("sys"),
    });     
    
    Type atc = schema.type("sys::AbstractTestComp");
    verifyEq(atc.qname, "sys::AbstractTestComp");
    verifyEq(atc.manifest.flags, Type.ABSTRACT | Type.PUBLIC);
    
    Type tc = schema.type("sys::TestComp");
    verifyEq(tc.qname, "sys::TestComp");
    verifyEq(tc.facets.size(), 2);
    verifyEq(tc.facets.getb("testonly"), true);    
    verifyEq(tc.facets.gets("testStr"), "roger\nroger");
    verifyEq(tc.manifest.flags, Type.PUBLIC);
    
    Slot z1 = tc.slot("z1");
    verifyEq(z1.qname,   "sys::TestComp.z1");
    verifyEq(z1.type.id, Type.boolId);
    verifyEq(z1.facets.size(), 6);
    verifyEq(z1.facets.getb("config"), true);
    verifyEq(z1.facets.getb("bTrue"),  true);
    verifyEq(z1.facets.getb("bFalse"), false);
    verifyEq(z1.facets.geti("i"), 35);
    verifyEq(z1.facets.getf("f"), 3.8f);
    verifyEq(z1.facets.gets("s"), "hello");

    verifyEq(tc.slot("bufA").facets.geti("max"), 4);
    verifyEq(tc.slot("bufB").facets.geti("max"), 2);
    verifyEq(tc.slot("str").facets.getb("asStr"), true);
    verifyEq(tc.slot("str").facets.geti("max"), 5);
  }

}
