/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static io.leangen.geantyref.Annotations.A1;
import static io.leangen.geantyref.Annotations.A2;
import static io.leangen.geantyref.Annotations.A3;
import static io.leangen.geantyref.Annotations.A4;
import static io.leangen.geantyref.Annotations.A5;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests if type-use annotations from different locations are inherited and merged correctly.
 * E.g. Some annotations may be declared on the type variable T, others inherited from the corresponding
 * type variable of the parent types, and some on field of type T (declared anywhere in the hierarchy),
 * and all these should be merged when the field's {@link AnnotatedType} is resolved.
 *
 * @author Bojan Tomic (kaqqao)
 */
public class AnnotationsMergeTest {

    private static AnnotatedType A1_LONG = new TypeToken<@A1 Long>(){}.getAnnotatedType();
    private static AnnotatedType PARENT_A2_STRING = new TypeToken<ParentArray<@A2 String, Number>>(){}.getAnnotatedType();
    private static AnnotatedType CHILD_STRING = new TypeToken<ChildArray<List<String>>>(){}.getAnnotatedType();
    private static AnnotatedType CHILD_STRING_A5_ARRAY = new TypeToken<Child<String @A5 []>>(){}.getAnnotatedType();
    private static Annotation[] A4 = new TypeToken<@A4 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A5 = new TypeToken<@A5 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A3_A4 = new TypeToken<@A3 @A4 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A1_A2 = new TypeToken<@A1 @A2 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A1_A2_A3 = new TypeToken<@A1 @A2 @A3 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A1_A2_A3_A4 = new TypeToken<@A1 @A2 @A3 @A4 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A1_A2_A3_A4_A5 = new TypeToken<@A1 @A2 @A3 @A4 @A5 String>(){}.getAnnotatedType().getAnnotations();

    @Test
    public void fieldTypeAnnotationInheritanceTest() throws NoSuchFieldException {
        Field field = ChildArray.class.getField("annotated");
        AnnotatedType fieldType = GenericTypeReflector.getExactFieldType(field, PARENT_A2_STRING);
        assertArrayEquals(A5, fieldType.getAnnotations());
        assertArrayEqualsUnsorted(A1_A2_A3_A4, ((AnnotatedArrayType) fieldType).getAnnotatedGenericComponentType().getAnnotations());
    }

    @Test
    public void childFieldTypeAnnotationInheritanceTest() throws NoSuchFieldException, NoSuchMethodException {
        Field field = ChildArray.class.getField("annotated");
        AnnotatedType parentType = GenericTypeReflector.getExactSuperType(CHILD_STRING, GrandParentArray.class);
        AnnotatedType fieldType = GenericTypeReflector.getExactFieldType(field, parentType);
        assertArrayEquals(A5, fieldType.getAnnotations());
        assertArrayEqualsUnsorted(A1_A2_A3_A4, ((AnnotatedArrayType) fieldType).getAnnotatedGenericComponentType().getAnnotations());
    }

    @Test
    public void fieldArrayTypeAnnotationInheritanceTest() throws NoSuchFieldException, NoSuchMethodException {
        Field field = Child.class.getField("annotated");
        AnnotatedType parentType = GenericTypeReflector.getExactSuperType(CHILD_STRING_A5_ARRAY, GrandParent.class);
        AnnotatedType fieldType = GenericTypeReflector.getExactFieldType(field, parentType);
        assertArrayEqualsUnsorted(A1_A2_A3_A4_A5, fieldType.getAnnotations());
        assertEquals(0, ((AnnotatedArrayType) fieldType).getAnnotatedGenericComponentType().getAnnotations().length);
    }

    @Test
    public void methodTypeAnnotationInheritanceTest() throws NoSuchMethodException {
        Method method = Inter.class.getMethod("cool");
        AnnotatedType returnType = GenericTypeReflector.getExactReturnType(method, CHILD_STRING);
        assertArrayEqualsUnsorted(A1_A2_A3, returnType.getAnnotations());
        method = ChildArray.class.getMethod("cool");
        returnType = GenericTypeReflector.getExactReturnType(method, CHILD_STRING);
        assertArrayEquals(A4, returnType.getAnnotations());
    }

    @Test
    public void classAnnotationsMergeTest() throws NoSuchFieldException {
        Field nested = GrandParent.class.getField("nested");
        Field array = GrandParent.class.getField("array");
        AnnotatedType returnType = GenericTypeReflector.getExactFieldType(nested, new TypeToken<GrandParent<String>>(){}.getAnnotatedType());
        AnnotatedType bound = ((AnnotatedWildcardType) ((AnnotatedParameterizedType) returnType)
                .getAnnotatedActualTypeArguments()[0]).getAnnotatedUpperBounds()[0];
        assertArrayEqualsUnsorted(A4, bound.getAnnotations());
        assertArrayEqualsUnsorted(A3_A4, ((AnnotatedParameterizedType) bound).getAnnotatedActualTypeArguments()[0].getAnnotations());
        returnType = GenericTypeReflector.getExactFieldType(array, new TypeToken<GrandParent<String>>(){}.getAnnotatedType());
        AnnotatedType component = ((AnnotatedArrayType) returnType).getAnnotatedGenericComponentType();
        assertArrayEqualsUnsorted(A4, component.getAnnotations());
        assertArrayEqualsUnsorted(A3_A4, ((AnnotatedParameterizedType) component).getAnnotatedActualTypeArguments()[0].getAnnotations());
    }

    @Test
    public void classAnnotationsNonPropagationTest() {
        AnnotatedType number = GenericTypeReflector.getExactSuperType(A1_LONG, Number.class);
        assertNotNull(number);
        assertTrue("Class annotations are not propagated upwards", number.getAnnotations().length == 0);
    }

    @Test
    public void variableAnnotationsMergeTest() {
        AnnotatedType resolvedVar = GenericTypeReflector.getTypeParameter(PARENT_A2_STRING, ParentArray.class.getTypeParameters()[0]);
        assertNotNull(resolvedVar);
        assertArrayEqualsUnsorted(A1_A2, resolvedVar.getAnnotations());
    }

    @A4
    private class GrandParent<@A3 T1> {
        public @A3 T1 annotated;
        public List<? extends GrandParent<GrandParent<String>>> nested;
        public GrandParent<GrandParent<String>>[] array;
    }

    private class GrandParentArray<@A3 T1> {
        public @A3 T1 @A5 [] annotated;
    }

    private class Parent<@A1 T1, @A2 S1> extends GrandParent<@A4 T1> {}

    private class ParentArray<@A1 T1, @A2 S1> extends GrandParentArray<@A4 T1> {
        public @A3 S1 getVal(){return null;}
    }

    private class Child<T2> extends Parent<@A2 T2, @A4 Number> {}

    private class ChildArray<T2> extends ParentArray<@A2 T2, @A4 Number> implements Inter<@A3 T2> {
        public @A4 T2 cool(){return null;}
    }

    private static void assertArrayEqualsUnsorted(Annotation[] sortedExpected, Annotation[] unsortedActual) {
        assertArrayEquals(sortedExpected, sort(unsortedActual));
    }

    private static Annotation[] sort(Annotation[] annotations) {
        Arrays.sort(annotations, Comparator.comparing(Annotation::toString));
        return annotations;
    }
}
