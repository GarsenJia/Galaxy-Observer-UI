// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.galaxy.archive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a Layout file.
 *
 * @author Ahli
 */
public final class LayoutReaderDom {
	private static final String TEMPLATE = "template";
	private static final String FRAME = "Frame";
	private static final String ANY_TAG = "*";
	private static final String RACE_CONSTANT = "##";
	private static final String CONSTANT = "Constant";
	private static final String NAME = "name";
	private static final String CONSTANT_MARKER = "#";
	private static final Logger logger = LogManager.getLogger(LayoutReaderDom.class);
	
	/**
	 * Disabled Constructor.
	 */
	private LayoutReaderDom() {
	}
	
	/**
	 * @param f
	 * @param ownConstants
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static List<String> getDependencyLayouts(final File f, List<String> ownConstants)
			throws ParserConfigurationException, SAXException, IOException {
		final String nameWithFileEnding = f.getName();
		final String nameWOfileEnding =
				nameWithFileEnding.substring(0, Math.max(0, nameWithFileEnding.lastIndexOf('.')));
		
		final DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
		dbFac.setNamespaceAware(false);
		dbFac.setValidating(false);
		dbFac.setAttribute("http://xml.org/sax/features/external-general-entities", false);
		dbFac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		dbFac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		dbFac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		dbFac.setXIncludeAware(false);
		dbFac.setExpandEntityReferences(false);
		dbFac.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		final DocumentBuilder dBuilder = dbFac.newDocumentBuilder();
		final Document doc = dBuilder.parse(f);
		
		final ArrayList<String> list = new ArrayList<>();
		
		// check TEMPLATES
		final NodeList nodes = doc.getElementsByTagName(ANY_TAG);
		for (int i = 0, len = nodes.getLength(); i < len; i++) {
			final Node frame = nodes.item(i);
			// check if node is a frame
			if (frame.getNodeName().equalsIgnoreCase(FRAME)) {
				final NamedNodeMap attributes = frame.getAttributes();
				for (int j = 0; j < attributes.getLength(); j++) {
					final Node attr = attributes.item(j);
					
					// attribute is Template
					if (attr.getNodeName().equalsIgnoreCase(TEMPLATE)) {
						final String dependency = attr.getNodeValue();
						if (dependency != null) {
							int firstIndex = dependency.indexOf('/');
							int firstIndex2 = dependency.indexOf('\\');
							if (firstIndex < 0) {
								firstIndex = Integer.MAX_VALUE;
							}
							if (firstIndex2 < 0) {
								firstIndex2 = Integer.MAX_VALUE;
							}
							firstIndex = Math.min(firstIndex, firstIndex2);
							final String layoutName = dependency.substring(0, firstIndex);
							if (!layoutName.equalsIgnoreCase(nameWOfileEnding) &&
									!doesNameAppearInList(layoutName, list)) {
								if (logger.isTraceEnabled()) {
									logger.trace(nameWOfileEnding + " has dependency to " + layoutName);
								}
								list.add(layoutName);
							}
						}
						
					}
					
				}
				
			}
			
		}
		
		if (ownConstants == null) {
			ownConstants = getLayoutsConstantDefinitions(doc);
			
			if (logger.isTraceEnabled()) {
				for (final String str : ownConstants) {
					logger.trace(nameWOfileEnding + " defines constant " + str);
				}
			}
		}
		
		// constantUsage
		// nodes = doc.getElementsByTagName("*");
		final ArrayList<String> usedConstants = new ArrayList<>();
		for (int i = 0, len = nodes.getLength(); i < len; i++) {
			final Node node = nodes.item(i);
			
			// String nodeName = node.getNodeName();
			// if(nodeName.startsWith("#")){
			// String constName = nodeName;
			// if(!doesNameAppearInList(constName, usedConstants)){
			// System.out.println(nameWOfileEnding + " uses undefined constant "
			// + constName);
			// usedConstants.add(constName);
			// }
			// }
			
			// if (true) {
			final NamedNodeMap attributes = node.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				final Node attribute = attributes.item(j);
				final String attrName = attribute.getNodeName();
				final String attrValue = attribute.getNodeValue();
				
				// attribute name
				if (attrName.startsWith(CONSTANT_MARKER) && !doesNameAppearInList(attrName, usedConstants) &&
						!doesConstantNameAppearInList(attrName, ownConstants)) {
					if (logger.isTraceEnabled()) {
						logger.trace(nameWOfileEnding + " uses undefined constant " + attrName);
					}
					usedConstants.add(attrName);
					list.add(attrName);
				}
				// attribute value
				if (attrValue.startsWith(CONSTANT_MARKER) && !doesNameAppearInList(attrValue, usedConstants) &&
						!doesConstantNameAppearInList(attrValue, ownConstants)) {
					if (logger.isTraceEnabled()) {
						logger.trace(nameWOfileEnding + " uses undefined constant " + attrValue);
					}
					usedConstants.add(attrValue);
					list.add(attrValue);
				}
			}
			// }
			
		}
		
		// // add all constants that are not self-defined to dependency list
		// x:for(String constant : usedConstants){
		// for(String own : ownConstants){
		// if(constant.equals(own)){
		// continue x;
		// }
		// }
		// // if not self-defined => add as dependency
		// list.add(constant);
		//
		// }
		
		return list;
		// TODO what if a template uses a constant? what if it is in another
		// TODO what if a constant is defined in a layout after its usage?
		// layout?
	}
	
	/**
	 * Checks if a name appears in the list.
	 *
	 * @param name
	 * @param list
	 * @return
	 */
	private static boolean doesNameAppearInList(final String name, final List<String> list) {
		for (final String n : list) {
			if (n.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns a list with Constants defined in the given layout file.
	 *
	 * @param doc
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static List<String> getLayoutsConstantDefinitions(final Document doc) {
		// create list of own constant definitions
		final ArrayList<String> ownConstants = new ArrayList<>();
		final NodeList constants = doc.getElementsByTagName(CONSTANT);
		for (int i = 0, len = constants.getLength(); i < len; i++) {
			final Node constant = constants.item(i);
			final NamedNodeMap attributes = constant.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				final Node attr = attributes.item(j);
				// attribute is Template
				if (attr.getNodeName().equalsIgnoreCase(NAME)) {
					ownConstants.add(attr.getNodeValue());
					logger.trace("FOUND CONSTANT DEFINITION: {}", () -> attr.getNodeValue());
				}
				// else
				// System.out.println("REJECTED CONSTANT ATTR: " +
				// attr.getNodeName());
			}
		}
		return ownConstants;
	}
	
	/**
	 * @param constUsage
	 * @param list
	 * @return
	 */
	private static boolean doesConstantNameAppearInList(final String constUsage, final List<String> list) {
		
		String name = constUsage;
		
		if (constUsage.startsWith(CONSTANT_MARKER)) {
			if (constUsage.startsWith(RACE_CONSTANT)) {
				name = constUsage.substring(2);
			} else {
				name = constUsage.substring(1);
			}
		}
		
		for (final String n : list) {
			if (n.equalsIgnoreCase(name)) {
				// System.out.println("check - true - "+constUsage);
				return true;
			}
		}
		// System.out.println("check - false - "+constUsage+" - "+name);
		return false;
	}
	
	/**
	 * Returns a list with Constants defined in the given layout file.
	 *
	 * @param f
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static List<String> getLayoutsConstantDefinitions(final File f)
			throws ParserConfigurationException, SAXException, IOException {
		final DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
		dbFac.setNamespaceAware(false);
		dbFac.setValidating(false);
		dbFac.setAttribute("http://xml.org/sax/features/external-general-entities", false);
		dbFac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		dbFac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		dbFac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		dbFac.setXIncludeAware(false);
		dbFac.setExpandEntityReferences(false);
		dbFac.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		final DocumentBuilder dBuilder = dbFac.newDocumentBuilder();
		final Document doc = dBuilder.parse(f);
		
		return getLayoutsConstantDefinitions(doc);
	}
}