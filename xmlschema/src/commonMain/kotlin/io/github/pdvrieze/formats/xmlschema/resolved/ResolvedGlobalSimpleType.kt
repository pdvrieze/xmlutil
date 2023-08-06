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

interface ResolvedGlobalSimpleType : ResolvedGlobalType, ResolvedSimpleType {
    override val rawPart: XSGlobalSimpleType

    override val mdlScope: VSimpleTypeScope.Global get() = VSimpleTypeScope.Global

    companion object {
        operator fun invoke(
            rawPart: XSGlobalSimpleType,
            schema: ResolvedSchemaLike
        ): ResolvedGlobalSimpleType {
            return ResolvedGlobalSimpleTypeImpl(rawPart, schema)
        }

        internal operator fun invoke(
            rawPart: SchemaAssociatedElement<XSGlobalSimpleType>,
            schema: ResolvedSchemaLike
        ): ResolvedGlobalSimpleType {
            return ResolvedGlobalSimpleTypeImpl(rawPart, schema)
        }
    }
}

