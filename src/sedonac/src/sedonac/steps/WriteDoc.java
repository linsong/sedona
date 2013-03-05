//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   7 Jun 07 (its my b-day!)  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import sedona.Env;
import sedona.util.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.Type;
import sedonac.namespace.TypeUtil;
import sedonac.util.*;

/**
 * WriteDoc generates the HTML Sedona docs for the APIs
 * if the Compiler.doc flag is set to true.
 */
public class WriteDoc
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public WriteDoc(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    if (!compiler.doc) return;                                       
    
    File outDir = compiler.outDir;
    if (outDir == null) outDir = new File(Env.home, "doc");
    else outDir = new File(compiler.outDir, "doc");

    this.www = compiler.www;
    this.kit = compiler.ast;
    this.dir = new File(outDir, kit.name);
    
    if (!kit.doc) return;    

    log.info("  WriteDoc [" + dir + "]");

    mkdir();
    filterTypes();
    index();
    generate();
  }

//////////////////////////////////////////////////////////////////////////
// Makedir
//////////////////////////////////////////////////////////////////////////

  private void mkdir()
  {
    // start with fresh dir
    try
    {
      FileUtil.delete(dir, log);
      FileUtil.mkdir(dir, log);
    }
    catch (IOException e)
    {
      throw err("Cannot make dir", new Location(dir), e);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Filter Types
//////////////////////////////////////////////////////////////////////////

  private void filterTypes()
  {
    // filter types to doc - we only doc public non-test types
    ArrayList acc = new ArrayList();
    for (int i=0; i<kit.types.length; ++i)
    {
      TypeDef t = kit.types[i];
      if (isDoc(t)) acc.add(t);
    }
    this.types = (TypeDef[])acc.toArray(new TypeDef[acc.size()]);
    Arrays.sort(types, typeCompare);
  }

//////////////////////////////////////////////////////////////////////////
// Header/Footer
//////////////////////////////////////////////////////////////////////////

  private void header(XWriter out, String title)
  {
    String home = www ? "../../index.html" : "../index.html";
    
    out.w("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n");
    out.w(" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
    out.w("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
    out.w("<head>\n");
    out.w("  <title>").w(title).w("</title>\n");
    out.w("  <meta http-equiv='Content-type' content='text/html;charset=UTF-8' />\n");
    out.w("  <link rel='stylesheet' type='text/css' href='../style.css'/>\n");
    out.w("</head>\n");
    out.w("<body>\n"); 

    out.w("<p>\n");
    out.w("  <a href='").w(home).w("'>\n");
    out.w("    <img src='../logo.png' alt='Sedona'/>\n");
    out.w("  </a>\n");
    out.w("</p>\n");

    if (www)
    {
      out.w("<ul class='tabs'>\n");
      out.w("  <li><a href='").w(home).w("'>Home</a></li>\n");
      out.w("  <li><a class='active' href='../index.html'>Documentation</a></li>\n");
      out.w("  <li><a href='../../community.html'>Community</a></li>\n");
      out.w("  <li><a href='/download/'>Downloads</a></li>\n");
      out.w("  <li><a href='../../forum.html'>Forum</a></li>\n");
      out.w("</ul>\n");    
    }
    
    nav(out);
  }

  private void footer(XWriter out)
  {
    nav(out);
    out.w("<div class='copyright'><script type='text/javascript'>document.write(\"Copyright &#169; \" + new Date().getFullYear() + \" Tridium, Inc.\")</script></div>\n");
    out.w("</body>\n");
    out.w("</html>\n");
  }

  public static void writeCopyright(XWriter out)
  {
    out.w("<div class='copyright'><script type='text/javascript'>document.write(\"Copyright &#169; \" + new Date().getFullYear() + \" Tridium, Inc.\")</script></div>\n");
  }

  private void nav(XWriter out)
  {
    if (www)
      wwwNav(out);
    else
      normNav(out);
  }
  
  private void normNav(XWriter out)
  {
    out.w("<div class='nav'>\n");
    out.w("  <a href='../index.html'>Index</a> |\n");
    out.w("  <a href='../api.html'>Kits</a> |\n");
    out.w("  <a href='index.html'>").w(kit.name).w("</a>\n");
    out.w("</div>\n");
  }

  private void wwwNav(XWriter out)
  {
    out.w("<div class='nav'>\n");
    out.w("  <a href='../index.html'>Index</a> |\n");
    out.w("  <a href='../api.html'>Kits</a> |\n");
    out.w("  <a href='index.html'>").w(kit.name).w("</a>\n");
    out.w("</div>\n");
  }

//////////////////////////////////////////////////////////////////////////
// Index
//////////////////////////////////////////////////////////////////////////

  private void index()
  {
    File f = new File(dir, "index.html");
    try
    {
      XWriter out = new XWriter(f);
      index(out);
      out.close();
    }
    catch (Exception e)
    {
      throw err("Cannot write file", new Location(f), e);
    }
  }

  private void index(XWriter out)
  {
    header(out, kit.name);
    out.w("<h1 class='title'>").w(kit.name).w("</h1>\n");
    out.w("<ul>\n");
    if (kit.description!=null)
    {
      out.w("<p>\n");
      out.w(kit.description);
      out.w("</p>\n");
    }

    for (int i=0; i<types.length; ++i)
    {
      TypeDef t = types[i];
      out.w("  <li><a href='").w(t.name).w(".html'>").w(t.name).w("</a></li>\n");
    }
    out.w("</ul>\n");
    footer(out);
  }

//////////////////////////////////////////////////////////////////////////
// Generate
//////////////////////////////////////////////////////////////////////////

  private void generate()
  {
    for (int i=0; i<types.length; ++i)
      generate(types[i]);
  }

  private void generate(TypeDef t)
  {
    File f = new File(dir, t.name + ".html");
    try
    {
      XWriter out = new XWriter(f);
      generate(t, out);
      out.close();
    }
    catch (Exception e)
    {
      throw err("Cannot write file", new Location(f), e);
    }
  }

  /**
   * Generate the documentation for this TypeDef.
   */
  void generate(TypeDef t, XWriter out)
  {
    header(out, t.qname);

    // type details
    out.w("<h1 class='title'>").w(t.qname).w("</h1>\n");    
    
    // inheritance
    out.w("<hr/>\n");
    out.w("<pre class='inheritance'>");
    ArrayList list = new ArrayList();
    Type base = t.base;
    while (base != null)
    {
      list.add(0, base);
      base = base.base();
    }
    int spaces = 0;
    for (int i=0; i<list.size(); i++)
    {                                
      if (spaces > 0) out.w(TextUtil.getSpaces(spaces));
      typeLink((Type)list.get(i), out, false); out.w("\n");
      spaces += 2;
      
    }    
    if (spaces > 0) out.w(TextUtil.getSpaces(spaces));
    out.w(t.qname).w("\n");
    out.w("</pre>\n");

    out.w("<br>\n");

    // Print modifiers
    out.w("<code style='color:darkgreen;font-weight:bold;font-size:120%'>");
    typeModifiers(t, out, "em");
    out.w(" class <b style='font-weight:bolder'>" + t.name() + "</b>  ");
    out.w("</code>");

    // Print facets
    if ((t.facets()!=null) && (!t.facets().isEmpty())) 
    {
      out.w("<code style='color:darkblue;font-weight:bold'>\n");
      out.safe(t.facets().toString());
      out.w("</code>\n");
    }
    out.w("<br>\n");

    out.w("<hr/>\n");
    
    // type doc
    if (t.doc != null)
      writeDoc(t.doc, out);

    // slot details
    out.w("<hr/>\n");
    SlotDef[] slots = t.slotDefs();
    Arrays.sort(slots, slotCompare);
    boolean dl = false;
    for (int i=0; i<slots.length; i++)
    {
      SlotDef slot = slots[i];
      if (!isDoc(slot)) continue;
      if (!dl)
      {
        out.w("<dl>\n");
        dl = true;
      }

      // Print name of method - except if cstr, print type name instead of _iInit
      String sname = slot.name;
      if (slot.isMethod())
      {
        MethodDef m = (MethodDef)slot;
        if (m.isInstanceInit()) sname = m.parent.name();
      }
      out.w("<dt>").w(sname).w("</dt>\n");

      out.w("<dd>");

      out.w("<p class='sig'><code>");
      slotDef(slot, out);
      out.w("</code></p>\n");

      if (slot.doc != null)
        writeDoc(slot.doc, out);

      out.w("</dd>\n");
    }
    if (dl) out.w("</dl>\n");

    footer(out);
  }

  /**
   * Generate the documentation for this SlotDef.
   */
  void slotDef(SlotDef slot, XWriter out)
  {
    if (slot.isField())
    {
      FieldDef f = (FieldDef)slot;
      slotModifiers(slot, out, "em");
      out.w("<b>");
      typeLink(f.type(), out);
      out.w(" ").w(f.name());
      out.w("</b>");
    }
    else
    {
      MethodDef m = (MethodDef)slot;
      slotModifiers(slot, out, "em");
      out.w("<b>");
      typeLink(m.returnType(), out);

      // Print name of method - if cstr, print type name instead of _iInit
      String mname = m.name();
      if (m.isInstanceInit()) mname = m.parent.name();
      out.w(" ").w( mname ).w("(");

      for (int i=0; i<m.params.length; i++)
      {
        if (i > 0) out.w(", ");
        typeLink(m.params[i].type, out);
        out.w(" ").w(m.params[i].name);
      }
      out.w(")");
      out.w("</b>");
    }

    // If any facets, print them last
    if ((slot.facets()!=null) && (!slot.facets().isEmpty())) 
      out.safe(" " + slot.facets().toString());

    out.w("\n\n");
  }


  /**
   * Print all modifiers associated with this type
   */
  void typeModifiers(TypeDef t, XWriter out, String htag)
  {
    if ((htag!=null) && (htag.length()>0)) out.w("<" + htag + ">");
    if (t.isPublic())    out.w("public ");
    if (t.isInternal())  out.w("internal ");
    if (t.isAbstract())  out.w("abstract ");
    if (t.isConst())     out.w("const ");
    if (t.isFinal())     out.w("final ");
    if ((htag!=null && (htag.length()>0))) out.w("</" + htag + ">");
  }

  /**
   * Print all modifiers associated with this slot
   */
  void slotModifiers(SlotDef slot, XWriter out, String htag)
  {
    if ((htag!=null) && (htag.length()>0)) out.w("<" + htag + ">");
    if (slot.isPublic())    out.w("<em>public</em> ");
    if (slot.isProtected()) out.w("<em>protected</em> ");
    if (slot.isPrivate())   out.w("<em>private</em> ");
    if (slot.isInternal())  out.w("<em>internal</em> ");
    if (slot.isStatic())    out.w("<em>static</em> ");
    if (slot.isAbstract())  out.w("<em>abstract</em> ");       // abstract implies virtual
    else if (slot.isAction())    out.w("<em>action</em> ");    // action implies virtual
    else if (slot.isVirtual())   out.w("<em>virtual</em> ");
    if (slot.isNative())    out.w("<em>native</em> ");
    if (slot.isOverride())  out.w("<em>override</em> ");
    if (slot.isConst())     out.w("<em>const</em> ");
    if (slot.isDefine())    out.w("<em>define</em> ");
    if (slot.isInline())    out.w("<em>inline</em> ");
    if (slot.isProperty())  out.w("<em>property</em> ");
    if ((htag!=null) && (htag.length()>0)) out.w("</" + htag + ">");
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  boolean isDoc(TypeDef t)
  {
    if (!t.isPublic()) return false;
    if (TypeUtil.isTestOnly(t)) return false;
    return true;
  }

  boolean isDoc(SlotDef slot)
  {
   if (slot.isPrivate()) return false;
   if (slot.isInternal()) return false;
   if (slot.synthetic) return false;
   return true;
  }

  /**
   * Parse the text into a DocNode array and write out
   * as HTMl markup.
   */
  void writeDoc(String doc, XWriter out)
  {
    DocParser.DocNode[] nodes = new DocParser(doc).parse();
    for (int i=0; i<nodes.length; i++)
    {
      int id = nodes[i].id();
      String text = nodes[i].text;
      switch (id)
      {
        case DocParser.DocNode.PARA:
          out.w("<p>").safe(text).w("</p>\n");
          break;
        case DocParser.DocNode.PRE:
          out.w("<pre class='doc'>").safe(text).w("</pre>\n");
          break;
        default:
          throw new IllegalStateException("Unknown DocNode id: " + id);
      }
    }
  }

  /**
   * Write out a Type as a hyperlink.
   */
  void typeLink(Type t, XWriter out) { typeLink(t, out, true); }
  void typeLink(Type t, XWriter out, boolean shorten)
  {
    if (t.isPrimitive())
    {
      out.w(t);
    }
    else if (t.isArray())
    {
      typeLink(t.arrayOf(), out, shorten);
      out.w("[]");
    }
    else if (!t.isPublic())
    {
      typeLink(t.base(), out, shorten);
    }
    else
    {
      String name = shorten ? t.name() : t.qname();
      String href = "../" + t.kit().name() + "/" + t.name() + ".html";      
      out.w("<a href='").w(href).w("'>").w(name).w("</a>");
    }
  }

//////////////////////////////////////////////////////////////////////////
// Comparators
//////////////////////////////////////////////////////////////////////////

  static class TypeComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      TypeDef a = (TypeDef)o1;
      TypeDef b = (TypeDef)o2;
      return a.name.compareTo(b.name);
    }
  }
  static TypeComparator typeCompare = new TypeComparator();

  static class SlotComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      SlotDef a = (SlotDef)o1;
      SlotDef b = (SlotDef)o2;

      // If types are the same, order by name string as usual
      if ( ((a instanceof FieldDef) && (b instanceof FieldDef)) ||
           ((a instanceof MethodDef) && (b instanceof MethodDef)) )
        return a.name.compareTo(b.name);

      // If types are different, FieldDef comes first
      if (a instanceof FieldDef) return -1;
      return 1;

      //return a.name.compareTo(b.name);
    }
  }
  static SlotComparator slotCompare = new SlotComparator();

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  KitDef kit;
  File dir;
  TypeDef[] types;
  boolean www;
}
