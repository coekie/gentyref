/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * CaptureType represents a wildcard that has gone through capture conversion.
 * It is a custom subinterface of Type, not part of the java builtin Type hierarchy.
 *
 * @author Wouter Coekaerts {@literal (wouter@coekaerts.be)}
 * @author Bojan Tomic {@literal (veggen@gmail.com)}
 */
public interface CaptureType extends Type {
    /**
     * Returns an array of <tt>Type</tt> objects representing the upper bound(s) of this capture.
     * This includes both the upper bound of a <tt>? extends</tt> wildcard, and the bounds declared
     * with the type variable. References to other (or the same) type variables in bounds coming
     * from the type variable are replaced by their matching capture.
     *
     * @return upper bound(s) of this capture
     */
    Type[] getUpperBounds();

    /**
     * Overwrite the upper bounds of this capture. Should not normally be used.
     * When transforming a capture type into its annotated version, it  might be necessary
     * to set the upper bounds in a separate step to break an otherwise infinite recursion.
     *
     * @param upperBounds upper bound(s) of this capture
     */
    void setUpperBounds(Type[] upperBounds);

    /**
     * Returns an array of <tt>Type</tt> objects representing the lower bound(s) of this type
     * variable. This is the bound of a <tt>? super</tt> wildcard. This normally contains only one
     * or no types; it is an array for consistency with {@link WildcardType#getLowerBounds()}.
     *
     * @return lower bound(s) of this capture
     */
    Type[] getLowerBounds();

    /**
     * @return type variable associated to this capture
     */
    TypeVariable<?> getTypeVariable();

    /**
     * @return wildcard type associated to this capture
     */
    WildcardType getWildcardType();
}
