//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Jun 09  Brian Frank  Creation
//

package sedonacert.prov;

import sedona.sox.*;
import sedonacert.*;

/**
 * Bundle for provisioning tests.
 */
public class Prov extends Bundle
{         
  
  public Prov(Runner runner) 
  { 
    super(runner, "prov", new Test[]
    { 
      new ProvBackup(),
    });
  }       
  
}
