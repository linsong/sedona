//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   20 May 08  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import sedona.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.namespace.*;
import sedonac.ir.*;
import sedonac.scode.*;

/**
 * OptimizeIr performs peephole optimization on the IR instructions.
 */
public class OptimizeIr
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public OptimizeIr(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {                               
    if (!compiler.optimize) return;
    log.debug("  OptimizeIr");
    IrType[] types = compiler.ir.types;
    for (int i=0; i<types.length; ++i)
    {
      IrType t = types[i];
      for (int j=0; j<t.declared.length; ++j)
        if (t.declared[j] instanceof IrMethod)
          optimize((IrMethod)t.declared[j]);
    }                   
  }

//////////////////////////////////////////////////////////////////////////
// Profile
//////////////////////////////////////////////////////////////////////////

  /*
  public void profile()
  {
    for (int k=0; k<compiler.kits.length; ++k)
    {
      IrType[] types = compiler.kits[k].types;
      for (int i=0; i<types.length; ++i)
      {
        IrType t = types[i];
        for (int j=0; j<t.declared.length; ++j)
          if (t.declared[j] instanceof IrMethod)
            profile((IrMethod)t.declared[j]);
      }
    }                 
    
    Profile[] top = (Profile[])doubles.values().toArray(new Profile[doubles.size()]);    
    Arrays.sort(top);
    for (int i=0; i<40; ++i)
      System.out.println("  " + top[i]);
  }  

  void profile(IrMethod m)
  {
    if (m.code == null || m.code.length < 2) return;
    while (doOptimize(m));

    for (int i=0; i<m.code.length-1; ++i)
    {
      IrOp a = m.code[i];
      IrOp b = m.code[i+1];

      String x = SCode.name(a.opcode) + " " + SCode.name(b.opcode);
      Profile p = (Profile)doubles.get(x);
      if (p == null) { p = new Profile(); p.name = x; p.count = 1; }
      else p.count++;
      doubles.put(x,p);      
    }    
  }

  static class Profile implements Comparable
  {
    public int compareTo(Object o)
    {
      int x = ((Profile)o).count;
      if (x == count) return 0;
      if (count > x) return -1;
      return 1;
    }

    public String toString()
    {
      return TextUtil.padRight(""+count, 8) + " " + name;
    }

    String name;
    int count;
  }

  HashMap doubles = new HashMap();  // String: Profile
  */
  
//////////////////////////////////////////////////////////////////////////
// Optimize
//////////////////////////////////////////////////////////////////////////
  
  /**
   * Recursively optimize the method until we don't 
   * have any optimizations left.
   */
  void optimize(IrMethod m)
  {
    if (m.code == null || m.code.length < 2) return;
    noPeephole.clear();
    while (doOptimize(m));
  }

  /**
   * Attempt to find a peephole optimization.
   */
  boolean doOptimize(IrMethod m)
  {
    IrOp[] code = m.code;
    for (int i=0; i<code.length-1; ++i)
    {

      IrOp a = code[i];
      IrOp b = code[i+1];
      if (a.index != i) throw new IllegalStateException();
      
      IrOp replace = optimizeOp(m, a);
      if (replace != null)
      {
        replace(m, i, replace);
        return true;
      }
      
      IrOp join = peephole(m, a, b);
      if (join != null) 
      {
        join(m, i, join);
        return true;
      }
    }
    return false;
  }

  /**
   * Replace m.code[at] with x
   */
  void replace(IrMethod m, int at, IrOp x)
  { 
    x.index = at;
    m.code[at] = x;             
  }

  /**
   * Merge m.code[at] and m.code[at+1] with x
   */
  void join(IrMethod m, int at, IrOp x)
  {
    // build new instruction array
    final boolean isCast = m.code[at].opcode == SCode.Cast;
    IrOp[] o = m.code;
    IrOp[] n = new IrOp[o.length-1];
    for (int i=0; i<o.length; ++i)
    {
      if (i < at) n[i] = o[i];
      else if (i == at) n[i] = x;
      else if (i > at+1) n[i-1] = o[i];
    }
    m.code = n;

    // update any jump labels
    for (int i=0; i<n.length; ++i)
    {
      IrOp op = n[i];
      op.index = i;

      if (op.isJump())
      {
        int label = Integer.parseInt(op.arg);
        if (label == at+1) 
        {
          // this originally always threw an ISE, and I'm not sure what the
          // thinking was there.  This case can legitimately happen when
          // optimizing a Cast opcode, so we will explicitly check that case.
          if (!isCast)
          {
            // Instead of failing with ISE, let's revert back to state of
            // opcodes when we entered this method and mark the offending
            // opcode to be excluded from peephole optimization.
            for (int z=0; z<o.length; ++z)
            {
              IrOp revert = o[z];
              revert.index = z;
              if (revert.isJump())
              {
                label = Integer.parseInt(revert.arg);
                if (label >= at)
                  revert.arg = String.valueOf(label+1);
              }
            }
            m.code = o;
            noPeephole.add(o[at]);
            return;
          }
          op.arg = String.valueOf(label-1);
        }
        if (label > at) op.arg = String.valueOf(label-1);
      }

      if (op.opcode == SCode.Switch)
      {
        StringBuffer s = new StringBuffer();
        String[] toks = TextUtil.split(op.arg, ',');
        for (int j=0; j<toks.length; ++j)
        {
          int label = Integer.parseInt(toks[j]);
          if (label == at+1) throw new IllegalStateException(x.toString());
          if (label > at) label -= 1;
          if (j > 0) s.append(',');
          s.append(label);
        }
        op.arg = s.toString();
      }
    }
  }

////////////////////////////////////////////////////////////////
// Single Opcode Optimization
////////////////////////////////////////////////////////////////

  /**
   * Can we replace the single opcode with a more efficient one?
   * Return null if no optimization.
   */
  IrOp optimizeOp(IrMethod m, IrOp op)                           
  {           
    // optimize method call:
    //   - Str.get into byte[] load instruction                                             
    //   - Str.set into byte[] store instruction                                             
    if (op.opcode == SCode.Call)      
    {
      String qname = op.argToSlot().qname();
      if (qname.equals("sys::Str.get")) return new IrOp(SCode.Load8BitArray);
      if (qname.equals("sys::Str.set")) return new IrOp(SCode.Store8BitArray);
    }                           
    
    return null;
  }              

////////////////////////////////////////////////////////////////
// Peephole
////////////////////////////////////////////////////////////////
  
  /**
   * Peephole optimize attempts to replace to consecutive
   * opcodes with a single opcode.  Return null if no 
   * optimization.
   */
  IrOp peephole(IrMethod m, IrOp a, IrOp b)
  {                  
    if (noPeephole.contains(a))  return null;

    // cast is never used in the SVM (only used for JVM bytecode)
    if (a.opcode == SCode.Cast) return b;
             
    // LoadInlineFieldU1 where we can guarantee field
    // is at zero offset is an unnecessary operation since
    // the base address and field address is the same
    if (a.opcode == SCode.LoadInlineFieldU1 && isFieldAtZero(a.arg))
      return b;        
    
    // LoadDataAddr  b
    if (a.opcode == SCode.LoadDataAddr)
    {
      if (b.opcode == SCode.LoadInlineFieldU1) return new IrOp(SCode.LoadDataInlineFieldU1, b.arg);
    }
    
    // LoadParam0  b
    if (a.opcode == SCode.LoadParam0)
    {
      if (b.opcode == SCode.LoadInlineFieldU1) return new IrOp(SCode.LoadParam0InlineFieldU1, b.arg);
      if (b.opcode == SCode.Call) return new IrOp(SCode.LoadParam0Call, b.arg);      
    }
    
    // a JumpNonZero
    if (b.opcode == SCode.JumpNonZero)
    {
      if (a.opcode == SCode.IntEq)     return new IrOp(SCode.JumpIntEq,    b.arg);
      if (a.opcode == SCode.IntNotEq)  return new IrOp(SCode.JumpIntNotEq, b.arg);
      if (a.opcode == SCode.IntLt)     return new IrOp(SCode.JumpIntLt,    b.arg);
      if (a.opcode == SCode.IntLtEq)   return new IrOp(SCode.JumpIntLtEq,  b.arg);
      if (a.opcode == SCode.IntGt)     return new IrOp(SCode.JumpIntGt,    b.arg);
      if (a.opcode == SCode.IntGtEq)   return new IrOp(SCode.JumpIntGtEq,  b.arg);
      if (a.opcode == SCode.EqZero)    return new IrOp(SCode.JumpZero,    b.arg);
      if (a.opcode == SCode.NotEqZero) return new IrOp(SCode.JumpNonZero, b.arg);
    }

    // a JumpZero
    if (b.opcode == SCode.JumpZero)
    {
      if (a.opcode == SCode.IntEq)     return new IrOp(SCode.JumpIntNotEq, b.arg);
      if (a.opcode == SCode.IntNotEq)  return new IrOp(SCode.JumpIntEq,    b.arg);
      if (a.opcode == SCode.IntLt)     return new IrOp(SCode.JumpIntGtEq,  b.arg);
      if (a.opcode == SCode.IntLtEq)   return new IrOp(SCode.JumpIntGt,    b.arg);
      if (a.opcode == SCode.IntGt)     return new IrOp(SCode.JumpIntLtEq,  b.arg);
      if (a.opcode == SCode.IntGtEq)   return new IrOp(SCode.JumpIntLt,    b.arg);
      if (a.opcode == SCode.EqZero)    return new IrOp(SCode.JumpNonZero,  b.arg);
      if (a.opcode == SCode.NotEqZero) return new IrOp(SCode.JumpZero,     b.arg);
    }

    // LoadI0 b
    if (a.opcode == SCode.LoadI0)
    {         
      if (b.opcode == SCode.JumpIntEq) return new IrOp(SCode.JumpZero, b.arg);
      if (b.opcode == SCode.JumpIntNotEq) return new IrOp(SCode.JumpNonZero, b.arg);
      
      if (b.opcode == SCode.IntEq)    return new IrOp(SCode.EqZero);
      if (b.opcode == SCode.IntNotEq) return new IrOp(SCode.NotEqZero);
    }

    // LoadNull b (this probably isn't correct for a 64-bit VM, 
    // but should work fine on a 32-bit VM where null is zero)
    if (a.opcode == SCode.LoadNull)
    {          
      if (b.opcode == SCode.ObjEq)    return new IrOp(SCode.EqZero);
      if (b.opcode == SCode.ObjNotEq) return new IrOp(SCode.NotEqZero);
    }

    return null;
  }                      
  
  /**
   * Can we guarantee the field is at the zero offset.
   * To be conservative since we don't know how ImageGen
   * will layout the fields, we assume this is only valid
   * if the field's parent only has one field (but this fits
   * common cases like Str).
   */
  boolean isFieldAtZero(String qname)
  {                  
    Field f = (Field)ns.resolveSlot(qname);
    Type parent = f.parent();
    
    int numFields = 0;
    Slot[] slots = parent.slots();
    for (int i=0; i<slots.length; ++i)
    {
      Slot s = slots[i];
      if (s.isField() && !s.isStatic()) 
        numFields++;
    }
    
    return numFields == 1 && parent.base().isObj(); 
  } 
  

  /**
   * Return if the dest is the destination of any jump instructions.
   */            
  /*
  boolean isJumpDest(IrMethod m, IrOp dest)
  {
    for (int i=0; i<m.code.length; ++i)
    {
      IrOp op = m.code[i];
      
      if (op.isJump())
      {
        int label = Integer.parseInt(op.arg);
        if (label == dest.index) return true;
      }

      if (op.opcode == SCode.Switch)
      {
        StringBuffer s = new StringBuffer();
        String[] toks = TextUtil.split(op.arg, ',');
        for (int j=0; j<toks.length; ++j)
        {
          int label = Integer.parseInt(toks[j]);
          if (label == dest.index) return true;
        }
      }
    }
    
    return false;
  }       
  */

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////
  
  private final Set noPeephole = new HashSet();

}

