package io.leangen.gentyref8;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

import static io.leangen.gentyref8.GenericTypeReflector.typeArraysEqual;

/**
 * Created by bojan.tomic on 7/24/16.
 */
public class AnnotatedWildcardTypeImpl extends AnnotatedTypeImpl implements AnnotatedWildcardType {

	private AnnotatedType[] lowerBounds;
	private AnnotatedType[] upperBounds;

	public AnnotatedWildcardTypeImpl(WildcardType type, Annotation[] annotations, AnnotatedType[] lowerBounds, AnnotatedType[] upperBounds) {
		super(type, annotations);
		this.lowerBounds = lowerBounds;
		if (upperBounds == null || upperBounds.length == 0) {
			upperBounds = new AnnotatedType[1];
			upperBounds[0] = GenericTypeReflector.annotate(Object.class);
		}
		this.upperBounds = upperBounds;
	}

	@Override
	public AnnotatedType[] getAnnotatedLowerBounds() {
		return lowerBounds;
	}

	@Override
	public AnnotatedType[] getAnnotatedUpperBounds() {
		return upperBounds;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof AnnotatedWildcardType) || !super.equals(other)) {
			return false;
		}
		return typeArraysEqual(lowerBounds, ((AnnotatedWildcardType) other).getAnnotatedLowerBounds())
				&& typeArraysEqual(upperBounds, ((AnnotatedWildcardType) other).getAnnotatedUpperBounds());
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Arrays.hashCode(lowerBounds) + Arrays.hashCode(upperBounds);
	}
}
