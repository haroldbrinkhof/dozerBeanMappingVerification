package be.catsandcoding.dozer.mappings;

import org.dozer.DozerConverter;

public class WrongToParameterConverter extends DozerConverter<Boolean, Integer> {
    public WrongToParameterConverter(Class<Boolean> prototypeA, Class<Integer> prototypeB) {
        super(prototypeA, prototypeB);
    }

    @Override
    public Integer convertTo(Boolean aBoolean, Integer integer) {
        return null;
    }

    @Override
    public Boolean convertFrom(Integer integer, Boolean aBoolean) {
        return null;
    }
}
