package com.ahli.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * Helper class for the executable jar file. It is capable of determining the position.
 *
 * @author Ahli
 */
public final class JarHelper {
	private static final Logger logger = LogManager.getLogger(JarHelper.class); // $NON-NLS-1$
	
	/**
	 * Disabled Constructor.
	 */
	private JarHelper() {
	}
	
	/**
	 * from stackoverflow because why doesn't java have this functionality? It's not like nobody would need that or it
	 * is trivial to create...
	 *
	 * @param aclass
	 * @return File at base path
	 */
	public static File getJarDir(final Class<?> aclass) {
		logger.debug("_FINDING JAR'S PATH"); //$NON-NLS-1$
		
		// ATTEMPT #1
		final File f = new File(System.getProperty("java.class.path")); //$NON-NLS-1$
		final File dir = f.getAbsoluteFile().getParentFile();
		String str = dir.toString();
		logger.debug("Attempt#1 java.class.path: " + str); //$NON-NLS-1$
		
		// check if started in eclipse
		if (str.contains(File.separator + "target" + File.separator + "classes;")) { //$NON-NLS-1$ //$NON-NLS-2$
			// get current working directory
			final URI uri = new File(".").toURI(); //$NON-NLS-1$
			// results in: "file:/D:/GalaxyObsUI/dev/./"
			// but maybe results in something completely different like
			// notepad++'s directory...
			
			str = uri.getPath();
			logger.debug("_URI path:" + str); //$NON-NLS-1$
			
			// fix for intellij
			if (str.endsWith("/tools/./")) {
				str = str.substring(0, str.length() - 2);
				final String dirStr = dir.toString();
				final String tools = "\\tools\\";
				str += dirStr.substring(dirStr.indexOf(tools) + tools.length(), dirStr.indexOf("\\target\\"));
				str = str.replace('\\', '/');
			}
			
			if (str.startsWith("file:/")) { //$NON-NLS-1$
				str = str.substring(6);
			}
			if (str.startsWith("/")) { //$NON-NLS-1$
				str = str.substring(1);
			}
			if (str.endsWith("/./")) { //$NON-NLS-1$
				str = str.substring(0, str.length() - 3);
			}
			
			final URL url = aclass.getProtectionDomain().getCodeSource().getLocation();
			// class returns "rsrc:./", if 2nd option during jar export was
			// chosen
			if (!url.toString().startsWith("rsrc:./")) { //$NON-NLS-1$
				// wild guess that we are in test environment
				str += "/testEnv/dev/"; //$NON-NLS-1$
				logger.debug("assuming Test Environment: " + str); //$NON-NLS-1$
			}
			
		} else {
			if (str.contains(".jar;")) {
				str = str.substring(0, str.indexOf(".jar"));
				logger.debug("path before .jar: " + str);
				str = str.substring(0, str.lastIndexOf(File.separator));
			}
			
		}
		logger.debug("_RESULT PATH: " + str); //$NON-NLS-1$
		
		return new File(str);
	}
	
}
