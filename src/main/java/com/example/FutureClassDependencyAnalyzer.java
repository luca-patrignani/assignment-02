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
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedEnumDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedRecordDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.vertx.core.Future;

import javax.lang.model.type.DeclaredType;
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
        final Future<Set<String>> importedSymbolsFuture = compilationUnit.compose(
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
        final Future<Set<String>> definedClasses = compilationUnit.compose(
                cu -> Future.succeededFuture(
                        Stream.of(
                            cu.findAll(ClassOrInterfaceDeclaration.class).stream().map(ClassOrInterfaceDeclaration::resolve),
                            cu.findAll(EnumDeclaration.class).stream().map(EnumDeclaration::resolve),
                            cu.findAll(RecordDeclaration.class).stream().map(RecordDeclaration::resolve)
                        ).flatMap(s -> s)
                                .map(ResolvedReferenceTypeDeclaration::getQualifiedName)
                                .collect(toSet())
                )
        );
       @SuppressWarnings("OptionalGetWithoutIsPresent")
       final Future<String> topClassName = compilationUnit.compose(
               cu -> Future.succeededFuture(
                       (String)cu.findFirst(TypeDeclaration.class)
                           .flatMap(TypeDeclaration::getFullyQualifiedName)
                           .get()
               )
       );
       return Future.all(importedSymbolsFuture, usedTypesFuture, definedClasses, topClassName)
               .compose(compositeFuture -> {
                   @SuppressWarnings("unchecked")
                   final var dependencies = new HashSet<>((Set<String>)compositeFuture.resultAt(0));
                   dependencies.addAll(compositeFuture.resultAt(1));
                   final Set<String> dc = compositeFuture.resultAt(2);
                   dependencies.removeAll(dc);
                   final String className = compositeFuture.resultAt(3);
                   return Future.succeededFuture(new DepsReport(
                           className,
                           dependencies
                   ));
               });
    }
}