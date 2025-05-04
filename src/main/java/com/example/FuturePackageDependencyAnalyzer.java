package com.example;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class FuturePackageDependencyAnalyzer {
    private final FutureClassDependencyAnalyzer cda;
    private final Vertx vertx = Vertx.vertx();
    private final Path root;

    public FuturePackageDependencyAnalyzer(final Path rootDirectory) {
        this.root = rootDirectory;
        cda = new FutureClassDependencyAnalyzer(rootDirectory.toAbsolutePath());
    }

    public Future<DepsReport> getPackageDependencies(Future<Path> packagePath) {
        Future<Set<String>> allDep = packagePath
                .compose(dirPath -> vertx.fileSystem().readDir(dirPath.toString()))
                .compose(strings -> FuturesHelper.all(
                        strings.stream()
                                .map(vertx.fileSystem()::readFile)
                                .map(cda::getClassDependencies)
                                .toList()
                ))
                .compose(f ->
                            Future.succeededFuture(f.stream()
                                    .map(DepsReport::dependencies)
                                    .toList())
                ).map(x -> x.stream().flatMap(Set::stream).collect(toSet()));
        var packageName = Future.succeededFuture(getPackageName(packagePath.result()));
        return Future.all(packageName, allDep)
                .compose(compositeFuture -> {
                    @SuppressWarnings("unchecked") final var dependencies = new HashSet<>((Set<String>) compositeFuture.resultAt(1));

                    final String pName = compositeFuture.resultAt(0);

                    return Future.succeededFuture(new DepsReport(
                            pName,
                            dependencies
                    ));
                });

    }

    private String getPackageName(Path directoryPath) {
        Path relative = root.relativize(directoryPath);
        return relative.toString().replace(File.separatorChar, '.');
    }
}
