//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   7 Jun 07  Brian Frank  Creation
//

package sedona.offline;

import java.io.*;
import sedona.*;
import sedona.util.*;
import sedona.xml.*;

/**
 * OfflineLink models an execution relationship between two
 * slots in an offline application database.
 */
public class OfflineLink
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   */
  public OfflineLink(OfflineComponent fromComp, Slot fromSlot,
                     OfflineComponent toComp, Slot toSlot)
  {
    this.fromComp = fromComp;
    this.fromSlot = fromSlot;
    this.toComp   = toComp;
    this.toSlot   = toSlot;
  }

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  /**
   * Equals is determined by the == operator.
   */
  public final boolean equals(Object obj)
  {
    return this == obj;
  }

  /**
   * Check that this link matches that link.
   */
  public boolean equivalent(OfflineLink that)
  {                              
    if (fromComp.id < 0 || toComp.id < 0)
      return this.fromComp  == that.fromComp &&
             this.fromSlot  == that.fromSlot &&
             this.toComp.id == that.toComp.id &&
             this.toSlot.id == that.toSlot.id;
    else
      return this.fromComp.id == that.fromComp.id &&
             this.fromSlot.id == that.fromSlot.id &&
             this.toComp.id   == that.toComp.id &&
             this.toSlot.id   == that.toSlot.id;
  }

  /**
   * To string.
   */
  public String toString()
  {
    return fromComp.path() + "." + fromSlot.name + " -> " +
           toComp.path() + "." + toSlot.name;
  }

//////////////////////////////////////////////////////////////////////////
// XML IO
//////////////////////////////////////////////////////////////////////////

  /**
   * Encode this link to XML format.
   */
  public void encodeXml(XWriter out)
  {
    out.w("  <link ")
       .attr("from", fromComp.path() + "." + fromSlot.name).w(" ")
       .attr("to", toComp.path() + "." + toSlot.name)
       .w("/>\n");
  }

  /**
   * Decode an XML <link> element.
   */
  public static OfflineLink decodeXml(OfflineApp app, XElem xml)
    throws Exception
  {
    String fromSig = xml.get("from");
    String toSig   = xml.get("to");

    // parse
    int fromDot         = fromSig.indexOf('.');
    int toDot           = toSig.indexOf('.');
    if (fromDot <= 0) throw new XException("Invalid from component/slot path: " + fromSig, xml);
    if (toDot <= 0) throw new XException("Invalid to component/slot path: " + toSig, xml);

    String fromPath     = fromSig.substring(0, fromDot);
    String toPath       = toSig.substring(0, toDot);
    String fromSlotName = fromSig.substring(fromDot+1);
    String toSlotName   = toSig.substring(toDot+1);

    OfflineComponent from = app.lookup(fromPath);
    OfflineComponent to   = app.lookup(toPath);
    if (from == null) throw new XException("Unknown from component: " + fromPath, xml);
    if (to == null) throw new XException("Unknown to component: " + toPath, xml);

    Slot fromSlot = from.slot(fromSlotName);
    Slot toSlot = to.slot(toSlotName);
    if (fromSlot == null) throw new XException("Unknown from slot: " + from.type+"."+fromSlotName, xml);
    if (toSlot == null) throw new XException("Unknown to slot: " + to.type+"."+toSlotName, xml);

    return new OfflineLink(from, fromSlot, to, toSlot);
  }

//////////////////////////////////////////////////////////////////////////
// Binary IO
//////////////////////////////////////////////////////////////////////////

  /**
   * Save to binary app format.
   */
  public void encodeBinary(Buf out)
  {
    out.u2(fromComp.id);
    out.u1(fromSlot.id);
    out.u2(toComp.id);
    out.u1(toSlot.id);
  }

  /**
   * Decode from the binary app format.
   */
  public static OfflineLink decodeBinary(OfflineApp app, int fromCompId, Buf in)
    throws Exception
  {
    int fromSlotId = in.u1();
    int toCompId   = in.u2();
    int toSlotId   = in.u1();

    OfflineComponent from = app.lookup(fromCompId);
    OfflineComponent to   = app.lookup(toCompId);
    if (from == null) throw new IOException("Invalid link from comp id: " + fromCompId);
    if (to   == null) throw new IOException("Invalid link to comp id: " + toCompId);

    Slot fromSlot = from.type.slot(fromSlotId);
    Slot toSlot   = to.type.slot(toSlotId);
    if (fromSlot == null) throw new IOException("Invalid link from slot id: " + fromSlotId);
    if (toSlot   == null) throw new IOException("Invalid link to slot id: " + toSlotId);

    return new OfflineLink(from, fromSlot, to, toSlot);
  }


//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public OfflineComponent fromComp;
  public Slot fromSlot;
  public OfflineComponent toComp;
  public Slot toSlot;

}
