package de.ulbms.scdh.seed.xc.dts.transformations;

import org.apache.jena.rdf.model.*;

/**
 * The {@link RdfUtils} offers functions for dealing with common RDF processing tasks.
 */
public class RdfUtils {

	/**
	 * Copies property of resource from source to target model.
	 *
	 * @param source - the source {@link Model} to copy from
	 * @param target - the target {@link Model} to copy to
	 * @param resource - the {@link Resource} in the source model where the property is on
	 * @param property - the {@link Property} to copy
	 */
	public static void copySimpleProperty(Model source, Model target, Resource resource, Property property) {
		target.add(source.listStatements(resource, property, (RDFNode) null));
	}

	/**
	 * Copies property of resource from source to target model.
	 *
	 * @param source - the source {@link Model} to copy from
	 * @param target - the target {@link Model} to copy to
	 * @param resource - the {@link Resource} in the source model where the property is on
	 * @param property - the {@link Property} to copy
	 */
	public static void copyComplexProperty(Model source, Model target, Resource resource, Property property) {
		Model stmts = source.listStatements(resource, property, (RDFNode) null).toModel();
		// LOG.debug("coping complex {} properties {}", stmts.size(), property.getURI());
		target.add(stmts);
		StmtIterator statements = stmts.listStatements();
		while (statements.hasNext()) {
			Statement statement = statements.next();
			if (statement.getObject().isResource() || statement.getObject().isAnon()) {
				StmtIterator substatements =
						source.listStatements(statement.getObject().asResource(), null, (RDFNode) null);
				while (substatements.hasNext()) {
					Statement substatement = substatements.next();
					target.add(substatement);
					if (substatement.getObject().isResource()
							|| substatement.getObject().isAnon()) {
						copyComplexProperty(
								source, target, substatement.getObject().asResource(), null);
					}
				}
			}
		}
	}
}
