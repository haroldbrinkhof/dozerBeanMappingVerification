package be.catsandcoding.dozer;

import be.catsandcoding.dozer.components.XmlParser;
import org.dozer.DozerConverter;

public class CustomConverter extends DozerConverter<String, XmlParser> {

    public CustomConverter(Class<String> prototypeA, Class<XmlParser> prototypeB) {
        super(prototypeA, prototypeB);
    }

    @Override
    public XmlParser convertTo(String s, XmlParser xmlParser) {
        return xmlParser;
    }

    @Override
    public String convertFrom(XmlParser xmlParser, String s) {
        return s;
    }
}
