/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref.factory;

import junit.framework.TestCase;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;

/**
 * Test for http://code.google.com/p/gentyref/issues/detail?id=16
 */
public class Issue16Test extends TestCase {
    /**
     * Simple test for our {@link GenericTypeReflector#getExactReturnType(Method, AnnotatedType)}.
     */
    public void test() throws NoSuchMethodException {
        assertEquals(String.class, new StringOuter().getExactReturnType());
    }

    /**
     * Just showing that you can do the same with a type token (if it is a constant)
     */
    public void testTypeToken() throws NoSuchMethodException {
        Type inner = new TypeToken<StringOuter.Inner>() {
        }.getType();
        final Method get = GenericOuter.Inner.class.getMethod("get");
        assertEquals(String.class, GenericTypeReflector.getExactReturnType(get, inner));
    }

    /**
     * Testing our {@link GenericTypeReflector#getExactReturnType(Method, AnnotatedType)} with a raw
     * type.
     */
    public void testRaw() throws NoSuchMethodException {
        assertEquals(Object.class, new GenericOuter<String>().getExactReturnType());
    }

    public class GenericOuter<T> {
        /**
         * This is the solution to the original problem in Issue 16, using the new
         * {@link TypeFactory#innerClass(Type, Class)} method.
         */
        public Type getExactReturnType() throws NoSuchMethodException {
            final Type inner = TypeFactory.innerClass(this.getClass(), Inner.class);
            final Method get = Inner.class.getMethod("get");
            return GenericTypeReflector.getExactReturnType(get, inner);
        }

        public class Inner {
            public T get() {
                return null;
            }
        }
    }

    public class StringOuter extends GenericOuter<String> {
    }
}
