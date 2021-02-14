package be.catsandcoding.dozer.components;

import io.github.classgraph.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CustomClassLoader {
    private final List<String> paths = new ArrayList<>();

    public CustomClassLoader(String... paths){
        this(Arrays.asList(paths.clone()));
    }
    public CustomClassLoader(List<String> paths) {
        this.paths.add("");
        this.paths.addAll(paths);
    }

    public Optional<Class<?>> loadClass(String fullyQualifiedClassName){
        try (ScanResult scanResult = new ClassGraph().enableAllInfo()
                .overrideClasspath(paths)
                .scan()) {
            String path = scanResult.getClasspath();
            return Optional.ofNullable(scanResult.loadClass(fullyQualifiedClassName, true));
        }
    }



}
