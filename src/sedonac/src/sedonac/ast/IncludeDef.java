//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Dec 08  Matthew Giannini Creation
//
package sedonac.ast;

import java.util.*;
import sedona.*;
import sedonac.*;
import sedonac.ir.*;

/**
 * IncludeDef models a set of type includes from a particular kit.
 */
public class IncludeDef
  extends DependDef
{

  public IncludeDef(Location loc, Depend depend)
  {
    super(loc, depend);
    this.typeToSource = new HashMap();
    this.sourceKit = null;
  }
  
  /**
   * Maps include types to the ZipEntry source file in the sourceKit that
   * defines the type.
   * <p/>
   * The InitKitCompile step adds just the type names. Zip entries are
   * mapped during the ResolveIncludes step.
   */
  public HashMap typeToSource; // String (type name) -> ZipEntry
  public IrKit sourceKit;
  
}
