/**
 * 
 */
package tech.leangen.gentyref8;

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
			Class<?> clazz = (Class<?>) ((ParameterizedType) type).getRawType();
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

			Type owner = ((ParameterizedType) type).getOwnerType();
			type = (owner instanceof ParameterizedType) ? (AnnotatedParameterizedType) annotate(owner) : null;
		} while (type != null);
	}

	private AnnotatedType annotate(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			TypeVariable<?>[] typeVars = ((Class<?>) pType.getRawType()).getTypeParameters();
			AnnotatedType[] args = new AnnotatedType[typeVars.length];

			for (int i = 0; i < typeVars.length; i++) {
				args[i] = new AnnotatedTypeVariableImpl(typeVars[i]);
			}
			return new AnnotatedParameterizedTypeImpl(pType, ((Class<?>) pType.getRawType()).getAnnotations(), args);
		}
		if (type instanceof Class) {
			return new AnnotatedTypeImpl(type, ((Class)type).getAnnotations());
		}
		throw new IllegalArgumentException("fuck if I know");
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
			return replaceAnnotations(map.get(tv), merged);
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
			WildcardType w = new WildcardTypeImpl(stream(up).map(AnnotatedType::getType).toArray(Type[]::new), stream(lw).map(AnnotatedType::getType).toArray(Type[]::new));
			return new AnnotatedWildcardTypeImpl(w, wType.getAnnotations(), lw, up);
		} else if (type instanceof AnnotatedArrayType) {
			return type;
//			return GenericArrayTypeImpl.createArrayType(map(((GenericArrayType)type).getGenericComponentType()));
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

	private AnnotatedType replaceAnnotations(AnnotatedType type, Annotation[] annotations) {
		if (type instanceof AnnotatedWildcardType) {
			AnnotatedWildcardType wType = (AnnotatedWildcardType) type;
			return new AnnotatedWildcardTypeImpl((WildcardType) type.getType(), annotations, wType.getAnnotatedLowerBounds(), wType.getAnnotatedUpperBounds());
		}
		if (type instanceof AnnotatedTypeVariable) {
			AnnotatedTypeVariable vType = (AnnotatedTypeVariable) type;
			return new AnnotatedTypeVariableImpl((TypeVariable<?>) vType.getType(), annotations);
		}
		if (type instanceof AnnotatedParameterizedType) {
			AnnotatedParameterizedType pType = (AnnotatedParameterizedType) type;
			return new AnnotatedParameterizedTypeImpl((ParameterizedType) pType.getType(), annotations, pType.getAnnotatedActualTypeArguments());
		}
		return new AnnotatedTypeImpl(type.getType(), annotations);
	}
}