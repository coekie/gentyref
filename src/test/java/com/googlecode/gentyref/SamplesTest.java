package com.googlecode.gentyref;

import java.lang.reflect.Type;

import junit.framework.TestCase;

/**
 * Simple samples of what gentyref does, in the form of tests.
 * See http://code.google.com/p/gentyref/wiki/ExampleUsage
 * 
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
public class SamplesTest extends TestCase {
	interface Processor<T> {
		void process(T t);
	}
	
	class StringProcessor implements Processor<String> {
		public void process(String s) {
			System.out.println("processing " + s);
		}
	}
	
	class IntegerProcessor implements Processor<Integer> {
		public void process(Integer i) {
			System.out.println("processing " + i);
		}
	}
	
	/*
	 * Returns true if processorClass extends Processor<String>
	 */
	public boolean isStringProcessor(Class<? extends Processor<?>> processorClass) {
		// Use TypeToken to get an instanceof a specific Type
		Type type = new TypeToken<Processor<String>>(){}.getType();
		// Use GenericTypeReflector.isSuperType to check if a type is a supertype of another
		return GenericTypeReflector.isSuperType(type, processorClass);
	}
	
	public void testProsessor() {
		assertTrue(isStringProcessor(StringProcessor.class));
		assertFalse(isStringProcessor(IntegerProcessor.class));
	}
}
