// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.galaxy.ui;

import com.ahli.util.XmlDomHelper;
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
 * Reads a a Desc Index File.
 *
 * @author Ahli
 */
public final class DescIndexReader {
	private static final Logger logger = LogManager.getLogger(DescIndexReader.class);
	
	/**
	 *
	 */
	private DescIndexReader() {
	}
	
	/**
	 * Grabs all layout file paths from a given descIndex file.
	 *
	 * @param f
	 * 		descIndex file
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static List<String> getLayoutPathList(final File f, final Mode mode)
			throws SAXException, IOException, ParserConfigurationException {
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
		logger.trace("reading layouts from descIndexFile: {}", f);
		final Document doc = dBuilder.parse(f);
		
		// must be in a DataComponent node
		final NodeList nodeList = doc.getElementsByTagName("*");
		final ArrayList<String> list = new ArrayList<>(nodeList.getLength());
		for (int i = 0, len = nodeList.getLength(); i < len; ++i) {
			final Node node = nodeList.item(i);
			if ("Include".equalsIgnoreCase(node.getNodeName())) {
				final NamedNodeMap attributes = node.getAttributes();
				
				// ignore requiredtoload if desired
				if ((mode == Mode.ONLY_LOADABLE && XmlDomHelper.isFailingRequiredToLoad(attributes)) ||
						(mode == Mode.ONLY_UNLOADABLE && !XmlDomHelper.isFailingRequiredToLoad(attributes))) {
					continue;
				}
				
				final String path = attributes.item(0).getNodeValue();
				list.add(path);
				logger.trace("Adding layout path to layoutPathList: {}", path);
			}
		}
		return list;
	}
	
	public enum Mode {
		ONLY_LOADABLE, ONLY_UNLOADABLE, ALL
	}
}
