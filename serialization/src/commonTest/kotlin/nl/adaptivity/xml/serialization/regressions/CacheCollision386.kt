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


package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.test.Test
import kotlin.uuid.Uuid

@Suppress("DEPRECATION")
class CacheCollision386 {
    @Test
    fun xmlSameNameBugTest() {
        val serializer = XML {
            recommended_0_87_0()
        }

        val connectionDocument = OpenConnection(
            OpenConnectionElement(
                freeSpace = 123,
                resolution = 12345,
                systemVersion = "Android 18",
                totalSpace = 123,
                appSpace = 123,
            )
        )

        val registerDocument = RegisterClient(
            RegisterClientElement(
                systemVersion = "Android 18",
                utcOffset = 111,
                freeSpace = 123,
                resolution = 12345,
                totalSpace = 123,
                appSpace = 123,
                systemId = Uuid.random().toString(),
            )
        )

        val xmlOpenConnection = serializer.encodeToString(OpenConnection.serializer(), connectionDocument)
        println(xmlOpenConnection)

        val xmlRegistration = serializer.encodeToString(RegisterClient.serializer(), registerDocument)
        println(xmlRegistration)
    }


    @Serializable
    @SerialName("root")
    data class RegisterClient(
        val element: RegisterClientElement
    )

    @Serializable
    @XmlSerialName("element")
    data class RegisterClientElement(
        val freeSpace: Long,
        val resolution: Int,
        val systemVersion: String,
        val utcOffset: Long,
        val totalSpace: Long,
        val appSpace: Long,
        val systemId: String?,
    )


    @Serializable
    @SerialName("root")
    data class OpenConnection(
        val element: OpenConnectionElement,
    )

    @Serializable
    @SerialName("element")
    data class OpenConnectionElement(
        val freeSpace: Long,
        val resolution: Int,
        val systemVersion: String,
        val totalSpace: Long,
        val appSpace: Long,
    )
}
