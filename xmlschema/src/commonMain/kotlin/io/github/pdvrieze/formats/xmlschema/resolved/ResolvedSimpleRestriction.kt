/*
 * Copyright (c) 2022.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleRestriction
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl

/**
 * Restriction is used for simple types.
 */
class ResolvedSimpleRestriction(
    override val rawPart: XSSimpleRestriction,
    schema: ResolvedSchemaLike,
    context: ResolvedSimpleType,
    inheritedTypes: SingleLinkedList<ResolvedType>
) : ResolvedSimpleRestrictionBase(rawPart, schema, inheritedTypes) {

    override val model: IModel by lazy {
        Model(rawPart, schema, context = context, inheritedTypes = inheritedTypes)
    }

    override fun checkDerivation(checkHelper: CheckHelper, inheritedTypes: SingleLinkedList<ResolvedType>) {
        super.checkDerivation(checkHelper, inheritedTypes)
        check(VDerivationControl.RESTRICTION !in baseType.mdlFinal) {
            "$baseType is final for restriction, and can not be restricted"
        }
    }
}


