/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package com.coekie.gentyref;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.coekie.gentyref.GenericTypeReflector.annotate;
import static com.coekie.gentyref.GenericTypeReflector.merge;
import static com.coekie.gentyref.GenericTypeReflector.updateAnnotations;
import static java.util.Arrays.stream;

/**
 * Mapping between type variables and actual parameters.
 *
 * @author Wouter Coekaerts {@literal (wouter@coekaerts.be)}
 * @author Bojan Tomic {@literal (veggen@gmail.com)}
 */
class VarMap {
    private final Map<TypeVariable, AnnotatedType> map = new HashMap<>();

    /**
     * Creates an empty VarMap
     */
    VarMap() {
    }

    /**
     * Creates a VarMap mapping the type parameters of the class used in {@code type} to their
     * actual value.
     */
    VarMap(AnnotatedParameterizedType type) {
        // loop over the type and its generic owners
        do {
            Class<?> clazz = (Class<?>) ((ParameterizedType) type.getType()).getRawType();
            AnnotatedType[] arguments = type.getAnnotatedActualTypeArguments();
            TypeVariable[] typeParameters = clazz.getTypeParameters();

            // since we're looping over two arrays in parallel, just to be sure check they have the same size
            if (arguments.length != typeParameters.length) {
                throw new IllegalStateException("The given type [" + type + "] is inconsistent: it has " +
                        arguments.length + " arguments instead of " + typeParameters.length);
            }

            for (int i = 0; i < arguments.length; i++) {
                add(typeParameters[i], arguments[i]);
            }

            Type owner = ((ParameterizedType) type.getType()).getOwnerType();
            type = (owner instanceof ParameterizedType) ? (AnnotatedParameterizedType) annotate(owner) : null;
        } while (type != null);
    }

    VarMap(ParameterizedType type) {
        this((AnnotatedParameterizedType) annotate(type));
    }

    VarMap(TypeVariable[] variables, AnnotatedType[] values) {
        addAll(variables, values);
    }

    void add(TypeVariable variable, AnnotatedType value) {
        map.put(variable, value);
    }

    void addAll(TypeVariable[] variables, AnnotatedType[] values) {
        assert variables.length == values.length;
        for (int i = 0; i < variables.length; i++) {
            map.put(variables[i], values[i]);
        }
    }

    AnnotatedType map(AnnotatedType type) {
        if (type.getType() instanceof Class) {
            return updateAnnotations(type, ((Class) type.getType()).getAnnotations());
        } else if (type instanceof AnnotatedTypeVariable) {
            TypeVariable tv = (TypeVariable) type.getType();
            if (!map.containsKey(tv)) {
                throw new UnresolvedTypeVariableException(tv);
            }
            TypeVariable varFromClass = map.keySet().stream().filter(key -> key.equals(tv)).findFirst().get();
            Annotation[] merged = merge(type.getAnnotations(), tv.getAnnotations(), map.get(tv).getAnnotations(), varFromClass.getAnnotations());
            return updateAnnotations(map.get(tv), merged);
        } else if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType pType = (AnnotatedParameterizedType) type;
            ParameterizedType inner = (ParameterizedType) pType.getType();
            Class raw = (Class) inner.getRawType();
            AnnotatedType[] typeParameters = new AnnotatedType[raw.getTypeParameters().length];
            for (int i = 0; i < typeParameters.length; i++) {
                AnnotatedType typeParameter = map(pType.getAnnotatedActualTypeArguments()[i]);
                typeParameters[i] = updateAnnotations(typeParameter, raw.getTypeParameters()[i].getAnnotations());
            }
            Type[] rawArgs = stream(typeParameters).map(AnnotatedType::getType).toArray(Type[]::new);
            ParameterizedType newInner = new ParameterizedTypeImpl((Class) inner.getRawType(), rawArgs, inner.getOwnerType() == null ? null : map(annotate(inner.getOwnerType())).getType());
            return new AnnotatedParameterizedTypeImpl(newInner, merge(pType.getAnnotations(), raw.getAnnotations()), typeParameters);
        } else if (type instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType wType = (AnnotatedWildcardType) type;
            AnnotatedType[] up = map(wType.getAnnotatedUpperBounds());
            AnnotatedType[] lw = map(wType.getAnnotatedLowerBounds());
            Type[] upperBounds;
            if (up == null || up.length == 0) {
                upperBounds = ((WildcardType) wType.getType()).getUpperBounds();
            } else {
                upperBounds = stream(up).map(AnnotatedType::getType).toArray(Type[]::new);
            }
            WildcardType w = new WildcardTypeImpl(upperBounds, stream(lw).map(AnnotatedType::getType).toArray(Type[]::new));
            return new AnnotatedWildcardTypeImpl(w, wType.getAnnotations(), lw, up);
        } else if (type instanceof AnnotatedArrayType) {
            return AnnotatedArrayTypeImpl.createArrayType(map(((AnnotatedArrayType) type).getAnnotatedGenericComponentType()), type.getAnnotations());
        } else {
            throw new RuntimeException("Not implemented: mapping " + type.getClass() + " (" + type + ")");
        }
    }

    AnnotatedType[] map(AnnotatedType[] types) {
        AnnotatedType[] result = new AnnotatedType[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = map(types[i]);
        }
        return result;
    }

    Type[] map(Type[] types) {
        AnnotatedType[] result = map(Arrays.stream(types).map(GenericTypeReflector::annotate).toArray(AnnotatedType[]::new));
        return Arrays.stream(result).map(AnnotatedType::getType).toArray(Type[]::new);
    }

    Type map(Type type) {
        return map(annotate(type)).getType();
    }
}
