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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleList
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

abstract class ResolvedListDerivationBase() : ResolvedSimpleType.Derivation() {
    abstract override val model: IModel

    final override val baseType: ResolvedSimpleType get() = AnySimpleType

    val itemType: ResolvedSimpleType get() = model.itemType

    override fun checkDerivation(checkHelper: CheckHelper) {
        checkHelper.checkType(itemType)

        check(VDerivationControl.LIST !in itemType.mdlFinal) {
            "$baseType is final for list, and can not be put in a list"
        }
        check(itemType.mdlVariety != ResolvedSimpleType.Variety.LIST) {
            "The item in a list must be of variety atomic or union"
        }
    }

    interface IModel : ResolvedAnnotated.IModel {
        val itemType: ResolvedSimpleType
    }

    protected class Model : ResolvedAnnotated.Model, IModel {

        override val itemType: ResolvedSimpleType

        constructor(
            rawPart: XSSimpleList?,
            schema: ResolvedSchemaLike,
            context: ResolvedSimpleType,
        ) : super(rawPart) {
            val itemTypeName = rawPart?.itemTypeName
            itemType = when {
                itemTypeName != null -> schema.simpleType(itemTypeName)
                else -> {
                    val simpleTypeDef = requireNotNull(rawPart?.simpleType) {
                        "Item type is not specified, either by name or member"
                    }
                    ResolvedLocalSimpleType(simpleTypeDef, schema, context)
                }
            }
        }

        constructor(
            itemType: ResolvedSimpleType,
            id: VID? = null,
            annotations: List<ResolvedAnnotation> = emptyList(),
            otherAttrs: Map<QName, String> = emptyMap(),
        ) : super(annotations, id, otherAttrs) {
            this.itemType = itemType
        }

    }

}
