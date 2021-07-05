/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.SerializationException

public open class XmlSerialException(message: String, cause: Throwable? = null) : SerializationException(message, cause)

public class XmlParsingException(locationInfo: String?, message: String, cause: Exception? = null) :
    XmlSerialException("Invalid XML value at position: $locationInfo: $message", cause)

public class UnknownXmlFieldException(locationInfo: String?, xmlName: String, candidates: Collection<Any> = emptyList()) :
    XmlSerialException("Could not find a field for name $xmlName${if (candidates.isNotEmpty()) candidates.joinToString(prefix = "\n  candidates: ") else ""}${locationInfo?.let { " at position $it" } ?: ""}")
