/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package com.coekie.gentyref;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

class AnnotatedArrayTypeImpl extends AnnotatedTypeImpl implements AnnotatedArrayType {

    private AnnotatedType componentType;

    AnnotatedArrayTypeImpl(Type type, Annotation[] annotations, AnnotatedType componentType) {
        super(type, annotations);
        this.componentType = componentType;
    }

    static AnnotatedArrayType createArrayType(AnnotatedType componentType, Annotation[] annotations) {
        return new AnnotatedArrayTypeImpl(GenericArrayTypeImpl.createArrayType(componentType.getType()), annotations, componentType);
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
        return super.hashCode() ^ componentType.hashCode();
    }

    @Override
    public String toString() {
        return componentType.toString() + " " + annotationsString() + "[]";
    }
}
