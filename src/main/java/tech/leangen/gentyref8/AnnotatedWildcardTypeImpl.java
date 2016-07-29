package tech.leangen.gentyref8;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * Created by bojan.tomic on 7/24/16.
 */
public class AnnotatedWildcardTypeImpl extends AnnotatedTypeImpl implements AnnotatedWildcardType {

	private AnnotatedType[] lowerBounds;
	private AnnotatedType[] upperBounds;

	public AnnotatedWildcardTypeImpl(WildcardType type, Annotation[] annotations, AnnotatedType[] lowerBounds, AnnotatedType[] upperBounds) {
		super(type, annotations);
		this.lowerBounds = lowerBounds;
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
		return other instanceof AnnotatedWildcardTypeImpl
				&& super.equals(other)
				&& Arrays.equals(((AnnotatedWildcardTypeImpl) other).lowerBounds, this.lowerBounds)
				&& Arrays.equals(((AnnotatedWildcardTypeImpl) other).upperBounds, this.upperBounds);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Arrays.hashCode(lowerBounds) + Arrays.hashCode(upperBounds);
	}
}
