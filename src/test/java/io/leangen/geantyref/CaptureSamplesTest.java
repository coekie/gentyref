/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import junit.framework.TestCase;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * See <a href="https://github.com/leangen/geantyref/wiki/CaptureType">CaptureType</a>
 *
 * @author Wouter Coekaerts {@literal (wouter@coekaerts.be)}
 */
public class CaptureSamplesTest extends TestCase {
    public void testFoo() throws NoSuchFieldException {
        Foo<? extends Number> foo = new Foo<Integer>();
        foo.listWildcard = new ArrayList<Long>();
        //foo.listT = new ArrayList<Long>(); // does not compile

        Type fooWildcard = new TypeToken<Foo<? extends Number>>() {}.getType();

        Type listWildcardFieldType = GenericTypeReflector.getExactFieldType(Foo.class.getDeclaredField("listWildcard"), fooWildcard);
        Type listTFieldType = GenericTypeReflector.getExactFieldType(Foo.class.getDeclaredField("listT"), fooWildcard);

        assertEquals(new TypeToken<List<? extends Number>>() {}.getType(), listWildcardFieldType);
        assertTrue(GenericTypeReflector.isSuperType(listWildcardFieldType, new TypeToken<ArrayList<Long>>() {}.getType()));
        assertFalse(GenericTypeReflector.isSuperType(listTFieldType, new TypeToken<ArrayList<Long>>() {}.getType()));
    }

    @SuppressWarnings("unused")
    public void testBar() throws NoSuchFieldException {
        Bar<?> bar = new Bar<Integer>();
        Number n = bar.t;

        Type barType = new TypeToken<Bar<?>>() {}.getType();
        Type captureType = GenericTypeReflector.getExactFieldType(Bar.class.getDeclaredField("t"), barType);
        assertTrue(GenericTypeReflector.isSuperType(Number.class, captureType));
    }

    class Foo<T> {
        List<? extends Number> listWildcard;
        List<T> listT;
    }

    class Bar<T extends Number> {
        T t;
    }
}
