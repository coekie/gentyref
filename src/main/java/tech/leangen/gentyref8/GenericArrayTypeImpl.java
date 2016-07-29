package tech.leangen.gentyref8;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

class GenericArrayTypeImpl implements GenericArrayType {
	private Type componentType;
	
	static Class<?> createArrayType(Class<?> componentType) {
		// there's no (clean) other way to create a array class, than creating an instance of it
		return Array.newInstance(componentType, 0).getClass();
	}
	
	static AnnotatedType createArrayType(AnnotatedType componentType) {
		if (componentType.getType() instanceof Class) {
			return new AnnotatedArrayTypeImpl(createArrayType((Class<?>)componentType.getType()), (Class) componentType.getType());
		} else {
			return new AnnotatedArrayTypeImpl(new GenericArrayTypeImpl(componentType.getType()), new Annotation[0], componentType);
		}
	}
	
	private GenericArrayTypeImpl(Type componentType) {
		super();
		this.componentType = componentType;
	}

	public Type getGenericComponentType() {
		return componentType;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GenericArrayType))
			return false;
		return componentType.equals(((GenericArrayType)obj).getGenericComponentType());
	}
	
	@Override
	public int hashCode() {
		return componentType.hashCode() * 7;
	}
	
	@Override
	public String toString() {
		return componentType + "[]";
	}
}
