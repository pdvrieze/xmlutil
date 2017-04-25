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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import nl.adaptivity.util.contentEquals
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer

import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

import java.io.IOException
import java.io.OutputStream
import java.io.Writer


/**
 * An android implementation of XmlWriter.
 * Created by pdvrieze on 15/11/15.
 */
class AndroidXmlWriter : AbstractXmlWriter
{

  private val mNamespaceHolder = NamespaceHolder()
  private val mRepairNamespaces: Boolean
  private val mWriter: XmlSerializer

  @Throws(XmlPullParserException::class, IOException::class)
  @JvmOverloads constructor(writer: Writer, repairNamespaces: Boolean = true) : this(repairNamespaces)
  {
    mWriter.setOutput(writer)
    initWriter(mWriter)
  }

  @Throws(XmlPullParserException::class)
  private constructor(repairNamespaces: Boolean)
  {
    mRepairNamespaces = repairNamespaces
    mWriter = BetterXmlSerializer()
    initWriter(mWriter)
  }

  @Throws(XmlPullParserException::class, IOException::class)
  @JvmOverloads constructor(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean = true) : this(
      repairNamespaces)
  {
    mWriter.setOutput(outputStream, encoding)
    initWriter(mWriter)
  }

  private fun initWriter(writer: XmlSerializer)
  {
    try
    {
      writer.setPrefix(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI)
    } catch (e: IOException)
    {
      throw RuntimeException(e)
    }

  }

  @JvmOverloads constructor(serializer: XmlSerializer, repairNamespaces: Boolean = true)
  {
    mWriter = serializer
    mRepairNamespaces = repairNamespaces
    initWriter(mWriter)
  }

  // Object Initialization end

  @Throws(XmlException::class)
  override fun flush()
  {
    try
    {
      mWriter.flush()
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun startTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?)
  {
    val namespaceStr = StringUtil.toString(namespace!!.toString())
    try
    {
      if (namespace != null && namespace.length > 0)
      {
        mWriter.setPrefix(prefix?.toString() ?: "", namespaceStr)
      }
      mWriter.startTag(namespaceStr, StringUtil.toString(localName))
      mNamespaceHolder.incDepth()
      ensureNamespaceIfRepairing(namespace, prefix)
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  private fun ensureNamespaceIfRepairing(namespace: CharSequence, prefix: CharSequence?)
  {
    if (mRepairNamespaces && namespace != null && namespace.length > 0 && prefix != null)
    {
      // TODO fix more cases than missing namespaces with given prefix and uri
      if (!StringUtil.isEqual(mNamespaceHolder.getNamespaceUri(prefix), namespace))
      {
        namespaceAttr(prefix, namespace)
      }
    }
  }

  @Throws(XmlException::class)
  override fun comment(text: CharSequence)
  {
    try
    {
      mWriter.comment(StringUtil.toString(text))
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun text(text: CharSequence)
  {
    try
    {
      mWriter.text(text.toString())
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun cdsect(text: CharSequence)
  {
    try
    {
      mWriter.cdsect(text.toString())
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun entityRef(text: CharSequence)
  {
    try
    {
      mWriter.entityRef(StringUtil.toString(text))
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun processingInstruction(text: CharSequence)
  {
    try
    {
      mWriter.processingInstruction(StringUtil.toString(text))
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun ignorableWhitespace(text: CharSequence)
  {
    try
    {
      mWriter.ignorableWhitespace(StringUtil.toString(text))
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun attribute(namespace: CharSequence?, name: CharSequence, prefix: CharSequence?, value: CharSequence)
  {
    try
    {
      val sNamespace = StringUtil.toString(namespace)
      val sPrefix = StringUtil.toString(prefix)
      if (sPrefix != null && sNamespace != null)
      {
        setPrefix(sPrefix!!, sNamespace!!)
      }
      mWriter.attribute(sNamespace, StringUtil.toString(name), StringUtil.toString(value))
      ensureNamespaceIfRepairing(sNamespace!!, sPrefix)
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun docdecl(text: CharSequence)
  {
    try
    {
      mWriter.docdecl(StringUtil.toString(text))
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  /**
   * {@inheritDoc}
   * @param version Unfortunately the serializer is forced to version 1.0
   */
  @Throws(XmlException::class)
  override fun startDocument(version: CharSequence?, encoding: CharSequence?, standalone: Boolean?)
  {
    try
    {
      mWriter.startDocument(StringUtil.toString(encoding), standalone)
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun endDocument()
  {
    if (BuildConfig.DEBUG && depth != 0) throw AssertionError()
    try
    {
      mWriter.endDocument()
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun endTag(namespace: CharSequence?, localName: CharSequence, prefix: CharSequence?)
  {
    try
    {
      mWriter.endTag(StringUtil.toString(namespace), StringUtil.toString(localName))
      mNamespaceHolder.decDepth()
    } catch (e: IOException)
    {
      throw XmlException(e)
    }

  }

  @Throws(XmlException::class)
  override fun setPrefix(prefix: CharSequence, namespaceUri: CharSequence)
  {
    if (!StringUtil.isEqual(namespaceUri, getNamespaceUri(prefix)))
    {
      mNamespaceHolder.addPrefixToContext(prefix, namespaceUri)
      try
      {
        mWriter.setPrefix(StringUtil.toString(prefix), StringUtil.toString(namespaceUri))
      } catch (e: IOException)
      {
        throw XmlException(e)
      }

    }
  }

  @Throws(XmlException::class)
  override fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence)
  {
    mNamespaceHolder.addPrefixToContext(namespacePrefix, namespaceUri)
    try
    {
      if (namespacePrefix != null && namespacePrefix.length > 0)
      {
        mWriter.attribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, StringUtil.toString(namespacePrefix),
                          StringUtil.toString(namespaceUri))
      } else
      {
        mWriter.attribute(XMLConstants.NULL_NS_URI, XMLConstants.XMLNS_ATTRIBUTE, StringUtil.toString(namespaceUri))
      }
    } catch (e: IOException)
    {
      throw RuntimeException(e)
    }

  }

  override val namespaceContext: NamespaceContext
    get() = mNamespaceHolder.namespaceContext

  override fun getNamespaceUri(prefix: CharSequence): CharSequence?
  {
    return mNamespaceHolder.getNamespaceUri(prefix)
  }

  override fun getPrefix(namespaceUri: CharSequence?): CharSequence?
  {
    return mNamespaceHolder.getPrefix(namespaceUri!!)
  }

  @Throws(XmlException::class)
  override fun close()
  {
    super.close()
    mNamespaceHolder.clear()
  }

  // Property accessors start
  override val depth: Int
    get() = mNamespaceHolder.depth
  // Property acccessors end
}// Object Initialization

private object StringUtil {
  @Deprecated("Not needed in kotlin", ReplaceWith("charSequence.toString()"))
  fun toString(charSequence: CharSequence): String {
    return charSequence.toString()
  }


  @JvmName("toStringOpt")
  @Deprecated("Not needed in kotlin", ReplaceWith("charSequence?.toString()"))
  fun toString(charSequence: CharSequence?): String? {
    return charSequence?.toString()
  }

  @Deprecated("Not needed anymore", ReplaceWith("seq1.contentEquals(seq2)"))
  fun isEqual(seq1: CharSequence, seq2: CharSequence):Boolean {
    return seq1.contentEquals(seq2)
  }

  @JvmName("isEqualO1O2")
  @Deprecated("Not needed anymore", ReplaceWith("seq1.contentEquals(seq2)"))
  fun isEqual(seq1: CharSequence?, seq2: CharSequence?):Boolean {
    return if(seq2==null) seq1==null else seq1?.contentEquals(seq2) ?: false
  }

  @JvmName("isEqualO1")
  @Deprecated("Not needed anymore", ReplaceWith("seq1.contentEquals(seq2)"))
  fun isEqual(seq1: CharSequence?, seq2: CharSequence):Boolean {
    return seq1?.contentEquals(seq2) ?: false
  }
}