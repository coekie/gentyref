package com.coekie.gentyref;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** http://code.google.com/p/gentyref/issues/detail?id=13 */
public class Issue13Test {
  /** Test that Object is seen as superclass of any class, interface and array */
  @Test
  public void testObjectSuperclassOfInterface() throws NoSuchMethodException {
    assertEquals(
        Object.class, GenericTypeReflector.getExactSuperType(ArrayList.class, Object.class));
    assertEquals(Object.class, GenericTypeReflector.getExactSuperType(List.class, Object.class));
    assertEquals(
        Object.class, GenericTypeReflector.getExactSuperType(String[].class, Object.class));
  }

  /** Test that toString method can be resolved in any class, interface or array */
  @Test
  public void testObjectMethodOnInterface() throws NoSuchMethodException {
    Method toString = Object.class.getMethod("toString");
    assertEquals(String.class, GenericTypeReflector.getExactReturnType(toString, ArrayList.class));
    assertEquals(String.class, GenericTypeReflector.getExactReturnType(toString, List.class));
    assertEquals(String.class, GenericTypeReflector.getExactReturnType(toString, String[].class));
  }
}
