// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.mpq;

import com.ahli.util.SilentXmlSaxErrorHandler;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Locale;

/**
 * @author Ahli
 */
public final class XmlCompressorDom {
	private static final String AHLI_SETTING = "@setting";
	private static final String AHLI_HOTKEY = "@hotkey";
	private static final String ANY_TAGNAME = "*";
	private static final Logger logger = LogManager.getLogger(XmlCompressorDom.class);
	
	/**
	 *
	 */
	private XmlCompressorDom() {
	}
	
	/**
	 * @param cachePath
	 * @param ignoreCommentCountPerFile
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws TransformerConfigurationException
	 */
	public static void processCache(final String cachePath, final int ignoreCommentCountPerFile)
			throws ParserConfigurationException, SAXException, TransformerConfigurationException {
		
		logger.info("Compressing XML files...");
		logger.trace("cachePath: {}", () -> cachePath);
		
		final Collection<File> filesOfCache = FileUtils.listFiles(new File(cachePath), null, true);
		
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
		// provide error handler that does not print incompatible files into console
		dBuilder.setErrorHandler(new SilentXmlSaxErrorHandler());
		
		final TransformerFactory factory = TransformerFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		final Transformer transformer = factory.newTransformer();
		
		for (final File curFile : filesOfCache) {
			final Document doc;
			try (final InputStream is = new BufferedInputStream(Files.newInputStream(curFile.toPath()))) {
				
				doc = dBuilder.parse(is);
				
			} catch (final SAXParseException | IOException e) {
				if (logger.isTraceEnabled()) {
					logger.trace("Error while compressing xml.", e);
				}
				continue;
			}
			
			logger.trace("compression - processing file: {}", curFile::getPath);
			
			// process all nodes
			final NodeList nodes = doc.getElementsByTagName(ANY_TAGNAME);
			for (int i = 0, len = nodes.getLength(); i < len; ++i) {
				final Node curNode = nodes.item(i);
				
				// remove whitespace
				trimWhitespace(curNode);
			}
			
			// remove comment nodes except first one
			final Element elem = doc.getDocumentElement();
			final NodeList childNodes = elem.getChildNodes();
			removeCommentsInChildNodes(childNodes, ignoreCommentCountPerFile);
			
			// write DOM back to XML
			try {
				transformer.transform(new DOMSource(doc), new StreamResult(curFile));
			} catch (final TransformerException e) {
				logger.error("Transforming to generate XML file failed.", e);
			}
			
		}
		
	}
	
	/**
	 * @param childNodes
	 * @param ignoreCount
	 */
	private static void removeCommentsInChildNodes(final NodeList childNodes, int ignoreCount) {
		for (int i = 0, len = childNodes.getLength(); i < len; ++i) {
			final Node curNode = childNodes.item(i);
			
			if (curNode.getNodeType() == Node.COMMENT_NODE) {
				if (ignoreCount == 0) {
					
					// keep hotkeys/settings definition alive
					final Comment comment = (Comment) curNode;
					final String text = comment.getData().trim().toLowerCase(Locale.ENGLISH);
					if (!text.contains(AHLI_HOTKEY) && !text.contains(AHLI_SETTING)) {
						
						curNode.getParentNode().removeChild(curNode);
						--i;
						--len;
					}
					
				} else {
					--ignoreCount;
				}
			} else {
				removeCommentsInChildNodes(curNode.getChildNodes(), ignoreCount);
			}
		}
	}
	
	/**
	 * @param node
	 */
	public static void trimWhitespace(final Node node) {
		final NodeList childNodes = node.getChildNodes();
		for (int i = 0, len = childNodes.getLength(); i < len; ++i) {
			final Node child = childNodes.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				child.setTextContent(child.getTextContent().trim());
			}
			trimWhitespace(child);
		}
	}
	
}
