package com.coekie.gentyref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * ReflectionStrategy for implementations that provide a getExactSuperType implementation
 *
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
public abstract class AbstractReflectionStrategy implements ReflectionStrategy {

  public final void testExactSuperclass(Type expectedSuperclass, Type type) {
    // test if the supertype of the given class is as expected
    assertEquals(
        expectedSuperclass,
        getExactSuperType(type, GenericTypeReflectorTest.getClassType(expectedSuperclass)));
  }

  public final void testInexactSupertype(Type superType, Type subType) {
    if (superType instanceof ParameterizedType || superType instanceof Class) {
      // test if it's not exact
      assertFalse(
          superType.equals(
              getExactSuperType(subType, GenericTypeReflectorTest.getClassType(superType))));
    }
  }

  protected abstract Type getExactSuperType(Type type, Class<?> searchClass);
}
