package io.leangen.geantyref;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static io.leangen.geantyref.Annotations.A1;
import static io.leangen.geantyref.Annotations.A2;
import static io.leangen.geantyref.Annotations.A3;
import static io.leangen.geantyref.Annotations.A4;
import static io.leangen.geantyref.Annotations.A5;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests if type-use annotations from different locations are inherited and merged correctly.
 * E.g. Some annotations may be declared on the type variable T, others inherited from the corresponding
 * type variable of the parent types, and some on field of type T (declared anywhere in the hierarchy),
 * and all these should be merged when the field's {@link AnnotatedType} is resolved.
 * 
 * @author Bojan Tomic (kaqqao)
 */
public class AnnotationsMergeTest {
    
    private static AnnotatedType PARENT_A2_STRING = new TypeToken<ParentArray<@A2 String, Number>>(){}.getAnnotatedType();
    private static AnnotatedType CHILD_STRING = new TypeToken<ChildArray<List<String>>>(){}.getAnnotatedType();
    private static AnnotatedType CHILD_STRING_A5_ARRAY = new TypeToken<Child<String @A5 []>>(){}.getAnnotatedType();
    private static Annotation[] A4 = new TypeToken<@A4 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A5 = new TypeToken<@A5 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A1_A2_A3 = new TypeToken<@A1 @A2 @A3 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A1_A2_A3_A4 = new TypeToken<@A1 @A2 @A3 @A4 String>(){}.getAnnotatedType().getAnnotations();
    private static Annotation[] A1_A2_A3_A4_A5 = new TypeToken<@A1 @A2 @A3 @A4 @A5 String>(){}.getAnnotatedType().getAnnotations();

    @Test
    public void fieldTypeAnnotationInheritanceTest() throws NoSuchFieldException {
        Field field = ChildArray.class.getField("annotated");
        AnnotatedType fieldType = GenericTypeReflector.getExactFieldType(field, PARENT_A2_STRING);
        assertArrayEquals(A5, fieldType.getAnnotations());
        assertArrayContains(A1_A2_A3_A4, ((AnnotatedArrayType) fieldType).getAnnotatedGenericComponentType().getAnnotations());
    }
    
    @Test
    public void fieldTypeAnnotationInheritanceTest2() throws NoSuchFieldException, NoSuchMethodException {
        Field field = ChildArray.class.getField("annotated");
        AnnotatedType parentType = GenericTypeReflector.getExactSuperType(CHILD_STRING, GrandParentArray.class);
        AnnotatedType fieldType = GenericTypeReflector.getExactFieldType(field, parentType);
        assertArrayEquals(A5, fieldType.getAnnotations());
        assertArrayContains(A1_A2_A3_A4, ((AnnotatedArrayType) fieldType).getAnnotatedGenericComponentType().getAnnotations());
    }

    @Test
    public void fieldArrayTypeAnnotationInheritanceTest() throws NoSuchFieldException, NoSuchMethodException {
        Field field = Child.class.getField("annotated");
        AnnotatedType parentType = GenericTypeReflector.getExactSuperType(CHILD_STRING_A5_ARRAY, GrandParent.class);
        AnnotatedType fieldType = GenericTypeReflector.getExactFieldType(field, parentType);
        assertArrayContains(A1_A2_A3_A4_A5, fieldType.getAnnotations());
        assertEquals(0, ((AnnotatedArrayType) fieldType).getAnnotatedGenericComponentType().getAnnotations().length);
    }

    @Test
    public void methodTypeAnnotationInheritanceTest() throws NoSuchMethodException {
        Method method = Inter.class.getMethod("cool");
        AnnotatedType returnType = GenericTypeReflector.getExactReturnType(method, CHILD_STRING);
        assertArrayContains(A1_A2_A3, returnType.getAnnotations());
        method = ChildArray.class.getMethod("cool");
        returnType = GenericTypeReflector.getExactReturnType(method, CHILD_STRING);
        assertArrayEquals(A4, returnType.getAnnotations());
    }
    
    private class GrandParent<@A3 T1> {
        public @A3 T1 annotated;
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
    
    private static void assertArrayContains(Annotation[] a1, Annotation[] a2) {
        assertArrayEquals(a1, sort(a2));
    }
    
    private static Annotation[] sort(Annotation[] annotations) {
        Arrays.sort(annotations, (o1, o2) -> o1.toString().compareTo(o2.toString()));
        return annotations;
    }
}
