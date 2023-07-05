/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Test
import kotlin.test.assertEquals


/**
 * Test attempting to replicate #148
 */
class SingleElementListWithCustomItem148 {


    @Serializable
    data class Inner(
        val test: String
    )

    @Serializable
    data class Wrapper(
        //@XmlChildrenName("InnerChildName")

        val dataList: List<@Serializable(with = CustomInnerSerializer::class) Inner>,
        val testAttr: Int
    )

    internal object CustomInnerSerializer : KSerializer<Inner> {
        override val descriptor = buildClassSerialDescriptor("CustomInner") {
            val o = Inner.serializer().descriptor
            for( i in 0 until o.elementsCount) {
                element(
                    elementName = o.getElementName(i),
                    descriptor = o.getElementDescriptor(i),
                    annotations = o.getElementAnnotations(i)
                )
            }
        }

        override fun deserialize(decoder: Decoder) = Inner.serializer().deserialize(decoder)

        override fun serialize(encoder: Encoder, value: Inner) = Inner.serializer().serialize(encoder, value)

    }

    internal object CustomListSerializer : KSerializer<List<Inner>> {
        private val listSerializer = ListSerializer(CustomInnerSerializer)
        override val descriptor = SerialDescriptor("CustomListSerializer", listSerializer.descriptor)
        override fun deserialize(decoder: Decoder) = listSerializer.deserialize(decoder)
        override fun serialize(encoder: Encoder, value: List<Inner>) =
            listSerializer.serialize(encoder, value)
    }

    @Test
    fun xml_list_test() {
        val data = Wrapper(testAttr = 500, dataList = listOf( Inner("test1"), Inner("test2")))

        val x = XML{}
        val res = x.encodeToString(Wrapper.serializer(), data)
        assertXmlEquals("<Wrapper testAttr=\"500\"><CustomInner test=\"test1\"/><CustomInner test=\"test2\"/></Wrapper>", res)

        val des = x.decodeFromString<Wrapper>(res)
        println(des)
        assertEquals(data.dataList, des.dataList)
    }

}
