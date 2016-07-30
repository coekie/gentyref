package io.leangen.gentyref8;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

/**
 * Created by bojan.tomic on 7/24/16.
 */
public class AnnotatedTypeVariableImpl extends AnnotatedTypeImpl implements AnnotatedTypeVariable {

	private AnnotatedType[] annotatedBounds;

	public AnnotatedTypeVariableImpl(TypeVariable<?> type) {
		super(type, type.getAnnotations());
		this.annotatedBounds = type.getAnnotatedBounds();
	}

	public AnnotatedTypeVariableImpl(TypeVariable<?> type, Annotation[] annotations) {
		super(type, annotations);
		this.annotatedBounds = type.getAnnotatedBounds();
	}

	@Override
	public AnnotatedType[] getAnnotatedBounds() {
		return annotatedBounds;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AnnotatedTypeVariableImpl
				&& super.equals(other)
				&& Arrays.equals(((AnnotatedTypeVariableImpl) other).annotatedBounds, this.annotatedBounds);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Arrays.hashCode(annotatedBounds);
	}
}
