package com.googlecode.gentyref.factory;

/**
 * Generic class with inner classes for testing.
 */
public class GenericOuter<T> {
	/**
	 * A non-generic inner class with a generic outer class.
	 */
	public class Inner {
	}
	
	/**
	 * Generic inner class with a generic outer class.
	 */
	public class DoubleGeneric<S> {
	}
	
	/**
	 * Static generic inner class. 
	 */
	public static class StaticGenericInner<S> {
	}
}
