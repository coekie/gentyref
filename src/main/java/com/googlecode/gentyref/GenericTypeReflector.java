package com.googlecode.gentyref;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for doing reflection on types.
 * 
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
public class GenericTypeReflector {
	
	private static Class<?> getRawType(Type type) {
		if (type instanceof Class) {
			return (Class<?>)type;
		} else if (type instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType) type).getRawType();
		} else if (type instanceof TypeVariable) {
			TypeVariable<?> tv = (TypeVariable<?>)type;
			if (tv.getBounds().length == 0)
				return Object.class;
			else
				return getRawType(tv.getBounds()[0]);
		} else {
			// TODO at least support CaptureType here
			throw new RuntimeException("not supported: " + type.getClass());
		}
	}
	
	/**
	 * Maps type parameters in a type to their values. 
	 * @param toMapType Type possibly containing type arguments
	 * @param typeAndParams must be either ParameterizedType, or (in case there are no type arguments, or it's a raw type) Class
	 * @return toMapType, but with type parameters from typeAndParams replaced.
	 */
	private static Type mapTypeParameters(Type toMapType, Type typeAndParams) {
		Class<?> clazz;
		Type[] arguments;
		
		if (typeAndParams instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType)typeAndParams;
			clazz = (Class<?>)pType.getRawType(); // getRawType should always be Class
			arguments = pType.getActualTypeArguments();
			// TODO type parameters from pType pType.getOwnerType()
		} else if (typeAndParams instanceof Class) {
			clazz = (Class<?>)typeAndParams;
			if (clazz.getTypeParameters().length == 0) {
				return toMapType;
			} else {
				return getRawType(toMapType);
			}
		} else {
			throw new AssertionError("Unexpected type " + typeAndParams.getClass());
		}
		return mapTypeParameters(toMapType, clazz.getTypeParameters(), arguments);
	}
	
	private static Type mapTypeParameters(Type toMapType, TypeVariable<?>[] typeVariables, Type[] arguments) {
		return new VarMap(typeVariables, arguments).map(toMapType);
	}
	
	/**
	 * With type a supertype of searchClass, returns the exact supertype of the given class, including type parameters.
	 * For example, with <tt>class StringList extends List&lt;String&gt;</tt>, <tt>getExactSuperType(StringList.class, Collection.class)</tt>
	 * returns a {@link ParameterizedType} representing <tt>Collection&lt;String&gt;</tt>.
	 */
	public static Type getExactSuperType(Type type, Class<?> searchClass) {
		if (type instanceof ParameterizedType || type instanceof Class) {
			Class<?> clazz;
			if (type instanceof ParameterizedType) {
				clazz = (Class<?>)((ParameterizedType)type).getRawType();
			} else {
				clazz = (Class<?>)type;
			}
			
			if (searchClass == clazz) {
				return type;
			}
			
			if (! searchClass.isAssignableFrom(clazz))
				return null;
		}
		
		for (Type superType: getExactDirectSuperTypes(type)) {
			Type result = getExactSuperType(superType, searchClass);
			if (result != null)
				return result;
		}
		
		return null;
	}
	
	/**
	 * Returns the class given, or the class of the ParameterizedType given
	 * @param classOrParamterizedType Instance of either Class or ParameterizedType
	 */
	private static Class<?> getClass(Type classOrParameterizedType) {
		if (classOrParameterizedType instanceof Class)
			return (Class<?>)classOrParameterizedType;
		else
			return (Class<?>)((ParameterizedType) classOrParameterizedType).getRawType();
	}
	
	/**
	 * Checks if the capture of subType is a subtype of superType
	 */
	public static boolean isSuperType(Type superType, Type subType) {
		if (superType instanceof ParameterizedType || superType instanceof Class) {
			Class<?> superClass = getClass(superType);
			Type mappedSubType = getExactSuperType(capture(subType), superClass);
			if (mappedSubType == null) {
				return false;
			} else if (superType instanceof Class<?>) {
				return true;
			} else if (mappedSubType instanceof Class<?>) {
				// TODO treat supertype by being raw type differently 
				return true; // class has no parameters, or it's a raw type
			} else {
				assert mappedSubType instanceof ParameterizedType;
				ParameterizedType pMappedSubType = (ParameterizedType) mappedSubType;
				assert pMappedSubType.getRawType() == superClass;
				
				Type[] superTypeArgs = ((ParameterizedType)superType).getActualTypeArguments();
				Type[] subTypeArgs = pMappedSubType.getActualTypeArguments();
				assert superTypeArgs.length == subTypeArgs.length;
				for (int i = 0; i < superTypeArgs.length; i++) {
					if (! contains(superTypeArgs[i], subTypeArgs[i])) {
						return false;
					}
				}
				// TODO check outer class too
				return true;
			}
		} else if (superType instanceof CaptureType) {
			for (Type lowerBound : ((CaptureType) superType).getLowerBounds()) {
				if (isSuperType(lowerBound, subType)) {
					return true;
				}
			}
			return false;
		} else {
			throw new RuntimeException("not implemented: " + superType.getClass());
		}
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
	private static Type[] getExactDirectSuperTypes(Type type) {
		if (type instanceof ParameterizedType || type instanceof Class) {
			Class<?> clazz;
			if (type instanceof ParameterizedType) {
				clazz = (Class<?>)((ParameterizedType)type).getRawType();
			} else {
				clazz = (Class<?>)type;
			}
			
			Type[] superInterfaces = clazz.getGenericInterfaces();
			Type superClass = clazz.getGenericSuperclass();
			Type[] result;
			int resultIndex;
			if (superClass == null) {
				result = new Type[superInterfaces.length];
				resultIndex = 0;
			} else {
				result = new Type[superInterfaces.length + 1];
				resultIndex = 1;
				result[0] = mapTypeParameters(superClass, type);
			}
			for (Type superInterface : superInterfaces) {
				result[resultIndex++] = mapTypeParameters(superInterface, type);
			}
			
			return result;
		} else if (type instanceof TypeVariable) {
			TypeVariable<?> tv = (TypeVariable<?>) type;
			return tv.getBounds();
		} else if (type instanceof WildcardType) {
			// This should be a rare case: normally this wildcard is already captured.
			// But it does happen if the upper bound of a type variable contains a wildcard
			// TODO shouldn't upper bound of type variable have been captured too? (making this case impossible?)
			return ((WildcardType) type).getUpperBounds();
		} else if (type instanceof CaptureType) {
			return ((CaptureType)type).getUpperBounds();
		} else {
			throw new RuntimeException("not implemented type: " + type);
		}
	}

	/**
	 * Returns the exact return type of the given method in the given type.
	 * This may be different from <tt>m.getGenericReturnType()</tt> when the method was declared in a superclass,
	 * of <tt>type</tt> is a raw type.
	 */
	public static Type getExactReturnType(Method m, Type type) {
		Type returnType = m.getGenericReturnType();
		Type exactDeclaringType = getExactSuperType(capture(type), m.getDeclaringClass());
		return mapTypeParameters(returnType, exactDeclaringType);
	}
	
	/**
	 * Returns the exact type of the given field in the given type.
	 * This may be different from <tt>f.getGenericType()</tt> when the field was declared in a superclass,
	 * of <tt>type</tt> is a raw type.
	 */
	public static Type getExactFieldType(Field f, Type type) {
		Type returnType = f.getGenericType();
		Type exactDeclaringType = getExactSuperType(capture(type), f.getDeclaringClass());
		return mapTypeParameters(returnType, exactDeclaringType);
	}
	
	/**
	 * Applies capture conversion to the given type.
	 */
	public static Type capture(Type type) {
		VarMap varMap = new VarMap();
		List<CaptureTypeImpl> toInit = new ArrayList<CaptureTypeImpl>();
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType)type;
			Class<?> clazz = (Class<?>)pType.getRawType();
			Type[] arguments = pType.getActualTypeArguments();
			TypeVariable<?>[] vars = clazz.getTypeParameters();
			Type[] capturedArguments = new Type[arguments.length];
			assert arguments.length == vars.length;
			for (int i = 0; i < arguments.length; i++) {
				Type argument = arguments[i];
				if (argument instanceof WildcardType) {
					CaptureTypeImpl captured = new CaptureTypeImpl((WildcardType)argument, vars[i]);
					argument = captured;
					toInit.add(captured);
				}
				capturedArguments[i] = argument;
				varMap.add(vars[i], argument);
			}
			for (CaptureTypeImpl captured : toInit) {
				captured.init(varMap);
			}
//			if (pType.getOwnerType() != null) {
				// TODO capture owner type
//				throw new RuntimeException("no implemented: owner type");
//			}
			return new ParameterizedTypeImpl(clazz, capturedArguments, null);
		} else {
			return type;
		}
	}
}
