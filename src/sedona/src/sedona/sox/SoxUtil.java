//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   18 Sep 06  Brian Frank  Creation
//

package sedona.sox;

import java.util.ArrayList;

import sedona.*;

public class SoxUtil
{
////////////////////////////////////////////////////////////////
// Constructor
////////////////////////////////////////////////////////////////

  public SoxUtil(Schema s, VersionInfo v)
  {
    schema = s;
    version = v;
  }

  SoxUtil(SoxClient c)
  {
    client = c;
  }
  
////////////////////////////////////////////////////////////////
// Schema
////////////////////////////////////////////////////////////////

  public Schema getSchema() { return schema; }
  public void setSchema(Schema s) { schema = s; }
  public VersionInfo getVersion() { return version; }
  public void setVersion(VersionInfo v) { version = v; }


//////////////////////////////////////////////////////////////////////////
// Apply
//////////////////////////////////////////////////////////////////////////

  /**
   * Given a component read or event message then apply the
   * changes to our SoxComponent cache.
   */
  public SoxComponent apply(Msg msg, int compId, int what, SoxComponent c)
    throws Exception
  {
    if (c == null)
    {
      // if we have a 't' message, then we can create the component,
      // otherwise we ignore
      if (what == 't') return applyTreeNew(msg, compId);
      return null;
    }

    // handle apply for existing component
    switch (what)
    {
      case 't': applyTreeCached(msg, c); break;
      case 'c': applyProps(msg, c, 'c', false); break;
      case 'r': applyProps(msg, c, 'r', false); break;
      case 'C': applyProps(msg, c, 'c', true); break;
      case 'R': applyProps(msg, c, 'r', true); break;
      case 'l': applyLinks(msg, c); break;
      default:  System.out.println("WARNING: apply of unknown category: " + (char)what + " 0x" + Integer.toHexString(what));
    }
    return c;
  }

  /**
   * Apply a tree message for a new component.
   */
  SoxComponent applyTreeNew(Msg msg, int compId)
    throws Exception
  {
    Kit kit = schema.kit(msg.u1());
    Type type = kit.type(msg.u1());
    SoxComponent c = new SoxComponent(client, compId, type);
    applyTree(msg, c);
    return c;
  }

  /**
   * Apply a tree message for an existing cached component.
   */
  void applyTreeCached(Msg msg, SoxComponent c)
    throws Exception
  {
    Kit kit = schema.kit(msg.u1());
    Type type = kit.type(msg.u1());
    if (c.type != type)
    {
      // TODO
      System.out.println("ERROR: Component has changed type!");
      Thread.dumpStack();
    }
    applyTree(msg, c);
  }

  /**
   * Common code for applyTreeNew() and applyTreeCached().
   */
  void applyTree(Msg msg, SoxComponent c)
    throws Exception
  {
    final String name = msg.str();
    c.name = name;
    c.parent = msg.u2();
    c.permissions = msg.u1();
    int[] children = new int[msg.u1()];
    for (int i=0; i<children.length; ++i)
      children[i] = msg.u2();
    c.setChildren(children);
    c.fireChanged(SoxComponent.TREE);
  }

  /**
   * Apply a config/runtime props message for an existing component.
   */
  void applyProps(Msg msg, SoxComponent c, int what, boolean operatorOnly)
    throws Exception
  {
    Slot[] slots = c.type.slots;
    for (int i=0; i<slots.length; ++i)
    {
      Slot slot = slots[i];
      if (!slot.isProp()) continue;
      if (operatorOnly && !slot.isOperator()) continue;
      if (what == 'c' ? slot.isRuntime() : slot.isConfig()) continue;
      Value val = c.get(slot).decodeBinary(msg);
      c.set(slot, val);
    }

    c.fireChanged(what == 'c' ? SoxComponent.CONFIG : SoxComponent.RUNTIME);
  }

  /**
   * Apply a links message for an existing component.
   */
  void applyLinks(Msg msg, SoxComponent c)
    throws Exception
  {
    ArrayList acc = null;
    while (true)
    {
      int fromCompId = msg.u2();
      if (fromCompId == 0xffff | fromCompId < 0) break;

      Link link = new Link();
      link.fromCompId = fromCompId;
      link.fromSlotId = msg.u1();
      link.toCompId   = msg.u2();
      link.toSlotId   = msg.u1();

      if (acc == null) acc = new ArrayList();
      acc.add(link);
    }

    if (acc == null)
      c.links = Link.none;
    else
      c.links = (Link[])acc.toArray(new Link[acc.size()]);

    c.fireChanged(SoxComponent.LINKS);
  }

  
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  private SoxClient client;
  Schema schema;
  VersionInfo version;


}
