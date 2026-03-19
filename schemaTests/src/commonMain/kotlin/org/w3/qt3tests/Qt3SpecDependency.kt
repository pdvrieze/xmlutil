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
import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.XPathVersion.*
import io.github.pdvrieze.formats.xpath.XQueryVersion
import io.github.pdvrieze.formats.xpath.XQueryVersion.*

class Qt3SpecDependency(val supportedSpecs: List<Spec>, satisfied: Boolean = true): Qt3Dependency(satisfied) {
    override val value: String
        get() = supportedSpecs.joinToString(" ") { it.value }

    override val type: Qt3DependencyType
        get() = Qt3DependencyType.SPEC

    fun override(other: Qt3SpecDependency): Qt3SpecDependency {
        val newSatisfied: Boolean

        val baseXPath = xpathVersions()
        val baseXQuery = xqueryVersions()

        val otherXPath = other.xpathVersions()
        val otherXQuery = other.xqueryVersions()

        val newXPath: MutableSet<XPathVersion>
        val newXQuery: MutableSet<XQueryVersion>

        when {
            satisfied && other.satisfied -> {
                newXPath = baseXPath.intersect(otherXPath).toMutableSet()
                newXQuery = baseXQuery.intersect(otherXQuery).toMutableSet()
                newSatisfied = true
            }

            satisfied /*&& ! other.satisfied*/ -> {
                newXPath = baseXPath.subtract(otherXPath).toMutableSet()
                newXQuery = baseXQuery.subtract(otherXQuery).toMutableSet()
                newSatisfied = true
            }

            /*!satisfied &&*/ other.satisfied -> {
                newXPath = otherXPath.subtract(baseXPath).toMutableSet()
                newXQuery = otherXQuery.subtract(baseXQuery).toMutableSet()
                newSatisfied = true
            }

            else -> { /* !satisfied && ! other.satisfied */
                newXPath = baseXPath.union(otherXPath).toMutableSet()
                newXQuery = baseXQuery.union(otherXQuery).toMutableSet()
                newSatisfied = false
            }
        }

        val newSpecs = mutableListOf<Spec>()
        val sortedSpecs = Spec.entries.sortedByDescending { it.supported.size }

        for (set in listOf(newXPath, newXQuery)) {
            while (set.isNotEmpty()) {
                val spec = sortedSpecs.firstOrNull { set.containsAll(it.supported) }
                    ?: throw IllegalStateException("Could not find spec for $set")

                newSpecs.add(spec)
                set.removeAll(spec.supported)
            }
        }

        return Qt3SpecDependency(newSpecs, newSatisfied)
    }

    internal fun xpathVersions(): Set<XPathVersion> = supportedSpecs.asSequence()
        .flatMap { it.supported }
        .filterIsInstance<XPathVersion>()
        .toHashSet()


    internal fun xqueryVersions(): Set<XQueryVersion> = supportedSpecs.asSequence()
        .flatMap { it.supported }
        .filterIsInstance<XQueryVersion>()
        .toHashSet()

    fun supportedXPath(baseSupport: List<XPathVersion> = XPathVersion.entries): Set<XPathVersion> {
        val xpathVersions = xpathVersions()

        return when {
            satisfied -> xpathVersions

            else -> baseSupport.toHashSet()
                .apply { removeAll(xpathVersions) }
        }
    }

    fun supportedXQuery(baseSupport: List<XQueryVersion> = XQueryVersion.entries): Set<XQueryVersion> {
        val xqueryVersions = xqueryVersions()

        return when {
            satisfied -> xqueryVersions
            else -> baseSupport.toHashSet()
                .apply { removeAll(xqueryVersions) }
        }
    }

    override fun toString(): String {
        return buildString {
            append("Qt3SpecDependency(supportedSpecs=$supportedSpecs, satisfied=$satisfied)")
        }
    }


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

    companion object {
        val XPATH1: Qt3SpecDependency = Qt3SpecDependency(listOf(Spec.XP10Plus))
        val XPATH3_1: Qt3SpecDependency = Qt3SpecDependency(listOf(Spec.XP31))
    }
}
