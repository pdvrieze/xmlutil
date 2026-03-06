/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes

import io.github.pdvrieze.formats.xmlschema.datatypes.ResSimpleBuiltinRestriction
import io.github.pdvrieze.formats.xmlschema.resolved.BuiltinSchemaXmlschema
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedBuiltinAtomicType
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleRestrictionBase
import io.github.pdvrieze.xml.schematypes.types.PrimitiveDatatype
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic

sealed interface ResPrimitiveDatatype<out T: XsdAtomic> : ResolvedBuiltinAtomicType<T>, ResAtomicDatatype<T>, PrimitiveDatatype<T> {

    override val baseType: ResAnyAtomicType
        get() = ResAnyAtomicType

    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = ResSimpleBuiltinRestriction(baseType, schema = BuiltinSchemaXmlschema)

    override val mdlPrimitiveTypeDefinition: ResPrimitiveDatatype<T> get() = this
}
