//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Mar 07  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import sedona.kit.*;
import sedona.manifest.*;
import sedona.util.*;
import sedona.xml.*;
import sedonac.Compiler;
import sedonac.*;

/**
 * TableOfContents reads a toc.xml file and generates an index.html
 * with the contents and updates the navigation bars of the .html
 * documentation files in the same directory.
 */
public class TableOfContents
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public TableOfContents(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    try
    {                 
      this.www = compiler.www;
      this.xml = compiler.xml;
      this.tocFile = compiler.input;
      this.dir = tocFile.getParentFile(); 
      
      outDir = compiler.outDir;
      if (outDir == null) outDir = dir;    
      if (!outDir.exists()) outDir.mkdirs();
      
      new File(outDir, "api.html").delete();

      log.info("  TableOfContents [" + dir + " -> " + outDir + "]");

      parseXml();
      flattenChapters();
      processHtml();
      writeDocIndex();
      writeApiIndex();   
      if (www) copyResources();

      quitIfErrors();
    }
    catch (XException e)
    {
      throw err(e);
    }
    catch (CompilerException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw err("Cannot generate docs", new Location(tocFile), e);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Parse XML
//////////////////////////////////////////////////////////////////////////

  private void parseXml()
  {
    XElem[] xsections = xml.elems("section");

    sections = new Section[xsections.length];
    for (int i=0; i<xsections.length; ++i)
      sections[i] = toSection(xsections[i]);
  }

  private Section toSection(XElem xml)
  {
    XElem[] xchapters = xml.elems("chapter");

    Section s = new Section();
    s.name = xml.get("name");
    s.chapters = new Chapter[xchapters.length];
    for (int i=0; i<xchapters.length; ++i)
      s.chapters[i] = toChapter(xchapters[i]);

    return s;
  }

  private Chapter toChapter(XElem xml)
  {
    Chapter c = new Chapter();
    c.name  = xml.get("name");
    c.href  = xml.get("href");
    c.blurb = xml.get("blurb");
    return c;
  }

//////////////////////////////////////////////////////////////////////////
// Flatten Chapters
//////////////////////////////////////////////////////////////////////////

  private void flattenChapters()
  {
    ArrayList acc = new ArrayList();

    for (int i=0; i<sections.length; ++i)
    {
      Section s = sections[i];
      for (int j=0; j<s.chapters.length; ++j)
        acc.add(s.chapters[j]);
    }

    chapters = (Chapter[])acc.toArray(new Chapter[acc.size()]);
    chaptersByHref = new HashMap();

    for (int i=0; i<chapters.length; ++i)
    {
      Chapter c = chapters[i];
      if (i > 0) c.prev = chapters[i-1];
      if (i+1 < chapters.length) c.next = chapters[i+1];
      chaptersByHref.put(c.href, c);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Process HTML
//////////////////////////////////////////////////////////////////////////

  private void processHtml()
  {
    File[] files = dir.listFiles();
    for (int i=0; i<files.length; ++i)
    {
      File f = files[i];
      String name = f.getName();
      if (!name.endsWith(".html")) continue;
      if (name.equals("index.html")) continue;
      try
      {
        processHtml(f);
      }
      catch (Exception e)
      {                                                    
        e.printStackTrace();
        throw err("Cannot process file", new Location(f), e);
      }
    }
  }

  private void processHtml(File f)
    throws Exception
  {
    log.debug("    " + f);

    // map file to chapter
    Chapter chapter = (Chapter)chaptersByHref.get(f.getName());
    if (chapter == null)
    {                      
      if (!f.getName().equals("api.html"))
        err("Chapter not mapped in toc.xml", new Location(f));
      return;
    }
    chapter.used = true;

    // read file into memory
    String[] lines = FileUtil.readLines(f);

    // rewrite the HTML file but replace the header
    // and footer
    XWriter out = new XWriter(new File(outDir, f.getName()));
    try
    {
      boolean skip = false;
      boolean didHeader = false, didFooter = false;
      for (int i=0; i<lines.length; ++i)
      {
        String line = lines[i];
        if (line.indexOf("TOC-HEADER-START") >= 0)
        {
          writeHeader(out, chapter);
          skip = true;
        }
        else if (line.indexOf("TOC-HEADER-END") >= 0)
        {
          skip = false;
          didHeader = true;
        }
        else if (line.indexOf("TOC-FOOTER-START") >= 0)
        {
          writeFooter(out, chapter);
          skip = true;
        }
        else if (line.indexOf("TOC-FOOTER-END") >= 0)
        {
          skip = false;
          didFooter = true;
        }
        else
        {
          if (!skip)
          {
            out.w(line);
            out.w("\n");
          }
        }
      }

      if (!didHeader || !didFooter)
        err("Missing header/footer comments", new Location(f));
    }
    finally
    {
      out.close();
    }
  }


////////////////////////////////////////////////////////////////
// Doc Index
////////////////////////////////////////////////////////////////

  private void writeDocIndex()
  {
    File f = new File(outDir, "index.html");
    XWriter out = null;
    try
    {
      out = new XWriter(f);
      writeDocIndex(out);
    }
    catch (Exception e)
    {
      throw err("Cannot write doc index", new Location(f));
    }
    finally
    {
      try { out.close(); } catch (Exception e) {}
    }
  }

  private void writeDocIndex(XWriter out)
  {
    out.w("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n");
    out.w("  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
    out.w("\n");
    out.w("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
    out.w("<!-- Auto-generated by sedonac -->\n");
    out.w("<head>\n");
    out.w("  <title>Documentation</title>\n");
    out.w("  <meta http-equiv='Content-type' content='text/html;charset=UTF-8' />\n");
    out.w("  <link rel='stylesheet' type='text/css' href='style.css'/>\n");
    out.w("</head>\n");
    out.w("<body>\n"); 
    
    header(out);     
    //nav(out, null, null);

    out.w("  <h1>API</h1>\n");
    out.w("  <ul>\n");
    out.w("    <li><b><a href='api.html'>API Index</a></b>: API for each kit</li>\n");
    out.w("  </ul>\n");
    
    for (int i=0; i<sections.length; ++i)
    {
      Section section = sections[i];
      out.w("\n");
      out.w("  <h1>" + section.name + "</h1>\n");
      out.w("  <ul>\n");
      for (int j=0; j<section.chapters.length; ++j)
      {
        Chapter chapter = section.chapters[j];
        out.w("  <li><b><a href='" + chapter.href + "'>" +
           chapter.name + "</a></b>: " +
          chapter.blurb + "</li>\n");  // should check XML escape
      }
      out.w("  </ul>\n");
    }

    //nav(out, null, null);
    writeCopyright(out);
    out.w("</body>\n");
    out.w("</html>\n");
  }

////////////////////////////////////////////////////////////////
// API Index
////////////////////////////////////////////////////////////////

  private void writeApiIndex()
  {
    File f = new File(outDir, "api.html");
    XWriter out = null;
    try
    {
      out = new XWriter(f);
      writeApiIndex(out);
    }
    catch (Exception e)
    {
      throw err("Cannot write api index", new Location(f));
    }
    finally
    {
      try { out.close(); } catch (Exception e) {}
    }
  }

  private void writeApiIndex(XWriter out)
  {
    out.w("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n");
    out.w("  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
    out.w("\n");
    out.w("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
    out.w("<!-- Auto-generated by sedonac -->\n");
    out.w("<head>\n");
    out.w("  <title>API Index</title>\n");
    out.w("  <meta http-equiv='Content-type' content='text/html;charset=UTF-8' />\n");
    out.w("  <link rel='stylesheet' type='text/css' href='style.css'/>\n");
    out.w("</head>\n");
    out.w("<body>\n");
    
    header(out);
    nav(out, null, null);
    
    out.w("<h1>Kits</h1>\n");
    out.w("<ul>\n");
    String[] kits = KitDb.kits();
    for (int i=0; i<kits.length; ++i)
    {                             
      try
      {
        KitManifest km = ManifestDb.loadForLocalKit(kits[i]);
        if (km.doc)
          out.w("  <li><b><a href='" + km.name + "/index.html'>" + km.name + "</a></b></li>\n");
      }
      catch (Exception e)
      {
        System.out.println("ERROR: " + e);
      }
    }
    out.w("</ul>\n");

    nav(out, null, null);
    writeCopyright(out);
    out.w("</body>\n");
    out.w("</html>\n");
  }

//////////////////////////////////////////////////////////////////////////
// Copy Resources
//////////////////////////////////////////////////////////////////////////

  private void copyResources()
  {
    copyResources(".css");
    copyResources(".png");
  }

  private void copyResources(String ext)
  {                                    
    File[] files = dir.listFiles();
    for (int i=0; i<files.length; ++i)
      if (files[i].getName().endsWith(ext))    
      {
        try
        {
          FileUtil.copy(files[i], new File(outDir, files[i].getName()), compiler.log);
        }
        catch (IOException e)
        {
          throw err("Cannot copy file", new Location(files[i]));
        }
      }
  }

//////////////////////////////////////////////////////////////////////////
// HTML
//////////////////////////////////////////////////////////////////////////

  private void writeHeader(XWriter out, Chapter chapter)
  {
    out.w("<!-- TOC-HEADER-START -->\n");
    out.w("<!-- Auto-generated by sedonac -->\n");
    out.w("<head>\n");
    out.w("  <title>" + chapter.name + "</title>\n");
    out.w("  <meta http-equiv='Content-type' content='text/html;charset=UTF-8' />\n");
    out.w("  <link rel='stylesheet' type='text/css' href='style.css'/>\n");
    out.w("</head>\n");
    out.w("<body>\n");
    
    header(out);
    nav(out, chapter);
    
    out.w("<h1 class='title'>" + chapter.name + "</h1>\n");
    out.w("<div class='content'>\n");
    out.w("<!-- TOC-HEADER-END -->\n");
  }

  private void writeFooter(XWriter out, Chapter chapter)
  {
    out.w("<!-- TOC-FOOTER-START -->\n");
    out.w("<!-- Auto-generated by sedonac -->\n");
    out.w("</div>\n");
    nav(out, chapter);
    writeCopyright(out);
    out.w("</body>\n");
    out.w("<!-- TOC-FOOTER-END -->\n");
  }
  
  private void writeCopyright(XWriter out)
  {
    WriteDoc.writeCopyright(out);
  }

////////////////////////////////////////////////////////////////
// Header
////////////////////////////////////////////////////////////////

  private void header(XWriter out)
  {
    String home = www ? "../index.html" : "index.html";
    
    out.w("<p>\n");
    out.w("  <a href='").w(home).w("'>\n");
    out.w("    <img src='logo.png' alt='Sedona'/>\n");
    out.w("  </a>\n");
    out.w("</p>\n");             

    if (www)
    {
      out.w("<ul class='tabs'>\n");
      out.w("  <li><a href='").w(home).w("'>Home</a></li>\n");
      out.w("  <li><a class='active' href='index.html'>Documentation</a></li>\n");
      out.w("  <li><a href='../community.html'>Community</a></li>\n");
      out.w("  <li><a href='/download/'>Downloads</a></li>\n");      
      out.w("  <li><a href='../forum.html'>Forum</a></li>\n");
      out.w("</ul>\n");    
    }
  }
  
////////////////////////////////////////////////////////////////
// Navigation
////////////////////////////////////////////////////////////////

  private void nav(XWriter out, Chapter chapter)
  {
    nav(out, 
        chapter.prev == null ? null : chapter.prev.href,
        chapter.next == null ? null : chapter.next.href);
  }

  private void nav(XWriter out, String prev, String next)
  {
    if (www)
      wwwNav(out, prev, next);
    else
      normNav(out, prev, next);
  }
  
  private void normNav(XWriter out, String prev, String next)
  {
    out.w("<div class='nav'>\n");
    out.w("  <a href='index.html'>Index</a>\n");
    if (prev != null) out.w(" | <a href='" + prev + "'>Prev</a>\n");
    if (next != null) out.w(" | <a href='" + next + "'>Next</a>\n");
    out.w("</div>\n");
  }

  private void wwwNav(XWriter out, String prev, String next)
  {
    out.w("<div class='nav'>\n");
    out.w("  <a href='index.html'>Index</a>\n");
    if (prev != null) out.w(" | <a href='" + prev + "'>Prev</a>\n");
    if (next != null) out.w(" | <a href='" + next + "'>Next</a>\n");
    out.w("</div>\n");
  }

//////////////////////////////////////////////////////////////////////////
// DOM
//////////////////////////////////////////////////////////////////////////

  static class Section
  {
    String name;
    Chapter[] chapters;
  }

  static class Chapter
  {
    String name;
    String href;
    String blurb;
    Chapter prev;
    Chapter next;
    boolean used;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////
  
  boolean www;
  File tocFile;
  File dir;
  File outDir;
  XElem xml;
  Section[] sections;
  Chapter[] chapters;
  HashMap chaptersByHref;
}
