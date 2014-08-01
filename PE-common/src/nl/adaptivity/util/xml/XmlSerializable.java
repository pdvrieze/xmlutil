package nl.adaptivity.util.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


public interface XmlSerializable {
  /**
   * Write the object to an xml stream. The object is expected to write itself and its children.
   * @param out The stream to write to.
   * @throws XMLStreamException When something breaks.
   */
  void serialize(XMLStreamWriter out) throws XMLStreamException;
}
