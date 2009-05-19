//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   17 Nov 08  Brian Frank  Creation
//

package sedona.vm.sys;

import java.io.*;
import sedona.vm.*;

/**
 * sys::FileStore native methods
 */              
public class FileStore_n
{

  public static int doSize(StrRef name)             
  {            
    return (int)toFile(name).length();
  }
  
  public static Object doOpen(StrRef name, StrRef m)
  {                    
    try
    {
      File f = toFile(name);        
      String mode = m.toString();
      if (mode.equals("w")) mode = "rw";
      if (mode.equals("m")) mode = "rw";
      return new RandomAccessFile(f, mode);
    }
    catch (IOException e)
    {
      System.out.println("File_n.doOpen: " + e); 
      return null;
    }
  }
  
  public static int doRead(Object fp)
  {          
    try
    {
      return ((RandomAccessFile)fp).read();
    }
    catch (IOException e)
    {
      System.out.println("File_n.doRead: " + e); 
      return -1;
    }
  }
  
  public static int doReadBytes(Object fp, byte[] b, int off, int len)  
  { 
    try
    {                                   
      return ((RandomAccessFile)fp).read(b, off, len);
    }
    catch (IOException e)
    {
      System.out.println("File_n.doReadBytes: " + e); 
      return 0;
    }
  }
  
  public static byte doWrite(Object fp, int b)
  {
    try
    {
      ((RandomAccessFile)fp).write(b);
      return 1;
    }
    catch (IOException e)
    {
      System.out.println("File_n.doWrite: " + e); 
      return 0;
    }
  }
  
  public static byte doWriteBytes(Object fp, byte[] b, int off, int len)
  {
    try
    {
      ((RandomAccessFile)fp).write(b, off, len);
      return 1;
    }
    catch (IOException e)
    {
      System.out.println("File_n.doWriteBytes: " + e); 
      return 0;
    }
  }
  
  public static byte doSeek(Object fp, int pos)
  {           
    try
    {
      ((RandomAccessFile)fp).seek(pos);
      return 1;
    }
    catch (IOException e)
    {
      System.out.println("File_n.doSeek: " + e); 
      return 0;
    }
  }
  
  public static int doTell(Object fp)
  {
    try
    {
      return (int)((RandomAccessFile)fp).getFilePointer();
    }
    catch (IOException e)
    {
      System.out.println("File_n.doTell: " + e); 
      return 0;
    }
  }
  
  public static void doFlush(Object fp)
  {                     
    try
    {
      ((RandomAccessFile)fp).getFD().sync();
    }
    catch (IOException e)
    {
      System.out.println("File_n.doFlush: " + e);
    }
  }
  
  public static byte doClose(Object fp)
  {
    try
    {
      ((RandomAccessFile)fp).close();
      return 1;
    }
    catch (IOException e)
    {
      System.out.println("File_n.doClose: " + e);
      return 0;
    }
  }
 
  static File toFile(StrRef name)
  {
    return new File(name.toString());
  }
  

  public static boolean rename(StrRef from, StrRef to)
  {      
    File fromFile = new File(from.toString());
    File toFile = new File(to.toString());
    return fromFile.renameTo(toFile);
  }
  
}

