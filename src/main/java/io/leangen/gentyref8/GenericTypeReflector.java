package io.leangen.gentyref8;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Utility class for doing reflection on types.
 *
 * @author Wouter Coekaerts {@literal (wouter@coekaerts.be)}
 * @author Bojan Tomic {@literal (veggen@gmail.com)}
 */
public class GenericTypeReflector {
	private static final WildcardType UNBOUND_WILDCARD = new WildcardTypeImpl(new Type[]{Object.class}, new Type[]{});
	
	/**
	 * Returns the erasure of the given type.
	 */
	public static Class<?> erase(Type type) {
		if (type instanceof Class) {
			return (Class<?>)type;
		} else if (type instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType) type).getRawType();
		} else if (type instanceof TypeVariable) {
			TypeVariable<?> tv = (TypeVariable<?>)type;
			if (tv.getBounds().length == 0)
				return Object.class;
			else
				return erase(tv.getBounds()[0]);
		} else if (type instanceof GenericArrayType) {
			GenericArrayType aType = (GenericArrayType) type;
			return GenericArrayTypeImpl.createArrayType(erase(aType.getGenericComponentType()));
		} else if (type instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType) type;
			Type[] lowerBounds = wildcardType.getLowerBounds();
			return erase(lowerBounds.length > 0 ? lowerBounds[0] : wildcardType.getUpperBounds()[0]);
		} else {
			throw new RuntimeException("not supported: " + type.getClass());
		}
	}
	
	/**
	 * Maps type parameters in a type to their values. 
	 * @param toMapType Type possibly containing type arguments
	 * @param typeAndParams must be either ParameterizedType, or (in case there are no type arguments, or it's a raw type) Class
	 * @return toMapType, but with type parameters from typeAndParams replaced.
	 */
	public static AnnotatedType mapTypeParameters(AnnotatedType toMapType, AnnotatedType typeAndParams) {
		if (isMissingTypeParameters(typeAndParams.getType())) {
			return new AnnotatedTypeImpl(erase(toMapType.getType()), toMapType.getAnnotations());
		} else {
			VarMap varMap = new VarMap();
			AnnotatedType handlingTypeAndParams = typeAndParams;
			while(handlingTypeAndParams instanceof AnnotatedParameterizedType) {
				AnnotatedParameterizedType pType = (AnnotatedParameterizedType)handlingTypeAndParams;
				Class<?> clazz = (Class<?>)((ParameterizedType) pType.getType()).getRawType(); // getRawType should always be Class
				TypeVariable[] vars = clazz.getTypeParameters();
				varMap.addAll(vars, pType.getAnnotatedActualTypeArguments());
				Type owner = ((ParameterizedType) pType.getType()).getOwnerType();
				handlingTypeAndParams = owner == null ? null : annotate(owner);
			}
			return varMap.map(toMapType);
		}
	}

	/**
	 * Checks if the given type is a class that is supposed to have type parameters, but doesn't.
	 * In other words, if it's a really raw type.
	 */
	public static boolean isMissingTypeParameters(Type type) {
		if (type instanceof Class) {
			for (Class<?> clazz = (Class<?>) type; clazz != null; clazz = clazz.getEnclosingClass()) {
				if (clazz.getTypeParameters().length != 0)
					return true;
			}
			return false;
		} else if (type instanceof ParameterizedType) {
			return false;
		} else {
			throw new AssertionError("Unexpected type " + type.getClass());
		}
	}
	
	/**
	 * Returns a type representing the class, with all type parameters the unbound wildcard ("?").
	 * For example, <tt>addWildcardParameters(Map.class)</tt> returns a type representing <tt>Map&lt;?,?&gt;</tt>.
	 * @return <ul>
	 * <li>If clazz is a class or interface without type parameters, clazz itself is returned.</li>
	 * <li>If clazz is a class or interface with type parameters, an instance of ParameterizedType is returned.</li>
	 * <li>if clazz is an array type, an array type is returned with unbound wildcard parameters added in the the component type.   
	 * </ul>
	 */
	public static Type addWildcardParameters(Class<?> clazz) {
		if (clazz.isArray()) {
			return GenericArrayTypeImpl.createArrayType(addWildcardParameters(clazz.getComponentType()));
		} else if (isMissingTypeParameters(clazz)) {
			TypeVariable<?>[] vars = clazz.getTypeParameters();
			Type[] arguments = new Type[vars.length];
			Arrays.fill(arguments, UNBOUND_WILDCARD);
			Type owner = clazz.getDeclaringClass() == null ? null : addWildcardParameters(clazz.getDeclaringClass());
			return new ParameterizedTypeImpl(clazz, arguments, owner);
		} else {
			return clazz;
		}
	}
	
	/**
	 * Finds the most specific supertype of <tt>type</tt> whose erasure is <tt>searchClass</tt>.
	 * In other words, returns a type representing the class <tt>searchClass</tt> plus its exact type parameters in <tt>type</tt>.
	 * 
	 * <ul>
	 * <li>Returns an instance of {@link ParameterizedType} if <tt>searchClass</tt> is a real class or interface and <tt>type</tt> has parameters for it</li>
	 * <li>Returns an instance of {@link GenericArrayType} if <tt>searchClass</tt> is an array type, and <tt>type</tt> has type parameters for it</li>
	 * <li>Returns an instance of {@link Class} if <tt>type</tt> is a raw type, or has no type parameters for <tt>searchClass</tt></li>
	 * <li>Returns null if <tt>searchClass</tt> is not a superclass of type.</li>
	 * </ul>
	 * 
	 * <p>For example, with <tt>class StringList implements List&lt;String&gt;</tt>, <tt>getExactSuperType(StringList.class, Collection.class)</tt>
	 * returns a {@link ParameterizedType} representing <tt>Collection&lt;String&gt;</tt>.
	 * </p>
	 */
	public static AnnotatedType getExactSuperType(AnnotatedType type, Class<?> searchClass) {
		if (type instanceof AnnotatedParameterizedType || type.getType() instanceof Class || type instanceof AnnotatedArrayType) {
			Class<?> clazz = erase(type.getType());

			if (searchClass == clazz) {
				return type;
			}

			if (! searchClass.isAssignableFrom(clazz))
				return null;
		}

		for (AnnotatedType superType: getExactDirectSuperTypes(type)) {
			AnnotatedType result = getExactSuperType(superType, searchClass);
			if (result != null)
				return result;
		}

		return null;
	}

	public static Type getExactSuperType(Type type, Class<?> searchClass) {
		AnnotatedType superType = getExactSuperType(annotate(type), searchClass);
		return superType == null ? null : superType.getType();
	}

	/**
	 * Gets the type parameter for a given type that is the value for a given type variable.
	 * For example, with <tt>class StringList implements List&lt;String&gt;</tt>,
	 * <tt>getTypeParameter(StringList.class, Collection.class.getTypeParameters()[0])</tt>
	 * returns <tt>String</tt>. 
	 *  
	 * @param type The type to inspect.
	 * @param variable The type variable to find the value for.
	 * @return The type parameter for the given variable. Or null if type is not a subtype of the
	 * 	type that declares the variable, or if the variable isn't known (because of raw types).
	 */
	public static AnnotatedType getTypeParameter(AnnotatedType type, TypeVariable<? extends Class<?>> variable) {
		Class<?> clazz = variable.getGenericDeclaration();
		AnnotatedType superType = getExactSuperType(type, clazz);
		if (superType instanceof AnnotatedParameterizedType) {
			int index = Arrays.asList(clazz.getTypeParameters()).indexOf(variable);
			return ((AnnotatedParameterizedType)superType).getAnnotatedActualTypeArguments()[index];
		} else {
			return null;
		}
	}

	public static Type getTypeParameter(Type type, TypeVariable<? extends Class<?>> variable) {
		return getTypeParameter(annotate(type), variable).getType();
	}

	/**
	 * Checks if the capture of subType is a subtype of superType
	 */
	public static boolean isSuperType(Type superType, Type subType) {
		if (superType instanceof ParameterizedType || superType instanceof Class || superType instanceof GenericArrayType) {
			Class<?> superClass = erase(superType);
			AnnotatedType annotatedMappedSubType = getExactSuperType(capture(annotate(subType)), superClass);
			Type mappedSubType = annotatedMappedSubType == null ? null : annotatedMappedSubType.getType();
			if (mappedSubType == null) {
				return false;
			} else if (superType instanceof Class<?>) {
				return true;
			} else if (mappedSubType instanceof Class<?>) {
				// TODO treat supertype by being raw type differently ("supertype, but with warnings")
				return true; // class has no parameters, or it's a raw type
			} else if (mappedSubType instanceof GenericArrayType) {
				Type superComponentType = getArrayComponentType(superType);
				assert superComponentType != null;
				Type mappedSubComponentType = getArrayComponentType(mappedSubType);
				assert mappedSubComponentType != null;
				return isSuperType(superComponentType, mappedSubComponentType);
			} else {
				assert mappedSubType instanceof ParameterizedType;
				ParameterizedType pMappedSubType = (ParameterizedType) mappedSubType;
				assert pMappedSubType.getRawType() == superClass;
				ParameterizedType pSuperType = (ParameterizedType)superType;

				Type[] superTypeArgs = pSuperType.getActualTypeArguments();
				Type[] subTypeArgs = pMappedSubType.getActualTypeArguments();
				assert superTypeArgs.length == subTypeArgs.length;
				for (int i = 0; i < superTypeArgs.length; i++) {
					if (! contains(superTypeArgs[i], subTypeArgs[i])) {
						return false;
					}
				}
				// params of the class itself match, so if the owner types are supertypes too, it's a supertype.
				return pSuperType.getOwnerType() == null || isSuperType(pSuperType.getOwnerType(), pMappedSubType.getOwnerType());
			}
		} else if (superType instanceof CaptureType) {
			if (superType.equals(subType))
				return true;
			for (Type lowerBound : ((CaptureType) superType).getLowerBounds()) {
				if (isSuperType(lowerBound, subType)) {
					return true;
				}
			}
			return false;
		} else if (superType instanceof GenericArrayType) {
			return isArraySupertype(superType, subType);
		} else {
			throw new RuntimeException("not implemented: " + superType.getClass());
		}
	}

	private static boolean isArraySupertype(Type arraySuperType, Type subType) {
		Type superTypeComponent = getArrayComponentType(arraySuperType);
		assert superTypeComponent != null;
		Type subTypeComponent = getArrayComponentType(subType);
		if (subTypeComponent == null) { // subType is not an array type
			return false;
		} else {
			return isSuperType(superTypeComponent, subTypeComponent);
		}
	}

	/**
	 * If type is an array type, returns the type of the component of the array.
	 * Otherwise, returns null.
	 */
	public static AnnotatedType getArrayComponentType(AnnotatedType type) {
		if (type.getType() instanceof Class) {
			Class<?> clazz = (Class<?>)type.getType();
			return new AnnotatedTypeImpl(clazz.getComponentType(), clazz.getAnnotations());
		} else if (type instanceof AnnotatedArrayType) {
			AnnotatedArrayType aType = (AnnotatedArrayType) type;
			return aType.getAnnotatedGenericComponentType();
		} else {
			return null;
		}
	}

	public static Type getArrayComponentType(Type type) {
		return getArrayComponentType(annotate(type)).getType();
	}

	private static boolean contains(Type containingType, Type containedType) {
		if (containingType instanceof WildcardType) {
			WildcardType wContainingType = (WildcardType)containingType;
			for (Type upperBound : wContainingType.getUpperBounds()) {
				if (! isSuperType(upperBound, containedType)) {
					return false;
				}
			}
			for (Type lowerBound : wContainingType.getLowerBounds()) {
				if (! isSuperType(containedType, lowerBound)) {
					return false;
				}
			}
			return true;
		} else {
			return containingType.equals(containedType);
		}
	}

	/**
	 * Returns the direct supertypes of the given type. Resolves type parameters.
	 */
	private static AnnotatedType[] getExactDirectSuperTypes(AnnotatedType type) {
		if (type instanceof AnnotatedParameterizedType || (type != null && type.getType() instanceof Class)) {
			Class<?> clazz;
			if (type instanceof AnnotatedParameterizedType) {
				clazz = (Class<?>)((ParameterizedType)type.getType()).getRawType();
			} else {
				// TODO primitive types?
				clazz = (Class<?>)type.getType();
				if (clazz.isArray())
					return getArrayExactDirectSuperTypes(annotate(clazz));
			}

			AnnotatedType[] superInterfaces = clazz.getAnnotatedInterfaces();
			AnnotatedType superClass = clazz.getAnnotatedSuperclass();

			// the only supertype of an interface without superinterfaces is Object
			if (superClass == null && superInterfaces.length == 0 && clazz.isInterface()) {
				return new AnnotatedType[] {new AnnotatedTypeImpl(Object.class)};
			}

			AnnotatedType[] result;
			int resultIndex;
			if (superClass == null) {
				result = new AnnotatedType[superInterfaces.length];
				resultIndex = 0;
			} else {
				result = new AnnotatedType[superInterfaces.length + 1];
				resultIndex = 1;
				result[0] = mapTypeParameters(superClass, type);
			}
			for (AnnotatedType superInterface : superInterfaces) {
				result[resultIndex++] = mapTypeParameters(superInterface, type);
			}

			return result;
		} else if (type instanceof AnnotatedTypeVariable) {
			AnnotatedTypeVariable tv = (AnnotatedTypeVariable) type;
			return tv.getAnnotatedBounds();
		} else if (type instanceof AnnotatedWildcardType) {
			// This should be a rare case: normally this wildcard is already captured.
			// But it does happen if the upper bound of a type variable contains a wildcard
			// TODO shouldn't upper bound of type variable have been captured too? (making this case impossible?)
			return ((AnnotatedWildcardType) type).getAnnotatedUpperBounds();
		} else if (type instanceof AnnotatedCaptureTypeImpl) {
			return ((AnnotatedCaptureTypeImpl)type).getAnnotatedUpperBounds();
		} else if (type instanceof AnnotatedArrayType) {
			return getArrayExactDirectSuperTypes(type);
		} else if (type == null) {
			throw new NullPointerException();
		} else {
			throw new RuntimeException("not implemented type: " + type);
		}
	}

	private static AnnotatedType[] getArrayExactDirectSuperTypes(AnnotatedType arrayType) {
		// see http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.10.3
		AnnotatedType typeComponent = getArrayComponentType(arrayType);

		AnnotatedType[] result;
		int resultIndex;
		if (typeComponent != null && typeComponent.getType() instanceof Class && ((Class<?>)typeComponent.getType()).isPrimitive()) {
			resultIndex = 0;
			result = new AnnotatedType[3];
		} else {
			AnnotatedType[] componentSupertypes = getExactDirectSuperTypes(typeComponent);
			result = new AnnotatedType[componentSupertypes.length + 3];
			for (resultIndex = 0; resultIndex < componentSupertypes.length; resultIndex++) {
				result[resultIndex] = AnnotatedArrayTypeImpl.createArrayType(componentSupertypes[resultIndex]);
			}
		}
		result[resultIndex++] = new AnnotatedTypeImpl(Object.class);
		result[resultIndex++] = new AnnotatedTypeImpl(Cloneable.class);
		result[resultIndex++] = new AnnotatedTypeImpl(Serializable.class);
		return result;
	}

	/**
	 * Returns the exact return type of the given method in the given type.
	 * This may be different from <tt>m.getGenericReturnType()</tt> when the method was declared in a superclass,
	 * or <tt>type</tt> has a type parameter that is used in the return type, or <tt>type</tt> is a raw type.
	 */
	public static AnnotatedType getExactReturnType(Method m, AnnotatedType type) {
		AnnotatedType returnType = m.getAnnotatedReturnType();
		AnnotatedType exactDeclaringType = getExactSuperType(capture(type), m.getDeclaringClass());
		if (exactDeclaringType == null) { // capture(type) is not a subtype of m.getDeclaringClass()
			throw new IllegalArgumentException("The method " + m + " is not a member of type " + type);
		}
		return mapTypeParameters(returnType, exactDeclaringType);
	}

	public static Type getExactReturnType(Method m, Type type) {
		return getExactReturnType(m, annotate(type)).getType();
	}

	/**
	 * Returns the exact type of the given field in the given type.
	 * This may be different from <tt>f.getGenericType()</tt> when the field was declared in a superclass,
	 * or <tt>type</tt> has a type parameter that is used in the type of the field, or <tt>type</tt> is a raw type.
	 */
	public static AnnotatedType getExactFieldType(Field f, AnnotatedType type) {
		AnnotatedType returnType = f.getAnnotatedType();
		AnnotatedType exactDeclaringType = getExactSuperType(capture(type), f.getDeclaringClass());
		if (exactDeclaringType == null) { // capture(type) is not a subtype of f.getDeclaringClass()
			throw new IllegalArgumentException("The field " + f + " is not a member of type " + type);
		}
		return mapTypeParameters(returnType, exactDeclaringType);
	}

	public static Type getExactFieldType(Field f, Type type) {
		return getExactFieldType(f, annotate(type)).getType();
	}

	/**
	 * Returns the exact parameter types of the given method in the given type.
	 * This may be different from <tt>m.getGenericParameterTypes()</tt> when the method was declared in a superclass,
	 * or <tt>type</tt> has a type parameter that is used in one of the parameters, or <tt>type</tt> is a raw type.
	 */
	public static AnnotatedType[] getExactParameterTypes(Method m, AnnotatedType type) {
		AnnotatedType[] parameterTypes = m.getAnnotatedParameterTypes();
		AnnotatedType exactDeclaringType = getExactSuperType(capture(type), m.getDeclaringClass());
		if (exactDeclaringType == null) { // capture(type) is not a subtype of m.getDeclaringClass()
			throw new IllegalArgumentException("The method " + m + " is not a member of type " + type);
		}

		AnnotatedType[] result = new AnnotatedType[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			result[i] = mapTypeParameters(parameterTypes[i], exactDeclaringType);
		}
		return result;
	}

	public static Type[] getExactParameterTypes(Method m, Type type) {
		return Arrays.stream(getExactParameterTypes(m, annotate(type))).map(AnnotatedType::getType).toArray(Type[]::new);
	}

	/**
	 * Applies capture conversion to the given type.
	 */
	public static AnnotatedType capture(AnnotatedType type) {
		if (type instanceof AnnotatedParameterizedType) {
			return capture((AnnotatedParameterizedType)type);
		} else {
			return type;
		}
	}
//
	/**
	 * Applies capture conversion to the given type.
	 * @see #capture(AnnotatedType)
	 */
	public static AnnotatedParameterizedType capture(AnnotatedParameterizedType type) {
		// the map from parameters to their captured equivalent

		VarMap varMap = new VarMap();
		// list of CaptureTypes we've created but aren't fully initialized yet
		// we can only initialize them *after* we've fully populated varMap
		List<AnnotatedCaptureTypeImpl> toInit = new ArrayList<>();

		Class<?> clazz = (Class<?>)((ParameterizedType)type.getType()).getRawType();
		AnnotatedType[] arguments = type.getAnnotatedActualTypeArguments();
		TypeVariable<?>[] vars = clazz.getTypeParameters();
		AnnotatedType[] capturedArguments = new AnnotatedType[arguments.length];

		assert arguments.length == vars.length; // NICE throw an explaining exception

		for (int i = 0; i < arguments.length; i++) {
			AnnotatedType argument = arguments[i];
			if (argument instanceof AnnotatedWildcardType) {
				AnnotatedCaptureTypeImpl captured = new AnnotatedCaptureTypeImpl((AnnotatedWildcardType)argument, new AnnotatedTypeVariableImpl(vars[i]));
				argument = captured;
				toInit.add(captured);
			}
			capturedArguments[i] = argument;
			varMap.add(vars[i], argument);
		}
		for (AnnotatedCaptureTypeImpl captured : toInit) {
			captured.init(varMap);
		}
		ParameterizedType inner = (ParameterizedType) type.getType();
		AnnotatedType ownerType = (inner.getOwnerType() == null) ? null : capture(annotate(inner.getOwnerType()));
		Type[] rawArgs = Arrays.stream(capturedArguments).map(an -> an.getType()).toArray(Type[]::new);
		ParameterizedType nn = new ParameterizedTypeImpl(clazz, rawArgs, ownerType == null ? null : ownerType.getType());
		return new AnnotatedParameterizedTypeImpl(nn, type.getAnnotations(), capturedArguments);
	}

	/**
	 * Returns the display name of a Type.
	 */
	public static String getTypeName(Type type) {
		if(type instanceof Class) {
			Class<?> clazz = (Class<?>) type;
			return clazz.isArray() ? (getTypeName(clazz.getComponentType()) + "[]") : clazz.getName();
		} else {
			return type.toString();
		}
	}
	
	/**
	 * Returns list of classes and interfaces that are supertypes of the given type.
	 * For example given this class:
	 * <tt>class {@literal Foo<A extends Number & Iterable<A>, B extends A>}</tt><br>
	 * calling this method on type parameters <tt>B</tt> (<tt>Foo.class.getTypeParameters()[1]</tt>)
	 * returns a list containing <tt>Number</tt> and <tt>Iterable</tt>.
	 * <p>
	 * This is mostly useful if you get a type from one of the other methods in <tt>GenericTypeReflector</tt>,
	 * but you don't want to deal with all the different sorts of types,
	 * and you are only really interested in concrete classes and interfaces.
	 * </p>
	 *  
	 * @return A List of classes, each of them a supertype of the given type.
	 * 	If the given type is a class or interface itself, returns a List with just the given type.
	 *  The list contains no duplicates, and is ordered in the order the upper bounds are defined on the type.
	 */
	public static List<Class<?>> getUpperBoundClassAndInterfaces(Type type) {
		LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
		buildUpperBoundClassAndInterfaces(type, result);
		return new ArrayList<>(result);
	}

	public static AnnotatedType annotate(Type type) {
		return annotate(type, new HashMap<>());
	}

	public static AnnotatedType annotate(Type type, Annotation[] annotations) {
        return updateAnnotations(annotate(type), annotations);
    }

	private static AnnotatedType annotate(Type type, Map<CaptureCacheKey, AnnotatedType> cache) {
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterized = (ParameterizedType) type;
			AnnotatedType[] params = new AnnotatedType[parameterized.getActualTypeArguments().length];
			for (int i = 0; i < params.length; i++) {
				AnnotatedType param = annotate(parameterized.getActualTypeArguments()[i], cache);
				params[i] = updateAnnotations(param, erase(type).getTypeParameters()[i].getAnnotations());
			}
			return new AnnotatedParameterizedTypeImpl(parameterized, erase(type).getAnnotations(), params);
		}
		if (type instanceof CaptureType) {
			CaptureCacheKey key = new CaptureCacheKey(((CaptureType) type));
			if (cache.containsKey(key)) {
				return cache.get(key);
			}
			CaptureType capture = ((CaptureType) type);
			AnnotatedCaptureType annotatedCapture = new AnnotatedCaptureTypeImpl(
					((AnnotatedWildcardType) annotate(capture.getWildcardType(), cache)),
					(AnnotatedTypeVariable) annotate(capture.getTypeVariable(), cache),
					null);

			cache.put(new CaptureCacheKey(capture), annotatedCapture);
			AnnotatedType[] upperBounds = Arrays.stream(capture.getUpperBounds())
					.map(bound -> annotate(bound, cache))
					.toArray(AnnotatedType[]::new);
			annotatedCapture.setAnnotatedUpperBounds(upperBounds);
			return annotatedCapture;
		}
		if (type instanceof WildcardType) {
			WildcardType wildcard = (WildcardType) type;
			AnnotatedType[] lowerBounds = Arrays.stream(wildcard.getLowerBounds())
					.map(bound -> annotate(bound, cache))
					.toArray(AnnotatedType[]::new);
			AnnotatedType[] upperBounds = Arrays.stream(wildcard.getUpperBounds())
					.map(bound -> annotate(bound, cache))
					.toArray(AnnotatedType[]::new);
			return new AnnotatedWildcardTypeImpl(wildcard, erase(type).getAnnotations(), lowerBounds, upperBounds);
		}
		if (type instanceof TypeVariable) {
			return new AnnotatedTypeVariableImpl((TypeVariable<?>) type);
		}
		if (type instanceof GenericArrayType) {
			GenericArrayType genArray = (GenericArrayType) type;
			return new AnnotatedArrayTypeImpl(genArray, new Annotation[0], annotate(genArray.getGenericComponentType()));
		}
		if (type instanceof Class) {
			return new AnnotatedTypeImpl(type, ((Class) type).getAnnotations());
		}
		throw new IllegalArgumentException("Unrecognized type: " + type.getTypeName());
	}

	public static AnnotatedType replaceAnnotations(AnnotatedType original, Annotation[] annotations) {
		if (original instanceof AnnotatedParameterizedType) {
			return new AnnotatedParameterizedTypeImpl((ParameterizedType) original.getType(), annotations,
					((AnnotatedParameterizedType) original).getAnnotatedActualTypeArguments());
		}
		if (original instanceof AnnotatedCaptureType) {
			return new AnnotatedCaptureTypeImpl(
					((AnnotatedCaptureType) original).getAnnotatedWildcardType(),
					((AnnotatedCaptureType) original).getAnnotatedTypeVariable(),
					((AnnotatedCaptureType) original).getAnnotatedUpperBounds(),
					annotations);
		}
		if (original instanceof AnnotatedWildcardType) {
			return new AnnotatedWildcardTypeImpl((WildcardType) original.getType(), annotations,
					((AnnotatedWildcardType) original).getAnnotatedLowerBounds(),
					((AnnotatedWildcardType) original).getAnnotatedUpperBounds());
		}
		if (original instanceof AnnotatedTypeVariable) {
			return new AnnotatedTypeVariableImpl((TypeVariable<?>) original.getType(), annotations);
		}
		if (original instanceof AnnotatedArrayType) {
			return new AnnotatedArrayTypeImpl((GenericArrayType) original.getType(), annotations,
					((AnnotatedArrayType) original).getAnnotatedGenericComponentType());
		}
		return new AnnotatedTypeImpl(original.getType(), annotations);
	}

	public static AnnotatedType updateAnnotations(AnnotatedType original, Annotation[] annotations) {
		if (Arrays.equals(original.getAnnotations(), annotations)) {
			return original;
		}
		return replaceAnnotations(original, merge(original.getAnnotations(), annotations));
	}

	public static AnnotatedType clone(AnnotatedType type) {
        return replaceAnnotations(type, type.getAnnotations());
    }

    /**
     * Returns an array containing all annotations declared by the given annotated types, without duplicates.
     *
     * @param types Annotated types whose annotations are to be extracted and merged
     * @return An array containing all annotations declared by the given annotated types, without duplicates
     */
    public static Annotation[] getMergedAnnotations(AnnotatedType... types) {
        return Arrays.stream(types)
                .flatMap(type -> Arrays.stream(type.getAnnotations()))
                .distinct()
                .toArray(Annotation[]::new);
    }

    /**
     * Merges an arbitrary number of annotations arrays, and removes duplicates.
     *
     * @param annotations Annotation arrays to merge and deduplicate
     * @return An array containing all annotations from the given arrays, without duplicates
     */
    public static Annotation[] merge(Annotation[]... annotations) {
        return Arrays.stream(annotations).reduce(
                (acc, arr) -> Stream.concat(Arrays.stream(acc), Arrays.stream(arr)).distinct().toArray(Annotation[]::new))
                .orElse(new Annotation[0]);
    }

	public static boolean typeArraysEqual(AnnotatedType[] t1, AnnotatedType[] t2) {
		if (t1 == null && t2 != null) return false;
		if (t2 == null && t1 != null) return false;
		if (t1 == null) return true;
		if (t1.length != t2.length) return false;

		for (int i = 0; i < t1.length; i++) {
			if (!t1[i].getType().equals(t2[i].getType()) || !Arrays.equals(t1[i].getAnnotations(), t2[i].getAnnotations())) {
				return false;
			}
		}
		return true;
	}

    public static boolean equals(AnnotatedType t1, AnnotatedType t2) {
        Objects.requireNonNull(t1);
        Objects.requireNonNull(t2);
        t1 = t1 instanceof AnnotatedTypeImpl ? t1 : clone(t1);
        t2 = t2 instanceof AnnotatedTypeImpl ? t2 : clone(t2);

        return t1.equals(t2);
    }

	/**
	 * Helper method for getUpperBoundClassAndInterfaces, adding the result to the given set.
	 */
	private static void buildUpperBoundClassAndInterfaces(Type type, Set<Class<?>> result) {
		if (type instanceof ParameterizedType || type instanceof Class<?>) {
			result.add(erase(type));
			return;
		}

		for (AnnotatedType superType : getExactDirectSuperTypes(annotate(type))) {
			buildUpperBoundClassAndInterfaces(superType.getType(), result);
		}
	}

	private static class CaptureCacheKey {
		CaptureType capture;

		CaptureCacheKey(CaptureType capture) {
			this.capture = capture;
		}

		@Override
		public int hashCode() {
			return capture.getWildcardType().hashCode() + capture.getTypeVariable().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof CaptureCacheKey)) {
				return false;
			}
			CaptureType other = ((CaptureCacheKey) obj).capture;
			if (!capture.getWildcardType().equals(other.getWildcardType())
					|| !capture.getTypeVariable().equals(other.getTypeVariable())) {
				return false;
			}
			return Arrays.equals(capture.getUpperBounds(), other.getUpperBounds());
		}
	}
}
