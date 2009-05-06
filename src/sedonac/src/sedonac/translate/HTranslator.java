//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Mar 07  Brian Frank  Creation
//

// this code is in a vegetative state

package sedonac.translate;

import java.io.*;
import java.util.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * HTranslator
 */
public class HTranslator
  extends CTranslator
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public HTranslator(Compiler compiler, TypeDef type)
  {
    super(compiler, type);
  }

//////////////////////////////////////////////////////////////////////////
// Translate
//////////////////////////////////////////////////////////////////////////

  public File toFile()
  {
    return new File(outDir, qname(type) + ".h");
  }

  public void doTranslate()
  {
    String barrier = "__" + qname(type).toUpperCase() + "_H";
    w("#ifndef ").w(barrier).nl();
    w("#define ").w(barrier).nl();
    w("").nl();
    w("#include \"sedona.h\"").nl();
    includes();
    w("").nl();
    structForward();
    w("").nl();
    staticFieldForwards();
    w("").nl();
    methodForwards();
    w("").nl();
    w("#endif").nl().nl();
  }

//////////////////////////////////////////////////////////////////////////
// Includes
//////////////////////////////////////////////////////////////////////////

  public void findIncludes(HashMap acc, FieldDef f)
  {
    addInclude(acc, f.type);
  }

  public void findIncludes(HashMap acc, MethodDef m)
  {
    addInclude(acc, m.ret);
    for (int i=0; i<m.params.length; ++i)
      addInclude(acc, m.params[i].type);
  }

//////////////////////////////////////////////////////////////////////////
// Forwards
//////////////////////////////////////////////////////////////////////////

  public void structForward()
  {
    w("typedef struct").nl();
    w("{").nl();
    FieldDef[] fields = type.fieldDefs();
    int num = 0;
    for (int i=0; i<fields.length; ++i)
    {
      FieldDef f = fields[i];
      if (!f.isStatic())
      {
        structField(f);
        num++;
      }
    }
    if (num == 0) w("  uint8_t dummy;").nl();
    w("} ").nl();
    w(qname(type)).w(";").nl();
  }

  public void structField(FieldDef f)
  {
    w("  ").wtype(f.type);
    if (f.type.isRef() && !f.isInline()) w("*");
    w(" ").w(f.name).w(";").nl();
  }

  public void staticFieldForwards()
  {
    FieldDef[] fields = type.fieldDefs();
    for (int i=0; i<fields.length; ++i)
    {
      FieldDef f = fields[i];
      if (f.isStatic())
      {
        w("extern ");
        fieldSig(f);
        w(";").nl();
      }
    }
  }

  public void methodForwards()
  {
    MethodDef[] methods = type.methodDefs();
    for (int i=0; i<methods.length; ++i)
    {
      MethodDef m = methods[i];
      w("extern ");
      methodSig(m);
      w(";").nl();
    }
  }

}
