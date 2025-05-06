package com.example;


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
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class RxClassDependencyAnalyzer {

    public RxClassDependencyAnalyzer(final Path rootDirectory) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(rootDirectory));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public Flowable<RxDepsReport> getClassDependencies(Flowable<String> classCode) {
        return classCode.map(StaticJavaParser::parse)
                .map(compilationUnit -> {
                    final Flowable<String> importedSymbols =
                            Flowable.fromStream(compilationUnit
                                    .findAll(ImportDeclaration.class).stream()
                                    .map(ImportDeclaration::getNameAsString)
                            .distinct());

                    final Flowable<String> usedTypes =
                            Flowable.fromStream(compilationUnit
                                            .findAll(ClassOrInterfaceType.class).stream()
                            .flatMap(classOrInterfaceType -> {
                                try {
                                    return Stream.of(classOrInterfaceType.resolve());
                                } catch (UnsolvedSymbolException | IllegalStateException ignored) {
                                    return Stream.empty();
                                }
                            })
                            .map(ResolvedType::asReferenceType)
                            .map(ResolvedReferenceType::getQualifiedName));
                    final String className = (String) compilationUnit.findFirst(TypeDeclaration.class)
                            .flatMap(TypeDeclaration::getFullyQualifiedName)
                            .get();
                    return new RxDepsReport(className,Flowable.merge(usedTypes,importedSymbols).distinct());
                }).subscribeOn(Schedulers.computation());
    }
}
