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

import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName

sealed interface IResolvedAttributeUse : ResolvedAnnotated {
    val mdlRequired: Boolean
    val mdlAttributeDeclaration: ResolvedAttributeDef
    val mdlValueConstraint: ValueConstraint?
    val mdlInheritable: Boolean
    val mdlQName: QName

    fun checkUse(checkHelper: CheckHelper)
    fun isValidRestrictionOf(baseAttr: IResolvedAttributeUse, version: SchemaVersion): Boolean {
        if (baseAttr.mdlRequired && !mdlRequired) return false

        val valueConstraint = mdlValueConstraint ?: mdlAttributeDeclaration.mdlValueConstraint
        if (valueConstraint != null) {
            baseAttr.mdlAttributeDeclaration.mdlTypeDefinition.validate(valueConstraint.value, version)
            (baseAttr.mdlValueConstraint ?: baseAttr.mdlAttributeDeclaration.mdlValueConstraint)?.let { bc ->
                if (!valueConstraint.isValidRestrictionOf(baseAttr.mdlAttributeDeclaration.mdlTypeDefinition, bc)) return false
            }
        } else {
            val baseConstraint = baseAttr.mdlValueConstraint ?: baseAttr.mdlAttributeDeclaration.mdlValueConstraint
            if (baseConstraint is ValueConstraint.Fixed) return false
        }

        return mdlAttributeDeclaration.mdlTypeDefinition
            .isValidRestrictionOf(baseAttr.mdlAttributeDeclaration.mdlTypeDefinition)
    }
}
