package io.leangen.geantyref;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class AnnotatedTypeImpl implements AnnotatedType {

    protected Type type;
    protected Map<Class<? extends Annotation>, Annotation> annotations;

    public AnnotatedTypeImpl(Type type) {
        this(type, new Annotation[0]);
    }

    public AnnotatedTypeImpl(Type type, Annotation[] annotations) {
        this.type = Objects.requireNonNull(type);
        this.annotations = new HashMap<>();
        for (Annotation annotation : annotations) {
            this.annotations.put(annotation.annotationType(), annotation);
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return (T) annotations.get(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations.values().toArray(new Annotation[annotations.size()]);
    }

    //TODO return declared annotations, not all
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AnnotatedType)) {
            return false;
        }
        AnnotatedType otherType = (AnnotatedType) other;
        return this.getType().equals(otherType.getType()) && Arrays.equals(this.getAnnotations(), otherType.getAnnotations());
    }

    @Override
    public int hashCode() {
        return 31 * (this.getType().hashCode() + Arrays.hashCode(this.getAnnotations()));
    }
}
