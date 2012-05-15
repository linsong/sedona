//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Mar 07  Brian Frank  Creation
//

package sedonac.gen;

import java.util.*;
import sedona.Buf;
import sedona.manifest.TypeManifest;
import sedona.util.TextUtil;
import sedonac.Compiler;
import sedonac.CompilerException;
import sedonac.CompilerSupport;
import sedonac.Location;
import sedonac.ast.Expr;
import sedonac.ir.IrAddressable;
import sedonac.ir.IrField;
import sedonac.ir.IrKit;
import sedonac.ir.IrMethod;
import sedonac.ir.IrOp;
import sedonac.ir.IrPrimitive;
import sedonac.ir.IrSlot;
import sedonac.ir.IrType;
import sedonac.ir.IrVTable;
import sedonac.namespace.Method;
import sedonac.namespace.PrimitiveType;
import sedonac.namespace.Slot;
import sedonac.namespace.Type;
import sedonac.namespace.TypeUtil;
import sedonac.scode.SCode;
import sedonac.scode.SCodeImage;

/**
 * ImageGen
 */
public class ImageGen
  extends CompilerSupport
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public ImageGen(Compiler compiler)
  {
    super(compiler);
    this.image      = compiler.image;
    this.kits       = compiler.kits;
    this.constPool  = new ConstPool(this);
  }

//////////////////////////////////////////////////////////////////////////
// Assemble
//////////////////////////////////////////////////////////////////////////

  public void generate()
  {
    code.bigEndian = (image.endian == SCode.vmBigEndian);
    code.checkAlignment = true;
    header();
    bootstrap();
    vtables();
    kits();
    types();
    slots();
    logs();
    methods();
    tests();
    constPool.ints();
    constPool.floats();
    constPool.longs();
    constPool.doubles();
    constPool.strings();
    constPool.bufs();
    constPool.arrays();
    constPool.qnameTypes();
    constPool.qnameSlots();
    backpatch();
    finish();
  }

//////////////////////////////////////////////////////////////////////////
// Header
//////////////////////////////////////////////////////////////////////////

  private void header()
  {
    code.i4(SCode.vmMagic);      //  0 magic
    code.u1(SCode.vmMajorVer);   //  4 major version
    code.u1(SCode.vmMinorVer);   //  5 minor version
    code.u1(image.blockSize);    //  6 sedona block size in bytes
    code.u1(image.refSize);      //  7 pointer size in bytes
    code.i4(-1);                 //  8 spacer for image size in bytes
    code.i4(compiler.dataSize);  // 12 data size in bytes
    code.u2(-1);                 // 16 spacer for main method block index
    code.u2(-1);                 // 18 spacer for test table
    code.u2(-1);                 // 20 spacer for kits block index
    code.u1(flat.kits.length);   // 22 num kits
    code.u1(scodeFlags());       // 23 scode flags
    resumeBlockIndex();          // 24 resume method block index
    blockAlign();
  }

  void resumeBlockIndex()
  {
    addBlockIndex(findMain(image.resume).codeAddr);
  }

  int scodeFlags()
  {
    int flags = 0;
    if (image.debug) flags |= SCode.scodeDebug;
    if (image.test)  flags |= SCode.scodeTest;
    return flags;
  }

//////////////////////////////////////////////////////////////////////////
// Bootstrap
//////////////////////////////////////////////////////////////////////////

  private void bootstrap()
  {
    // set main block index to current location
    code.u2(16, blockIndex());

    // 2 params and 0 locals
    code.u1(2);
    code.u1(0);

    // if debug
    IrMethod main = findMain(image.main);
    if (image.debug)
    {
      padArg(2);
      code.u1(SCode.MetaSlot);
      addBlockIndex(qnameSlot(main));
    }

    // call the static initializer of all the types
    for (int i=0; i<flat.staticInits.length; ++i)
    {
      padArg(2);
      code.u1(SCode.Call);
      addBlockIndex(flat.staticInits[i].codeAddr);
    }

    // call main method and return its value
    code.u1(SCode.LoadParam0);
    code.u1(SCode.LoadParam1);
    padArg(2);
    code.u1(SCode.Call);
    addBlockIndex(main.codeAddr);
    code.u1(SCode.ReturnPop);
    blockAlign();
  }

  private IrMethod findMain(String qname)
  {
    IrMethod[] methods = flat.methods;
    IrMethod main = null;

    for (int i=0; i<methods.length; ++i)
      if (methods[i].qname.equals(qname))
        { main = methods[i]; break; }

    if (main == null)
      throw err("Unresolved main method: " + qname);

    if (main.params.length != 2 ||
        !main.params[0].isArray() ||
        !main.params[0].arrayOf().isStr() ||
        !main.params[1].isInt())
      throw err("Invalid signature for main, method must be (Str[], int): " + qname);

    if (!main.ret.isInt())
      throw err("Main method must return int: " + qname);

    if (!main.isStatic())
      throw err("Main method must be static: " + qname);

    if (main.isNative())
      throw err("Main method cannot be static: " + qname);

    return main;
  }

//////////////////////////////////////////////////////////////////////////
// Virtual Tables
//////////////////////////////////////////////////////////////////////////

  private void vtables()
  {
    for (int i=0; i<flat.virtTypes.length; ++i)
      vtable(flat.virtTypes[i].vtable);
  }

  private void vtable(IrVTable vtable)
  {
    // always align vtables types on at least 2 byte
    // boundaries regardless of block size
    code.align(2);
    vtable.blockIndex = blockIndex();

    // write out the vtable (starting at offset 0 for now)
    for (int i=0; i<vtable.methods.length; ++i)
    {
      IrMethod m = vtable.methods[i];
      if (m.isAbstract())
        code.u2(0);
      else
        addBlockIndex(m.codeAddr);
    }

    // align on start of next block
    blockAlign();
  }

//////////////////////////////////////////////////////////////////////////
// Kit Metadata
//////////////////////////////////////////////////////////////////////////

  private void kits()
  {
    // sanity check that I'm using the same memory
    // layout as FieldLayout computed for Type fields
    IrType x = (IrType)ns.resolveType("sys::Kit");
    IrField fid       = (IrField)x.slot("id");
    IrField fname     = (IrField)x.slot("name");
    IrField fversion  = (IrField)x.slot("version");
    IrField fchecksum = (IrField)x.slot("checksum");
    IrField ftypes    = (IrField)x.slot("types");
    IrField ftypesLen = (IrField)x.slot("typesLen");
    if (fid.offset       != 0 ||
        ftypesLen.offset != 1 ||
        fname.offset     != 2 ||
        fversion.offset  != 4 ||
        fchecksum.offset != 8 ||
        ftypes.offset    != 12)
      throw new IllegalStateException("Mismatch kit field layout");

    // sanity check number of kits
    if (flat.kits.length >= 255) throw err("Too many kits");

    // sort kits by name according to standard schema order
    sedona.Schema.sortKits(flat.kits);

    // write out all the kit instances
    for (int i=0; i<flat.kits.length; ++i)
    {
      IrKit k = flat.kits[i];
      k.id = i;
      kit(k);

      // sanity check that kits are in alphabetic order (sys
      // is always zero though) - this guarantees reusable
      // schemas when this list of kits is reused
      if (i == 0 && !flat.kits[0].name.equals("sys"))
        throw err("Sys kit id not 0!");
      if (i >= 2 && flat.kits[i-1].name.compareTo(flat.kits[i].name) >= 0)
        throw err("Kits are not sorted by name: " + flat.kits[i-1].name + " >= " + flat.kits[i].name);
    }

    // write Sys.kits list
    code.align(2);
    IrField sysKits = (IrField)ns.resolveSlot("sys::Sys.kits");
    sysKits.storage.setBlockIndex(blockIndex());
    code.u2(20, blockIndex());  // header to kits
    for (int i=0; i<flat.kits.length; ++i)
      addBlockIndex(flat.kits[i]);
    blockAlign();
  }

  private void kit(IrKit k)
  {
    // always align comp types on at least 4
    // byte boundaries regardless of block size
    code.align(4);
    k.blockIndex = blockIndex();

    // sanity check number of types
    boolean isSys = k.name.equals("sys");
    IrType[] types = k.reflectiveTypes();
    int typesLen = types.length;

    if (isSys)
      typesLen += flat.primitives.length;
    if (typesLen >= 255) throw err("Too many types in kit '" + k.name + "'");

    // create a map of qname -> type manifest
    HashMap byQname = new HashMap();
    for (int i=0; i<k.manifest.types.length; ++i)
    {
      TypeManifest mf = k.manifest.types[i];
      byQname.put(mf.qname, mf);
    }

    // Assign IrType ids based on TypeManifest ids.
    for (int i=0; i<types.length; ++i)
    {
      IrType ir = types[i];
      TypeManifest mf = (TypeManifest)byQname.get(ir.qname);
      if (mf == null)
        throw err("IrType doesn't have manifest entry: " + ir.qname);
      ir.id = mf.id;
    }

    // Now sort the types based on id
    Arrays.sort(types, new Comparator()
    {
      public int compare(Object o1, Object o2)
      {
        return ((IrType)o1).id - ((IrType)o2).id;
      }
    });

    // write instance fields
    // NOTE: must keep this in sync with SCodeParser
    code.u1(k.id);
    code.u1(typesLen);
    addBlockIndex(string(k.name));
    addBlockIndex(string(k.version.toString()));
    code.u2(0); // padding
    code.i4(k.manifest.checksum);

    // write types array; if sys we have to write predefined first
    int id = 0;
    if (isSys)
    {
      for (int i=0; i<flat.primitives.length; ++i)
        addBlockIndex(flat.primitives[i]);
      id = flat.primitives.length;
    }
    for (int i=0; i<types.length; ++i)
    {
      IrType t = types[i];
      if (t == null || t.id < 0) throw err("Type id not mapped correct: " + i);
      addBlockIndex(t);
    }

    // align on start of next block
    blockAlign();
  }

//////////////////////////////////////////////////////////////////////////
// Type Metadata
//////////////////////////////////////////////////////////////////////////

  private void types()
  {
    // sanity check that I'm using the same memory
    // layout as FieldLayout computed for Type fields
    IrType x = (IrType)ns.resolveType("sys::Type");
    IrField fid       = (IrField)x.slot("id");
    IrField fname     = (IrField)x.slot("name");
    IrField fkit      = (IrField)x.slot("kit");
    IrField fbase     = (IrField)x.slot("base");
    IrField fsizeof   = (IrField)x.slot("sizeof");
    IrField finit     = (IrField)x.slot("instanceInitMethod");
    IrField fslots    = (IrField)x.slot("slots");
    IrField fslotsLen = (IrField)x.slot("slotsLen");
    if (fid.offset        != 0  ||
        fslotsLen.offset  != 1  ||
        fname.offset      != 2  ||
        fkit.offset       != 4  ||
        fbase.offset      != 6  ||
        fsizeof.offset    != 8  ||
        finit.offset      != 10 ||
        fslots.offset     != 12)
      throw new IllegalStateException("Mismatch type field layout");

    // write out the primitive types instances
    for (int i=0; i<flat.primitives.length; ++i)
      type(flat.primitives[i]);

    // write out all the type instances
    for (int i=0; i<flat.reflectiveTypes.length; ++i)
      type(flat.reflectiveTypes[i]);
  }

  private void type(IrType t)
  {
    // always align comp types on at least 2 byte
    // boundaries regardless of block size
    code.align(2);
    t.blockIndex = blockIndex();

    // sanity check number of slots
    IrSlot[] slots = t.reflectiveSlots;
    if (slots == null) slots = new IrSlot[0];
    if (slots.length >= 255) throw err("Too many slots in type '" + t.qname + "'");

    // write instance fields
    code.u1(t.id);
    code.u1(slots.length);
    addBlockIndex(string(t.name));
    addBlockIndex(t.kit);
    if (!t.base.isaComponent())
      code.u2(0);
    else
      addBlockIndex((IrType)t.base);
    code.u2(t.sizeof);
    addBlockIndex(((IrMethod)t.slot(Method.INSTANCE_INIT)).codeAddr);
    for (int i=0; i<slots.length; ++i)
    {
      IrSlot slot = t.reflectiveSlots[i];
      if (slot.id != i) throw new IllegalStateException("slot id mismatch " + slot);
      addBlockIndex(slots[i].reflect);
    }

    // align on start of next block
    blockAlign();
  }

  private void type(IrPrimitive primitive)
  {
    code.align(2);
    primitive.blockIndex = blockIndex();

    // write instance fields
    code.u1(primitive.type.id);
    code.u1(0);
    addBlockIndex(string(primitive.type.name));
    addBlockIndex((IrKit)ns.resolveKit("sys"));

    blockAlign();
  }

//////////////////////////////////////////////////////////////////////////
// Reflective Slots
//////////////////////////////////////////////////////////////////////////

  private void slots()
  {
    // sanity check that I'm using the same memory
    // layout as FieldLayout computed for Type fields
    IrType x = (IrType)ns.resolveType("sys::Slot");
    IrField fid       = (IrField)x.slot("id");
    IrField fname     = (IrField)x.slot("name");
    IrField fflags    = (IrField)x.slot("flags");
    IrField ftype     = (IrField)x.slot("type");
    IrField fhandle   = (IrField)x.slot("handle");
    if (fid.offset     != 0 ||
        fflags.offset  != 1 ||
        fname.offset   != 2 ||
        ftype.offset   != 4 ||
        fhandle.offset != 6)
      throw new IllegalStateException("Mismatch slot field layout");

    // write out each slot's reflection meta-data (this is
    // different than a const static's storage location or
    // a method's code implementation)
    for (int i=0; i<flat.reflectiveSlots.length; ++i)
      slot(flat.reflectiveSlots[i]);
  }

  private void slot(IrSlot slot)
  {
    code.align(2);
    slot.reflect.setBlockIndex(blockIndex());

    code.u1(slot.id);
    code.u1(slot.rtFlags);
    addBlockIndex(string(slot.name));
    if (slot instanceof IrField)
    {
      IrField f = (IrField)slot;
      addTypeBlockIndex(f.type);
      code.u2(f.offset);
    }
    else
    {
      IrMethod m  = (IrMethod)slot;
      if (m.params.length == 0)
        addTypeBlockIndex(ns.voidType);
      else if (m.params.length == 1)
        addTypeBlockIndex(m.params[0]);
      else
        throw err("Invalid number of params for action: ", m.qname());
      // all actions are by definition virtual
      code.u2(m.vindex, false);
    }

    blockAlign();
  }

//////////////////////////////////////////////////////////////////////////
// Log Metadata
//////////////////////////////////////////////////////////////////////////

  private void logs()
  {
    // sanity check that I'm using the same memory
    // layout as FieldLayout computed for Type fields
    IrType x = (IrType)ns.resolveType("sys::Log");
    IrField fid    = (IrField)x.slot("id");
    IrField fqname = (IrField)x.slot("qname");
    if (fid.offset    != 0 ||
        fqname.offset != 2)
      throw new IllegalStateException("Mismatch log field layout");

    // get the complete list of logs
    IrField[] logs = flat.logDefines;

    // sanity check number of kits
    if (logs.length >= 0x7fff) throw err("Too many logs");

    // sort by qname
    Arrays.sort(logs, new Comparator()
    {
      public int compare(Object a, Object b)
        { return ((IrField)a).qname.compareTo(((IrField)b).qname); }
    });

    // write out all the log instances
    for (int i=0; i<logs.length; ++i)
    {
      IrField f = logs[i];
      f.id = i;
      log(f);
    }

    // write Sys.logs list
    code.align(2);
    IrField sysLogs = (IrField)ns.resolveSlot("sys::Sys.logs");
    sysLogs.storage.setBlockIndex(blockIndex());
    for (int i=0; i<logs.length; ++i)
      addBlockIndex(logs[i].storage);
    blockAlign();
  }

  private void log(IrField f)
  {
    // always align logs types on 2 byte
    // boundaries regardless of block size
    code.align(2);
    f.storage.setBlockIndex(blockIndex());

    // write instance fields
    code.u2(f.id);
    addBlockIndex(string(TypeUtil.toLogName(f)));

    // align on start of next block
    blockAlign();
  }

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  private void methods()
  {
    for (int i=0; i<flat.methods.length; ++i)
      method(flat.methods[i]);
  }

  private void method(IrMethod m)
  {
    if (m.isAbstract() || m.isNative()) return;

    int numParams = m.numParams();

    loc = new Location(m.qname);
    m.codeAddr.setBlockIndex(blockIndex());
//if (m.qname.startsWith("sys::") && !m.qname.endsWith("Test"))
//System.out.println(" -- " + m.qname + " [" + m.codeAddr.getBlockIndex() + " 0x" + Integer.toHexString(m.codeAddr.getBlockIndex()) + "] -> " + m.codeAddr);
    code.u1(numParams);
    code.u1(m.maxLocals);
    if (image.debug)
    {
      padArg(2);
      code.u1(SCode.MetaSlot);
      addBlockIndex(qnameSlot(m));
    }
    if (m.code != null) ops(m.code);
    blockAlign();
    loc = new Location("ImageGen");
  }

  private void ops(IrOp[] ops)
  {
    // keep redoing the generation until we get
    // all our farjmps right (typically this should
    // be no more than twice unless we hit a really
    // weird boundary condition)
    int start = code.pos;
    Backpatch origBackpatch = backpatch;
    while (true)
    {
      // write all the ops
      for (int i=0; i<ops.length; ++i)
        op(ops[i]);

      // map jump indices to offsets and check if we
      // we need to redo the loop with far jumps
      boolean redo = false;
      for (int i=0; i<ops.length; ++i)
        if (ops[i].isJump())
          redo |= patchJump(ops, ops[i]);

      // either done or start over
      if (!redo) break;
      code.pos = code.size = start;
      backpatch = origBackpatch;
    }

    // backpatch switches
    for (int i=0; i<ops.length; ++i)
      if (ops[i].opcode == SCode.Switch)
        patchSwitch(ops, ops[i]);
  }

  private boolean patchJump(IrOp[] ops, IrOp jump)
  {
    // get target operation
    IrOp target = jumpTarget(ops, jump.argToInt());

    // calculate jump offset (based on jump opcode pos)
    int off = jumpOffset(jump, target);

    // if already a far jump, then patch it and we are done
    if (jump.argType() == SCode.jmpfarArg)
    {
      code.s2(jump.pos+1, off);
      return false;
    }

    // check if this needs to become a far jump
    if (off < Byte.MIN_VALUE || off > Byte.MAX_VALUE)
    {
      switch (jump.opcode)
      {
        case SCode.Jump:         jump.opcode = SCode.JumpFar;         break;
        case SCode.JumpNonZero:  jump.opcode = SCode.JumpFarNonZero;  break;
        case SCode.JumpZero:     jump.opcode = SCode.JumpFarZero;     break;
        case SCode.Foreach:      jump.opcode = SCode.ForeachFar;      break;
        case SCode.JumpIntEq:    jump.opcode = SCode.JumpFarIntEq;    break;
        case SCode.JumpIntNotEq: jump.opcode = SCode.JumpFarIntNotEq; break;
        case SCode.JumpIntGt:    jump.opcode = SCode.JumpFarIntGt;    break;
        case SCode.JumpIntGtEq:  jump.opcode = SCode.JumpFarIntGtEq;  break;
        case SCode.JumpIntLt:    jump.opcode = SCode.JumpFarIntLt;    break;
        case SCode.JumpIntLtEq:  jump.opcode = SCode.JumpFarIntLtEq;  break;
        default: throw new IllegalStateException("missing jmp to jmpfar " + jump);
      }
      return true;
    }

    // write near jump
    code.s1(jump.pos+1, off);
    return false;
  }

  private void patchSwitch(IrOp[] ops, IrOp switchOp)
  {
    String[] tokens = TextUtil.split(switchOp.arg, ',');
    for (int i=0; i<tokens.length; ++i)
    {
      IrOp target = jumpTarget(ops, Integer.parseInt(tokens[i]));
      int off = jumpOffset(switchOp, target);
      code.s2(switchOp.pos+3+(i*2), off);
    }
  }

  private IrOp jumpTarget(IrOp[] ops, int index)
  {
    if (index < 0 || index  >= ops.length)
      throw err("Invalid jump index " + index);
    return ops[index];
  }

  private int jumpOffset(IrOp jump, IrOp target)
  {
    // calculate jump offset (based on jump opcode pos)
    int off = target.pos - jump.pos;

    // check that jump fits in signed 16 bit
    if (off < Short.MIN_VALUE || off > Short.MAX_VALUE)
      throw err("Jump too far");

    return off;
  }

  private void op(IrOp op)
  {
    padArg(argAlignment(op));  // nop padding so that args are aligned
    op.pos = code.size;        // save position of operation
    if (op.isFieldOp())
    {
      fieldOp(op);            // handle field opcodes specially
    }
    else if (op.opcode == SCode.CallVirtual)
    {
      callVirtual(op);
    }
    else if (op.opcode == SCode.CallNative ||
             op.opcode == SCode.CallNativeWide ||
             op.opcode == SCode.CallNativeVoid)
    {
      callNative(op);
    }
    else if (op.opcode == SCode.Switch)
    {
      switchOp(op);
    }
    else if (op.opcode == SCode.LoadArrayLiteral)
    {
      // array literals use an internal bytecode which actually
      // gets written as LoadBuf (to maintain compatibility with
      // older SVMs); but effectively all we are doing is loading
      // a pointer to the code section which is exactly what the
      // LoadBuf opcode does
      code.u1(SCode.LoadBuf);
      opArg(op);
    }
    else if (op.opcode == SCode.LoadSlotId)
    {
      // LoadSlotId qname
      // Optimize as constant integer load
      IrSlot slot = (IrSlot)ns.resolveSlot(op.arg);
      switch (slot.id)
      {
        case 0:
          code.u1(SCode.LoadI0); break;
        case 1:
          code.u1(SCode.LoadI1); break;
        case 2:
          code.u1(SCode.LoadI2); break;
        case 3:
          code.u1(SCode.LoadI3); break;
        case 4:
          code.u1(SCode.LoadI4); break;
        case 5:
          code.u1(SCode.LoadI5); break;
        default:
          code.u1(SCode.LoadIntU1);
          code.u1(slot.id);
      }
    }
    else
    {
      code.u1(op.opcode);      // write opcode
      opArg(op);               // argument
    }
  }

  private void opArg(IrOp op)
  {
    switch (op.argType())
    {
      case SCode.noArg:     return;
      case SCode.u1Arg:     code.u1(op.argToInt());   return;
      case SCode.u2Arg:     code.u2(op.argToInt());   return;
      case SCode.s4Arg:     code.i4(op.argToInt());   return;
      case SCode.intArg:    addBlockIndex(toInt(op.argToInt())); return;
      case SCode.longArg:   addBlockIndex(toLong(op.argToLong())); return;
      case SCode.floatArg:  addBlockIndex(toFloat(op.argToFloat())); return;
      case SCode.doubleArg: addBlockIndex(toDouble(op.argToDouble())); return;
      case SCode.strArg:    addBlockIndex(string(op.argToStr())); return;
      case SCode.bufArg:    addBlockIndex(buf(op.argToBuf())); return;
      case SCode.arrayArg:  addBlockIndex(array((Expr.Literal)op.resolvedArg)); return;
      case SCode.methodArg: addBlockIndex(op.argToMethod().codeAddr); return;
      case SCode.jmpArg:    code.s1(0xff);            return;
      case SCode.jmpfarArg: code.s2(0xffff);          return;
      case SCode.slotArg:
          addBlockIndex(op.argToIrSlot().reflect);
          break;
      case SCode.typeArg:
        if (op.opcode == SCode.InitVirt)
          addBlockIndex(op.argToIrType().vtable);
        else
          addTypeBlockIndex(op.argToType());
        return;
      default: throw new IllegalStateException(SCode.name(op.opcode));
    }
  }

  private int argAlignment(IrOp op)
  {
    switch (op.argType())
    {
      case SCode.noArg:     return 0;
      case SCode.u1Arg:     return 0;
      case SCode.u2Arg:     return 2;
      case SCode.s4Arg:     return 4;
      case SCode.intArg:    return 2;
      case SCode.longArg:   return 2;
      case SCode.floatArg:  return 2;
      case SCode.doubleArg: return 2;
      case SCode.strArg:    return 2;
      case SCode.bufArg:    return 2;
      case SCode.fieldArg:  return op.argToField().offsetWidth();
      case SCode.methodArg: return 0;  // methods block indices not aligned
      case SCode.slotArg:   return 2;  // const ref to slot literal
      case SCode.typeArg:   return 2;  // const ref to type literal (or vtable)
      case SCode.jmpArg:    return 0;
      case SCode.jmpfarArg: return 2;
      case SCode.switchArg: return 2;
      case SCode.arrayArg:  return 2;  // const ref to literal in code section
      default: throw new IllegalStateException(SCode.name(op.opcode));
    }
  }

  private void padArg(int argAlignment)
  {
    if (argAlignment < 2) return;

    // pad such that once we add the a one byte opcode the
    // argument will be aligned on the specified byte boundary
    int len = code.size+1;
    int rem = len % argAlignment;
    if (rem == 0) return;
    code.pad(argAlignment-rem);
    if ((code.size+1) % argAlignment != 0) throw new IllegalStateException();
  }

  private void callVirtual(IrOp op)
  {
    IrMethod m  = op.argToMethod();
    if (m.vindex < 0) throw new IllegalStateException("Method not assigned vindex: " + m.qname);
    code.u1(op.opcode);
    code.u2(m.vindex, false);
    code.u1(m.numParams());
  }

  private void callNative(IrOp op)
  {
    IrMethod m  = op.argToMethod();
    if (m.nativeId == null) throw new IllegalStateException("Method not assigned native id: " + m.qname);
    int numParams = m.numParams();
    code.u1(op.opcode);
    code.u1(m.nativeId.kitId);
    code.u1(m.nativeId.methodId);
    code.u1(numParams);
  }

  private void fieldOp(IrOp op)
  {
    IrField f  = op.argToField();
    int width  = f.offsetWidth();
    int offset = f.offset;

    // const static is written as block index
    if (op.opcode == SCode.LoadConstStatic)
    {
      code.u1(op.opcode);
      addBlockIndex(f.storage);
      return;
    }

    // sanity checking
    if (offset < 0)
      throw err("Field offset not set: " + f.qname);
    if (offset > 0x7fffffff)
      throw err("Field offset is too big: " + f.qname);

    // sanity check that IR used a field u1 opcode
    switch (op.opcode)
    {
      case SCode.Load8BitFieldU1:   case SCode.Store8BitFieldU1:
      case SCode.Load16BitFieldU1:  case SCode.Store16BitFieldU1:
      case SCode.Load32BitFieldU1:  case SCode.Store32BitFieldU1:
      case SCode.Load64BitFieldU1:  case SCode.Store64BitFieldU1:
      case SCode.LoadRefFieldU1:    case SCode.StoreRefFieldU1:
      case SCode.LoadConstFieldU1:
      case SCode.LoadInlineFieldU1:
      case SCode.LoadDataInlineFieldU1:
      case SCode.LoadParam0InlineFieldU1:
        break;
      default:
        throw err("Invalid field opcode used for IR", f.qname + " " + SCode.name(op.opcode));
    }

    // switch to a bigger opcode based on width taking
    // advantage that U1, U2, and U4 opcodes are contiguous
    if (width == 1)
    {
      code.u1(op.opcode);
      code.u1(offset);
    }
    else if (width == 2)
    {
      code.u1(op.opcode+1);
      if (code.size % 2 != 0) throw new IllegalStateException();
      code.u2(offset);
    }
    else
    {
      // there isn't a U4 version of LoadConst
      if (op.opcode == SCode.LoadConstFieldU1)
        throw new IllegalStateException("Const field too wide: " + f.qname);

      code.u1(op.opcode+2);
      if (code.size % 4 != 0) throw new IllegalStateException();
      code.i4(offset);
    }
  }

  private void switchOp(IrOp op)
  {
    int num = TextUtil.split(op.arg, ',').length;

    code.u1(op.opcode);
    code.u2(num);
    for (int i=0; i<num; ++i)
      code.s2(0xffff);
  }

//////////////////////////////////////////////////////////////////////////
// Const Pool
//////////////////////////////////////////////////////////////////////////

  private ConstPool.IrInt toInt(int v)
  {
    return constPool.toInt(new Integer(v));
  }

  private ConstPool.IrLong toLong(long v)
  {
    return constPool.toLong(new Long(v));
  }

  private ConstPool.IrFloat toFloat(float v)
  {
    return constPool.toFloat(new Float(v));
  }

  private ConstPool.IrDouble toDouble(double v)
  {
    return constPool.toDouble(new Double(v));
  }

  private ConstPool.IrStr string(String val)
  {
    return constPool.string(val);
  }

  private ConstPool.IrBuf buf(Buf val)
  {
    return constPool.buf(val);
  }

  private ConstPool.IrArray array(Expr.Literal literal)
  {
    return constPool.array(literal);
  }

  private ConstPool.IrQnameType qnameType(Type type)
  {
    return constPool.qnameType(type);
  }

  private ConstPool.IrQnameSlot qnameSlot(Slot slot)
  {
    return constPool.qnameSlot(slot);
  }

//////////////////////////////////////////////////////////////////////////
// Tests
//////////////////////////////////////////////////////////////////////////

  private void tests()
  {
    // set tests table block index to current location
    code.align(2);
    if (image.test)
    {
      code.u2(18, blockIndex());
    }
    else
    {
      code.u2(18, 0);
      return;
    }

    // write out tests table
    IrMethod[] testMethods = compiler.testMethods;
    code.u2(testMethods.length);
    for (int i=0; i<testMethods.length; ++i)
    {
      IrMethod m = testMethods[i];
      addBlockIndex(qnameSlot(m));
      addBlockIndex(m.codeAddr);
    }
    blockAlign();
  }

//////////////////////////////////////////////////////////////////////////
// Backpatch
//////////////////////////////////////////////////////////////////////////

  private void backpatch()
  {
    for (Backpatch bp = backpatch; bp != null; bp = bp.next)
    {
      if (bp.ir.getBlockIndex() == 0)
        throw err("Attempt to back patch IR with no block index: " + bp.ir);

      code.u2(bp.off, bp.ir.getBlockIndex(), bp.ir.alignBlockIndex());
    }
  }

  private void finish()
  {
    code.i4(8, code.size);
    image.code = code.trim();
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  public int blockIndex()
  {
    int ix = code.size;
    if (ix % image.blockSize != 0) throw new IllegalStateException();
    return ix / image.blockSize;
  }

  public void blockAlign()
  {
    code.align(image.blockSize);
  }

  public CompilerException err(String msg)
  {
    return err(msg, loc);
  }

//////////////////////////////////////////////////////////////////////////
// Backpatch
//////////////////////////////////////////////////////////////////////////

  private void addBlockIndex(IrAddressable ir)
  {
    // create backpatch record
    Backpatch bp = new Backpatch();
    bp.off = code.size;
    bp.ir  = ir;

    // add to link list
    bp.next = backpatch;
    backpatch = bp;

    // leave two byte space for block index to backpatch later
    code.u2(0xffff, ir.alignBlockIndex());
  }

  public void addTypeBlockIndex(Type type)
  {
    if (type instanceof IrType)
      addBlockIndex((IrType)type);
    else
      addBlockIndex(flat.primitives[((PrimitiveType)type).id]);
  }

  static class Backpatch
  {
    int off;           // offset into code buffer
    IrAddressable ir;  // construct to backpatch once we know its block index
    Backpatch next;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  Location loc;
  Buf code = new Buf(32768);
  SCodeImage image;
  IrKit[] kits;            // all kits being generated into image
  IrType[] types;          // all types across all kits
  IrMethod[] methods;      // all methods across all kits/types
  IrMethod[] staticInits;  // all static initializers across all kits/types
  IrField[] staticFields;  // all static fields across all kits/types
  ConstPool constPool;     // map of addressable constants
  Backpatch backpatch;     // link list of locations to back patch

}
