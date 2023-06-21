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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleContentExtension
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_Assertion
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_SimpleExtensionType
import nl.adaptivity.xmlutil.QName

class ResolvedSimpleContentExtension(
    scope: ResolvedComplexType,
    override val rawPart: XSSimpleContentExtension,
    schema: ResolvedSchemaLike
) : ResolvedSimpleContentDerivation(schema), T_SimpleExtensionType {
    override val asserts: List<T_Assertion> get() = rawPart.asserts
    override val attributes: List<ResolvedLocalAttribute> =
        DelegateList(rawPart.attributes) { ResolvedLocalAttribute(scope, it, schema) }
    override val attributeGroups: List<ResolvedAttributeGroupRef> =
        DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }
    override val anyAttribute: XSAnyAttribute? get() = rawPart.anyAttribute
    override val annotation: XSAnnotation? get() = rawPart.annotation

    override val id: VID? get() = rawPart.id

    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override val base: QName get() = rawPart.base

    override val baseType: ResolvedSimpleType by lazy {
        schema.simpleType(base)
    }

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        val b = base

        if (b !in seenTypes) {
            val inherited = baseType.qName ?.let(::SingleLinkedList) ?: SingleLinkedList.empty()
            baseType.check(seenTypes, inherited)
            // Recursion is allowed
        }
    }
}
