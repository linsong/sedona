//
// Copyright (c) 2006 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   31 May 06  Brian Frank  Creation
//

package sedonac;

import java.io.*;
import sedona.xml.*;
import sedonac.ast.*;
import sedonac.namespace.*;
import sedonac.ir.*;

/**
 * CompilerSupport provides a bunch of convenience methods
 * used by compiler support classes including CompilerStep.
 */
public abstract class CompilerSupport
  extends AstVisitor
{

////////////////////////////////////////////////////////////////
// Cosntructor
////////////////////////////////////////////////////////////////

  public CompilerSupport(Compiler compiler)
  {
    this.compiler = compiler;
    this.log      = compiler.log;
    this.ns       = compiler.ns;
    this.flat     = compiler.flat;
  }

////////////////////////////////////////////////////////////////
// Errors
////////////////////////////////////////////////////////////////

  public void quitIfErrors()
  {
    compiler.quitIfErrors();
  }

  public CompilerException err(String msg)
  {
    return compiler.err(msg);
  }

  public CompilerException err(String msg, Location loc)
  {
    return compiler.err(msg, loc);
  }

  public CompilerException err(String msg, String loc)
  {
    return compiler.err(msg, loc);
  }

  public CompilerException err(String msg, File loc)
  {
    return compiler.err(msg, String.valueOf(loc));
  }

  public CompilerException err(String msg, Location loc, Throwable e)
  {
    return compiler.err(msg, loc, e);
  }

  public CompilerException err(String msg, String loc, Throwable e)
  {
    return compiler.err(msg, loc, e);
  }

  public CompilerException err(CompilerException err)
  {
    return compiler.err(err);
  }

  public CompilerException err(XException err)
  {
    return compiler.err(err);
  }
  
////////////////////////////////////////////////////////////////
// Warnings
////////////////////////////////////////////////////////////////

  public void warn(String msg)
  {
    compiler.warn(msg);
  }
  
  public void warn(String msg, Location loc)
  {
    compiler.warn(msg, loc);
  }
  
////////////////////////////////////////////////////////////////
// Fields
////////////////////////////////////////////////////////////////

  public final Compiler compiler;
  public final CompilerLog log;
  public final Namespace ns;
  public final IrFlat flat;

}
