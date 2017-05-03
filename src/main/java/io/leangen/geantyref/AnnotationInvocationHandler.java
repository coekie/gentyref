/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * An implementation of {@link Annotation} that mimics the behavior of normal annotations.
 * It is an {@link InvocationHandler}, meant to be used via {@link TypeFactory#annotation(Class, Map)}.
 * <p>
 * The constructor checks that the all the elements required by the annotation interface are provided
 * and that the types are compatible. If extra elements are provided, they are ignored.
 * If a value is of an incompatible type is provided or no value is provided for an element
 * without a default value, {@link AnnotationFormatException} is thrown.
 * </p>
 * <p>
 * Note: {@link #equals(Object)} and {@link #hashCode()} and implemented as specified
 * by {@link Annotation}, so instances are safe to mix with normal annotations.
 *
 * @see Annotation
 */
class AnnotationInvocationHandler implements Annotation, InvocationHandler, Serializable {

    private static final long serialVersionUID = 8615044376674805680L;
    /**
     * Maps primitive {@code Class}es to their corresponding wrapper {@code Class}.
     */
    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<>();

    static {
        primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
        primitiveWrapperMap.put(Byte.TYPE, Byte.class);
        primitiveWrapperMap.put(Character.TYPE, Character.class);
        primitiveWrapperMap.put(Short.TYPE, Short.class);
        primitiveWrapperMap.put(Integer.TYPE, Integer.class);
        primitiveWrapperMap.put(Long.TYPE, Long.class);
        primitiveWrapperMap.put(Double.TYPE, Double.class);
        primitiveWrapperMap.put(Float.TYPE, Float.class);
    }

    private final Class<? extends Annotation> annotationType;
    private final Map<String, Object> values;
    private final int hashCode;

    AnnotationInvocationHandler(Class<? extends Annotation> annotationType, Map<String, Object> values) throws AnnotationFormatException {
        Class[] interfaces = annotationType.getInterfaces();
        if (annotationType.isAnnotation() && interfaces.length == 1 && interfaces[0] == Annotation.class) {
            this.annotationType = annotationType;
            this.values = Collections.unmodifiableMap(normalize(annotationType, values));
            this.hashCode = calculateHashCode();
        } else {
            throw new AnnotationFormatException(annotationType.getName() + " is not an annotation type");
        }
    }

    static Map<String, Object> normalize(Class<? extends Annotation> annotationType, Map<String, Object> values) throws AnnotationFormatException {
        Set<String> missing = new HashSet<>();
        Set<String> invalid = new HashSet<>();
        Map<String, Object> valid = new HashMap<>();
        for (Method element : annotationType.getDeclaredMethods()) {
            String elementName = element.getName();
            if (values.containsKey(elementName)) {
                Class<?> returnType = element.getReturnType();
                if (returnType.isPrimitive()) {
                    returnType = primitiveWrapperMap.get(returnType);
                }

                if (returnType.isInstance(values.get(elementName))) {
                    valid.put(elementName, values.get(elementName));
                } else {
                    invalid.add(elementName);
                }
            } else {
                if (element.getDefaultValue() != null) {
                    valid.put(elementName, element.getDefaultValue());
                } else {
                    missing.add(elementName);
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new AnnotationFormatException("Missing value(s) for " + String.join(",", missing));
        }
        if (!invalid.isEmpty()) {
            throw new AnnotationFormatException("Incompatible type(s) provided for " + String.join(",", invalid));
        }
        return valid;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (values.containsKey(method.getName())) {
            return values.get(method.getName());
        }
        return method.invoke(this, args);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return annotationType;
    }

    /**
     * Performs an equality check as described in {@link Annotation#equals(Object)}.
     *
     * @param other The object to compare
     * @return Whether the given object is equal to this annotation or not
     * @see Annotation#equals(Object)
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!annotationType.isInstance(other)) {
            return false;
        }

        Annotation that = annotationType.cast(other);

        //compare annotation member values
        for (Entry<String, Object> element : values.entrySet()) {
            Object value = element.getValue();
            Object otherValue;
            try {
                otherValue = that.annotationType().getMethod(element.getKey()).invoke(that);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            if (!Objects.deepEquals(value, otherValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates the hash code of this annotation as described in {@link Annotation#hashCode()}.
     *
     * @return The hash code of this annotation.
     * @see Annotation#hashCode()
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('@').append(annotationType.getName()).append('(');
        Set<String> sorted = new TreeSet<>(values.keySet());
        for (String elementName : sorted) {
            String value;
            if (values.get(elementName).getClass().isArray()) {
                value = Arrays.deepToString(new Object[] {values.get(elementName)})
                        .replaceAll("^\\[\\[", "[")
                        .replaceAll("]]$", "]");
            } else {
                value = values.get(elementName).toString();
            }
            result.append(elementName).append('=').append(value).append(", ");
        }
        // remove the trailing separator
        if (values.size() > 0) {
            result.delete(result.length() - 2, result.length());
        }
        result.append(")");

        return result.toString();
    }


    private int calculateHashCode() {
        int hashCode = 0;

        for (Entry<String, Object> element : values.entrySet()) {
            hashCode += (127 * element.getKey().hashCode()) ^ calculateHashCode(element.getValue());
        }

        return hashCode;
    }

    private int calculateHashCode(Object element) {
        if (!element.getClass().isArray()) {
            return element.hashCode();
        }
        if (element instanceof Object[]) {
            return Arrays.hashCode((Object[]) element);
        }
        if (element instanceof byte[]) {
            return Arrays.hashCode((byte[]) element);
        }
        if (element instanceof short[]) {
            return Arrays.hashCode((short[]) element);
        }
        if (element instanceof int[]) {
            return Arrays.hashCode((int[]) element);
        }
        if (element instanceof long[]) {
            return Arrays.hashCode((long[]) element);
        }
        if (element instanceof char[]) {
            return Arrays.hashCode((char[]) element);
        }
        if (element instanceof float[]) {
            return Arrays.hashCode((float[]) element);
        }
        if (element instanceof double[]) {
            return Arrays.hashCode((double[]) element);
        }
        if (element instanceof boolean[]) {
            return Arrays.hashCode((boolean[]) element);
        }

        return Objects.hashCode(element);
    }
}
