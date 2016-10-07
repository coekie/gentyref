package io.leangen.gentyref8;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import static io.leangen.gentyref8.GenericTypeReflector.typeArraysEqual;

class AnnotatedParameterizedTypeImpl extends AnnotatedTypeImpl implements AnnotatedParameterizedType {

    private AnnotatedType[] typeArguments;

    public AnnotatedParameterizedTypeImpl(ParameterizedType rawType, Annotation[] annotations, AnnotatedType[] typeArguments) {
        super(rawType, annotations);
        this.typeArguments = typeArguments;
    }

    @Override
    public AnnotatedType[] getAnnotatedActualTypeArguments() {
        return typeArguments;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AnnotatedParameterizedType) || !super.equals(other)) {
            return false;
        }
        return typeArraysEqual(typeArguments, ((AnnotatedParameterizedType) other).getAnnotatedActualTypeArguments());
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(typeArguments);
    }
}
