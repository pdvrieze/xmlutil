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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttributeGroup
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalAttributeGroup(
    override val rawPart: XSAttributeGroup,
    schema: ResolvedSchemaLike,
    val location: String,
) : ResolvedAttributeGroup(rawPart, schema), ResolvedAnnotated, NamedPart, VAttributeScope.Member {

    internal constructor(rawPart: SchemaAssociatedElement<XSAttributeGroup>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)

    override val mdlQName: QName = rawPart.name.toQname(schema.targetNamespace)

    val attributes: List<IResolvedAttributeUse> = DelegateList(rawPart.attributes) {
        ResolvedLocalAttribute(this, it, schema)
    }

    val attributeGroups: List<ResolvedAttributeGroupRef>
        get() = DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }

    val anyAttribute: XSAnyAttribute?
        get() = rawPart.anyAttribute

    override val id: VID?
        get() = rawPart.id

    fun checkAttributeGroup(checkHelper: CheckHelper) {
        for (a in attributes) { a.checkUse(checkHelper) }
        for (ag in attributeGroups) { ag.checkRef(checkHelper) }
    }
}
