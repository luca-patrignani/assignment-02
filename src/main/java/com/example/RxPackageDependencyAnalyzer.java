package com.example;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class RxPackageDependencyAnalyzer {

    private final RxClassDependencyAnalyzer cda;
    private final Path root;

    public RxPackageDependencyAnalyzer(final Path rootDirectory, Path root) {
        cda = new RxClassDependencyAnalyzer(rootDirectory.toAbsolutePath());
        this.root = root;
    }

    public Flowable<DepsReport> getPackageDependencies(Path packagePath) throws IOException {
        final var packageName = getPackageName(packagePath);
        final Flowable<String> dependencies;
        try {
            dependencies = Flowable
                    .fromStream(Files.walk(packagePath).filter(Files::isRegularFile))
                    .map(f -> Flowable.fromCallable(() -> Files.readString(f)).subscribeOn(Schedulers.io()))
                    .flatMap(cda::getClassDependencies)
                    .flatMap(dr -> Flowable.fromCallable(dr::dependencies))
                    .flatMap(strings -> Flowable.fromStream(strings.stream()))
                    .filter(s -> !s.contains(packageName))
                    .distinct()

                                    ;
        } catch (IOException e) {
            return Flowable.error(e);
        }

        return Flowable.fromCallable(() -> new DepsReport(packageName,Set.of(""));
    }

    private String getPackageName(Path directoryPath) {
        Path relative = root.relativize(directoryPath);
        return relative.toString().replace(File.separatorChar, '.');
    }
}
