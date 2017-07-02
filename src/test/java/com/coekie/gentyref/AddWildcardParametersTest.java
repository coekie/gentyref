package com.coekie.gentyref;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Test;

public class AddWildcardParametersTest {
  @Test
  public void testNoParams() {
    assertEquals(String.class, GenericTypeReflector.addWildcardParameters(String.class));
  }

  @Test
  public void testMap() {
    assertEquals(
        new TypeToken<Map<?, ?>>() {}.getType(),
        GenericTypeReflector.addWildcardParameters(Map.class));
  }

  @Test
  public void testInnerAndOuter() {
    class Outer<T> {
      class Inner<S> {}
    }
    assertEquals(
        new TypeToken<Outer<?>.Inner<?>>() {}.getType(),
        GenericTypeReflector.addWildcardParameters(Outer.Inner.class));
  }

  @Test
  public void testInner() {
    class Outer {
      class Inner<S> {}
    }
    assertEquals(
        new TypeToken<Outer.Inner<?>>() {}.getType(),
        GenericTypeReflector.addWildcardParameters(Outer.Inner.class));
  }

  @Test
  public void testOuter() {
    class Outer<T> {
      class Inner {}
    }
    assertEquals(
        new TypeToken<Outer<?>.Inner>() {}.getType(),
        GenericTypeReflector.addWildcardParameters(Outer.Inner.class));
  }

  @Test
  public void testNoParamArray() {
    assertEquals(String[].class, GenericTypeReflector.addWildcardParameters(String[].class));
    assertEquals(String[][].class, GenericTypeReflector.addWildcardParameters(String[][].class));
  }

  @Test
  public void testGenericArray() {
    assertEquals(
        new TypeToken<Map<?, ?>[]>() {}.getType(),
        GenericTypeReflector.addWildcardParameters(Map[].class));
    assertEquals(
        new TypeToken<Map<?, ?>[][]>() {}.getType(),
        GenericTypeReflector.addWildcardParameters(Map[][].class));
  }
}
