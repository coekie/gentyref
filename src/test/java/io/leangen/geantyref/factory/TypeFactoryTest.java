/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref.factory;


import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeArgumentNotInBoundException;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import io.leangen.geantyref.factory.GenericOuter.DoubleGeneric;
import io.leangen.geantyref.factory.GenericOuter.Inner;

import static io.leangen.geantyref.TypeFactory.innerClass;
import static io.leangen.geantyref.TypeFactory.parameterizedClass;
import static io.leangen.geantyref.TypeFactory.parameterizedInnerClass;
import static io.leangen.geantyref.TypeFactory.unboundWildcard;
import static io.leangen.geantyref.TypeFactory.wildcardExtends;
import static io.leangen.geantyref.TypeFactory.wildcardSuper;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;

public class TypeFactoryTest extends TestCase {
    private static final Type GENERICOUTER_STRING = new TypeToken<GenericOuter<String>>() {
    }.getType();

    private static final Annotation a1Literal = new TypeToken<@A1(value = "Test value", values = {"value 1", "value 2"}, longs = {2L, 4L, 6L}) String>() {
    }.getAnnotatedType().getAnnotation(A1.class);

    private static final Annotation a2Literal = new TypeToken<
            @A2(annotations = {
                    @A1(value = "Test value", values = {"value 1", "value 2"}, longs = {2L, 4L, 6L}),
                    @A1(value = "Different value", values = {"value 3", "value 4"}, longs = {4L, 16L, 36L})
            }) String>() {
    }.getAnnotatedType().getAnnotation(A2.class);
    
    /**
     * If there are no type parameters, it's just a Class
     */
    public void testSimpleClass() {
        assertEquals(String.class, parameterizedClass(String.class, (Type[]) null));
    }

    /**
     * Also for an inner class: if there are no type parameters it's just a Class
     */
    public void testSimpleInner() {
        assertEquals(SimpleOuter.SimpleInner.class,
                innerClass(SimpleOuter.class, SimpleOuter.SimpleInner.class));
    }

    public void testSimpleGeneric() {
        assertEquals(new TypeToken<List<String>>() {
        }.getType(), parameterizedClass(List.class, String.class));
    }

    /**
     * If the given parameters are null, it's a raw type
     */
    public void testSimpleRaw() {
        assertEquals(List.class, parameterizedClass(List.class, (Type[]) null));
    }

    /**
     * An empty array as arguments is not the same as null:
     * it means the caller explicitly expects the class not to need type arguments.
     * So we throw an exception if they were needed.
     */
    public void testEmptyArgumentsForGenericClass() {
        try {
            parameterizedClass(List.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testTooManyTypeArguments() {
        try {
            parameterizedClass(List.class, String.class, String.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testGenericOwner() {
        assertEquals(new TypeToken<GenericOuter<String>.Inner>() {
                }.getType(),
                innerClass(GENERICOUTER_STRING, Inner.class));
    }

    public void testDoubleGeneric() {
        assertEquals(new TypeToken<GenericOuter<String>.DoubleGeneric<Integer>>() {
                }.getType(),
                parameterizedInnerClass(GENERICOUTER_STRING, DoubleGeneric.class, Integer.class));
    }

    /**
     * If the owner is raw, the whole type is raw (test for a non-generic inner class)
     */
    public void testRawGenericOwner() {
        assertEquals(Inner.class, innerClass(GenericOuter.class, Inner.class));
    }

    /**
     * If the owner is raw, the whole type is raw (test also for a generic inner class)
     */
    public void testDoubleGenericRawOwner() {
        assertEquals(DoubleGeneric.class,
                parameterizedInnerClass(GenericOuter.class, DoubleGeneric.class, Integer.class));
    }

    /**
     * If the inner class is raw, the whole type is raw
     */
    public void testDoubleGenericRawInner() {
        assertEquals(DoubleGeneric.class,
                innerClass(GENERICOUTER_STRING, DoubleGeneric.class));
    }

    /**
     * If the outer class is not specified, take it as a raw one
     */
    public void testDoubleGenericMissingOwner() {
        assertEquals(DoubleGeneric.class,
                parameterizedClass(DoubleGeneric.class, Integer.class));
    }

    public void testOwnerForTopLevel() {
        try {
            innerClass(String.class, Integer.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    /**
     * An owner type that is a subtype of the enclosing class is converted into the right
     * parameterized version of that enclosing class.
     */
    public void testConcreteOuter() {
        assertEquals(new TypeToken<GenericOuter<String>.Inner>() {
                }.getType(),
                innerClass(StringOuter.class, Inner.class));

        // sanity check: the compiler does the same
        assertEquals(new TypeToken<GenericOuter<String>.Inner>() {
                }.getType(),
                new TypeToken<StringOuter.Inner>() {
                }.getType());
    }

    @SuppressWarnings("rawtypes")
    public void testConcreteRawOuter() {
        assertEquals(Inner.class,
                innerClass(RawOuter.class, Inner.class));

        // sanity check: the compiler does the same
        assertEquals(Inner.class,
                new TypeToken<RawOuter.Inner>() {
                }.getType());
    }

    public void testConcreteRawOuterGenericInner() {
        assertEquals(DoubleGeneric.class,
                parameterizedInnerClass(RawOuter.class, DoubleGeneric.class, String.class));

        // TODO what does the GenericTypeReflector.getExactReturnType do for a method in RawOuter that returns DoubleGeneric?
    }

    public void testSimpleOuterGenericInner() {
        assertEquals(new TypeToken<SimpleOuter.GenericInner<String>>() {
                }.getType(),
                parameterizedInnerClass(SimpleOuter.class, SimpleOuter.GenericInner.class, String.class));
    }

    public void testSimpleOuterRawInner() {
        assertEquals(SimpleOuter.GenericInner.class,
                innerClass(SimpleOuter.class, SimpleOuter.GenericInner.class));
    }

    /**
     * If the outer class is not specified, it doesn't matter if it's not generic anyways
     */
    public void testMissingSimpleOuterGenericInner() {
        assertEquals(new TypeToken<SimpleOuter.GenericInner<String>>() {
                }.getType(),
                parameterizedClass(SimpleOuter.GenericInner.class, String.class));
    }

    public void testMissingSimpleOuterRawInner() {
        assertEquals(SimpleOuter.GenericInner.class,
                parameterizedClass(SimpleOuter.GenericInner.class, (Type[]) null));
    }

    public void testWrongOwnerSimple() {
        try {
            innerClass(String.class, SimpleOuter.SimpleInner.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testWrongOwnerGeneric() {
        try {
            parameterizedInnerClass(String.class, SimpleOuter.GenericInner.class, String.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testWrongOwnerRaw() {
        try {
            innerClass(String.class, SimpleOuter.GenericInner.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testStaticInnerWithoutOwner() {
        Type result = parameterizedClass(GenericOuter.StaticGenericInner.class, Integer.class);

        assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>() {
                }.getType(),
                result);

        // sanity check: even static inner classes' ParameterizedTypes refer to the owner
        assertEquals(GenericOuter.class,
                ((ParameterizedType) new TypeToken<GenericOuter.StaticGenericInner<Integer>>() {
                }.getType()).getOwnerType());

        // so our ParameterizedType should do the same
        assertEquals(GenericOuter.class, ((ParameterizedType) result).getOwnerType());
    }

    public void testStaticInnerWithRawOwner() {
        assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>() {
                }.getType(),
                parameterizedInnerClass(GenericOuter.class, GenericOuter.StaticGenericInner.class, Integer.class));
    }

    public void testStaticInnerWithRawSubclassOwner() {
        assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>() {
                }.getType(),
                parameterizedInnerClass(RawOuter.class, GenericOuter.StaticGenericInner.class, Integer.class));

        // sanity check: the compiler does the same
        assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>() {
                }.getType(),
                new TypeToken<RawOuter.StaticGenericInner<Integer>>() {
                }.getType());
    }

    /**
     * If the owner is given as a generic type, just ignore the type arguments
     */
    public void testStaticInnerWithGenericOwner() {
        Type result = parameterizedInnerClass(GENERICOUTER_STRING, GenericOuter.StaticGenericInner.class, Integer.class);

        assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>() {
        }.getType(), result);
        assertEquals(GenericOuter.class, ((ParameterizedType) result).getOwnerType());
    }

    public void testStaticInnerWithWrongOwner() {
        try {
            parameterizedInnerClass(String.class, GenericOuter.StaticGenericInner.class, Integer.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    // TODO what if the specified owner type is a wildcard, a capture, a (generic) array or a type variable,...
    //  The use of "getExactSuperType" should make it smart in handling those...

    public void testNullTypeArgument() {
        try {
            parameterizedClass(List.class, new Type[]{null});
            fail("expected exception");
        } catch (NullPointerException expected) {//expected
        }
    }

    public void testTypeArgumentInBound() {
        assertEquals(new TypeToken<Bound<Integer>>() {
                }.getType(),
                parameterizedClass(Bound.class, Integer.class));
    }

    public void testTypeArgumentsNotInBound() {
        try {
            parameterizedClass(Bound.class, String.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testTypeArgumentInReferringBound() {
        assertEquals(new TypeToken<ReferringBound<List<Integer>, Integer>>() {
                }.getType(),
                parameterizedClass(ReferringBound.class, parameterizedClass(List.class, Integer.class), Integer.class));
    }

    public void testTypeArgumentsNotInReferringBound() {
        try {
            parameterizedClass(ReferringBound.class, parameterizedClass(List.class, Integer.class), Number.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testTypeArgumentInRecursiveBound() {
        assertEquals(new TypeToken<RecursiveBound<InRecursiveBound>>() {
                }.getType(),
                parameterizedClass(RecursiveBound.class, InRecursiveBound.class));
    }

    public void testTypeArgumentNotInRecursiveBound() {
        // type RecursiveBound<NotInRecursiveBound> is not valid
        try {
            parameterizedClass(RecursiveBound.class, NotInRecursiveBound.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testTypeArgumentInRawBound() {
        assertEquals(new TypeToken<RawBound<List<String>>>() {
                }.getType(),
                parameterizedClass(RawBound.class, parameterizedClass(List.class, String.class)));
    }

    public void testTypeArgumentNotInRawBound() {
        try {
            parameterizedClass(RawBound.class, parameterizedClass(Collection.class, String.class));
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    /**
     * A raw type argument to a parameter with a non-raw bound, is not valid
     */
    // TODO testRawTypeArgumentInParameterizedBoundNotValid
    // this is not implemented yet, because isSuperType() can't signal that it's raw
    // (and a dumb check that the argument is missing variables isn't good enough, because it would
    // also block a type that is raw but has an (indirect non-raw supertype)
    public void ignoredTestRawTypeArgumentInParameterizedBoundNotValid() {

        // ParameterizedBound<List> is not valid
        try {
            parameterizedClass(ParameterizedBound.class, List.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    /**
     * If the bound is raw, the a raw argument is fine
     */
    @SuppressWarnings("rawtypes")
    public void testRawTypeArgumentInRawBound() {
        assertEquals(new TypeToken<RawBound<ArrayList>>() {
                }.getType(),
                parameterizedClass(RawBound.class, ArrayList.class));
    }

    public void testTypeArgumentInBoundReferringToOwner() {
        Type result = parameterizedInnerClass(
                parameterizedClass(BoundReferringToOwner.class, Number.class),
                BoundReferringToOwner.In.class,
                Integer.class
        );

        // unfortunately this doesn't compile with JDK 5 (but it does with the Eclipse compiler)
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6557954
//		assertEquals(new TypeToken<BoundReferringToOwner<Number>.In<Integer>>(){}.getType(),
//				result
//		);

        // so we only test that it didn't throw an exception, and returned a ParamerizedType
        assertTrue(result instanceof ParameterizedType);
    }

    public void testTypeArgumentNotInBoundReferringToOwner() {
        try {
            parameterizedInnerClass(
                    parameterizedClass(BoundReferringToOwner.class, Number.class),
                    BoundReferringToOwner.In.class,
                    String.class
            );
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testUnboundWildcardTypeArgumentInBound() {
        assertEquals(new TypeToken<Bound<?>>() {
                }.getType(),
                parameterizedClass(Bound.class, unboundWildcard()));
    }

    public void testWildcardExtendsTypeArgumentInBound() {
        assertEquals(new TypeToken<Bound<? extends Integer>>() {
                }.getType(),
                parameterizedClass(Bound.class, wildcardExtends(Integer.class)));
    }

    public void testWildcardExtendsTypeArgumentNotInBound() {
        try {
            parameterizedClass(Bound.class, wildcardExtends(Thread.class));
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    public void testUnrelatedWildcardExtendsTypeArgumentInBound() {
        assertEquals(new TypeToken<Bound<? extends Runnable>>() {
                }.getType(),
                parameterizedClass(Bound.class, wildcardExtends(Runnable.class)));
    }

    public void testWildcardSuperTypeArgumentInBound() {
        assertEquals(new TypeToken<Bound<? super Integer>>() {
                }.getType(),
                parameterizedClass(Bound.class, wildcardSuper(Integer.class)));
    }

    public void testWildcardSuperTypeArgumentNotInBound() {
        try {
            parameterizedClass(Bound.class, wildcardSuper(String.class));
            fail("expected exception");
        } catch (IllegalArgumentException expected) { //expected
        }
    }

    public void testWildcardInReferringBound() {
        assertEquals(new TypeToken<ReferringBound<?, String>>() {
                }.getType(),
                parameterizedClass(ReferringBound.class, unboundWildcard(), String.class));
    }

    public void testInReferringToWildcardBound() {
        // JDK doesn't allow this, but the eclipse compiler does.
        // We prefer to be lenient, so we allow it.
        // But we can't assertEquals it to a TypeToken because that wouldn't compile.
        parameterizedClass(
                ReferringBound.class,
                parameterizedClass(List.class, wildcardExtends(Integer.class)),
                wildcardExtends(Number.class)
        );
    }

    public void testNotInReferringToWildcardBound() {
        try {
            parameterizedClass(
                    ReferringBound.class,
                    parameterizedClass(List.class, wildcardExtends(Number.class)),
                    wildcardExtends(Integer.class)
            );
            fail("expected exception");
        } catch (TypeArgumentNotInBoundException expected) { //expected
        }
    }

    public void testLocalClass() {
        class Local<T> {}
        assertEquals(new TypeToken<Local<String>>() {
                }.getType(),
                parameterizedClass(Local.class, String.class));
    }

    /**
     * Specifying an owner for a local class is not allowed, because such a ParameterizedType also
     * doesn't have an class as its direct owner (the method is the owner, but that can't be
     * represented). (Java reflection also doesn't see the enclosing class as owner).
     */
    public void testLocalClassWithOwner() {
        class Local<T> {}
        try {
            parameterizedInnerClass(TypeFactoryTest.class, Local.class, String.class);
            fail("expected exception");
        } catch (IllegalArgumentException expected) {//expected
        }
    }

    private Type getFirstTypeArgument(TypeToken<?> typeToken) {
        return ((ParameterizedType) typeToken.getType()).getActualTypeArguments()[0];
    }

    public void testUnboundWildcard() {
        assertEquals(getFirstTypeArgument(new TypeToken<List<?>>() {
        }), unboundWildcard());
    }

    public void testWildcardExtends() {
        assertEquals(getFirstTypeArgument(new TypeToken<List<? extends String>>() {
                }),
                wildcardExtends(String.class));
    }

    public void testWildcardSuper() {
        assertEquals(getFirstTypeArgument(new TypeToken<List<? super String>>() {
                }),
                wildcardSuper(String.class));
    }

    public void testClassArray() {
        assertSame(String[].class, TypeFactory.arrayOf(String.class));
    }

    public void testGenericArray() {
        assertEquals(new TypeToken<List<String>[]>() {
                }.getType(),
                TypeFactory.arrayOf(parameterizedClass(List.class, String.class)));
    }

    public void testAnnotationCreation() throws AnnotationFormatException {
        String value = "Test value";
        String[] values = new String[] {"value 1", "value 2"};
        Map<String, Object> elements = new HashMap<>();
        elements.put("value", value);
        elements.put("values", values);
        A1 a1 = TypeFactory.annotation(A1.class, elements);
        assertEquals(value, a1.value());
        assertArrayEquals(values, a1.values());
        assertArrayEquals(new long[] {1L, 2L, 3L}, a1.longs());
        long[] longs = new long[] {5L, 9L, 13L};
        elements.put("longs", longs);
        a1 = TypeFactory.annotation(A1.class, elements);
        assertArrayEquals(longs, a1.longs());
    }

    public void testIncompleteAnnotationCreation() {
        Map<String, Object> elements = new HashMap<>();
        elements.put("value", "Test value");
        try {
            TypeFactory.annotation(A1.class, elements);
            fail("expected exception");
        } catch (AnnotationFormatException e) {//expected
        }
    }

    public void testIncompatibleAnnotationCreation() {
        Map<String, Object> elements = new HashMap<>();
        elements.put("value", "Test value");
        elements.put("values", new int[] {1, 2});
        try {
            TypeFactory.annotation(A1.class, elements);
            fail("expected exception");
        } catch (AnnotationFormatException e) {//expected
        }
    }
    
    public void testAnnotationEqualityAndHashCode() throws AnnotationFormatException {
        Map<String, Object> a1Vals = new HashMap<>();
        a1Vals.put("value", "Test value");
        a1Vals.put("values", new String[] {"value 1", "value 2"});
        a1Vals.put("longs", new long[] {2L, 4L, 6L});
        A1 a = TypeFactory.annotation(A1.class, a1Vals);

        a1Vals.put("value", "Different value");
        a1Vals.put("values", new String[] {"value 3", "value 4"});
        a1Vals.put("longs", new long[] {4L, 16L, 36L});
        A1 b = TypeFactory.annotation(A1.class, a1Vals);

        a1Vals.put("longs", new long[] {2L, 3L, 6L});
        A1 c = TypeFactory.annotation(A1.class, a1Vals);
        
        Map<String, Object> a2Vals = new HashMap<>();
        a2Vals.put("annotations", new A1[] {a, b});
        
        A2 a2 = TypeFactory.annotation(A2.class, a2Vals);
        Map<String, Object> b2Vals = new HashMap<>();
        b2Vals.put("annotations", new A1[] {a, c});
        
        A2 b2 = TypeFactory.annotation(A2.class, b2Vals);
        
        assertEquals(a1Literal, a);
        assertEquals(a1Literal.hashCode(), a.hashCode());
        assertEquals(a1Literal.toString(), a.toString());
        assertEquals(a2Literal, a2);
        assertEquals(a2Literal.hashCode(), a2.hashCode());
        assertEquals(a2Literal.toString(), a2.toString());

        assertNotEquals(a1Literal, c);
        assertNotEquals(a1Literal.hashCode(), c.hashCode());
        assertNotEquals(a1Literal.toString(), c.toString());
        assertNotEquals(a2Literal, b2);
        assertNotEquals(a2Literal.hashCode(), b2.hashCode());
        assertNotEquals(a2Literal.toString(), b2.toString());
    }

    private static class Bound<T extends Number> {
    }

    private static class ReferringBound<A extends List<B>, B> {
    }

    static class RecursiveBound<A extends RecursiveBound<A>> {
    }

    static class InRecursiveBound extends RecursiveBound<InRecursiveBound> {
    }

    private static class NotInRecursiveBound extends RecursiveBound<InRecursiveBound> {
    }

    @SuppressWarnings("rawtypes")
    private static class RawBound<A extends List> {
    }

    private static class ParameterizedBound<A extends List<Integer>> {
    }

    private static class BoundReferringToOwner<X> {
        class In<Y extends X> {
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface A1 {
        String value();

        String[] values();

        String def() default "";

        long[] longs() default {1L, 2L, 3L};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface A2 {
        A1[] annotations();
    }
}


