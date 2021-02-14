package be.catsandcoding.dozer.mappings;

import org.dozer.DozerConverter;

public class WrongFromParameterConverter extends DozerConverter<Integer, Boolean> {
    public WrongFromParameterConverter(Class<Integer> prototypeA, Class<Boolean> prototypeB) {
        super(prototypeA, prototypeB);
    }

    @Override
    public Boolean convertTo(Integer number, Boolean aBool) {
        return null;
    }

    @Override
    public Integer convertFrom(Boolean aBool, Integer number) {
        return null;
    }
}
