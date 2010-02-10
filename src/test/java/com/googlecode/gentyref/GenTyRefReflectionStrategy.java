package com.googlecode.gentyref;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import junit.framework.Assert;

class GenTyRefReflectionStrategy extends AbstractReflectionStrategy {
	public boolean isSupertype(Type superType, Type subType) {
		return GenericTypeReflector.isSuperType(superType, subType);
	}

	protected Type getExactSuperType(Type type, Class<?> searchClass) {
		Type result = GenericTypeReflector.getExactSuperType(type, searchClass);
		
		// sanity check: erase(result) == searchClass and result is a supertype of type 
		if (result != null) {
			Assert.assertEquals(searchClass, GenericTypeReflector.erase(result));
			GenericTypeReflector.isSuperType(result, type);
		}
		
		return GenericTypeReflector.getExactSuperType(type, searchClass);
	}

	public Type getReturnType(Type type, Method m) {
		return GenericTypeReflector.getExactReturnType(m, type);
	}

	public Type getFieldType(Type type, Field f) {
		return GenericTypeReflector.getExactFieldType(f, type);
	}
}