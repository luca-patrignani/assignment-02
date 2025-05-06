package com.example.rx;


import com.example.DepsReport;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RxClassDependencyAnalyzer {

    public RxClassDependencyAnalyzer(final Path rootDirectory) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(rootDirectory));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public Flowable<DepsReport> getClassDependencies(Flowable<String> classCode) {
        return classCode.map(StaticJavaParser::parse)
                .subscribeOn(Schedulers.computation())
                .map(compilationUnit -> {
                    final Stream<String> importedSymbols =
                            compilationUnit.findAll(ImportDeclaration.class).stream()
                                    .map(ImportDeclaration::getNameAsString);
                    final Stream<String> usedTypes =
                            compilationUnit.findAll(ClassOrInterfaceType.class).stream()
                            .flatMap(classOrInterfaceType -> {
                                try {
                                    return Stream.of(classOrInterfaceType.resolve());
                                } catch (UnsolvedSymbolException | IllegalStateException ignored) {
                                    return Stream.empty();
                                }
                            })
                            .map(ResolvedType::asReferenceType)
                            .map(ResolvedReferenceType::getQualifiedName);
                    final String className = (String) compilationUnit.findFirst(TypeDeclaration.class)
                            .flatMap(TypeDeclaration::getFullyQualifiedName)
                            .get();
                    return new DepsReport(className, Stream.concat(importedSymbols, usedTypes).collect(Collectors.toSet()));
                });
    }
}
