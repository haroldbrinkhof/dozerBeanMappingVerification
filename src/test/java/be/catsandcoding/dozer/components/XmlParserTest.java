package be.catsandcoding.dozer.components;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public class XmlParserTest {


    @Test
    public void getMappings_happyPath() throws ParserConfigurationException, SAXException, IOException {
        List<XmlParser.Mapping> actual = new XmlParser().parseServiceConfig("be/catsandcoding/dozer/mappings/SuccessMapping.xml");

        Assertions.assertEquals(2, actual.size());
        Assertions.assertEquals("be.catsandcoding.dozer.mappings.PreSuccess", actual.get(0).getFrom());
        Assertions.assertEquals("be.catsandcoding.dozer.mappings.Success", actual.get(0).getTo());


        Assertions.assertEquals(1, actual.get(0).getFields().size());
        Assertions.assertTrue(actual.get(0).isOneWay());
        Assertions.assertEquals("customField", actual.get(0).getFields().get(0).getFrom());
        Assertions.assertEquals("success", actual.get(0).getFields().get(0).getTo());


        Assertions.assertEquals(2, actual.get(1).getFields().size());
        Assertions.assertFalse(actual.get(1).isOneWay());
        Assertions.assertEquals("littleLuck", actual.get(1).getFields().get(0).getFrom());
        Assertions.assertEquals("success", actual.get(1).getFields().get(0).getTo());
        Assertions.assertEquals("be.catsandcoding.dozer.mappings.MazzeltjeToSuccessConvertor", actual.get(1).getFields().get(0).getConverter());

        Assertions.assertEquals("failure", actual.get(1).getFields().get(1).getFrom());
        Assertions.assertEquals("failure", actual.get(1).getFields().get(1).getTo());
        Assertions.assertNull(actual.get(1).getFields().get(1).getConverter());
    }

    @Test
    public void getMainConfig_happyPath() throws ParserConfigurationException, SAXException, IOException {
        String path = "be/catsandcoding/dozer/mappings/MainConfig.xml";

        List<XmlParser.Field> actual = new XmlParser().parseMainConfig(path);
        Assertions.assertEquals(1, actual.size());
        Assertions.assertEquals("be.catsandcoding.dozer.CustomConverter", actual.get(0).getConverter());
        Assertions.assertEquals("java.lang.String", actual.get(0).getFrom());
        Assertions.assertEquals("be.catsandcoding.dozer.components.XmlParser", actual.get(0).getTo());

    }
}
