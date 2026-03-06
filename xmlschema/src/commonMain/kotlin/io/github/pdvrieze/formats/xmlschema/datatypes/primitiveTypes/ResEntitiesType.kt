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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnonListBaseType
import io.github.pdvrieze.formats.xmlschema.datatypes.ResListDatatype
import io.github.pdvrieze.formats.xmlschema.resolved.BuiltinListDerivation
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.xml.schematypes.types.EntitiesType
import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdEntities
import io.github.pdvrieze.xml.schematypes.values.XsdEntity
import io.github.pdvrieze.xml.schematypes.values.XsdString

object ResEntitiesType : ResListDatatype<XsdEntities, XsdEntity>, EntitiesType<XsdEntities, XsdEntity> {

    override val baseType: ResListDatatype<XsdAnySimple, XsdAnySimple> get() = AnonListBaseType

    override fun validate(representation: XsdString, version: SchemaVersion) {}
    override val itemType: ResEntityType get() = ResEntityType
    override val mdlItemTypeDefinition: ResEntityType get() = itemType

    override val simpleType: Nothing? get() = null

    override val simpleDerivation: ResolvedSimpleType.Derivation
        get() = BuiltinListDerivation(ResEntityType)
}
