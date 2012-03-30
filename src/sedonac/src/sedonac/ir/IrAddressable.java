//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   6 Mar 07  Brian Frank  Creation
//

package sedonac.ir;

import sedonac.namespace.*;

/**
 * IrAddressable is the base class for IR constructs which
 * are addressed via a Sedona block index which is used to compute
 * portable memory addresses with the following equation:
 *   addr = codeBaseAddr + blockIndex*blockSize
 */
public interface IrAddressable
{

  public int getBlockIndex();
  public void setBlockIndex(int ix);

  public boolean alignBlockIndex();

  public static class Impl implements IrAddressable
  {
    public Impl(String name) { this.name = name; align = true; }
    public Impl(String name, boolean align) { this.name = name; this.align = align; }

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int ix) { blockIndex = ix; }

    public boolean alignBlockIndex() { return align; }

    public final String toString() { return "IrAddressable.Impl [" + name + "]"; }

    public String name;
    public int blockIndex;    
    boolean align;
  }

}
