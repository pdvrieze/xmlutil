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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttribute
import nl.adaptivity.xmlutil.QName

sealed class ResolvedAttribute(
    rawPart: XSAttribute
) : ResolvedAnnotated, VSimpleTypeScope.Member {

    final override val otherAttrs: Map<QName, String> = rawPart.resolvedOtherAttrs()

    abstract override val model: Model

    abstract val mdlInheritable: Boolean

    abstract val mdlScope: VAttributeScope

    abstract val mdlQName: QName
    val mdlTypeDefinition: ResolvedSimpleType get() = model.mdlTypeDefinition
    val mdlValueConstraint: ValueConstraint? get() = model.mdlValueConstraint

    abstract class Model(rawPart: XSAttribute) : ResolvedAnnotated.Model(rawPart) {

        abstract val mdlTypeDefinition: ResolvedSimpleType

        val mdlValueConstraint: ValueConstraint? = ValueConstraint(rawPart)

    }

}

