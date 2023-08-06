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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSIAssertCommon
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleContentExtension
import nl.adaptivity.xmlutil.QName

class ResolvedSimpleContentExtension(
    scope: ResolvedComplexType,
    override val rawPart: XSSimpleContentExtension,
    schema: ResolvedSchemaLike,
    inheritedTypes: SingleLinkedList<ResolvedType>,
) : ResolvedSimpleContentDerivation(rawPart, schema) {
    override val model: Model by lazy { Model(rawPart, schema, inheritedTypes) }
    val asserts: List<XSIAssertCommon> get() = rawPart.asserts
    val attributes: List<IResolvedAttributeUse> =
        DelegateList(rawPart.attributes) { ResolvedLocalAttribute(scope, it, schema) }
    val attributeGroups: List<ResolvedAttributeGroupRef> =
        DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }
    val anyAttribute: XSAnyAttribute? get() = rawPart.anyAttribute

    override val id: VID? get() = rawPart.id

    val base: QName get() = rawPart.base


    class Model(
        rawPart: XSSimpleContentExtension,
        schema: ResolvedSchemaLike,
        inheritedTypes: SingleLinkedList<ResolvedType>
    ) : ResolvedSimpleContentDerivation.Model(rawPart, schema) {
        override val baseType: ResolvedType = schema.type(rawPart.base, inheritedTypes)
    }

}
