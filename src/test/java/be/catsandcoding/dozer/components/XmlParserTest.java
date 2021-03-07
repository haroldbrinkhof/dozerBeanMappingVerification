package be.catsandcoding.dozer.components;

import be.catsandcoding.dozer.Options;
import be.catsandcoding.dozer.generated.ConverterType;
import be.catsandcoding.dozer.generated.Field;
import be.catsandcoding.dozer.generated.Mappings;
import be.catsandcoding.dozer.generated.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class XmlParserTest {

    private static Method getMappingsForFile;
    private static Verifier verifier;
    @BeforeAll
    public static void setup() throws NoSuchMethodException, JAXBException {
        verifier = new Verifier(new Options());
        getMappingsForFile = Verifier.class.getDeclaredMethod("getMappingsForFile",String.class);
        getMappingsForFile.setAccessible(true);
    }

    private String getAbsolutePath(String path) throws URISyntaxException {
        return Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(path)).toURI().toString();
    }

    @Test
    public void getMappings_happyPath() throws JAXBException, URISyntaxException, InvocationTargetException, IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/SuccessMapping.xml";
        Mappings actual = (Mappings)getMappingsForFile.invoke(verifier,getAbsolutePath(path));

        Assertions.assertEquals(2, actual.getMapping().size());
        Assertions.assertEquals("be.catsandcoding.dozer.mappings.PreSuccess", actual.getMapping().get(0).getClassA().getContent());
        Assertions.assertEquals("be.catsandcoding.dozer.mappings.Success", actual.getMapping().get(0).getClassB().getContent());


        Assertions.assertEquals(Type.ONE_WAY, actual.getMapping().get(0).getType());
        List<Field> fields = actual.getMapping().get(0).getFieldOrFieldExclude().stream()
                .filter(field -> field instanceof Field).map(f -> (Field)f).collect(Collectors.toList());
        Assertions.assertEquals("customField", fields.get(0).getA().getContent());
        Assertions.assertEquals("success", fields.get(0).getB().getContent());


        fields = actual.getMapping().get(1).getFieldOrFieldExclude().stream()
                .filter(f -> f instanceof Field).map(f -> (Field)f).collect(Collectors.toList());
        Assertions.assertEquals(Type.ONE_WAY, actual.getMapping().get(0).getType());
        Assertions.assertEquals("littleLuck", fields.get(0).getA().getContent());
        Assertions.assertEquals("success", fields.get(0).getB().getContent());
        Assertions.assertEquals("be.catsandcoding.dozer.mappings.MazzeltjeToSuccessConvertor", fields.get(0).getCustomConverter());

        Assertions.assertEquals("failure", fields.get(1).getA().getContent());
        Assertions.assertEquals("failure", fields.get(1).getB().getContent());
        Assertions.assertNull(fields.get(1).getCustomConverter());
    }

    @Test
    public void getMainConfig_happyPath() throws URISyntaxException, InvocationTargetException, IllegalAccessException {
        String path = "be/catsandcoding/dozer/mappings/MainConfig.xml";

        Mappings actual = (Mappings)getMappingsForFile.invoke(verifier, getAbsolutePath(path));
        Assertions.assertEquals(1, actual.getConfiguration().getCustomConverters().getConverter().size());
        List<ConverterType> converters = actual.getConfiguration().getCustomConverters().getConverter();

        Assertions.assertEquals("be.catsandcoding.dozer.CustomConverter", converters.get(0).getType());
        Assertions.assertEquals("java.lang.String", converters.get(0).getClassA().getContent());
        Assertions.assertEquals("java.lang.Boolean", converters.get(0).getClassB().getContent());

    }
}
