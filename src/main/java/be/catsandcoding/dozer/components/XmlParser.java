package be.catsandcoding.dozer.components;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class XmlParser {

    private NodeList getNodes(String path) throws ParserConfigurationException, IOException, SAXException {
        try(InputStream ins = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(path)).openStream()) {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(ins));
            return document.getChildNodes();
        }
    }


    private List<Node> findNodes(NodeList nodes, String tagToRetain){
        final List<Node> found = new ArrayList<>();

        for(int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if(node.hasChildNodes()) {
                found.addAll(findNodes(node.getChildNodes(), tagToRetain));
            }
            if(node.getNodeName().equals(tagToRetain)) {
                found.add(node);
            }
        }

        return found;
    }

    public List<Field> parseMainConfig(String path) throws IOException, SAXException, ParserConfigurationException {
        List<Node> nodes = findNodes(getNodes(path), "converter");

        return nodes.stream().map( node -> {
            String converter = getAttribute(node, "type");
            String from = getChildNodeTextContent(node, "class-a");
            String to = getChildNodeTextContent(node, "class-b");
            return new Field(from, to, converter);
        }).collect(Collectors.toList());
    }

    public List<Mapping> parseServiceConfig(String path) throws IOException, SAXException, ParserConfigurationException {
        List<Node> nodes = findNodes(getNodes(path), "mapping");

        return nodes.stream().map( node -> {
            String type = getAttribute(node, "type");
            boolean oneWay = type != null && type.equals("one-way");

            String from = getChildNodeTextContent(node, "class-a");
            String to = getChildNodeTextContent(node, "class-b");
            List<Field> fields = findNodes(node.getChildNodes(), "field").stream().map( fieldNode -> {
                String fromField = getChildNodeTextContent(fieldNode, "a");
                String toField = getChildNodeTextContent(fieldNode, "b");
                String converter = getAttribute(fieldNode, "custom-converter");
                return new Field(fromField, toField, converter);
            }).collect(Collectors.toList());
            return new Mapping(from, to, fields, oneWay);
        }).collect(Collectors.toList());
    }

    private String getAttribute(Node node, String attributeName) {
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        return attribute == null ? null : attribute.getNodeValue();
    }
    private String getChildNodeTextContent(Node node, String childName) {
        return findNodes(node.getChildNodes(), childName).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("XML config: " + childName +" missing")).getTextContent();
    }

    public static class Pair{
        private final String from;
        private final String to;

        public Pair(String from, String to){
            this.from = from;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }
    }

    public static class Field extends Pair {
        private final String converter;

        public Field(String from, String to, String converter){
            super(from, to);
            this.converter = converter;
        }

        public String getConverter() {
            return converter;
        }
    }

    public static class Mapping extends Pair {
        private final boolean oneWay;
        private final List<Field> fields;

        public Mapping(String from, String to, List<Field> fields, boolean oneWay) {
            super(from, to);
            this.fields = fields;
            this.oneWay = oneWay;
        }

        public List<Field> getFields() {
            return Collections.unmodifiableList(fields);
        }

        public boolean isOneWay() {
            return oneWay;
        }
    }





}
