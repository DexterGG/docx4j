/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */
package org.docx4j.openpackaging.parts;


import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;

import org.docx4j.jaxb.Context;
import org.docx4j.jaxb.NamespacePrefixMapperUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.dom4j.Document;

/** OPC Parts are either XML, or binary (or text) documents.
 * 
 *  Most are XML documents.
 *  
 *  docx4j aims to represent XML parts using JAXB.  
 *  
 *  Any XML Part for which we have a JAXB representation (eg the main
 *  document part) should extend this Part.  
 *  
 *  This class provides only one of the methods for serializing (marshalling) the 
 *  Java content tree back into XML data found in 
 *  javax.xml.bind.Marshaller interface.  You can always use 
 *  any of the others by getting the jaxbElement required by those
 *  methods.
 *  
 *  Insofar as unmarshalling is concerned, at present it doesn't 
 *  contain all the methods in javax.xml.bind.unmarshaller interface.
 *  This is because the content always comes from the same place
 *  (ie from a zip file or JCR via org.docx4j.io.*).  
 *  TODO - what is the best thing to unmarshall from?
 *  
 *  
 * */
public abstract class JaxbXmlPart extends Part {
	
	// This class is abstract
	// Most applications ought to be able to instantiate
	// any part as the relevant subclass.
	// If it was not abstract, users would have to
	// take care to set its content type and
	// relationship type when adding the part.
	
	public JaxbXmlPart(PartName partName) throws InvalidFormatException {
		super(partName);
		setJAXBContext(Context.jc);						
	}

	public JaxbXmlPart(PartName partName, JAXBContext jc) throws InvalidFormatException {
		super(partName);
		setJAXBContext(jc);
	}
	
	protected JAXBContext jc;
	
	public void setJAXBContext(JAXBContext jc) {
		this.jc = jc;
	}
	
	
	/** The content tree (ie JAXB representation of the Part) */
	public Object jaxbElement = null;

	public Object getJaxbElement() {
		return jaxbElement;
	}

	public void setJaxbElement(Object jaxbElement) {
		this.jaxbElement = jaxbElement;
	}
	
	
	
    /**
     * Marshal the content tree rooted at <tt>jaxbElement</tt> into a DOM tree.
     * 
     * @param node
     *      DOM nodes will be added as children of this node.
     *      This parameter must be a Node that accepts children
     *      ({@link org.w3c.dom.Document},
     *      {@link  org.w3c.dom.DocumentFragment}, or
     *      {@link  org.w3c.dom.Element})
     * 
     * @throws JAXBException
     *      If any unexpected problem occurs during the marshalling.
     */
    public void marshal(org.w3c.dom.Node node) throws JAXBException {
    	
    	try {
			marshal(node, NamespacePrefixMapperUtils.getPrefixMapper()  );
		} catch (ClassNotFoundException e) {
			throw new JAXBException("Neither JAXB RI nor Java 6 implementation present", e);
		}
    	
	}

    /**
     * Marshal the content tree rooted at <tt>jaxbElement</tt> into a DOM tree.
     * 
     * @param node
     *      DOM nodes will be added as children of this node.
     *      This parameter must be a Node that accepts children
     *      ({@link org.w3c.dom.Document},
     *      {@link  org.w3c.dom.DocumentFragment}, or
     *      {@link  org.w3c.dom.Element})
     * 
     * @throws JAXBException
     *      If any unexpected problem occurs during the marshalling.
     */
    public void marshal(org.w3c.dom.Node node, 
    		Object namespacePrefixMapper) throws JAXBException {

		try {
			Marshaller marshaller = jc.createMarshaller();

			try {
				
				if ( (namespacePrefixMapper instanceof  org.docx4j.jaxb.NamespacePrefixMapper)
						|| (namespacePrefixMapper instanceof  org.docx4j.jaxb.NamespacePrefixMapperRelationshipsPart) ) {
				
					marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", 
							namespacePrefixMapper ); 
				
					// Reference implementation appears to be present (in endorsed dir?)
					log.info("setProperty: com.sun.xml.bind.namespacePrefixMapper");
					
				} else {
					
					// Use JAXB distributed in Java 6 - note 'internal' 
					// Switch to other mapper
					log.info("attempting to setProperty: com.sun.xml.INTERNAL.bind.namespacePrefixMapper");
					marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", namespacePrefixMapper);
					
				}
				
			} catch (javax.xml.bind.PropertyException cnfe) {
				
				log.error(cnfe);
				throw cnfe;
				
			}
			
			marshaller.marshal(jaxbElement, node);

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    /**
	 * Marshal the content tree rooted at <tt>jaxbElement</tt> into an output
	 * stream, using org.docx4j.jaxb.NamespacePrefixMapper.
	 * 
	 * @param os
	 *            XML will be added to this stream.
	 * 
	 * @throws JAXBException
	 *             If any unexpected problem occurs during the marshalling.
	 */
    public void marshal(java.io.OutputStream os) throws JAXBException {

		
		marshal( os, new org.docx4j.jaxb.NamespacePrefixMapper() ); 

	}

    /**
	 * Marshal the content tree rooted at <tt>jaxbElement</tt> into an output
	 * stream
	 * 
	 * @param os
	 *            XML will be added to this stream.
	 * @param namespacePrefixMapper
	 *            namespacePrefixMapper
	 * 
	 * @throws JAXBException
	 *             If any unexpected problem occurs during the marshalling.
	 */
    public void marshal(java.io.OutputStream os, com.sun.xml.bind.marshaller.NamespacePrefixMapper namespacePrefixMapper) throws JAXBException {

		try {
			Marshaller marshaller = jc.createMarshaller();

			try { 
				marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", 
						namespacePrefixMapper ); 

				// Reference implementation appears to be present (in endorsed dir?)
				log.info("using property com.sun.xml.bind.namespacePrefixMapper");
				
			} catch (javax.xml.bind.PropertyException cnfe) {
				
				log.error(cnfe);

				log.info("attempting to use com.sun.xml.INTERNAL.bind.namespacePrefixMapper");
				
				// Use JAXB distributed in Java 6 - note 'internal' 
				if ( namespacePrefixMapper instanceof  org.docx4j.jaxb.NamespacePrefixMapper ) {
					// Switch to other mapper
					marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", 
							new org.docx4j.jaxb.NamespacePrefixMapperSunInternal()  ); 	
					
				} else if ( namespacePrefixMapper instanceof  org.docx4j.jaxb.NamespacePrefixMapperRelationshipsPart ) {
						// Switch to other mapper
						marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", 
								new org.docx4j.jaxb.NamespacePrefixMapperRelationshipsPartSunInternal()  ); 					
				} else {
					// Just use what we have been given
					marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", 
							namespacePrefixMapper ); 										
				}
				
				
			}
			
			System.out.println("marshalling " + this.getClass().getName() + " ..." );									
			
			marshaller.marshal(jaxbElement, os);
			
			System.out.println(this.getClass().getName() + " marshalled \n\n" );									

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    /**
	 * Unmarshal XML data from the specified InputStream and return the
	 * resulting content tree. Validation event location information may be
	 * incomplete when using this form of the unmarshal API.
	 * 
	 * <p>
	 * Implements <a href="#unmarshalGlobal">Unmarshal Global Root Element</a>.
	 * 
	 * @param is
	 *            the InputStream to unmarshal XML data from
	 * @return the newly created root object of the java content tree
	 * 
	 * @throws JAXBException
	 *             If any unexpected errors occur while unmarshalling
	 */
    public Object unmarshal( java.io.InputStream is ) throws JAXBException {
    	
		try {
			
//			if (jc==null) {
//				setJAXBContext(Context.jc);				
//			}
		    		    
			Unmarshaller u = jc.createUnmarshaller();
			
			//u.setSchema(org.docx4j.jaxb.WmlSchema.schema);
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());

			log.debug("unmarshalling " + this.getClass().getName() );															
			jaxbElement = u.unmarshal( is );						
			log.debug( this.getClass().getName() + " unmarshalled" );									

		} catch (Exception e ) {
			e.printStackTrace();
		}
    	
		return jaxbElement;
    	
    }	
//    public abstract Object unmarshal( java.io.InputStream is ) throws JAXBException;

    public Object unmarshal(org.w3c.dom.Element el) throws JAXBException {

		try {

			Unmarshaller u = jc.createUnmarshaller();
						
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());

			jaxbElement = u.unmarshal( el );

			return jaxbElement;
			
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	
	
}