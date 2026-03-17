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

package org.w3.qt3tests

import io.github.pdvrieze.formats.xpath.SpecVersion
import io.github.pdvrieze.formats.xpath.XPathVersion.*
import io.github.pdvrieze.formats.xpath.XQueryVersion.*

class Qt3SpecDependency(val supportedSpecs: List<Spec>, satisfied: Boolean = true): Qt3Dependency(satisfied) {
    override val value: String
        get() = supportedSpecs.joinToString(" ") { it.value }

    override val type: Qt3DependencyType
        get() = Qt3DependencyType.SPEC

    enum class Spec(val value: String, val supported: Set<SpecVersion>) {
        XP10("XP10", XPath1_0),
        XP10Plus("XP10+", XPath1_0, XPath2_0, XPath3_0, XPath3_1),
        XP20("XP20", XPath2_0),
        XP20Plus("XP20+", XPath2_0, XPath3_0, XPath3_1),
        XP30("XP30", XPath3_0),
        XP30Plus("XP30+", XPath3_0, XPath3_1),
        XP31Plus("XP31+", XPath3_1),
        XP31("XP31", XPath3_1),
        XQ10("XQ10", XQuery1_0),
        XQ10Plus("XQ10+", XQuery1_0, XQuery2_0, XQuery3_0, XQuery3_1),
        XQ20("XQ20", XQuery2_0),
        XQ20Plus("XQ20+", XQuery2_0, XQuery3_0, XQuery3_1),
        XQ30("XQ30", XQuery3_0),
        XQ30Plus("XQ30+", XQuery3_0, XQuery3_1),
        XQ31Plus("XQ31+", XQuery3_1),
        XQ31("XQ31", XQuery3_1),
        ;

        constructor(value: String, vararg supported: SpecVersion): this(value, supported.toHashSet())

        companion object {
            fun from(value: String): Spec = entries.first { it.value == value }
        }
    }
}
