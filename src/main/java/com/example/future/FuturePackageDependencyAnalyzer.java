package com.example.future;

import com.example.DepsReport;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class FuturePackageDependencyAnalyzer {
    private final FutureClassDependencyAnalyzer cda;
    private final FileSystem fs = Vertx.vertx().fileSystem();
    private final Path root;

    public FuturePackageDependencyAnalyzer(final Path rootDirectory) {
        this.root = rootDirectory;
        cda = new FutureClassDependencyAnalyzer(rootDirectory.toAbsolutePath());
    }

    public Future<DepsReport> getPackageDependencies(Future<Path> packagePath) {
        var packageName = Future.succeededFuture(getPackageName(packagePath.result()));
        Future<Set<String>> allDependencies = packagePath
                .compose(dirPath -> readAllFiles(dirPath.toString()))
                .compose(allFiles -> FuturesHelper.all(
                        allFiles.stream()
                                .map(fs::readFile)
                                .map(cda::getClassDependencies)
                                .toList()))
                .compose(f -> Future.succeededFuture(f.stream()
                        .map(DepsReport::dependencies)
                        .toList()))
                .map(x -> x.stream()
                        .flatMap(Set::stream)
                        .filter(s -> !s.contains(packageName.result()))
                        .collect(toSet()));

        return Future.all(packageName, allDependencies)
                .compose(compositeFuture -> {
                    @SuppressWarnings("unchecked") final var dependencies = new HashSet<>((Set<String>) compositeFuture.resultAt(1));
                    final String pName = compositeFuture.resultAt(0);
                    return Future.succeededFuture(new DepsReport(
                            pName,
                            dependencies
                    ));
                });

    }

    public Future<List<String>> readAllFiles(String dirPath) {
        return fs.readDir(dirPath).compose(entries -> {


            List<Future<List<String>>> futures = entries.stream().map(entry ->
                    fs.props(entry).compose(props -> {
                        if (props.isDirectory()) {
                            return readAllFiles(entry);
                        } else {
                            return Future.succeededFuture(List.of(entry));
                        }
                    })
            ).collect(Collectors.toList());

            // Combine all in one Future
            return FuturesHelper.all(futures).map(cf ->
                    cf.stream()
                            .flatMap(obj -> obj.stream())
                            .collect(Collectors.toList())
            );
        });
    }

    private String getPackageName(Path directoryPath) {
        Path relative = root.relativize(directoryPath);
        return relative.toString().replace(File.separatorChar, '.');
    }
}
