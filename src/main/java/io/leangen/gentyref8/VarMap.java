/**
 * 
 */
package io.leangen.gentyref8;

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
import java.util.stream.Stream;

import static io.leangen.gentyref8.GenericTypeReflector.annotate;
import static io.leangen.gentyref8.GenericTypeReflector.updateAnnotations;
import static java.util.Arrays.stream;

/**
 * Mapping between type variables and actual parameters.
 *
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
class VarMap {
	private final Map<TypeVariable, AnnotatedType> map = new HashMap<>();

	/**
	 * Creates an empty VarMap
	 */
	VarMap() {
	}

	/**
	 * Creates a VarMap mapping the type parameters of the class used in <tt>type</tt> to their actual value.
	 */
	VarMap(AnnotatedParameterizedType type) {
		// loop over the type and its generic owners
		do {
			Class<?> clazz = (Class<?>) ((ParameterizedType)type.getType()).getRawType();
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

	void add(TypeVariable variable, AnnotatedType value) {
		map.put(variable, value);
	}

	void addAll(TypeVariable[] variables, AnnotatedType[] values) {
		assert variables.length == values.length;
		for (int i = 0; i < variables.length; i++) {
			map.put(variables[i], values[i]);
		}
	}

	VarMap(TypeVariable[] variables, AnnotatedType[] values) {
		addAll(variables, values);
	}

	AnnotatedType map(AnnotatedType type) {
		if (type.getType() instanceof Class) {
			return type;
		} else if (type instanceof AnnotatedTypeVariable) {
			TypeVariable tv = (TypeVariable) type.getType();
			if (!map.containsKey(tv)) {
				throw new UnresolvedTypeVariableException(tv);
			}
			Annotation[] merged = Stream.concat(Arrays.stream(type.getAnnotations()), Arrays.stream(map.get(tv).getAnnotations())).distinct().toArray(Annotation[]::new);
			return updateAnnotations(map.get(tv), merged);
		} else if (type instanceof AnnotatedParameterizedType) {
			AnnotatedParameterizedType pType = (AnnotatedParameterizedType) type;
			ParameterizedType inner = (ParameterizedType) pType.getType();
			AnnotatedType[] args = map(pType.getAnnotatedActualTypeArguments());
			Type[] rawArgs = stream(args).map(AnnotatedType::getType).toArray(Type[]::new);
			ParameterizedType v = new ParameterizedTypeImpl((Class)inner.getRawType(), rawArgs, inner.getOwnerType() == null ? null : map(annotate(inner.getOwnerType())).getType());
			return new AnnotatedParameterizedTypeImpl(v, pType.getAnnotations(), args);
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
			return AnnotatedArrayTypeImpl.createArrayType(map(((AnnotatedArrayType)type).getAnnotatedGenericComponentType()));
		} else {
			throw new RuntimeException("not implemented: mapping " + type.getClass() + " (" + type + ")");
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