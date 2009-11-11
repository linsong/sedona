//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 May 09  Brian Frank  Creation
//

package sedonac.steps;

import sedona.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * ResolveFacets is used to map each type and slot's FacetDef[] 
 * into a Facets instance by mapping the value Exprs into Values.
 */
public class ResolveFacets
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public ResolveFacets(Compiler compiler)
  {
    super(compiler);  
  }

  public void run()
  {
    log.debug("  ResolveFacets");
    walkAst(WALK_TO_SLOTS);
    quitIfErrors();
  }

//////////////////////////////////////////////////////////////////////////
// Visitor
//////////////////////////////////////////////////////////////////////////

  public void enterType(TypeDef t)
  {
    super.enterType(t);         
    resolve(t);
  }

  public void enterField(FieldDef f)
  {
    super.enterField(f);
    resolve(f);              
    f.setRtFlags(TypeUtil.rtFlags(f, f.facets()));
  }
  
  public void enterMethod(MethodDef m)
  {
    super.enterMethod(m);
    resolve(m);
    m.setRtFlags(TypeUtil.rtFlags(m, m.facets()));
  }
  
//////////////////////////////////////////////////////////////////////////
// Resolve Facets
//////////////////////////////////////////////////////////////////////////

  void resolve(FacetsNode node)
  {
    // if no facets defined this is easy
    if (node.facetDefs.length == 0)
    {                                   
      node.setResolvedFacets(Facets.empty);           
      return;
    }     

    // resolve facets into             
    Facets facets = new Facets();
    for (int i=0; i<node.facetDefs.length; ++i)
    {
      FacetDef f = node.facetDefs[i];
      if (facets.get(f.name) != null)
        err("Duplicate facet name '" + f.name + "'", f.loc);
      facets.set(f.name, resolve(f));
    } 
    node.setResolvedFacets(facets);
  }
  
  Value resolve(FacetDef f)
  {
    Expr.Literal literal = f.val.toLiteral();
    if (literal == null)
    { 
      err("Facet value must be a literal", f.loc);
      return Bool.TRUE;
    }      
    return literal.toValue();
  }

}
