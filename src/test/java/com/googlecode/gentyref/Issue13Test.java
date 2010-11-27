package com.googlecode.gentyref;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * http://code.google.com/p/gentyref/issues/detail?id=13
 */
public class Issue13Test extends TestCase {
	/**
	 * Test that Object is seen as superclass of any class, interface and array
	 */
	public void testObjectSuperclassOfInterface() throws NoSuchMethodException {
		assertEquals(Object.class, GenericTypeReflector.getExactSuperType(ArrayList.class, Object.class));
		assertEquals(Object.class, GenericTypeReflector.getExactSuperType(List.class, Object.class));
		assertEquals(Object.class, GenericTypeReflector.getExactSuperType(String[].class, Object.class));
	}
	
	/**
	 * Test that toString method can be resolved in any class, interface or array
	 */
    public void testObjectMethodOnInterface() throws NoSuchMethodException {
    	Method toString = Object.class.getMethod("toString", new Class<?>[]{});
        assertEquals(String.class, GenericTypeReflector.getExactReturnType(toString, ArrayList.class));
		assertEquals(String.class, GenericTypeReflector.getExactReturnType(toString, List.class));
		assertEquals(String.class, GenericTypeReflector.getExactReturnType(toString, String[].class));
    }
}
