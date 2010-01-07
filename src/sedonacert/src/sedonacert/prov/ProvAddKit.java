/**
 * Copyright 2009 Tridium, Inc. All Rights Reserved.
 */
package sedonacert.prov;

import java.io.*;

import sedona.*;
import sedona.kit.*;
import sedona.offline.*;
import sedona.sox.*;
import sedona.util.*;
import sedona.xml.*;

/**
 * Test that we can provision the device to run
 * an app with a new kit.
 * 
 * @author Matthew Giannini
 * @creation Dec 4, 2009
 *
 */
public class ProvAddKit extends ProvTest
{
  public ProvAddKit()
  {
    super("addKit");
  }
  
  public void run() throws Throwable
  {
    init();
    createAddSab();
    makeScode(schema, addScodeXml, true);
    provision(addSab, addScodeBin);
    
    // now get the running app and scode. Verify the schema
    getApp(prov().addSab);
    getScode(prov().addScode);
    OfflineApp savedApp = OfflineApp.decodeAppBinary(prov().addSab);
    verifyEq(savedApp.schema.key, schema.key);
    
    // verify the cert component
    SoxClient c = runner.sox;
    SoxComponent app = c.loadApp();
    SoxComponent cert = app.child(certName);
    verify(cert != null);
    verifyEq(c.readProp(cert, cert.slot("certInt")), intVal);
  }
  
  private void init()
  {
    this.addSax = new File(runner.testDir, "addKit.sax");
    this.addSab = new File(runner.testDir, "addKit.sab");
    this.addScodeXml = new File(runner.testDir, "addKit.xml");
    this.addScodeBin = new File(runner.testDir, "addKit.scode");
  }
  
  private void createAddSab() throws Throwable
  {
    // convert initSab to sax
    compile(prov().initSab);
    
    // add soxcert kit
    XElem root = XParser.make(getSax(prov().initSab)).parse();
    KitFile certKit = KitDb.matchBest("soxcert");
    if (certKit == null) fail("soxcert kit not in the kit database");
    root.elem("schema").addContent(
      new XElem("kit")
      .addAttr("name", certKit.name)
      .addAttr("checksum", TextUtil.intToHexString(certKit.checksum)));

    // re-load app with new schema and add CertComp component
    OfflineApp addApp = OfflineApp.decodeAppXml(root);
    verify(addApp.schema.kit("soxcert") != null);
    schema = addApp.schema;
    
    OfflineComponent certComp = 
      new OfflineComponent(schema.type("soxcert::CertComp"), certName);
    certComp.set(certComp.slot("certInt"), intVal = Int.make(certKit.checksum));
    
    addApp.add(addApp, certComp);
    addApp.assignIds();
    addApp.encodeAppBinary(addSab);
  }
  
  private File getSax(File sab)
  {
    return new File(runner.testDir, FileUtil.getBase(sab.getName())+".sax");
  }
  
  public static final String certName = "cert"; 

  protected File addSax;
  protected File addSab;
  protected File addScodeXml;
  protected File addScodeBin;
  protected Schema schema;
  protected Int intVal;

}
