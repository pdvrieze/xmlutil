/*
 * Copyright (c) 2024-2025.
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

    public val locationInfo: XmlReader.LocationInfo?

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
            super("${reader.extLocationInfo ?: "Unknown position"} - $message", cause) {
        this.locationInfo = reader.extLocationInfo
    }

    public constructor(message: String, reader: XmlReader) :
            super("${reader.extLocationInfo ?: "Unknown position"} - $message") {
        this.locationInfo = reader.extLocationInfo
    }

}
