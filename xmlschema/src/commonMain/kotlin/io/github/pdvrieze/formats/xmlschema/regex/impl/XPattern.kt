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

@file:Suppress("DEPRECATION") // Char.toInt()
package io.github.pdvrieze.formats.xmlschema.regex.impl

import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchema.Version

/** Represents a compiled pattern used by [Regex] for matching, searching, or replacing strings. */
internal class XPattern(val pattern: String, version: Version) {

    var flags = 0
        private set

    /** A lexer instance used to get tokens from the pattern. */
    private val lexemes = XRLexer(pattern, version)

    /** List of all capturing groups in the pattern. Primarily used for handling back references. */
    val capturingGroups = mutableListOf<XRFSet>()

    /** Mapping from group name to its index */
    val groupNameToIndex = hashMapOf<String, Int>()

    /** Is true if back referenced sets replacement by second compilation pass is needed.*/
    private var needsBackRefReplacement = false

    /** A number of group quantifiers in the pattern */
    var groupQuantifierCount = 0
        private set

    /**
     * A number of consumers found in the pattern.
     * Consumer is any expression ending with an FSet except capturing groups (they are counted by [capturingGroups])
     */
    var consumersCount = 0
        private set

    /** A node to start a matching/searching process by call startNode.matches/startNode.find. */
    internal val startNode: XRAbstractSet

    /** Compiles the given pattern */
    init {
        if (flags != 0 && flags or flagsBitMask != flagsBitMask) {
            throw IllegalArgumentException("Invalid match flags value")
        }
        startNode = processExpression(-1, this.flags, null)

        if (!lexemes.isEmpty()) {
            throw XRPatternSyntaxException("Trailing characters", pattern, lexemes.curTokenIndex)
        }

        // Finalize compilation
        if (needsBackRefReplacement) {
            startNode.processSecondPass()
        }
    }

    override fun toString(): String = pattern

    /** Return true if the pattern has the specified flag */
    private fun hasFlag(flag: Int): Boolean = flags and flag == flag

    // Compilation methods. ============================================================================================
    /** A->(a|)+ */
    private fun processAlternations(last: XRAbstractSet): XRAbstractSet {
        val auxRange = XRCharClass(hasFlag(XPattern.CASE_INSENSITIVE))
        while (!lexemes.isEmpty() && lexemes.isLetter()
                && (lexemes.lookAhead == 0
                    || lexemes.lookAhead == XRLexer.CHAR_VERTICAL_BAR
                    || lexemes.lookAhead == XRLexer.CHAR_RIGHT_PARENTHESIS)) {
            auxRange.add(lexemes.next())
            if (lexemes.currentChar == XRLexer.CHAR_VERTICAL_BAR) {
                lexemes.next()
            }
        }
        val rangeSet = processRangeSet(auxRange)
        rangeSet.next = last

        return rangeSet
    }

    /** E->AE; E->S|E; E->S; A->(a|)+ E->S(|S)* */
    private fun processExpression(ch: Int, newFlags: Int, last: XRAbstractSet?): XRAbstractSet {
        val children = ArrayList<XRAbstractSet>()
        val savedFlags = flags
        var saveChangedFlags = false

        if (newFlags != flags) {
            flags = newFlags
        }

        // Create a right finalizing set.
        val fSet: XRFSet
        when (ch) {
            // Special groups: non-capturing, look ahead/behind etc.
            XRLexer.CHAR_NONCAP_GROUP -> fSet = XRNonCapFSet(consumersCount++)
            XRLexer.CHAR_POS_LOOKAHEAD,
            XRLexer.CHAR_NEG_LOOKAHEAD -> fSet = XRAheadFSet()
            XRLexer.CHAR_POS_LOOKBEHIND,
            XRLexer.CHAR_NEG_LOOKBEHIND -> fSet = XRBehindFSet(consumersCount++)
            XRLexer.CHAR_ATOMIC_GROUP -> fSet = XRAtomicFSet(consumersCount++)
            // A Capturing group.
            else -> {
                if (last == null) {
                    // Whole pattern - group #0.
                    fSet = XRFinalSet()
                    saveChangedFlags = true
                } else {
                    fSet = XRFSet(capturingGroups.size)
                }

                capturingGroups.add(fSet)

                if (ch == XRLexer.CHAR_NAMED_GROUP) {
                    val name = (lexemes.curSpecialToken as XRNamedGroup).name
                    if (groupNameToIndex.containsKey(name)) {
                        throw XRPatternSyntaxException("Named capturing group <$name> is already defined", pattern, lexemes.curTokenIndex)
                    }
                    groupNameToIndex[name] = fSet.groupIndex
                }
            }
        }

        if (last != null) {
            lexemes.next()
        }

        //Process to EOF or ')'
        do {
            val child: XRAbstractSet
            when {
                // a|...
                lexemes.isLetter() && lexemes.lookAhead == XRLexer.CHAR_VERTICAL_BAR -> child = processAlternations(fSet)
                // ..|.., e.g. in "a||||b"
                lexemes.currentChar == XRLexer.CHAR_VERTICAL_BAR -> {
                    child = XREmptySet(fSet)
                    lexemes.next()
                }
                else -> {
                    child = processSubExpression(fSet)
                    if (lexemes.currentChar == XRLexer.CHAR_VERTICAL_BAR) {
                        lexemes.next()
                    }
                }
            }
            children.add(child)
        } while (!(lexemes.isEmpty() || lexemes.currentChar == XRLexer.CHAR_RIGHT_PARENTHESIS))

        // |) or |<EOF> - add an empty node.
        if (lexemes.lookBack == XRLexer.CHAR_VERTICAL_BAR) {
            children.add(XREmptySet(fSet))
        }

        // Restore flags.
        if (flags != savedFlags && !saveChangedFlags) {
            flags = savedFlags
            lexemes.restoreFlags(flags)
        }

        when (ch) {
            XRLexer.CHAR_NONCAP_GROUP -> return XRNonCapturingJointSet(children, fSet)
            XRLexer.CHAR_POS_LOOKAHEAD -> return XRPositiveLookAheadSet(children, fSet)
            XRLexer.CHAR_NEG_LOOKAHEAD -> return XRNegativeLookAheadSet(children, fSet)
            XRLexer.CHAR_POS_LOOKBEHIND -> return XRPositiveLookBehindSet(children, fSet)
            XRLexer.CHAR_NEG_LOOKBEHIND -> return XRNegativeLookBehindSet(children, fSet)
            XRLexer.CHAR_ATOMIC_GROUP -> return XRAtomicJointSet(children, fSet)

            else -> when (children.size) {
                0 -> return XREmptySet(fSet)
                1 -> return XRSingleSet(children[0], fSet)
                else -> return XRJointSet(children, fSet)
            }
        }
    }


    /**
     * T->aaa
     */
    private fun processSequence(): XRAbstractSet {
        val substring = StringBuilder()
        while (!lexemes.isEmpty()
                && lexemes.isLetter()
                && !lexemes.isSurrogate()
                && (!lexemes.isNextSpecial && lexemes.lookAhead == 0 // End of a pattern.
                    || !lexemes.isNextSpecial && XRLexer.isLetter(lexemes.lookAhead)
                    || lexemes.lookAhead == XRLexer.CHAR_RIGHT_PARENTHESIS
                    || lexemes.lookAhead and 0x8000ffff.toInt() == XRLexer.CHAR_LEFT_PARENTHESIS
                    || lexemes.lookAhead == XRLexer.CHAR_VERTICAL_BAR
                    || lexemes.lookAhead == XRLexer.CHAR_DOLLAR)) {
            val ch = lexemes.next()

            if (Char.isSupplementaryCodePoint(ch)) {
                substring.append(Char.toChars(ch))
            } else {
                substring.append(ch.toChar())
            }
        }
        return XRSequenceSet(substring, hasFlag(CASE_INSENSITIVE))
    }

    /**
     * D->a
     */
    private fun processDecomposedChar(): XRAbstractSet {
        val codePoints = IntArray(XRLexer.MAX_DECOMPOSITION_LENGTH)
        val codePointsHangul: CharArray
        var readCodePoints = 0
        var curSymb = -1
        var curSymbIndex = -1

        if (!lexemes.isEmpty() && lexemes.isLetter()) {
            curSymb = lexemes.next()
            codePoints[readCodePoints] = curSymb
            curSymbIndex = curSymb - XRLexer.LBase
        }

        /*
         * We process decomposed Hangul syllable LV or LVT or process jamo L.
         * See http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf
         * "3.12 Conjoining Jamo Behavior"
         */
        if (curSymbIndex >= 0 && curSymbIndex < XRLexer.LCount) {
            codePointsHangul = CharArray(XRLexer.MAX_HANGUL_DECOMPOSITION_LENGTH)
            codePointsHangul[readCodePoints++] = curSymb.toChar()

            curSymb = lexemes.currentChar
            curSymbIndex = curSymb - XRLexer.VBase
            if (curSymbIndex >= 0 && curSymbIndex < XRLexer.VCount) {
                codePointsHangul[readCodePoints++] = curSymb.toChar()
                lexemes.next()
                curSymb = lexemes.currentChar
                curSymbIndex = curSymb - XRLexer.TBase
                if (curSymbIndex >= 0 && curSymbIndex < XRLexer.TCount) {
                    codePointsHangul[@Suppress("UNUSED_CHANGED_VALUE")readCodePoints++] = curSymb.toChar()
                    lexemes.next()

                    //LVT syllable
                    return XRHangulDecomposedCharSet(codePointsHangul, 3)
                } else {

                    //LV syllable
                    return XRHangulDecomposedCharSet(codePointsHangul, 2)
                }
            } else {

                //L jamo
                return XRCharSet(codePointsHangul[0], hasFlag(CASE_INSENSITIVE))
            }

        /*
         * We process single codepoint or decomposed codepoint.
         * We collect decomposed codepoint and obtain
         * one DecomposedCharSet.
         */
        } else {
            readCodePoints++

            while (readCodePoints < XRLexer.MAX_DECOMPOSITION_LENGTH
                    && !lexemes.isEmpty() && lexemes.isLetter()
                    && !XRLexer.isDecomposedCharBoundary(lexemes.currentChar)) {
                codePoints[readCodePoints++] = lexemes.next()
            }

            /*
             * We have read an ordinary symbol.
             */
            if (readCodePoints == 1 && !XRLexer.hasSingleCodepointDecomposition(codePoints[0])) {
                return processCharSet(codePoints[0])
            } else {
                return XRDecomposedCharSet(codePoints, readCodePoints)
            }
        }
    }

    /**
     * S->BS; S->QS; S->Q; B->a+
     */
    private fun processSubExpression(last: XRAbstractSet): XRAbstractSet {
        var cur: XRAbstractSet
        when {
            lexemes.isLetter() && !lexemes.isNextSpecial && XRLexer.isLetter(lexemes.lookAhead) -> {
                when {
                    hasFlag(XPattern.CANON_EQ) -> {
                        cur = processDecomposedChar()
                        if (!lexemes.isEmpty()
                            && (lexemes.currentChar != XRLexer.CHAR_RIGHT_PARENTHESIS || last is XRFinalSet)
                            && lexemes.currentChar != XRLexer.CHAR_VERTICAL_BAR
                            && !lexemes.isLetter()) {

                            cur = processQuantifier(last, cur)
                        }
                    }
                    lexemes.isHighSurrogate() || lexemes.isLowSurrogate() -> {
                        val term = processTerminal(last)
                        cur = processQuantifier(last, term)
                    }
                    else -> {
                        cur = processSequence()
                    }
                }
            }
            lexemes.currentChar == XRLexer.CHAR_RIGHT_PARENTHESIS -> {
                if (last is XRFinalSet) {
                    throw XRPatternSyntaxException("unmatched )", pattern, lexemes.curTokenIndex)
                }
                cur = XREmptySet(last)
            }
            else -> {
                val term = processTerminal(last)
                cur = processQuantifier(last, term)
            }
        }

        if (!lexemes.isEmpty()
            && (lexemes.currentChar != XRLexer.CHAR_RIGHT_PARENTHESIS || last is XRFinalSet)
            && lexemes.currentChar != XRLexer.CHAR_VERTICAL_BAR) {

            val next = processSubExpression(last)
            if (cur is XRLeafQuantifierSet
                // '*' or '{0,}' quantifier
                && cur.max == XRQuantifier.INF
                && cur.min == 0
                && !next.first(cur.innerSet)) {
                // An Optimizer node for the case where there is no intersection with the next node
                cur = XRUnifiedQuantifierSet(cur)
            }
            cur.next = next
        } else  {
            cur.next = last
        }
        return cur
    }

    private fun quantifierFromLexerToken(quant: Int): XRQuantifier {
        return when (quant) {
            XRLexer.QUANT_COMP, XRLexer.QUANT_COMP_R, XRLexer.QUANT_COMP_P -> {
                lexemes.nextSpecial() as XRQuantifier
            }
            else -> {
                lexemes.next()
                XRQuantifier.fromLexerToken(quant)
            }
        }
    }

    /**
     * Q->T(*|+|?...) also do some optimizations.
     */
    private fun processQuantifier(last: XRAbstractSet, term: XRAbstractSet): XRAbstractSet {
        val quant = lexemes.currentChar

        if (term.type == XRAbstractSet.TYPE_DOTSET && (quant == XRLexer.QUANT_STAR || quant == XRLexer.QUANT_PLUS)) {
            lexemes.next()
            return XRDotQuantifierSet(term, last, quant, XRAbstractLineTerminator.getInstance(flags), hasFlag(XPattern.DOTALL))
        }

        return when (quant) {

            XRLexer.QUANT_STAR, XRLexer.QUANT_PLUS, XRLexer.QUANT_ALT, XRLexer.QUANT_COMP -> {
                val quantifier = quantifierFromLexerToken(quant)
                when {
                    term is XRLeafSet ->
                        XRLeafQuantifierSet(quantifier, term, last, quant)
                    term.consumesFixedLength ->
                        XRFixedLengthQuantifierSet(quantifier, term, last, quant)
                    else ->
                        XRGroupQuantifierSet(quantifier, term, last, quant, groupQuantifierCount++)
                }
            }

            XRLexer.QUANT_STAR_R, XRLexer.QUANT_PLUS_R, XRLexer.QUANT_ALT_R, XRLexer.QUANT_COMP_R -> {
                val quantifier = quantifierFromLexerToken(quant)
                when {
                    term is XRLeafSet ->
                        XRReluctantLeafQuantifierSet(quantifier, term, last, quant)
                    term.consumesFixedLength ->
                        XRReluctantFixedLengthQuantifierSet(quantifier, term, last, quant)
                    else ->
                        XRReluctantGroupQuantifierSet(quantifier, term, last, quant, groupQuantifierCount++)
                }
            }

            XRLexer.QUANT_PLUS_P, XRLexer.QUANT_STAR_P, XRLexer.QUANT_ALT_P, XRLexer.QUANT_COMP_P -> {
                val quantifier = quantifierFromLexerToken(quant)
                when {
                    term is XRLeafSet ->
                        XRPossessiveLeafQuantifierSet(quantifier, term, last, quant)
                    term.consumesFixedLength ->
                        XRPossessiveFixedLengthQuantifierSet(quantifier, term, last, quant)
                    else ->
                        XRPossessiveGroupQuantifierSet(quantifier, term, last, quant, groupQuantifierCount++)
                }
            }

            else -> term
        }
    }

    /**
     * T-> letter|[range]|{char-class}|(E)
     */
    private fun processTerminal(last: XRAbstractSet): XRAbstractSet {
        val term: XRAbstractSet
        var char = lexemes.currentChar
        // Process flags: (?...)(?...)...
        while (char and 0xff00ffff.toInt() == XRLexer.CHAR_FLAGS) {
            lexemes.next()
            flags = (char shr 16) and flagsBitMask
            char = lexemes.currentChar
        }
        // The terminal is some kind of group: (E). Call processExpression for it.
        if (char and 0x8000ffff.toInt() == XRLexer.CHAR_LEFT_PARENTHESIS) {
            var newFlags = flags
            if (char and 0xff00ffff.toInt() == XRLexer.CHAR_NONCAP_GROUP) {
                newFlags = (char shr 16) and flagsBitMask
            }
            term = processExpression(char and 0xff00ffff.toInt(), newFlags, last) // Remove flags from the token.
            if (lexemes.currentChar != XRLexer.CHAR_RIGHT_PARENTHESIS) {
                throw XRPatternSyntaxException("unmatched (", pattern, lexemes.curTokenIndex)
            }
            lexemes.next()
        } else {
            // Other terminals.
            when (char) {
                XRLexer.CHAR_LEFT_SQUARE_BRACKET -> { // Range: [...]
                    lexemes.next()
                    var negative = false
                    if (lexemes.currentChar == XRLexer.CHAR_CARET) {
                        negative = true
                        lexemes.next()
                    }

                    term = processRange(negative, last)
                    if (lexemes.currentChar != XRLexer.CHAR_RIGHT_SQUARE_BRACKET) {
                        throw XRPatternSyntaxException("unmatched [", pattern, lexemes.curTokenIndex)
                    }
                    lexemes.setModeWithReread(XRLexer.Mode.PATTERN)
                    lexemes.next()
                }

                XRLexer.CHAR_DOT -> {  // Dot: .
                    lexemes.next()
                    term = XRDotSet(XRAbstractLineTerminator.getInstance(flags), hasFlag(DOTALL))
                }

                XRLexer.CHAR_CARET -> { // Beginning of the string: ^
                    lexemes.next()
                    term = XRSOLSet(XRAbstractLineTerminator.getInstance(flags), hasFlag(MULTILINE))
                    consumersCount++
                }

                XRLexer.CHAR_DOLLAR -> { // End of the string: $
                    lexemes.next()
                    term = XREOLSet(consumersCount++, XRAbstractLineTerminator.getInstance(flags), hasFlag(MULTILINE))

                }

                // A special token (\D, \w etc), 'u0000' or the end of the pattern.
                0 -> {
                    val cc: XRAbstractCharClass? = lexemes.curSpecialToken as XRAbstractCharClass?
                    when {
                        cc != null -> {
                            term = processRangeSet(cc)
                            lexemes.next()
                        }
                        !lexemes.isEmpty() -> {
                            term = XRCharSet(char.toChar())
                            lexemes.next()
                        }
                        else -> term = XREmptySet(last)
                    }
                }

                else -> {
                    when {
                        // A regular character.
                        char >= 0 && !lexemes.isSpecial -> {
                            term = processCharSet(char)
                            lexemes.next()
                        }
                        char == XRLexer.CHAR_VERTICAL_BAR -> {
                            term = XREmptySet(last)
                        }
                        char == XRLexer.CHAR_RIGHT_PARENTHESIS -> {
                            if (last is XRFinalSet) {
                                throw XRPatternSyntaxException("unmatched )", pattern, lexemes.curTokenIndex)
                            }
                            term = XREmptySet(last)
                        }
                        else -> {
                            val current = if (lexemes.isSpecial) lexemes.curSpecialToken.toString() else char.toString()
                            throw XRPatternSyntaxException("Dangling meta construction: $current", pattern, lexemes.curTokenIndex)
                        }
                    }
                }
            }
        }
        return term
    }

    /** Creates a back reference to the group with specified [groupIndex], or throws if the group doesn't exist yet. */
    private fun createBackReference(groupIndex: Int): XRBackReferenceSet {
        if (groupIndex >= 0 && groupIndex < capturingGroups.size) {
            capturingGroups[groupIndex].isBackReferenced = true
            needsBackRefReplacement = true // And process back references in the second pass.
            return XRBackReferenceSet(groupIndex, consumersCount++, hasFlag(CASE_INSENSITIVE))
        } else {
            throw XRPatternSyntaxException("No such group yet exists at this point in the pattern", pattern, lexemes.curTokenIndex)
        }
    }

    /**
     * Process [...] ranges
     */
    private fun processRange(negative: Boolean, last: XRAbstractSet): XRAbstractSet {
        val res = processRangeExpression(negative)
        val rangeSet = processRangeSet(res)
        rangeSet.next = last

        return rangeSet
    }

    private fun processRangeExpression(alt: Boolean): XRCharClass {
        var result = XRCharClass(hasFlag(XPattern.CASE_INSENSITIVE), alt)
        var buffer = -1
        var subtraction = false
        var firstInClass = true

        var notClosed = lexemes.currentChar != XRLexer.CHAR_RIGHT_SQUARE_BRACKET
        while (!lexemes.isEmpty() && (notClosed || firstInClass)) {
            when (lexemes.currentChar) {

                XRLexer.CHAR_RIGHT_SQUARE_BRACKET -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                    }
                    buffer = ']'.toInt()
                    lexemes.next()
                }

                XRLexer.CHAR_LEFT_SQUARE_BRACKET -> {
                    if (!subtraction) {
                        throw XRPatternSyntaxException(
                            "Schema patters don't allow nested groups that are not subtractions",
                            pattern,
                            lexemes.curTokenIndex
                        )
                    }
                    if (buffer >= 0) {
                        result.add(buffer)
                        buffer = -1
                    }
                    lexemes.next()
                    var negative = false
                    if (lexemes.currentChar == XRLexer.CHAR_CARET) {
                        lexemes.next()
                        negative = true
                    }
                    if (subtraction) {
                        result.intersection(processRangeExpression(!negative)) // subtraction is intersection with inverse
                    } else {
                        result.union(processRangeExpression(negative))
                    }

                    lexemes.next()
                }

                XRLexer.CHAR_HYPHEN -> {
                    if (lexemes.lookAhead == XRLexer.CHAR_LEFT_SQUARE_BRACKET) {
                        lexemes.next()
                        subtraction = true
                        buffer = -1
                    } else if (firstInClass
                        || lexemes.lookAhead == XRLexer.CHAR_RIGHT_SQUARE_BRACKET
                        || (buffer < 0 && lexemes.version != Version.V1_0)) {
                        // Note that mid-range hyphens are only supported in 1.1

                        // Treat the hypen as a normal character.
                        if (buffer >= 0) {
                            result.add(buffer)
                        }
                        buffer = '-'.toInt()
                        lexemes.next()
                    } else {
                        // A range.
                        lexemes.next()
                        var cur = lexemes.currentChar

                        if (!lexemes.isSpecial
                            && (cur >= 0
                                || lexemes.lookAhead == XRLexer.CHAR_RIGHT_SQUARE_BRACKET
                                || lexemes.lookAhead == XRLexer.CHAR_LEFT_SQUARE_BRACKET
                                || buffer < 0)) {

                            try {
                                if (!XRLexer.isLetter(cur)) {
                                    cur = cur and 0xFFFF
                                }
                                result.add(buffer, cur)
                            } catch (e: Exception) {
                                throw XRPatternSyntaxException("Illegal character range", pattern, lexemes.curTokenIndex)
                            }

                            lexemes.next()
                            buffer = -1
                        } else {
                            throw XRPatternSyntaxException("Illegal character range", pattern, lexemes.curTokenIndex)
                        }
                    }
                }

                XRLexer.CHAR_CARET -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                    }
                    buffer = '^'.toInt()
                    lexemes.next()
                }

                0 -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                    }
                    val cs = lexemes.curSpecialToken as XRAbstractCharClass?
                    if (cs != null) {
                        result.add(cs)
                        buffer = -1
                    } else {
                        buffer = 0
                    }

                    lexemes.next()
                }

                else -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                    }
                    buffer = lexemes.next()
                }
            }

            firstInClass = false
            notClosed = lexemes.currentChar != XRLexer.CHAR_RIGHT_SQUARE_BRACKET
        }
        if (notClosed) {
            throw XRPatternSyntaxException("Missing ']'", pattern, lexemes.curTokenIndex)
        }
        if (buffer >= 0) {
            result.add(buffer)
        }
        return result
    }

    private fun processRangeSet(charClass: XRAbstractCharClass): XRAbstractSet {
        if (charClass.hasLowHighSurrogates()) {
            val lowHighSurrRangeSet = XRSurrogateRangeSet(charClass.classWithSurrogates())

            if (charClass.mayContainSupplCodepoints) {
                return XRCompositeRangeSet(XRSupplementaryRangeSet(charClass.classWithoutSurrogates(), hasFlag(CASE_INSENSITIVE)), lowHighSurrRangeSet)
            }

            return XRCompositeRangeSet(XRRangeSet(charClass.classWithoutSurrogates(), hasFlag(CASE_INSENSITIVE)), lowHighSurrRangeSet)
        }

        if (charClass.mayContainSupplCodepoints) {
            return XRSupplementaryRangeSet(charClass, hasFlag(CASE_INSENSITIVE))
        }

        return XRRangeSet(charClass, hasFlag(CASE_INSENSITIVE))
    }

    private fun processCharSet(ch: Int): XRAbstractSet {
        val isSupplCodePoint = Char.isSupplementaryCodePoint(ch)

        return when {
            isSupplCodePoint -> XRSequenceSet(Char.toChars(ch).concatToString(0, 2), hasFlag(CASE_INSENSITIVE))
            ch.toChar().isLowSurrogate() ->  XRLowSurrogateCharSet(ch.toChar())
            ch.toChar().isHighSurrogate() -> XRHighSurrogateCharSet(ch.toChar())
            else -> XRCharSet(ch.toChar(), hasFlag(CASE_INSENSITIVE))
        }
    }

    companion object {
        //TODO: Use RegexOption enum here.
        // Flags.
        /**
         * This constant specifies that a pattern matches Unix line endings ('\n')
         * only against the '.', '^', and '$' meta characters.
         */
        val UNIX_LINES = 1 shl 0

        /**
         * This constant specifies that a `Pattern` is matched
         * case-insensitively. That is, the patterns "a+" and "A+" would both match
         * the string "aAaAaA".
         */
        val CASE_INSENSITIVE = 1 shl 1

        /**
         * This constant specifies that a `Pattern` may contain whitespace or
         * comments. Otherwise comments and whitespace are taken as literal
         * characters.
         */
        val COMMENTS = 1 shl 2

        /**
         * This constant specifies that the meta characters '^' and '$' match only
         * the beginning and end end of an input line, respectively. Normally, they
         * match the beginning and the end of the complete input.
         */
        val MULTILINE = 1 shl 3

        /**
         * This constant specifies that the whole `Pattern` is to be taken
         * literally, that is, all meta characters lose their meanings.
         */
        val LITERAL = 1 shl 4

        /**
         * This constant specifies that the '.' meta character matches arbitrary
         * characters, including line endings, which is normally not the case.
         */
        val DOTALL = 1 shl 5

        /**
         * This constant specifies that a character in a `Pattern` and a
         * character in the input string only match if they are canonically
         * equivalent.
         */
        val CANON_EQ = 1 shl 6

        /** A bit mask that includes all defined match flags */
        internal val flagsBitMask = XPattern.UNIX_LINES or
                XPattern.CASE_INSENSITIVE or
                XPattern.COMMENTS or
                XPattern.MULTILINE or
                XPattern.LITERAL or
                XPattern.DOTALL or
                XPattern.CANON_EQ


        /**
         * Quotes a given string using "\Q" and "\E", so that all other meta-characters lose their special meaning.
         * If the string is used for a `Pattern` afterwards, it can only be matched literally.
         */
        fun quote(s: String): String {
            return StringBuilder()
                    .append("\\Q")
                    .append(s.replace("\\E", "\\E\\\\E\\Q"))
                    .append("\\E").toString()
        }
    }
}

internal class XRPatternSyntaxException(
    val description: String = "",
    val pattern: String = "",
    val index: Int = -1
) : IllegalArgumentException(formatMessage(description, pattern, index)) {
    companion object {
        fun formatMessage(description: String, pattern: String, index: Int): String {
            if (index < 0 || pattern == "") {
                return description
            }

            val filler = if (index >= 1) " ".repeat(index) else ""
            return """
                $description near index: $index
                $pattern
                $filler^
            """.trimIndent()
        }
    }
}

