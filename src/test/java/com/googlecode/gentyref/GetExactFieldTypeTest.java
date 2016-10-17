
package com.googlecode.gentyref;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigInteger;

import junit.framework.TestCase;

/**
 * Some additional tests for {@link GenericTypeReflector#getExactFieldType}.
 * 
 * @author Curtis Rueden
 */
public class GetExactFieldTypeTest extends TestCase {

	private Field thingField;

	@Override
	public void setUp() throws NoSuchFieldException {
		thingField = Thing.class.getDeclaredField("thing");
	}

	/**
	 * Tests that the exact type of field can be determined when viewed from a
	 * reifiable subclass.
	 */
	public void testReifiable() {
		final Type exactType =
			GenericTypeReflector.getExactFieldType(thingField, StringThing.class);
		assertSame(exactType, String.class);
	}

	/**
	 * Tests that the exact type of field can be determined when viewed from an
	 * anonymous, reifiable subclass.
	 */
	public void testAnonymousReifiable() {
		final Type exactType =
			GenericTypeReflector.getExactFieldType(thingField, new Thing<String>() {}
				.getClass());
		assertSame(exactType, String.class);
	}

	/**
	 * Tests that the exact type of field can be determined when viewed from a
	 * non-reifiable subclass.
	 */
	public void testNonreifiable() {
		final Type exactType =
			GenericTypeReflector.getExactFieldType(thingField, NumberThing.class);
		assertSame(exactType, Number.class);
	}

	/**
	 * Tests that the exact type of field can be determined when viewed from a
	 * reifiable subclass of a non-reifiable subclass.
	 */
	public void testTwoLayerReifiable() {
		final Type exactType =
			GenericTypeReflector.getExactFieldType(thingField,
				new NumberThing<Integer>() {}.getClass());
		assertSame(exactType, Integer.class);
	}

	/**
	 * Tests that the exact type of field can be determined when viewed from a
	 * non-reifiable subclass of a non-reifiable subclass.
	 */
	public void testTwoLayerNonreifiable() {
		final Type exactType =
			GenericTypeReflector.getExactFieldType(thingField, BIThing.class);
		assertSame(exactType, BigInteger.class);
	}

	public static class Thing<T> {
		public T thing;
	}

	public static class StringThing extends Thing<String> {}

	public static class NumberThing<N extends Number> extends Thing<N> {}

	public static class BIThing<BI extends BigInteger> extends NumberThing<BI> {}

}
