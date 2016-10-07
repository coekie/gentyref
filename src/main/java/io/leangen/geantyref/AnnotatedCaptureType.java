package io.leangen.geantyref;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;

/**
 * Annotated equivalent of {@link CaptureType}
 */
interface AnnotatedCaptureType extends AnnotatedType {

    AnnotatedType[] getAnnotatedUpperBounds();

    AnnotatedType[] getAnnotatedLowerBounds();

    AnnotatedTypeVariable getAnnotatedTypeVariable();

    AnnotatedWildcardType getAnnotatedWildcardType();

    void setAnnotatedUpperBounds(AnnotatedType[] upperBounds);
}
