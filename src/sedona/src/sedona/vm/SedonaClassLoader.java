//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   21 Oct 08  Brian Frank  Creation
//

package sedona.vm;

import java.io.*;
import java.util.zip.*;
import java.util.*;
import sedona.*;
import sedona.kit.*;

/**
 * SedonaClassLoader is used to load bytecode from kit files.
 */
public class SedonaClassLoader
  extends ClassLoader
{         
   
////////////////////////////////////////////////////////////////
// Construction
////////////////////////////////////////////////////////////////

  public SedonaClassLoader(ClassLoader parent, Schema schema, Context cx)
    throws Exception
  {
    super(parent);
    if (cx == null) cx = new Context();
    this.schema = schema;
    this.kits = resolveKits();
    this.reflector = new Reflector(this, schema, cx);     
  }                  

  /**
   * Use the schema's class loader as the parent class loader.
   * <p>
   * {@code this(schema.getClass().getClassLoader(), schema, cx)}
   */
  public SedonaClassLoader(Schema schema, Context cx)
    throws Exception
  {
    this(schema.getClass().getClassLoader(), schema, cx);
  }

  /**
   * {@code this(schema, new Contest()) }
   */
  public SedonaClassLoader(Schema schema)
    throws Exception
  {                                       
    this(schema, new Context());
  }                          
    
////////////////////////////////////////////////////////////////
// Resolve Kits
////////////////////////////////////////////////////////////////
  
  /**
   * Given a schema of kit checksums resolve them to the 
   * actual kit files we'll be using to load classes.  Return
   * a map of String kitname to ZipFiles.
   */
  private HashMap resolveKits()   
    throws Exception
  {
    HashMap kits = new HashMap();
    for (int i=0; i<schema.kits.length; ++i)   
    {
      Kit kit = schema.kits[i];
      kits.put(kit.name, resolveKit(kit));
    }
    return kits;
  }
  
  /**
   * Given a kit name/checksum resolve to the zip file we'll be
   * using to 
   */
  private ZipFile resolveKit(Kit kit)
    throws Exception
  {        
    Depend depend = Depend.makeChecksum(kit.name, kit.checksum);
    KitFile kitFile = KitDb.matchBest(depend);
    if (kitFile == null)
      throw new Exception("Kit found found in local kit database: " + depend);
    return new ZipFile(kitFile.file);
  }                       
  
////////////////////////////////////////////////////////////////
// ClassLoader
////////////////////////////////////////////////////////////////
  
  public Class findClass(String name) 
    throws ClassNotFoundException
  {  
    // we expect all sedona classes to 
    // be under the "sedona.vm" package
    final String kitName = parseKitName(name);
    final String typeName = parseTypeName(name);
    if (kitName != null && typeName != null)
    {
      byte[] cf = findSedonaClass(kitName, typeName);
      if (cf != null)
        return defineClass(name, cf, 0, cf.length);
    }
    throw new ClassNotFoundException(name);
  }
  
  /**
   * Get the kit name from the fully qualified class.
   * 
   * @param name the package qualified type name of the class.  Should be of
   * form {@code sedona.vm.<kitName>.<typeName>}
   * 
   * @return the type name of the type, or {@code null} if the name is
   * not a sedona type name.
   */
  protected String parseKitName(final String name)
  {
    if (name.startsWith("sedona.vm."))
    {
      String qname = name.substring("sedona.vm.".length());
      int dot = qname.indexOf('.');
      return (dot >= 0) ? qname.substring(0, dot) : null;
    }
    return null;
  }
  
  /**
   * Get the type name from the fully qualified class.
   * 
   * @param name the package qualified type name of the class.  Should be of
   * form {@code sedona.vm.<kitName>.<typeName>}
   * 
   * @return the type name of the type, or {@code null} if the name is
   * not a sedona type name.
   */
  protected String parseTypeName(final String name)
  {
    if (name.startsWith("sedona.vm."))
    {
      String qname = name.substring("sedona.vm.".length());
      int dot = qname.indexOf('.');
      return (dot >= 0) ? qname.substring(dot+1) : null;
    }
    return null;
  }

  private byte[] findSedonaClass(String kitName, String typeName)
  {
    ZipFile kit = (ZipFile)kits.get(kitName);
    if (kit == null) return null;            

    String path = "sedona/vm/" + kitName + "/" + typeName + ".class";
    ZipEntry entry = kit.getEntry(path);
    if (entry == null) return null;
    
    try
    {    
      byte[] cf = new byte[(int)entry.getSize()];
      DataInputStream in = new DataInputStream(kit.getInputStream(entry));
      in.readFully(cf);
      in.close();       

      // if (true) dump(typeName, cf);
      return cf;
    }
    catch (IOException e)
    {
      System.out.println("ERROR: Cannot read classfile from zip: " + path);
      return null;
    }    
  }                   
  
  private void dump(String name, byte[] cf)
  {
    try
    {                    
      File f = new File("javap" + File.separator + name + ".class");          
      System.out.println(":::: Dump " + f + "...");
      FileOutputStream out = new FileOutputStream(f);
      out.write(cf);
      out.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////
 
  public final Schema schema;       // schema we are loading for
  public final Reflector reflector; // reflection/initialization support
  final HashMap kits;               // String -> ZipFile
  
    
}

