//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 Jun 08  Brian Frank  Creation
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
 * SecurityTest (over Sox)
 */
public class SecurityTest
  extends AbstractSoxTest  
  implements Constants
{

  public void doTest()
    throws Exception
  {                        
    // verify root has full access       
    connect("root");
    verifyPerm(g0,   or|ow|oi|ar|aw|ai|ua);
    verifyPerm(g1,   or|ow|oi|ar|aw|ai|ua);
    verifyPerm(g2,   or|ow|oi|ar|aw|ai|ua);
    verifyPerm(g3,   or|ow|oi|ar|aw|ai|ua);
    verifyPerm(g01,  or|ow|oi|ar|aw|ai|ua);
    verifyPerm(g23,  or|ow|oi|ar|aw|ai|ua);
    verifyUser(u0, true);
    verifyUser(u3, true);
    verifyTree(g2, true); 
    verifyLink(g0, g23, true);
    verifyProv(getSvmName(), true);
    verifyProv("kits.scode", false);
    verifyProv("app.sab", false);
    client.close();     
    
    // verify ann has full access op access only to group 0   
    connect("ann");
    verifyPerm(g0,   or|ow|oi);
    verifyPerm(g1,   0);
    verifyPerm(g2,   0);
    verifyPerm(g3,   0);
    verifyPerm(g01,  or|ow|oi);
    verifyPerm(g23,  0);
    verifyUser(u0, false);
    verifyUser(u3, false);
    verifyKids(app, new OfflineComponent[] { service, g0 });
    verifyTree(g01, false);
    verifyLink(g0, g01, false);
    verifyProv("svm.exe", false);
    verifyProv("kits.scode", false);
    verifyProv("app.sab", true);
    client.close();     
    
    // verify bob has ro group 0 and or/ow in group 2
    connect("bob");
    verifyPerm(g0,   or);
    verifyPerm(g1,   0);
    verifyPerm(g2,   or|ow);
    verifyPerm(g3,   0);
    verifyPerm(g01,  or);
    verifyPerm(g23,  or|ow);
    verifyKids(app, new OfflineComponent[] { service, g0, g2 });
    verifyTree(g2, false);
    verifyProv("svm.exe", false);
    verifyProv("kits.scode", true);
    verifyProv("app.sab", false);             
    client.close();                   
    
    // dan 
    //   group 3  ar|aw|or|ow|oi|ua
    //   group 2  ar|ai|or|ow
    //   group 1  or|oi
    //   group 0  nil
    connect("dan");
    verifyPerm(g0,  0);
    verifyPerm(g1,  or|oi);
    verifyPerm(g2,  or|ow|ar|ai);
    verifyPerm(g3,  or|ow|oi|ar|aw|ua);
    verifyPerm(g01, or|oi);
    verifyPerm(g12, ar|ai|or|ow|oi);
    verifyPerm(g23, or|ow|oi|ar|aw|ai|ua);
    verifyUser(u0, false);
    verifyUser(u3, true);
    verifyKids(app, new OfflineComponent[] { g1, g2, g3 });
    verifyTree(g2, false);
    verifyTree(g3, true);
    client.close();                    
  }              
  
  void verifyPerm(OfflineComponent s, int perm)
    throws Exception
  {             
    System.out.println("-- " + client.username + " " + s.name() + " 0x" + Integer.toHexString(perm) + "...");          
    SoxComponent c = verifyCompRead(s, perm);
    if (c == null) return;
    
    verifyEq(c.id(), s.id());          
    updateOrSub = !updateOrSub; 
    if (updateOrSub)
      client.subscribe(c, ALL);        
    else
      client.update(c, ALL);        
    
    // verify different permissions
    verifyOpRead(s, c, perm);
    verifyAdminRead(s, c, perm);
    verifyOpWrite(s, c, perm);
    verifyAdminWrite(s, c, perm);
    verifyOpInvoke(s, c, perm);
    verifyAdminInvoke(s, c, perm);
    
    // unsubscribe      
    client.unsubscribe(c, ALL);        
  }                      
  
  SoxComponent verifyCompRead(OfflineComponent s, int perm)
    throws Exception
  {               
    SoxComponent c = null;       
    Exception ex = null;         
    try
    {
      c = client.load(s.id());   
    }
    catch (Exception e) { ex = e; }
    // System.out.println("  verify: " + s.name() + " 0x" + Integer.toHexString(perm) + " -> " + c);    
    
    // no operator read means we can't even see component
    if ((perm & or) == 0) 
    {
      verify(c == null);  
      verify(ex != null);  

      // verify no update (using direct id)
      ex = null;
      try { client.update(new SoxComponent(client, s.id(), s.type), TREE);  } catch(Exception e) { ex = e; }      
      verify(ex != null); 
      
      // verify no subscribe (using direct id)
      // Sox Protocol 1.1 silently ignore subscription in this case.
//      ex = null;
//      try { client.subscribe(new SoxComponent(client, s.id(), s.type), ALL);  } catch(Exception e) { ex = e; }      
//      verify(ex == null);
    }                     
    
    return c;
  }
    
  
  void verifyOpRead(OfflineComponent s, SoxComponent c, int perm)
    throws Exception
  { 
    // read prop
    verifyEq(readInt(s, "i2"), -123456789);
    verifyEq(readLong(s, "j1"), 'o');     
    
    // update (readComp)
    verifyEq(c.permissions(), perm);
    verifyEq(c.getInt("i2"), -123456789);
    verifyEq(c.getLong("j1"), 'o');         
  }
    
  void verifyAdminRead(OfflineComponent s, SoxComponent c, int perm)
    throws Exception
  {
    Exception ex = null;
    if ((perm & ar) != 0)
    {
      // read prop
      verifyEq(readInt(s, "i1"), 'a');
      verifyEq(readStr(s, "str"), "hi");     
      
      // update (readComp)
      verifyEq(c.getInt("i1"), 'a');
      verifyEq(c.getDouble("d1"), 123456789d);
      verifyEq(c.getDouble("d2"), 256d);
      verifyEq(c.getStr("str"), "hi");     
      
      // verify can see links
      verifyEq(c.links().length, 2);
    }
    else
    {
      // read prop        
      try { readInt(s, "i1"); } catch (Exception e) { ex = e; } 
        verify(ex != null); ex = null; 
      try { readStr(s, "str"); } catch (Exception e) { ex = e; } 
        verify(ex != null); ex = null;
      
      // update (readComp)
      verifyEq(c.getInt("i1"), 0);
      verifyEq(c.getDouble("d1"), 0d);
      verifyEq(c.getDouble("d2"), 256d);
      verifyEq(c.getStr("str"), "foo");
      
      // verify can't see links
      verifyEq(c.links().length, 0);
    }                      
  }
    
  void verifyOpWrite(OfflineComponent s, SoxComponent c, int perm)
    throws Exception
  {
    Exception ex = null;
    try
    {
      client.write(c, c.type.slot("j1"), Long.make(0xabcdffff0123L));
    }                   
    catch (Exception e) { ex = e; }    
    if ((perm & ow) != 0)
    {             
      verifyEq(readLong(s, "j1"), 0xabcdffff0123L);
      client.write(c, c.type.slot("j1"), Long.make('o'));
    }                              
    else
    {                        
      verify(ex != null);
      verifyEq(readLong(s, "j1"), 'o');
    }     
  }
    
  void verifyAdminWrite(OfflineComponent s, SoxComponent c, int perm)
    throws Exception
  {
    Exception ex = null;
    try
    {
      client.write(c, c.type.slot("str"), Str.make("foo"));
    }                   
    catch (Exception e) { ex = e; }    
    if ((perm & aw) != 0)
    {             
      verifyEq(readStr(s, "str"), "foo");
      client.write(c, c.type.slot("str"), Str.make("hi"));
    }                              
    else
    {
      verify(ex != null);
      if ((perm & ar) != 0)
        verifyEq(readStr(s, "str"), "hi");
    }         
  }
    
  void verifyOpInvoke(OfflineComponent s, SoxComponent c, int perm)
    throws Exception
  {
    Exception ex = null;
    try
    {
      client.invoke(c, c.type.slot("addJ1"), Long.make(2));
    }                   
    catch (Exception e) { ex = e; }    
    if ((perm & oi) != 0)
    {             
      verifyEq(readLong(s, "j1"), 'q');
      client.invoke(c, c.type.slot("addJ1"), Long.make(-2));
      verifyEq(readLong(s, "j1"), 'o');
    }                              
    else
    {               
      verify(ex != null);
      verifyEq(readLong(s, "j1"), 'o');
    }      
  }
    
  void verifyAdminInvoke(OfflineComponent s, SoxComponent c, int perm)
    throws Exception
  {
    Exception ex = null;
    try
    {
      client.invoke(c, c.type.slot("incI1"), null);
    }                   
    catch (Exception e) { ex = e; }    
    if ((perm & ai) != 0)
    {             
      verifyEq(readInt(s, "i1"), 'b');
      client.invoke(c, c.type.slot("addI1"), Int.make(-1));
      verifyEq(readInt(s, "i1"), 'a');
    }                              
    else
    {
      verify(ex != null);
      if ((perm & ar) != 0)
        verifyEq(readInt(s, "i1"), 'a');
    }
  }

  void verifyTree(OfflineComponent s, boolean adminWrite)
    throws Exception
  {                            
    SoxComponent c = client.load(s.id());                               
    int origNumKids = client.update(c, TREE).children().length;
    verify(origNumKids != 0);
    
    Type addType = schema.type("sys::Folder");
    Value[] addValues = new Value[] {Int.make(s.getInt("meta"))};
    
    if (adminWrite)
    {
      // add
      SoxComponent folder = client.add(c, addType, "folder", addValues);
      verifyEq(client.update(folder, TREE).name(), "folder");
      verifyEq(client.update(c, TREE).children().length, origNumKids+1);
      
      // rename
      verifyEq(client.update(c, TREE).name(), s.name());
      client.rename(c, "foobar");
      verifyEq(client.update(c, TREE).name(), "foobar");
      client.rename(c, s.name());
      
      // delete
      client.delete(folder);
      verifyEq(client.update(c, TREE).children().length, origNumKids);
      
      // reorder        
      int[] kidIds = c.childrenIds();
      ArrayUtil.swap(kidIds, 0, kidIds.length-1);
      client.reorder(c, kidIds);
      verifyEq(client.update(c, TREE).childrenIds(), kidIds);
    }
    else
    {                            
      Exception ex = null;
      
      // add
      try { client.add(c, addType, "folder", addValues); } catch (Exception e) { ex = e; }
      verify(ex != null); ex = null;
      verifyEq(client.update(c, TREE).children().length, origNumKids);
      
      // rename
      verifyEq(client.update(c, TREE).name(), s.name());
      try { client.rename(c, "foobar"); } catch (Exception e) { ex = e; }
      verify(ex != null); ex = null;
      verifyEq(client.update(c, TREE).name(), s.name());
      
      // delete
      try { client.delete(c); } catch (Exception e) { ex = e; }
      verify(ex != null); ex = null;
      verifyEq(client.update(c, TREE).name(), s.name());
      
      // reorder                                                  
      int[] oldIds = c.childrenIds();
      int[] newIds = c.childrenIds();
      ArrayUtil.swap(newIds, 0, newIds.length-1);
      try { client.reorder(c, newIds); } catch (Exception e) { ex = e; }
      verifyEq(client.update(c, TREE).childrenIds(), oldIds);
      verify(ex != null); ex = null;
    }     
  }

  void verifyLink(OfflineComponent from, OfflineComponent to, boolean can)
    throws Exception
  {           
    int slotId = to.type.slot("s2").id;
    Link link = new Link(from.id(), slotId, to.id(), slotId);
    SoxComponent cto   = client.load(to.id());  
    boolean arOnTo = (cto.permissions() & ar) != 0;
    if (can)
    { 
      // link
      client.link(link);
      
      // verify link
      Link[] links = client.update(cto, LINKS).links();
      verifyEq(links.length, 3);                            
      verifyHasLink(links, link);
      
      // unlink
      client.unlink(link);
      verifyEq(client.update(cto, LINKS).links().length, 2);
    }
    else
    { 
      Exception ex = null;
      
      // verify can't link
      try { client.link(link); } catch (Exception e) { ex = e; }
      Link[] links = client.update(cto, LINKS).links();
      verifyEq(links.length, arOnTo ? 2 : 0);
      verify(ex != null);

      // verify can't unlink  
      ex = null;
      try { client.unlink(links[1]); } catch (Exception e) { ex = e; }
      verifyEq(links.length, arOnTo ? 2 : 0);
      links = client.update(cto, LINKS).links();
      verifyEq(links.length, arOnTo ? 2 : 0);
      verify(ex != null);      
    }                                                              
  }                          
  
  void verifyHasLink(Link[] links, Link link)
  {
    for (int i=0; i<links.length; ++i)
      if (links[i].equals(link)) return;
    verify(false, "Missing link " + link);
  }

  void verifyKids(OfflineComponent s, OfflineComponent[] skids)
    throws Exception
  {               
    SoxComponent c = client.load(s.id());      
    
    /*
    System.out.println("expected kids:");
    for (int i=0; i<skids.length; ++i)
      System.out.println("  " + skids[i].id() + " " + skids[i].name());
    System.out.println("actual kids:");
    for (int i=0; i<c.children().length; ++i)
      System.out.println("  " + c.children()[i].id() + " " + c.children()[i].name());
    */
      
    verifyEq(c.children().length, skids.length);
    for (int i=0; i<skids.length; ++i)
      verifyEq(c.children()[i].id(), skids[i].id());
  }           

////////////////////////////////////////////////////////////////
// User Admin
////////////////////////////////////////////////////////////////

  void verifyUser(OfflineComponent s, boolean can)
    throws Exception
  {                 
    SoxComponent c = verifyCompRead(s, can ? or|ow|oi|ar|aw|ai : 0);
    if (c == null) return;     
    client.update(c, ALL);        
    verifyEq(c.getInt("meta"), s.getInt("meta"));
    verifyEq(c.getBuf("cred"), s.getBuf("cred"));
    verifyEq(c.getInt("perm"), s.getInt("perm"));
  }

////////////////////////////////////////////////////////////////
// Provisioning
////////////////////////////////////////////////////////////////

  void verifyProv(String filename, boolean can)
    throws Exception
  {                                   
    File outFile = new File(testDir(), "tempprov");               
    outFile.delete();
    verify(outFile.length() == 0);
    
    Exception ex = null;
    try { client.getFile(filename, SoxFile.make(outFile), null, null); } catch (Exception e) { ex = e; }

    if (can)
    { 
      verify(outFile.length() > 0);
      verify(ex == null);     
    }
    else
    {
      verify(outFile.length() == 0);
      verify(ex != null);     
    }
  }
  
////////////////////////////////////////////////////////////////
// Setup
////////////////////////////////////////////////////////////////

  public void connect(String user)
    throws Exception
  {  
    trace("Connect [" + user + "]...");
    InetAddress addr = InetAddress.getLocalHost();
    client = new SoxClient(sock, addr, 1876, user, "pw");
    client.connect(null);
    verify(!client.isClosed());
    verify(client.session() != null);
  }

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

    this.app = new OfflineApp(schema);
    this.app.setInt("meta", 0xf);
    this.service = app.add(app, new OfflineComponent(schema.type("sys::Component"), "service"));
    this.plat  = app.add(service, new OfflineComponent(schema.type("sys::PlatformService"), "plat"));
    this.sox   = app.add(service, new OfflineComponent(schema.type("sox::SoxService"), "sox"));
    this.users = app.add(service, new OfflineComponent(schema.type("sys::UserService"), "users"));
    
    this.root = newUser("root", 0x01, 0x7f7f7f7f, provSvm);
    this.ann  = newUser("ann",  0x01, (or|ow|oi), provApp);
    this.bob  = newUser("bob",  0x01, ((or|ow) << 16) | or, provKits);
    this.dan  = newUser("dan",  0x01, ((ar|aw|or|ow|oi|ua)<<24)|((ar|ai|or|ow)<<16)|((or|oi)<<8), 0);
    this.u0   = newUser("u0",   0x01,  0, 0);
    this.u3   = newUser("u2",   0x08,  0, 0);
    
    this.g0   = newComp(app, "g0",   0x01);
    this.g1   = newComp(app, "g1",   0x02);
    this.g2   = newComp(app, "g2",   0x04);
    this.g3   = newComp(app, "g3",   0x08);
    this.g01  = newComp(g3,  "g01",  0x03);
    this.g12  = newComp(g3,  "g12",  0x06);
    this.g23  = newComp(g3,  "g23",  0x0C);
        
    app.assignIds();
    // app.dump();
    return app;                 
  }           
  
  OfflineComponent newUser(String name, int groups, int perm, int prov)
  {
    OfflineComponent c = app.add(users, new OfflineComponent(schema.type("sys::User"), name));
    c.setInt("meta", groups);                       
    c.setBuf("cred", new Buf(UserUtil.credentials(name, "pw")));  
    c.setInt("perm", perm);  
    c.setInt("prov", prov);  
    return c;
  }

  OfflineComponent newComp(OfflineComponent parent, String name, int groups)
  {                                         
    Type type = schema.type("sys::TestComp");
    
    OfflineComponent c = new OfflineComponent(type, name);
    c.setInt("meta", groups);                       
    c.setInt("i1", 'a');                       
    c.setLong("j1", 'o');                       
    c.setDouble("d1", 123456789d);                       
    c.setStr("str", "hi");
    app.add(parent, c);
    
    // add some dummy children
    OfflineComponent d1 = new OfflineComponent(type, "dummy1");
    d1.setInt("meta", groups);   
    app.add(c, d1);           
    OfflineComponent d2 = new OfflineComponent(type, "dummy2");
    d2.setInt("meta", groups);   
    app.add(c, d2);             
    
    // link d1.f1 -> c.f1
    // link c.f2  -> d2.f2
    Slot f1 = type.slot("f1", true);
    Slot f2 = type.slot("f2", true);
    app.addLink(new OfflineLink(d1, f1, c, f1));          
    app.addLink(new OfflineLink(c, f1,  d2, f2));          

    return c;
  }

//////////////////////////////////////////////////////////////////////////
// Database
//////////////////////////////////////////////////////////////////////////

  OfflineApp app;
    OfflineComponent service;
      OfflineComponent plat;
      OfflineComponent sox;
      OfflineComponent users;
        OfflineComponent root;
        OfflineComponent ann;
        OfflineComponent bob;
        OfflineComponent dan;
        OfflineComponent u0;
        OfflineComponent u3;
    OfflineComponent g0;
    OfflineComponent g1;
    OfflineComponent g2;
    OfflineComponent g3;
      OfflineComponent g01;
      OfflineComponent g12;
      OfflineComponent g23;

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  static final int TREE = SoxComponent.TREE;
  static final int ALL  = SoxComponent.TREE|SoxComponent.CONFIG|SoxComponent.RUNTIME|SoxComponent.LINKS;

  boolean updateOrSub;

}
