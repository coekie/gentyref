/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package com.coekie.gentyref;

import java.awt.*;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.coekie.gentyref.GenericTypeReflector.getExactSubType;

/**
 * Test for reflection done in GenericTypeReflector.
 * This class inherits most of its tests from the superclass, and adds a few more.
 */
public class GenericTypeReflectorTest extends AbstractGenericsReflectorTest {
    public GenericTypeReflectorTest() {
        super(new GenTyRefReflectionStrategy());
    }

    public void testGetTypeParameter() {
        @SuppressWarnings("serial")
        class StringList extends ArrayList<String> {
        }
        assertEquals(String.class, GenericTypeReflector.getTypeParameter(StringList.class, Collection.class.getTypeParameters()[0]));
    }

    public void testGetUpperBoundClassAndInterfaces() {
        class Foo<A extends Number & Iterable<A>, B extends A> {
        }
        TypeVariable<?> a = Foo.class.getTypeParameters()[0];
        TypeVariable<?> b = Foo.class.getTypeParameters()[1];
        assertEquals(Arrays.asList(Number.class, Iterable.class),
                GenericTypeReflector.getUpperBoundClassAndInterfaces(a));
        assertEquals(Arrays.asList(Number.class, Iterable.class),
                GenericTypeReflector.getUpperBoundClassAndInterfaces(b));
    }

    /**
     * Call getExactReturnType with a method that is not a method of the given type.
     * Issue #6
     */
    public void testGetExactReturnTypeIllegalArgument() throws SecurityException, NoSuchMethodException {
        Method method = ArrayList.class.getMethod("set", int.class, Object.class);
        try {
            // ArrayList.set overrides List.set, but it's a different method so it's not a member of the List interface
            GenericTypeReflector.getExactReturnType(method, List.class);
            fail("expected exception");
        } catch (IllegalArgumentException e) { // expected
        }
    }

    /**
     * Same as {@link #testGetExactReturnTypeIllegalArgument()} for getExactFieldType
     */
    public void testGetExactFieldTypeIllegalArgument() throws SecurityException, NoSuchFieldException {
        Field field = Dimension.class.getField("width");
        try {
            GenericTypeReflector.getExactFieldType(field, List.class);
            fail("expected exception");
        } catch (IllegalArgumentException e) { // expected
        }
    }

    public void testGetExactParameterTypes() throws SecurityException, NoSuchMethodException {
        // method: boolean add(int index, E o), erasure is boolean add(int index, Object o)
        Method getMethod = List.class.getMethod("add", int.class, Object.class);
        Type[] result = GenericTypeReflector.getExactParameterTypes(getMethod, new TypeToken<ArrayList<String>>() {}.getType());
        assertEquals(2, result.length);
        assertEquals(int.class, result[0]);
        assertEquals(String.class, result[1]);
    }

    public void testGetExactConstructorParameterTypes() throws SecurityException, NoSuchMethodException {
        // constructor: D(T o), erasure is D(Object o)
        Constructor ctor = D.class.getDeclaredConstructor(Object.class);
        Type[] result = GenericTypeReflector.getExactParameterTypes(ctor, new TypeToken<D<String>>() {}.getType());
        assertEquals(1, result.length);
        assertEquals(String.class, result[0]);
    }

    public void testGetExactSubType() {
        AnnotatedParameterizedType parent = (AnnotatedParameterizedType) new TypeToken<P<String, Integer>>(){}.getAnnotatedType();
        AnnotatedParameterizedType subType = (AnnotatedParameterizedType) getExactSubType(parent, C.class);
        assertNotNull(subType);
        assertEquals(Integer.class, subType.getAnnotatedActualTypeArguments()[0].getType());
        assertEquals(String.class, subType.getAnnotatedActualTypeArguments()[1].getType());
    }

    public void testGetExactSubTypeUnresolvable() {
        AnnotatedParameterizedType parent = (AnnotatedParameterizedType) new TypeToken<P<String, Integer>>(){}.getAnnotatedType();
        AnnotatedType resolved = GenericTypeReflector.getExactSubType(parent, C1.class);
        assertNotNull(resolved);
        assertEquals(C1.class, resolved.getType());
    }
    
    public void testGetExactSubTypeUnresolvable2() {
        AnnotatedType parent = new TypeToken<N>(){}.getAnnotatedType();
        AnnotatedType resolved = GenericTypeReflector.getExactSubType(parent, C1.class);
        assertNotNull(resolved);
        assertEquals(C1.class, resolved.getType());
    }

    public void testGetExactSubTypeNotOverlapping() {
        AnnotatedParameterizedType parent = (AnnotatedParameterizedType) new TypeToken<List<String>>(){}.getAnnotatedType();
        AnnotatedType subType = getExactSubType(parent, Set.class);
        assertNull(subType);
    }

    public void testGetExactSubTypeNotParameterized() {
        AnnotatedParameterizedType parent = (AnnotatedParameterizedType) new TypeToken<List<String>>(){}.getAnnotatedType();
        AnnotatedType subType = getExactSubType(parent, String.class);
        assertNotNull(subType);
        assertEquals(String.class, subType.getType());
    }

    public void testGetExactSubTypeArray() {
        AnnotatedType parent = new TypeToken<List<String>[]>(){}.getAnnotatedType();
        AnnotatedType subType = getExactSubType(parent, ArrayList[].class);
        assertNotNull(subType);
        assertTrue(subType instanceof AnnotatedArrayType);
        AnnotatedType componentType = ((AnnotatedArrayType) subType).getAnnotatedGenericComponentType();
        assertTrue(componentType instanceof AnnotatedParameterizedType);
        assertEquals(String.class, ((AnnotatedParameterizedType) componentType).getAnnotatedActualTypeArguments()[0].getType());
    }

    private class N {}
    private class P<S, K> extends N {}
    private class M<U, R> extends P<U, R> {}
    private class C<X, Y> extends M<Y, X> {}
    private class C1<X, Y, Z> extends M<Y, X> {}
    private static class D<T> { D(T t) {}}
}
