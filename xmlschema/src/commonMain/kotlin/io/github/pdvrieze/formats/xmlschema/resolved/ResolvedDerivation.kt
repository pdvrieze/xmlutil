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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.T_ComplexDerivation
import nl.adaptivity.xmlutil.QName

sealed class ResolvedDerivation(scope: ResolvedComplexType, override val schema: ResolvedSchemaLike) :
    ResolvedPart {
    abstract override val rawPart: XSComplexContent.XSComplexDerivationBase

    val term: ResolvedComplexType.ResolvedDirectParticle<*>? by lazy {
        when (val t = rawPart.term) {
            is XSAll -> ResolvedAll(scope, t, schema)
            is XSChoice -> ResolvedChoice(scope, t, schema)
            is XSGroupRef -> ResolvedGroupRef(t, schema)
            is XSSequence -> ResolvedSequence(scope, t, schema)
            null -> null
        }
    }

    final val asserts: List<XSIAssertCommon> get() = rawPart.asserts
    abstract val attributes: List<ResolvedLocalAttribute>
    abstract val attributeGroups: List<ResolvedAttributeGroupRef>
    final val anyAttribute: XSAnyAttribute? get() = rawPart.anyAttribute
    final val annotation: XSAnnotation? get() = rawPart.annotation
    final val id: VID? get() = rawPart.id
    final override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs
    final val base: QName? get() = rawPart.base
    final val openContent: XSOpenContent? get() = rawPart.openContent

    val baseType: ResolvedGlobalType by lazy {
        schema.type(base ?: AnyType.qName)
    }

    open fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super<ResolvedPart>.check(checkedTypes)
        val b = base
        if (b != null) { // Recursion is allowed, but must be managed
            baseType.check(checkedTypes, inheritedTypes)
        }

        term?.check(checkedTypes)
        attributes.forEach { it.check(checkedTypes) }
        attributeGroups.forEach { it.check(checkedTypes) }

    }
}
