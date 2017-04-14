/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

/**
 * Indicates that invalid data has been encountered during annotation creation.
 * Similar to {@link java.lang.annotation.AnnotationFormatError} but meant to be handled by the user.
 */
public class AnnotationFormatException extends Exception {

    private static final long serialVersionUID = -2680103741623459660L;

    AnnotationFormatException() {
        super();
    }

    AnnotationFormatException(String message) {
        super(message);
    }
}
