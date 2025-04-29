package com.example;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.vertx.core.Future;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class FutureClassDependencyAnalyzer {
    public FutureClassDependencyAnalyzer(final Path rootDirectory) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(rootDirectory));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public Future<DepsReport> getClassDependencies(Future<InputStream> classCode) {
        Future<CompilationUnit> compilationUnit = classCode
                .compose(code -> Future.succeededFuture(StaticJavaParser.parse(code)));
        final var importedTypesFuture = compilationUnit.compose(
                cu -> Future.succeededFuture(
                    cu.findAll(ImportDeclaration.class).stream()
                            .map(ImportDeclaration::getNameAsString)
                            .collect(toSet())
                )
        );
        Future<Set<String>> usedTypesFuture = compilationUnit.compose(
                cu -> Future.succeededFuture(
                        cu.findAll(ClassOrInterfaceType.class).stream()
                                .flatMap(classOrInterfaceType -> {
                                    try {
                                        return Stream.of(classOrInterfaceType.resolve());
                                    } catch (UnsolvedSymbolException ignored) {
                                        return Stream.empty();
                                    }
                                })
                                .map(ResolvedType::asReferenceType)
                                .map(ResolvedReferenceType::getQualifiedName)
                        .collect(toSet())
                )
        );
        Future<Set<TypeDeclaration<?>>> declaredTypesFuture = compilationUnit.compose(cu -> {
                    final var classDec = new HashSet<TypeDeclaration<?>>(cu.findAll(ClassOrInterfaceDeclaration.class));
                    final var enumDec = new HashSet<>(cu.findAll(EnumDeclaration.class));
                    final var recordDec = new HashSet<>(cu.findAll(RecordDeclaration.class));
                    classDec.addAll(enumDec);
                    classDec.addAll(recordDec);
                    return Future.succeededFuture(classDec);
                }
        );
        Future<Map<ClassVisibility, Set<String>>> classesForVisibility = declaredTypesFuture
                .compose(classOrInterfaceDeclarations ->
                        Future.succeededFuture(
                                classOrInterfaceDeclarations.stream()
                                        .collect(Collectors.groupingBy(ClassVisibility::fromClassDeclaration,
                                                mapping(TypeDeclaration::getNameAsString, toSet())))
                        )
                );

        return Future.all(usedTypesFuture, classesForVisibility)
                .compose(compositeFuture -> {
                    @SuppressWarnings("unchecked") final var dependencyTypes = (Set<String>) compositeFuture.resultAt(0);
                    @SuppressWarnings("unchecked") final var declaredTypes = (Map<ClassVisibility, Set<String>>) compositeFuture.resultAt(1);
                    for (final Set<String> classDecSet : declaredTypes.values()) {
                        dependencyTypes.removeAll(classDecSet);
                    }
                    return Future.succeededFuture(new DepsReport(
                            declaredTypes.getOrDefault(ClassVisibility.PUBLIC, Set.of()),
                            declaredTypes.getOrDefault(ClassVisibility.PROTECTED, Set.of()),
                            declaredTypes.getOrDefault(ClassVisibility.PACKAGE_PROTECTED, Set.of()),
                            dependencyTypes
                    ));
                });
    }

    public enum ClassVisibility {
        PUBLIC, PROTECTED, PACKAGE_PROTECTED, PRIVATE;

        static ClassVisibility fromClassDeclaration(TypeDeclaration<?> classOrInterfaceDeclaration) {
            NodeList<Modifier> classModifiers = classOrInterfaceDeclaration.getModifiers();
            if (classModifiers.contains(Modifier.publicModifier())) {
                return ClassVisibility.PUBLIC;
            }
            if (classModifiers.contains(Modifier.protectedModifier())) {
                return ClassVisibility.PROTECTED;
            }
            if (classModifiers.contains(Modifier.privateModifier())) {
                return ClassVisibility.PRIVATE;
            }
            return ClassVisibility.PACKAGE_PROTECTED;
        }
    }

}
