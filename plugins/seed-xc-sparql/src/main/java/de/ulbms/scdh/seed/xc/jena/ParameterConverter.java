package de.ulbms.scdh.seed.xc.jena;

import de.ulbms.scdh.seed.xc.api.ParameterValue;
import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import jakarta.enterprise.context.ApplicationScoped;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ParameterConverter} class casts strings to Java objects based on a xs-type.
 */
@ApplicationScoped
public class ParameterConverter {

	private static final Logger LOG = LoggerFactory.getLogger(ParameterConverter.class);

	private static final String SEQUENCE_MODIFIERS = "?+*";

	private RDFNode toNode(String name, String value, String type) throws TransformationPreparationException {
		Model model = ModelFactory.createDefaultModel();
		switch (type) {
			case "xs:anyURI" -> {
				return model.createResource(value);
			}
			case "xs:string" -> {
				return model.createTypedLiteral(value);
			}
			case "xs:integer" -> {
				try {
					return model.createTypedLiteral(Integer.parseInt(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to integer", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:long" -> {
				try {
					return model.createTypedLiteral(Long.parseLong(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to long", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:float" -> {
				try {
					return model.createTypedLiteral(Float.parseFloat(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to float", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:double" -> {
				try {
					return model.createTypedLiteral(Double.parseDouble(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to double", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:boolean" -> {
				try {
					return model.createTypedLiteral(Boolean.parseBoolean(value));
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
					return model.createTypedLiteral(calendar);
				} catch (ParseException e) {
					LOG.error("failed to cast '{}' value of parameter {} to calendar", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			default -> {
				// defaults to string again
				LOG.error("no valid type information for parameter {}: {}. Using string", name, type);
				return model.createTypedLiteral(value);
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
		} else {
			String sequenceModifier = type.substring(type.length() - 1);
			if (!SEQUENCE_MODIFIERS.contains(sequenceModifier)) {
				query.setParam(name, toNode(name, value.getFirst(), type));
			} else if (sequenceModifier.equals("+") && value.isEmpty()) {
				LOG.error("empty sequence for parameter '{}' with type {}", name, type);
				throw new TransformationPreparationException("empty sequence for type " + type);
			} else if (sequenceModifier.equals("?") && value.size() > 1) {
				LOG.error("sequence of more then one item for parameter '{}' of type {}", name, type);
				throw new TransformationPreparationException("sequence of more than one item for type " + type);
			} else {

				LOG.debug("setting VALUES of type {}", type.substring(0, type.length() - 1));
				List<RDFNode> values = new ArrayList<>();
				for (String v : value) values.add(toNode(name, v, type.substring(0, type.length() - 1)));
				query.setValues(name, values);
			}
		}
	}
}
