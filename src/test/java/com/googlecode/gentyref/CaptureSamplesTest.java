package com.googlecode.gentyref;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * see http://code.google.com/p/gentyref/wiki/CaptureType
 * 
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
public class CaptureSamplesTest extends TestCase {
	class Foo<T> {
		List<? extends Number> listWildcard;
		List<T> listT;
	}
	
	public void testFoo() throws NoSuchFieldException {
		Foo<? extends Number> foo = new Foo<Integer>();
		foo.listWildcard = new ArrayList<Long>();
		//foo.listT = new ArrayList<Long>(); // does not compile
		
		Type fooWildcard = new TypeToken<Foo<? extends Number>>(){}.getType();
		
		Type listWildcardFieldType = GenericTypeReflector.getExactFieldType(Foo.class.getDeclaredField("listWildcard"), fooWildcard);
		Type listTFieldType = GenericTypeReflector.getExactFieldType(Foo.class.getDeclaredField("listT"), fooWildcard);
		
		assertEquals(new TypeToken<List<? extends Number>>(){}.getType(), listWildcardFieldType);
		assertTrue(GenericTypeReflector.isSuperType(listWildcardFieldType, new TypeToken<ArrayList<Long>>(){}.getType()));
		assertFalse(GenericTypeReflector.isSuperType(listTFieldType, new TypeToken<ArrayList<Long>>(){}.getType()));
	}
	
	class Bar<T extends Number> {
		T t;
	}
	
	@SuppressWarnings("unused")
	public void testBar() throws NoSuchFieldException {
		Bar<?> bar = new Bar<Integer>();
		Number n = bar.t;
		
		Type barType = new TypeToken<Bar<?>>(){}.getType();
		Type captureType = GenericTypeReflector.getExactFieldType(Bar.class.getDeclaredField("t"), barType);
		assertTrue(GenericTypeReflector.isSuperType(Number.class, captureType));
	}
}
