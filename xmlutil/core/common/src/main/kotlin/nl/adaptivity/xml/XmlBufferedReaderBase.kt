/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import nl.adaptivity.xml.XmlEvent.*


abstract class XmlBufferedReaderBase(private val delegate: XmlReader) : AbstractXmlReader() {
  private val namespaceHolder = NamespaceHolder()

  protected abstract val hasPeekItems:Boolean

  protected var current: XmlEvent? = null
    private set

  private val currentElement: StartElementEvent
    get() = current as? StartElementEvent ?: throw XmlException(
      "Expected a start element, but did not find it.")

  override val namespaceUri: CharSequence
    get() = when (current?.eventType) {
      EventType.ATTRIBUTE     -> (current as Attribute).namespaceUri
      EventType.START_ELEMENT -> (current as StartElementEvent).namespaceUri
      EventType.END_ELEMENT   -> (current as EndElementEvent).namespaceUri
      else                                      -> throw XmlException(
        "Attribute not defined here: namespaceUri")
    }


  override val localName: CharSequence
    get() = when (current?.eventType) {
      EventType.ATTRIBUTE     -> (current as Attribute).localName
      EventType.START_ELEMENT -> (current as StartElementEvent).localName
      EventType.END_ELEMENT   -> (current as EndElementEvent).localName
      else                                      -> throw XmlException(
        "Attribute not defined here: namespaceUri")
    }

  override val prefix: CharSequence
    get() = when (current?.eventType) {
      EventType.ATTRIBUTE     -> (current as Attribute).prefix
      EventType.START_ELEMENT -> (current as StartElementEvent).prefix
      EventType.END_ELEMENT   -> (current as EndElementEvent).prefix
      else                                      -> throw XmlException(
        "Attribute not defined here: namespaceUri")
    }

  override val depth: Int
    get() = namespaceHolder.depth

  override val text: CharSequence
    get() {
      return if (current!!.eventType === EventType.ATTRIBUTE) {
        (current as Attribute).value
      } else (current as TextEvent).text
    }

  override val attributeCount: Int
    get() = currentElement.attributes.size

  override val isStarted: Boolean
    get() = current!=null

  override val eventType: EventType
    get() = current?.eventType ?: if (hasNext()) {
      throw XmlException("Attempting to get the event type before getting an event.")
    } else {
      throw XmlException("Attempting to read beyond the end of the stream")
    }

  override val namespaceStart:Int get() = 0

  override val namespaceEnd: Int
    get() = currentElement.namespaceDecls.size

  override val locationInfo: String? get() { // allow for location info at the start of the document
    return current?.locationInfo ?: delegate.locationInfo
  }

  override val namespaceContext: NamespaceContext
    get(){
      return currentElement.namespaceContext
    }

  override val encoding: CharSequence?
    get() = (current as StartDocumentEvent).encoding

  override val standalone: Boolean?
    get() = (current as StartDocumentEvent).standalone

  override val version: CharSequence?
    get() = (current as StartDocumentEvent).version

  fun nextEvent(): XmlEvent {
    if (hasPeekItems) {
      return removeFirstToCurrent()
    }
    if (!hasNext()) {
      throw NoSuchElementException()
    }
    peek()
    return removeFirstToCurrent()
  }

  private fun removeFirstToCurrent(): XmlEvent {
    val event:XmlEvent = bufferRemoveFirst()
    this.current = event

    when (event.eventType) {
      EventType.START_ELEMENT -> {
        namespaceHolder.incDepth()
        val start = event as StartElementEvent
        for (ns in start.namespaceDecls) {
          namespaceHolder.addPrefixToContext(ns)
        }
      }
      EventType.END_ELEMENT   -> namespaceHolder.decDepth()
      else                                      -> {} /* Do nothing */
    }
    return event
  }

  internal fun peek(): XmlEvent? {
    if (hasPeekItems) {
      return peekFirst()
    }
    addAll(doPeek())
    return peekFirst()
  }

  /**
   * Get the next event to add to the queue. Children can override this to customize the events that are added to the
   * peek buffer. Normally this method is only called when the peek buffer is empty.
   */
  protected open fun doPeek(): List<XmlEvent> {
    if (delegate.hasNext()) {
      delegate.next() // Don't forget to actually read the next element
      val event = XmlEvent.from(delegate)
      val result = ArrayList<XmlEvent>(1)
      result.add(event)
      return result
    }
    return emptyList()
  }

  override fun hasNext(): Boolean {
    if (hasPeekItems) {
      return true
    }
    return peek() != null

  }

  protected fun stripWhiteSpaceFromPeekBuffer() {
    while (hasPeekItems && peekLast().let { peekLast ->
      peekLast is TextEvent && isXmlWhitespace(peekLast.text) }) {
      bufferRemoveLast()
    }
  }


  protected abstract fun peekFirst(): XmlEvent?
  protected abstract fun peekLast(): XmlEvent?
  protected abstract fun bufferRemoveLast():XmlEvent
  protected abstract fun bufferRemoveFirst():XmlEvent
  protected abstract fun add(event: XmlEvent)
  protected abstract fun addAll(events: Collection<XmlEvent>)

  override fun close() {
    delegate.close()
  }

  override fun nextTag(): EventType
  {
    return nextTagEvent().eventType
  }

  fun nextTagEvent(): XmlEvent {
    val current = nextEvent()
    return when (current.eventType) {
      EventType.TEXT                                                   -> {
        if (isXmlWhitespace((current as TextEvent).text)) {
          nextTagEvent()
        } else {
          throw XmlException("Unexpected element found when looking for tags: " + current)
        }
      }
      EventType.COMMENT, EventType.IGNORABLE_WHITESPACE,
      EventType.PROCESSING_INSTRUCTION                                 -> nextTagEvent()
      EventType.START_ELEMENT, EventType.END_ELEMENT -> current
      else                                                                               -> throw XmlException(
        "Unexpected element found when looking for tags: " + current)
    }
  }

  override fun next(): EventType {
    return nextEvent().eventType
  }

  override fun getAttributeNamespace(i: Int) = currentElement.attributes[i].namespaceUri

  override fun getAttributePrefix(i: Int) = currentElement.attributes[i].prefix

  override fun getAttributeLocalName(i: Int) = currentElement.attributes[i].localName

  override fun getAttributeValue(i: Int) = currentElement.attributes[i].value

  override fun getAttributeValue(nsUri: CharSequence?, localName: CharSequence) =
        currentElement.attributes.firstOrNull { attr ->
            (nsUri == null || nsUri == attr.namespaceUri) && localName == attr.localName
        }?.value


  override fun getNamespacePrefix(i: Int): CharSequence {
    return currentElement.namespaceDecls[i].prefix
  }

  override fun getNamespaceUri(i: Int): CharSequence {
    return currentElement.namespaceDecls[i].namespaceURI
  }

  override fun getNamespacePrefix(namespaceUri: CharSequence): CharSequence? {
    return currentElement.getPrefix(namespaceUri)
  }

  override fun getNamespaceUri(prefix: CharSequence): String? {
    return currentElement.getNamespaceUri(prefix)
  }
}
