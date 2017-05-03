/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;

import static io.leangen.geantyref.GenericTypeReflector.typeArraysEqual;

class AnnotatedParameterizedTypeImpl extends AnnotatedTypeImpl implements AnnotatedParameterizedType {

    private AnnotatedType[] typeArguments;

    AnnotatedParameterizedTypeImpl(ParameterizedType rawType, Annotation[] annotations, AnnotatedType[] typeArguments) {
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
        return 31 * super.hashCode() ^ GenericTypeReflector.hashCode(typeArguments);
    }

    @Override
    public String toString() {
        ParameterizedType rawType = (ParameterizedType) type;
        String rawName = GenericTypeReflector.getTypeName(rawType.getRawType());

        StringBuilder typeName = new StringBuilder();
        if (rawType.getOwnerType() != null) {
            typeName.append(GenericTypeReflector.getTypeName(rawType.getOwnerType())).append('.');

            String prefix = (rawType.getOwnerType() instanceof ParameterizedType) ? ((Class<?>) ((ParameterizedType) rawType.getOwnerType()).getRawType()).getName() + '$'
                    : ((Class<?>) rawType.getOwnerType()).getName() + '$';
            if (rawName.startsWith(prefix))
                rawName = rawName.substring(prefix.length());
        }
        typeName.append(rawName);
        return annotationsString() + typeName.toString() + "<" + typesString(typeArguments) + ">";
    }
}
