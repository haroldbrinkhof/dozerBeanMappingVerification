package be.catsandcoding.dozer;

import org.apache.tools.ant.BuildException;
import org.dozer.DozerBeanMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;



public class DozerMappingVerificationTest {
    private Method loadDozerInstance;
    private Method verifyServiceMappings;
    private Method verifyMainConfig;
    private final DozerMappingVerification dozerMappingVerification = new DozerMappingVerification();

    @BeforeEach
    public void setup() throws NoSuchMethodException {
        loadDozerInstance = dozerMappingVerification.getClass().getDeclaredMethod("loadDozerInstance");
        loadDozerInstance.setAccessible(true);
        verifyServiceMappings = dozerMappingVerification.getClass().getDeclaredMethod("verifyServiceMappings", String.class);
        verifyServiceMappings.setAccessible(true);
        verifyMainConfig = dozerMappingVerification.getClass().getDeclaredMethod("verifyMainConfig", String.class);
        verifyMainConfig.setAccessible(true);
    }

    @Test
    public void loadDozerInstance_success_returnsDozerBeanMapper() throws InvocationTargetException, IllegalAccessException {
        dozerMappingVerification.setFullyQualifiedDozerClassName("be.catsandcoding.dozer.mapper.SuccessDozerBeanMapper");
        Assertions.assertNotNull(loadDozerInstance.invoke(dozerMappingVerification));
        Assertions.assertTrue(loadDozerInstance.invoke(dozerMappingVerification) instanceof DozerBeanMapper);
    }

    @Test
    public void loadDozerInstance_failureToFindMapper_throwsIllegalStateException() {
        Exception actual = Assertions.assertThrows(InvocationTargetException.class , () -> {
            dozerMappingVerification.setFullyQualifiedDozerClassName("be.catsandcoding.dozer.mapping.SuccessDozerBeanMapper");
            loadDozerInstance.invoke(dozerMappingVerification);
        });
        Assertions.assertEquals(IllegalStateException.class, actual.getCause().getClass());
        Assertions.assertEquals("Could not locate DozerBeanMapper class", actual.getCause().getMessage());
    }

    @Test
    public void loadDozerInstance_notDozerBeanMapper_throwsClassCastException() {
        Exception actual = Assertions.assertThrows(InvocationTargetException.class , () -> {
            dozerMappingVerification.setFullyQualifiedDozerClassName("java.lang.String");
            loadDozerInstance.invoke(dozerMappingVerification);
        });
        Assertions.assertEquals(ClassCastException.class, actual.getCause().getClass());
    }

    @Test
    public void verifyServiceMappings_happyPath() throws InvocationTargetException, IllegalAccessException {
            verifyServiceMappings.invoke(dozerMappingVerification, "be/catsandcoding/dozer/mappings/SuccessMapping.xml");
    }

    @Test
    public void verifyMainConfig_happyPath() throws InvocationTargetException, IllegalAccessException {
        verifyMainConfig.invoke(dozerMappingVerification, "be/catsandcoding/dozer/mappings/MainConfig.xml");
    }

    @Test
    public void verifyServiceMappings_failure_classA_notFound() throws IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/FailureMappingClassANotFound.xml";
        String notFound = "be.catsandcoding.dozer.mappings.NotFound";
        try {
            verifyServiceMappings.invoke(dozerMappingVerification, path);
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [class-a]: %s could not be instantiated", path, notFound), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_classB_notFound() throws IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/FailureMappingClassBNotFound.xml";
        String notFound = "be.catsandcoding.dozer.mappings.NotFound";
        try {
            verifyServiceMappings.invoke(dozerMappingVerification, path);
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [class-b]: %s could not be instantiated", path, notFound), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_fieldA_notFound() throws IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/FailureMappingFieldANotFound.xml";
        String notFound = "be.catsandcoding.dozer.mappings.PreSuccess";
        String field = "notFound";
        try {
            verifyServiceMappings.invoke(dozerMappingVerification, path);
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [class-a]: %s does not have the necessary field %s", path, notFound, field), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_fieldB_notFound() throws IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/FailureMappingFieldBNotFound.xml";
        String notFound = "be.catsandcoding.dozer.mappings.Success";
        String field = "notFound";
        try {
            verifyServiceMappings.invoke(dozerMappingVerification, path);
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [class-b]: %s does not have the necessary field %s", path, notFound, field), e.getCause().getMessage());
        }
    }


    @Test
    public void verifyServiceMappings_failure_converter_notFound() throws IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/FailureMappingConverterNotFound.xml";
        String notFound = "be.catsandcoding.dozer.NotFound";

        try {
            verifyServiceMappings.invoke(dozerMappingVerification, path);
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [custom-converter]: %s could not be instantiated", path, notFound), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_converter_doesNotInheritFromDozerConverter() throws IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/FailureMappingConverterNotDozerConverter.xml";
        String notFound = "java.lang.String";

        try {
            verifyServiceMappings.invoke(dozerMappingVerification, path);
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [custom-converter]: %s is not an instance of org.dozer.DozerConverter", path, notFound), e.getCause().getMessage());
        }
    }


    @Test
    public void verifyServiceMappings_failure_converter_wrongFromType() throws IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/FailureMappingConverterWrongFromType.xml";
        String notFound = "be.catsandcoding.dozer.mappings.WrongFromParameterConverter";

        try {
            verifyServiceMappings.invoke(dozerMappingVerification, path);
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [custom-converter]: %s does not have the correct convertTo method", path, notFound), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_converter_wrongToType() throws IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/FailureMappingConverterWrongToType.xml";
        String notFound = "be.catsandcoding.dozer.mappings.WrongToParameterConverter";

        try {
            verifyServiceMappings.invoke(dozerMappingVerification, path);
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [custom-converter]: %s does not have the correct convertTo method", path, notFound), e.getCause().getMessage());
        }
    }
}
