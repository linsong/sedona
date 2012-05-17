//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 June 09  Matthew Giannini  Creation
//
package sedona.platform;

import sedona.xml.*;

/**
 * NativeManifest represents the Native method information stored in a
 * PlatformManifest.
 */
public class NativeManifest
{
  public NativeManifest(PlatformManifest platform, String qname, String nativeId)
  {
    this.platform = platform;
    this.qname = qname;
    this.nativeId = nativeId;
  }
  
//////////////////////////////////////////////////////////////////////////
// XML
//////////////////////////////////////////////////////////////////////////
  
  public void encodeXml(XWriter out)
  {
    out.w("  <nativeMethod ")
      .attr("qname", qname).w(" ")
      .attr("id", nativeId).w(" />\n");
  }
  
  public static NativeManifest decodeXml(PlatformManifest platform, XElem xml)
  {
    final String qname = xml.get("qname");
    final String nativeId = xml.get("id");
    return new NativeManifest(platform, qname, nativeId);
  }
  
//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final PlatformManifest platform;
  public final String qname;
  public final String nativeId;

}
