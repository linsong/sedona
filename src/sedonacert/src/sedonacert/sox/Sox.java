//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Jun 09  Brian Frank  Creation
//

package sedonacert.sox;

import sedona.sox.*;
import sedonac.test.TestException;
import sedonacert.*;

/**
 * Bundle for sox protocol tests.
 */
public class Sox extends Bundle
{         
  
  public Sox(Runner runner) 
  { 
    super(runner, "sox", new Test[]
    {                     
      new SoxLogin(),
      new SoxAdd(),   
      new SoxReadWrite(),
      new SoxInvoke(),
      new SoxLinks(),
      new SoxUpdate(),
      new SoxSubscribe(),
      new SoxRename(),
      new SoxReorder(),
      new SoxDelete(), 
      new SoxQuery(),
    });
  }       
  
  SoxComponent soxCertComp()         
    throws Exception
  {
    if (soxCertId <= 0) throw new TestException("Cannot if SoxAdd fails");
    return runner.sox.load(soxCertId);
  }
  
  int soxCertId = -1;
}
