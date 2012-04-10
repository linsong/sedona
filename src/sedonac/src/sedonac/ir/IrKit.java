//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   3 Mar 07  Brian Frank  Creation
//

package sedonac.ir;

import java.util.*;
import sedona.kit.*;
import sedona.manifest.*;
import sedona.util.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.namespace.*;

/**
 * IrKit
 */
public class IrKit
  implements Kit, IrAddressable
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

 public IrKit(Location loc, String name)
 {
   this.loc = loc;
   this.name = name;
 }

 public IrKit(Location loc, KitFile file)
 {
   this.loc = loc;
   this.name = file.name;                
   this.file = file;
 }

 public IrKit()
 {
 }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  public void dump()
  {
    System.out.println("Kit " + name + " [" + id + "]");
    for (int i=0; types != null && i<types.length; ++i)
      types[i].dump();
  }

//////////////////////////////////////////////////////////////////////////
// Kit
//////////////////////////////////////////////////////////////////////////

  public String name() { return name; }
  public Type[] types() { return types; }
  public Type type(String name) 
  { 
    if (typesByName != null)
      return (Type)typesByName.get(name); 
    
    System.out.println("WARNING: IrKit.typesByName not set yet");
    for (int i=0; i<types.length; ++i)
      if (types[i].name.equals(name)) return types[i];
    return null;
  }

  public IrType[] reflectiveTypes()
  {
    ArrayList acc = new ArrayList();
    for (int i=0; i<types.length; ++i)
    {
      if (types[i].isReflective())
        acc.add(types[i]);
    }
    return (IrType[])acc.toArray(new IrType[acc.size()]);
  }

  public IrType[] compTypes()
  {
    ArrayList acc = new ArrayList();
    for (int i=0; i<types.length; ++i)
      if (types[i].isaComponent())
        acc.add(types[i]);
    return (IrType[])acc.toArray(new IrType[acc.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// IrAddressable
//////////////////////////////////////////////////////////////////////////

  public int getBlockIndex() { return blockIndex; }
  public void setBlockIndex(int i) { blockIndex = i; }
  
  public boolean alignBlockIndex() { return true; }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public Location loc;
  public KitFile file;
  public int id;
  public String name;
  public Version version;
  public boolean test;
  public IrType[] types;
  public HashMap typesByName;
  public int blockIndex;
  public KitManifest manifest;

}
