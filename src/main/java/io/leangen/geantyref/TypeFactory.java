package io.leangen.geantyref;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * Utility class for creating instances of {@link Type}. These types can be used with the {@link
 * GenericTypeReflector} or anything else handling Java types.
 *
 * @author Wouter Coekaerts {@literal (wouter@coekaerts.be)}
 */
public class TypeFactory {
    private static final WildcardType UNBOUND_WILDCARD = new WildcardTypeImpl(new Type[]{Object.class}, new Type[]{});

    /**
     * Creates a type of class {@code clazz} with {@code arguments} as type arguments.
     * <p>
     * For example: {@code parameterizedClass(Map.class, Integer.class, String.class)}
     * returns the type {@code Map<Integer, String>}.
     *
     * @param clazz     Type class of the type to create
     * @param arguments Type arguments for the variables of {@code clazz}, or null if these are not
     *                  known.
     * @return A {@link ParameterizedType}, or simply {@code clazz} if {@code arguments} is
     * {@code null} or empty.
     */
    public static Type parameterizedClass(Class<?> clazz, Type... arguments) {
        return parameterizedInnerClass(null, clazz, arguments);
    }

    public static AnnotatedType parameterizedAnnotatedClass(Class<?> clazz, Annotation[] annotations, AnnotatedType... arguments) {
        if (arguments == null || arguments.length == 0) {
            return GenericTypeReflector.annotate(clazz, annotations);
        }
        Type[] typeArguments = Arrays.stream(arguments).map(AnnotatedType::getType).toArray(Type[]::new);
        return new AnnotatedParameterizedTypeImpl((ParameterizedType) parameterizedClass(clazz, typeArguments), annotations, arguments);
    }

    /**
     * Creates a type of {@code clazz} nested in {@code owner}.
     *
     * @param owner The owner type. This should be a subtype of {@code clazz.getDeclaringClass()},
     *              or {@code null} if no owner is known.
     * @param clazz Type class of the type to create
     * @return A {@link ParameterizedType} if the class declaring {@code clazz} is generic and its
     * type parameters are known in {@code owner} and {@code clazz} itself has no type parameters.
     * Otherwise, just returns {@code clazz}.
     */
    public static Type innerClass(Type owner, Class<?> clazz) {
        return parameterizedInnerClass(owner, clazz, (Type[]) null);
    }

    /**
     * Creates a type of {@code clazz} with {@code arguments} as type arguments, nested in
     * {@code owner}. <p> In the ideal case, this returns a {@link ParameterizedType} with all
     * generic information in it. If some type arguments are missing or if the resulting type simply
     * doesn't need any type parameters, it returns the raw {@code clazz}. Note that types with
     * some parameters specified and others not, don't exist in Java. <p> If the caller does not
     * know the exact {@code owner} type or {@code arguments}, {@code null} should be given (or
     * {@link #parameterizedClass(Class, Type...)} or {@link #innerClass(Type, Class)} could be
     * used). If they are not needed (non-generic owner and/or {@code clazz} has no type
     * parameters), they will be filled in automatically. If they are needed but are not given, the
     * raw {@code clazz} is returned. <p> The specified {@code owner} may be any subtype of
     * {@code clazz.getDeclaringClass()}. It is automatically converted into the right
     * parameterized version of the declaring class. If {@code clazz} is a {@code static} (nested)
     * class, the owner is not used.
     *
     * @param owner     The owner type. This should be a subtype of {@code clazz.getDeclaringClass()},
     *                  or {@code null} if no owner is known.
     * @param clazz     Type class of the type to create
     * @param arguments Type arguments for the variables of {@code clazz}, or null if these are not
     *                  known.
     * @return A {@link ParameterizedType} if {@code clazz} or the class declaring {@code clazz}
     * is generic, and all the needed type arguments are specified in {@code owner} and
     * {@code arguments}. Otherwise, just returns {@code clazz}.
     * @throws IllegalArgumentException if {@code arguments} (is non-null and) has an incorrect
     *                                  length, or if one of the {@code arguments} is not within
     *                                  the bounds declared on the matching type variable, or if
     *                                  owner is non-null but {@code clazz} has no declaring class
     *                                  (e.g. is a top-level class), or if owner is not a a subtype
     *                                  of {@code clazz.getDeclaringClass()}.
     * @throws NullPointerException     if {@code clazz} or one of the elements in
     *                                  {@code arguments} is null.
     */
    public static Type parameterizedInnerClass(Type owner, Class<?> clazz, Type... arguments) {
        // never allow an owner on a class that doesn't have one
        if (clazz.getDeclaringClass() == null && owner != null) {
            throw new IllegalArgumentException("Cannot specify an owner type for a top level class");
        }

        Type realOwner = transformOwner(owner, clazz);

        if (arguments == null) {
            if (clazz.getTypeParameters().length == 0) {
                // no arguments known, but no needed so just use an empty argument list.
                // (we can still end up with a generic type if the owner is generic)
                arguments = new Type[0];
            } else {
                // missing type arguments, return the raw type
                return clazz;
            }
        } else {
            if (arguments.length != clazz.getTypeParameters().length) {
                throw new IllegalArgumentException("Incorrect number of type arguments for [" + clazz + "]: " +
                        "expected " + clazz.getTypeParameters().length + ", but got " + arguments.length);
            }
        }

        // if the class and its owner simply have no parameters at all, this is not a parameterized type
        if (!GenericTypeReflector.isMissingTypeParameters(clazz)) {
            return clazz;
        }

        // if the owner type is missing type parameters and clazz is non-static, this is a raw type
        if (realOwner != null && !Modifier.isStatic(clazz.getModifiers())
                && GenericTypeReflector.isMissingTypeParameters(realOwner)) {
            return clazz;
        }

        ParameterizedType result = new ParameterizedTypeImpl(clazz, arguments, realOwner);
        checkParametersWithinBound(result);
        return result;
    }

    /**
     * Check if the type arguments of the given type are within the bounds declared on the type
     * parameters. Only the type arguments of the type itself are checked, the possible owner type
     * is assumed to be valid. <p> It does not follow the checks defined in the <a
     * href="http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.5">JLS</a>
     * because there are several problems with those (see http://stackoverflow.com/questions/7003009
     * for one). Instead, this applies some intuition and follows what Java compilers seem to do.
     *
     * @param type possibly inconsistent type to check.
     * @throws IllegalArgumentException if the type arguments are not within the bounds
     */
    private static void checkParametersWithinBound(ParameterizedType type) {
        Type[] arguments = type.getActualTypeArguments();
        TypeVariable<?>[] typeParameters = ((Class<?>) type.getRawType()).getTypeParameters();

        // a map of type arguments in the type, to fill in variables in the bounds
        VarMap varMap = new VarMap(type);

        // for every bound on every parameter
        for (int i = 0; i < arguments.length; i++) {
            for (Type bound : typeParameters[i].getBounds()) {
                // replace type variables in the bound by their value
                Type replacedBound = varMap.map(bound);


                if (arguments[i] instanceof WildcardType) {
                    WildcardType wildcardTypeParameter = (WildcardType) arguments[i];

                    // Check if a type satisfying both the bounds of the variable and of the wildcard could exist

                    // upper bounds must not be mutually exclusive
                    for (Type wildcardUpperBound : wildcardTypeParameter.getUpperBounds()) {
                        if (!couldHaveCommonSubtype(replacedBound, wildcardUpperBound)) {
                            throw new TypeArgumentNotInBoundException(arguments[i], typeParameters[i], bound);
                        }
                    }
                    // a lowerbound in the wildcard must satisfy every upperbound
                    for (Type wildcardLowerBound : wildcardTypeParameter.getLowerBounds()) {
                        if (!GenericTypeReflector.isSuperType(replacedBound, wildcardLowerBound)) {
                            throw new TypeArgumentNotInBoundException(arguments[i], typeParameters[i], bound);
                        }
                    }
                } else {
                    if (!GenericTypeReflector.isSuperType(replacedBound, arguments[i])) {
                        throw new TypeArgumentNotInBoundException(arguments[i], typeParameters[i], bound);
                    }
                }
            }
        }
    }

    /**
     * Checks if the intersection of two types is not empty.
     */
    private static boolean couldHaveCommonSubtype(Type type1, Type type2) {
        // this is an optimistically naive implementation.
        // if they are parameterized types their parameters need to be checked,...
        // so we're just a bit too lenient here

        Class<?> erased1 = GenericTypeReflector.erase(type1);
        Class<?> erased2 = GenericTypeReflector.erase(type2);
        // if they are both classes
        if (!erased1.isInterface() && !erased2.isInterface()) {
            // then one needs to be a subclass of another
            if (!erased1.isAssignableFrom(erased2) && !erased2.isAssignableFrom(erased1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Transforms the given owner type into an appropriate one when constructing a parameterized
     * type.
     */
    private static Type transformOwner(Type givenOwner, Class<?> clazz) {
        if (givenOwner == null) {
            // be lenient: if this is an inner class but no owner was specified, assume a raw owner type
            // (or if there is no owner just return null)
            return clazz.getDeclaringClass();
        } else {
            // If the specified owner is not of the declaring class' type, but instead a subtype,
            // transform it into the declaring class with the exact type parameters.
            // For example with "class StringOuter extends GenericOuter<String>", transform
            // "StringOuter.Inner" into "GenericOuter<String>.Inner", just like the Java compiler does.
            Type transformedOwner = GenericTypeReflector.getExactSuperType(GenericTypeReflector.annotate(givenOwner).getType(),
                    clazz.getDeclaringClass());

            if (transformedOwner == null) { // null means it's not a supertype
                throw new IllegalArgumentException("Given owner type [" + givenOwner + "] is not appropriate for ["
                        + clazz + "]: it should be a subtype of " + clazz.getDeclaringClass());
            }

            if (Modifier.isStatic(clazz.getModifiers())) {
                // for a static inner class, the owner shouldn't have type parameters
                return GenericTypeReflector.erase(transformedOwner);
            } else {
                return transformedOwner;
            }
        }
    }

    /**
     * Returns the wildcard type without bounds.
     * This is the '{@code ?}' in for example {@code List<?>}.
     *
     * @return The unbound wildcard type
     */
    public static WildcardType unboundWildcard() {
        return UNBOUND_WILDCARD;
    }

    /**
     * Creates a wildcard type with an upper bound. <p> For example {@code wildcardExtends(String.class)}
     * returns the type {@code ? extends String}.
     *
     * @param upperBound Upper bound of the wildcard
     * @return A wildcard type
     */
    public static WildcardType wildcardExtends(Type upperBound) {
        if (upperBound == null) {
            throw new NullPointerException();
        }
        return new WildcardTypeImpl(new Type[]{upperBound}, new Type[]{});
    }

    /**
     * Creates a wildcard type with a lower bound.
     * <p>
     * For example {@code wildcardSuper(String.class)} returns the type {@code ? super String}.
     *
     * @param lowerBound Lower bound of the wildcard
     * @return A wildcard type
     */
    public static WildcardType wildcardSuper(Type lowerBound) {
        if (lowerBound == null) {
            throw new NullPointerException();
        }
        return new WildcardTypeImpl(new Type[]{Object.class}, new Type[]{lowerBound});
    }

    /**
     * Creates a array type. <p> If {@code componentType} is not a generic type but a {@link Class}
     * object, this returns the {@link Class} representing the non-generic array type. Otherwise,
     * returns a {@link GenericArrayType}. <p> For example: <ul> <li>{@code arrayOf(String.class)}
     * returns {@code String[].class}</li> <li>{@code arrayOf(parameterizedClass(List.class,
     * String.class))} returns the {@link GenericArrayType} for {@code List<String>[]}
     * </ul>
     *
     * @param componentType The type of the components of the array.
     * @return An array type.
     */
    public static Type arrayOf(Type componentType) {
        return GenericArrayTypeImpl.createArrayType(componentType);
    }

    public static AnnotatedArrayType arrayOf(AnnotatedType componentType, Annotation[] annotations) {
        return AnnotatedArrayTypeImpl.createArrayType(componentType, annotations);
    }
}
