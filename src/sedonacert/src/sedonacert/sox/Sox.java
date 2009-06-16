//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Jun 09  Brian Frank  Creation
//

package sedonacert.sox;

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
    });
  } 
  
}
