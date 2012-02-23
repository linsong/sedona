/**
 * Copyright 2009 Tridium, Inc. All Rights Reserved.
 */
package sedonacert.prov;

import sedona.offline.OfflineApp;
import sedona.sox.*;

/**
 *
 * @author Matthew Giannini
 * @creation Dec 4, 2009
 *
 */
public class ProvInit extends ProvTest
{
  public ProvInit()
  {
    super("init");
  }

  public void run() throws Throwable
  {
    SoxClient c = runner.sox;
    SoxComponent app = c.loadApp();
    SoxComponent soxCert = app.child("soxcert");
    if (soxCert == null) 
      fail("Could not get soxcert component");
    
    // remove soxcert component that sox bundle added and save
    c.delete(soxCert);
    c.invoke(app, app.slot("save"), null);
    
    // save off the current app and scode state to the bundle
    // so that other tests can restore it.
    getApp(prov().initSab);
    getScode(prov().initScode);
    
    // sanity check the saved app
    OfflineApp a = OfflineApp.decodeApp(prov().initSab);
    verifyEq(a.schema.key, c.readSchema().key);
    
    // initial app cannot have soxcert kit installed
    if (a.schema.kit("soxcert") != null) 
      fail("soxcert kit cannot be installed by default"); 
  }

}
