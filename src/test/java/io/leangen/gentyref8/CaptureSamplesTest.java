package io.leangen.gentyref8;

import junit.framework.TestCase;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * see http://code.google.com/p/gentyref/wiki/CaptureType
 *
 * @author Wouter Coekaerts {@literal (wouter@coekaerts.be)}
 */
public class CaptureSamplesTest extends TestCase {
    public void testFoo() throws NoSuchFieldException {
        Foo<? extends Number> foo = new Foo<Integer>();
        foo.listWildcard = new ArrayList<Long>();
        //foo.listT = new ArrayList<Long>(); // does not compile

        Type fooWildcard = new TypeToken<Foo<? extends Number>>() {
        }.getType();

        Type listWildcardFieldType = GenericTypeReflector.getExactFieldType(Foo.class.getDeclaredField("listWildcard"), fooWildcard);
        Type listTFieldType = GenericTypeReflector.getExactFieldType(Foo.class.getDeclaredField("listT"), fooWildcard);

        assertEquals(new TypeToken<List<? extends Number>>() {
        }.getType(), listWildcardFieldType);
        assertTrue(GenericTypeReflector.isSuperType(listWildcardFieldType, new TypeToken<ArrayList<Long>>() {
        }.getType()));
        assertFalse(GenericTypeReflector.isSuperType(listTFieldType, new TypeToken<ArrayList<Long>>() {
        }.getType()));
    }

    @SuppressWarnings("unused")
    public void testBar() throws NoSuchFieldException {
        Bar<?> bar = new Bar<Integer>();
        Number n = bar.t;

        Type barType = new TypeToken<Bar<?>>() {
        }.getType();
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
