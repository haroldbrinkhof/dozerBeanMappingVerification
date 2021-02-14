package be.catsandcoding.dozer.mapper;

import org.dozer.DozerBeanMapper;

import java.util.Collections;

public class SuccessDozerBeanMapper extends DozerBeanMapper{
    public SuccessDozerBeanMapper(){
        this.setMappingFiles(Collections.singletonList("be/catsandcoding/dozer/mappings/SuccessMapping.xml"));
    }
}
