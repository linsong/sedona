//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   14 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.util.*;
import sedona.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.ir.*;
import sedonac.namespace.*;
import sedonac.scode.*;

/**
 * FieldLayout walks a set of fields and lays them out in memory
 * by assigning an offset from a base pointer taking into account
 * alignment.
 */
public class FieldLayout
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public FieldLayout(Compiler compiler)
  {
    super(compiler);
    this.refSize = compiler.image == null ? 4 : compiler.image.refSize;
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    log.debug("  FieldLayout");
    findIrTypes();
    layoutInstanceFields();
    layoutStaticFields();
    if (compiler.dumpLayout) dump();
    quitIfErrors();
  }

//////////////////////////////////////////////////////////////////////////
// Find Ir Types
//////////////////////////////////////////////////////////////////////////

  private void findIrTypes()
  {
    this.todo = new HashMap();
    
    // create a todo list which includes all the types
    for (int i=0; i<flat.types.length; ++i)
    {
      IrType type = flat.types[i];
      todo.put(type.qname, type);
    }                            

    // if assembling an AST, then include that too
    if (compiler.ir != null)
    {
      for (int i=0; i<compiler.ir.types.length; ++i)
      {
        IrType type = compiler.ir.types[i];
        todo.put(type.qname, type);
      }
    }

    // flatten todo list    
    this.types = (IrType[])todo.values().toArray(new IrType[todo.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// Instance Fields
//////////////////////////////////////////////////////////////////////////

  private void layoutInstanceFields()
  {
    this.processing = new HashMap();

    // process each type
    for (int i=0; i<types.length; ++i)
      layoutInstanceFields(types[i]);
    quitIfErrors();

    // verify we processed all the types
    for (int i=0; i<types.length; ++i)
      if (types[i].sizeof < 0) throw new IllegalStateException();
  }

  private void layoutInstanceFields(IrType type)
  {
    // check that this type is still in the todo
    // list, otherwise we've already processed it
    String qname = type.qname;
    if (!todo.containsKey(qname)) return;     

    // check if this guy is in the processing queue,
    // in which case we have cyclic inlining
    if (processing.containsKey(qname))
    {
      err("Cyclic inline field in '" + qname + "'", loc);
      return;
    }
    processing.put(qname, type);

    // layout the fields
    loc = new Location(type.qname);
    
    // compute starting offset for instance fields which are always
    // laid out after their base class's fields (or the vtable)
    int off = 0;
    if (type.base != null)
    {
      // check if we need to recursive layout the base class
      if (type.base.sizeof() < 0)
        layoutInstanceFields(TypeUtil.ir(type.base));

      // optimize for virtual which will have sizeof 4,
      // but only needs two bytes to vtable (this optimization
      // for base classes with padding should be more generic)
      if (type.base.isVirtual())
        off += 2;
      else
        off = type.base.sizeof();
    }
    if (off < 0) throw new IllegalStateException(type.qname);

    // layout the fields
    type.sizeof = layoutFields(off, type.instanceFields());

    // now that is has been processed, removed it the
    // todo and processing maps
    processing.remove(qname);
    todo.remove(qname);
  }

//////////////////////////////////////////////////////////////////////////
// Static Fields
//////////////////////////////////////////////////////////////////////////

  private void layoutStaticFields()
  {
    int dataSize = layoutFields(0, flat.staticFields);
    if (dataSize < 0) throw err("Data size could not be computed");
    compiler.dataSize = dataSize;
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  private int layoutFields(int off, IrField[] fields)
  {
    // layout 1, 2, then 4 bytes fields; putting the smaller fields
    // first ensures that maximize access to the first 255 bytes which
    // use the more compact opcodes (we pad everything out to 4 bytes
    // no matter what anyways)

    // one byte fields
    off = layoutFields(fields, off, 1);

    // two byte fields
    while (off % 2 != 0) off++;
    off = layoutFields(fields, off, 2);

    // four byte fields (includes 8 byte fields too)
    while (off % 4 != 0) off++;
    off = layoutFields(fields, off, 4);

    // anything left over is odd sized inlines
    IrField unsizedArray = null;
    for (int i=0; i<fields.length; ++i)
    {
      // skip fields already assigned an offset
      IrField f = fields[i];
      if (fields[i].offset != -1) continue;
      loc = new Location(f.qname);

      // we should never reach this point unless an inline field
      if (!f.isInline()) throw new IllegalStateException(f.qname);

      // if const field, then everything is laid out by
      // the compiler - just mark the field's offset at the
      // end of the fixed sized slots
      if (f.isConst())
      {
        f.offset = off;
        continue;
      }

      // if this an unsized array, then we will lay
      // it out last after this for loop
      if (f.ctorLengthParam >= 0)
      {
        if (unsizedArray != null)
          throw new IllegalStateException("More than one unsized array? " + f.qname);
        unsizedArray = f;
        continue;
      }

      // check if we need to recurse and layout the type
      // of this field before we proceed (for arrays we
      // check the of type)
      Type type = f.type;
      Type recurse = type;
      if (type.isArray() && type.arrayOf().isRef())
      {
        if (f.arrayInit)
          recurse = type.arrayOf();
        else
          recurse = null; // don't need size, just refs
      }

      // if the field type hasn't been laid out yet
      if (recurse != null && recurse.sizeof() < 0)
      {
        // try to lay it out now
        layoutInstanceFields(TypeUtil.ir(recurse));

        // if we detect a cyclic inline then just bail
        if (recurse.sizeof() < 0) return -1;
      }

      // compute size
      int size;
      if (type.isArray() && type.arrayOf().isRef())
      {
        // size to fit n references/pointers
        Type of = type.arrayOf();
        int len = type.arrayLength().val();
        size = len * refSize;

        // if {...} initializer, also size to fit n instances
        if (f.arrayInit) size += len * of.sizeof();
      }

      // handle inline non-array instances
      else
      {
        // size to fit one instance; if ctor args provided then
        // we might need to compute a dynamic size
        if (f.ctorLengthArg != null)
          size = calcDynamicSize(f);
        else
          size = type.sizeof();
      }

      // sanity
      if (size < 0)
        throw new IllegalStateException();

      // everything should be started at 4-byte alignment
      if (off % 4 != 0) throw new IllegalStateException();

      // inline enough memory to whole an instance of the type
      f.offset = off;
      off += size;
      while (off % 4 != 0) off++;
    }

    // if there is an unsized array, it is always
    // laid out last in the memory
    if (unsizedArray != null)
    {
      unsizedArray.offset = off;
    }

    // everything is sized in multiples of 4 for alignment
    if (off % 4 != 0) throw new IllegalStateException();

    // return size of fields
    return off;
  }

  private int layoutFields(IrField[] fields, int off, int sizeof)
  {
    if (off % sizeof != 0) throw new IllegalStateException();
    for (int i=0; i<fields.length; ++i)
    {
      IrField f = fields[i];
      Type type = f.type;
      int offIncr = sizeof;

      boolean sizeMatch;
      if (type.isPrimitive())
      { 
        if (type.sizeof() == 8)
        {
          sizeMatch = sizeof == 4; // align long/double on 4 byte boundaries
          offIncr = 8;
        }
        else
        {
          sizeMatch = (type.sizeof() == sizeof); 
        }
      }
      else if (f.isConst() && !f.isInline())
      {
        // const instance fields are 2 byte block index
        sizeMatch = sizeof == 2;
      }
      else
      {
        sizeMatch = (sizeof == refSize && !f.isInline());
      }

      if (sizeMatch)
      {                        
        f.offset = off;
        off += offIncr;
      }
    }
    return off;
  }

  private int calcDynamicSize(IrField field)
  {
    // this code is run in a scenerio like this:
    //   inline Buf(5) buf

    // get the length argument we are passing to the constructor
    int len;
    if (field.ctorLengthArg instanceof Expr.Literal)
      len = ((Expr.Literal)field.ctorLengthArg).asInt();
    else
      len = ((Expr.Field)field.ctorLengthArg).field.define().asInt();

    // this is the base size of the field's instance
    int baseSize = field.type.sizeof();

    // this is the unsized field within the field's instance
    // we are dynamically sizing via the ctor argument
    Field unsizedField = TypeUtil.getUnsizedArrayField(field.type);
    Type unsizedOf = unsizedField.type().arrayOf();

    // get the width of a single item in that unsized array
    int unsizedOfSize = unsizedOf.isRef() ? refSize : unsizedOf.sizeof();

    // the total instance size if the base size, plus the
    // memory to store len items in the instance's unsized array
    return baseSize + unsizedOfSize*len;
  }

//////////////////////////////////////////////////////////////////////////
// Dump
//////////////////////////////////////////////////////////////////////////

  private void dump()
  {
    System.out.println("==== FieldLayout =====");
    
    IrType[] types = (IrType[])this.types.clone();
    Arrays.sort(types, new Comparator()
    {
      public int compare(Object a, Object b) { return a.toString().compareTo(b.toString()); }
    });
    for (int i=0; i<types.length; ++i)
    {
      IrType type = types[i];
      IrField[] ifields = type.instanceFields();
      if (ifields.length == 0) continue;
      System.out.println("  --- " + type.qname + " [sizeof " + type.sizeof + "] ---");
      dumpFields(ifields);
    }

    System.out.println("  --- static fields [dataSize " + compiler.dataSize + "] ---");
    dumpFields(flat.staticFields);
    System.out.println("  ---------------------");
  }

  private void dumpFields(IrField[] fields)
  {
    Arrays.sort(fields, new Comparator()
    {
      public int compare(Object a, Object b)
      {
        return ((IrField)a).offset > ((IrField)b).offset ? 1 : -1;
      }
    });

    for (int i=0; i<fields.length; ++i)
      System.out.println("    " + TextUtil.pad(fields[i].offset + ": ", 5) + fields[i].qname);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  Location loc;
  IrType[] types;        // list of types to process
  HashMap processing;    // map of qname to typse being processed
  HashMap todo;          // map of qname to types left to process
  int refSize;           // size of pointer in bytes
}
