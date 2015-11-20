package nl.adaptivity.xml;

/**
 * Created by pdvrieze on 16/11/15.
 */
public abstract class AbstractXmlWriter implements XmlWriter {

  /**
   * Default implementation that merely flushes the stream.
   * @throws XmlException When something fails
   */
  @Override
  public void close() throws XmlException {
    flush();
  }
}
