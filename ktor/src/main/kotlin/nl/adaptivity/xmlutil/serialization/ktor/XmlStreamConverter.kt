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

package nl.adaptivity.xmlutil.serialization.ktor

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.serialization.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.*
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.XML
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class XmlStreamConverter private constructor(
    private val format: XML,
    private val defaultCharset: Charset = Charsets.UTF_8
                                           ) : ContentConverter {
    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any
                                       ): WriterContent? {
        val result = try {
            serializerFromResponseType(context, format.serializersModule)?.let {
                serializeContent(it, format, value, contentType, context)
            }
        } catch (cause: SerializationException) {
            // can fail due to
            // 1. https://github.com/Kotlin/kotlinx.serialization/issues/1163)
            // 2. mismatching between compile-time and runtime types of the response.
            null
        }
        if (result != null) {
            return result
        }

        val guessedSearchSerializer = guessSerializer(value, format.serializersModule)
        return serializeContent(guessedSearchSerializer, format, value, contentType, context)
    }

    private fun serializeContent(
        serializer: KSerializer<*>,
        format: XML,
        value: Any,
        contentType: ContentType,
        context: PipelineContext<Any, ApplicationCall>
                                ): WriterContent {
        return WriterContent(contentType = contentType, body = {
            val writer = XmlStreaming.newWriter(this)
            @Suppress("UNCHECKED_CAST")
            format.encodeToWriter(writer, serializer as KSerializer<Any>, value)
        })
    }

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val charset = context.call.request.contentCharset() ?: defaultCharset

        coroutineScope {
            reader(kotlin.coroutines.coroutineContext) {
                val is = ChannelInputStream(this@coroutineScope, channel)


            }
        }


        val channel = request.value as? ByteReadChannel ?: return null

        val serializer = format.serializersModule.serializer(request.typeInfo)
        val contentPacket = channel.readRemaining()

        val channelInputStream: ChannelInputStream(channel)

//        channel.reader

        return when (format) {
            is StringFormat -> format.decodeFromString(serializer, contentPacket.readText(charset))
            is BinaryFormat -> format.decodeFromByteArray(serializer, contentPacket.readBytes())
            else            -> {
                contentPacket.discard()
                error("Unsupported format $format")
            }
        }
    }
}

internal class ChannelInputStream(private val coroutineScope: CoroutineScope, private val channel: ByteReadChannel): InputStream() {
    override fun read(): Int {
        return coroutineScope.async { channel.readByte() }.getCompleted().toInt()
    }
}