/*
 * Copyright (c) 2021.
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

package org.w3.xml.xmschematestsuite

import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("testSet", TS_NAMESPACE, TS_PREFIX)
class TSTestSet(
    val contributor: String,
    val name: String,
    @SerialName("version")
    val schemaVersion : SchemaVersion? = null,
    @XmlElement(true)
    val annotation: TSAnnotation? = null,
    @XmlElement(true)
    val testGroups: List<TSTestGroup> = emptyList(),
)
