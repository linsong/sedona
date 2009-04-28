//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   24 Apr 07  Brian Frank  Creation
//

package sedonac.ir;

import sedonac.namespace.*;

/**
 * IrVTable
 */
public class IrVTable
  implements IrAddressable
{

//////////////////////////////////////////////////////////////////////////
// IrAddressable
//////////////////////////////////////////////////////////////////////////

  public int getBlockIndex() { return blockIndex; }
  public void setBlockIndex(int i) { blockIndex = i; }
  
  public boolean alignBlockIndex() { return true; }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public int blockIndex;
  public IrMethod[] methods;

}
