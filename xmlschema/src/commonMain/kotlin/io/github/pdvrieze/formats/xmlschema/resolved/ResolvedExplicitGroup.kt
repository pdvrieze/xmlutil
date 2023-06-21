/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAll
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSChoice
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSExplicitGroup
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSequence
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*

sealed class ResolvedExplicitGroup(
    parent: ResolvedType,
    override val schema: ResolvedSchemaLike
) : ResolvedPart, ResolvedAnnotated, T_ExplicitGroup {
    abstract override val rawPart: XSExplicitGroup

    override val elements: List<ResolvedLocalElement> by lazy {
        DelegateList(rawPart.elements) { ResolvedLocalElement(parent as ResolvedComplexType, it, schema) }
    }

    override val groups: List<ResolvedGroupRef> by lazy {
        DelegateList(rawPart.groups) { ResolvedGroupRef(it, schema) }
    }

    override val anys: List<T_AnyElement>
        get() = TODO("not implemented")

    override fun check() {
        super<ResolvedAnnotated>.check()
        for (element in elements) {
            element.check()
        }
    }
}

class ResolvedAll(
    parent: ResolvedType,
    override val rawPart: XSAll,
    override val schema: ResolvedSchemaLike
) : ResolvedExplicitGroup(parent, schema), T_All {
    override val minOccurs: VNonNegativeInteger
        get() = rawPart.minOccurs ?: VNonNegativeInteger(1)

    override val maxOccurs: T_AllNNI.Value
        get() = rawPart.maxOccurs ?: T_AllNNI(1)

    init {
        require(minOccurs.toUInt() <= 1.toUInt()) { "minOccurs must be 0 or 1, but was $minOccurs"}
        require(maxOccurs.toUInt() <= 1.toUInt()) { "maxOccurs must be 0 or 1, but was $maxOccurs"}
    }
}

class ResolvedChoice(
    parent: ResolvedType,
    override val rawPart: XSChoice,
    override val schema: ResolvedSchemaLike
) : ResolvedExplicitGroup(parent, schema), T_Choice {
    override val minOccurs: VNonNegativeInteger
        get() = rawPart.minOccurs ?: VNonNegativeInteger(1)

    override val maxOccurs: T_AllNNI
        get() = rawPart.maxOccurs ?: T_AllNNI(1)

    override val choices: List<ResolvedChoice> =
        DelegateList(rawPart.choices) { ResolvedChoice(parent, it, schema) }

    override val sequences: List<ResolvedSequence> =
        DelegateList(rawPart.sequences) { ResolvedSequence(parent, it, schema) }

/*
    init {
        require(minOccurs.toUInt() <= 1.toUInt()) { "minOccurs must be 0 or 1, but was $minOccurs"}
        require(maxOccurs.toUInt() <= 1.toUInt()) { "maxOccurs must be 0 or 1, but was $maxOccurs"}
    }
*/
}

class ResolvedSequence(
    parent: ResolvedType,
    override val rawPart: XSSequence,
    override val schema: ResolvedSchemaLike
) : ResolvedExplicitGroup(parent, schema), T_Sequence {
    override val minOccurs: VNonNegativeInteger
        get() = rawPart.minOccurs ?: VNonNegativeInteger(1)

    override val maxOccurs: T_AllNNI
        get() = (rawPart.maxOccurs as? T_AllNNI.Value) ?: T_AllNNI.Value(1u)

    override val choices: List<ResolvedChoice> =
        DelegateList(rawPart.choices) { ResolvedChoice(parent, it, schema) }

    override val sequences: List<ResolvedSequence> =
        DelegateList(rawPart.sequences) { ResolvedSequence(parent, it, schema) }

/*
    init {
        require(minOccurs.toUInt() <= 1.toUInt()) { "minOccurs must be 0 or 1, but was $minOccurs"}
        require(maxOccurs.toUInt() <= 1.toUInt()) { "maxOccurs must be 0 or 1, but was $maxOccurs"}
    }
*/
}

