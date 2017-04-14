/*
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or at <a href="http://www.apache.org/licenses/LICENSE-2">apache.org</a>.
 */

package io.leangen.geantyref;

import static io.leangen.geantyref.Annotations.A1;
import static io.leangen.geantyref.Annotations.A2;

/**
 * This interface has to remain a top level class (unlike most other classes used for tests) due to
 * <a href="http://stackoverflow.com/questions/39952812/why-annotation-on-generic-type-argument-is-not-visible-for-nested-type">a bug in JDK8</a>
 * @author Bojan Tomic (kaqqao)
 */
public interface Inter<@A1 I> {
    @A2 I cool();
}
