package io.leangen.gentyref8;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

class GenericArrayTypeImpl implements GenericArrayType {
    private Type componentType;

    GenericArrayTypeImpl(Type componentType) {
        super();
        this.componentType = componentType;
    }

    static Class<?> createArrayType(Class<?> componentType) {
        // there's no (clean) other way to create a array class, than creating an instance of it
        return Array.newInstance(componentType, 0).getClass();
    }

    static Type createArrayType(Type componentType) {
        if (componentType instanceof Class) {
            return createArrayType((Class<?>) componentType);
        } else {
            return new GenericArrayTypeImpl(componentType);
        }
    }

    public Type getGenericComponentType() {
        return componentType;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GenericArrayType && componentType.equals(((GenericArrayType) obj).getGenericComponentType());
    }

    @Override
    public int hashCode() {
        return componentType.hashCode() * 7;
    }

    @Override
    public String toString() {
        return componentType + "[]";
    }
}
