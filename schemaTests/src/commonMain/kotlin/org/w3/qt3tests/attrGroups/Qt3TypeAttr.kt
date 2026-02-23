/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.w3.qt3tests.attrGroups

import org.w3.qt3tests.Qt3DependencyType

interface Qt3TypeAttr {
    /**
     * The <code>type</code> attribute of a `dependency` element indicates what type of dependency
     * it is: the set of possible values is enumerated.
     *
     * The most common `type` is `spec`, which indicates a dependency on specific versions of
     * XPath or XQuery. In this case the corresponding `value` attribute is a space-separated list
     * whose tokens are, for example, "`XQ10`" indicating XQuery 1.0, "`XQ10+`" indicating XQuery 1.0 or later,
     * "`XQ30+`" indication XQuery 3.0 or later, or "`XP20+`" indicating XPath 2.0 or later. The tokens in the list
     * are alternatives; the test may be run if any of the dependencies is satisfied.
     *
     * Similarly, if the `type` is `xml-version`, the corresponding value is a space-separated
     * list whose tokens are "`1.0`" (XML 1.0), "`1.1`" (XML 1.1), "`1.0:5+`" (1.0, 5th edition or later), "`1.0:4-`" (1.0,
     * fourth edition or earlier).
     */
    val type: Qt3DependencyType?
}

