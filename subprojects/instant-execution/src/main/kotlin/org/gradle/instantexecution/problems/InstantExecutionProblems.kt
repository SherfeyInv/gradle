/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.instantexecution.problems

import org.gradle.instantexecution.InstantExecutionErrorException
import org.gradle.instantexecution.InstantExecutionProblemException

import kotlin.reflect.KClass


sealed class PropertyProblem {

    abstract val trace: PropertyTrace

    abstract val message: StructuredMessage

    abstract val exception: Throwable

    companion object {

        fun forWarning(trace: PropertyTrace, message: StructuredMessage, cause: Throwable? = null) =
            Warning(trace, message, InstantExecutionProblemException(
                exceptionMessageFor(trace, message), cause
            ))

        fun forError(trace: PropertyTrace, message: StructuredMessage, cause: Throwable) =
            Error(trace, message, InstantExecutionErrorException(
                exceptionMessageFor(trace, message), cause
            ))

        private
        fun exceptionMessageFor(trace: PropertyTrace, message: StructuredMessage) =
            propertyDescriptionFor(trace) + ": " + message
    }

    /**
     * A problem that does not necessarily compromise the execution of the build.
     */
    data class Warning internal constructor(
        override val trace: PropertyTrace,
        override val message: StructuredMessage,
        override val exception: InstantExecutionProblemException
    ) : PropertyProblem()

    /**
     * A problem that compromises the execution of the build.
     * Instant execution state should be discarded.
     */
    data class Error internal constructor(
        override val trace: PropertyTrace,
        override val message: StructuredMessage,
        override val exception: InstantExecutionErrorException
    ) : PropertyProblem()
}


data class StructuredMessage(val fragments: List<Fragment>) {

    override fun toString(): String = fragments.joinToString(separator = "") { fragment ->
        when (fragment) {
            is Fragment.Text -> fragment.text
            is Fragment.Reference -> "'${fragment.name}'"
        }
    }

    sealed class Fragment {

        data class Text(val text: String) : Fragment()

        data class Reference(val name: String) : Fragment()
    }

    companion object {

        fun build(builder: Builder.() -> Unit) = StructuredMessage(
            Builder().apply(builder).fragments
        )
    }

    class Builder {

        internal
        val fragments = mutableListOf<Fragment>()

        fun text(string: String) {
            fragments.add(Fragment.Text(string))
        }

        fun reference(name: String) {
            fragments.add(Fragment.Reference(name))
        }

        fun reference(type: Class<*>) {
            reference(type.name)
        }

        fun reference(type: KClass<*>) {
            reference(type.qualifiedName!!)
        }
    }
}


sealed class PropertyTrace {

    object Unknown : PropertyTrace()

    object Gradle : PropertyTrace()

    class Task(
        val type: Class<*>,
        val path: String
    ) : PropertyTrace()

    class Bean(
        val type: Class<*>,
        val trace: PropertyTrace
    ) : PropertyTrace()

    class Property(
        val kind: PropertyKind,
        val name: String,
        val trace: PropertyTrace
    ) : PropertyTrace()

    override fun toString(): String =
        StringBuilder().apply {
            sequence.forEach {
                appendStringOf(it)
            }
        }.toString()

    private
    fun StringBuilder.appendStringOf(trace: PropertyTrace) {
        when (trace) {
            is Gradle -> {
                append("Gradle runtime")
            }
            is Property -> {
                append(trace.kind)
                append(" ")
                quoted(trace.name)
                append(" of ")
            }
            is Bean -> {
                quoted(trace.type.name)
                append(" bean found in ")
            }
            is Task -> {
                append("task ")
                quoted(trace.path)
                append(" of type ")
                quoted(trace.type.name)
            }
            is Unknown -> {
                append("unknown property")
            }
        }
    }

    private
    fun StringBuilder.quoted(s: String) {
        append('`')
        append(s)
        append('`')
    }

    val sequence: Sequence<PropertyTrace>
        get() = sequence {
            var trace = this@PropertyTrace
            while (true) {
                yield(trace)
                trace = trace.tail ?: break
            }
        }

    private
    val tail: PropertyTrace?
        get() = when (this) {
            is Bean -> trace
            is Property -> trace
            else -> null
        }
}


enum class PropertyKind {
    Field {
        override fun toString() = "field"
    },
    InputProperty {
        override fun toString() = "input property"
    },
    OutputProperty {
        override fun toString() = "output property"
    }
}
