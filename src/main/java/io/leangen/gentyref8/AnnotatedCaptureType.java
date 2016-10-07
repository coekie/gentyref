package io.leangen.gentyref8;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;

/**
 * Created by bojan.tomic on 7/30/16.
 */
interface AnnotatedCaptureType extends AnnotatedType {

	AnnotatedType[] getAnnotatedUpperBounds();

	AnnotatedType[] getAnnotatedLowerBounds();

	AnnotatedTypeVariable getAnnotatedTypeVariable();

	AnnotatedWildcardType getAnnotatedWildcardType();

	void setAnnotatedUpperBounds(AnnotatedType[] upperBounds);
}
