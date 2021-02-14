package be.catsandcoding.dozer.mappings;

import org.dozer.DozerConverter;

public class MazzeltjeToSuccessConvertor extends DozerConverter<Integer, Boolean> {

    public MazzeltjeToSuccessConvertor(Class<Integer> prototypeA, Class<Boolean> prototypeB) {
        super(prototypeA, prototypeB);
    }

    @Override
    public Boolean convertTo(Integer mazzeltje, Boolean success) {
         return mazzeltje > 0;
    }

    @Override
    public Integer convertFrom(Boolean success, Integer mazzeltje) {
        return success?100:0;
    }
}
