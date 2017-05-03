package io.leangen.geantyref;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationInvocationHandlerTest {
    @Test
    public void normalize() throws Exception {
        // Given
        Map<String, Object> values = new HashMap<>();
        values.put("aBoolean", Boolean.FALSE);
        values.put("anInt", 42);

        // When
        Map<String, Object> normalize = AnnotationInvocationHandler.normalize(MyAnnotation.class, unmodifiableMap(values));

        // Then
        assertThat(normalize, equalTo(values));
    }

    @Test(expected = AnnotationFormatException.class)
    public void normalizeWithBadValues() throws Exception {
        // Given
        Map<String, Object> values = new HashMap<>();
        values.put("aBoolean", "Some text");
        values.put("anInt", 42);

        // When
        AnnotationInvocationHandler.normalize(MyAnnotation.class, unmodifiableMap(values));
    }

    @Test
    public void normalizeDefaultValues() throws Exception {
        // Given
        Map<String, Object> values = new HashMap<>();
        values.put("aBoolean", Boolean.FALSE);
        values.put("anInt", 0);

        // When
        Map<String, Object> normalize = AnnotationInvocationHandler.normalize(MyAnnotation.class, emptyMap());

        // Then
        assertThat(normalize, equalTo(values));
    }

    @interface MyAnnotation {
        boolean aBoolean() default false;

        int anInt() default 0;
    }
}
