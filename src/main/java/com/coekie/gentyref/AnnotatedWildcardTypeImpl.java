/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package com.coekie.gentyref;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

import static com.coekie.gentyref.GenericTypeReflector.typeArraysEqual;

class AnnotatedWildcardTypeImpl extends AnnotatedTypeImpl implements AnnotatedWildcardType {

    private AnnotatedType[] lowerBounds;
    private AnnotatedType[] upperBounds;

    AnnotatedWildcardTypeImpl(WildcardType type, Annotation[] annotations, AnnotatedType[] lowerBounds, AnnotatedType[] upperBounds) {
        super(type, annotations);
        if (lowerBounds == null || lowerBounds.length == 0) {
            lowerBounds = new AnnotatedType[0];
        }
        if (upperBounds == null || upperBounds.length == 0) {
            upperBounds = new AnnotatedType[1];
            upperBounds[0] = GenericTypeReflector.annotate(Object.class);
        }
        validateBounds(type, lowerBounds, upperBounds);
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
    }

    @Override
    public AnnotatedType[] getAnnotatedLowerBounds() {
        return lowerBounds;
    }

    @Override
    public AnnotatedType[] getAnnotatedUpperBounds() {
        return upperBounds;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AnnotatedWildcardType) || !super.equals(other)) {
            return false;
        }
        return typeArraysEqual(lowerBounds, ((AnnotatedWildcardType) other).getAnnotatedLowerBounds())
                && typeArraysEqual(upperBounds, ((AnnotatedWildcardType) other).getAnnotatedUpperBounds());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() ^ GenericTypeReflector.hashCode(lowerBounds) ^ GenericTypeReflector.hashCode(upperBounds);
    }

    @Override
    public String toString() {
        if (lowerBounds.length > 0) {
            return annotationsString() + "? super " + typesString(lowerBounds);
        } else if (upperBounds.length == 0 && upperBounds[0].getType() == Object.class) {
            return annotationsString() + "?";
        } else {
            return annotationsString() + "? extends " + typesString(upperBounds);
        }
    }
    
    private static void validateBounds(WildcardType type, AnnotatedType[] lowerBounds, AnnotatedType[] upperBounds) {
        if (type.getLowerBounds().length != lowerBounds.length) {
            throw new IllegalArgumentException("Incompatible lower bounds " + Arrays.toString(lowerBounds) + " for type " + type.toString());
        }
        if (type.getUpperBounds().length != upperBounds.length) {
            throw new IllegalArgumentException("Incompatible upper bounds " + Arrays.toString(upperBounds) + " for type " + type.toString());
        }
        for (int i = 0; i < type.getLowerBounds().length; i++) {
            if (GenericTypeReflector.erase(type.getLowerBounds()[i]) != GenericTypeReflector.erase(lowerBounds[i].getType())) {
                throw new IllegalArgumentException("Bound " + lowerBounds[i].getType() + " incompatible with "
                        + type.getLowerBounds()[i] + " in type " + type.toString());
            }
        }
        for (int i = 0; i < type.getUpperBounds().length; i++) {
            if (GenericTypeReflector.erase(type.getUpperBounds()[i]) != GenericTypeReflector.erase(upperBounds[i].getType())) {
                throw new IllegalArgumentException("Bound " + upperBounds[i].getType() + " incompatible with "
                        + type.getUpperBounds()[i] + " in type " + type.toString());
            }
        }
    }
}
