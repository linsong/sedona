//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Mar 07  Brian Frank  Creation
//

package sedonac.namespace;

import sedonac.ast.*;

/**
 * Field is the interace for classes which represent a field
 * such as IrField or FieldDef.
 */
public interface Field
  extends Slot
{

  /**
   * Get the type of the field.
   */
  public Type type();

  /**
   * If a define field return literal expr.
   */
  public Expr.Literal define();

  /**
   * Index of constructor parameter used to specify
   * the length of this field's unsized array.
   */
  public int ctorLengthParam();



}
