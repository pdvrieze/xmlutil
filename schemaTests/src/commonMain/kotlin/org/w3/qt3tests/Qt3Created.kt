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

package org.w3.qt3tests

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VDate
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.qt3tests.attrGroups.Qt3ByAttr
import org.w3.qt3tests.attrGroups.Qt3OnAttr

/**
 * Provides details of the author and creation date of a test case, source document, schema, etc.
 */
@Serializable
@XmlSerialName("created", QT3TNS)
class Qt3Created(
    override val by: String? = null,
    override val on: VDate? = null,
) : Qt3ByAttr, Qt3OnAttr
