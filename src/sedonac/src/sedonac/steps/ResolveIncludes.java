//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Dec 08  Matthew Giannini Creation
//
package sedonac.steps;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.namespace.*;
import sedonac.parser.*;

/**
 * Resolves all kits referenced in an {@code <include>} directive, and then
 * resolves the included types from those kits and puts them in the
 * compiler.ast.types list.
 */
public class ResolveIncludes
  extends ResolveDepends
{

  public ResolveIncludes(Compiler compiler)
  {
    super(compiler);
    this.autoMountKitIntoNamespace = false;
  }

  public void run()
  {
    IncludeDef[] includes = compiler.ast.includes;
    if (includes == null || includes.length == 0)
    {
      compiler.ast.types = new TypeDef[0];
      return;
    }

    log.debug("  ResolveIncludes");
    
    // Resolve IrKit for each included kit
    for (int i=0; i<includes.length; ++i)
      includes[i].sourceKit = resolveDepend(includes[i]);

    quitIfErrors();
    
    for (int i=0; i<includes.length; ++i)
    {
      IrKit sourceKit = includes[i].sourceKit;
      resolveRecursive(sourceKit, null);
      
      // Do not allow includes from kits that contain natives
      if (sourceKit.manifest.hasNatives)
        err("Cannot include types from kits that have natives: " + sourceKit.file);
    }
    
    quitIfErrors();
    
    // Resolve included types from each included kit
    ArrayList includeTypes = new ArrayList(includes.length);
    for (int i=0; i<includes.length; ++i)
    {
      autoInclude(includes[i]);
      resolveTypes(includes[i], includeTypes);
    }
      
    quitIfErrors();
    
    compiler.ast.types = (TypeDef[])includeTypes.toArray(new TypeDef[includeTypes.size()]);
  }
  
  /**
   * Auto-include any types from the source kit that are implicitly depended
   * on - even if they are not explicitly mentioned in the <include> directive.
   */
  private void autoInclude(IncludeDef include)
  {
    // Get list of type names in this <include> element
    Collection typeNameSet = include.typeToSource.keySet();
    String[] typeNames = (String[])typeNameSet.toArray( new String[typeNameSet.size()] );

    // For each type, check for dependencies on other types and auto-include them
    for (int j=0; j<typeNames.length; ++j)
      recursivelyAutoInclude(include, typeNames[j]);
  }
  
  private void recursivelyAutoInclude(IncludeDef include, final String typeName)
  {
    Type type = include.sourceKit.type(typeName);
    if (type == null) return; // We'll catch this error later
    Type base = type.base();
    
    int colon = -1;
    String baseKit = base.qname().substring(0, colon = base.qname().indexOf(':'));
    String baseTypeName = base.qname().substring(colon+2);
    
    if (baseKit.equals(include.depend.name())
      && !include.typeToSource.containsKey(baseTypeName))
    {
      StringBuffer sb = new StringBuffer()
      .append("Auto-Including '").append(base.toString()).append("' from kit ")
      .append(include.sourceKit.file).append(". '").append(typeName)
      .append("' depends on it.");
      warn(sb.toString());
      
      include.typeToSource.put(baseTypeName, null);
      recursivelyAutoInclude(include, baseTypeName);
    }
  }
  
  private void resolveTypes(IncludeDef include, ArrayList types)
  {
    File sourceKit = include.sourceKit.file.file;   
    Location zipLoc = null;
    try
    {
      ZipFile zip = new ZipFile(sourceKit);
      Enumeration entries = zip.entries();
      boolean hasSource = false;
      while (entries.hasMoreElements())
      {
        ZipEntry entry = (ZipEntry)entries.nextElement();
        if (entry.isDirectory() || !entry.getName().endsWith(".sedona")) continue;
        
        // Entry is a Sedona source file. Parse it to get its types.
        hasSource = true;
        zipLoc = new Location(sourceKit, entry);
        TypeDef[] astTypes = 
          new Parser(compiler, zipLoc, zip.getInputStream(entry)).parse();
        
        for (int i=0; i<astTypes.length; ++i)
        {
          if (include.typeToSource.containsKey(astTypes[i].name()))
          {
            // This TypeDef is one that should be included in the compiled kit.
            // Map the type to the source file entry that defines it, and then
            // store this TypeDef so we can add it to the AST type list later.
            include.typeToSource.put(astTypes[i].name(), entry);
            types.add(astTypes[i]);
            log.debug("      Include '" + astTypes[i].name() + "' from: " + sourceKit.getName() + "|" + entry.getName());
          }
        }
      }
      
      if (!hasSource)
        err("<include> error: Kit '" + include.sourceKit.name() + "' does not include source files.");
      
      // Accumulate errors for any types that were not resolved.
      Iterator iter = include.typeToSource.keySet().iterator();
      while (iter.hasNext())
      {
        String key = (String)iter.next();
        if (include.typeToSource.get(key) == null)
          err("<include> error: type '" + key + "' was not defined in any source files from kit '" + include.sourceKit.file + "'");
      }
      
      zip.close();
    }
    catch (CompilerException e)
    {
      // Fail immediately since we shouldn't have compile exceptions from
      // kit source files.
      throw err("Failed to compile source from kit", zipLoc, e);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw err("Cannot import types from kit file", new Location(sourceKit), e);
    }
  }
}
