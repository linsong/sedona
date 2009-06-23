//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   17 Nov 08  Brian Frank  Creation
//

package sedona.vm.sys;

import java.lang.reflect.*;
import sedona.vm.*;

/**
 * sys::Type native methods
 */
public class Type_n
{

  public static Object malloc(Object self, Context cx)
    throws Exception
  {
    // map the IType to a Java class
    IType type = (IType)self;
    String className = "sedona.vm." + type.kit().name() + "." + type.name();
    ClassLoader loader = self.getClass().getClassLoader();
    Class cls = loader.loadClass(className);

    // allocat and initialize the instance
    Object comp = cls.newInstance();
    Field f = cls.getField("type");
    f.set(comp, type);
    Method m = cls.getMethod("_iInit", new Class[0]);
    m.invoke(comp, null);
    return comp;
  }

}

