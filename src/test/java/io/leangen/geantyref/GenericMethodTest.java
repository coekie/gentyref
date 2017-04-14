/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GenericMethodTest extends TestCase {

    /**
     * Test if UnresolvedTypeVariableException is thrown if there is an unresolved type parameter
     * coming from a simple generic method in a parameter.
     */
    public void testSimpleGenericMethodUnresolvedParameter() throws SecurityException, NoSuchMethodException {
        class C {
            @SuppressWarnings("unused")
            public <E> String m(List<E> l) {
                return null;
            }
        }
        Method m = C.class.getMethod("m", List.class);

        // getting the return type should succeed, because it doesn't contain the type variable
        assertEquals(String.class, GenericTypeReflector.getExactReturnType(m, C.class));

        try {
            GenericTypeReflector.getExactParameterTypes(m, C.class);
            fail("expected UnresolvedTypeVariableException");
        } catch (UnresolvedTypeVariableException e) {
            assertEquals(m.getTypeParameters()[0], e.getTypeVariable());
        }
    }

    /**
     * Test if UnresolvedTypeVariableException is thrown if there is an unresolved type parameter
     * coming from a simple generic method in a return type
     */
    public void testSimpleGenericMethodUnresolvedReturnType() throws SecurityException, NoSuchMethodException {
        class C {
            @SuppressWarnings("unused")
            public <E> List<E> m(String p) {
                return null;
            }
        }

        Method m = C.class.getMethod("m", String.class);

        // getting the parameters should succeed, because it doesn't contain the type variable
        assertTrue(Arrays.equals(new Type[]{String.class}, GenericTypeReflector.getExactParameterTypes(m, C.class)));

        try {
            GenericTypeReflector.getExactReturnType(m, C.class);
            fail("expected UnresolvedTypeVariableException");
        } catch (UnresolvedTypeVariableException e) {
            assertEquals(m.getTypeParameters()[0], e.getTypeVariable());
        }
    }

    /**
     * Test if UnresolvedTypeVariableException is thrown if there is an unresolved type parameter
     * coming from a simple generic method, deep in the return type, nested in a generic array and
     * wildcards
     */
    public void testSimpleGenericMethodDeepUnresolvedReturnType() throws SecurityException, NoSuchMethodException {
        class C {
            @SuppressWarnings("unused")
            public <E> List<? extends List<? super E>>[] m(String p) {
                return null;
            }
        }

        Method m = C.class.getMethod("m", String.class);

        try {
            GenericTypeReflector.getExactReturnType(m, C.class);
            fail("expected UnresolvedTypeVariableException");
        } catch (UnresolvedTypeVariableException e) {
            assertEquals(m.getTypeParameters()[0], e.getTypeVariable());
        }
    }

    /**
     * Test expected UnresolvedTypeVariableException for getExactReturnType and
     * getExactParameterTypes on Arrays.asList
     */
    public void testArraysAsListUnresolved() throws SecurityException, NoSuchMethodException {
        Method asList = Arrays.class.getMethod("asList", Object[].class);
        try {
            GenericTypeReflector.getExactReturnType(asList, Arrays.class);
            fail("expected UnresolvedTypeVariableException");
        } catch (UnresolvedTypeVariableException e) {
            assertEquals(asList.getTypeParameters()[0], e.getTypeVariable());
        }
        try {
            GenericTypeReflector.getExactParameterTypes(asList, Arrays.class);
            fail("expected UnresolvedTypeVariableException");
        } catch (UnresolvedTypeVariableException e) {
            assertEquals(asList.getTypeParameters()[0], e.getTypeVariable());
        }
    }

    /**
     * Returns a class nested in this method that contains a method "m"
     * who's return type is this method's type variable E.
     */
    public <E> Class<?> returnsTypeVariableFromOuterMethod() throws SecurityException, NoSuchMethodException {
        class C {
            @SuppressWarnings("unused")
            public E m() {
                return null;
            }
        }
        return C.class;
    }

    public void testReturnsTypeVariableFromOuterMethod() throws SecurityException, NoSuchMethodException {
        Class<?> c = returnsTypeVariableFromOuterMethod();
        Method m = c.getMethod("m");
        TypeVariable<?> e = GenericMethodTest.class.getMethod("returnsTypeVariableFromOuterMethod").getTypeParameters()[0];

        try {
            GenericTypeReflector.getExactReturnType(m, c);
            fail("expected UnresolvedTypeVariableException");
        } catch (UnresolvedTypeVariableException ex) {
            assertEquals(e, ex.getTypeVariable());
        }
    }

    public <E> Class<?> extendsWithTypeVariableFromOuterMethod() throws SecurityException, NoSuchMethodException {
        class C extends ArrayList<E> {
        }
        return C.class;
    }

    public void testExtendsWithTypeVariableFromOuterMethod() throws SecurityException, NoSuchMethodException {
        Class<?> c = extendsWithTypeVariableFromOuterMethod();
        TypeVariable<?> e = GenericMethodTest.class.getMethod("extendsWithTypeVariableFromOuterMethod").getTypeParameters()[0];

        try {
            GenericTypeReflector.getExactSuperType(c, List.class);
            fail("expected UnresolvedTypeVariableException");
        } catch (UnresolvedTypeVariableException ex) {
            assertEquals(e, ex.getTypeVariable());
        }
    }

    public <E> Type typeVariableFromMethod() {
        return new TypeToken<List<E>>() {
        }.getType();
    }

    /**
     * Test when a type containing a type variable from an unrelated method is given.
     * This is different from the other tests in that the type variable from the method is not
     * introduced by navigating types, but is directly given by the gentyref API user.
     */
    public void testTypeVariableFromMethodGiven() throws SecurityException, NoSuchMethodException {
        Type listE = typeVariableFromMethod();
        TypeVariable<?> e = GenericMethodTest.class.getMethod("typeVariableFromMethod").getTypeParameters()[0];

        Type result = GenericTypeReflector.getExactSuperType(listE, Collection.class);

        Type collectionE = new ParameterizedTypeImpl(Collection.class, new Type[]{e}, null);
        assertEquals(collectionE, result);
    }
}
