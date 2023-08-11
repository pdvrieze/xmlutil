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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.types.VNamespaceConstraint
import io.github.pdvrieze.formats.xmlschema.types.VProcessContents
import io.github.pdvrieze.formats.xmlschema.types.VQNameListBase
import nl.adaptivity.xmlutil.QName

class ResolvedAnyAttribute : ResolvedWildcardBase<VQNameListBase.AttrElem> {

    constructor(
        mdlNamespaceConstraint: VNamespaceConstraint<VQNameListBase.AttrElem>,
        mdlProcessContents: VProcessContents,
    ) : super(mdlNamespaceConstraint, mdlProcessContents)

    constructor(
        rawPart: XSAnyAttribute,
        schema: ResolvedSchemaLike,
    ) : super(
        rawPart,
        rawPart.toConstraint(schema, true), // for now attributes always have ## local in the context
        rawPart.processContents ?: VProcessContents.STRICT
    )

    fun matches(name: QName, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
        return mdlNamespaceConstraint.matches(name, context, schema)
    }

    fun restricts(base: ResolvedAnyAttribute): Boolean {
/*
        when (mdlProcessContents) {
            VProcessContents.SKIP -> if (base.mdlProcessContents != VProcessContents.SKIP) return false
            VProcessContents.LAX -> if (base.mdlProcessContents == VProcessContents.STRICT) return false
            VProcessContents.STRICT -> {} // strict always restricts others
        }
*/

        return base.mdlNamespaceConstraint.isSupersetOf(mdlNamespaceConstraint)
    }

}
