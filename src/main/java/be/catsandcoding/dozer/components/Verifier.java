package be.catsandcoding.dozer.components;

import be.catsandcoding.dozer.Options;
import be.catsandcoding.dozer.generated.*;
import org.apache.tools.ant.BuildException;
import org.xml.sax.SAXException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class Verifier implements FileVisitor<Path> {
    private CustomClassLoader customClassLoader = null;

    private final JAXBContext jc = JAXBContext.newInstance(Mappings.class);
    private final Unmarshaller unmarshaller = jc.createUnmarshaller();
    private final HashMap<String, String> processed;
    private final Options options;


    public Verifier(Options options) throws JAXBException {
        this.options = options;
        processed = loadPreviouslyProcessedFiles(options.getPathToProgressKeepingFile().orElse(null));
    }

    private CustomClassLoader getCustomClassLoader() {
        if(this.customClassLoader == null){
            this.customClassLoader = new CustomClassLoader(options.getClassPathDirectories());
        }
        return this.customClassLoader;
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, String> loadPreviouslyProcessedFiles(String path){
        HashMap<String, String> found = new HashMap<>();
        if(path != null) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(path))) {
                found = (HashMap<String, String>) in.readObject();
            } catch (IOException | ClassNotFoundException ignored) {
                System.out.printf("DozerMappingVerification: could not load progress-file at %s.%n", path);
            }
        }
        return found;
    }

    private void savePreviouslyProcessedFiles(){
        options.getPathToProgressKeepingFile().ifPresent( file -> {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                out.writeObject(processed);
            } catch (IOException ignored) {
                System.out.println("ATTENTION: COULD NOT SAVE PROGRESS FILE CHECKING WILL BE SLOWER NEXT TIME! (check path settings in .pom)%n");
            }
        });
    }

    public void verify(String pathToXmlResource) throws ParserConfigurationException, SAXException, IOException, JAXBException, URISyntaxException, NoSuchAlgorithmException {
        Mappings mappings = getMappingsForFile(pathToXmlResource);
        String checkSum = getChecksum(mappings);
        if(processed.containsKey(pathToXmlResource) && processed.get(pathToXmlResource).equals(checkSum)){
            System.out.printf("DozerMappingVerification: no changes to %s since last check, skipping.%n", pathToXmlResource);
            return;
        }

        VerificationMethods methods = new VerificationMethods(getCustomClassLoader(), options.isCaseInsensitiveFieldMatchingAllowed(), options.isIgnoringErrorsAllowed());
        methods.verifyMappings(mappings.getMapping(), pathToXmlResource);
        methods.verifyConfiguration(mappings.getConfiguration(), pathToXmlResource);
        processed.put(pathToXmlResource, checkSum);
        savePreviouslyProcessedFiles();
        System.out.printf("DozerMappingVerification: %s successfully verified.%n", pathToXmlResource);
    }

    public static String getChecksum(Serializable object) throws IOException, NoSuchAlgorithmException {
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(object);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(baos.toByteArray());
            return DatatypeConverter.printHexBinary(digest);
        }
    }

    public Mappings getMappingsForFile(String path) throws URISyntaxException, JAXBException {
        URI file = new URI(path);
        return (Mappings) unmarshaller.unmarshal(new File(file));
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if(hasXmlExtension(file)){
            try {
                verify(file.toUri().toString());
            } catch(ParserConfigurationException | IOException | SAXException | NoSuchAlgorithmException e) {
                if(options.isIgnoringErrorsAllowed()) {
                    System.out.println(e.getMessage());
                } else {
                    throw new BuildException(e.getMessage(), e);
                }
            } catch (URISyntaxException | JAXBException ignored) {
                // URI should be fine for proper files and we only care for xml files that correspond to the dozer .xsd
            }
        }
        return FileVisitResult.CONTINUE;
    }
    private boolean hasXmlExtension(Path file){
        return file != null && file.getFileName().toString().toLowerCase().endsWith(".xml");
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        if(exc == null){ return FileVisitResult.CONTINUE; }

        throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if(exc == null){ return FileVisitResult.CONTINUE; }

        throw exc;
    }
}
