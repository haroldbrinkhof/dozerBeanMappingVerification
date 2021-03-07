package be.catsandcoding.dozer;

import be.catsandcoding.dozer.components.Verifier;
import org.apache.tools.ant.BuildException;
import org.dozer.DozerBeanMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Objects;


public class DozerMappingVerificationTest {
    private Method loadDozerInstance;
    private Method verify;
    private final Verifier verifier = new Verifier(new Options());
    private final DozerMappingVerification dozerMappingVerification = new DozerMappingVerification();

    public DozerMappingVerificationTest() throws JAXBException {
    }

    private String getAbsolutePath(String path) throws URISyntaxException {
        return Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(path)).toURI().toString();
    }

    @BeforeEach
    public void setup() throws NoSuchMethodException {
        loadDozerInstance = dozerMappingVerification.getClass().getDeclaredMethod("loadDozerInstance");
        loadDozerInstance.setAccessible(true);
        verify = verifier.getClass().getDeclaredMethod("verify", String.class);
        verify.setAccessible(true);
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
    public void verifyServiceMappings_happyPath() throws InvocationTargetException, IllegalAccessException, URISyntaxException {
            verify.invoke(verifier, getAbsolutePath("be/catsandcoding/dozer/mappings/SuccessMapping.xml"));
    }

    @Test
    public void verifyMainConfig_happyPath() throws InvocationTargetException, IllegalAccessException, URISyntaxException {
        verify.invoke(verifier, getAbsolutePath("be/catsandcoding/dozer/mappings/MainConfig.xml"));
    }

    @Test
    public void verifyServiceMappings_failure_classA_notFound() throws IllegalAccessException, URISyntaxException {
        String path = getAbsolutePath("be/catsandcoding/dozer/mappings/FailureMappingClassANotFound.xml");
        String notFound = "be.catsandcoding.dozer.mappings.NotFound";
        try {
            verify.invoke(verifier, path);
            Assertions.fail("expected InvocationTargetException");
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [class-a]: %s could not be instantiated", path, notFound), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_classB_notFound() throws IllegalAccessException, URISyntaxException {
        String path = getAbsolutePath("be/catsandcoding/dozer/mappings/FailureMappingClassBNotFound.xml");
        String notFound = "be.catsandcoding.dozer.mappings.NotFound";
        try {
            verify.invoke(verifier, path);
            Assertions.fail("expected InvocationTargetException");
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [class-b]: %s could not be instantiated", path, notFound), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_fieldA_notFound() throws IllegalAccessException, URISyntaxException {
        String path = getAbsolutePath("be/catsandcoding/dozer/mappings/FailureMappingFieldANotFound.xml");
        String notFound = "be.catsandcoding.dozer.mappings.PreSuccess";
        String field = "notFound";
        try {
            verify.invoke(verifier, path);
            Assertions.fail("expected InvocationTargetException");
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [class-a]: %s does not have the necessary field %s", path, notFound, field), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_fieldB_notFound() throws IllegalAccessException, URISyntaxException {
        String path = getAbsolutePath("be/catsandcoding/dozer/mappings/FailureMappingFieldBNotFound.xml");
        String notFound = "be.catsandcoding.dozer.mappings.Success";
        String field = "notFound";
        try {
            verify.invoke(verifier, path);
            Assertions.fail("expected InvocationTargetException");
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [class-b]: %s does not have the necessary field %s", path, notFound, field), e.getCause().getMessage());
        }
    }


    @Test
    public void verifyServiceMappings_failure_converter_notFound() throws IllegalAccessException, URISyntaxException {
        String path = getAbsolutePath("be/catsandcoding/dozer/mappings/FailureMappingConverterNotFound.xml");
        String notFound = "be.catsandcoding.dozer.NotFound";

        try {
            verify.invoke(verifier, path);
            Assertions.fail("expected InvocationTargetException");
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [custom-converter]: %s could not be instantiated", path, notFound), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_converter_doesNotInheritFromDozerConverter() throws IllegalAccessException, URISyntaxException {
        String path = getAbsolutePath("be/catsandcoding/dozer/mappings/FailureMappingConverterNotDozerConverter.xml");
        String notFound = "java.lang.String";

        try {
            verify.invoke(verifier, path);
            Assertions.fail("expected InvocationTargetException");
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [custom-converter]: %s is not an instance of org.dozer.DozerConverter", path, notFound), e.getCause().getMessage());
        }
    }


    @Test
    public void verifyServiceMappings_failure_converter_wrongFromType() throws IllegalAccessException, URISyntaxException {
        String path = getAbsolutePath("be/catsandcoding/dozer/mappings/FailureMappingConverterWrongFromType.xml");
        String notFound = "be.catsandcoding.dozer.mappings.WrongFromParameterConverter";

        try {
            verify.invoke(verifier, path);
            Assertions.fail("expected InvocationTargetException");
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [custom-converter]: %s does not have the correct convertTo method", path, notFound), e.getCause().getMessage());
        }
    }

    @Test
    public void verifyServiceMappings_failure_converter_wrongToType() throws IllegalAccessException, URISyntaxException {
        String path = getAbsolutePath("be/catsandcoding/dozer/mappings/FailureMappingConverterWrongToType.xml");
        String notFound = "be.catsandcoding.dozer.mappings.WrongToParameterConverter";

        try {
            verify.invoke(verifier, path);
            Assertions.fail("expected InvocationTargetException");
        } catch (InvocationTargetException e){
            Assertions.assertTrue(e.getCause() instanceof BuildException);
            Assertions.assertEquals(String.format("Problem with mapping%n%s [custom-converter]: %s does not have the correct convertTo method", path, notFound), e.getCause().getMessage());
        }
    }
}
