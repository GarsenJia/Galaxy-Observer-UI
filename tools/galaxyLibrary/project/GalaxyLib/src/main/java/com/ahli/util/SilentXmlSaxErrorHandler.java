// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Error Handler for XML parsing that is completely silent.
 *
 * @author Ahli
 */
public class SilentXmlSaxErrorHandler implements ErrorHandler {
	
	public SilentXmlSaxErrorHandler() {
		// nothing to do
	}
	
	@Override
	public void warning(final SAXParseException e) {
		// do nothing to be silent
	}
	
	@Override
	public void fatalError(final SAXParseException e) throws SAXException {
		throw e;
	}
	
	@Override
	public void error(final SAXParseException e) throws SAXException {
		throw e;
	}
	
}
