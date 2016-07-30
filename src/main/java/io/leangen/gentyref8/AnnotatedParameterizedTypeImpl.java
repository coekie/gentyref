package io.leangen.gentyref8;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

/**
 * Created by bojan.tomic on 7/24/16.
 */
public class AnnotatedParameterizedTypeImpl extends AnnotatedTypeImpl implements AnnotatedParameterizedType {

	private AnnotatedType[] typeArguments;

	public AnnotatedParameterizedTypeImpl(ParameterizedType rawType, Annotation[] annotations, AnnotatedType[] typeArguments) {
		super(rawType, annotations);
		this.typeArguments = typeArguments;
	}

	@Override
	public AnnotatedType[] getAnnotatedActualTypeArguments() {
		return typeArguments;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AnnotatedParameterizedTypeImpl
				&& super.equals(other)
				&& Arrays.equals(((AnnotatedParameterizedTypeImpl) other).typeArguments, this.typeArguments);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Arrays.hashCode(typeArguments);
	}
}
