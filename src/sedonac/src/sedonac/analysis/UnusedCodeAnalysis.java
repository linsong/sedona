//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

package sedonac.analysis;

import java.util.*;

import sedonac.Compiler;
import sedonac.*;
import sedonac.ast.*;
import sedonac.namespace.*;

/**
 * Reports warnings for
 * <ul>
 * <li>Internal types that are not used
 * <li>Internal or private methods that are not used
 * </ul>
 * <p>
 * We currently do not report unreferenced fields because reflected slots are
 * set by the vm. Also, certain fields (like {@code inet::TcpSocket.socket}) are
 * set by native code. There is no way to know this at compile time. We could
 * consider a <code>@sedonac.unchecked</code> as a hint to the compiler, but
 * for now we just won't report it.
 * 
 * @author Matthew Giannini
 * @creation Nov 10, 2009
 * 
 */
public class UnusedCodeAnalysis extends CompilerStep
{
  public UnusedCodeAnalysis(Compiler compiler)
  {
    super(compiler);
  }

  public void run()
  {
    refs = new HashMap();
    walkAst(CompilerStep.WALK_TO_EXPRS);
    logUnused();
    refs = null;
  }
  
  public void enterType(TypeDef t)
  {
    super.enterType(t);
    if (t.isInternal() && !refs.containsKey(t))
      refs.put(t, null);
  }

  public void enterMethod(MethodDef m)
  {
    super.enterMethod(m);
    if ((m.isInternal() || m.isPrivate()) && !refs.containsKey(m))
      refs.put(m, null);
    
    // Always consider these referenced
    if (m.qname().equals("sys::Sys.malloc") ||
        m.qname().equals("sys::Sys.free"))
    {
      refs.put(m, m);
    }
  }
  
  public void enterField(FieldDef f)
  {
    super.enterField(f);
    if ((f.isInternal() || f.isPrivate()) && !refs.containsKey(f))
      refs.put(f, null);
    markReferenced(f.type);
  }

  public Expr expr(Expr expr)
  {
    switch (expr.id)
    {
      case Expr.CALL:         markReferenced(((Expr.Call)expr).method); break;
      case Expr.CAST:         markReferenced(((Expr.Cast)expr).type);   break;
      case Expr.FIELD:        markReferenced(((Expr.Field)expr).field); break;
      case Expr.LOCAL:        markReferenced(((Expr.Local)expr).type);  break;
      case Expr.SLOT_LITERAL: 
        markReferenced(((Expr.Literal)expr).asSlot().parent()); 
        break;
      case Expr.STATIC_TYPE:  markReferenced(expr.type);                break;
      case Expr.TYPE_LITERAL: 
        markReferenced(((Expr.Literal)expr).asType()); 
        break;
    }
    return super.expr(expr);
  }
  
  private void markReferenced(Type t)
  {
    if (t == null || !t.isInternal() || !(t instanceof TypeDef)) 
      return;
    refs.put(t, t);
  }
  
  private void markReferenced(Method m)
  {
    if (m == null || m.isPublic() || m.isProtected() || !(m instanceof MethodDef))
      return;
    refs.put(m, m);
  }
  
  private void markReferenced(Field f)
  {
    if (f == null || f.isPublic() || f.isProtected() || !(f instanceof FieldDef))
      return;
    if (curMethod != null && curMethod.isInstanceInit())
      return;
    refs.put(f, f);
  }
  
  private void logUnused()
  {
    AstNode[] nodes = (AstNode[])refs.keySet().toArray(new AstNode[refs.size()]);
    Arrays.sort(nodes, new Comparator()
    {
      public int compare(Object o1, Object o2)
      {
        return ((AstNode)o1).loc.compareTo(((AstNode)o2).loc);
      }
    });
    
    for (int i=0; i<nodes.length; ++i)
    {
      AstNode node = nodes[i];
      if (refs.get(node) != null) continue;
      if (node instanceof TypeDef)
      {
        TypeDef t = (TypeDef)node;
        warn("internal class '" + t.qname() + "' is not used", t.loc);
      }
      else if (node instanceof MethodDef)
      {
        MethodDef m = (MethodDef)node;
        String scope = m.isInternal() ? "internal" : "private";
        warn(scope + " method '" + m.qname() + "' is not used", m.loc);
      } 
      /*
      else if (node instanceof FieldDef)
      {
        FieldDef f = (FieldDef)node;
        String scope = f.isInternal() ? "internal" : "private";
        warn(scope + " field '" + f.qname() + "' is not used", f.loc);
      }
      */
    }
  }
  
  public void exitMethod(MethodDef m)
  {
    // Mark return type and parameter types as referenced
    markReferenced(m.ret);
    
    Type[] paramTypes = m.paramTypes();
    for (int i=0; i<paramTypes.length; ++i)
      markReferenced(paramTypes[i]);
    
    super.exitMethod(m);
  }

  private HashMap refs;
}
