//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Jun 07  Brian Frank  Creation
//

package sedonac.test;

import java.io.*;
import sedona.*;
import sedona.offline.*;
import sedona.util.*;
import sedona.xml.*;

/**
 * AppTest
 */
public class AppTest
  extends Test
{

  public void testIO()
    throws Exception
  {
    // build app
    buildApp();
    // app.dump();
    verify(app.equivalent(app));

    // verify lookup
    verify(app.lookup(0)      == app);
    verify(app.lookup(a.id())   == a);
    verify(app.lookup(c.id())   == c);
    verify(app.lookup("/")    == app);
    verify(app.lookup("/a")   == a);
    verify(app.lookup("/a/b") == b);
    verify(app.lookup("/a/c") == c);
    verify(app.lookup("/foo") == null);

    // serialize to XML
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    XWriter xout = new XWriter(bout);
    app.encodeAppXml(xout,false);
    xout.flush();

    // serialize from XML
    XElem xml = XParser.make("buf", new ByteArrayInputStream(bout.toByteArray())).parse();
    OfflineApp app2 = OfflineApp.decodeAppXml(xml);
    // app2.dump();
    verify(app.equivalent(app2));

    // serialize to binary
    Buf buf = app.encodeAppBinary();

    // serialize from binary
    buf.flip();
    OfflineApp app3 = OfflineApp.decodeAppBinary(buf);
    // app3.dump();
    verify(app.equivalent(app3));
  }                            

  public void testErrChecking()
    throws Exception
  {                 
    buildApp();
    Exception ex;

    // wrongType: bufB = Str
    ex = null; try { a.set("bufB", Str.make("foo")); } catch (Exception e) { ex = e; }         
    verify(ex != null);

    // wrongType: str = Buf
    ex = null; try { a.set("str", new Buf()); } catch (Exception e) { ex = e; }         
    verify(ex != null);

    // wrongType: i1 = float
    ex = null; try { a.set("i1", sedona.Float.make(2)); } catch (Exception e) { ex = e; }         
    verify(ex != null);
    
    // bufTooLong
    ex = null; try { a.set("bufB", new Buf(new byte[] { 0, 0, 0 })); } catch (Exception e) { ex = e; }         
    verify(ex != null);
    
    // strTooLong
    ex = null; try { a.set("str", Str.make("abcde")); } catch (Exception e) { ex = e; }         
    verify(ex != null);
    
    // strNotAscii
    ex = null; try { a.set("str", Str.make("\u00ab")); } catch (Exception e) { ex = e; }         
    verify(ex != null);
  }
  
  public void buildApp()          
    throws Exception
  {
    // build app
    this.schema = Schema.load(new KitPart[] { KitPart.forLocalKit("sys") });
    this.app = new OfflineApp(schema);
    this.a = new OfflineComponent(schema.type("sys::TestComp"), "a");
      a.setInt("b1", 0xf0);
      a.setInt("s1", 32000);
      a.setInt("i1", 0xfedcba08);
      a.setFloat("f1", 4.08f);
    this.b = new OfflineComponent(schema.type("sys::TestComp"), "b");
    this.c = new OfflineComponent(schema.type("sys::SubTestComp"), "c");
      c.setInt("sb", 7);
      c.setInt("si", 1972);
    app.add(app, a);
    app.add(a, b);
    app.add(a, c);
    app.assignIds();
    app.addLink(new OfflineLink(a, a.slot("b1"), b, b.slot("b2")));
  }

  Schema schema;
  OfflineApp app;
  OfflineComponent a, b, c;  

}
