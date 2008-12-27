package com.googlecode.gentyref;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.googlecode.gentyref.GenericsReflector;



public class GenericsReflectorTest extends AbstractGenericsReflectorTest {
	@Override
	protected void testExactSuperclass(Type expectedSuperclass, Type type) {
		// test if it's really seen as a supertype
		assertTrue(GenericsReflector.isSuperType(expectedSuperclass, type));
		
		// test if the supertype of the given class is as expected
		assertEquals(expectedSuperclass, getExactSuperType(type, getClass(expectedSuperclass)));
	}
	
	@Override
	protected void testInexactSupertype(Type superType, Type subType) {
		// first if it's seen as a supertype
		assertTrue(GenericsReflector.isSuperType(superType, subType));
		
		
		if (superType instanceof ParameterizedType || superType instanceof Class) {
			// test if it's not exact
			assertFalse(superType.equals(getExactSuperType(subType, getClass(superType))));
		}
	}
	
	private Type getExactSuperType(Type type, Class<?> searchClass) {
		return GenericsReflector.getExactSuperType(type, searchClass);
	}
	
	@Override
	protected Type getExactReturnType(Method m, Type type) {
		return GenericsReflector.getExactReturnType(m, type);
	}
	
	@Override
	protected Type getExactFieldType(Field f, Type type) {
		return GenericsReflector.getExactFieldType(f, type);
	}
	
}
