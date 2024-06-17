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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGlobalSimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleList
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleRestriction
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleUnion
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalSimpleTypeImpl internal constructor(
    rawPart: XSGlobalSimpleType,
    schema: ResolvedSchemaLike,
    val location: String = "",
) : ResolvedGlobalSimpleType {

    internal constructor(
        rawPart: SchemaAssociatedElement<XSGlobalSimpleType>,
        schema: ResolvedSchemaLike
    ) : this(rawPart.element, schema, rawPart.schemaLocation)

    init {
        check(rawPart.name.isNotEmpty()) { "Empty names are forbidden" }
    }

    override val mdlQName: QName = rawPart.name.toQname(schema.targetNamespace)

    override val mdlFinal: Set<VDerivationControl.Type> = rawPart.final ?: schema.finalDefault

    override val simpleDerivation: ResolvedSimpleType.Derivation = when (val raw = rawPart.simpleDerivation) {
        is XSSimpleUnion -> ResolvedUnionDerivation(raw, schema, this)
        is XSSimpleList -> ResolvedListDerivation(raw, schema, this)
        is XSSimpleRestriction -> ResolvedSimpleRestriction(raw, schema, this)
        else -> error("unsupported derivation")
    }

    override val model: Model by lazy { ModelImpl(rawPart, schema, this) }

    override fun toString(): String = "simpleType($mdlQName)"

    interface Model : ResolvedSimpleType.Model {
    }

    private class ModelImpl(
        rawPart: XSGlobalSimpleType,
        schema: ResolvedSchemaLike,
        context: ResolvedGlobalSimpleTypeImpl
    ) : ResolvedSimpleType.ModelBase(rawPart, schema, context), Model
}
