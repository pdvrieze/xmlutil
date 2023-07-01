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

package io.github.pdvrieze.formats.xmlschema.model

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSFacet
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets

interface SimpleTypeModel : TypeModel, SimpleTypeContext {
    val mdlFacets: List<XSFacet>
    val mdlFundamentalFacets: FundamentalFacets
    val mdlVariety: Variety
    val mdlPrimitiveTypeDefinition: PrimitiveDatatype?
    val mdlItemTypeDefinition: SimpleTypeModel?
    val mdlMemberTypeDefinitions: List<SimpleTypeModel>

    interface Local : SimpleTypeModel, TypeModel.Local {
        val mdlContext: SimpleTypeContext
    }

    interface Global : SimpleTypeModel, TypeModel.Global {
        val mdlName: VNCName

    }

    enum class Variety { ATOMIC, LIST, UNION, NIL }

    interface Derivation : TypeModel.Derivation
}

interface TypeContext : SimpleTypeContext, ComplexTypeContext

interface SimpleTypeContext
