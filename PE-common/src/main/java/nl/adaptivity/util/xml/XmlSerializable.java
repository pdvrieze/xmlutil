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

package nl.adaptivity.util.xml;

import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


public interface XmlSerializable {

  @XmlAccessorType(XmlAccessType.PROPERTY)
  class SimpleAdapter {
    private volatile static MethodHandle _getContext;
    private volatile static boolean _failedReflection = false;
    private static MethodHandle _getAllDeclaredPrefixes;
    private static MethodHandle _getNamespaceURI;

    QName name;
    private SimpleNamespaceContext namespaceContext;

    @NotNull
    public Map<QName, Object> getAttributes() {
      return attributes;
    }

    @XmlAnyAttribute final
    Map<QName, Object> attributes = new HashMap<>();

    @XmlAnyElement(lax = true, value= W3CDomHandler.class) final
    List<Object> children = new ArrayList<>();

    public void setAttributes(@NotNull final NamedNodeMap attributes) {
      for(int i=attributes.getLength()-1; i>=0; --i) {
        final Attr attr = (Attr) attributes.item(i);
        final String prefix = attr.getPrefix();
        if (prefix==null) {
          this.attributes.put(new QName(attr.getLocalName()), attr.getValue());
        } else if (! XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)){
          this.attributes.put(new QName(attr.getNamespaceURI(), attr.getLocalName(), prefix), attr.getValue());
        }
      }
    }

    public void beforeUnmarshal(@NotNull final Unmarshaller unmarshaller, final Object parent) {
      if (parent instanceof JAXBElement) {
        name = ((JAXBElement) parent).getName();
      }

      if (_failedReflection) {
        return;
      }
      final Object context;
      try {
        if (_getContext == null) {
          synchronized (getClass()) {
            final Lookup lookup = MethodHandles.lookup();
            _getContext = lookup.unreflect(unmarshaller.getClass().getMethod("getContext"));
            context = _getContext.invoke(unmarshaller);
            _getAllDeclaredPrefixes = lookup.unreflect(context.getClass().getMethod("getAllDeclaredPrefixes"));
            _getNamespaceURI = lookup.unreflect(context.getClass().getMethod("getNamespaceURI", String.class));

          }
        } else {
          context = _getContext.invoke(unmarshaller);
        }

        if (context != null) {
          final String[] prefixes = (String[]) _getAllDeclaredPrefixes.invoke(context);
          if (prefixes != null && prefixes.length > 0) {
            final String[] namespaces = new String[prefixes.length];
            for (int i = prefixes.length - 1; i >= 0; --i) {
              namespaces[i] = (String) _getNamespaceURI.invoke(context, prefixes[i]);
            }
            namespaceContext = new SimpleNamespaceContext(prefixes, namespaces);
          }
        }

      } catch (@NotNull final Throwable e) {
        Logger.getAnonymousLogger().log(Level.FINE, "Could not retrieve namespace context from marshaller", e);
        _failedReflection = true;
      }
    }

  }

  class JAXBUnmarshallingAdapter<T extends XmlSerializable> extends JAXBAdapter {

    @NotNull
    private final XmlDeserializerFactory<T> mFactory;

    public JAXBUnmarshallingAdapter(@NotNull final Class<T> targetType) {
      final XmlDeserializer factoryTypeAnn = targetType.getAnnotation(XmlDeserializer.class);
      if (factoryTypeAnn==null || factoryTypeAnn.value()==null) {
        throw new IllegalArgumentException("For unmarshalling with this adapter to work, the type "+targetType.getName()+" must have the "+XmlDeserializer.class.getName()+" annotation");
      }
      try {
        @SuppressWarnings("unchecked") final XmlDeserializerFactory<T> factory = factoryTypeAnn.value().newInstance();
        mFactory = factory;
      } catch (@NotNull InstantiationException|IllegalAccessException e) {
        throw new IllegalArgumentException("The factory must have a visible no-arg constructor",e);
      }
    }

    @Override
    public T unmarshal(@NotNull final SimpleAdapter v) throws Exception {
      try {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final Document document = dbf.newDocumentBuilder().newDocument();

        final QName outerName = v.name == null ? new QName("value") : v.name;
        final Element root;
        root = XmlUtil.createElement(document, outerName);


        final SimpleNamespaceContext sourceNamespaceContext = v.namespaceContext;



        for (int i = ((SimpleNamespaceContext) sourceNamespaceContext).size() - 1; i >= 0; --i) {
          final String prefix = sourceNamespaceContext.getPrefix(i);
          final String namespace = sourceNamespaceContext.getNamespaceURI(i);
          if (! (XMLConstants.NULL_NS_URI.equals(namespace)|| // Not null namespace
                 XMLConstants.XML_NS_PREFIX.equals(prefix)|| // or xml mPrefix
                 XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))) { // or xmlns mPrefix

          }

          if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) { // Set the default namespace, unless it is the null namespace
            if (! XMLConstants.NULL_NS_URI.equals(namespace)) {
              root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", namespace);
            }
          } else if (! XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) { // Bind the mPrefix, except for xmlns itself
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, namespace);
          }
        }


        for (final Entry<QName, Object> attr : v.attributes.entrySet()) {
          XmlUtil.setAttribute(root, attr.getKey(), (String) attr.getValue());
        }
        for (final Object child : v.children) {
          if (child instanceof Node) {
            root.appendChild(document.importNode((Node) child, true));
          }
        }
        final XMLInputFactory xif = XMLInputFactory.newFactory();
        final XmlReader reader = XmlStreaming.newReader(new DOMSource(root));
        reader.nextTag();
        // XXX remove temporary cast
        return mFactory.deserialize((StAXReader) reader);
      } catch (@NotNull final Exception e) {
        e.printStackTrace();
        throw e;
      }
    }

  }

  class JAXBAdapter extends XmlAdapter<SimpleAdapter, XmlSerializable> {

    @Override
    public XmlSerializable unmarshal(final SimpleAdapter v) throws Exception {
      throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public SimpleAdapter marshal(@NotNull final XmlSerializable v) throws Exception {


      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final Document document = dbf.newDocumentBuilder().newDocument();
      final DocumentFragment content = document.createDocumentFragment();
      final XMLOutputFactory xof = XMLOutputFactory.newFactory();
      final XMLStreamWriter out = xof.createXMLStreamWriter(new DOMResult(content));
      // XXX Fix this temporary cast
      v.serialize((StAXWriter) XmlStreaming.newWriter(new DOMResult(content)));
      final int childCount = content.getChildNodes().getLength();
      if (childCount==0) {
        return new SimpleAdapter();
      } else if (childCount==1) {
        final SimpleAdapter result = new SimpleAdapter();
        final Node child = content.getFirstChild();
        if (child instanceof Element) {
          result.setAttributes(child.getAttributes());
          for(Node child2=child.getFirstChild(); child2!=null; child2=child2.getNextSibling()) {
            result.children.add(child2);
          }
        } else {
          result.children.add(child);
        }
        return result;
      } else { // More than one child
        final SimpleAdapter result = new SimpleAdapter();
        for(Node child=content.getFirstChild(); child!=null; child=child.getNextSibling()) {
          result.children.add(child);
        }
        return result;
      }
    }
  }

  /** XXX Get rid of StAXWriter
   * Write the object to an xml stream. The object is expected to write itself and its children.
   * @param out The stream to write to.
   * @throws XMLStreamException When something breaks.
   */
  void serialize(XmlWriter out) throws XmlException;

}
