/*
 * Copyright (c) 2020.
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

package net.devrieze.serialization.examples.soap

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * This class represents an actual message in the gtxlink webservice. It carries its own namespace and has a predefined
 * prefix. There needs to be a prefix here as the content in the example uses the unnamed namespace.
 */
@Serializable
@XmlSerialName("Ge", "http://www.gxtlink.com/webservice/", "ns2")
data class GeResult<out T>(
    /**
     * Code is a primitive, so the name comes from here (the use site). The example data has this in the empty namespace
     * so we must specify this namespace to avoid the default (inheriting the namespace). We want to write this as
     * element so need to specify the `@XmlElement` annotation.
     */
    @XmlSerialName("code", "", "")
    @XmlElement(true)
    val code: Int,
    /**
     * The data property does not require annotation. It is not a primitive so by default the name comes from the actual
     * serialized type and is serialized as element (by default).
     */
    val data: T
                      )
