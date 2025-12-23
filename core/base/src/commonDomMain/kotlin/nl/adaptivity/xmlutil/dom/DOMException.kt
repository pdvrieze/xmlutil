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

package nl.adaptivity.xmlutil.dom

public class DOMException : Exception {
    public val error: Error
    public val code: Short get() = error.code

    public constructor() : super() { error = Error.UNKNOWN }
    public constructor(message: String?) : super(message) { error = Error.UNKNOWN }
    public constructor(message: String?, cause: Throwable?) : super(message, cause) { error = Error.UNKNOWN }
    public constructor(cause: Throwable?) : super(cause) { error = Error.UNKNOWN }
    public constructor(error: Error) : super() { this.error = error }
    public constructor(error: Error, message: String?) : super(message) { this.error = error }
    public constructor(error: Error, message: String?, cause: Throwable?) : super(message, cause) { this.error = error }
    public constructor(error: Error, cause: Throwable?) : super(cause) { this.error = error }

    public enum class Error(public val code: Short) {
        UNKNOWN(-1),
        INDEX_SIZE_ERR(1),
        DOMSTRING_SIZE_ERR(2),
        HIERARCHY_REQUEST_ERR(3),
        WRONG_DOCUMENT_ERR(4),
        INVALID_CHARACTER_ERR(5),
        NO_DATA_ALLOWED_ERR(6),
        NO_MODIFICATION_ALLOWED_ERR(7),
        NOT_FOUND_ERR(8),
        NOT_SUPPORTED_ERR(9),
        INUSE_ATTRIBUTE_ERR(10),
        INVALID_STATE_ERR(11),
        SYNTAX_ERR(12),
        INVALID_MODIFICATION_ERR(13),
        NAMESPACE_ERR(14),
        INVALID_ACCESS_ERR(15),
        VALIDATION_ERR(16),
        TYPE_MISMATCH_ERR(17),
    }

    public companion object {
        private fun ext(s: String): String = when {
            s.isNotEmpty() -> " - $s"
            else -> ""
        }

        public fun indexSizeErr(details: String = ""): DOMException =
            DOMException(Error.INDEX_SIZE_ERR, "Index range is negative or greater than allowed${ext(details)}")

        public fun domstringSizeErr(details: String = ""): DOMException =
            DOMException(Error.DOMSTRING_SIZE_ERR, "Range does not fit into a string${ext(details)}")

        public fun hierarchyRequestErr(details: String = ""): DOMException =
            DOMException(Error.HIERARCHY_REQUEST_ERR, "Node inserted where it doesn't belong${ext(details)}")

        public fun wrongDocumentErr(details: String = ""): DOMException =
            DOMException(Error.WRONG_DOCUMENT_ERR, "Node used in a different document than that created it (unsupported)${ext(details)}")

        public fun invalidCharacterErr(details: String = ""): DOMException =
            DOMException(Error.INVALID_CHARACTER_ERR, "Invalid character specified${ext(details)}")

        public fun noDataAllowedErr(details: String = ""): DOMException =
            DOMException(Error.NO_DATA_ALLOWED_ERR, "This node does not support data${ext(details)}")

        public fun noModificationAllowedErr(details: String = ""): DOMException =
            DOMException(Error.NO_MODIFICATION_ALLOWED_ERR, "Modification not allowed here${ext(details)}")

        public fun notFoundErr(details: String = ""): DOMException =
            DOMException(Error.NOT_FOUND_ERR, "Reference to non-existing node${ext(details)}")

        public fun notSupportedErr(details: String = ""): DOMException =
            DOMException(Error.NOT_SUPPORTED_ERR, "The implementation does not support the requested type or operation${ext(details)}")

        public fun inuseAttributeErr(details: String = ""): DOMException =
            DOMException(Error.INUSE_ATTRIBUTE_ERR, "The attribute is already in use elsewhere${ext(details)}")

        public fun invalidStateErr(details: String = ""): DOMException =
            DOMException(Error.INVALID_STATE_ERR, "Attempt to use an object that is not (or no longer) usable${ext(details)}")

        public fun syntaxErr(details: String = ""): DOMException =
            DOMException(Error.SYNTAX_ERR, "Invalid or illegal string is specified${ext(details)}")

        public fun invalidModificationErr(details: String = ""): DOMException =
            DOMException(Error.INVALID_MODIFICATION_ERR, "Attempt to modify the type of the underlying object${ext(details)}")

        public fun namespaceErr(details: String = ""): DOMException =
            DOMException(Error.NAMESPACE_ERR, "Attempt to create or change an object inconsistent with namespaces${ext(details)}")

        public fun invalidAccessErr(details: String = ""): DOMException =
            DOMException(Error.INVALID_ACCESS_ERR, "Parameter or operation not supported by the underlying object${ext(details)}")

        public fun validationErr(details: String = ""): DOMException =
            DOMException(Error.VALIDATION_ERR, "Call would make the node invalid with respect to partial validity${ext(details)}")

        public fun typeMismatchErr(details: String = ""): DOMException =
            DOMException(Error.TYPE_MISMATCH_ERR, "Object incompatible with the type of the parameter${ext(details)}")

    }

}
