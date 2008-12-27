/**
 * 
 */
package com.googlecode.gentyref;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

class WildcardTypeImpl implements WildcardType {
	private final Type[] lowerBounds;
	private final Type[] upperBounds;
	
	public WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
		this.lowerBounds = lowerBounds;
		this.upperBounds = upperBounds;
	}

	public Type[] getLowerBounds() {
		return lowerBounds;
	}

	public Type[] getUpperBounds() {
		return upperBounds;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof WildcardType))
			return false;
		WildcardType other = (WildcardType)obj;
		return Arrays.equals(lowerBounds, other.getLowerBounds())
			&& Arrays.equals(upperBounds, other.getUpperBounds());
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(lowerBounds) ^ Arrays.hashCode(upperBounds);
	}
}