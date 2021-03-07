package be.catsandcoding.dozer;

import be.catsandcoding.dozer.components.CustomClassLoader;
import be.catsandcoding.dozer.components.Verifier;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.dozer.DozerBeanMapper;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.Class;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DozerMappingVerification extends Task {

    private String fullyQualifiedDozerClassName;
    private final List<String> paths = new ArrayList<>();
    private String classPath;
    private String pathToLoadXmlFrom;
    private String pathToPreviouslyCheckedFileResults;
    private boolean caseInsensitiveFieldMatchingAllowed;
    private boolean ignoringErrorsAllowed;

    public static void main(String... args){
        DozerMappingVerification dozerMappingVerification = new DozerMappingVerification();
        dozerMappingVerification.execute();
    }

    public String getPathToLoadXmlFrom() {
        return pathToLoadXmlFrom;
    }

    public String getPathToPreviouslyCheckedFileResults() {
        return pathToPreviouslyCheckedFileResults;
    }

    public void setPathToPreviouslyCheckedFileResults(String pathToPreviouslyCheckedFiles) {
        this.pathToPreviouslyCheckedFileResults = pathToPreviouslyCheckedFiles;
    }

    public void setPathToLoadXmlFrom(String pathToLoadXmlFrom) {
        this.pathToLoadXmlFrom = pathToLoadXmlFrom;
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

    public boolean isCaseInsensitiveFieldMatchingAllowed() {
        return caseInsensitiveFieldMatchingAllowed;
    }

    public void setCaseInsensitiveFieldMatchingAllowed(boolean caseInsensitiveFieldMatchingAllowed) {
        this.caseInsensitiveFieldMatchingAllowed = caseInsensitiveFieldMatchingAllowed;
    }

    public boolean isIgnoringErrorsAllowed() {
        return ignoringErrorsAllowed;
    }

    public void setIgnoringErrorsAllowed(boolean ignoringErrorsAllowed) {
        this.ignoringErrorsAllowed = ignoringErrorsAllowed;
    }

    private DozerBeanMapper loadDozerInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> dozerBeanMapper;
        try {
            dozerBeanMapper = Class.forName(getFullyQualifiedDozerClassName());
        } catch (ClassNotFoundException e){
            dozerBeanMapper = new CustomClassLoader(paths).loadClass(getFullyQualifiedDozerClassName())
                    .orElseThrow(() -> new IllegalStateException("Could not locate DozerBeanMapper class"));
        }

        Object target = dozerBeanMapper.newInstance();
        return (DozerBeanMapper) target;
    }

    private void performVerification(){
        Options options = new Options()
                .withClassPathDirectories(Arrays.asList(getClassPath().split(";")))
                .withCaseInsensitiveFieldMatchingAllowed(isCaseInsensitiveFieldMatchingAllowed())
                .withIgnoringErrorsAllowed(isIgnoringErrorsAllowed())
                .withPathToProgressKeepingFile(getPathToPreviouslyCheckedFileResults());

        try {
            Verifier verifier = new Verifier(options);
            if(fullyQualifiedDozerClassName != null) {
                DozerBeanMapper dozerBeanMapper = loadDozerInstance();
                for (String path : dozerBeanMapper.getMappingFiles()) {
                    verifier.verify(path);
                }
            } else if (getPathToLoadXmlFrom() != null){
                Files.walkFileTree(Paths.get(pathToLoadXmlFrom), verifier);

            }

        } catch(ParserConfigurationException | URISyntaxException | SAXException | IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | JAXBException | NoSuchAlgorithmException e) {
            if(isIgnoringErrorsAllowed()){
                System.out.println(e.getMessage());
            } else {
                throw new BuildException(e.getMessage(), e);
            }
        }

    }

    @Override
    public void execute() throws BuildException {
        super.execute();

        performVerification();
    }
}


