//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   12 May 09  Brian Frank  Creation
//

package sedonac.ast;

import sedona.*;
import sedonac.*;

/**
 * Base class for TypeDef and SlotDef
 */
public abstract class FacetsNode extends AstNode
{

  public FacetsNode(Location loc, FacetDef[] facetDefs)
  {                                             
    super(loc);
    this.facetDefs = facetDefs;
  }

  public Facets facets() 
  {                    
    if (resolvedFacets == null) 
      throw new IllegalStateException("Facets haven't been resolved yet");
    return resolvedFacets;
  }                    
  
  public void setResolvedFacets(Facets f)
  {                                     
    resolvedFacets = f;
  }                   
  
  public void setFacet(String name, int val)
  {      
    facets(); // facets must be resolved
    if (resolvedFacets.isRO())
      resolvedFacets = new Facets();
    resolvedFacets.seti(name, val);  
  }

  public void addFacetDef(String name, Expr val)
  {
    FacetDef[] temp = new FacetDef[facetDefs.length+1];
    System.arraycopy(facetDefs, 0, temp, 0, facetDefs.length);
    temp[facetDefs.length] = new FacetDef(loc, name, val);
    facetDefs = temp;
  }

  public void walkFacets(AstVisitor visitor, int depth)
  {             
    for (int i=0; i<facetDefs.length; ++i)
      facetDefs[i].walk(visitor, depth);
  }

  public FacetDef[] facetDefs;
  private Facets resolvedFacets;
}
