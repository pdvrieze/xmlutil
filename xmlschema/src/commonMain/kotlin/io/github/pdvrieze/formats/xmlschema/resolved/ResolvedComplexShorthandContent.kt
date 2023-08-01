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

import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import nl.adaptivity.xmlutil.QName

class ResolvedComplexShorthandContent(
    scope: ResolvedComplexType,
    override val rawPart: XSComplexType.Shorthand,
    schema: ResolvedSchemaLike
) : ResolvedComplexTypeContent(schema) {
    val particle: ResolvedGroupParticle<ResolvedGroupLikeTerm>? by lazy {
        val r: ResolvedGroupParticle<ResolvedGroupLikeTerm>? = when (val t = rawPart.term) {
            is XSAll -> ResolvedAll(scope, t, schema)
            is XSChoice -> ResolvedChoice(scope, t, schema)
            is XSGroupRef -> ResolvedGroupRef(t, schema)
            is XSSequence -> ResolvedSequence(scope, t, schema)
            null -> null
            else -> error("Compiler issue")
        }
        r
    }

    val asserts: List<XSIAssertCommon> get() = rawPart.asserts
    val attributes: List<ResolvedLocalAttribute> =
        DelegateList(rawPart.attributes) { ResolvedLocalAttribute(scope, it, schema) }
    val attributeGroups: List<ResolvedAttributeGroupRef> =
        DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }
    val anyAttribute: XSAnyAttribute? get() = rawPart.anyAttribute
    val openContent: XSOpenContent? get() = rawPart.openContent

    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super.check(checkedTypes)
        particle?.check(checkedTypes)
        attributes.forEach { it.check(checkedTypes) }
        attributeGroups.forEach { it.check(checkedTypes) }
    }

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        particle?.mdlTerm?.collectConstraints(collector)
    }
}
