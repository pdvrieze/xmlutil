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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalAttribute
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedProhibitedAttribute(
    rawPart: XSLocalAttribute,
    schema: ResolvedSchemaLike
) : IResolvedAttributeUse {

    override val model: ResolvedAnnotated.IModel = ResolvedAnnotated.Model(rawPart)

    init {
        check(rawPart.use == XSAttrUse.PROHIBITED) { "Prohibited attributes must have prohibited use" }
        require(rawPart.default == null) { "Prohibited attributes may not have a default value" }
        if (schema.version != ResolvedSchema.Version.V1_0) {
            require(rawPart.fixed == null) { "Prohibited attributes may not have a fixed value in 1.1" }
        }
    }

    override val mdlInheritable: Boolean get() = false
    override val mdlRequired: Boolean get() = false
    override val mdlAttributeDeclaration: Nothing get() = throw UnsupportedOperationException("Prohibited attribute have no declaration")

    override val mdlValueConstraint: ValueConstraint? get() = null

    override val mdlQName: QName by lazy {
        rawPart.ref ?: run {

            val targetNS = rawPart.targetNamespace ?: when (schema.attributeFormDefault) {
                VFormChoice.QUALIFIED -> schema.targetNamespace

                else -> null
            }
            checkNotNull(rawPart.name) { "3.2.3(3.1) - name is required if ref is mising" }
                .toQname(targetNS)
        }
    }

    override fun toString(): String = "prohibitedAttribute($mdlQName)"

    override fun isValidRestrictionOf(baseAttr: IResolvedAttributeUse): Boolean {
        if(baseAttr.mdlRequired) return false //Prohibited attributes cannot restrict required attributes
        return true
    }

    override fun checkUse(checkHelper: CheckHelper) {
        checkNotNull(mdlQName)
    }
}
