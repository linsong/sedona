//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 Jun 07  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import java.net.*;
import java.util.*;
import sedona.*;
import sedona.Byte;
import sedona.Short;
import sedona.Long;
import sedona.Float;
import sedona.Double;
import sedona.offline.*;
import sedona.dasp.*;
import sedona.sox.*;
import sedona.util.*;
import sedona.xml.*;
import sedonac.Compiler;

/**
 * SoxTest
 */
public class SoxTest
  extends AbstractSoxTest
{

  public void doTest()
    throws Exception
  {
    verifyConnect();
    verifyServer("verifyTrue");
    verifySchema();
    verifyVersion();
    verifyLoad();              
    verifyWrite();       
    verifyInvoke();       
    verifyUpdate();
    verifySubscribe();
    verifyAdd();
    verifyRename();
    verifyReorder();
    verifyDelete();
    verifyLinks();          
    verifyQuery();              
    verifyFileTransfer(); 
    verifyBinaryTransfer();
    verifyFileRename();
    verifyClose();
  }

//////////////////////////////////////////////////////////////////////////
// Connect
//////////////////////////////////////////////////////////////////////////

  private void verifyConnect()
    throws Exception
  {
    InetAddress addr = InetAddress.getLocalHost();
    Exception ex;

    // verify invalid user name
    /* TODO: things are stateless now, but eventually we'll have security
    trace("Connect invalid username...");
    client = new UdpSoxClient(addr, 1876, "foo", "");
    ex = null;
    try { client.connect(); } catch (SoxAuthException e) { ex = e; }
    verify(ex != null);
    verify(client.isClosed());

    // verify invalid password name
    trace("Connect invalid password...");
    client = new UdpSoxClient(addr, 1876, "admin", "blah");
    ex = null;
    try { client.connect(); } catch (SoxAuthException e) { ex = e; }
    verify(ex != null);
    verify(client.isClosed());
    */

    Hashtable options = new Hashtable();    
    options.put("dasp.test", new DaspTest.TestHooks());

    // connect bad username
    trace("Connect with bad username...");      
    int errorCode = -1;
    try
    {
      client = new SoxClient(sock, addr, 1876, "baduser", "");
      client.connect(options);
    }
    catch (DaspException e) 
    { 
      errorCode = e.errorCode; 
    }
    verifyEq(errorCode, DaspConst.NOT_AUTHENTICATED);

    // connect bad password
    trace("Connect with bad password...");      
    errorCode = -1;
    try
    {
      client = new SoxClient(sock, addr, 1876, "admin", "");
      client.connect(options);
    }
    catch (DaspException e) 
    { 
      errorCode = e.errorCode; 
    }
    verifyEq(errorCode, DaspConst.NOT_AUTHENTICATED);

    // connect
    trace("Connect with good username and password...");
    client = new SoxClient(sock, addr, 1876, "admin", "pw");
    client.connect(options);
    verify(!client.isClosed());
    verify(client.session() != null);
  }

//////////////////////////////////////////////////////////////////////////
// Schema
//////////////////////////////////////////////////////////////////////////

  private void verifySchema()
    throws Exception
  {
    Schema a = this.schema;
    Schema b = client.readSchema();
    verify(a.equivalent(b));
  }
  
//////////////////////////////////////////////////////////////////////////
// Version
//////////////////////////////////////////////////////////////////////////

  private void verifyVersion()
    throws Exception
  {
    VersionInfo v = client.readVersion();
    verifyEq(v.kits.length, schema.kits.length);
    for (int i=0; i<v.kits.length; ++i)
    {
      verifyEq(v.kits[i].name,     schema.kits[i].name);
      verifyEq(v.kits[i].checksum, schema.kits[i].checksum);
      verifyEq(v.kits[i].version,  schema.kits[i].manifest.version);
    }       
    verify(v.props != null);
  }

//////////////////////////////////////////////////////////////////////////
// Load (lazy)
//////////////////////////////////////////////////////////////////////////

  private void verifyLoad()
    throws Exception
  {
    SoxComponent capp = client.loadApp();
    verifyEq(capp.id(), 0);
    verifyEq(capp.name(), "app");
    verifyEq(capp.path(), "/");
    verifyEq(capp.parentId(), Component.nullId);
    verifyEq(capp.type, schema.type("sys::App"));
    verifyEq(capp.childrenIds().length, 4);
    verifyEq(capp.childrenIds()[0], app.children()[0].id());
    verifyEq(capp.childrenIds()[1], app.children()[1].id());
    verifyEq(capp.childrenIds()[2], app.children()[2].id());
    verifyEq(capp.childrenIds()[3], app.children()[3].id());

    // verify read skipping parents
    SoxComponent csoxTest = client.load(soxTestId);
    verifyEq(csoxTest.path(), "/service/sox/soxtest");
    verifyEq(csoxTest.id(), soxTestId);
    verifyEq(csoxTest.name(), "soxtest");
    verifyEq(csoxTest.parentId(), sox.id());
    verifyEq(csoxTest.children().length, 0);

    // lookup children
    SoxComponent[] kids = capp.children();
    SoxComponent cservice = capp.child("service");
    SoxComponent ca = capp.child("a");
    SoxComponent cb = capp.child("b");
    SoxComponent cbaz = capp.child("baz");
    verifyEq(kids.length, 4);
    verify(kids[0] == cservice);
    verify(kids[1] == ca);
    verify(kids[2] == cb);
    verify(kids[3] == cbaz);

    // verify a
    verifyEq(ca.id(), a.id());
    verifyEq(ca.name(), "a");
    verifyEq(ca.path(), "/a");
    verifyEq(ca.parentId(), 0);
    verify(ca.parent() == capp);
    verifyEq(ca.childrenIds().length, 2);
  }

//////////////////////////////////////////////////////////////////////////
// Write
//////////////////////////////////////////////////////////////////////////

  private void verifyWrite()
    throws Exception
  {
    // verify original values
    verifyEq(readBool(aa, "z2"),  true);
    verifyEq(readByte(aa, "b1"),  0);
    verifyEq(readShort(aa, "s2"), 0xbeef);
    verifyEq(readInt(aa, "i2"),   -123456789);
    verifyEq(readLong(aa, "j2"),  -3000000000L);
    verifyEq(readFloat(aa, "f2"), 2.04f);
    verifyEq(readDouble(aa, "d2"), 256.0);

    // write
    client.write(aa.id(), aa.type.slot("z2", true), Bool.make(false));
    client.write(aa.id(), aa.type.slot("b1", true), sedona.Byte.make(0xe3));
    client.write(aa.id(), aa.type.slot("s2", true), sedona.Short.make(1234));
    client.write(aa.id(), aa.type.slot("i2", true), Int.make(69));
    client.write(aa.id(), aa.type.slot("j2", true), Long.make(0xaabbccddeeffL));
    client.write(aa.id(), aa.type.slot("f2", true), sedona.Float.make(-908f));
    client.write(aa.id(), aa.type.slot("d2", true), sedona.Double.make(204.0));
    client.write(aa.id(), aa.type.slot("bufB", true), new Buf(new byte[] {0x19, 0x72}));
    client.write(aa.id(), aa.type.slot("str", true), Str.make("G\n!>"));

    // verify new written values
    verifyEq(readBool(aa, "z2"),  false);
    verifyEq(readByte(aa, "b1"),  0xe3);
    verifyEq(readShort(aa, "s2"), 1234);
    verifyEq(readInt(aa, "i2"),   69);     
    verifyEq(readLong(aa, "j2"),  0xaabbccddeeffL);
    verifyEq(readFloat(aa, "f2"), -908f);
    verifyEq(readDouble(aa, "d2"), 204d);
    verifyEq(readBuf(aa, "bufB"), new Buf(new byte[] {0x19, 0x72}));
    verifyEq(readStr(aa, "str"), "G\n!>");
    
    // nulls
    client.write(aa.id(), aa.type.slot("z2", true), Bool.NULL);
    client.write(aa.id(), aa.type.slot("f2", true), sedona.Float.NULL);
    verify(read(aa, "z2") == Bool.NULL);
    verifyEq(read(aa, "z2").isNull(), true);
    verify(read(aa, "f2") == Float.NULL);
    verifyEq(read(aa, "f2").isNull(), true);

    // test buffer overrun
    Component.testMode = true;
    client.write(aa.id(), aa.type.slot("bufA", true), new Buf(new byte[] {0xa, 0xb, 0xc, 0xd, 0xe}));
    client.write(aa.id(), aa.type.slot("bufB", true), new Buf(new byte[] {0xa, 0xb, 0xc, 0xd, 0xe}));
    client.write(aa.id(), aa.type.slot("str", true), Str.make("01234"));
    verifyEq(readBuf(aa, "bufA"), new Buf(new byte[] {0xa, 0xb, 0xc, 0xd}));
    verifyEq(readBuf(aa, "bufB"), new Buf(new byte[] {0xa, 0xb}));
    verifyEq(readStr(aa, "str"), "0123");
    Component.testMode = false;
    
    // test buffer overrun with error checking
    Exception ex;
    ex = null; try { client.write(aa.id(), aa.type.slot("bufA", true), new Buf(new byte[] {0xa, 0xb, 0xc, 0xd, 0xe})); } catch(Exception e) { ex = e; }
    verify(ex != null);
    ex = null; try {  client.write(aa.id(), aa.type.slot("str", true), Str.make("01234")); } catch(Exception e) { ex = e; }
    verify(ex != null);
    ex = null; try {  client.write(aa.id(), aa.type.slot("str", true), Str.make("\u0080")); } catch(Exception e) { ex = e; }
    verify(ex != null);
    
    // test write to component with virtual actions
    ex = null; try { client.write(baz.id(), baz.type.slot("bazFloat", true), Float.make(2.0f)); } catch(Exception e) { ex = e; }
    if (ex != null) ex.printStackTrace();
    verify(ex == null);
  }

//////////////////////////////////////////////////////////////////////////
// Invoke
//////////////////////////////////////////////////////////////////////////

  private void verifyInvoke()
    throws Exception
  { 
    // reset to clean slate
    client.write(aa.id(), aa.type.slot("i1", true), Int.make(9));
    client.write(aa.id(), aa.type.slot("z1", true), Bool.FALSE);
    client.write(aa.id(), aa.type.slot("d1", true), sedona.Double.make(77.0));
    client.write(aa.id(), aa.type.slot("bufA", true), new Buf(new byte[] {}));
    client.write(aa.id(), aa.type.slot("str", true), Str.make(""));
    
    // verify clean slate
    verifyEq(readInt(aa, "i1"),  9);
    verifyEq(readBool(aa, "z1"),  false);
    verifyEq(readDouble(aa, "d1"), 77.0);
    verifyEq(readBuf(aa, "bufA"), new Buf(new byte[] {}));
    verifyEq(readStr(aa, "str"), "");  
    
    // invokes
    client.invoke(aa.id(), aa.type.slot("incI1", true), null);
    client.invoke(aa.id(), aa.type.slot("setZ1", true), Bool.TRUE);
    client.invoke(aa.id(), aa.type.slot("addD1", true), Double.make(303.0));
    client.invoke(aa.id(), aa.type.slot("actionBuf", true), new Buf(new byte[] { 0x1a, 0x2b, 0x3c, 0x4d }));
    client.invoke(aa.id(), aa.type.slot("actionStr", true), Str.make("#$%^"));
    
    // verify invoke results
    verifyEq(readInt(aa, "i1"),  10);
    verifyEq(readBool(aa, "z1"),  true);
    verifyEq(readDouble(aa, "d1"), 380.0);
    verifyEq(readBuf(aa, "bufA"), new Buf(new byte[] { 0x1a, 0x2b, 0x3c, 0x4d }));
    verifyEq(readStr(aa, "str"), "#$%^");  
    
    // buffer overruns
    client.invoke(aa.id(), aa.type.slot("actionBuf", true), new Buf(new byte[] { (byte)0xca, (byte)0xfe, (byte)0xba, (byte)0xbe, (byte)0xff}));
    verifyEq(readBuf(aa, "bufA"), new Buf(new byte[] { (byte)0xca, (byte)0xfe, (byte)0xba, (byte)0xbe}));
  }

//////////////////////////////////////////////////////////////////////////
// Update
//////////////////////////////////////////////////////////////////////////

  private void verifyUpdate()
    throws Exception
  {
    // load a and verify current tree and hollow props/links
    SoxComponent ca = client.load(a.id());
    Listener listener = new Listener(ca);
    verifyEq(ca.name(), "a");
    verifyEq(ca.children().length, 2);
    verifyEq(ca.children()[0].id(), aa.id());
    verifyEq(ca.children()[1].id(), ab.id());
    verifyEq(ca.get("z1").isNull(), true);
    verifyEq(ca.getBool("z2"), true);
    verifyEq(ca.getInt("b1"), 0);
    verifyEq(ca.getInt("b2"), 0xab);
    verifyEq(ca.getInt("s1"), 0);
    verifyEq(ca.getInt("s2"), 0xbeef);
    verifyEq(ca.getInt("i1"), 0);
    verifyEq(ca.getInt("i2"), -123456789);
    verifyEq(ca.getLong("j1"), 0);
    verifyEq(ca.getLong("j2"), -3000000000L);
    verifyEq(ca.get("f1").isNull(), true);
    verifyEq(ca.getFloat("f1"), java.lang.Float.NaN);
    verifyEq(ca.getFloat("f2"), 2.04f);
    verifyEq(ca.getDouble("d1"), 0.0);
    verifyEq(ca.getDouble("d2"), 256d);
    verifyEq(ca.getBuf("bufA").size, 0);
    verifyEq(ca.getBuf("bufB").size, 0);
    verifyEq(ca.links().length, 0);

    // update everything
    client.update(ca, CONFIG|RUNTIME|LINKS);
    verifyEq(ca.getBool("z2"), true);
    verifyEq(ca.getInt("b1"), 0xf0);
    verifyEq(ca.getInt("b2"), 0xab);
    verifyEq(ca.getInt("s1"), 32000);
    verifyEq(ca.getInt("s2"), 0xbeef);
    verifyEq(ca.getInt("i1"), 0xfedcba08);
    verifyEq(ca.getInt("i2"), -123456789);
    verifyEq(ca.getLong("j1"), 0);
    verifyEq(ca.getLong("j2"), -3000000000L);
    verifyEq(ca.getFloat("f1"), 4.08f);
    verifyEq(ca.getFloat("f2"), 2.04f);
    verifyEq(ca.getDouble("d1"), 0.0);
    verifyEq(ca.getDouble("d2"), 256.0);
    verifyEq(ca.getBuf("bufA"), new Buf());
    verifyEq(ca.getBuf("bufB"), new Buf());
    verifyEq(ca.links().length, 1);
    verifyLink(ca.links()[0], a, "b1", aa, "b2");
    listener.verify(CONFIG|RUNTIME|LINKS);

    // make a change to all the properties and verify
    invoke(soxTest, "changeProps", Int.make(a.id()));
    client.update(ca, CONFIG|RUNTIME|LINKS);
    verifyEq(ca.getBool("z2"), false);
    verifyEq(ca.getInt("b1"), 0xf1);
    verifyEq(ca.getInt("b2"), 0xac);
    verifyEq(ca.getInt("s1"), 32001);
    verifyEq(ca.getInt("s2"), 0xbef0);
    verifyEq(ca.getInt("i1"), 0xfedcba09);
    verifyEq(ca.getInt("i2"), -123456788);
    verifyEq(ca.getLong("j1"), 1);
    verifyEq(ca.getLong("j2"), -2999999999L);
    verifyEq(ca.getFloat("f1"), 5.08f);
    verifyEq(ca.getFloat("f2"), 3.04f);
    verifyEq(ca.getDouble("d1"), 1.0);
    verifyEq(ca.getDouble("d2"), 257.0);
    verifyEq(ca.getBuf("bufA"), new Buf(new byte[] {1}));
    verifyEq(ca.getBuf("bufB"), new Buf(new byte[] {1}));
    verifyEq(ca.links().length, 1);
    listener.verify(CONFIG|RUNTIME|LINKS);
  }

//////////////////////////////////////////////////////////////////////////
// Subscribe
//////////////////////////////////////////////////////////////////////////

  private void verifySubscribe()
    throws Exception
  {
    // subscribe to tree events
    client.subscribeToAllTreeEvents();
    
    // load a
    SoxComponent ca = client.load(a.id());
    Listener listener = new Listener(ca);
    listener.clear();
    verifyEq(ca.subscription(), TREE);
    verifySub(a, 0);

    // change all the props
    invoke(soxTest, "changeProps", Int.make(a.id()));

    // verify old props (from last update)
    verifyEq(ca.getBool("z2"), false);
    verifyEq(ca.getInt("b1"), 0xf1);
    verifyEq(ca.getInt("b2"), 0xac);
    verifyEq(ca.getInt("s1"), 32001);
    verifyEq(ca.getInt("s2"), 0xbef0);
    verifyEq(ca.getInt("i1"), 0xfedcba09);
    verifyEq(ca.getInt("i2"), -123456788);
    verifyEq(ca.getLong("j1"), 1);
    verifyEq(ca.getLong("j2"), -2999999999L);
    verifyEq(ca.getFloat("f1"), 5.08f);
    verifyEq(ca.getFloat("f2"), 3.04f);
    verifyEq(ca.getDouble("d1"), 1.0);
    verifyEq(ca.getDouble("d2"), 257.0);
    verifyEq(ca.getBuf("bufA"), new Buf(new byte[] {1}));
    verifyEq(ca.getBuf("bufB"), new Buf(new byte[] {1}));

    // subscribe to just runtime
    client.subscribe(ca, RUNTIME);
    verifyEq(ca.subscription(), TREE|RUNTIME);
    verifySub(a, RUNTIME);
    listener.verify(RUNTIME);

    // verify new rt props (but old config)
    verifyEq(ca.getBool("z2"), true);
    verifyEq(ca.getInt("b1"), 0xf1);
    verifyEq(ca.getInt("b2"), 0xad);
    verifyEq(ca.getInt("s1"), 32001);
    verifyEq(ca.getInt("s2"), 0xbef1);
    verifyEq(ca.getInt("i1"), 0xfedcba09);
    verifyEq(ca.getInt("i2"), -123456787);
    verifyEq(ca.getLong("j1"), 1);
    verifyEq(ca.getLong("j2"), -2999999998L);
    verifyEq(ca.getFloat("f1"), 5.08f);
    verifyEq(ca.getFloat("f2"), 4.04f);
    verifyEq(ca.getDouble("d1"), 1.0);
    verifyEq(ca.getDouble("d2"), 258.0);
    verifyEq(ca.getBuf("bufA"), new Buf(new byte[] {1}));
    verifyEq(ca.getBuf("bufB"), new Buf(new byte[] {2}));

    // change all the props
    invoke(soxTest, "changeProps", Int.make(a.id()));
    pause();

    // verify new rt props (but old config)
    verifyEq(ca.getBool("z2"), false);
    verifyEq(ca.getInt("b1"), 0xf1);
    verifyEq(ca.getInt("b2"), 0xae);
    verifyEq(ca.getInt("s1"), 32001);
    verifyEq(ca.getInt("s2"), 0xbef2);
    verifyEq(ca.getInt("i1"), 0xfedcba09);
    verifyEq(ca.getInt("i2"), -123456786);
    verifyEq(ca.getLong("j1"), 1);
    verifyEq(ca.getLong("j2"), -2999999997L);
    verifyEq(ca.getFloat("f1"), 5.08f);
    verifyEq(ca.getFloat("f2"), 5.04f);
    verifyEq(ca.getDouble("d1"), 1.0);
    verifyEq(ca.getDouble("d2"), 259.0);
    verifyEq(ca.getBuf("bufA"), new Buf(new byte[] {1}));
    verifyEq(ca.getBuf("bufB"), new Buf(new byte[] {3}));
    listener.verify(RUNTIME);

    // subscribe to config
    client.subscribe(ca, CONFIG);
    verifyEq(ca.subscription(), TREE|CONFIG|RUNTIME);
    verifySub(a, CONFIG|RUNTIME);
    listener.verify(CONFIG);

    // verify new rt anc config props
    verifyEq(ca.getBool("z2"), false);
    verifyEq(ca.getInt("b1"), 0xf3);
    verifyEq(ca.getInt("b2"), 0xae);
    verifyEq(ca.getInt("s1"), 32003);
    verifyEq(ca.getInt("s2"), 0xbef2);
    verifyEq(ca.getInt("i1"), 0xfedcba0b);
    verifyEq(ca.getInt("i2"), -123456786);
    verifyEq(ca.getLong("j1"), 3);
    verifyEq(ca.getLong("j2"), -2999999997L);
    verifyEq(ca.getFloat("f1"), 7.08f);
    verifyEq(ca.getFloat("f2"), 5.04f);
    verifyEq(ca.getDouble("d1"), 3.0);
    verifyEq(ca.getDouble("d2"), 259.0);
    verifyEq(ca.getBuf("bufA"), new Buf(new byte[] {3}));
    verifyEq(ca.getBuf("bufB"), new Buf(new byte[] {3}));

    // change all the props
    invoke(soxTest, "changeProps", Int.make(a.id()));
    pause();

    // verify new rt anc config props
    verifyEq(ca.getBool("z2"), true);
    verifyEq(ca.getInt("b1"), 0xf4);
    verifyEq(ca.getInt("b2"), 0xaf);
    verifyEq(ca.getInt("s1"), 32004);
    verifyEq(ca.getInt("s2"), 0xbef3);
    verifyEq(ca.getInt("i1"), 0xfedcba0c);
    verifyEq(ca.getInt("i2"), -123456785);
    verifyEq(ca.getLong("j1"), 4);
    verifyEq(ca.getLong("j2"), -2999999996L);
    verifyEq(ca.getFloat("f1"), 8.08f);
    verifyEq(ca.getFloat("f2"), 6.04f);
    verifyEq(ca.getDouble("d1"), 4.0);
    verifyEq(ca.getDouble("d2"), 260.0);
    verifyEq(ca.getBuf("bufA"), new Buf(new byte[] {4}));
    verifyEq(ca.getBuf("bufB"), new Buf(new byte[] {4}));
    listener.verify(RUNTIME|CONFIG);

    // unsubscribe to runtime
    client.unsubscribe(ca, RUNTIME);
    verifyEq(ca.subscription(), TREE|CONFIG);
    verifySub(a, CONFIG);

    // change all the props
    invoke(soxTest, "changeProps", Int.make(a.id()));
    pause();

    // verify new config props, old rt
    verifyEq(ca.getBool("z2"), true);
    verifyEq(ca.getInt("b1"), 0xf5);
    verifyEq(ca.getInt("b2"), 0xaf);
    verifyEq(ca.getInt("s1"), 32005);
    verifyEq(ca.getInt("s2"), 0xbef3);
    verifyEq(ca.getInt("i1"), 0xfedcba0d);
    verifyEq(ca.getInt("i2"), -123456785);
    verifyEq(ca.getLong("j1"), 5);
    verifyEq(ca.getLong("j2"), -2999999996L);
    verifyEq(ca.getFloat("f1"), 9.08f);
    verifyEq(ca.getFloat("f2"), 6.04f);
    verifyEq(ca.getDouble("d1"), 5.0);
    verifyEq(ca.getDouble("d2"), 260.0);
    verifyEq(ca.getBuf("bufA"), new Buf(new byte[] {5}));
    verifyEq(ca.getBuf("bufB"), new Buf(new byte[] {4}));
    listener.verify(CONFIG);

    // change just buf
    client.write(ca.id(), ca.type.slot("bufA", true), new Buf(new byte[] {19, 72}));
    pause();
    verifyEq(ca.getBuf("bufA"), new Buf(new byte[] {19, 72}));
    listener.verify(CONFIG);
  }

  private void verifySub(Component x, int mask)
    throws Exception
  {
    int arg = (x.id() << 24) | (client.remoteId() << 8) | (mask);
    verifyServer("verifySub", Int.make(arg));
  }

//////////////////////////////////////////////////////////////////////////
// Add
//////////////////////////////////////////////////////////////////////////

  private void verifyAdd()
    throws Exception
  {
    // load a
    SoxComponent ca = client.load(a.id());
    verifyEq(ca.children().length, 2);
    verifyEq(ca.children()[0].id(), aa.id());
    verifyEq(ca.children()[1].id(), ab.id());

    // add another TestComp
    Value[] vals =
    {
      Int.make(0x0203),    // meta
      Bool.TRUE,           // z1
      Byte.make(35),       // b2
      Short.make(1896),    // s2
      Int.make(123454321), // i2
      Long.make(0xeeeeddddccccbbbbL), // j2
      Float.make(601),     // f2
      Double.make(4.8),    // d2
      new Buf(new byte[] { (byte)0xaa, (byte)0xbb,
        (byte)0xcc, (byte)0xdd, (byte)0xee}),  // bufA (overrun)
      Str.make("hello"),  // str (overrun)
    };                          
    Component.testMode = true;
    SoxComponent x = client.add(ca, ca.type, "x", vals);
    this.xId = x.id();
    Component.testMode = false;

    // verify local client changes
    verifyEq(ca.children().length, 3);
    verifyEq(ca.children()[0].id(), aa.id());
    verifyEq(ca.children()[1].id(), ab.id());
    verifyEq(ca.children()[2].id(), x.id());
    verifyEq(x.parentId(), a.id());
    verify(x.client() == client);
    verify(x.parent() == ca);
    verifyEq(x.name(), "x");
    verifyEq(x.path(), "/a/x");
    verifyEq(x.getInt("meta"), 0x0203);
    verifyEq(x.getBool("z1"),  true);
    verifyEq(x.getInt("b1"),  35);
    verifyEq(x.getInt("s1"),  1896);
    verifyEq(x.getInt("i1"),  123454321);
    verifyEq(x.getLong("j1"), 0xeeeeddddccccbbbbL);
    verifyEq(x.getFloat("f1"),  601f);
    verifyEq(x.getDouble("d1"), 4.8);
    verifyEq(x.getBuf("bufA").size,   5);   // client doesn't see truncation yet
    verifyEq(x.getBuf("bufA").get(0), 0xaa);
    verifyEq(x.getBuf("bufA").get(1), 0xbb);
    verifyEq(x.getBuf("bufA").get(2), 0xcc);
    verifyEq(x.getBuf("bufA").get(3), 0xdd);
    verifyEq(x.getStr("str"), "hello");    // client doesn't see truncation yet

    // verify server side changes
    verifyServer("verifyAdd", Int.make(x.id()));
  }

//////////////////////////////////////////////////////////////////////////
// Rename
//////////////////////////////////////////////////////////////////////////

  private void verifyRename()
    throws Exception
  {
    SoxComponent cx = client.load(xId);
    verifyEq(cx.name(), "x");

    // do the rename
    client.rename(cx, "foobar");

    // verify client side
    verifyEq(cx.name(), "foobar");

    // verify server side
    verifyServer("verifyRename", Int.make(xId));
  }

//////////////////////////////////////////////////////////////////////////
// Reorder
//////////////////////////////////////////////////////////////////////////

  private void verifyReorder()
    throws Exception
  {
    // load proxies
    SoxComponent cb = client.load(b.id());
    SoxComponent cr = client.load(r.id());
    SoxComponent cs = client.load(s.id());
    SoxComponent ct = client.load(t.id());
    
    // old order
    verifyEq(cb.childrenIds().length, 3);
    verifyEq(cb.childrenIds()[0], r.id());
    verifyEq(cb.childrenIds()[1], s.id());
    verifyEq(cb.childrenIds()[2], t.id());
    verify(cb.children()[0] == cr);
    verify(cb.children()[1] == cs);
    verify(cb.children()[2] == ct);    
    
    // verify errors
    Exception ex = null;
    ex = null; 
    try { client.reorder(cb, new int[] { t.id(), s.id() }); } catch(Exception e) { ex = e; } 
    verify(ex != null);
    ex = null; 
    try { client.reorder(cb, new int[] { t.id(), s.id(), r.id(), r.id() }); } catch(Exception e) { ex = e; } 
    verify(ex != null);
    ex = null; 
    try { client.reorder(cb, new int[] { t.id(), s.id(), s.id() }); } catch(Exception e) { ex = e; } 
    verify(ex != null);
    ex = null; 
    try { client.reorder(cb, new int[] { t.id(), 9999, r.id() }); } catch(Exception e) { ex = e; } 
    verify(ex != null);   

    // order
    client.reorder(cb, new int[] { t.id(), s.id(), r.id() });
    client.update(cb, SoxComponent.TREE|SoxComponent.CONFIG|SoxComponent.RUNTIME|SoxComponent.LINKS);
    
    // verify server side
    verifyServer("verifyReorderCallback", Int.make(b.id()));


    // new order
    verifyEq(cb.childrenIds().length, 3);
    verifyEq(cb.childrenIds()[0], t.id());
    verifyEq(cb.childrenIds()[1], s.id());
    verifyEq(cb.childrenIds()[2], r.id());
    verify(cb.children()[0] == ct);
    verify(cb.children()[1] == cs);
    verify(cb.children()[2] == cr);    
    
    // order again
    client.reorder(cb, new int[] { t.id(), r.id(), s.id() });
    client.update(cb, SoxComponent.TREE|SoxComponent.CONFIG|SoxComponent.RUNTIME|SoxComponent.LINKS);
    
    // verify server side again
    verifyServer("verifyReorderCallback", Int.make(b.id()));
    
    // new order 
    verifyEq(cb.childrenIds().length, 3);
    verifyEq(cb.childrenIds()[0], t.id());
    verifyEq(cb.childrenIds()[1], r.id());
    verifyEq(cb.childrenIds()[2], s.id());
    verify(cb.children()[0] == ct);
    verify(cb.children()[1] == cr);
    verify(cb.children()[2] == cs);    
  }


//////////////////////////////////////////////////////////////////////////
// Delete
//////////////////////////////////////////////////////////////////////////

  private void verifyDelete()
    throws Exception
  {
    // load proxies
    SoxComponent ca   = client.load(a.id());
    SoxComponent cb   = client.load(b.id());
    SoxComponent caa  = client.load(aa.id());
    SoxComponent caa1 = client.load(aa1.id());
    SoxComponent caa2 = client.load(aa2.id());

    // check start case
    verifyEq(ca.children().length, 3);
    verifyEq(ca.children()[0].id(), aa.id());
    verifyEq(ca.children()[1].id(), ab.id());
    verifyEq(ca.children()[2].id(), xId);

    verifyEq(caa.children().length, 2);
    verifyEq(caa.children()[0].id(), aa1.id());
    verifyEq(caa.children()[1].id(), aa2.id());

    // check links before delete:
    //   a.b1   -> aa.b2
    //   aa.i1  -> b.i2
    //   aa1.i1 -> aa2.i2
    //   aa2.b1 -> b.b2
    client.update(ca, LINKS);
    client.update(cb, LINKS);
    verifyEq(ca.links().length, 1);
    verifyEq(cb.links().length, 2);
    verifyLink(ca.links()[0], a,   "b1", aa, "b2");
    verifyLink(cb.links()[0], aa2, "b1", b,  "b2");
    verifyLink(cb.links()[1], aa,  "i1", b,  "i2");

    // nuke aa
    client.delete(caa);

    // verify client side
    verifyEq(ca.children().length, 2);
    verifyEq(ca.children()[0].id(), ab.id());
    verifyEq(ca.children()[1].id(), xId);

    // error responses
    SoxException ex;
    ex = null; try { client.load(aa.id());  } catch(SoxException e) { ex = e; } verify(ex != null);
    ex = null; try { client.load(aa1.id()); } catch(SoxException e) { ex = e; } verify(ex != null);
    ex = null; try { client.load(aa2.id()); } catch(SoxException e) { ex = e; } verify(ex != null);

    // check links after delete:
    //   a.b1   -> aa.b2
    //   aa.i1  -> b.i2
    //   aa1.i1 -> aa2.i2
    //   aa2.b1 -> b.b2
    client.update(ca, LINKS);
    client.update(cb, LINKS);
    verifyEq(ca.links().length, 0);
    verifyEq(cb.links().length, 0);

    // verify server side
    verifyServer("verifyDelete", Int.make(aa.id()));
    verifyServer("verifyDelete", Int.make(aa1.id()));
    verifyServer("verifyDelete", Int.make(aa2.id()));
  }

//////////////////////////////////////////////////////////////////////////
// Links
//////////////////////////////////////////////////////////////////////////

  //   b:           TestComp
  //     r:         TestComp
  //     s:         TestComp
  //     t:         TestComp
  //

  private void verifyLinks()
    throws Exception
  {
    SoxComponent cr = client.load(r.id());
    SoxComponent cs = client.load(s.id());
    SoxComponent ct = client.load(t.id());

    Listener rListener = new Listener(cr);
    Listener sListener = new Listener(cs);
    Listener tListener = new Listener(ct);

    client.subscribe(new SoxComponent[] {cr, cs, ct}, LINKS);
    rListener.verify(LINKS);
    sListener.verify(LINKS);
    tListener.verify(LINKS);

    // start off:
    //   r.i1 -> s.i2
    //   s.f1 -> t.f2
    verifyEq(cr.links().length, 1);
      verifyLink(cr.links()[0], r, "i1", s, "i2");
    verifyEq(cs.links().length, 2);
      verifyLink(cs.links()[0], r, "i1", s, "i2");
      verifyLink(cs.links()[1], s, "f1", t, "f2");
    verifyEq(ct.links().length, 1);
      verifyLink(ct.links()[0], s, "f1", t, "f2");

    // add r.s1 -> t.s2
    client.link(new Link(cr, cr.slot("s1"), ct, ct.slot("s2")));
    pause();
    rListener.verify(LINKS);
    sListener.verify(0);
    tListener.verify(LINKS);

    // should now be:
    //   r.i1 -> s.i2
    //   r.s1 -> t.s2
    //   s.f1 -> t.f2
    verifyEq(cr.links().length, 2);
      verifyLink(cr.links()[0], r, "s1", t, "s2");
      verifyLink(cr.links()[1], r, "i1", s, "i2");
    verifyEq(cs.links().length, 2);
      verifyLink(cs.links()[0], r, "i1", s, "i2");
      verifyLink(cs.links()[1], s, "f1", t, "f2");
    verifyEq(ct.links().length, 2);
      verifyLink(ct.links()[0], r, "s1", t, "s2");
      verifyLink(ct.links()[1], s, "f1", t, "f2");

    // remove r.i1 -> s.i2
    client.unlink(new Link(cr, cr.slot("i1"), cs, cs.slot("i2")));
    pause();
    rListener.verify(LINKS);
    sListener.verify(LINKS);
    tListener.verify(0);

    // should now be:
    //   r.s1 -> t.s2
    //   s.f1 -> t.f2
    verifyEq(cr.links().length, 1);
      verifyLink(cr.links()[0], r, "s1", t, "s2");
    verifyEq(cs.links().length, 1);
      verifyLink(cs.links()[0], s, "f1", t, "f2");
    verifyEq(ct.links().length, 2);
      verifyLink(ct.links()[0], r, "s1", t, "s2");
      verifyLink(ct.links()[1], s, "f1", t, "f2");

    // remove r.s1 -> t.s2
    client.unlink(new Link(cr, cr.slot("s1"), ct, ct.slot("s2")));
    pause();
    rListener.verify(LINKS);
    sListener.verify(0);
    tListener.verify(LINKS);

    // should now be:
    //   s.f1 -> t.f2
    verifyEq(cr.links().length, 0);
    verifyEq(cs.links().length, 1);
      verifyLink(cs.links()[0], s, "f1", t, "f2");
    verifyEq(ct.links().length, 1);
      verifyLink(ct.links()[0], s, "f1", t, "f2");

    client.unsubscribe(new SoxComponent[] {cr, cs, ct}, LINKS);
  }

//////////////////////////////////////////////////////////////////////////
// Query
//////////////////////////////////////////////////////////////////////////

  private void verifyQuery()
    throws Exception
  { 
    // sox service                                                                 
    int[] ids = client.queryService(schema.type("sox::SoxService"));
    verifyEq(ids.length, 1);
    verifyEq(ids[0], sox.id());    
    
    // user service
    ids = client.queryService(schema.type("sys::UserService"));
    verifyEq(ids.length, 1);
    verifyEq(ids[0], users.id());    
    
    // web service
    ids = client.queryService(schema.type("web::WebService"));
    verifyEq(ids.length, 1);
    verifyEq(ids[0], web.id());    
    
    // all services
    ids = client.queryService(schema.type("sys::Service"));
    verifyEq(ids.length, 4);
    
    // zero entry is platform service (which might go away)
    verifyEq(ids[0], users.id());    
    verifyEq(ids[1], web.id());    
    verifyEq(ids[2], sox.id());    
    verifyEq(ids[3], plat.id());    
    
    // bad services
    ids = client.queryService(schema.type("sys::User"));
    verifyEq(ids.length, 0);
  }

//////////////////////////////////////////////////////////////////////////
// FileTransfer
//////////////////////////////////////////////////////////////////////////

  private void verifyFileTransfer()
    throws Exception
  {
    // test file not found
    verifyFileNotFound();

    // test empty file
    verifyFileTransfer("", null);

    // test 1 byte file
    verifyFileTransfer("#", null);

    // test 3 byte file
    verifyFileTransfer("abc", null);

    // now test right below and and above the 1, 2, and 3 chunk boundary
    for (int i=0; i<3; ++i)
    {
      Properties reqProps = new Properties();
      reqProps.put("chunkSize", "10");
      for (int j=8; j<=12; ++j)
      {
        String data = "";
        for (int x=0; x<i*10+j; ++x) data += (char)('0'+x);
        Properties resProps = verifyFileTransfer(data, reqProps);
        verify(resProps.get("chunkSize").equals("10"));
      }
    }

    // test little file with dropped chunks
    Properties reqProps = new Properties();
    reqProps.put("chunkSize", "3");
    reqProps.put("test.drop", "0");
    verifyFileTransfer("0123abcd[.]", reqProps);
    reqProps.put("test.drop", "1");
    verifyFileTransfer("0123abcd[.]", reqProps);
    reqProps.put("test.drop", "2");
    verifyFileTransfer("0123abcd[.]", reqProps);
    reqProps.put("test.drop", "3");
    verifyFileTransfer("0123abcd[.]", reqProps);
    reqProps.put("test.drop", "0,1,3");
    verifyFileTransfer("0123abcd[.]", reqProps);

    // test a big file
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<2000; ++i)
    {
      sb.append((char)('a'+(i%26)));
      if (i > 0 && (i+1)%26 == 0) sb.append("\n");
    }
    sb.append("\n[end]");
    verifyFileTransfer(sb.toString(), null);

    // test big file with dropped chunks
    reqProps = new Properties();
    reqProps.put("chunkSize", "99"); // 21 chunks (0-20)
    reqProps.put("test.drop", "0,4,17,19");
    verifyFileTransfer(sb.toString(), reqProps);
  }
  
  private void verifyBinaryTransfer() throws Exception
  {
    final String INIT = "0123456789";
    final String URI = "blob.txt";
    Properties putProps = new Properties();
    
    // create initial state
    putProps.put("mode", "w");
    client.putFile(URI, memFile(INIT), putProps, null);
    Thread.sleep(100);
    
    putProps.put("mode", "m");
    putProps.put("chunkSize", "1");
    // try a bad offset (bigger than file) for get
    verifyBinGet("", 0, 10);
    verifyBinGet("", 1, 10);
    
    // quickly test 1 byte read boundaries
    verifyBinGet("9", 1, 9);
    verifyBinGet("9", 0, 9);  // equivalent to previous
    verifyBinGet("0", 1, 0); 

    StringBuffer expected = new StringBuffer(INIT);
    for (int i=0; i<=INIT.length()/2 + 1; ++i)
    {
      final char c = (char)('a'+i);
      final String s = ""+c+c;
      putProps.put("offset", Integer.toString(i*2));
      System.out.println();
      System.out.println("]]]]]]]]]]]]]]]]]>>> TEST BINARY PUT " + putProps);
      setupBinaryDrops();
      client.putFile(URI, memFile(s), putProps, null);
      Thread.sleep(100);
      
      expected = new StringBuffer()
      .append(expected.substring(0, i*2))
      .append(c).append(c)
      .append(((i+1)*2 < INIT.length()) ? expected.substring((i+1)*2) : "");
      
      verifyBinGet(expected.toString(), 0, 0);
      verifyBinGet(s, 2, i*2);
      // next two make sure sox adjusts for file size that is too big for offset
      verifyBinGet(expected.substring(i), 0, i);
      verifyBinGet(expected.substring(i), expected.length(), i);
    }    
    
  }
  
  static int binDropCount = 0;
  private Properties verifyBinGet(String expected, final int fileSize, final int offset)
    throws Exception
  {
    Buf b = new Buf();
    Properties getProps = new Properties();
    getProps.put("fileSize", Integer.toString(fileSize));
    getProps.put("offset", Integer.toString(offset));
    getProps.put("chunkSize", "1");
    
    System.out.println();
    System.out.println("]]]]]]]]]]]]]]]]]>>> TEST BINARY GET " + getProps);
    if ((binDropCount++ % 3) == 0)
      setupBinaryDrops();

    Properties respProps = tryTransfer(GETFILE, client, "blob.txt", SoxFile.make(b), getProps);
    //Properties respProps = client.getFile("blob.txt", SoxFile.make(b), getProps, null);

    verifyEq(expected.length(), b.size);
    String s = new String(b.trim());
    System.out.println("Verify: " + expected + " == " + s);
    verifyEq(expected, s);
    
    ((DaspTest.TestHooks)client.session().test).clearDrop();
    return respProps;
  }
  
  private void setupBinaryDrops()
  {
    int[] drop = new int[] {0,1,2,3,5,7,11,13,17,19,21,23};
    DaspTest.TestHooks testHooks = (DaspTest.TestHooks)client.session().test;
    testHooks.clearDrop();
    for (int i=0; i < drop.length; ++i) 
      testHooks.addDrop(testHooks.sendSeqNum() + 1 + drop[i]);
  }
  
  private SoxFile memFile(String s) { return SoxFile.make(new Buf(s.getBytes())); }

  private void verifyFileNotFound()
    throws Exception
  {
    SoxException ex = null;
    try
    {
      client.getFile("boo hoo.txt", SoxFile.make(new File(testDir(), "x")), null, null);
    }
    catch (SoxException e)
    {
      System.out.println("  " + e);
      ex = e;
    }
    verify(ex != null);
    verify(ex.getMessage().indexOf("not found") > 0);
  }

  private Properties verifyFileTransfer(String data, Properties reqProps)
    throws Exception
  {                                                         
    // setup drop sequences        
    String dropStr = reqProps == null ? "" : reqProps.getProperty("test.drop", "");
    DaspTest.TestHooks testHooks = (DaspTest.TestHooks)client.session().test;
    
    // drop a keep alive for good measure
    if (dropStr.length() > 0) testHooks.dropType = DaspConst.KEEPALIVE;    
    
    // parse drop seq nums
    String[] dropToks = TextUtil.split(dropStr, ',');
    for (int i=0; i<dropToks.length; ++i)
    {
      int chunkNum = Integer.parseInt(dropToks[i]);
      testHooks.addDrop(testHooks.sendSeqNum() + 1 + chunkNum);
    }
      
    // write test file to app's directory
    File f = new File(testDir(), "transfer.txt");
    PrintWriter out = new PrintWriter( openFileWriter(f) );
    out.print(data);
    out.close();

    // get/read our test file
    System.out.println();
    System.out.println("]]]]]]]]]]]]]]]]]>>> TEST FILE GET " + reqProps + " " + data.length());
    File g = new File(testDir(), "get.txt");

    Properties resProps = tryTransfer(GETFILE, client, "transfer.txt", SoxFile.make(g), reqProps);
    //Properties resProps = client.getFile("transfer.txt", SoxFile.make(g), reqProps, null);

    verify(g.exists());
    verifyEq(g.length(), f.length());
    verifyEq(data, readToStr(g));        

    System.out.println();
    System.out.println("]]]]]]]]]]]]]]]]]>>> TEST FILE PUT " + reqProps);
    // put/write out test file
    File p = new File(testDir(), "put.txt");

    resProps = tryTransfer(PUTFILE, client, "put.txt", SoxFile.make(f), reqProps);
    //resProps = client.putFile("put.txt", SoxFile.make(f), reqProps, null);

    Thread.sleep(100); // give server a chance to close
    verify(p.exists());
    verifyEq(p.length(), f.length());
    verifyEq(data, readToStr(p));     

    return resProps;
  }

  private String readToStr(File f)
    throws IOException
  {
    char[] buf = new char[(int)f.length()];
    Reader in = new BufferedReader(new FileReader(f));
    try
    {
      for (int i=0, ch; (ch = in.read()) >= 0; ++i)
        buf[i] = (char)ch;
      return new String(buf);
    }
    finally
    {
      in.close();
    }
  }

//////////////////////////////////////////////////////////////////////////
// FileRename
//////////////////////////////////////////////////////////////////////////

  private void verifyFileRename()
    throws Exception
  {                            
    File o = new File(testDir(), "oldname.txt");
    File n = new File(testDir(), "newname.txt");
    o.delete();
    n.delete();
    new FileOutputStream(o).close();
    verify(o.exists());
    verify(!n.exists());
    
    client.renameFile("oldname.txt", "newname.txt");
    
    verify(!o.exists());
    verify(n.exists());
  }

//////////////////////////////////////////////////////////////////////////
// Close
//////////////////////////////////////////////////////////////////////////

  private void verifyClose()
    throws Exception
  {
    int remoteId = client.remoteId();

    trace("Close...");
    verify(!client.isClosed());
    verify(client.remoteId() != -1);
    client.close();
    verify(client.isClosed());
    verify(client.remoteId() == -1);

    client.connect();
    verifyServer("verifyClose", Int.make(remoteId));
    client.close();
    verify(client.isClosed());
  }

//////////////////////////////////////////////////////////////////////////
// Server App
//////////////////////////////////////////////////////////////////////////

  public OfflineApp buildApp()
    throws Exception
  {
    this.schema = Schema.load(new KitPart[]
    {
      KitPart.forLocalKit("sys"),
      KitPart.forLocalKit("sox"),
      KitPart.forLocalKit("inet"),
      KitPart.forLocalKit("web"),
    });
    // start web service on non-root port
    OfflineComponent web = new OfflineComponent(schema.type("web::WebService"), "web");
    web.setInt("port", 8080);
    
    this.app = new OfflineApp(schema);
    this.service = app.add(app, new OfflineComponent(schema.type("sys::Component"), "service"));
    this.plat    = app.add(service, new OfflineComponent(schema.type("sys::PlatformService"), "plat"));
    this.sox     = app.add(service, new OfflineComponent(schema.type("sox::SoxService"), "sox"));
    this.web     = app.add(service, web);
    this.soxTest = app.add(sox, new OfflineComponent(this.soxTestType = schema.type("sox::SoxTest"), "soxtest"));
    this.users   = app.add(service, new OfflineComponent(schema.type("sys::UserService"), "users"));
    this.admin   = app.add(users, new OfflineComponent(schema.type("sys::User"), "admin"));
    
    admin.setBuf("cred", new Buf(UserUtil.credentials("admin", "pw")));  
    admin.setInt("perm", 0xffffffff);  
    admin.setInt("prov", 0xff);  

    this.a = new OfflineComponent(schema.type("sys::TestComp"), "a");
      a.setInt("b1", 0xf0);
      a.setInt("b2", 0x32);
      a.setInt("s1", 32000);
      a.setInt("i1", 0xfedcba08);
      a.setFloat("f1", 4.08f);
    this.aa = new OfflineComponent(schema.type("sys::TestComp"), "aa");
    this.ab = new OfflineComponent(schema.type("sys::SubTestComp"), "ab");
      ab.setInt("sb", 7);
      ab.setInt("si", 1972);
    this.aa1 = new OfflineComponent(schema.type("sys::TestComp"), "aa1");
    this.aa2 = new OfflineComponent(schema.type("sys::TestComp"), "aa2");
    this.b = new OfflineComponent(schema.type("sys::TestComp"), "b");
      this.r = new OfflineComponent(schema.type("sys::TestComp"), "r");
      this.s = new OfflineComponent(schema.type("sys::TestComp"), "s");
      this.t = new OfflineComponent(schema.type("sys::TestComp"), "t");
    this.baz = new OfflineComponent(schema.type("sys::BazAction"), "baz");
    app.add(app, a);
      app.add(a, aa);
        app.add(aa, aa1);
        app.add(aa, aa2);
      app.add(a, ab);
    app.add(app, b);
      app.add(b, r);
      app.add(b, s);
      app.add(b, t);
    app.add(app, baz);
    app.assignIds();

    // a.b1   -> aa.b2
    // aa.i1  -> b.i2
    // aa1.i1 -> aa2.i2
    // aa2.b1 -> b.b2
    // r.i1   -> s.i2
    // s.f1   -> t.f2
    Type tc = a.type;
    app.addLink(new OfflineLink(a,   tc.slot("b1"), aa,  tc.slot("b2")));
    app.addLink(new OfflineLink(aa,  tc.slot("i1"), b,   tc.slot("i2")));
    app.addLink(new OfflineLink(aa1, tc.slot("i1"), aa2, tc.slot("i2")));
    app.addLink(new OfflineLink(aa2, tc.slot("b1"), b,   tc.slot("b2")));
    app.addLink(new OfflineLink(r,   tc.slot("i1"), s,   tc.slot("i2")));
    app.addLink(new OfflineLink(s,   tc.slot("f1"), t,   tc.slot("f2")));
    // app.dump();

    this.soxTestId = soxTest.id();
    verify(app.lookup(soxTestId) == soxTest);
    verify(app.equivalent(app));
    return app;
  }
  
//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  public void verifyServer(String action)
    throws Exception
  {
    verifyServer(action, null);
  }

  public void verifyServer(String action, Value arg)
    throws Exception
  {
    trace("verifyServer(" + action + ")");
    int verifiesBefore = readInt(soxTest, "verifies");
    int failuresBefore = readInt(soxTest, "failures");

    trace("{");
    invoke(soxTest, action, arg);
    trace("}");

    int verifiesAfter = readInt(soxTest, "verifies");
    int failuresAfter = readInt(soxTest, "failures");
    verify(verifiesAfter > verifiesBefore);
    verify(failuresAfter == failuresBefore);
  }

  public void verifyLink(Link link, OfflineComponent from, String fromSlotStr, OfflineComponent to, String toSlotStr)
  {
    Slot fromSlot = from.type.slot(fromSlotStr, true);
    Slot toSlot   = to.type.slot(toSlotStr, true);

    // System.out.println("verifyLink " + link + " ?= " + from.id() +"." + fromSlot.id + " -> " + to.id() + "." + toSlot.id);

    verifyEq(link.fromCompId, from.id());
    verifyEq(link.fromSlotId, fromSlot.id);
    verifyEq(link.toCompId,   to.id());
    verifyEq(link.toSlotId,   toSlot.id);
  }


  /**
   * Do a Sox file transfer, with a few retries if necessary.
   * Allows for OS conditions that might cause file I/O to fail temporarily.
   */
  private final static int GETFILE = 0;
  private final static int PUTFILE = 1;

  private Properties tryTransfer(int which, SoxClient c, String fname, SoxFile f, Properties props)
    throws Exception
  {
    Properties respProps = null;
    final int maxAttempts = 10;
    String fn = which==GETFILE ? "get" : "put";

    // Retry to avoid test failure due to transient OS issue (file in use, etc).
    // Give it maxAttempts chances to succeed before throwing exception. 
    int t;
    for (t=0; t<maxAttempts-1; t++)
    {
      try 
      {  
        if (which==GETFILE)      respProps = c.getFile(fname, f, props, null); 
        else if (which==PUTFILE) respProps = c.putFile(fname, f, props, null); 
        else throw new Exception("Error in test!");
      }
      catch (Exception e) { }

      if (respProps!=null) break;
      try { Thread.sleep(10); } catch (InterruptedException e) { }
    }


    // Try once more (if necessary), this time catch exception if any & fail
    if (respProps==null) 
    {
      try 
      {  
        if (which==GETFILE)      respProps = c.getFile(fname, f, props, null); 
        else if (which==PUTFILE) respProps = c.putFile(fname, f, props, null); 
        else throw new Exception("Error in test!");
      } 
      catch (Exception e)
      {
        e.printStackTrace();
        fail("\nFailed to " + fn + " file " + fname + " after " + maxAttempts + " attempts");
      }
    }
    else
      System.out.println("\t" + fn + "file(" + fname + ") succeeded after " + t + " failures.");
    
    return respProps;
  }



//////////////////////////////////////////////////////////////////////////
// Listener
//////////////////////////////////////////////////////////////////////////

  class Listener implements SoxComponentListener
  {
    Listener(SoxComponent target)
    {
      this.target = target;
      target.listener = this;
    }

    public void changed(SoxComponent comp, int mask)
    {
      this.comp = comp;
      this.mask |= mask;
    }

    void clear()
    {
      this.comp = null;
      this.mask = 0;
    }

    void verify(int mask)
    {
      if (mask == 0)
      {
        if (this.comp != null)
          System.out.println("Listener.verify " + comp + " mask=" + Integer.toHexString(this.mask));
        SoxTest.this.verify(this.comp == null);
      }
      else
      {
        SoxTest.this.verify(this.comp == target, "" + this.comp + " != " + target);
      }
      SoxTest.this.verifyEq(this.mask, mask);
      clear();
    }

    SoxComponent target;
    SoxComponent comp;
    int mask;
  }

//////////////////////////////////////////////////////////////////////////
// Database
//////////////////////////////////////////////////////////////////////////

  //
  // app
  //   service
  //     plat:      PlatformService
  //     sox:       SoxService
  //     web:       WebService
  //     soxText:   SoxTest
  //     users:     UserService
  //       admin:   User
  //   a:           TestComp
  //     aa:        TestComp
  //       aa1:     TestComp
  //       aa2:     TestComp
  //     ab:        TestComp
  //   b:           TestComp
  //     r:         TestComp
  //     s:         TestComp
  //     t:         TestComp
  //   c:           BazAction
  //
  // a.b1   -> aa.b2
  // aa.i1  -> b.i2
  // aa1.i1 -> aa2.i2
  // aa2.b1 -> b.b2
  // r.i1   -> s.i2
  // s.f1   -> t.f2
  //
  OfflineApp app;
    OfflineComponent service;
      OfflineComponent plat;
      OfflineComponent sox;
      OfflineComponent web;
      OfflineComponent soxTest;
      OfflineComponent users;
        OfflineComponent admin;
    OfflineComponent a;
      OfflineComponent aa;
        OfflineComponent aa1;
        OfflineComponent aa2;
      OfflineComponent ab;
    OfflineComponent b;
      OfflineComponent r;
      OfflineComponent s;
      OfflineComponent t;
    OfflineComponent baz;

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  Type soxTestType;
  int soxTestId;
  int xId;
}
