//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   29 May 08  Brian Frank  Creation
//

package sedonac.steps;

import java.io.*;
import java.util.*;
import sedona.xml.*;
import sedonac.Compiler;
import sedonac.*;

/**
 * CheckHtmlLinks reads in all the HTML files of the target
 * directory and verifies that hrefs all point to something.
 */
public class CheckHtmlLinks
  extends CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public CheckHtmlLinks(Compiler compiler)
  {
    super(compiler);
  }

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public void run()
  {
    this.dir = compiler.input.getParentFile();
    try
    {
      
      log.info("  CheckHtmlLinks [" + dir + "]");
      
      parseFiles();
      mapIds();       
      checkLinks();

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
      throw err("Cannot check html links", new Location(dir), e);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Parse XML
//////////////////////////////////////////////////////////////////////////

  private void parseFiles()
  {
    File[] files = dir.listFiles();    
    ArrayList acc = new ArrayList();
    for (int i=0; i<files.length; ++i)
    {
      File f = files[i];                      
      String name = f.getName().toLowerCase();
      if (!name.endsWith(".html")) continue;
      if (name.equals("api.html")) continue;
      
      log.debug("    Parse [" + f + "]");
      
      XElem xml = parseFile(f);
      if (xml == null) continue;
      
      Html html = new Html();
      html.file = f;
      html.xml = xml;
      acc.add(html);    
      byName.put(f.getName(), html);
    } 
    htmls = (Html[])acc.toArray(new Html[acc.size()]);
  }

  private XElem parseFile(File f)
  { 
    XElem xml = null;                            
    try
    {     
      return XParser.make(f).parse();
    }
    catch (XException e)
    {
      err(e);
      return null;
    }
    catch (Exception e)
    {
      err("Cannot parse HTML", new Location(f), e);
      return null;
    }
  }

//////////////////////////////////////////////////////////////////////////
// Map Ids
//////////////////////////////////////////////////////////////////////////

  private void mapIds()
  {              
    for (int i=0; i<htmls.length; ++i)
      mapIds(htmls[i], htmls[i].xml);
  }   
  
  private void mapIds(Html html, XElem elem)
  {  
    // check for id attribute
    String id = elem.get("id", null);
    if (id != null)
    {                                   
      XElem dup = (XElem)html.ids.put(id, elem);
      if (dup != null)
        err("Duplicate id '" + id + "' (line " + dup.line() + ")", new Location(elem));
    }                        
    
    // recurse
    XElem[] kids = elem.elems();
    for (int i=0; i<kids.length; ++i)
      mapIds(html, kids[i]);
  }

//////////////////////////////////////////////////////////////////////////
// Check Links
//////////////////////////////////////////////////////////////////////////

  private void checkLinks()
  {
    for (int i=0; i<htmls.length; ++i)
      checkLinks(htmls[i], htmls[i].xml);
  }   
  
  private void checkLinks(Html html, XElem elem)
  {             
    String href = elem.get("href", null);
    if (href != null)
      checkLink(html, elem, href);     
    
    // recurse
    XElem[] kids = elem.elems();
    for (int i=0; i<kids.length; ++i)
      checkLinks(html, kids[i]);
  }

  private void checkLink(Html html, XElem elem, String href)
  {                                  
    String filename = href;
    String frag = null;    
    
    if (href.startsWith("http:")) return;
    
    int pound = href.indexOf('#');
    if (pound >= 0)
    {
      filename = href.substring(0, pound);
      frag = href.substring(pound+1);
    }                       
    
    if (filename.equals(""))
      filename = html.file.getName();
    
    Html targetHtml = (Html)byName.get(filename);
    if (targetHtml == null)    
    {                                     
      File f = new File(dir, filename);
      if (!f.exists())
      {
        // Check if target exists when resolved against output directory
        f = new File(compiler.outDir, filename);
        if (!f.exists())
        {
          if (filename.startsWith("../"))
            warn("Cannot resolve relateive href target '" + href + "'", new Location(elem));
          else
            err("Unknown href target '" + href + "'", new Location(elem));
        }
        return;                                                     
      }      
      
      try
      {
        if (!filename.endsWith(f.getCanonicalFile().getName()))
        {
          err("Invalid case for href target '" + href + "'", new Location(elem));
          return;                                                         
        }
      }
      catch (Exception e)
      {
        err("Internal error '" + href + "'", new Location(elem));
        e.printStackTrace();
      }
    } 
    
    if (frag != null && targetHtml != null)
    {
      XElem targetElem = (XElem)targetHtml.ids.get(frag);
      if (targetElem == null)    
      {
        err("Unknown href target '" + href + "'", new Location(elem));
        return;
      } 
    }
  }

//////////////////////////////////////////////////////////////////////////
// Html
//////////////////////////////////////////////////////////////////////////

  static class Html
  {
    File file;
    XElem xml;
    HashMap ids = new HashMap();  // String -> XElem
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////
  
  File dir;
  HashMap byName = new HashMap(); // file name -> Html
  Html[] htmls;

}
