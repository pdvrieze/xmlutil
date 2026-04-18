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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.IOException
import kotlin.jvm.JvmOverloads


/**
 * Simple exception for xml related things.
 */
public open class XmlException : IOException {

    public var locationInfo: XmlReader.LocationInfo?
        private set

    public var errContext: String? = null
        private set

    public val rawMessage: String? get() = super.message

    @XmlUtilInternal
    public fun setFileLocation(fileName: String) {
        val locationInfo = locationInfo?.withFileName(fileName) ?: FileNameLocationInfo(fileName)
        if (locationInfo !== locationInfo) this.locationInfo = locationInfo
    }

    public fun addErrorContext(errContext: String) {
        when (val c = this.errContext) {
            null -> errContext
            else -> "$errContext/$c"
        }
    }

    @JvmOverloads
    public constructor(locationInfo: XmlReader.LocationInfo? = null) {
        this.locationInfo = locationInfo
    }

    @JvmOverloads
    public constructor(message: String, locationInfo: XmlReader.LocationInfo? = null) : super(message) {
        this.locationInfo = locationInfo
    }

    public constructor(message: String, cause: Throwable) : super(message, cause) {
        this.locationInfo = null
    }

    public constructor(message: String, locationInfo: XmlReader.LocationInfo?, cause: Throwable) : super(message, cause) {
        this.locationInfo = locationInfo
    }

    public constructor(cause: Throwable) : super(cause) {
        this.locationInfo = null
    }

    public constructor(locationInfo: XmlReader.LocationInfo?, cause: Throwable) : super(cause) {
        this.locationInfo = locationInfo
    }

    public constructor(message: String, reader: XmlReader, cause: Throwable) :
            super(message, cause) {
        this.locationInfo = reader.extLocationInfo
    }

    public constructor(message: String, reader: XmlReader) :
            super(message) {
        this.locationInfo = reader.extLocationInfo
    }

    override val message: String?
        get() = "${locationInfo ?: "Unknown position"} - ${rawMessage}"


    @XmlUtilInternal
    public class FileNameLocationInfo(public val fileName: String): XmlReader.LocationInfo {
        override fun toString(): String {
            return "file $fileName@<unknown>"
        }

        override fun withFileName(fileName: String): XmlReader.LocationInfo = when {
            fileName == this.fileName -> this
            else -> FileNameLocationInfo(fileName)
        }
    }

}
