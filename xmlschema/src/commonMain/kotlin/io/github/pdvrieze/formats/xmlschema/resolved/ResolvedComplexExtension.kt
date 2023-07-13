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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSComplexContent
import io.github.pdvrieze.formats.xmlschema.types.T_ComplexExtensionType
import nl.adaptivity.xmlutil.QName

class ResolvedComplexExtension(
    override val rawPart: XSComplexContent.XSExtension,
    scope: ResolvedComplexType,
    schema: ResolvedSchemaLike
) : ResolvedDerivation(scope, schema), T_ComplexExtensionType {

    override val attributes: List<ResolvedLocalAttribute> =
        DelegateList(rawPart.attributes) { ResolvedLocalAttribute(scope, it, schema) }

    override val attributeGroups: List<ResolvedAttributeGroupRef> =
        DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }

    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super<ResolvedDerivation>.check(checkedTypes, inheritedTypes)
        require(base !in inheritedTypes.dropLastOrEmpty(1)) { "Recursive type use in complex content: $base" }
    }
}
