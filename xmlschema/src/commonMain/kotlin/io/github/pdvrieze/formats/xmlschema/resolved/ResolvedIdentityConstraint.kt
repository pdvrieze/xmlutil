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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName

sealed interface ResolvedIdentityConstraint : ResolvedAnnotated {
    val selector: XSSelector

    val mdlQName: QName?

    /**
     * At least 1 if selector is present
     */
    val fields: List<XSField>

    val owner: ResolvedElement
    val constraint: ResolvedIdentityConstraint
    val mdlIdentityConstraintCategory: Category
    val mdlSelector: XPathExpression
    val mdlFields: List<XPathExpression>

    companion object {
        operator fun invoke(
            rawPart: XSIdentityConstraint,
            schema: ResolvedSchemaLike,
            context: ResolvedElement
        ): ResolvedIdentityConstraint = when (rawPart) {
            is XSKey -> ResolvedKey(rawPart, schema, context)
            is XSUnique -> ResolvedUnique(rawPart, schema, context)
            is XSKeyRef -> ResolvedKeyRef(rawPart, schema, context)
        }

        fun Ref(
            owner: ResolvedElement,
            constraint: ResolvedIdentityConstraint
        ): ResolvedIdentityConstraint {
            return constraint
        }
    }

    enum class Category { KEY, KEYREF, UNIQUE }

    fun checkConstraint(checkHelper: CheckHelper)
}
