package de.ulbms.scdh.seed.xc.api.inject;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@link CompileTime} is a qualifier for beans injected for
 * compiling a {@link de.ulbms.scdh.seed.xc.api.Transformation}.
 *
 * @see  <a href="https://quarkus.io/guides/cdi#you-talked-about-some-qualifiers">Introduction to Contexts and Dependency Injection (CDI)</a>
 */
@Qualifier
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
public @interface CompileTime {}
