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

package io.github.pdvrieze.formats.xmlschema.resolved.facets

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedPart
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType

interface IResolvedMaxLength : ResolvedPart {
    val value: ULong
    val fixed: Boolean?
    fun checkLength(resolvedLength: Int, repr: String)
    fun validate(type: ResolvedSimpleType, representation: String): Result<Unit>
    fun check(type: ResolvedSimpleType)
}
