/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import net.devrieze.util.kotlin.matches
import nl.adaptivity.xml.XmlEvent.*
import nl.adaptivity.xml.XmlStreaming.EventType
import java.util.*
import javax.xml.namespace.NamespaceContext


open class XmlBufferedReader(private val mDelegate: XmlReader) : AbstractXmlReader() {
  private val mPeekBuffer = ArrayDeque<XmlEvent>()
  private val mNamespaceHolder = NamespaceHolder()

  protected var current: XmlEvent? = null
    private set

  private val currentElement: StartElementEvent
    @Throws(XmlException::class)
    get() = current as? StartElementEvent ?: throw XmlException("Expected a start element, but did not find it.")

  @Throws(XmlException::class)
  fun nextEvent(): XmlEvent {
    if (!mPeekBuffer.isEmpty()) {
      return removeFirstToCurrent()
    }
    if (!hasNext()) {
      throw NoSuchElementException()
    }
    peek()
    return removeFirstToCurrent()
  }

  private fun removeFirstToCurrent(): XmlEvent {
    val event:XmlEvent = mPeekBuffer.removeFirst()
    this.current = event

    when (event.eventType) {
      EventType.START_ELEMENT -> {
        mNamespaceHolder.incDepth()
        val start = event as StartElementEvent
        for (ns in start.namespaceDecls) {
          mNamespaceHolder.addPrefixToContext(ns)
        }
      }
      EventType.END_ELEMENT   -> mNamespaceHolder.decDepth()
      else -> {} /* Do nothing */
    }
    return event
  }

  @Throws(XmlException::class)
  internal fun peek(): XmlEvent? {
    if (!mPeekBuffer.isEmpty()) {
      return mPeekBuffer.peek()
    }
    addAll(doPeek())
    return mPeekBuffer.peek()
  }

  /**
   * Get the next event to add to the queue. Children can override this to customize the events that are added to the
   * peek buffer. Normally this method is only called when the peek buffer is empty.
   */
  @Throws(XmlException::class)
  protected open fun doPeek(): List<XmlEvent> {
    if (mDelegate.hasNext()) {
      mDelegate.next() // Don't forget to actually read the next element
      val event = XmlEvent.from(mDelegate)
      val result = ArrayList<XmlEvent>(1)
      result.add(event)
      return result
    }
    return emptyList()
  }

  override fun hasNext(): Boolean {
    if (!mPeekBuffer.isEmpty()) {
      return true
    }
    try {
      return peek() != null
    } catch (e: XmlException) {
      throw RuntimeException(e)
    }

  }

  protected fun stripWhiteSpaceFromPeekBuffer() {
    var peekLast: XmlEvent
    while (mPeekBuffer.size > 0 && mPeekBuffer.peekLast().let { peekLast ->
      peekLast is TextEvent && isXmlWhitespace(peekLast.text) }) {
      mPeekBuffer.removeLast()
    }
  }

  protected val isPeekBufferEmpty: Boolean
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    get() = mPeekBuffer.isEmpty()

  protected fun peekFirst(): XmlEvent {
    return mPeekBuffer.peekFirst()
  }

  private fun add(event: XmlEvent) {
    mPeekBuffer.addLast(event)
  }

  private fun addAll(events: Collection<XmlEvent>) {
    mPeekBuffer.addAll(events)
  }

  @Throws(XmlException::class)
  override fun close() {
    mDelegate.close()
    mPeekBuffer.clear()
  }

  @Throws(XmlException::class)
  override fun nextTag(): EventType {
    return nextTagEvent().eventType
  }

  @Throws(XmlException::class)
  fun nextTagEvent(): XmlEvent {
    val current = nextEvent()
    when (current.eventType) {
      EventType.TEXT                                                                    -> {
        if (isXmlWhitespace((current as TextEvent).text)) {
          return nextTagEvent()
        }
        return nextTagEvent()
      }
      EventType.COMMENT // ignore
        , EventType.IGNORABLE_WHITESPACE, EventType.PROCESSING_INSTRUCTION -> return nextTagEvent()
      EventType.START_ELEMENT, EventType.END_ELEMENT                       -> return current
    }
    throw XmlException("Unexpected element found when looking for tags: " + current)
  }

  @Throws(XmlException::class)
  override fun next(): EventType? {
    return nextEvent().eventType
  }

  override val namespaceUri: CharSequence
    @Throws(XmlException::class)
    get() = when (current?.eventType) {
      EventType.ATTRIBUTE     -> (current as Attribute).namespaceUri
      EventType.START_ELEMENT -> (current as StartElementEvent).namespaceUri
      EventType.END_ELEMENT   -> (current as EndElementEvent).namespaceUri
      else                                 -> throw XmlException("Attribute not defined here: namespaceUri")
    }


  override val localName: CharSequence
    @Throws(XmlException::class)
    get() = when (current?.eventType) {
      EventType.ATTRIBUTE     -> (current as Attribute).localName
      EventType.START_ELEMENT -> (current as StartElementEvent).localName
      EventType.END_ELEMENT   -> (current as EndElementEvent).localName
      else                                 -> throw XmlException("Attribute not defined here: namespaceUri")
    }

  override val prefix: CharSequence
    @Throws(XmlException::class)
    get() = when (current?.eventType) {
      EventType.ATTRIBUTE     -> (current as Attribute).prefix
      EventType.START_ELEMENT -> (current as StartElementEvent).prefix
      EventType.END_ELEMENT   -> (current as EndElementEvent).prefix
      else                                 -> throw XmlException("Attribute not defined here: namespaceUri")
    }

  override val depth: Int
    get() = mNamespaceHolder.depth

  override val text: CharSequence
    get() {
      return if (current!!.eventType === EventType.ATTRIBUTE) {
         (current as Attribute).value
      } else (current as TextEvent).text
    }

  override val attributeCount: Int
    @Throws(XmlException::class)
    get() = currentElement.attributes.size

  @Throws(XmlException::class)
  override fun getAttributeNamespace(i: Int) = currentElement.attributes[i].namespaceUri

  @Throws(XmlException::class)
  override fun getAttributePrefix(i: Int) = currentElement.attributes[i].prefix

  @Throws(XmlException::class)
  override fun getAttributeLocalName(i: Int) = currentElement.attributes[i].localName

  @Throws(XmlException::class)
  override fun getAttributeValue(i: Int) = currentElement.attributes[i].value

  override val isStarted: Boolean
    get() = current!=null

  override val eventType: EventType
    @Throws(XmlException::class)
    get() = current?.eventType ?: if (hasNext()) {
      throw XmlException("Attempting to get the event type before getting an event.")
    } else {
      throw XmlException("Attempting to read beyond the end of the stream")
    }

  @Throws(XmlException::class)
  override fun getAttributeValue(nsUri: CharSequence?, localName: CharSequence) = 
        currentElement.attributes.firstOrNull { attr ->
    (nsUri == null || nsUri matches attr.namespaceUri) && localName matches attr.localName }?.value

  override val namespaceStart:Int get() = 0

  override val namespaceEnd: Int
    @Throws(XmlException::class)
    get() = currentElement.namespaceDecls.size

  @Throws(XmlException::class)
  override fun getNamespacePrefix(i: Int): CharSequence {
    return currentElement.namespaceDecls[i].prefix
  }

  @Throws(XmlException::class)
  override fun getNamespaceUri(i: Int): CharSequence {
    return currentElement.namespaceDecls[i].namespaceURI
  }

  @Throws(XmlException::class)
  override fun getNamespacePrefix(namespaceUri: CharSequence): CharSequence? {
    return currentElement.getPrefix(namespaceUri)
  }

  @Throws(XmlException::class)
  override fun getNamespaceUri(prefix: CharSequence): String? {
    return currentElement.getNamespaceUri(prefix)
  }

  override val locationInfo: String? get() { // allow for location info at the start of the document
    return current?.let {it.locationInfo} ?: mDelegate.locationInfo
  }

  override val namespaceContext: NamespaceContext
    @Throws(XmlException::class)
    get(){
    return currentElement.namespaceContext
  }

  override val encoding: CharSequence
    get() = (current as StartDocumentEvent).encoding

  override val standalone: Boolean?
    get() = (current as StartDocumentEvent).standalone

  override val version: CharSequence
    get() = (current as StartDocumentEvent).version
}
