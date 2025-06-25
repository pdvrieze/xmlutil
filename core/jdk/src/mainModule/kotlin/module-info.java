module net.devrieze.xmlutil.core.jdk {
    requires transitive net.devrieze.xmlutil.core;
    requires static java.xml;

    exports nl.adaptivity.xmlutil.jdk;

    provides nl.adaptivity.xmlutil.XmlStreamingFactory
            with nl.adaptivity.xmlutil.jdk.StAXStreamingFactory;
}
