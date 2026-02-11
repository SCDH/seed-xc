package de.ulbms.scdh.seed.xc.service;

import io.quarkus.runtime.annotations.RegisterForReflection;

import com.saxonica.config.EnterpriseConfiguration;

@RegisterForReflection(targets = {
    EnterpriseConfiguration.class
    })
public class ReflectionConfiguration {}
