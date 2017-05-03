/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

import static io.leangen.geantyref.GenericTypeReflector.typeArraysEqual;

class AnnotatedTypeVariableImpl extends AnnotatedTypeImpl implements AnnotatedTypeVariable {

    private AnnotatedType[] annotatedBounds;

    AnnotatedTypeVariableImpl(TypeVariable<?> type) {
        this(type, type.getAnnotations());
    }

    AnnotatedTypeVariableImpl(TypeVariable<?> type, Annotation[] annotations) {
        this(type, annotations, type.getAnnotatedBounds());
    }

    AnnotatedTypeVariableImpl(TypeVariable<?> type, Annotation[] annotations, AnnotatedType[] annotatedBounds) {
        super(type, annotations);
        if (annotatedBounds == null || annotatedBounds.length == 0) {
            annotatedBounds = new AnnotatedType[0];
        }
        validateBounds(type, annotatedBounds);
        this.annotatedBounds = annotatedBounds;
    }

    @Override
    public AnnotatedType[] getAnnotatedBounds() {
        return annotatedBounds;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AnnotatedTypeVariable) || !super.equals(other)) {
            return false;
        }
        return typeArraysEqual(annotatedBounds, ((AnnotatedTypeVariable) other).getAnnotatedBounds());
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ GenericTypeReflector.hashCode(annotatedBounds);
    }

    @Override
    public String toString() {
        return annotationsString() + ((TypeVariable) type).getName() + " extends " + typesString(annotatedBounds);
    }

    private static void validateBounds(TypeVariable type, AnnotatedType[] bounds) {
        if (type.getBounds().length != bounds.length) {
            throw new IllegalArgumentException("Incompatible bounds " + Arrays.toString(bounds) + " for type " + type.toString());
        }
        for (int i = 0; i < type.getBounds().length; i++) {
            if (GenericTypeReflector.erase(type.getBounds()[i]) != GenericTypeReflector.erase(bounds[i].getType())) {
                throw new IllegalArgumentException("Bound " + bounds[i].getType() + " incompatible with "
                        + type.getBounds()[i] + " in type " + type.toString());
            }
        }
    }
}
