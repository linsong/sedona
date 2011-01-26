//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   16 Aug 07  Brian Frank  Creation
//

package sedona.util.sedonadev;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import sedona.util.FileUtil;
import sedona.xml.XParser;

/**
 * Utility to upload files to sedonadev.org.
 */
public final class Upload
{

  public static void main(String[] args)
  {        
    try
    {
      String baseUri = "http://sedonadev.org/download/upload";
      String vendor   = null;
      String password = null;
      int options = 0;
      
      // check args
      if (args.length < 1)
      {  
        printHelp();
        System.exit(-1);
      }
      
      // parse args
      for (int i=0; i<args.length; i++)
      {
        String arg = args[i];
        if (arg.charAt(0) == '-')
        {
          if (arg.equals("-?") || arg.equals("-help"))
          {
            printHelp();
            System.exit(-1);
          }
          else if (arg.equals("-u"))
          {
            if (i+1 >= args.length || args[i+1].charAt(0) == '-')
              err("ERROR: Missing uri argument");
            baseUri = args[++i];
            options += 2;
          }
          else if (arg.equals("-v"))
          {
            if (i+1 >= args.length || args[i+1].charAt(0) == '-') 
              err("ERROR: Missing vendor name argument");          
            vendor = args[++i];
            options += 2;
          }
          else if (arg.equals("-p"))
          {
            if (i+1 >= args.length || args[i+1].charAt(0) == '-') 
              err("ERROR: Missing password argument");          
            password = args[++i];
            options += 2;
          }
          else
          {
            System.out.println("WARNING: Unknown option " + arg);
            options++;
          }        
        }
      }

      // make sure file exists
      if (options >= args.length) err("ERRROR: Missing file argument");      
      File file = new File(args[options]);
      if (!file.exists()) err("ERROR: File not found: " + file);
      
      new Upload(baseUri, vendor, password, file).upload();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(-1);
    }    
  } 
  
  public Upload(String baseUri, String vendor, String password, File file)
  {
    this.baseUri = baseUri;
    this.vendor = vendor;
    this.password = password;
    this.file = file;
  }
  
  public void upload() throws Exception
  {
    parseFile();
    transfer();
  }
  
  private void parseFile() throws Exception
  {
    if (file.getName().endsWith(".zip"))
    {
      ext = ".zip";
      fileType = "build";
    }
    else if (file.getName().endsWith(".par"))
    {
      ext = ".zip"; // for Content-Type (platform archive is a zip file)
      fileType = "platform";
    }
    else if (file.getName().endsWith(".xml"))
    {
      ext = ".xml";
      String root = XParser.make(file).parse().name();
      if (root.equals("kitManifest") || root.equals("sedonaPlatform"))
        fileType = root;
    }
    
    if (fileType == null)
      err("ERROR: Unknown file type: " + file.getCanonicalPath());
  }
  
  protected void transfer() throws Exception
  {
    System.out.println("Upload " + fileType + " [" + file.getCanonicalPath() + "]");
    
    final String digest = makeDigest(vendor, password);
    URL url = new URL(baseUri + "?type=" + fileType + ";vendor=" + vendor + ";digest=" + digest);
    
    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    conn.setRequestMethod("POST");
    if (ext.equals(".zip")) conn.setRequestProperty("Content-Type", "application/zip");
    else if (ext.equals(".xml")) conn.setRequestProperty("Content-Type", "text/xml");
    else throw new IllegalStateException("Ext = " + ext);
    conn.setDoInput(true);
    conn.setDoOutput(true);
    conn.setUseCaches(false);

    InputStream fin = new BufferedInputStream(new FileInputStream(file));
    OutputStream out = new BufferedOutputStream(conn.getOutputStream());
    FileUtil.pipe(fin, out);
    out.flush();
    fin.close();
    
    byte[] buf = new byte[conn.getContentLength()];
    DataInputStream in = null;
    try
    {
      in = new DataInputStream(conn.getInputStream());
      in.readFully(buf);    
      String resp = new String(buf);      
      if (!resp.equals("Got it!"))
      {
        System.out.println("--- resp ---");
        System.out.println(resp);
        System.out.println("------------");
        throw new Exception("Unexpected response");
      }    
  
      System.out.println("*** Success! ***");
      System.out.println("");
    }
    finally
    {
      if (in != null) in.close();
    }
  }
  
  protected final String baseUri;
  protected final String vendor;
  protected final String password;
  protected final File file;
  
  private String ext;
  private String fileType;

  public static String makeDigest(String vendor, String password)
    throws Exception
  {
    // plain text
    String plain = vendor.toLowerCase() + ":" + password;
    byte[] bytes = plain.getBytes("UTF-8");
    
    // hash
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    md.update(bytes);
    byte[] digest = md.digest();
    
    // convert to hex
    StringBuffer hex = new StringBuffer();
    for (int i=0; i<digest.length; i++)
    {
      String b = Integer.toHexString(digest[i] & 0xff);
      if (b.length() == 1) hex.append("0");
      hex.append(b);
    }
    
    return hex.toString();
  }
  
  static void printHelp()
  {
    System.out.println("usage:");
    System.out.println("  upload [options] <file>");
    System.out.println("options:");
    System.out.println("  -u <uri>       The uri to upload to.");
    System.out.println("                 If omitted, http://sedonadev.org/download/upload is used.");
    System.out.println("  -v <vendor>    The vendor name used to authenticate");
    System.out.println("  -p <password>  The password used to authenticate");
    System.out.println("  -? -help       Print this usage synopsis");
    System.out.println("");
  }
  
  static void err(String msg)
  {
    System.out.println(msg);
    System.out.println("");
    System.exit(-1);
  }
  
}

