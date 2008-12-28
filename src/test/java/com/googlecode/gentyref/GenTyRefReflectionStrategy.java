/**
 * 
 */
package com.googlecode.gentyref;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


class GenTyRefReflectionStrategy implements ReflectionStrategy {
	public void testExactSuperclass(Type expectedSuperclass, Type type) {
		// test if the supertype of the given class is as expected
		GenericTypeReflectorTest.assertEquals(expectedSuperclass, getExactSuperType(type, GenericTypeReflectorTest.getClassType(expectedSuperclass)));
	}

	public boolean isSupertype(Type superType, Type subType) {
		return GenericTypeReflector.isSuperType(superType, subType);
	}

	public void testInexactSupertype(Type superType, Type subType) {
		if (superType instanceof ParameterizedType || superType instanceof Class) {
			// test if it's not exact
			GenericTypeReflectorTest.assertFalse(superType.equals(getExactSuperType(subType, GenericTypeReflectorTest.getClassType(superType))));
		}
	}

	private Type getExactSuperType(Type type, Class<?> searchClass) {
		return GenericTypeReflector.getExactSuperType(type, searchClass);
	}

	public Type getReturnType(Type type, Method m) {
		return GenericTypeReflector.getExactReturnType(m, type);
	}

	public Type getFieldType(Type type, Field f) {
		return GenericTypeReflector.getExactFieldType(f, type);
	}
}