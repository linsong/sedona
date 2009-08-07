//
// Copyright (c) 1997 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   2 Oct 97  Brian Frank  Creation
//
package sedonac.jasm;

/**
 * IntHashMap is an optimized hashtable for hashing objects 
 * by an integer keys.  It removes the need to use wrapper 
 * Integers as with the standard collection classes.  
 */
public class IntHashMap
{

  /**
   * Default constructor.
   */
  public IntHashMap()
  {
    this(31, 0.75f);
  }

  /**
   * Constructor with initial capacity.
   */
  public IntHashMap(int initialCapacity)
  {
    this(initialCapacity, 0.75f);
  }

  /**
   * Constructor with capacity and load factor.
   */
  public IntHashMap(int initialCapacity, float loadFactor)
  {
    if (initialCapacity <= 0 || loadFactor <= 0.0) 
      throw new IllegalArgumentException();

     this.loadFactor = loadFactor;
     this.table = new Entry[initialCapacity];
     this.threshold = (int)(initialCapacity * loadFactor);
  }

  /**
   * @return the count of elements in the table.
   */
  public int size()
  {
    return count;
  }

  /**
   * @return if the table is empty.
   */
  public boolean isEmpty() 
  {
    return (count == 0);
  }

  /**
   * @return an iterator of the values for this table.
   */
  public Iterator iterator()
  {
    return new Iterator();
  }

  /**
   * Get the object identified by the given int key.
   * 
   * @return null if not in table.
   */
  public Object get(int key) 
  {
    Entry tab[] = table;
    int index = (key & 0x7FFFFFFF) % tab.length;

    for (Entry e = tab[index] ; e != null ; e = e.next) 
    {
      if (e.hash == key) 
        return e.value;
    }

    return null;
  }

  /**
   * Rehash contents of this table into a table
   * with a larger capacity.
   */
  private void rehash() 
  {
    int oldCapacity = table.length;
    Entry oldTable[] = table;

    int newCapacity = oldCapacity * 2 + 1;
    Entry newTable[] = new Entry[newCapacity];

    threshold = (int)(newCapacity * loadFactor);
    table = newTable;

    for (int i = oldCapacity ; i-- > 0 ;) 
    {
      for (Entry old = oldTable[i] ; old != null ; ) 
      {
        Entry e = old;
        old = old.next;

        int index = (e.hash & 0x7FFFFFFF) % newCapacity;
        e.next = newTable[index];
        newTable[index] = e;
      }
    }
  }

  /**
   * Put the given object into the table keyed on the int.
   * 
   * @return the previous value at this key, or null 
   *  if it did not have one.
   */
  public Object put(int key, Object value) 
  {
    if (value == null) 
      throw new NullPointerException();

    // Insure the key is not already in the hashtable
    Entry tab[] = table;
    int index = (key & 0x7FFFFFFF) % tab.length;
    for (Entry e = tab[index] ; e != null ; e = e.next) 
    {
      if (e.hash == key)
      {
        Object old = e.value;
        e.value = value;
        return old;
      }
    }

    // Insure capacity
    if (count >= threshold) 
    {
      rehash();
      return put(key, value);
    } 

    // Create the new entry
    Entry e = new Entry();
    e.hash  = key;
    e.value = value;
    e.next  = tab[index];
    tab[index] = e;
    count++;
    return null;
  }

  /**
   * Remove the vaulue identified by the key.
   *
   * @return the old object at the key, or null if
   *    there was no previous object for the key.
   */
  public Object remove(int key) 
  {
    Entry tab[] = table;
    int index = (key & 0x7FFFFFFF) % tab.length;

    for (Entry e = tab[index], prev=null; e != null ; prev=e, e=e.next) 
    {
      if (e.hash == key) 
      {
        if (prev != null) 
          prev.next = e.next;
        else 
          tab[index] = e.next;

        count--;
        return e.value;
      }
    }

    return null;
  }

  /**
   * Clear the hashtable of entries.
   */
  public void clear() 
  {
    Entry tab[] = table;
    for (int index = tab.length; --index >= 0; )
      tab[index] = null;
    count = 0;
  }
  
  /**
   * Return if the specified object another IntHashMap 
   * with the exact same key-value pairs.
   */
  public boolean equals(Object obj)
  {
    if (!(obj instanceof IntHashMap)) return false;
    if (this == obj) return true;
    IntHashMap o = (IntHashMap)obj;
    if (size() != o.size()) return false;
    for (int i=0; i<table.length; ++i)
    { 
      Entry entry = table[i];
      while (entry != null)
      {
        Object tv = entry.value;
        Object ov = o.get(entry.hash);
        if (ov == null || !tv.equals(ov))
          return false;
        entry = entry.next;
      }
    }
    return true;
  }
  
  /**
   * Clone the IntHashMap into a new instance.
   */
  public Object clone()
  {
    IntHashMap c = new IntHashMap(size()*3);
    for (int i=0; i<table.length; ++i)
    { 
      Entry entry = table[i];
      while (entry != null)
      {
        c.put(entry.hash, entry.value);
        entry = entry.next;
      }
    }
    return c;
  }

  /**
   * Get an array containing all values in 
   * hash table.
   */
  public Object[] toArray(Object[] a) 
  {
    int nxtIdx = 0;
    for (int i=0; i<table.length; i++)
    { 
      Entry entry = table[i];
      while ( entry != null && nxtIdx < count )
      {
        a[nxtIdx++] = entry.value;
        entry = entry.next;
      }
    }
    return a;
  }

////////////////////////////////////////////////////////////////
// Enumerator
////////////////////////////////////////////////////////////////

  public class Iterator
    implements java.util.Iterator
  {
  
    public boolean hasNext()
    {
      if (entry != null) return true;
      while(index-- > 0)
      {
        entry = table[index];
        if (entry != null) return true;
      }
      return false;
    }
    
    public int key()
    {              
      return key;
    }

    public Object next() 
    {
      if (entry == null) 
      {
        while ((index-- > 0) && ((entry = table[index]) == null));
      }

      if (entry != null) 
      {
        Entry e = entry;
        entry = e.next;
        key = e.hash;
        return e.value;
      }

      throw new java.util.NoSuchElementException();
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    private int index = table.length;
    private Entry entry;
    private int key;
  }

//////////////////////////////////////////////////////////////
// Entry
//////////////////////////////////////////////////////////////

  static class Entry 
  {
    int    hash;
    Object value;
    Entry  next;
  }

//////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////

  private Entry table[];
  private int count;
  private int threshold;
  private float loadFactor;

}
