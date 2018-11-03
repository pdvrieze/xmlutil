/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.SerializationException

open class XmlSerialException(message: String, cause: Throwable? = null): SerializationException(message, cause)

class XmlParsingException(locationInfo: String?, message: String, cause: Exception? = null):
    XmlSerialException("Invalid XML value at position: $locationInfo: $message", cause)

class UnknownXmlFieldException(locationInfo: String?, xmlName: String, candidates: Collection<Any> = emptyList()) :
    XmlSerialException("Could not find a field for name $xmlName${if (candidates.isNotEmpty()) candidates.joinToString(prefix="\n  candidates: ") else ""}")

/*
                "Could not find a field for name $name\n  candidates " +
                        "were: ${(nameMap.keys + polyMap.keys).joinToString()}"

 */

/*
class JsonInvalidValueInStrictModeException(value: Any, valueDescription: String) : JsonException(
    "$value is not a valid $valueDescription as per JSON spec.\n" +
            "You can disable strict mode to serialize such values"
                                                                                                 ) {
    constructor(floatValue: Float) : this(floatValue, "float")
    constructor(doubleValue: Double) : this(doubleValue, "double")
}

class JsonUnknownKeyException(key: String) : JsonException(
    "Strict JSON encountered unknown key: $key\n" +
            "You can disable strict mode to skip unknown keys"
                                                          )

class JsonParsingException(position: Int, message: String) : JsonException("Invalid JSON at $position: $message")

class JsonElementTypeMismatchException(key: String, expected: String) : JsonException("Element $key is not a $expected")
*/
