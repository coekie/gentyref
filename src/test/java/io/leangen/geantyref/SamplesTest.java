/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

/**
 * Simple samples of what gentyref does, in the form of tests.
 * See http://code.google.com/p/gentyref/wiki/ExampleUsage
 *
 * @author Wouter Coekaerts {@literal (wouter@coekaerts.be)}
 */
public class SamplesTest extends TestCase {
    /*
     * Returns true if processorClass extends Processor<String>
     */
    public boolean isStringProcessor(Class<? extends Processor<?>> processorClass) {
        // Use TypeToken to get an instanceof a specific Type
        Type type = new TypeToken<Processor<String>>() {
        }.getType();
        // Use GenericTypeReflector.isSuperType to check if a type is a supertype of another
        return GenericTypeReflector.isSuperType(type, processorClass);
    }

    public void testProsessor() {
        assertTrue(isStringProcessor(StringProcessor.class));
        assertFalse(isStringProcessor(IntegerProcessor.class));
    }

    public void testCollectorList() throws NoSuchMethodException {
        Method listMethod = StringCollector.class.getMethod("list");

        // java returns List<T>
        Type returnType = listMethod.getGenericReturnType();
        assertTrue(returnType instanceof ParameterizedType);
        assertTrue(((ParameterizedType) returnType).getActualTypeArguments()[0] instanceof TypeVariable<?>);

        // we get List<String>
        Type exactReturnType = GenericTypeReflector.getExactReturnType(listMethod, StringCollector.class);
        assertEquals(new TypeToken<List<String>>() {
        }.getType(), exactReturnType);
    }

    public void testCollectorAdd() throws NoSuchMethodException {
        Method addMethod = StringCollector.class.getMethod("add", Object.class);

        // returns [T]
        Type[] parameterTypes = addMethod.getGenericParameterTypes();
        assertEquals(1, parameterTypes.length);
        assertTrue(parameterTypes[0] instanceof TypeVariable<?>);

        // returns [String]
        Type[] exactParameterTypes = GenericTypeReflector.getExactParameterTypes(addMethod, StringCollector.class);
        assertEquals(1, exactParameterTypes.length);
        assertEquals(String.class, exactParameterTypes[0]);
    }

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

    abstract class Collector<T> {
        public List<T> list() {
            return null;
        }

        public void add(T item) {
        }
    }

    class StringCollector extends Collector<String> {
    }
}
