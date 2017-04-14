/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample in the form of a JUnit test, to show gentyref solves the problem on
 * http://stackoverflow.com/questions/182872/
 *
 * @author Wouter Coekaerts {@literal (wouter@coekaerts.be)}
 */
public class StackoverflowQ182872Test extends TestCase {
    private static Type LIST_OF_STRING = new TypeToken<List<String>>() {
    }.getType();

    public List<String> method1() {
        return null;
    }

    public ArrayList<String> method2() {
        return null;
    }

    public StringList method3() {
        return null;
    }

    public boolean returnTypeExtendsListOfString(Class<?> clazz, String methodName) throws NoSuchMethodException {
        Method method = clazz.getMethod(methodName);
        Type returnType = GenericTypeReflector.getExactReturnType(method, clazz); // needed for method5, else the return type is just "T"
        return GenericTypeReflector.isSuperType(LIST_OF_STRING, returnType);
    }

    public void testIt() throws NoSuchMethodException {
        assertTrue(returnTypeExtendsListOfString(StackoverflowQ182872Test.class, "method1"));
        assertTrue(returnTypeExtendsListOfString(StackoverflowQ182872Test.class, "method2"));
        assertTrue(returnTypeExtendsListOfString(StackoverflowQ182872Test.class, "method3"));
        assertTrue(returnTypeExtendsListOfString(Parameterized.class, "method4"));
        assertTrue(returnTypeExtendsListOfString(Parameterized2ListString.class, "method5"));
    }

    interface Parameterized<T extends StringList> {
        T method4();
    }

    interface Parameterized2<T> {
        T method5();
    }

    interface Parameterized2ListString extends Parameterized2<List<String>> {
    }

    public static class StringList extends ArrayList<String> {
    }
}
