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
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedProhibitedAttribute(
    parent: VAttributeScope.Member,
    override val rawPart: XSLocalAttribute,
    override val schema: ResolvedSchemaLike
) : IResolvedAttributeUse {

    init {
        check(rawPart.use == XSAttrUse.PROHIBITED) { "Prohibited attributes must have prohibited use" }
    }

    override val mdlInheritable: Boolean get() = false
    override val mdlRequired: Boolean get() = false
    override val mdlAttributeDeclaration: Nothing get() = throw UnsupportedOperationException("Prohibited attribute have no declaration")

    override val mdlValueConstraint: ValueConstraint? get() = null

    val mdlQName: QName by lazy {
        rawPart.ref ?: run {

            val targetNS = rawPart.targetNamespace ?: when (schema.attributeFormDefault) {
                VFormChoice.QUALIFIED -> schema.targetNamespace

                else -> null
            }
            checkNotNull(rawPart.name) { "3.2.3(3.1) - name is required if ref is mising" }
                .toQname(targetNS)
        }
    }
}
