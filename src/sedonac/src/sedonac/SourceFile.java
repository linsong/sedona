//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   30 May 07  Brian Frank  Creation
//

package sedonac;

import java.io.*;

/**
 * SourceFile models one input source file.
 */
public class SourceFile
{
  public String toString()
  {
    return file.toString();
  }

  public File file;
  public boolean testOnly;
}
