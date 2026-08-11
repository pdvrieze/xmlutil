/*
 * Copyright (c) 2023-2026.
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

package io.github.pdvrieze.formats.xmlschema.regex

import io.github.pdvrieze.formats.xmlschema.regex.impl.*
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import nl.adaptivity.xmlutil.core.impl.multiplatform.Language

/**
 * Represents the results from a single capturing group within a [MatchResult] of [Regex].
 *
 * @param value The value of captured group.
 * @param range The range of indices in the input string where group was captured.
 */
public data class XMatchGroup(val value: String, val range: IntRange)


/**
 * Returns a named group with the specified [name].
 *
 * @return An instance of [MatchGroup] if the group with the specified [name] was matched or `null` otherwise.
 * @throws IllegalArgumentException if there is no group with the specified [name] defined in the regex pattern.
 * @throws UnsupportedOperationException if this match group collection doesn't support getting match groups by name,
 * for example, when it's not supported by the current platform.
 */
@SinceKotlin("1.7")
public operator fun MatchGroupCollection.get(name: String): MatchGroup? {
    val namedGroups = this as? MatchNamedGroupCollection
        ?: throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")

    return namedGroups[name]
}


/**
 * Represents a compiled regular expression.
 * Provides functions to match strings in text with a pattern, replace the found occurrences and split text around matches.
 *
 * Note that in the future, the behavior of regular expression matching and replacement functions can be altered to match JVM implementation behavior where differences exist.
 */
public class XRegex internal constructor(internal val parsedPattern: XPattern) {

    internal enum class Mode {
        FIND, MATCH
    }

    /** Creates a regular expression from the specified [pattern] string and the default options.  */
    constructor(@Language("XsdRegExp") pattern: String, version: SchemaVersion) : this(pattern, RegexVariant.of(version))

    /** Creates a regular expression from the specified [pattern] string and the default options.  */
    constructor(@Language("XsdRegExp") pattern: String, variant: RegexVariant, patternFlags: String = "") : this(XPattern(pattern, RegexContext(variant),
        XRLexer.parseFlags(patternFlags)))

    /** The pattern string of this regular expression. */
    val pattern: String
        get() = parsedPattern.pattern

    private val startNode = parsedPattern.startNode

    companion object {

        /**
         * Returns a regular expression pattern string that matches the specified [literal] string literally.
         * No characters of that string will have special meaning when searching for an occurrence of the regular expression.
         */
        fun escape(literal: String): String = XPattern.quote(literal)

        /**
         * Returns a literal replacement expression for the specified [literal] string.
         * No characters of that string will have special meaning when it is used as a replacement string in [Regex.replace] function.
         */
        fun escapeReplacement(literal: String): String {
            if (!literal.contains('\\') && !literal.contains('$'))
                return literal

            val result = StringBuilder(literal.length * 2)
            literal.forEach {
                if (it == '\\' || it == '$') {
                    result.append('\\')
                }
                result.append(it)
            }

            return result.toString()
        }
    }

    private fun doMatch(input: CharSequence, mode: Mode): XMatchResult? {
        // TODO: Harmony has a default constructor for MatchResult. Do we need it?
        // TODO: Reuse the matchResult.
        val matchResult = XRMatchResultImpl(input, this)
        matchResult.mode = mode
        val matches = startNode.matches(0, input, matchResult) >= 0
        if (!matches) {
            return null
        }
        matchResult.finalizeMatch()
        return matchResult
    }

    /** Indicates whether the regular expression matches the entire [input]. */
    infix fun matches(input: CharSequence): Boolean = doMatch(input, Mode.MATCH) != null

    /** Indicates whether the regular expression can find at least one match in the specified [input]. */
    fun containsMatchIn(input: CharSequence): Boolean = find(input) != null

    public fun matchesAt(input: CharSequence, index: Int): Boolean =
        // TODO: expand and simplify
        matchAt(input, index) != null

    /**
     * Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of the [input] char sequence.
     */
    fun find(input: CharSequence, startIndex: Int = 0): XMatchResult? {
        when (startIndex) {
            !in 0..input.length -> { // allow for index after last char (for example for empty string)
                throw IndexOutOfBoundsException("Start index is out of bounds: $startIndex, input length: ${input.length}")
            }
        }
        val matchResult = XRMatchResultImpl(input, this)
        matchResult.mode = Mode.FIND
        matchResult.startIndex = startIndex
        val foundIndex = startNode.find(startIndex, input, matchResult)
        if (foundIndex >= 0) {
            matchResult.finalizeMatch()
            return matchResult
        } else {
            return null
        }
    }

    /**
     * Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     *
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of the [input] char sequence.
     *
     */
    fun findAll(input: CharSequence, startIndex: Int = 0): Sequence<XMatchResult> {
        if (startIndex < 0 || startIndex > input.length) {
            throw IndexOutOfBoundsException("Start index is out of bounds: $startIndex, input length: ${input.length}")
        }
        return generateSequence({ find(input, startIndex) }, XMatchResult::next)
    }

    /**
     * Attempts to match the entire [input] CharSequence against the pattern.
     *
     * @return An instance of [MatchResult] if the entire input matches or `null` otherwise.
     */
    fun matchEntire(input: CharSequence): XMatchResult?= doMatch(input, Mode.MATCH)

    public fun matchAt(input: CharSequence, index: Int): XMatchResult? {
        if (index !in 0..input.length) {
            throw IndexOutOfBoundsException("index is out of bounds: $index, input length: ${input.length}")
        }
        val matchResult = XRMatchResultImpl(input, this)
        matchResult.mode = Mode.FIND
        matchResult.startIndex = index
        val matches = startNode.matches(index, input, matchResult) >= 0
        if (!matches) {
            return null
        }
        matchResult.finalizeMatch()
        return matchResult
    }

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with specified [replacement] expression.
     *
     * The replacement string may contain references to the captured groups during a match. Occurrences of `${name}` or `$index`
     * in the replacement string will be substituted with the subsequences corresponding to the captured groups with the specified name or index.
     * In case of `$index`, the first digit after '$' is always treated as a part of group reference. Subsequent digits are incorporated
     * into `index` only if they would form a valid group reference. Only the digits '0'..'9' are considered as potential components
     * of the group reference. Note that indexes of captured groups start from 1, and the group with index 0 is the whole match.
     * In case of `${name}`, the `name` can consist of latin letters 'a'..'z' and 'A'..'Z', or digits '0'..'9'. The first character must be
     * a letter.
     *
     * Backslash character '\' can be used to include the succeeding character as a literal in the replacement string, e.g, `\$` or `\\`.
     * [Regex.escapeReplacement] can be used if [replacement] have to be treated as a literal string.
     *
     * @param input the char sequence to find matches of this regular expression in
     * @param replacement the expression to replace found matches with
     * @return the result of replacing each occurrence of this regular expression in [input] with the result of evaluating the [replacement] expression
     * @throws RuntimeException if [replacement] expression is malformed, or capturing group with specified `name` or `index` does not exist
     */
    fun replace(input: CharSequence, replacement: String): String
            = replace(input) { match -> substituteGroupRefs(match, replacement) }

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with the result of
     * the given function [transform] that takes [MatchResult] and returns a string to be used as a
     * replacement for that match.
     */
    fun replace(input: CharSequence, transform: (XMatchResult) -> CharSequence): String {
        var match: XMatchResult? = find(input) ?: return input.toString()

        var lastStart = 0
        val length = input.length
        val sb = StringBuilder(length)
        do {
            val foundMatch = match!!
            sb.append(input, lastStart, foundMatch.range.start)
            sb.append(transform(foundMatch))
            lastStart = foundMatch.range.endInclusive + 1
            match = foundMatch.next()
        } while (lastStart < length && match != null)

        if (lastStart < length) {
            sb.append(input, lastStart, length)
        }

        return sb.toString()
    }

    /**
     * Replaces the first occurrence of this regular expression in the specified [input] string with specified [replacement] expression.
     *
     * The replacement string may contain references to the captured groups during a match. Occurrences of `${name}` or `$index`
     * in the replacement string will be substituted with the subsequences corresponding to the captured groups with the specified name or index.
     * In case of `$index`, the first digit after '$' is always treated as a part of group reference. Subsequent digits are incorporated
     * into `index` only if they would form a valid group reference. Only the digits '0'..'9' are considered as potential components
     * of the group reference. Note that indexes of captured groups start from 1, and the group with index 0 is the whole match.
     * In case of `${name}`, the `name` can consist of latin letters 'a'..'z' and 'A'..'Z', or digits '0'..'9'. The first character must be
     * a letter.
     *
     * Backslash character '\' can be used to include the succeeding character as a literal in the replacement string, e.g, `\$` or `\\`.
     * [Regex.escapeReplacement] can be used if [replacement] have to be treated as a literal string.
     *
     * @param input the char sequence to find a match of this regular expression in
     * @param replacement the expression to replace the found match with
     * @return the result of replacing the first occurrence of this regular expression in [input] with the result of evaluating the [replacement] expression
     * @throws RuntimeException if [replacement] expression is malformed, or capturing group with specified `name` or `index` does not exist
     */
    fun replaceFirst(input: CharSequence, replacement: String): String {
        val match = find(input) ?: return input.toString()
        val length = input.length
        val result = StringBuilder(length)
        result.append(input, 0, match.range.start)
        result.append(substituteGroupRefs(match, replacement))
        if (match.range.endInclusive + 1 < length) {
            result.append(input, match.range.endInclusive + 1, length)
        }
        return result.toString()
    }

    /**
     * Splits the [input] CharSequence to a list of strings around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     */
    fun split(input: CharSequence, limit: Int = 0): List<String> {
        require(limit >= 0) { "Limit must be non-negative, but was $limit" }

        var match: XMatchResult? = find(input)

        if (match == null || limit == 1) return listOf(input.toString())

        val result = ArrayList<String>(if (limit > 0) limit.coerceAtMost(10) else 10)
        var lastStart = 0
        val lastSplit = limit - 1 // negative if there's no limit

        do {
            result.add(input.substring(lastStart, match!!.range.start))
            lastStart = match.range.endInclusive + 1
            if (lastSplit >= 0 && result.size == lastSplit) break
            match = match.next()
        } while (match != null)

        result.add(input.substring(lastStart, input.length))

        return result
    }

    /**
     * Splits the [input] CharSequence to a sequence of strings around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     */
    public fun splitToSequence(input: CharSequence, limit: Int = 0): Sequence<String> {
        require(limit >= 0) { "Limit must be non-negative, but was $limit" }

        return sequence {
            var match = find(input)
            if (match == null || limit == 1) {
                yield(input.toString())
                return@sequence
            }

            var nextStart = 0
            var splitCount = 0

            do {
                val foundMatch = match!!
                yield(input.substring(nextStart, foundMatch.range.first))
                nextStart = foundMatch.range.endInclusive + 1
                match = foundMatch.next()
            } while (++splitCount != limit - 1 && match != null)

            yield(input.substring(nextStart, input.length))
        }
    }

    /**
     * Returns the string representation of this regular expression, namely the [pattern] of this regular expression.
     */
    override fun toString(): String = parsedPattern.toString()
}

// The same code from K/JS regex.kt
private fun substituteGroupRefs(match: XMatchResult, replacement: String): String {
    var index = 0
    val result = StringBuilder(replacement.length)

    while (index < replacement.length) {
        val char = replacement[index++]
        if (char == '\\') {
            if (index == replacement.length)
                throw IllegalArgumentException("The Char to be escaped is missing")

            result.append(replacement[index++])
        } else if (char == '$') {
            if (index == replacement.length)
                throw IllegalArgumentException("Capturing group index is missing")

            if (replacement[index] == '{') {
                val endIndex = replacement.readGroupName(++index)
                val groupName = replacement.substring(index, endIndex)

                if (groupName.isEmpty())
                    throw IllegalArgumentException("Named capturing group reference should have a non-empty name")
                if (groupName[0] in '0'..'9')
                    throw IllegalArgumentException("Named capturing group reference {$groupName} should start with a letter")

                if (endIndex == replacement.length || replacement[endIndex] != '}')
                    throw IllegalArgumentException("Named capturing group reference is missing trailing '}'")

                result.append((match.groups as? XMatchNamedGroupCollection)?.get(groupName)?.value ?: "")
                index = endIndex + 1    // skip past '}'
            } else {
                if (replacement[index] !in '0'..'9')
                    throw IllegalArgumentException("Invalid capturing group reference")

                val groups = match.groups
                val endIndex = replacement.readGroupIndex(index, groups.size)
                val groupIndex = replacement.substring(index, endIndex).toInt()

                if (groupIndex >= groups.size)
                    throw IndexOutOfBoundsException("Group with index $groupIndex does not exist")

                result.append(groups[groupIndex]?.value ?: "")
                index = endIndex
            }
        } else {
            result.append(char)
        }
    }
    return result.toString()
}

private fun String.readGroupName(startIndex: Int): Int {
    var index = startIndex
    while (index < length) {
        val char = this[index]
        if (char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9') {
            index++
        } else {
            break
        }
    }
    return index
}

private fun String.readGroupIndex(startIndex: Int, groupCount: Int): Int {
    // at least one digit after '$' is always captured
    var index = startIndex + 1
    var groupIndex = this[startIndex] - '0'

    // capture the largest valid group index
    while (index < length && this[index] in '0'..'9') {
        val newGroupIndex = (groupIndex * 10) + (this[index] - '0')
        if (newGroupIndex in 0 until groupCount) {
            groupIndex = newGroupIndex
            index++
        } else {
            break
        }
    }
    return index
}
