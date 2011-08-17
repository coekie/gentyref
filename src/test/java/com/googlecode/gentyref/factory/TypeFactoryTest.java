package com.googlecode.gentyref.factory;

import static com.googlecode.gentyref.TypeFactory.innerClass;
import static com.googlecode.gentyref.TypeFactory.parameterizedClass;
import static com.googlecode.gentyref.TypeFactory.parameterizedInnerClass;
import static com.googlecode.gentyref.TypeFactory.unboundWildcard;
import static com.googlecode.gentyref.TypeFactory.wildcardExtends;
import static com.googlecode.gentyref.TypeFactory.wildcardSuper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import com.googlecode.gentyref.TypeArgumentNotInBoundException;
import com.googlecode.gentyref.TypeFactory;
import com.googlecode.gentyref.TypeToken;
import com.googlecode.gentyref.factory.GenericOuter.DoubleGeneric;
import com.googlecode.gentyref.factory.GenericOuter.Inner;

public class TypeFactoryTest extends TestCase {
	private static final Type GENERICOUTER_STRING = new TypeToken<GenericOuter<String>>(){}.getType();
	
	/**
	 * If there are no type parameters, it's just a Class
	 */
	public void testSimpleClass() {
		assertEquals(String.class, parameterizedClass(String.class, (Type[])null));
	}
	
	/**
	 * Also for an inner class: if there are no type parameters it's just a Class
	 */
	public void testSimpleInner() {
		assertEquals(SimpleOuter.SimpleInner.class,
				innerClass(SimpleOuter.class, SimpleOuter.SimpleInner.class));
	}
	
	public void testSimpleGeneric() {
		assertEquals(new TypeToken<List<String>>(){}.getType(), parameterizedClass(List.class, String.class));
	}
	
	/**
	 * If the given parameters are null, it's a raw type
	 */
	public void testSimpleRaw() {
		assertEquals(List.class, parameterizedClass(List.class, (Type[])null));
	}
	
	/**
	 * An empty array as arguments is not the same as null:
	 * it means the caller explicitly expects the class not to need type arguments.
	 * So we throw an exception if they were needed.
	 */
	public void testEmptyArgumentsForGenericClass() {
		try {
			parameterizedClass(List.class, new Type[]{});
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	public void testTooManyTypeArguments() {
		try {
			parameterizedClass(List.class, new Type[]{String.class, String.class});
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	public void testGenericOwner() {
		assertEquals(new TypeToken<GenericOuter<String>.Inner>(){}.getType(),
				innerClass(GENERICOUTER_STRING, Inner.class));
	}
	
	public void testDoubleGeneric() {
		assertEquals(new TypeToken<GenericOuter<String>.DoubleGeneric<Integer>>(){}.getType(),
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
		} catch (IllegalArgumentException expected) {
		}
	}
	
	/**
	 * An owner type that is a subtype of the enclosing class is converted into the right
	 * parameterized version of that enclosing class.
	 */
	public void testConcreteOuter() {
		assertEquals(new TypeToken<GenericOuter<String>.Inner>(){}.getType(),
				innerClass(StringOuter.class, Inner.class));
		
		// sanity check: the compiler does the same
		assertEquals(new TypeToken<GenericOuter<String>.Inner>(){}.getType(),
				new TypeToken<StringOuter.Inner>(){}.getType());
	}
	
	@SuppressWarnings("rawtypes")
	public void testConcreteRawOuter() {
		assertEquals(Inner.class,
				innerClass(RawOuter.class, Inner.class));
		
		// sanity check: the compiler does the same
		assertEquals(Inner.class,
				new TypeToken<RawOuter.Inner>(){}.getType());
	}
	
	public void testConcreteRawOuterGenericInner() {
		assertEquals(DoubleGeneric.class,
				parameterizedInnerClass(RawOuter.class, DoubleGeneric.class, String.class));
		
		// TODO what does the GenericTypeReflector.getExactReturnType do for a method in RawOuter that returns DoubleGeneric?
	}
	
	public void testSimpleOuterGenericInner() {
		assertEquals(new TypeToken<SimpleOuter.GenericInner<String>>(){}.getType(),
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
		assertEquals(new TypeToken<SimpleOuter.GenericInner<String>>(){}.getType(),
				parameterizedClass(SimpleOuter.GenericInner.class, String.class));
	}
	
	public void testMissingSimpleOuterRawInner() {
		assertEquals(SimpleOuter.GenericInner.class,
				parameterizedClass(SimpleOuter.GenericInner.class, (Type[])null));
	}
	
	public void testWrongOwnerSimple() {
		try {
			innerClass(String.class, SimpleOuter.SimpleInner.class);
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	public void testWrongOwnerGeneric() {
		try {
			parameterizedInnerClass(String.class, SimpleOuter.GenericInner.class, String.class);
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	public void testWrongOwnerRaw() {
		try {
			innerClass(String.class, SimpleOuter.GenericInner.class);
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	public void testStaticInnerWithoutOwner() {
		Type result = parameterizedClass(GenericOuter.StaticGenericInner.class, Integer.class);
		
		assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>(){}.getType(),
				result);
		
		// sanity check: even static inner classes' ParameterizedTypes refer to the owner
		assertEquals(GenericOuter.class,
				((ParameterizedType)new TypeToken<GenericOuter.StaticGenericInner<Integer>>(){}.getType()).getOwnerType());
		
		// so our ParameterizedType should do the same
		assertEquals(GenericOuter.class, ((ParameterizedType)result).getOwnerType());
	}
	
	public void testStaticInnerWithRawOwner() {
		assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>(){}.getType(),
			parameterizedInnerClass(GenericOuter.class, GenericOuter.StaticGenericInner.class, Integer.class));
	}
	
	public void testStaticInnerWithRawSubclassOwner() {
		assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>(){}.getType(),
			parameterizedInnerClass(RawOuter.class, GenericOuter.StaticGenericInner.class, Integer.class));
		
		// sanity check: the compiler does the same
		assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>(){}.getType(),
				new TypeToken<RawOuter.StaticGenericInner<Integer>>(){}.getType());
	}
	
	/**
	 * If the owner is given as a generic type, just ignore the type arguments
	 */
	public void testStaticInnerWithGenericOwner() {
		Type result = parameterizedInnerClass(GENERICOUTER_STRING, GenericOuter.StaticGenericInner.class, Integer.class);
		
		assertEquals(new TypeToken<GenericOuter.StaticGenericInner<Integer>>(){}.getType(), result);
		assertEquals(GenericOuter.class, ((ParameterizedType)result).getOwnerType());
	}
	
	public void testStaticInnerWithWrongOwner() {
		try {
			parameterizedInnerClass(String.class, GenericOuter.StaticGenericInner.class, Integer.class);
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	// TODO what if the specified owner type is a wildcard, a capture, a (generic) array or a type variable,...
	//  The use of "getExactSuperType" should make it smart in handling those...
	
	public void testNullTypeArgument() {
		try {
			parameterizedClass(List.class, new Type[]{null});
			fail("expected exception");
		} catch (NullPointerException expected) {
		}
	}
	
	static class Bound<T extends Number> {}
	
	public void testTypeArgumentInBound() {
		assertEquals(new TypeToken<Bound<Integer>>(){}.getType(),
				parameterizedClass(Bound.class, Integer.class));
	}
	
	public void testTypeArgumentsNotInBound() {
		try {
			parameterizedClass(Bound.class, String.class);
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	static class ReferingBound<A extends List<B>, B> {}
	
	public void testTypeArgumentInReferingBound() {
		assertEquals(new TypeToken<ReferingBound<List<Integer>, Integer>>(){}.getType(),
				parameterizedClass(ReferingBound.class, parameterizedClass(List.class, Integer.class), Integer.class));
	}
	
	public void testTypeArgumentsNotInReferingBound() {
		try {
			parameterizedClass(ReferingBound.class, parameterizedClass(List.class, Integer.class), Number.class);
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	static class RecursiveBound<A extends RecursiveBound<A>> {}
	static class InRecursiveBound extends RecursiveBound<InRecursiveBound> {}
	
	public void testTypeArgumentInRecursiveBound() {
		assertEquals(new TypeToken<RecursiveBound<InRecursiveBound>>(){}.getType(),
				parameterizedClass(RecursiveBound.class, InRecursiveBound.class));
	}
	
	static class NotInRecursiveBound extends RecursiveBound<InRecursiveBound> {}
	
	public void testTypeArgumentNotInRecursiveBound() {
		// type RecursiveBound<NotInRecursiveBound> is not valid
		try {
			parameterizedClass(RecursiveBound.class, NotInRecursiveBound.class);
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	@SuppressWarnings("rawtypes")
	static class RawBound<A extends List>{}
	
	public void testTypeArgumentInRawBound() {
		assertEquals(new TypeToken<RawBound<List<String>>>(){}.getType(),
				parameterizedClass(RawBound.class, parameterizedClass(List.class, String.class)));
	}
	
	public void testTypeArgumentNotInRawBound() {
		try {
			parameterizedClass(RawBound.class, parameterizedClass(Collection.class, String.class));
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	static class ParameterizedBound<A extends List<Integer>>{}
	
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
		} catch (IllegalArgumentException expected) {
		}
	}
	
	/**
	 * If the bound is raw, the a raw argument is fine
	 */
	@SuppressWarnings("rawtypes")
	public void testRawTypeArgumentInRawBound() {
		assertEquals(new TypeToken<RawBound<ArrayList>>(){}.getType(),
				parameterizedClass(RawBound.class, ArrayList.class));
	}
	
	static class BoundReferingToOwner<X> {
		class In<Y extends X> {}
	}
	
	public void testTypeArgumentInBoundReferingToOwner() {
		Type result = parameterizedInnerClass(
				parameterizedClass(BoundReferingToOwner.class, Number.class),
				BoundReferingToOwner.In.class,
				Integer.class
		);
		
		// unfortunately this doesn't compile with JDK 5 (but it does with the Eclipse compiler)
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6557954
//		assertEquals(new TypeToken<BoundReferingToOwner<Number>.In<Integer>>(){}.getType(),
//				result
//		);
		
		// so we only test that it didn't throw an exception, and returned a ParamerizedType
		assertTrue(result instanceof ParameterizedType);
	}
	
	public void testTypeArgumentNotInBoundReferingToOwner() {
		try {
			parameterizedInnerClass(
					parameterizedClass(BoundReferingToOwner.class, Number.class),
					BoundReferingToOwner.In.class,
					String.class
			);
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	public void testUnboundWildcardTypeArgumentInBound() {
		assertEquals(new TypeToken<Bound<?>>(){}.getType(),
				parameterizedClass(Bound.class, unboundWildcard()));
	}
	
	public void testWildcardExtendsTypeArgumentInBound() {
		assertEquals(new TypeToken<Bound<? extends Integer>>(){}.getType(),
				parameterizedClass(Bound.class, wildcardExtends(Integer.class)));
	}
	
	public void testWildcardExtendsTypeArgumentNotInBound() {
		try {
			parameterizedClass(Bound.class, wildcardExtends(Thread.class));
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	public void testUnrelatedWildcardExtendsTypeArgumentInBound() {
		assertEquals(new TypeToken<Bound<? extends Runnable>>(){}.getType(),
				parameterizedClass(Bound.class, wildcardExtends(Runnable.class)));
	}
	
	public void testWildcardSuperTypeArgumentInBound() {
		assertEquals(new TypeToken<Bound<? super Integer>>(){}.getType(),
				parameterizedClass(Bound.class, wildcardSuper(Integer.class)));
	}
	
	public void testWildcardSuperTypeArgumentNotInBound() {
		try {
			parameterizedClass(Bound.class, wildcardSuper(String.class));
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	public void testWildcardInReferingBound() {
		assertEquals(new TypeToken<ReferingBound<?, String>>(){}.getType(),
				parameterizedClass(ReferingBound.class, unboundWildcard(), String.class));
	}
	
	public void testInReferingToWildcardBound() {
		// JDK doesn't allow this, but the eclipse compiler does.
		// We prefer to be lenient, so we allow it.
		// But we can't assertEquals it to a TypeToken because that wouldn't compile.
		parameterizedClass(
				ReferingBound.class,
				parameterizedClass(List.class, wildcardExtends(Integer.class)),
				wildcardExtends(Number.class)
		);
	}
	
	public void testNotInReferingToWildcardBound() {
		try {
			parameterizedClass(
					ReferingBound.class,
					parameterizedClass(List.class, wildcardExtends(Number.class)),
					wildcardExtends(Integer.class)
			);
			fail("expected exception");
		} catch (TypeArgumentNotInBoundException expected) {
		}
	}
	
	public void testLocalClass() {
		class Local<T> {}
		System.out.println(Local.class.getDeclaringClass());
	
		assertEquals(new TypeToken<Local<String>>(){}.getType(),
				parameterizedClass(Local.class, String.class));
	}
	
	/**
	 * Specifying an owner for a local class is not allowed, because such a ParameterizedType also doesn't have an
	 * class as its direct owner (the method is the owner, but that can't be represented).
	 * (Java reflection also doesn't see the enclosing class as owner). 
	 */
	public void testLocalClassWithOwner() {
		class Local<T> {}
		try {
			parameterizedInnerClass(TypeFactoryTest.class, Local.class, String.class);
			fail("expected exception");
		} catch (IllegalArgumentException expected) {
		}
	}
	
	private Type getFirstTypeArgument(TypeToken<?> typeToken) {
		return ((ParameterizedType) typeToken.getType()).getActualTypeArguments()[0];
	}
	
	public void testUnboundWildcard() {
		assertEquals(getFirstTypeArgument(new TypeToken<List<?>>(){}), TypeFactory.unboundWildcard());
	}
	
	public void testWildcardExtends() {
		assertEquals(getFirstTypeArgument(new TypeToken<List<? extends String>>(){}),
				TypeFactory.wildcardExtends(String.class));
	}
	
	public void testWildcardSuper() {
		assertEquals(getFirstTypeArgument(new TypeToken<List<? super String>>(){}),
				TypeFactory.wildcardSuper(String.class));
	}
	
	public void testClassArray() {
		assertSame(String[].class, TypeFactory.arrayOf(String.class));
	}
	
	public void testGenericArray() {
		assertEquals(new TypeToken<List<String>[]>(){}.getType(),
				TypeFactory.arrayOf(parameterizedClass(List.class, String.class)));
	}
}


