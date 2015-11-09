package nl.adaptivity.util.xml;

import net.devrieze.annotations.NotNull;
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

    public Map<QName, Object> getAttributes() {
      return attributes;
    }

    @XmlAnyAttribute final
    Map<QName, Object> attributes = new HashMap<>();

    @XmlAnyElement(lax = true, value= W3CDomHandler.class) final
    List<Object> children = new ArrayList<>();

    public void setAttributes(final NamedNodeMap pAttributes) {
      for(int i=pAttributes.getLength()-1; i>=0; --i) {
        Attr attr = (Attr) pAttributes.item(i);
        String prefix = attr.getPrefix();
        if (prefix==null) {
          attributes.put(new QName(attr.getLocalName()), attr.getValue());
        } else if (! XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)){
          attributes.put(new QName(attr.getNamespaceURI(), attr.getLocalName(), prefix), attr.getValue());
        }
      }
    }

    public void beforeUnmarshal(Unmarshaller unmarshaller, Object parent) {
      if (parent instanceof JAXBElement) {
        name = ((JAXBElement) parent).getName();
      }

      if (_failedReflection) {
        return;
      }
      Object context;
      try {
        if (_getContext == null) {
          synchronized (getClass()) {
            Lookup lookup = MethodHandles.lookup();
            _getContext = lookup.unreflect(unmarshaller.getClass().getMethod("getContext"));
            context = _getContext.invoke(unmarshaller);
            _getAllDeclaredPrefixes = lookup.unreflect(context.getClass().getMethod("getAllDeclaredPrefixes"));
            _getNamespaceURI = lookup.unreflect(context.getClass().getMethod("getNamespaceURI", String.class));

          }
        } else {
          context = _getContext.invoke(unmarshaller);
        }

        if (context != null) {
          String[] prefixes = (String[]) _getAllDeclaredPrefixes.invoke(context);
          if (prefixes != null && prefixes.length > 0) {
            String[] namespaces = new String[prefixes.length];
            for (int i = prefixes.length - 1; i >= 0; --i) {
              namespaces[i] = (String) _getNamespaceURI.invoke(context, prefixes[i]);
            }
            namespaceContext = new SimpleNamespaceContext(prefixes, namespaces);
          }
        }

      } catch (Throwable e) {
        Logger.getAnonymousLogger().log(Level.FINE, "Could not retrieve namespace context from marshaller", e);
        _failedReflection = true;
      }
    }

  }

  class JAXBUnmarshallingAdapter<T extends XmlSerializable> extends JAXBAdapter {

    @NotNull
    private final XmlDeserializerFactory<T> mFactory;

    public JAXBUnmarshallingAdapter(Class<T> targetType) {
      XmlDeserializer factoryTypeAnn = targetType.getAnnotation(XmlDeserializer.class);
      if (factoryTypeAnn==null || factoryTypeAnn.value()==null) {
        throw new IllegalArgumentException("For unmarshalling with this adapter to work, the type "+targetType.getName()+" must have the "+XmlDeserializer.class.getName()+" annotation");
      }
      try {
        @SuppressWarnings("unchecked") XmlDeserializerFactory<T> factory = factoryTypeAnn.value().newInstance();
        mFactory = factory;
      } catch (InstantiationException|IllegalAccessException e) {
        throw new IllegalArgumentException("The factory must have a visible no-arg constructor",e);
      }
    }

    @Override
    public T unmarshal(final SimpleAdapter v) throws Exception {
      try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document document = dbf.newDocumentBuilder().newDocument();

        QName outerName = v.name == null ? new QName("value") : v.name;
        Element root;
        root = XmlUtil.createElement(document, outerName);


        SimpleNamespaceContext sourceNamespaceContext = v.namespaceContext;



        for (int i = ((SimpleNamespaceContext) sourceNamespaceContext).size() - 1; i >= 0; --i) {
          String prefix = sourceNamespaceContext.getPrefix(i);
          String namespace = sourceNamespaceContext.getNamespaceURI(i);
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


        for (Entry<QName, Object> attr : v.attributes.entrySet()) {
          XmlUtil.setAttribute(root, attr.getKey(), (String) attr.getValue());
        }
        for (Object child : v.children) {
          if (child instanceof Node) {
            root.appendChild(document.importNode((Node) child, true));
          }
        }
        XMLInputFactory xif = XMLInputFactory.newFactory();
        XMLStreamReader xsr = xif.createXMLStreamReader(new DOMSource(root));
        xsr.nextTag();
        return mFactory.deserialize(xsr);
      } catch (Exception e) {
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

    @Override
    public SimpleAdapter marshal(final XmlSerializable v) throws Exception {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      Document document = dbf.newDocumentBuilder().newDocument();
      DocumentFragment content = document.createDocumentFragment();
      XMLOutputFactory xof = XMLOutputFactory.newFactory();
      XMLStreamWriter out = xof.createXMLStreamWriter(new DOMResult(content));
      v.serialize(out);
      int childCount = content.getChildNodes().getLength();
      if (childCount==0) {
        return new SimpleAdapter();
      } else if (childCount==1) {
        SimpleAdapter result = new SimpleAdapter();
        Node child = content.getFirstChild();
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
        SimpleAdapter result = new SimpleAdapter();
        for(Node child=content.getFirstChild(); child!=null; child=child.getNextSibling()) {
          result.children.add(child);
        }
        return result;
      }
    }
  }

  /**
   * Write the object to an xml stream. The object is expected to write itself and its children.
   * @param out The stream to write to.
   * @throws XMLStreamException When something breaks.
   */
  void serialize(XMLStreamWriter out) throws XMLStreamException;

}
