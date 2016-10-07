package io.leangen.gentyref8.factory;

/**
 * Generic class with inner classes for testing.
 */
public class GenericOuter<T> {
    /**
     * Static generic inner class.
     */
    public static class StaticGenericInner<S> {
    }

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
}
