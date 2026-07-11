package de.ulbms.scdh.seed.xc.dts.v1_0;

import de.ulbms.scdh.seed.xc.dts.model.*;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(
		targets = {
			CitableUnit.class,
			CitationTree.class,
			CiteStructure.class,
			Collection.class,
			CollectionMemberInner.class,
			ContextArray.class,
			DublinCore.class,
			Entry.class,
			Navigation.class,
			Pagination.class,
			Resource.class
		})
public class ReflectionConfiguration {}
