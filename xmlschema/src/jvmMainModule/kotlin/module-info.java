module io.github.pdvrieze.xmlutil.xmlschema {
    requires net.devrieze.xmlutil.serialization;
    requires kotlinx.serialization.core;
    requires kotlinx.datetime;
    requires java.xml;

    exports io.github.pdvrieze.formats.xmlschema.datatypes;
    exports io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances;
    exports io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes;
    exports io.github.pdvrieze.formats.xmlschema.datatypes.serialization;
    exports io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets;
    exports io.github.pdvrieze.formats.xmlschema.model;
    exports io.github.pdvrieze.formats.xmlschema.regex;
    exports io.github.pdvrieze.formats.xmlschema.resolved;
    exports io.github.pdvrieze.formats.xmlschema.resolved.checking;
    exports io.github.pdvrieze.formats.xmlschema.resolved.facets;
    exports io.github.pdvrieze.formats.xmlschema.types;

    exports io.github.pdvrieze.formats.xpath;
}
