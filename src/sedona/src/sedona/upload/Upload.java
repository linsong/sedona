//
// Copyright (c) 2008 Tridium, Inc.
//
// History:
//   23 Oct 08  Andy Frank  Creation
//

package sedona.upload;

import java.io.*;
import java.net.*;
import java.security.*;
import sedona.util.*;

/**
 * Utility to upload files to sedonadev.org.
 */
public class Upload
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

      // type off of file extension
      if (file.getName().endsWith(".zip"))      upload(baseUri, "build", vendor, password, file);
      else if (file.getName().endsWith(".xml")) upload(baseUri, "kitManifest", vendor, password, file);
      else err("ERROR: Unknown file type: " + file.getCanonicalPath());
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(-1);
    }    
  }    
  

  static void upload(String baseUri, String type, String vendor, String password, File file)
    throws Exception
  {    
    System.out.println("Upload " + type + " [" + file.getCanonicalPath() + "]");
    
    String digest = makeDigest(vendor, password);
    URL url = new URL(baseUri + "?type=" + type + ";vendor=" + vendor + ";digest=" + digest);

    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    conn.setRequestMethod("POST");
    if (type.equals("build"))            conn.setRequestProperty("Content-Type", "application/zip");
    else if (type.equals("kitManifest")) conn.setRequestProperty("Content-Type", "text/xml");
    conn.setDoInput(true);
    conn.setDoOutput(true);
    conn.setUseCaches(false);
    
    InputStream fin = new BufferedInputStream(new FileInputStream(file));
    OutputStream out = new BufferedOutputStream(conn.getOutputStream());
    FileUtil.pipe(fin, out);
    out.flush();
    fin.close();
    
    byte[] buf = new byte[conn.getContentLength()];
    DataInputStream in = new DataInputStream(conn.getInputStream());
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
  
//  static final String baseUri = "http://209.96.240.217/download/upload";
  //static final String baseUri = "http://localhost/download/upload";

}

