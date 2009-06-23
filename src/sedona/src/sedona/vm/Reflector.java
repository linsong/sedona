//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Oct 08  Brian Frank  Creation
//

package sedona.vm;

import java.lang.Byte;
import java.lang.Float;
import java.lang.Double;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import sedona.*;
import sedona.Type;
import sedona.kit.*;
import sedona.util.*;

/**
 * Reflector is responsible for managing Sedona side reflection.
 */
public class Reflector     
  extends ReflectUtil
{                       

  Reflector(ClassLoader loader, Schema schema, Context cx)
    throws Exception
  { 
    // set final fields    
    this.loader = loader;
    this.schema = schema;                                
    this.context = cx;
    
    // lookup common classes
    this.kitCls  = loader.loadClass("sedona.vm.sys.Kit");
    this.typeCls = loader.loadClass("sedona.vm.sys.Type");
    this.slotCls = loader.loadClass("sedona.vm.sys.Slot");
    this.sysCls  = loader.loadClass("sedona.vm.sys.Sys");
    this.logCls  = loader.loadClass("sedona.vm.sys.Log");
    this.bufCls  = loader.loadClass("sedona.vm.sys.Buf");
    
    // initialize Sys.context field
    initContext();
    
    // initialize the KitConst classes for each kit  
    initKitConsts();        
  } 

  void initContext()
    throws Exception
  {               
    set(sysCls, "context", context);  
  }           
  
  void initKitConsts()
    throws Exception
  {                            
    long t1 = System.currentTimeMillis();  
    
    // allocate Sys.kits
    Kit[] kits = schema.kits;
    Object[] rkits = (Object[])Array.newInstance(kitCls, schema.kits.length);
    set(sysCls, "kitsLen", new Integer(rkits.length));
    set(sysCls, "kits", rkits);
      
    // first phase is to initialize each KitConst
    // with its kit, type, and slot instances
    for (int i=0; i<kits.length; ++i)
      initKitConst(kits[i]);
    
    // second phase is to resolve base types and base type slots
    for (int i=0; i<kits.length; ++i)
      finishKitConst(kits[i]);        
    
    // third phase is to initialize the logs
    initLogs();
    
    // lastly we need to initialize the type field of
    // components which were statically allocated and 
    // weren't ready for bootstrap
    finishCompTypes();
      
    long t2 = System.currentTimeMillis();  
    System.out.println("initKitConsts (" + (t2-t1) + "ms)");
  }  
  
  void initKitConst(Kit kit)               
    throws Exception
  { 
    long t1 = System.currentTimeMillis();         
    
    // map to KitConst class                      
    Class cls = toKitConstCls(kit);
    
    // allocate sys::Kit
    Object rkit = kitCls.newInstance();
    set(cls, "kit", rkit); 
    
    // init index in Sys.kits
    ((Object[])get(sysCls, "kits"))[kit.id] = rkit;
    
    // allocate kit types array
    Object[] rtypes = (Object[])Array.newInstance(typeCls, kit.types.length);
       
    // init sys::Kit
    set(rkit, "id", new Byte((byte)kit.id));
    set(rkit, "_name", kit.name);
    set(rkit, "name", StrRef.make(kit.name));
    set(rkit, "version", StrRef.make(kit.manifest.version.toString()));
    set(rkit, "checksum", new Integer(kit.checksum));
    set(rkit, "typesLen", new Byte((byte)kit.types.length));
    set(rkit, "types", rtypes);    
    
    // init types
    for (int i=0; i<kit.types.length; ++i)
    {                
      // alloc type and store in kit.types
      Object rtype = rtypes[i] = typeCls.newInstance();        
      Type type = kit.types[i];  
      
      // alloc slots array                   
      Slot[] slots = type.slots;
      Object[] rslots = (Object[])Array.newInstance(slotCls, slots.length);
      
      // set KitConst.typeXXX field
      set(cls, "type" + type.name, rtype);
      
      // init sys::Type fields
      set(rtype, "id", new Byte((byte)type.id));
      set(rtype, "kit",  rkit);
      set(rtype, "_name", type.name);
      set(rtype, "name", StrRef.make(type.name));
      set(rtype, "slotsLen", new Byte((byte)rslots.length));
      set(rtype, "slots", rslots);
      
      // init slots
      for (int j=0; j<slots.length; ++j)
      {              
        // if inherited, then wait for finishKitConst
        Slot slot = slots[j];                            
        if (slot.parent != type) continue;
        
        // alloc slot and store in types.slot
        Object rslot = rslots[j] = slotCls.newInstance();

        // set KitConst.slotXXX field
        set(cls, "slot" + slot.parent.name + "_" + slot.name, rslot);
        
        // create instance of SlotAccessor for this slot                                       
        String accClassName = "Acc_" + slot.parent.name + "_" + slot.name;
        Object acc = typeClass(kit.name, accClassName).newInstance();
        set(acc, "slot", rslot);
        
        // init sys::Slot fields
        set(rslot, "id", new Byte((byte)slot.id));
        set(rslot, "_name", slot.name);
        set(rslot, "name", StrRef.make(slot.name));
        set(rslot, "flags", new Byte((byte)slot.flags));
        set(rslot, "type", rtype(slot.type));    
        set(rslot, "accessor", acc);
      }
    }                                             
    
    long t2 = System.currentTimeMillis();         
    //System.out.println("initKitConst (" + kit.name + " " + (t2-t1) + "ms)");
  }                                 

  void finishKitConst(Kit kit)               
    throws Exception
  {                           
    long t1 = System.currentTimeMillis();         
    
    // map to KitConst class                      
    Class cls = toKitConstCls(kit);
    
    // walk types
    for (int i=0; i<kit.types.length; ++i)
    {                      
      // get sys::Type from KitConst.typeXXX field 
      Type type = kit.types[i];
      Object rtype = get(cls, "type" + type.name);
      
      // resolve sys::Type.base (all type fields should be set now)
      set(rtype, "base", rtype(type.base));
      
      // init parent slots (all declared slots should be set now)
      Object[] rslots = (Object[])get(rtype, "slots");
      for (int j=0; j<type.slots.length; ++j)
      {              
        // if slot is inherited, then use parent's definition
        Slot slot = type.slots[j];                            
        if (slot.parent != type)
          rslots[j] = ((Object[])get(rtype(slot.parent), "slots"))[j];
      }
    }      

    long t2 = System.currentTimeMillis();         
    //System.out.println("finishKitConst (" + kit.name + " " + (t2-t1) + "ms)");
  }                

  void initLogs()
    throws Exception
  {                 
    ArrayList acc = new ArrayList(); 

    // walk each of the KitConst classes looking for Log fields
    for (int i=0; i<schema.kits.length; ++i)
    {                                    
      Kit kit = schema.kits[i];     
      Class cls = toKitConstCls(kit);
      Field[] fields = cls.getFields();
      for (int j=0; j<fields.length; ++j)
      {                                        
        Field f = fields[j];
        if (!f.getName().startsWith("log_")) continue;
        String qname = logFieldNameToQname(kit, f.getName());
        
        // parse                  
        Object log = f.get(null);                            
        set(log, "id", new java.lang.Short((short)acc.size()));
        set(log, "qname", VmUtil.strConst(qname));
        acc.add(log);
      }
    }                      
    
    // set Sys.logs, logsLen, logLevels
    int len = acc.size();
    Object[] logs = (Object[])Array.newInstance(logCls, len);
    acc.toArray(logs);
    set(sysCls, "logs", logs);
    set(sysCls, "logsLen", new java.lang.Short((short)len));
    set(sysCls, "logLevels", new byte[len]);
  }          
  
  static String logFieldNameToQname(Kit kit, String n)
  {     
    // formatted as "log_type_slot"
    n = n.substring(4);
    int slash = n.indexOf('_');
    String typeName = n.substring(0, slash);
    String slotName = n.substring(slash+1);
    
    // assume kit::type
    String qname = kit.name + "::" + typeName;
    
    // if slotName is "log" we use type qname
    if (slotName.equals("log")) return qname;
    
    // if ends in Log strip it
    if (slotName.endsWith("Log"))
      slotName = slotName.substring(0, slotName.length()-3);
    
    // use full slot qname
    return qname + "." + slotName;
  }      

  void finishCompTypes()               
    throws Exception
  {
    // static App Sys.app
    Object app = get(sysCls, "app");                    
    set(app, "type", rtype("sys", "App"));
  }       

////////////////////////////////////////////////////////////////
// Reflection Utils
////////////////////////////////////////////////////////////////

  String[] kitTests(Kit kit)
    throws Exception
  {
    return (String[])get(toKitConstCls(kit), "tests");
  }
  
  Class toKitConstCls(Kit kit)
    throws Exception
  {
    return loader.loadClass("sedona.vm." + kit.name + ".KitConst");  
  }
  
  Kit classToKit(Class cls)
  {
    String name = cls.getName();
    int slash2 = name.lastIndexOf('.');
    int slash1 = name.lastIndexOf('.', slash2-1);
    return schema.kit(name.substring(slash1+1, slash2));
  }                      
  
  Object rtype(Type t)                                                 
    throws Exception
  {
    if (t == null) return null;             
    return rtype(t.kit.name, t.name);                           
  }

  Object rtype(String qname)                                                 
    throws Exception
  {
    int colon = qname.indexOf(':');
    String kit  = qname.substring(0, colon);
    String type = qname.substring(colon+2);    
    return rtype(kit, type);                           
  }
  
  Object rtype(String kitName, String typeName)                                                 
    throws Exception
  {
    Class kc = loader.loadClass("sedona.vm." + kitName + ".KitConst");
    Object rtype = get(kc, "type" + typeName);
    if (rtype == null)
      System.out.println("WARNING: static init ordering problem rtype(" + kitName + "::" + typeName + ")");
    return rtype;
  }
  
  Class typeClass(String qname)                                                 
    throws Exception
  {
    int colon = qname.indexOf(':');
    String kit  = qname.substring(0, colon);
    String type = qname.substring(colon+2);    
    return typeClass(kit, type);
  }

  Class typeClass(String kitName, String typeName)                                                 
    throws Exception
  {
    return loader.loadClass("sedona.vm." + kitName + "." + typeName);
  }
    
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public final ClassLoader loader;
  public final Schema schema;  
  public final Context context;
  public final Class kitCls;
  public final Class typeCls;
  public final Class slotCls;   
  public final Class sysCls;   
  public final Class logCls;   
  public final Class bufCls;   
}

