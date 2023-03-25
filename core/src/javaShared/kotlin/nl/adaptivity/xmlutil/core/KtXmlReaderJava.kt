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

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.core.impl.isXmlWhitespace
import java.io.InputStream
import java.io.BufferedInputStream
import java.io.InputStreamReader

public fun KtXmlReader(inputStream: InputStream, encoding: String?, relaxed: Boolean = false): KtXmlReader {
    val bufferedInput = when {
        inputStream is BufferedInputStream && inputStream.markSupported() -> inputStream
        else -> BufferedInputStream(inputStream, 4096)
    }
    bufferedInput.mark(4000)
    val srcBuf = CharArray(4000)
    var srcBufCount = 0
    var enc = encoding

    try {
        if (enc == null) {
            // read four bytes
            var chk = 0
            while (srcBufCount < 4) {
                val i: Int = bufferedInput.read()
                if (i == -1) break
                chk = chk shl 8 or i
                srcBuf[srcBufCount++] = i.toChar()
            }
            if (srcBufCount == 4) {
                when (chk) {
                    0x00000FEFF -> {
                        enc = "UTF-32BE"
                    }

                    -0x20000 -> {
                        enc = "UTF-32LE"
                    }

                    0x03c -> {
                        enc = "UTF-32BE"
                        srcBuf[0] = '<'
                    }

                    0x03c000000 -> {
                        enc = "UTF-32LE"
                        srcBuf[0] = '<'
                    }

                    0x0003c003f -> {
                        enc = "UTF-16BE"
                        srcBuf[0] = '<'
                        srcBuf[1] = '?'
                    }

                    0x03c003f00 -> {
                        enc = "UTF-16LE"
                        srcBuf[0] = '<'
                        srcBuf[1] = '?'
                    }

                    0x03c3f786d -> {
                        while (true) {
                            val i: Int = bufferedInput.read()
                            if (i == -1) break
                            srcBuf[srcBufCount++] = i.toChar()
                            if (i == '>'.code) {
                                val xmlDeclContent = String(srcBuf, 0, srcBufCount)
                                var encAttrOffset = -1
                                do {
                                    encAttrOffset = xmlDeclContent.indexOf("encoding", encAttrOffset + 1)
                                    // TODO handle xml 1.1 whitespace
                                } while (!(encAttrOffset == 0 || xmlDeclContent[encAttrOffset - 1].isXmlWhitespace()))

                                if (encAttrOffset >= 0) {
                                    var eqPos = encAttrOffset + 8
                                    if (relaxed) {
                                        while (eqPos < xmlDeclContent.length && xmlDeclContent[eqPos].isXmlWhitespace()) {
                                            eqPos++
                                        }
                                    }
                                    if (eqPos >= xmlDeclContent.length || xmlDeclContent[eqPos] != '=') {
                                        error("Missing equality character in encoding attribute")
                                    }
                                    var openQuotPos = eqPos + 1
                                    if (relaxed) {
                                        while (openQuotPos < xmlDeclContent.length && xmlDeclContent[openQuotPos].isXmlWhitespace()) {
                                            openQuotPos++
                                        }

                                    }
                                    if (openQuotPos >= xmlDeclContent.length) {
                                        error("Missing quote for encoding attribute")
                                    }
                                    val delim = xmlDeclContent[openQuotPos]
                                    if (delim == '"' || delim == '\'') {
                                        var endQuotPos = openQuotPos + 1
                                        while (endQuotPos < xmlDeclContent.length && xmlDeclContent[endQuotPos] != delim) {
                                            endQuotPos++
                                        }
                                        if (endQuotPos < xmlDeclContent.length) {
                                            enc = xmlDeclContent.substring(openQuotPos + 1, endQuotPos)
                                        } else {
                                            error("Missing closing quote in encoding")
                                        }
                                    } else {
                                        error("Missing quote (' or \") for encoding")
                                    }
                                }
                                break
                            }
                        }
                        if (chk and -0x10000 == -0x1010000) {
                            enc = "UTF-16BE"
                            srcBuf[0] = (srcBuf[2].code shl 8 or srcBuf[3].code).toChar()
                        } else if (chk and -0x10000 == -0x20000) {
                            enc = "UTF-16LE"
                            srcBuf[0] = (srcBuf[3].code shl 8 or srcBuf[2].code).toChar()
                        } else if (chk and -0x100 == -0x10444100) {
                            enc = "UTF-8"
                            srcBuf[0] = srcBuf[3]
                        }
                    }

                    else -> if (chk and -0x10000 == -0x1010000) {
                        enc = "UTF-16BE"
                        srcBuf[0] = (srcBuf[2].code shl 8 or srcBuf[3].code).toChar()
                    } else if (chk and -0x10000 == -0x20000) {
                        enc = "UTF-16LE"
                        srcBuf[0] = (srcBuf[3].code shl 8 or srcBuf[2].code).toChar()
                    } else if (chk and -0x100 == -0x10444100) {
                        enc = "UTF-8"
                        srcBuf[0] = srcBuf[3]
                    }
                }
            }
        }
        if (enc == null) enc = "UTF-8"

        bufferedInput.reset()
        return KtXmlReader(InputStreamReader(bufferedInput, enc), enc, relaxed = relaxed)
    } catch (e: Exception) {
        throw XmlException("Invalid stream or encoding: " + e.toString(), e)
    }
}
