/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

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
