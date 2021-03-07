package be.catsandcoding.dozer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Options {
    private final List<String> classPathDirectories;
    private final String pathToProgressKeepingFile;
    private final boolean caseInsensitiveFieldMatchingAllowed;
    private final boolean ignoringErrorsAllowed;

    public Options(){
        this(new ArrayList<>(), null, false, false);
    }

    private Options(List<String> classPathDirectories,
                    String pathToProgressKeepingFile,
                    boolean caseInsensitiveFieldMatchingAllowed, boolean ignoringErrorsAllowed){
        this.classPathDirectories = Collections.unmodifiableList(classPathDirectories);
        this.pathToProgressKeepingFile = pathToProgressKeepingFile;
        this.caseInsensitiveFieldMatchingAllowed = caseInsensitiveFieldMatchingAllowed;
        this.ignoringErrorsAllowed = ignoringErrorsAllowed;
    }

    public Options withClassPathDirectories(List<String> classPathDirectories){
        return new Options(classPathDirectories,
                getPathToProgressKeepingFile().orElse(null),
                isCaseInsensitiveFieldMatchingAllowed(), isIgnoringErrorsAllowed());
    }

    public Options withPathToProgressKeepingFile(String pathToProgressKeepingFile){
        return new Options(getClassPathDirectories(),
                pathToProgressKeepingFile,
                isCaseInsensitiveFieldMatchingAllowed(), isIgnoringErrorsAllowed());
    }

    public Options withCaseInsensitiveFieldMatchingAllowed(boolean caseInsensitiveFieldMatchingAllowed){
        return new Options(getClassPathDirectories(),
                getPathToProgressKeepingFile().orElse(null),
                caseInsensitiveFieldMatchingAllowed, isIgnoringErrorsAllowed());
    }

    public Options withIgnoringErrorsAllowed(boolean ignoringErrorsAllowed){
        return new Options(getClassPathDirectories(),
                getPathToProgressKeepingFile().orElse(null),
                isCaseInsensitiveFieldMatchingAllowed(), ignoringErrorsAllowed);
    }

    public List<String> getClassPathDirectories() {
        return classPathDirectories;
    }

    public Optional<String> getPathToProgressKeepingFile() {
        return Optional.ofNullable(pathToProgressKeepingFile);
    }

    public boolean isCaseInsensitiveFieldMatchingAllowed() {
        return caseInsensitiveFieldMatchingAllowed;
    }

    public boolean isIgnoringErrorsAllowed() {
        return ignoringErrorsAllowed;
    }
}
