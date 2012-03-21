//
//Copyright (c) 2006 Tridium, Inc.
//Licensed under the Academic Free License version 3.0
//
//History:
//4 Dec 07  Brian Frank  Creation
//

package sedona.sox;

import sedona.util.*;

/**
* KitVersion is a kit name, checksum, version tuple.
*/
public class KitVersion
{ 
        
public KitVersion(String name, int checksum, Version version)
{
 this.name      = name;
 this.checksum  = checksum;
 this.version   = version;
}           

public String toString()
{
 return TextUtil.padRight(name, 16) + " " + 
       TextUtil.padRight(version.toString(), 8) + " 0x" + 
       Integer.toHexString(checksum);
}

public final String name;
public final int checksum;
public final Version version;
}
