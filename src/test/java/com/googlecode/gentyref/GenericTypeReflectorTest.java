package com.googlecode.gentyref;

import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Test for reflection done in GenericTypeReflector.
 * This class inherits most of its tests from the superclass, and adds a few more.
 */
public class GenericTypeReflectorTest extends AbstractGenericsReflectorTest {
	public GenericTypeReflectorTest() {
		super(new GenTyRefReflectionStrategy());
	}
	
	public void testGetTypeParameter() {
		class StringList extends ArrayList<String> {}
		assertEquals(String.class, GenericTypeReflector.getTypeParameter(StringList.class, Collection.class.getTypeParameters()[0]));
	}
	
	public void testGetUpperBoundClassAndInterfaces() {
		class Foo<A extends Number & Iterable<A>, B extends A> {}
		TypeVariable<?> a = Foo.class.getTypeParameters()[0];
		TypeVariable<?> b = Foo.class.getTypeParameters()[1];
		assertEquals(Arrays.<Class<?>>asList(Number.class, Iterable.class),
			GenericTypeReflector.getUpperBoundClassAndInterfaces(a));
		assertEquals(Arrays.<Class<?>>asList(Number.class, Iterable.class),
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
	
	public void testgetExactParameterTypes() throws SecurityException, NoSuchMethodException {
		// method: boolean add(int index, E o), erasure is boolean add(int index, Object o)
		Method getMethod = List.class.getMethod("add", int.class, Object.class);
		Type[] result = GenericTypeReflector.getExactParameterTypes(getMethod, new TypeToken<ArrayList<String>>(){}.getType());
		assertEquals(2, result.length);
		assertEquals(int.class, result[0]);
		assertEquals(String.class, result[1]);
	}
}
