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

import io.github.pdvrieze.formats.xpath.XPathExpression
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSIdentityConstraint

sealed class ResolvedNamedIdentityConstraint(
    rawPart: XSIdentityConstraint,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement
) : ResolvedIdentityConstraintBase(rawPart, schema, owner), ResolvedIdentityConstraint {

    final override val mdlSelector: XPathExpression = requireNotNull(rawPart.selector).xpath

    final override val mdlFields: List<XPathExpression> =
        rawPart.fields.map { it.xpath }
}

