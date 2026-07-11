package de.ulbms.scdh.seed.xc.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A reflection configuration like described in <a
 * href="https://quarkus.io/guides/writing-native-applications-tips#registering-for-reflection">Tips
 * for writing native applications</a> by Quarkus.io.<P/>
 *
 * We have to register the Jackson data bindings in the generated
 * model classes, since they use reflection.
 */
@RegisterForReflection(
		targets = {
			Config.class,
			Context.class,
			ParameterDescriptor.class,
			ParameterValue.class,
			Parser.class,
			ResourceMapping.class,
			ResourceMappingValue.class,
			RuntimeParametersInitialCallValue.class,
			RuntimeParameters.class,
			Serializer.class,
			TransformationIDs.class,
			TransformationInfoInitialCallablesValue.class,
			TransformationInfo.class,
			TransformationInfoLibrariesInner.class,
			TransformationMap.class,
			TypedParameter.class,
			XsltParameterDetails.class,
			XsltParameterDetailsValue.class,
		})
public class ReflectionConfiguration {}
