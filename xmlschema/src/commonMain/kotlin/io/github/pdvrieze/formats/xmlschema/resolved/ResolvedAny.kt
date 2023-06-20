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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAny
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*

class ResolvedAny(
    override val rawPart: XSAny,
    override val schema: ResolvedSchemaLike
) : ResolvedPart, ResolvedAnnotated, ResolvedParticle, T_Any {
    override val annotation: XSAnnotation?
        get() = super<ResolvedAnnotated>.annotation

    override val namespace: T_NamespaceList
        get() = rawPart.namespace ?: T_NamespaceList.ANY

    override val notNamespace: T_NotNamespaceList
        get() = rawPart.notNamespace ?: T_NotNamespaceList()

    override val notQName: T_QNameList
        get() = T_QNameList()

    override val processContents: T_ProcessContents
        get() = rawPart.processContents ?: T_ProcessContents.STRICT
}
