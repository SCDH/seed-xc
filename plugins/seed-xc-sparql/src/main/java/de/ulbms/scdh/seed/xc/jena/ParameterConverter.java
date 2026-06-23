package de.ulbms.scdh.seed.xc.jena;

import de.ulbms.scdh.seed.xc.api.ParameterValue;
import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import jakarta.enterprise.context.ApplicationScoped;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ParameterConverter} class casts strings to Java objects based on a xs-type.
 */
@ApplicationScoped
public class ParameterConverter {

	private static final Logger LOG = LoggerFactory.getLogger(ParameterConverter.class);

	private static final String SEQUENCE_MODIFIERS = "?+*";

	private Node toNode(String name, String value, String type) throws TransformationPreparationException {
		switch (type) {
			case "xs:anyURI" -> {
				return NodeFactory.createURI(value);
			}
			case "xs:string" -> {
				return NodeFactory.createLiteralByValue(value);
			}
			case "xs:integer" -> {
				try {
					return NodeFactory.createLiteralByValue(Integer.parseInt(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to integer", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:long" -> {
				try {
					return NodeFactory.createLiteralByValue(Long.parseLong(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to long", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:float" -> {
				try {
					return NodeFactory.createLiteralByValue(Float.parseFloat(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to float", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:double" -> {
				try {
					return NodeFactory.createLiteralByValue(Double.parseDouble(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to double", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:boolean" -> {
				try {
					return NodeFactory.createLiteralByValue(Boolean.parseBoolean(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to boolean", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:date" -> {
				try {
					Calendar calendar = Calendar.getInstance();
					SimpleDateFormat sdf = new SimpleDateFormat();
					calendar.setTime(sdf.parse(value));
					return NodeFactory.createLiteralByValue(calendar);
				} catch (ParseException e) {
					LOG.error("failed to cast '{}' value of parameter {} to calendar", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			default -> {
				// defaults to string again
				LOG.error("no valid type information for parameter {}: {}. Using string", name, type);
				return NodeFactory.createLiteralByValue(value);
			}
		}
	}

	/**
	 * Sets the variable with <code>name</code> in the supplied query.
	 *
	 * @param name - Name of the SPARQL variable
	 * @param value - Value to be set
	 * @param type - type information
	 * @param query - the parametrized query
	 * @throws TransformationPreparationException - on a cast failure
	 */
	public void setQueryParameter(String name, ParameterValue value, String type, ParameterizedSparqlString query)
			throws TransformationPreparationException {
		if (type == null || type.isEmpty()) {
			// assume string
			query.setLiteral(name, value.getFirst());
		} else if (!SEQUENCE_MODIFIERS.contains(type.substring(type.length() - 1))) {
			query.setParam(name, toNode(name, value.getFirst(), type));
		} else {
			// TODO: support plural
		}
	}
}
