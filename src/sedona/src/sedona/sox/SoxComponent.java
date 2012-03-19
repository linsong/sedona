//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   21 Jun 07  Brian Frank  Creation
//

package sedona.sox;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import sedona.*;
import sedona.util.ArrayUtil;

/**
 * SoxComponent represents a remote Sedona component
 * being accessed over a SoxClient session.
 */
public class SoxComponent
  extends Component
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  /**
   * Public constructor for testing only - always load from SoxClient.
   */
  public SoxComponent(SoxClient client, int id, Type type)
  {
    super(type);
    this.client = client;
    this.id = id;
  }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /**
   * Get associated SoxClient being used to access
   * this remote representation of a component.
   */
  public SoxClient client()
  {
    return client;
  }

  /**
   * Get the id which identifies this component in its app.
   */
  public int id()
  {
    return id;
  }

  /**
   * Get the component name which is unique with its parent.
   */
  public String name()
  {
    return name;
  }

  public Component getParent() { return parent(); }
  public Component[] getChildren() { return children(); }
  public Component getChild(String name) { return child(name); }

  /**
   * Get the parent id.
   */
  public int parentId()
  {
    return parent;
  }

  /**
   * Get my parent component. If this is the first time the
   * parent has been accessed, then method will block to perform
   * a network call.
   */
  public SoxComponent parent()
  {
    try
    {
      if (id == 0) return null;
      return client.load(parent);
    }
    catch (Exception e)
    {
      throw new SoxException("Cannot read parent", e);
    }
  }

  /**
   * Get the children ids.
   */
  public int[] childrenIds()
  {
    return (int[])children.clone();
  }

  /**
   * Get list of this component's children. If the children
   * have not been loaded yet, then this method will block to
   * perform one or more network calls.
   */
  public SoxComponent[] children()
  {
    try
    {
      // load chlidren
      int[] ids = this.children;
      SoxComponent[] comps = client.load(ids, false);
      
      // check if we had any errors reading the children 
      boolean errors = false;
      for (int i=0; i<comps.length; ++i)
        if (comps[i] == null) { errors = true; break; }
      if (!errors) return comps;

      // if we had errors it means that the children array 
      // on client side doesn't match the reality of the 
      // server, so ignore the children ids in error  
      ArrayList acc = new ArrayList();
      for (int i=0; i<comps.length; ++i) 
        if (comps[i] != null) acc.add(comps[i]);
      comps = (SoxComponent[])acc.toArray(new SoxComponent[acc.size()]);
      
      // save away actual list of ids we are using now
      ids = new int[comps.length];
      for (int i=0; i<comps.length; ++i) ids[i] = comps[i].id;
      this.children = ids;

      return comps;
    }
    catch (Exception e)
    {
      throw new SoxException("Cannot read children", e);
    }
  }

  /**
   * Lookup a child by its simple name.  If the children
   * have not been loaded yet, then this method will block to
   * perform one or more network calls.
   */
  public SoxComponent child(String name)
  {
    // scan linear - might want to stick kids in a
    // hash table, but that is probably overkill
    SoxComponent[] kids = (SoxComponent[])children();
    for (int i=0; i<kids.length; ++i)
      if (name.equals(kids[i].name))
        return kids[i];
    return null;
  }                         
  
  /**
   * Get the list of links into and out of this component.
   */
  public Link[] links()
  {
    return (Link[])links.clone();
  }

////////////////////////////////////////////////////////////////
// Security
////////////////////////////////////////////////////////////////

  /**
   * Return the permissions bitmask which defines what the
   * current client has access to do on this component.  This
   * value is available after tree load, and defaults to zero
   * until then.
   */
  public int permissions()
  {
    return permissions;
  }

////////////////////////////////////////////////////////////////
// Internal Children Id Management
////////////////////////////////////////////////////////////////

  synchronized void setChildren(int[] newChildren)
  {
    HashSet set = new HashSet(newChildren.length);
    for (int i=0; i<newChildren.length; ++i)
      set.add(new Integer(newChildren[i]));

    if (children != null)
    {
      for (int i=0; i<children.length; ++i)
      {
        if (!set.contains(new Integer(children[i])))
        {
          SoxComponent c = client.cache(children[i]);
          if (c != null)
            client.cacheRemove(c);
        }
      }
    }
    this.children = newChildren;
  }

  synchronized void addChild(int child)
  {
    for (int i=0; i<children.length; ++i)
      if (children[i] == child) return;
    children = ArrayUtil.addOne(children, child);                                   
  }

  synchronized void removeChild(int child)
  {
    children = ArrayUtil.removeOne(children, child);
  }

//////////////////////////////////////////////////////////////////////////
// Subscription
//////////////////////////////////////////////////////////////////////////

  /**
   * Get the bitmask of what is subscribed or 0 if not subscribed
   * at all.  If the SoxClient's watch was subscribed with allTreeEvents
   * then the TREE bit is always set.
   */
  public int subscription()
  {
    return (this.subscription) | (client.allTreeEvents ? TREE : 0);
  }

  /**
   * Fire a changed event on the listener.
   */
  public void fireChanged(int mask)
  {                          
    try
    {                 
      if (listener != null)
        listener.changed(this, mask);
    }
    catch (Throwable e)
    {
      e.printStackTrace();
    }
  }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  public void dump()
  {
    dump(new PrintWriter(System.out));
  }

  public void dump(PrintWriter out)
  {
    Slot[] slots = type.slots;
    out.println(type + " " + id + " " + name);
    for (int i=0; i<slots.length; ++i)
    {
      Slot slot = slots[i];
      if (slot.isProp())
        out.println("  " + slot.name + " = " + get(slot));
    }
    out.flush();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public static final int TREE    = 0x01;
  public static final int CONFIG  = 0x02;
  public static final int RUNTIME = 0x04;
  public static final int LINKS   = 0x08;

  public SoxComponentListener listener;
  final SoxClient client;
  final int id;
  String name;
  int parent;
  int subscription;
  Link[] links = Link.none;
  int[] children = new int[0];
  int permissions;

}
