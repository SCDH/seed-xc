package de.ulbms.scdh.seed.xc.service;

import com.saxonica.config.EnterpriseConfiguration;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {EnterpriseConfiguration.class})
public class ReflectionConfiguration {}
