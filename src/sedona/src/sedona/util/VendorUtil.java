//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//  03 June 09  Matthew Giannini  Creation
//
package sedona.util;

/**
 * VendorUtil provides utility functions for working with vendors.
 */
public class VendorUtil
{
  /**
   * Checks the given vendor name against the following rules.  If any of the
   * rules are broken, an Exception is thrown.
   * <ul>
   * <li>vendor name is alphanumeric
   * <li>0 < vendor name length <= 32
   * </ul>
   * 
   * @param vendor the vendor name to check
   * @throws Exception Thrown if the vendor fails any of the checks.
   */
  public static void checkVendorName(final String vendor) throws Exception
  {
    // vendor name is an alphanumeric text string and must be less than 32
    if (vendor.length() == 0) throw new Exception("Invalid vendor name '" + vendor + "' (size is zero)");
    if (vendor.length() > 32) throw new Exception("Invalid vendor name '" + vendor + "' (size > 32 chars)");
    for (int i=0; i<vendor.length(); ++i)
    {
      int c = vendor.charAt(i);
      if ('A' <= c && c <= 'Z') continue;                                          
      if ('a' <= c && c <= 'z') continue;                                          
      if ('0' <= c && c <= '9') continue;                                          
      throw new Exception("Invalid vendor name '" + vendor + "' (must be allphanumeric)");
    }
  }
  
  /**
   * Checks the given platformId to make sure that it is prefixed with {@code
   * (vendor.toLowerCase()+ "-")}. An exception is thrown if the check fails.
   * 
   * @param vendor
   *          the vendor name
   * @param platformId
   *          the platform id. If this value is {@code null}, then no checking
   *          is done.
   * @throws Exception
   *           Thrown if the check fails.
   */
  public static void checkPlatformPrefix(final String vendor, final String platformId)
    throws Exception
  {
    if (platformId == null) return;
    
    final String requiredPrefix = vendor.toLowerCase() + "-";
    if (!platformId.startsWith(requiredPrefix))
      throw new Exception("Platform id must be prefixed with '" + requiredPrefix + "' (case-sensitive)");
  }

}
