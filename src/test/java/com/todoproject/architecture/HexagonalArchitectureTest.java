package com.todoproject.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

/**
 * Enforces constitution principle II as a build gate, not a convention (research R9).
 *
 * <p>Five rules: the domain and application layers carry no framework types; the persistence
 * adapter is reached only through its port; the domain never depends on the application layer;
 * the layer order is one-directional; and {@code CardJpaEntity} never leaks outside its package.
 */
class HexagonalArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.todoproject");

    @Test
    void domainAndApplicationAreFrameworkFree() {
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage("com.todoproject.domain..", "com.todoproject.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "jakarta.servlet..",
                        "com.todoproject.adapter..");
        rule.check(CLASSES);
    }

    @Test
    void persistenceAdapterIsReachedOnlyThroughItsPort() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("com.todoproject.adapter.in..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.todoproject.adapter.out.persistence..");
        rule.check(CLASSES);
    }

    @Test
    void domainNeverDependsOnApplication() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("com.todoproject.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.todoproject.application..");
        rule.check(CLASSES);
    }

    @Test
    void layersAreOneDirectional() {
        ArchRule rule = Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain")
                .definedBy("com.todoproject.domain..")
                .layer("Application")
                .definedBy("com.todoproject.application..")
                .layer("Adapter")
                .definedBy("com.todoproject.adapter..", "com.todoproject.config..")
                .whereLayer("Domain")
                .mayOnlyBeAccessedByLayers("Application", "Adapter")
                .whereLayer("Application")
                .mayOnlyBeAccessedByLayers("Adapter");
        rule.check(CLASSES);
    }

    @Test
    void cardJpaEntityNeverLeavesItsPackage() {
        ArchRule rule = classes()
                .that()
                .haveSimpleName("CardJpaEntity")
                .should()
                .onlyBeAccessed()
                .byAnyPackage("com.todoproject.adapter.out.persistence..");
        rule.check(CLASSES);
    }
}
