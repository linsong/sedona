//
// This code licensed to public domain
//
// History:
//   2 Jul 07  Brian Frank  Creation
//

package sedona.util;

import java.lang.reflect.*;

/**
 * ArrayUtil
 */
public class ArrayUtil
{

  /**
   * Return the concatentation of the two arrays.
   */
  public static Object[] concat(Object[] a, Object[] b)
  {
    if (a == null || a.length == 0) return b;
    if (b == null || b.length == 0) return a;

    Class cls = a.getClass().getComponentType();
    Object[] temp = (Object[])Array.newInstance(cls, a.length+b.length);
    System.arraycopy(a, 0, temp, 0, a.length);
    System.arraycopy(b, 0, temp, a.length, b.length);
    return temp;
  }

  /**
   * Increase the size of the array by one element
   * and append the specified element.
   */
  public static int[] addOne(int[] array, int elem)
  {
    int[] temp = new int[array.length+1];
    System.arraycopy(array, 0, temp, 0, array.length);
    temp[array.length] = elem;
    return temp;
  }

  /**
   * Remove the specified element and return an array
   * that is one element shorter.  If the elem isn't
   * found in the array then return array unchanged.
   */
  public static int[] removeOne(int[] array, int elem)
  {
    if (array.length == 0) return array;

    int[] temp = new int[array.length-1];
    int len = array.length;
    for(int i=0; i<len; ++i)
    {
      if (array[i] == elem)
      {
        System.arraycopy(array, 0, temp, 0, i);
        System.arraycopy(array, i+1, temp, i, len-i-1);
        return temp;
      }
    }

    return array;
  }

  /**
   * Swap the integers at indices a and b.  Return array.
   */
  public static int[] swap(int[] array, int a, int b)
  {
    int temp = array[a];
    array[a] = array[b];
    array[b] = temp;
    return array;
  }

  /**
   * Return string format.
   */
  public static String toString(int[] array)
  {
    StringBuffer s = new StringBuffer();
    for (int i=0; i<array.length; ++i)
    {
      if (i > 0) s.append(",");
      s.append(array[i]);
    }
    return s.toString();
  }

  /**
   * Return hex string.
   */
  public static String toHex(byte[] buf, int off, int len)
  {
    StringBuffer s = new StringBuffer();
    for (int i=0; i<len; ++i)
    {
      int c = buf[i+off] & 0xff;
      s.append("0123456789abcdef".charAt(c>>4));
      s.append("0123456789abcdef".charAt(c&0xf));
    }
    return s.toString();
  }

}
