/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref.factory;

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
