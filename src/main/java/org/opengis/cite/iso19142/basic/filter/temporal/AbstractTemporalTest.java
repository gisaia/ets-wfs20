package org.opengis.cite.iso19142.basic.filter.temporal;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSElementDeclaration;
import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.geotoolkit.temporal.object.DefaultPosition;
import org.opengis.cite.iso19142.basic.filter.QueryFilterFixture;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalFactory;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.testng.SkipException;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public abstract class AbstractTemporalTest extends QueryFilterFixture {

    private static final Logger LOGR = Logger.getLogger( AbstractTemporalTest.class.getPackage().getName() );

    public TemporalProperty findTemporalProperty( QName featureType ) {
        List<XSElementDeclaration> temporalProperties = findTemporalProperties( featureType );
        if ( temporalProperties.isEmpty() ) {
            throw new SkipException( "Feature type has no temporal properties: " + featureType );
        }

        TemporalProperty temporalExtent = findTemporalExtent( featureType, temporalProperties );
        if ( temporalExtent == null )
            throw new SkipException(
                                     "Feature type + "
                                                             + featureType
                                                             + " has at least one temporal properties but an extent could not be calculated (e.g. all properties are nill). " );
        return temporalExtent;
    }

    private TemporalProperty findTemporalExtent( QName featureType, List<XSElementDeclaration> temporalProperties ) {
        Period temporalExtent = null;
        XSElementDeclaration temporalProperty = null;

        for ( XSElementDeclaration temporalProp : temporalProperties ) {
            try {
            	temporalProperty = temporalProp;
                temporalExtent = this.dataSampler.getTemporalExtentOfProperty( this.model, featureType, temporalProp );
            } catch ( Exception e ) {
                LOGR.warning( "Could not calculate the extent of the temporal property " + temporalProp
                              + " of the feature type " + featureType );
            }
        }

        if ( temporalProperty == null || temporalExtent == null )
            return null;
        return new TemporalProperty( temporalProperty, temporalExtent );
    }

    class TemporalProperty {

        private XSElementDeclaration property;

        private Period extent;

        public TemporalProperty( XSElementDeclaration property, Period extent ) {
            this.property = property;
            this.extent = extent;
        }

        public XSElementDeclaration getProperty() {
            return property;
        }

        public Period getExtent() {
            return extent;
        }
    }

    public static TemporalGeometricPrimitive gmlToTemporalGeometricPrimitive(Element gmlTime) {
        List<ZonedDateTime> instants = new ArrayList();
        String frame = gmlTime.getAttribute("frame");
        Element timePosition;
        ZonedDateTime zdt;
        if (gmlTime.getLocalName().equals("TimeInstant")) {
            timePosition = (Element)gmlTime.getElementsByTagNameNS("http://www.opengis.net/gml/3.2", "timePosition").item(0);
            if (!timePosition.getAttribute("frame").isEmpty()) {
                frame = timePosition.getAttribute("frame");
            }

            if (!frame.isEmpty() && !frame.contains("8601")) {
                throw new RuntimeException("Unsupported temporal reference frame: " + frame);
            }

            try {
                zdt = ZonedDateTime.parse(timePosition.getTextContent(), DateTimeFormatter.ISO_DATE_TIME);
                instants.add(zdt);
            } catch (DateTimeParseException var7) {
                throw new RuntimeException("Not an ISO instant: " + timePosition.getTextContent());
            }
        } else {
            timePosition = (Element)gmlTime.getElementsByTagNameNS("http://www.opengis.net/gml/3.2", "timePosition").item(0);
            instants.add(ZonedDateTime.parse(timePosition.getTextContent(), DateTimeFormatter.ISO_DATE_TIME));
            Element endPosition = (Element)gmlTime.getElementsByTagNameNS("http://www.opengis.net/gml/3.2", "timePosition").item(1);
            instants.add(ZonedDateTime.parse(endPosition.getTextContent(), DateTimeFormatter.ISO_DATE_TIME));
        }

        TemporalFactory tmFactory = new DefaultTemporalFactory();
        zdt = null;
        Object timePrimitive;
        if (instants.size() == 1) {
            timePrimitive = tmFactory.createInstant(new DefaultPosition(Date.from(((ZonedDateTime)instants.get(0)).toInstant())));
        } else {
            Instant beginInstant = tmFactory.createInstant(new DefaultPosition(Date.from(((ZonedDateTime)instants.get(0)).toInstant())));
            Instant endInstant = tmFactory.createInstant(new DefaultPosition(Date.from(((ZonedDateTime)instants.get(1)).toInstant())));
            timePrimitive = tmFactory.createPeriod(beginInstant, endInstant);
        }

        return (TemporalGeometricPrimitive)timePrimitive;
    }

}
