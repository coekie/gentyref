package io.leangen.gentyref8;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericArrayType;

class AnnotatedArrayTypeImpl extends AnnotatedTypeImpl implements AnnotatedArrayType {

    private AnnotatedType componentType;

    public AnnotatedArrayTypeImpl(GenericArrayType type, Annotation[] annotations, AnnotatedType componentType) {
        super(type, annotations);
        this.componentType = componentType;
    }

    public AnnotatedArrayTypeImpl(Class type, Class componentType) {
        super(type, type.getAnnotations());
        this.componentType = new AnnotatedTypeImpl(componentType, componentType.getAnnotations());
    }

    static AnnotatedType createArrayType(AnnotatedType componentType) {
        if (componentType.getType() instanceof Class) {
            return new AnnotatedArrayTypeImpl(GenericArrayTypeImpl.createArrayType((Class<?>) componentType.getType()), (Class) componentType.getType());
        } else {
            return new AnnotatedArrayTypeImpl(new GenericArrayTypeImpl(componentType.getType()), new Annotation[0], componentType);
        }
    }

    @Override
    public AnnotatedType getAnnotatedGenericComponentType() {
        return componentType;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AnnotatedArrayType
                && super.equals(other)
                && ((AnnotatedArrayType) other).getAnnotatedGenericComponentType().equals(this.componentType);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + componentType.hashCode();
    }
}
