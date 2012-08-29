//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Mar 07  Brian Frank  Creation
//

package sedonac.namespace;

import sedona.Facets;

/**
 * Slot is the interace for classes which represent a slot
 * at compile time such as IrSlot or SlotDef.
 *
 * Not to be confused with sedona.Slot used to represent slots
 * in Java at runtime.
 */
public interface Slot
{

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /**
   * Get the parent Type of this slot.
   */
  public Type parent();

  /**
   * Get the simple name of this slot.
   */
  public String name();

  /**
   * Get the fully qualified name of this slot such as "sys::Component.name".
   */
  public String qname();

  /**
   * Get facets metadata.
   */
  public Facets facets();

  /**
   * Is this a Field slot.
   */
  public boolean isField();

  /**
   * Is this a Method slot.
   */
  public boolean isMethod();

  /**
   * Is this slot inherited, routed to TypeUtil.isInherited()
   */
  public boolean isInherited(Type into);

  /**
   * Return if this slot is an action or property.
   */
  public boolean isReflective();

//////////////////////////////////////////////////////////////////////////
// Runtime Flags
//////////////////////////////////////////////////////////////////////////

  // These flags must be kept in sync with the Slot.sedona definitions

  public static final int RT_ACTION   = sedona.Slot.ACTION;
  public static final int RT_CONFIG   = sedona.Slot.CONFIG;
  public static final int RT_AS_STR   = sedona.Slot.AS_STR;
  public static final int RT_OPERATOR = sedona.Slot.OPERATOR;

  public int rtFlags();
  public boolean isRtAction();
  public boolean isRtConfig();

//////////////////////////////////////////////////////////////////////////
// Flags
//////////////////////////////////////////////////////////////////////////

  public static final int ABSTRACT  = 0x0001;
  public static final int ACTION    = 0x0002;
  public static final int CONST     = 0x0004;
  public static final int DEFINE    = 0x0008;
  public static final int INLINE    = 0x0010;
  public static final int INTERNAL  = 0x0020;
  public static final int NATIVE    = 0x0040;
  public static final int OVERRIDE  = 0x0080;
  public static final int PUBLIC    = 0x0100;
  public static final int PRIVATE   = 0x0200;
  public static final int PROPERTY  = 0x0400;
  public static final int PROTECTED = 0x0800;
  public static final int STATIC    = 0x1000;
  public static final int VIRTUAL   = 0x2000;

  public int flags();
  public boolean isAbstract();
  public boolean isAction();
  public boolean isConst();
  public boolean isDefine();
  public boolean isInline();
  public boolean isInternal();
  public boolean isNative();
  public boolean isOverride();
  public boolean isStatic();
  public boolean isPrivate();
  public boolean isProperty();
  public boolean isProtected();
  public boolean isPublic();
  public boolean isVirtual();

}
