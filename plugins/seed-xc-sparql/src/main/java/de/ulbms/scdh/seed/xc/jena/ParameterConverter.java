package de.ulbms.scdh.seed.xc.jena;

import de.ulbms.scdh.seed.xc.api.TransformationPreparationException;
import jakarta.enterprise.context.ApplicationScoped;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.jena.query.ParameterizedSparqlString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ParameterConverter} class casts strings to Java objects based on a xs-type.
 */
@ApplicationScoped
public class ParameterConverter {

	private static final Logger LOG = LoggerFactory.getLogger(ParameterConverter.class);

	/**
	 * Sets the variable with <code>name</code> in the supplied query.
	 *
	 * @param name - Name of the SPARQL variable
	 * @param value - Value to be set
	 * @param type - type information
	 * @param query - the parametrized query
	 * @throws TransformationPreparationException - on a cast failure
	 */
	public void setQueryParameter(String name, String value, String type, ParameterizedSparqlString query)
			throws TransformationPreparationException {
		switch (type) {
			case "xs:anyURI" -> query.setIri(name, value);
			case "xs:string" -> query.setLiteral(name, value);
			case "xs:integer" -> {
				try {
					query.setLiteral(name, Integer.parseInt(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to integer", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:long" -> {
				try {
					query.setLiteral(name, Long.parseLong(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to long", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:float" -> {
				try {
					query.setLiteral(name, Float.parseFloat(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to float", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:double" -> {
				try {
					query.setLiteral(name, Double.parseDouble(value));
				} catch (NumberFormatException e) {
					LOG.error("failed to cast '{}' value of parameter {} to double", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			case "xs:date" -> {
				try {
					Calendar calendar = Calendar.getInstance();
					SimpleDateFormat sdf = new SimpleDateFormat();
					calendar.setTime(sdf.parse(value));
					query.setLiteral(name, calendar);
				} catch (ParseException e) {
					LOG.error("failed to cast '{}' value of parameter {} to calendar", value, name);
					throw new TransformationPreparationException("failed to set parameter " + name, e);
				}
			}
			default -> {
				// defaults to string again
				LOG.error("no valid type information for parameter {}: {}. Using string", name, type);
				query.setLiteral(name, value);
			}
		}
	}
}
