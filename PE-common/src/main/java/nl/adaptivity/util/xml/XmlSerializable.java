package nl.adaptivity.util.xml;

import org.w3c.dom.*;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public interface XmlSerializable {

  @XmlAccessorType(XmlAccessType.PROPERTY)
  public static class SimpleAdapter {

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
