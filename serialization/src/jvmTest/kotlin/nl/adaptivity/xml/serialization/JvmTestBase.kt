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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XML

actual abstract class PlatformXmlTestBase<T> actual constructor(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule,
    baseXmlFormat: XML
) : XmlTestBase<T>(
    value,
    serializer,
    serializersModule,
    baseXmlFormat
)

actual abstract class PlatformTestBase<T> actual constructor(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule,
    baseXmlFormat: XML,
    baseJsonFormat: Json
) : TestBase<T>(value, serializer, serializersModule, baseXmlFormat, baseJsonFormat)

actual abstract class PlatformTestPolymorphicBase<T> actual constructor(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule,
    baseJsonFormat: Json
) : TestPolymorphicBase<T>(value, serializer, serializersModule, baseJsonFormat)
