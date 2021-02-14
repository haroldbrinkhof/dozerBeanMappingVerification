package be.catsandcoding.dozer;


import be.catsandcoding.dozer.components.CustomClassLoader;
import be.catsandcoding.dozer.components.XmlParser;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.dozer.DozerBeanMapper;
import org.dozer.DozerConverter;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Class.forName;

public class DozerMappingVerification extends Task {
    private String fullyQualifiedDozerClassName;
    private String mainConfig;
    private CustomClassLoader customClassLoader = null;
    private final List<String> paths = new ArrayList<>();
    private String classPath;

    public static void main(String... args){
        DozerMappingVerification dozerMappingVerification = new DozerMappingVerification();
        dozerMappingVerification.execute();
    }

    public void setFullyQualifiedDozerClassName(String fullyQualifiedDozerClassName) {
        this.fullyQualifiedDozerClassName = fullyQualifiedDozerClassName;
    }

    private String getFullyQualifiedDozerClassName(){
        if(this.fullyQualifiedDozerClassName == null) {
            throw new IllegalArgumentException("No class specified for DozerBeanMapper ");
        }
        return this.fullyQualifiedDozerClassName;
    }

    public String getClassPath() {
        return classPath == null ? "" : classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public String getMainConfig() {
        return mainConfig == null ? "" : mainConfig;
    }

    public void setMainConfig(String mainConfig) {
        this.mainConfig = mainConfig;
    }

    private CustomClassLoader getCustomClassLoader() {
        if(this.customClassLoader == null){
            this.customClassLoader = new CustomClassLoader(paths);
        }
        return this.customClassLoader;
    }

    private DozerBeanMapper loadDozerInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> dozerBeanMapper;
        try {
            dozerBeanMapper = Class.forName(getFullyQualifiedDozerClassName());
        } catch (ClassNotFoundException e){
            dozerBeanMapper = new CustomClassLoader(paths).loadClass(getFullyQualifiedDozerClassName())
                    .orElseThrow(() -> new IllegalStateException("Could not locate DozerBeanMapper class"));
        }
        System.out.println("dozer class found " + dozerBeanMapper.getCanonicalName());

        Object target = dozerBeanMapper.newInstance();
        return (DozerBeanMapper) target;
    }

    private void verifyMainConfig(String pathToXmlResource) throws ParserConfigurationException, SAXException, IOException {
        List<XmlParser.Field> fields = new XmlParser().parseMainConfig(pathToXmlResource);
        for(XmlParser.Field field: fields) {
            // verify all classes exist and they have the getters for the fields specified
            Class<?> classFrom = getCustomClassLoader().loadClass(field.getFrom())
                    .orElseThrow(() -> new BuildException(String.format("Problem with mapping%n%s [class-a]: %s could not be instantiated", pathToXmlResource, field.getFrom())));
            Class<?> classTo = getCustomClassLoader().loadClass(field.getTo())
                    .orElseThrow(() -> new BuildException(String.format("Problem with mapping%n%s [class-b]: %s could not be instantiated", pathToXmlResource, field.getTo())));
            verifyConverter(pathToXmlResource,field, classFrom, classTo, false);

        }
    }
    private void verifyServiceMappings(String pathToXmlResource) throws ParserConfigurationException, SAXException, IOException {
        // get the mappings
        List<XmlParser.Mapping> mappings = new XmlParser().parseServiceConfig(pathToXmlResource);
        for(XmlParser.Mapping mapping: mappings) {
            // verify all classes exist and they have the getters for the fields specified
            Class<?> classFrom = getCustomClassLoader().loadClass(mapping.getFrom())
                    .orElseThrow(() -> new BuildException(String.format("Problem with mapping%n%s [class-a]: %s could not be instantiated", pathToXmlResource, mapping.getFrom())));
            Class<?> classTo = getCustomClassLoader().loadClass(mapping.getTo())
                    .orElseThrow(() -> new BuildException(String.format("Problem with mapping%n%s [class-b]: %s could not be instantiated", pathToXmlResource, mapping.getTo())));
            for(XmlParser.Field field: mapping.getFields()) {
                verifyFieldAndConverter(pathToXmlResource, mapping, classFrom, classTo, field);
            }

        }
    }

    private void verifyFieldAndConverter(String pathToXmlResource, XmlParser.Mapping mapping, Class<?> classFrom, Class<?> classTo, XmlParser.Field field) {
        Field fromField = null;
        Field toField = null;

        try {
            fromField = classFrom.getDeclaredField(field.getFrom());
        } catch(NoSuchFieldException e) {
            throw new BuildException(String.format("Problem with mapping%n%s [class-a]: %s does not have the necessary field %s",
                    pathToXmlResource, mapping.getFrom(), field.getFrom()), e);
        }

        try {
            toField = classTo.getDeclaredField(field.getTo());
        } catch(NoSuchFieldException e) {
            throw new BuildException(String.format("Problem with mapping%n%s [class-b]: %s does not have the necessary field %s",
                    pathToXmlResource, mapping.getTo(), field.getTo()), e);
        }

        if(field.getConverter() == null) {
            return;
        }
        // verify custom converters exists if specified
        verifyConverter(pathToXmlResource, field, fromField.getType(), toField.getType(), mapping.isOneWay());
    }

    private void verifyConverter(String pathToXmlResource, XmlParser.Field field, Class<?> fromField, Class<?> toField, boolean oneWayConversion) {
        Class<?> classConverter = getCustomClassLoader().loadClass(field.getConverter())
                .orElseThrow(() -> new BuildException(String.format("Problem with mapping%n%s [custom-converter]: %s could not be instantiated",
                        pathToXmlResource, field.getConverter())));
        // make sure that we extend org.dozer.DozerConverter somewhere in the hierarchy
        if(!DozerConverter.class.isAssignableFrom(classConverter)){
            throw new BuildException(String.format("Problem with mapping%n%s [custom-converter]: %s is not an instance of org.dozer.DozerConverter",
                    pathToXmlResource, field.getConverter()));
        }
        try {
            // verify method exists and has the proper parameter
            Method converterMethod = classConverter.getDeclaredMethod("convertTo", fromField, toField);
            // make sure the return type is also corresponding
            if(!converterMethod.getReturnType().equals(toField)) {
                throw new NoSuchMethodException("return type incorrect");
            }
        } catch(NoSuchMethodException e) {
            throw new BuildException(String.format("Problem with mapping%n%s [custom-converter]: %s does not have the correct convertTo method",
                    pathToXmlResource, field.getConverter()), e);
        }
        // if the mapping is bi-directional swap parameter and return type
        if(!oneWayConversion) {
            try {
                Method converterMethod = classConverter.getDeclaredMethod("convertFrom", toField, fromField);
                if (!converterMethod.getReturnType().equals(fromField)) {
                    throw new NoSuchMethodException("return type incorrect");
                }
            } catch (NoSuchMethodException e) {
                throw new BuildException(String.format("Problem with mapping%n%s [custom-converter]: %s does not have the correct convertTo method",
                        pathToXmlResource, field.getConverter()), e);
            }
        }
    }

    @Override
    public void execute() throws BuildException {
        super.execute();
        if(!getClassPath().isEmpty()) {
            System.out.println("USING: " + getClassPath());
            paths.addAll(Arrays.asList(getClassPath().split(";")));
        }

        try {
            DozerBeanMapper dozerBeanMapper = loadDozerInstance();
            System.out.println("dozer loaded");
            for(String path: dozerBeanMapper.getMappingFiles()){
                verifyServiceMappings(path);
            }
            if(!getMainConfig().isEmpty()){
                verifyMainConfig(getMainConfig());
            }
        } catch(ParserConfigurationException | IOException | SAXException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new BuildException(e.getMessage(), e);
        }
    }
}


