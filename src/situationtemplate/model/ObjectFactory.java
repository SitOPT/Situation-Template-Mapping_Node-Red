//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.07.13 at 08:40:50 PM CEST 
//


package situationtemplate.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the situationtemplate.model package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SituationTemplate_QNAME = new QName("", "SituationTemplate");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: situationtemplate.model
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TConditionNode }
     * 
     */
    public TConditionNode createTConditionNode() {
        return new TConditionNode();
    }

    /**
     * Create an instance of {@link TSituationTemplate }
     * 
     */
    public TSituationTemplate createTSituationTemplate() {
        return new TSituationTemplate();
    }

    /**
     * Create an instance of {@link TOperationNode }
     * 
     */
    public TOperationNode createTOperationNode() {
        return new TOperationNode();
    }

    /**
     * Create an instance of {@link TTimeNode }
     * 
     */
    public TTimeNode createTTimeNode() {
        return new TTimeNode();
    }

    /**
     * Create an instance of {@link TSituationNode }
     * 
     */
    public TSituationNode createTSituationNode() {
        return new TSituationNode();
    }

    /**
     * Create an instance of {@link TContextNode }
     * 
     */
    public TContextNode createTContextNode() {
        return new TContextNode();
    }

    /**
     * Create an instance of {@link TParent }
     * 
     */
    public TParent createTParent() {
        return new TParent();
    }

    /**
     * Create an instance of {@link TConditionNode.CondValues }
     * 
     */
    public TConditionNode.CondValues createTConditionNodeCondValues() {
        return new TConditionNode.CondValues();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TSituationTemplate }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "SituationTemplate")
    public JAXBElement<TSituationTemplate> createSituationTemplate(TSituationTemplate value) {
        return new JAXBElement<TSituationTemplate>(_SituationTemplate_QNAME, TSituationTemplate.class, null, value);
    }

}
