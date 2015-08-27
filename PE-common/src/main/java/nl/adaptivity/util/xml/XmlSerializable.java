package nl.adaptivity.util.xml;

import net.devrieze.annotations.NotNull;
import org.w3c.dom.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public interface XmlSerializable {

  @XmlAccessorType(XmlAccessType.PROPERTY)
  public static class SimpleAdapter {

    QName name;

    public Map<QName, Object> getAttributes() {
      return attributes;
    }

    @XmlAnyAttribute
    Map<QName, Object> attributes = new HashMap<>();

    @XmlAnyElement(lax = true, value= W3CDomHandler.class)
    List<Object> children = new ArrayList<>();

    public void setAttributes(final NamedNodeMap pAttributes) {
      for(int i=pAttributes.getLength()-1; i>=0; --i) {
        Attr attr = (Attr) pAttributes.item(i);
        String prefix = attr.getPrefix();
        if (prefix==null) {
          attributes.put(new QName(attr.getLocalName()), attr.getValue());
        } else {
          attributes.put(new QName(attr.getNamespaceURI(), attr.getLocalName(), prefix), attr.getValue());
        }
      }
    }

    public void beforeUnmarshal(Unmarshaller unmarshaller, Object parent) {
      if (parent instanceof JAXBElement) {
        name=((JAXBElement) parent).getName();
      }
    }
  }

  public static class JAXBUnmarshallingAdapter extends JAXBAdapter {

    @NotNull
    private final XmlDeserializerFactory<?> mFactory;

    public JAXBUnmarshallingAdapter(Class<? extends XmlSerializable> targetType) {
      XmlDeserializer factoryTypeAnn = targetType.getAnnotation(XmlDeserializer.class);
      if (factoryTypeAnn==null || factoryTypeAnn.value()==null) {
        throw new IllegalArgumentException("For unmarshalling with this adapter to work, the type must have the "+XmlDeserializer.class.getName()+" annotation");
      }
      try {
        mFactory = factoryTypeAnn.value().newInstance();
      } catch (InstantiationException|IllegalAccessException e) {
        throw new IllegalArgumentException("The factory must have a visible no-arg constructor",e);
      }
    }

    @Override
    public XmlSerializable unmarshal(final SimpleAdapter v) throws Exception {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      Document document = dbf.newDocumentBuilder().newDocument();

      QName outerName = v.name==null ? new QName("value") : v.name;
      Element root;
      root = XmlUtil.createElement(document, outerName);
      for (Entry<QName, Object> attr: v.attributes.entrySet()) {
        XmlUtil.setAttribute(root, attr.getKey(),(String) attr.getValue());
      }
      for (Object child:v.children) {
        root.appendChild((Node) child);
      }

      XMLInputFactory xif = XMLInputFactory.newFactory();
      XMLStreamReader xsr = xif.createXMLStreamReader(new DOMSource(root));
      return (XmlSerializable) mFactory.deserialize(xsr);
    }

  }

  public static class JAXBAdapter extends XmlAdapter<SimpleAdapter, XmlSerializable> {

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
