package com.googlecode.gentyref;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

class GenericArrayTypeImpl implements GenericArrayType {
	private Type componentType;

	public GenericArrayTypeImpl(Type componentType) {
		super();
		this.componentType = componentType;
	}

	public Type getGenericComponentType() {
		return componentType;
	}
}
