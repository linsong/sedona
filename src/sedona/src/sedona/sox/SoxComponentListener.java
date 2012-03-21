//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Jun 07  Brian Frank  Creation
//

package sedona.sox;

import java.io.*;
import java.util.*;
import sedona.*;

/**
 * SoxComponentListener fires callbacks when SoxComponents are modified.
 */
public interface SoxComponentListener
{

  /**
   * Callback when the specified component is modified.  The mask
   * indicates what was changed (TREE, CONFIG, RUNTIME, or LINKS).
   */
  public void changed(SoxComponent c, int mask);

}

