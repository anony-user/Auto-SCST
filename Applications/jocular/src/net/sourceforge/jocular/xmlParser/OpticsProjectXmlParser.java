/*******************************************************************************
 * Copyright (c) 2013,Bryan Matthews
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.xmlParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import net.sourceforge.jocular.JocularFileParsingException;
import net.sourceforge.jocular.project.OpticsProject;

public class OpticsProjectXmlParser {
	
	private static String SCHEMA_LANGUAGE =  "http://java.sun.com/xml/jaxp/properties/schemaLanguage";		      
	private static String XML_SCHEMA =       "http://www.w3.org/2001/XMLSchema";
	private static String SCHEMA_SOURCE =    "http://java.sun.com/xml/jaxp/properties/schemaSource";	
	private static final String SCHEMA =     "/net/sourceforge/jocular/Jocular.xsd";
			
	/**
	 * Creates an OpticsProject from a parsed XML file
	 * 
	 * @param fileName Name of the xml file to parse a project from
	 * 
	 * @return Created OpticsProject object with instantiated optics objects
	 *         Returns blank project if file contains errors
	 *         
	 * @throws IOException
	 *  
	 *  
	 */
	public static OpticsProject parseProjectFile(String fileName) throws IOException, JocularFileParsingException{
		
		OpticsProjectXmlHandler handler = new OpticsProjectXmlHandler();
		
		XMLReader xmlReader;
		
		try{
			xmlReader = getXMLReader();
			
		}catch(ParserConfigurationException | SAXException e){
			String message = "Failed to configure file parser";							
			throw new JocularFileParsingException(message);
		}catch(URISyntaxException e){
			String message = "Failed to find schema";							
			throw new JocularFileParsingException(message);
		}
		
		xmlReader.setContentHandler(handler);
		xmlReader.setErrorHandler(new MyErrorHandler(System.err));
		
		try{
			xmlReader.parse(fileName);
			
		}catch(SAXException e){
			String message = "XML Parser exception!\n";	
			message = message.concat(e.getMessage());
			
			throw new JocularFileParsingException(message);
		}
		
		
		OpticsProject project = handler.getProject();
		project.setFileName(fileName);
		
		return project;
	}
	
	private static XMLReader getXMLReader() throws URISyntaxException, ParserConfigurationException, SAXException{
		
		SAXParser saxParser;
		XMLReader xmlReader;
		
		InputStream schemaIS = OpticsProjectXmlParser.class.getResourceAsStream(SCHEMA);
				
		SAXParserFactory factory = SAXParserFactory.newInstance();		
		factory.setNamespaceAware(true);
		factory.setValidating(true);

		saxParser = factory.newSAXParser();
		saxParser.setProperty(SCHEMA_LANGUAGE,XML_SCHEMA);
		saxParser.setProperty(SCHEMA_SOURCE, schemaIS);
		
		xmlReader = saxParser.getXMLReader();
		
		return xmlReader;
	}
	
	private static class MyErrorHandler implements ErrorHandler {
	    private PrintStream out;

	    MyErrorHandler(PrintStream out) {
	        this.out = out;
	    }

	    private String getParseExceptionInfo(SAXParseException spe) {
	        String systemId = spe.getSystemId();

	        if (systemId == null) {
	            systemId = "null";
	        }

	        String info = "\tURI=" + systemId + "\n\tLine=" 
	            + spe.getLineNumber() + "\n\t" + spe.getMessage();

	        return info;
	    }

	    public void warning(SAXParseException spe) throws SAXException {
	        out.println("\tWarning\n" + getParseExceptionInfo(spe));
	    }
	        
	    public void error(SAXParseException spe) throws SAXException {
	        String message = "\tError\n" + getParseExceptionInfo(spe);
	        throw new SAXException(message);
	    }

	    public void fatalError(SAXParseException spe) throws SAXException {
	        String message = "\tFatal Error\n" + getParseExceptionInfo(spe);
	        throw new SAXException(message);
	    }
	}	
}
