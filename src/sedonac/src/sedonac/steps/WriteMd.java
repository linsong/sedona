//
// Copyright (c) 2018 Sedona Community.
// Licensed under the Academic Free License version 3.0
//
// History:
//   27 Dec 18 divisuals  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import sedona.Env;
import sedona.manifest.*;
import sedona.util.*;
import sedona.xml.*;
import sedonac.*;
import sedonac.Compiler;
import sedonac.ast.*;
import sedonac.namespace.Type;
import sedonac.namespace.TypeUtil;
import sedonac.util.*;

/**
 * WriteMd generates the Markdown Sedona docs for the APIs
 * if the Compiler.md flag is set to true.
 */
public class WriteMd
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public WriteMd(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    if (!compiler.md) return;

    File outDir = compiler.outDir;
    if (outDir == null) outDir = new File(Env.home, "api");
    else outDir = new File(compiler.outDir, "api");

    this.www = compiler.www;
    this.kit = compiler.ast;
    this.dir = new File(outDir, kit.name);

    if (!kit.doc) return;

    log.info("  WriteMd [" + dir + "]");

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
    out.w("[![Sedona](../../logo.png)](/)\n");
    out.w("# ").w(title).w('\n');
    nav(out, title);
    out.w("\n---\n");
  }

  private void footer(XWriter out, String title)
  {
    out.w("\n---\n");
    nav(out, title);
  }

  public static void writeCopyright(XWriter out)
  {
    // out.w("<div class='copyright'><script type='text/javascript'>document.write(\"Copyright &#169; \" + new Date().getFullYear() + \" Tridium, Inc.\")</script></div>\n");
  }

  private void nav(XWriter out, String title)
  {
    if (www)
      wwwNav(out);
    else
      normNav(out, title);
  }

  private void normNav(XWriter out, String title)
  {
    boolean isKitLevel = title.equalsIgnoreCase(kit.name);
    String urlPrefix = isKitLevel ? "../.." : "../../..";
    out.w("[Doc Home]("+urlPrefix+") > ");
    out.w("[API Index]("+urlPrefix+"/api/api) > ");
    out.w("[").w(kit.name).w("]("+urlPrefix+"/api/").w(kit.name).w(")");
    if (isKitLevel)
      out.w(" > ").w(title);
    out.w("\n");
  }

  private void wwwNav(XWriter out)
  {
    // out.w("<div class='nav'>\n");
    // out.w("  <a href='../index.html'>Index</a> |\n");
    // out.w("  <a href='../api.html'>Kits</a> |\n");
    // out.w("  <a href='index.html'>").w(kit.name).w("</a>\n");
    // out.w("</div>\n");
  }

//////////////////////////////////////////////////////////////////////////
// Index
//////////////////////////////////////////////////////////////////////////

  private void index()
  {
    File f = new File(dir, "index.md");
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
    //out.w("<h1 class='title'>").w(kit.name).w("</h1>\n");
    //out.w("<ul>\n");
    if (kit.description!=null)
    {
      out.w(kit.description);
      out.w("\n\n");
    }

    for (int i=0; i<types.length; ++i)
    {
      TypeDef t = types[i];
      out.w("#### [").w(t.name).w("](").w(t.name).w(")\n");
    }
    out.w("\n");
    footer(out, kit.name);
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
    File f = new File(dir, t.name + ".md");
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
    header(out, t.name);

    // type details
    // out.w("<h1 class='title'>").w(t.qname).w("</h1>\n");
    // out.w("### ").w(t.qname).w("\n");

    // inheritance
    out.w("## Inheritance\n");
    // can't eliminate this HTML tag; MD doesn't allow hyperlinks within code
    out.w("<pre class='inheritance'>\n");
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

    // Print modifiers
    // can't eliminate this HTML tag; MD doesn't allow styling within code
    out.w("<code style='color:darkgreen;font-weight:bold;font-size:120%'>");
    typeModifiers(t, out);
    out.w(" class <b style='font-weight:bolder'>" + t.name() + "</b>  ");
    out.w("</code>");

    // Print facets
    // can't eliminate this HTML tag; MD doesn't allow styling within code
    if ((t.facets()!=null) && (!t.facets().isEmpty()))
    {
      out.w("<code style='color:darkblue;font-weight:bold'>\n");
      out.safe(t.facets().toString());
      out.w("</code>\n");
    }
    // type doc
    if (t.doc != null)
    {
      out.w("\n");
      writeMd(t.doc, out);
    }
    // slot details
    SlotDef[] slots = t.slotDefs();
    Arrays.sort(slots, slotCompare);
    boolean dl = false;
    boolean methodStart = false;
    for (int i=0; i<slots.length; i++)
    {
      SlotDef slot = slots[i];
      if (!isDoc(slot)) continue;
      if (!dl)
      {
        out.w("\n---\n");
        out.w("## Fields\n");
        out.w("<dl>\n");
        dl = true;
      }

      // Print name of method - except if cstr, print type name instead of _iInit
      String sname = slot.name;
      if (slot.isMethod())
      {
        if (!methodStart) // add method title first time
        {
          out.w("\n---\n");
          out.w("## Methods\n");
          methodStart = true;
        }
        MethodDef m = (MethodDef)slot;
        if (m.isInstanceInit()) sname = m.parent.name();
      }
      // out.w("<dt>").w(sname).w("</dt>\n");
      out.w("### ").w(sname).w("\n");

      // out.w("<dd>");

      // can't eliminate HTML tag; MD doesn't allow styling/ links within code
      out.w("<code>");
      slotDef(slot, out);
      out.w("</code>\n");

      if (slot.doc != null)
        writeMd(slot.doc, out);

      // out.w("</dd>\n");
    }
    if (dl) out.w("</dl>\n");

    footer(out, t.name);
  }

  /**
   * Generate the documentation for this SlotDef.
   */
  void slotDef(SlotDef slot, XWriter out)
  {
    if (slot.isField())
    {
      FieldDef f = (FieldDef)slot;
      slotModifiers(slot, out);
      out.w("**");
      typeLink(f.type(), out);
      out.w(" ").w(f.name());
      out.w("**");
    }
    else
    {
      MethodDef m = (MethodDef)slot;
      slotModifiers(slot, out);
      out.w("**");
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
      out.w("**");
    }

    // If any facets, print them last
    if ((slot.facets()!=null) && (!slot.facets().isEmpty()))
      out.safe(" " + slot.facets().toString());

  }


  /**
   * Print all modifiers associated with this type
   */
  void typeModifiers(TypeDef t, XWriter out)
  {
    if (t.isPublic())    out.w("public ");
    if (t.isInternal())  out.w("internal ");
    if (t.isAbstract())  out.w("abstract ");
    if (t.isConst())     out.w("const ");
    if (t.isFinal())     out.w("final ");
  }

  /**
   * Print all modifiers associated with this slot
   */
  void slotModifiers(SlotDef slot, XWriter out)
  {
    if (slot.isPublic())    out.w("_public_ ");
    if (slot.isProtected()) out.w("_protected_ ");
    if (slot.isPrivate())   out.w("_private_ ");
    if (slot.isInternal())  out.w("_internal_ ");
    if (slot.isStatic())    out.w("_static_ ");
    if (slot.isAbstract())  out.w("_abstract_ ");       // abstract implies virtual
    else if (slot.isAction())    out.w("_action_ ");    // action implies virtual
    else if (slot.isVirtual())   out.w("_virtual_ ");
    if (slot.isNative())    out.w("_native_ ");
    if (slot.isOverride())  out.w("_override_ ");
    if (slot.isConst())     out.w("_const_ ");
    if (slot.isDefine())    out.w("_define_ ");
    if (slot.isInline())    out.w("_inline_ ");
    if (slot.isProperty())  out.w("_property_ ");
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
   * as Markdown markup.
   */
  void writeMd(String doc, XWriter out)
  {
    DocParser.DocNode[] nodes = new DocParser(doc).parse();
    for (int i=0; i<nodes.length; i++)
    {
      int id = nodes[i].id();
      String text = nodes[i].text;
      switch (id)
      {
        case DocParser.DocNode.PARA:
          out.w("\n").w(text).w("\n");
          break;
        case DocParser.DocNode.PRE:
          out.w("\n```\n").w(text).w("\n```\n");
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
      String href = "/api/" + t.kit().name() + "/" + t.name();
      out.w("[").w(name).w("](").w(href).w(")");
    }
  }

//////////////////////////////////////////////////////////////////////////
// Comparators
//////////////////////////////////////////////////////////////////////////

  static class KitComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      KitManifest a = (KitManifest)o1;
      KitManifest b = (KitManifest)o2;
      return a.name.compareTo(b.name);
    }
  }
  public static final KitComparator KIT_COMPARE = new KitComparator();

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
