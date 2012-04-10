/**
 * Copyright 2009 Tridium, Inc. All Rights Reserved.
 */
package sedonacert.prov;

import sedona.*;
import sedona.offline.*;
import sedona.sox.*;

/**
 * Restore the app and scode that were backed up by ProvInit.
 * 
 * @author Matthew Giannini
 * @creation Dec 8, 2009
 *
 */
public class ProvRestore extends ProvTest
{

  public ProvRestore()
  {
    super("restore");
  }
  
  public void run() throws Throwable
  {
    provision(prov().initSab, prov().initScode);
    
    
    OfflineApp origApp = OfflineApp.decodeAppBinary(prov().initSab);
    SoxClient c = runner.sox;
    Schema runningSchema = runner.schema;
    verifyEq(origApp.schema.key, runningSchema.key);
    
    SoxComponent app = c.loadApp();
    verify(app.child(ProvAddKit.certName) == null);
    verify(app.child("soxcert") == null); // deleted by ProvInit
  }

}
