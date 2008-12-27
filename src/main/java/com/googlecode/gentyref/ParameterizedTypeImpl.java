package com.googlecode.gentyref;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

class ParameterizedTypeImpl implements ParameterizedType {
	private final Class<?> rawType;
	private final Type[] actualTypeArguments;
	private final Type ownerType;
	
	public ParameterizedTypeImpl(Class<?> rawType, Type[] actualTypeArguments, Type ownerType) {
		this.rawType = rawType;
		this.actualTypeArguments = actualTypeArguments;
		this.ownerType = ownerType;
	}

	public Type getRawType() {
		return rawType;
	}
	
	public Type[] getActualTypeArguments() {
		return actualTypeArguments;
	}

	public Type getOwnerType() {
		return ownerType;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ParameterizedType))
			return false;

		ParameterizedType other = (ParameterizedType) obj;
		return rawType.equals(other.getRawType())
			&& Arrays.equals(actualTypeArguments, other.getActualTypeArguments())
			&& (ownerType == null ? other.getOwnerType() == null : ownerType.equals(other.getOwnerType()));
	}
	
	@Override
	public int hashCode() {
		int result = rawType.hashCode() ^ Arrays.hashCode(actualTypeArguments);
		if (ownerType != null)
			result ^= ownerType.hashCode();
		return result;
	}
}
