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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSKeyRef
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName

sealed interface ResolvedKeyRef : ResolvedIdentityConstraint {
    override fun checkConstraint(checkHelper: CheckHelper)

    override val rawPart: XSKeyRef
    val refer: QName?

    val mdlReferencedKey: ResolvedReferenceableConstraint

    override val mdlIdentityConstraintCategory: ResolvedIdentityConstraint.Category get() = ResolvedIdentityConstraint.Category.KEYREF

    companion object {
        operator fun invoke(
            rawPart: XSKeyRef,
            schema: ResolvedSchemaLike,
            owner: ResolvedElement,
        ): ResolvedKeyRef = when (rawPart.name) {
            null -> ResolvedIndirectKeyRef(rawPart, schema, owner)
            else -> ResolvedDirectKeyRef(rawPart, schema, owner)
        }
    }
}

