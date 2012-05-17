//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Jun 09  Brian Frank  Creation
//

package sedona.vm;

import sedona.Facets;

/**
 * Context stores global state for a SVM
 */
public class Context
{        
  /**
   * Create a default Context with empty facets.
   */
  public Context()
  {
    facets = Facets.empty;
  }
  
  /**
   * Create a Context with the given facets. 
   * <p>
   * Note: The given facets are cloned and cannot be modified.
   * @see #getFacets()
   */
  public Context(final Facets facets)
  {
    this.facets = new Facets();
    String[] keys = facets.keys();
    for (int i=0; i<keys.length; ++i)
      this.facets.set(keys[i], facets.get(keys[i]));
    this.facets.ro();
  }
  
  /**
   * @return a read-only version of the Context facets.
   */
  public final Facets getFacets() { return facets; }
  
  /**
   * A SVM is running in a "sandbox" if the {@code sandboxed} facet for this
   * Context is set to {@code true}.
   * 
   * @return {@code getFacets().getb("sandboxed", false)}
   */
  public boolean isSandboxed() { return facets.getb("sandboxed", false); }
  
  private Facets facets;
}

