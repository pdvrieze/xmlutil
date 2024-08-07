/*
 * Copyright (c) 2023.
 *
 * This file is part of XmlUtil.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSField
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSIdentityConstraint
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSelector
import io.github.pdvrieze.formats.xpath.XPathExpression

sealed class ResolvedIndirectIdentityConstraint<T: ResolvedNamedIdentityConstraint>(
    rawPart: XSIdentityConstraint,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement
) : ResolvedIdentityConstraintBase(rawPart, schema, owner), ResolvedIdentityConstraint {

    protected  abstract val _ref: Lazy<T>

    val ref: T //by _ref
        get() {
            val r = _ref.value
            return r
        }

    final override val selector: XSSelector
        get() = ref.selector

    final override val fields: List<XSField>
        get() = ref.fields

    final override val mdlSelector: XPathExpression
        get() = ref.mdlSelector

    final override val mdlFields: List<XPathExpression>
        get() = ref.mdlFields
}
