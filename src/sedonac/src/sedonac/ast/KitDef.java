//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Mar 07  Brian Frank  Creation
//

package sedonac.ast;

import java.util.*;
import sedona.util.*;
import sedonac.*;
import sedonac.namespace.*;

/**
 * KitDef models the kit definition in the AST - it is parsed from "kit.xml".
 */
public class KitDef
  extends AstNode
  implements Kit
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public KitDef(Location loc)
  {
    super(loc);
  }

//////////////////////////////////////////////////////////////////////////
// Kit
//////////////////////////////////////////////////////////////////////////

  public String name() { return name; }
  public Type[] types() { return types; }
  public Type type(String name) { return (Type)typesByName.get(name); }

//////////////////////////////////////////////////////////////////////////
// AstNode
//////////////////////////////////////////////////////////////////////////

  public void write(AstWriter out)
  {
    out.w("kit ").w(name).nl();
    out.w("  vendor:        ").w(vendor).nl();
    out.w("  version:       ").w(version).nl();
    out.w("  description:   ").w(description).nl();
    out.w("  includeSource: ").w(includeSource).nl();
    out.w("  doc:           ").w(doc).nl();
    out.nl();
    for (int i=0; types != null && i<types.length; ++i)
      types[i].write(out);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public String name;
  public String vendor;
  public Version version;
  public String description;
  public boolean includeSource;
  public boolean doc;
  public TypeDef[] types;
  public HashMap typesByName;
  public DependDef[] depends;
  public IncludeDef[] includes; 
  public NativeDef[] natives;
  public Type[] reflectiveTypes;  // includes primitives for sys

}
