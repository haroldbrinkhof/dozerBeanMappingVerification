package be.catsandcoding.dozer;

import org.dozer.DozerConverter;

public class CustomConverter extends DozerConverter<String, Boolean> {

    public CustomConverter(Class<String> prototypeA, Class<Boolean> prototypeB) {
        super(prototypeA, prototypeB);
    }

    @Override
    public Boolean convertTo(String s, Boolean xmlParser) {
        return xmlParser;
    }

    @Override
    public String convertFrom(Boolean xmlParser, String s) {
        return s;
    }
}
