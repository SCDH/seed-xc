package de.ulbms.scdh.seed.xc.api.inject;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@link TransformTimeProvider} is a qualifier for
 * {@link de.ulbms.scdh.seed.xc.api.ResourceProvider} beans injected for
 * running a {@link de.ulbms.scdh.seed.xc.api.Transformation}. This
 * a bean qualifier is used for getting resource providers out of the
 * way when injecting Saxon resolvers.
 *
 * @see  <a href="https://quarkus.io/guides/cdi#you-talked-about-some-qualifiers">Introduction to Contexts and Dependency Injection (CDI)</a>
 */
@Qualifier
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
public @interface TransformTimeProvider {}
