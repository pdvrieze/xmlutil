/*
 * Copyright (c) 2022.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_SimpleDerivation

sealed class ResolvedSimpleDerivation(
    val schema: ResolvedSchemaLike
): G_SimpleDerivation.Types {
    abstract val rawPart: XSSimpleDerivation
}

class ResolvedSimpleListDerivation(
    override val rawPart: XSSimpleList,
    schema: ResolvedSchemaLike
): ResolvedSimpleDerivation(schema), G_SimpleDerivation.List

class ResolvedSimpleUnionDerivation(
    override val rawPart: XSSimpleUnion,
    schema: ResolvedSchemaLike
): ResolvedSimpleDerivation(schema), G_SimpleDerivation.Union

class ResolvedSimpleRestrictionDerivation(
    override val rawPart: XSSimpleRestriction,
    schema: ResolvedSchemaLike
): ResolvedSimpleDerivation(schema), G_SimpleDerivation.Restriction
