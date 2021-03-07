package be.catsandcoding.dozer.components;

import be.catsandcoding.dozer.generated.*;
import org.apache.tools.ant.BuildException;

import javax.xml.bind.annotation.XmlElement;
import java.lang.Class;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VerificationMethods {
    private final CustomClassLoader customClassLoader;
    private final boolean allowCaseInsensitiveFields;
    private final boolean ignoringErrorsAllowed;

    public VerificationMethods(CustomClassLoader customClassLoader, boolean allowCaseInsensitiveFields,
                               boolean ignoringErrorsAllowed){
        this.customClassLoader = customClassLoader;
        this.allowCaseInsensitiveFields = allowCaseInsensitiveFields;
        this.ignoringErrorsAllowed = ignoringErrorsAllowed;
    }

    public CustomClassLoader getCustomClassLoader() {
        return customClassLoader;
    }

    public void verifyConfiguration(Configuration configuration, String pathToXmlResource){
        if(configuration == null || configuration.getCustomConverters() == null) { return; }

        for(ConverterType converter: configuration.getCustomConverters().getConverter()){
            assureConverterCanBeLoaded(converter.getClassA().getContent(),"class-a", pathToXmlResource);
            assureConverterCanBeLoaded(converter.getClassB().getContent(),"class-b", pathToXmlResource);

            Class<?> classConverter = assureConverterCanBeLoaded(converter.getType(), "custom-converter", pathToXmlResource);
            // make sure that we extend org.dozer.DozerConverter somewhere in the hierarchy
            List<String> methodNames = Arrays.stream(classConverter.getMethods()).map(Method::getName).collect(Collectors.toList());
            if(!(methodNames.contains("convertTo") && methodNames.contains("convertFrom"))){
                String msg = String.format("Problem with mapping%n%s [custom-converter]: %s is not an instance of org.dozer.DozerConverter",
                        pathToXmlResource, converter.getType());
                if(ignoringErrorsAllowed){
                    System.out.println(msg);
                } else {
                    throw new BuildException(msg);
                }
            }

        }
    }
    private Class<?> assureConverterCanBeLoaded(String className, String attribute, String pathToXmlResource){
        return getCustomClassLoader().loadClass(className.trim())
                .orElseThrow(() -> new BuildException(String.format("Problem with mapping%n%s [%s]: %s could not be instantiated", pathToXmlResource, attribute, className)));
    }

    public void verifyMappings(List<Mapping> mappings, String pathToXmlResource){
        if(mappings == null) { return; }

        // verify mappings
        for(Mapping mapping: mappings) {
            // verify all classes exist and they have the getters for the fields specified
            Class<?> classFrom = assureConverterCanBeLoaded(mapping.getClassA().getContent(), "class-a", pathToXmlResource);
            Class<?> classTo = assureConverterCanBeLoaded(mapping.getClassB().getContent(), "class-b", pathToXmlResource);

            // verify fields
            List<Field> fields = mapping.getFieldOrFieldExclude().stream().filter(f -> f instanceof Field)
                    .map(f -> (Field) f).collect(Collectors.toList());
            for(Field field: fields){


                FieldAccessor fromField;
                FieldAccessor toField;
                fromField = handleField(field.getA(), classFrom, pathToXmlResource,"a");
                toField = handleField(field.getB(), classTo, pathToXmlResource, "b");

                // verify custom-converters on field
                if(fromField != null && toField != null) {
                    verifyFieldConverter(field, pathToXmlResource, fromField, toField);
                }
            }

        }
    }

    private static final class FieldAccessor{
        private final Optional<Method> getProperty;
        private final Optional<java.lang.reflect.Field> field;

        public FieldAccessor(java.lang.reflect.Field field, Method getProperty){
            this.field = Optional.ofNullable(field);
            this.getProperty = Optional.ofNullable(getProperty);
        }

        public Optional<Method> getPropertyMethod() {
            return getProperty;
        }

        public Optional<java.lang.reflect.Field> getField() {
            return field;
        }
    }

    private FieldAccessor handleField(FieldDefinition fieldDefinition, Class<?> clazz, String pathToXmlResource, String indicator){
        try {
            return getField(clazz, fieldDefinition.getContent());
        } catch(NoSuchFieldException e) {
            String msg = String.format("Problem with mapping%n%s [class-%s]: %s does not have the necessary field %s",
                    pathToXmlResource, indicator, clazz.getCanonicalName(), fieldDefinition.getContent());
            if(ignoringErrorsAllowed){
                System.out.println(msg);
                return null;
            } else {
                throw new BuildException(msg, e);
            }
        }

    }

    private FieldAccessor getField(Class<?> clazz, String name) throws NoSuchFieldException {
        String[] items = name.trim().split("\\.");
        String propertyName = items[0].trim();
        List<java.lang.reflect.Field> fields;
        java.lang.reflect.Field xmlAttachedField = null;
        Optional<java.lang.reflect.Field> anyMatch;
        Optional<Method> setterMatch = Optional.empty();
        Optional<Method> getterMatch = Optional.empty();

        do{
            fields = Arrays.asList(clazz.getDeclaredFields());
            List<java.lang.reflect.Field> xmlPresent = fields.stream().filter(f -> f.isAnnotationPresent(XmlElement.class)).collect(Collectors.toList());
            for(java.lang.reflect.Field xmlField: xmlPresent){
                XmlElement xmlElement = xmlField.getDeclaredAnnotation(XmlElement.class);
                if(name.equals(xmlElement.name())) {
                    xmlAttachedField = xmlField;
                }
            }

            anyMatch = fields.stream().filter(f -> allowCaseInsensitiveFields ?
                    f.getName().equalsIgnoreCase(propertyName) :
                    f.getName().equals(propertyName))
                    .findFirst();
            if(!anyMatch.isPresent()){
                setterMatch = setPropertyMethod(clazz, propertyName);
                getterMatch = getPropertyMethod(clazz, propertyName);
            }

        } while (xmlAttachedField == null && !(anyMatch.isPresent() || setterMatch.isPresent()) && !(clazz = clazz.getSuperclass()).equals(Object.class)  );

        AccessibleObject field = xmlAttachedField != null ? xmlAttachedField : (anyMatch.isPresent() ? anyMatch.get() : setterMatch
                .orElseThrow(NoSuchFieldException::new));  //xmlAttachedField : clazz.getDeclaredField(items[0].trim());

        java.lang.reflect.Field toReturn = xmlAttachedField != null ? xmlAttachedField : anyMatch.orElse(null);

        return items.length == 1 ? new FieldAccessor(toReturn, getterMatch.orElse(null))  :
                field instanceof java.lang.reflect.Field ?
                        getField(((java.lang.reflect.Field) field).getType(),String.join(".", Arrays.copyOfRange(items, 1, items.length))) :
                        getterMatch.isPresent() ? getField(getterMatch.get().getReturnType(), String.join(".",Arrays.copyOfRange(items, 1, items.length) )) :
                                getField(((Method) field).getParameterTypes()[0], String.join(".",Arrays.copyOfRange(items, 1, items.length) ));

    }

    private Optional<Method> setPropertyMethod(Class<?> clazz, String name) {
        return retrievePropertyMethod(clazz, m -> m.getName().equals(constructSetterName(name)));
    }
    private Optional<Method> getPropertyMethod(Class<?> clazz, String name) {
        return retrievePropertyMethod(clazz, m -> m.getName().equals(constructGetterName(name)) || m.getName().equals(constructGetterNameBoolean(name)));
    }
    private Optional<Method> retrievePropertyMethod(Class<?> clazz, Predicate<Method> filterToUse){
        Optional<Method> found;
        do {
            List<Method> methods = Arrays.asList(clazz.getMethods());
            found = methods.stream()
                    .filter(filterToUse)
                    .findFirst();
        } while (!found.isPresent() && (clazz = clazz.getSuperclass()) != Object.class);
        return found;
    }

    private String constructSetterName(String name){
        return "set" + name.substring(0,1).toUpperCase() + name.substring(1);
    }
    private String constructGetterName(String name){
        return "get" + name.substring(0,1).toUpperCase() + name.substring(1);
    }
    private String constructGetterNameBoolean(String name){
        return "is" + name.substring(0,1).toUpperCase() + name.substring(1);
    }


    private void verifyFieldConverter(Field field, String pathToXmlResource, FieldAccessor fromAccessor, FieldAccessor toAccessor) {
        if(field.getCustomConverter() == null) { return; }

        Class<?> classConverter = getCustomClassLoader().loadClass(field.getCustomConverter())
                .orElseThrow(() -> new BuildException(String.format("Problem with mapping%n%s [custom-converter]: %s could not be instantiated",
                        pathToXmlResource, field.getCustomConverter())));

        // make sure that we extend org.dozer.DozerConverter somewhere in the hierarchy
        List<String> methodNames = Arrays.stream(classConverter.getMethods()).map(Method::getName).collect(Collectors.toList());
        if(!(methodNames.contains("convertTo") && methodNames.contains("convertFrom"))){
            String msg = String.format("Problem with mapping%n%s [custom-converter]: %s is not an instance of org.dozer.DozerConverter",
                    pathToXmlResource, field.getCustomConverter());
           if(ignoringErrorsAllowed){
               System.out.println(msg);
           } else {
               throw new BuildException(msg);
           }
        }

        Class<?> fromField = fromAccessor.getField().isPresent() ? fromAccessor.getField().get().getType() : (fromAccessor.getPropertyMethod().orElseThrow(() -> new BuildException("Necessary field not found and getter method is missing"))).getReturnType() ;
        Class<?> toField = toAccessor.getField().isPresent() ? toAccessor.getField().get().getType() : (toAccessor.getPropertyMethod().orElseThrow(() -> new BuildException("Necessary field not found and getter method is missing"))).getReturnType() ;

        // verify method exists and has the proper parameter
        if(Arrays.stream(classConverter.getMethods()).filter(c -> c.getName().equals("convertTo"))
                .noneMatch(m -> m.getParameterCount() == 2 &&
                        (m.getParameterTypes()[0].getCanonicalName().equals(fromField.getCanonicalName()) &&
                                m.getParameterTypes()[1].getCanonicalName().equals(toField.getCanonicalName()) &&
                                m.getReturnType().getCanonicalName().equals(toField.getCanonicalName())) ||
                        (m.getParameterTypes()[0].isAssignableFrom(fromField) &&
                                m.getParameterTypes()[1].isAssignableFrom(toField) &&
                                m.getReturnType().isAssignableFrom(toField))
                )){
            String msg = String.format("Problem with mapping%n%s [custom-converter]: %s does not have the correct convertTo method",
                    pathToXmlResource, field.getCustomConverter());
            if(ignoringErrorsAllowed){
                System.out.println(msg);
            } else {
                throw new BuildException(msg);
            }
        }
        // if the mapping is bi-directional swap parameter and return type
        if(Type.BI_DIRECTIONAL.equals(field.getType())) {
            try {
                Method converterMethod = classConverter.getDeclaredMethod("convertFrom", toField, fromField);
                if (!converterMethod.getReturnType().equals(fromField)) {
                    throw new NoSuchMethodException("return type incorrect");
                }
            } catch (NoSuchMethodException e) {
                String msg = String.format("Problem with mapping%n%s [custom-converter]: %s does not have the correct convertTo method",
                        pathToXmlResource, field.getCustomConverter());
                if(ignoringErrorsAllowed){
                    System.out.println(msg);
                } else {
                    throw new BuildException(msg, e);
                }
            }
        }

    }

}
