package com.coekie.gentyref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Test for http://code.google.com/p/gentyref/issues/detail?id=3 */
public class Issue3Test {
  public class NamedDataObject {}

  @SuppressWarnings("unused")
  public abstract class HierarchicalNamedDataObject<
          RealClass extends HierarchicalNamedDataObject<RealClass>>
      extends NamedDataObject {
    private RealClass parent;

    private List<RealClass> children = new ArrayList<RealClass>();

    public List<RealClass> getChildren() {
      return children;
    }

    public RealClass getParent() {
      return parent;
    }
  }

  @Test
  public void testIt() throws NoSuchMethodException {
    // the return type for the raw type is a raw list
    Method getChildren = HierarchicalNamedDataObject.class.getMethod("getChildren");
    assertEquals(
        List.class,
        GenericTypeReflector.getExactReturnType(getChildren, HierarchicalNamedDataObject.class));

    // the return type for HierarchicalNamedDataObject<?> (constructed using addWildcardParameters)
    //  is a capture of a list whos element type is a HierarchicalNamedDataObject
    Type returnType =
        GenericTypeReflector.getExactReturnType(
            getChildren,
            GenericTypeReflector.addWildcardParameters(HierarchicalNamedDataObject.class));

    Type listType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
    assertTrue(listType instanceof CaptureType);
    Type listElementType = ((CaptureType) listType).getUpperBounds()[0];
    assertTrue(listElementType instanceof ParameterizedType);
    assertEquals(
        HierarchicalNamedDataObject.class, ((ParameterizedType) listElementType).getRawType());

    // using getCollectionElementTypes, you can directly get the classes&interfaces contained in the
    // list
    assertEquals(
        Collections.<Class<?>>singletonList(HierarchicalNamedDataObject.class),
        getCollectionElementTypes(getChildren, HierarchicalNamedDataObject.class));
  }

  private List<Class<?>> getCollectionElementTypes(Method getter, Class<?> clazz) {
    return GenericTypeReflector.getUpperBoundClassAndInterfaces(
        GenericTypeReflector.getTypeParameter(
            GenericTypeReflector.getExactReturnType(
                getter, GenericTypeReflector.addWildcardParameters(clazz)),
            Collection.class.getTypeParameters()[0]));
  }
}
