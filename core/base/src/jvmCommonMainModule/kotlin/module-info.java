module net.devrieze.xmlutil.core {
    requires transitive kotlin.stdlib;
    requires static kotlinx.serialization.core;
    requires static java.xml;

    exports nl.adaptivity.xmlutil;
    exports nl.adaptivity.xmlutil.core;
    exports nl.adaptivity.xmlutil.core.impl to
            net.devrieze.xmlutil.core.jdk, net.devrieze.xmlutil.serialization;
    exports nl.adaptivity.xmlutil.core.impl.idom to
            net.devrieze.xmlutil.serialization;
    exports nl.adaptivity.xmlutil.core.impl.multiplatform to
            io.github.pdvrieze.testutil, net.devrieze.xmlutil.serialization,
            io.github.pdvrieze.xmlutil.xmlschema, net.devrieze.xmlutil.xmlserializable;
    exports nl.adaptivity.xmlutil.core.internal to
            net.devrieze.xmlutil.serialization, io.github.pdvrieze.xmlutil.xmlschema;
    exports nl.adaptivity.xmlutil.dom;
    exports nl.adaptivity.xmlutil.dom2;
    exports nl.adaptivity.xmlutil.util;
    exports nl.adaptivity.xmlutil.util.impl to
            net.devrieze.xmlutil.serialization;

    // Note that while a generic implementation is provided, this is not published (so providers
    // can better override it).
    uses nl.adaptivity.xmlutil.XmlStreamingFactory;

    provides nl.adaptivity.xmlutil.util.SerializationProvider
            with nl.adaptivity.xmlutil.util.DefaultSerializationProvider;
}
