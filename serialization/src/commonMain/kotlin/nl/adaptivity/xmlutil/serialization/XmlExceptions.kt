/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.SerializationException
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal

public open class XmlSerialException(
    message: String,
    extLocationInfo: XmlReader.LocationInfo?,
    errContext: String,
    cause: Throwable? = null,
) : SerializationException(message, cause) {
    public var extLocationInfo: XmlReader.LocationInfo? = extLocationInfo
        private set

    public var errContext: String? = errContext
        private set

    public constructor(message: String, errContext: String, cause: Throwable? = null) : this(message, null, errContext, cause)

    public fun addErrorContext(errContext: String) {
        this.errContext = when (val c = this.errContext) {
            null, "" -> errContext
            else -> "$errContext/$c"
        }
    }

    @XmlUtilInternal
    public fun setFileLocation(fileName: String) {
        val locationInfo = extLocationInfo?.withFileName(fileName) ?: XmlException.FileNameLocationInfo(fileName)
        if (extLocationInfo !== locationInfo) extLocationInfo = locationInfo
    }

    /**
     * Message for this exception that does not include location information.
     */
    public val rawMessage: String? get() = super.message

    override val message: String?
        get() = when (extLocationInfo) {
            null -> rawMessage
            else -> "Serialization exception at [$extLocationInfo]: $rawMessage"
        }

    @XmlUtilInternal
    public fun <E: XmlSerialException> E.withFileName(fileName: String): E {
        setFileLocation(fileName)
        return this
    }

}

public class XmlParsingException(
    extLocationInfo: XmlReader.LocationInfo?,
    errContext: String,
    message: String,
    cause: Exception? = null
) : XmlSerialException(message, extLocationInfo, errContext, cause) {
    public constructor(locationInfo: String?, errContext: String, message: String, cause: Exception? = null) :
            this(locationInfo?.let(XmlReader::StringLocationInfo), errContext, message, cause)

    init {
        require("Unknown position" !in message) { "Position information should not be in the stored message" }
    }

    override val message: String
        get() {
            return "Invalid XML value at position: ${extLocationInfo ?: "<unknown>"}: $rawMessage"
        }
}

public class UnknownXmlFieldException
private constructor(message: String, extLocationInfo: XmlReader.LocationInfo?, errContext: String, cause: Throwable? = null) :
    XmlSerialException(message, extLocationInfo, errContext, cause) {

    public constructor(
        xmlName: String,
        extLocationInfo: XmlReader.LocationInfo?,
        candidates: Collection<Any> = emptyList()
    ) : this(
        "Could not find a field for name $xmlName${candidateString(candidates)}",
        extLocationInfo,
        xmlName,
        null
    )

    public constructor(
        locationInfo: String?,
        xmlName: String,
        candidates: Collection<Any> = emptyList()
    ) : this(xmlName, locationInfo?.let(XmlReader::StringLocationInfo), candidates)

}

private fun candidateString(candidates: Iterable<Any>) =
    when (candidates.iterator().hasNext()) {
        true -> candidates.joinToString(prefix = "\n  candidates: ") {
            when (it) {
                is PolyInfo -> "${it.tagName} (${it.descriptor.outputKind})"
                else -> it.toString()
            }
        }

        else -> ""
    }
