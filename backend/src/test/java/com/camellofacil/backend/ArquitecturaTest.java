package com.camellofacil.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.camellofacil.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    /** Las dependencias entre módulos van en un solo sentido: no hay ciclos. */
    @ArchTest
    static final ArchRule sin_ciclos_entre_modulos = slices()
            .matching("com.camellofacil.backend.(*)..")
            .should().beFreeOfCycles();

    /** La capa web solo habla con app; nunca con persistence. */
    @ArchTest
    static final ArchRule web_no_usa_persistence = noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat().resideInAPackage("..persistence..")
            .allowEmptyShould(true); // aún no hay clases que dependan de persistence

    /** La capa app no conoce la capa web. */
    @ArchTest
    static final ArchRule app_no_usa_web = noClasses()
            .that().resideInAPackage("..app..")
            .should().dependOnClassesThat().resideInAPackage("..web..");

    /** La capa persistence no conoce ni app ni web. */
    @ArchTest
    static final ArchRule persistence_no_usa_app_ni_web = noClasses()
            .that().resideInAPackage("..persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("..app..", "..web..")
            .allowEmptyShould(true); // aún no hay clases en persistence

    /** Inyección por constructor, no por campos. */
    @ArchTest
    static final ArchRule sin_autowired_en_campos = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.beans.factory.annotation.Autowired");
}
