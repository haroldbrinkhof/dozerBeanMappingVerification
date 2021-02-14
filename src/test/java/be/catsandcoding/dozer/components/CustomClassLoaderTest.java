package be.catsandcoding.dozer.components;

import be.catsandcoding.dozer.mapper.SuccessDozerBeanMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class CustomClassLoaderTest {
    @Test
    public void classLoader_classExists_returnsOptional(){
        String CLASS_NAME = "be.catsandcoding.dozer.mapper.SuccessDozerBeanMapper";
        CustomClassLoader customClassLoader = new CustomClassLoader("be/catsandcoding/dozer");
        Optional<Class<?>> actual = customClassLoader.loadClass(CLASS_NAME);
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals(CLASS_NAME, actual.get().getCanonicalName());
        Assertions.assertTrue(actual.get().isInstance(new SuccessDozerBeanMapper()));
    }

    @Test
    public void classLoader_classDoesNotExist_returnsEmpty(){
        String CLASS_NAME = "be.catsandcoding.dozer.mapper.NopeNopeNope";
        CustomClassLoader customClassLoader = new CustomClassLoader("be/catsandcoding/dozer");
        Optional<Class<?>> actual = customClassLoader.loadClass(CLASS_NAME);
        Assertions.assertFalse(actual.isPresent());
    }
}
