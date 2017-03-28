package application.baseUI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author Ahli
 *
 */
public class UITemplate {
	private final static Logger LOGGER = LogManager.getLogger(UITemplate.class);

	private String fileName = "";
	private UIElement element = null;
	private boolean isLocked = false;

	/**
	 * 
	 * @param fileName
	 * @param frame
	 */
	public UITemplate(String fileName, UIElement element) {
		this.fileName = fileName;
		this.element = element;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 *            the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return the element
	 */
	public UIElement getElement() {
		return element;
	}

	/**
	 * @param frame
	 *            the frame to set
	 */
	public void setElement(UIElement element) {
		this.element = element;
	}

	/**
	 * @return the isLocked
	 */
	public boolean isLocked() {
		return isLocked;
	}

	/**
	 * @param isLocked
	 *            the isLocked to set
	 */
	public void setLocked(boolean isLocked) {
		this.isLocked = isLocked;
	}

	/**
	 * 
	 * @param path
	 * @return
	 */
	public UIElement receiveFrameFromPath(String path){
		LOGGER.debug("receive Frame from path: "+path);
		LOGGER.debug("template's element name: "+element.getName());
		String curName = UIElement.getLeftPathLevel(path);
		LOGGER.debug("searched name: "+curName);
		if(curName.equalsIgnoreCase(element.getName())){
			String newPath = UIElement.removeLeftPathLevel(path);
			LOGGER.debug("match! new Path: "+newPath);
			
			return element.receiveFrameFromPath(newPath);
		}
		LOGGER.debug("did not find template: "+path);
		return null;
	}
}
