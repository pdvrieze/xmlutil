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

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.internal.*

@ExperimentalXmlUtilApi
internal class DoctypeParser(inOutBuffer: InOutBuffer, private val isXML11: Boolean) {
    private val inputBuffer = inOutBuffer as? InjectingInOutBuffer ?: InjectingInOutBuffer(inOutBuffer)

    private val parameterEntities = mutableMapOf<String, XmlEntity>()
    val generalEntities: MutableMap<String, XmlEntity> = mutableMapOf()

    private var unresolvedExternal: Boolean = false

    private fun Char.isNameChar() = when (isXML11) {
        true -> isNameChar11(this)
        else -> isNameChar10(this)
    }

    private fun parseName(): CharSequence {
        val start = inputBuffer.offset

        if (! isNameStartChar(inputBuffer.peekChar())) {
            error("NCName must start with a letter or underscore: '${inputBuffer.peekChar()}'")
        }
        var offset = 1
        while (inputBuffer.peek(offset).let { it >= 0 && it.toChar().isNameChar() }) {
            offset += 1
        }
        return inputBuffer.readSubRange(start, start + offset).also {
            // safe as newlines are not valid name characters
            inputBuffer.skip(offset)
        }
    }

    private fun parseParameterEntityReference() {
        assertOrSkip('%')

        val name = parseName().toString()
        if (! inputBuffer.tryRead(';')) error("Expected ';' after parameter entity reference")
        val ref = requireNotNull(parameterEntities[name]) { "Reference to unknown parameter entity reference: %$name;" }
        // note that the we surround the replacement with spaces.
        inputBuffer.inject(name, " ${ref.replacementValue} ", ref.location )
        // ignore reference for now as we are not validating
    }

    private fun parseElementDecl() {
        assertOrSkip("ELEMENT")

        inputBuffer.readWS()
        // we are not validating, so ignore the element definition
        while (!inputBuffer.peek('>')) inputBuffer.markPeekedAsRead()
        assertOrSkip('>')
    }

    private fun parseEntityDecl() {
        assertOrSkip("ENTITY")

        inputBuffer.readWS()

        val isParameterEntity = when {
            inputBuffer.tryRead('%') -> {
                inputBuffer.readWS()
                true
            }

            else -> false
        }
        val name = parseName().toString()
        inputBuffer.readWS()

        val delim = inputBuffer.peekChar()
        if (delim != '\'' && delim != '"') {
            // recognize but ignore external entities
            if (!(inputBuffer.peek("SYSTEM") ||
                        inputBuffer.peek("PUBLIC") ||
                        (!isParameterEntity && inputBuffer.peek("NDATA")))
            ) {
                error("Invalid delimiter for entity name: '$delim'. Expected: ' or \"")
            }

            while (!inputBuffer.peek('>')) {
                inputBuffer.markPeekedAsRead()
            }
        } else {
            parseEntityValue(name, isParameterEntity, delim)
        }

    }

    private fun parseCharEntity() {
        assertOrSkip("&#")

        var isHex = false
        var current = 0

        when (val first = inputBuffer.readChar()) {
            'x' -> isHex = true

            else -> current = addDigitToCodePoint(first, false, 0)
        }

        while (true) {
            when (val char = inputBuffer.readChar()) {
                ';' -> break
                else -> current = addDigitToCodePoint(char, isHex, current)
            }
        }

        inputBuffer.addCodepointToCopySequence(current)

    }

    private fun parseEntityValue(
        name: String,
        isParameterEntity: Boolean,
        delim: Char
    ) {
        assertOrSkip(delim)
        var isSimple = true

        var c = inputBuffer.peekChar()
        val value = inputBuffer.createCopySequence {
            while (c != delim) {
                when (c) {
                    '%' -> {
                        inputBuffer.pauseCopySequence()
                        inputBuffer.markPeekedAsRead()
                        val name = parseName().toString()
                        if (!inputBuffer.tryRead(';')) error("Expected ';' after parameter entity reference")
                        inputBuffer.resumeCopySequence()

                        val ref = requireNotNull(parameterEntities[name]) {
                            "Reference to unknown parameter entity reference: %$name;"
                        }
                        if(ref.isSimple) {
                            inputBuffer.addToCopySequence(ref.simpleValue)
                        } else {
                            isSimple = false
                            inputBuffer.inject(name, ref.replacementValue, ref.location)
                        }
                    }

                    // character entities are resolved in DTD's but regular references not.
                    '&' if (inputBuffer.peek(1, '#')) -> {
                        inputBuffer.pauseCopySequence()
                        parseCharEntity()
                        inputBuffer.resumeCopySequence()
                    }

                    '&', '<', '>' -> {
                        isSimple = false
                        inputBuffer.markPeekedAsRead()
                    }

                    else -> inputBuffer.markPeekedAsRead()
                }

                c = inputBuffer.peekChar()
            }

        }
        assertOrSkip(delim)
        inputBuffer.skipWS()
        assertOrSkip('>')

        val entity = XmlEntity(value.toString(), isSimple, inputBuffer.locationInfo)
        when {
            isParameterEntity -> parameterEntities[name] = entity
            else -> generalEntities[name] = entity
        }
    }

    private fun parseAttributeList() {
        assertOrSkip("ATTLIST")

        inputBuffer.readWS()
        // ignore attribute lists
        while (!inputBuffer.peek('>')) inputBuffer.markPeekedAsRead()
        assertOrSkip('>')
    }

    private fun parseNotation() {
        assertOrSkip("NOTATION")
        // ignore notations
        while (!inputBuffer.peek('>')) inputBuffer.markPeekedAsRead()
        assertOrSkip('>')
    }

    fun parse() {
        inputBuffer.skipWS()
        var d = inputBuffer.peek()
        while (d >= 0 && d != ']'.code) {
            when (val c = d.toChar()) {
                '%' -> parseParameterEntityReference()

                '<' -> {
                    inputBuffer.markPeekedAsRead()
                    when (inputBuffer.peekChar()) {
                        '!' -> { // Comment
                            inputBuffer.markPeekedAsRead()
                            when {
                                inputBuffer.peek("--") -> {
                                    inputBuffer.skip(2)
                                    // skip comments in doctype declarations
                                    while (!inputBuffer.peek("-->")) {
                                        inputBuffer.markPeekedAsRead()
                                    }
                                    inputBuffer.skip(3)
                                }

                                inputBuffer.peek("ELEMENT") -> parseElementDecl()

                                inputBuffer.peek("ATTLIST") -> parseAttributeList()

                                inputBuffer.peek("ENTITY") -> parseEntityDecl()

                                inputBuffer.peek("NOTATION") -> parseNotation()

                            }
                        }

                        '?' -> parseProcessingInstruction()

                    }

                }

                // NotationDecl


                else -> error("Unexpected content in document type declaration: '$c'")
            }
            inputBuffer.skipWS()
            d = inputBuffer.peek()
        }
    }

    private fun parseProcessingInstruction() {
        assertOrSkip('?')

        while (!inputBuffer.peek("?>")) inputBuffer.markPeekedAsRead()
    }

    private fun error(message: String): Nothing {
        throw XmlException(message, inputBuffer.locationInfo)
    }

    private fun assertOrSkip(expected: String) {
        if (DEBUG) {
            assert(inputBuffer.peek(expected))
            if (!inputBuffer.peek(expected)) {
                val found = buildString {
                    expected.indices.asSequence()
                        .map { inputBuffer.peek(it) }
                        .takeWhile { it >= 0 }
                        .forEach { append(it.toChar()) }
                    if (length < expected.length) append("<EOF>")
                    append(" (at ${inputBuffer.locationInfo})")
                }
                throw XmlException("Expected '$expected' but found '$found'", inputBuffer.locationInfo)
            }
        }
        inputBuffer.skip(expected.length)
    }

    private fun assertOrSkip(expected: Char) {
        if (DEBUG) {
            if (!inputBuffer.peek(expected)) {
                throw XmlException("Expected '$expected' but found '${inputBuffer.peekChar()}'", inputBuffer.locationInfo)
            }
        }
        inputBuffer.markPeekedAsRead()
    }

}
