package application.integration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Interface Class for the Settings .ini file.
 * 
 * @author Ahli
 *
 */
public class SettingsIniInterface {
	static Logger LOGGER = LogManager.getLogger(SettingsIniInterface.class);
	
	private String settingsFilePath = "";
	private String SC2_Path = "", HEROES_Path = "", HEROES_PTR_Path = "";
	private boolean ptrActive = false, SC2_64bit = false, HEROES_64bit = false, HEROES_PTR_64bit = false,
			HEROES_protectMPQ = false, SC2_protectMPQ = false, buildUnprotectedToo = false;
	
	public SettingsIniInterface(final String settingsFilePath) {
		this.settingsFilePath = settingsFilePath;
	}
	
	public void setSettingsFilePath(final String path) {
		settingsFilePath = path;
	}
	
	public String getSettingsFilePath() {
		return settingsFilePath;
	}
	
	/**
	 * Read all Settings from the Settings file.
	 * 
	 * @throws FileNotFoundException
	 */
	public void readSettingsFromFile() throws FileNotFoundException {
		final InputStreamReader is = new InputStreamReader(new FileInputStream(settingsFilePath),
				StandardCharsets.UTF_8);
		final BufferedReader br = new BufferedReader(is);
		String line = "";
		try {
			while ((line = br.readLine()) != null) {
				parseLine(line);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (final IOException e) {
			}
		}
	}
	
	/**
	 * Parse a line from the Settings file.
	 * 
	 * @param line
	 */
	private void parseLine(final String line) {
		String val = "";
		if (line.startsWith("Heroes_Path")) {
			val = getValFromIniLine(line);
			HEROES_Path = val;
		} else if (line.startsWith("HeroesPTR_Path")) {
			val = getValFromIniLine(line);
			HEROES_PTR_Path = val;
		} else if (line.startsWith("StarCraft2_Path")) {
			val = getValFromIniLine(line);
			SC2_Path = val;
		} else if (line.startsWith("PTRactive")) {
			val = getValFromIniLine(line);
			ptrActive = Boolean.parseBoolean(val);
		} else if (line.startsWith("Heroes_use64bit")) {
			val = getValFromIniLine(line);
			HEROES_64bit = Boolean.parseBoolean(val);
		} else if (line.startsWith("HeroesPTR_use64bit")) {
			val = getValFromIniLine(line);
			HEROES_PTR_64bit = Boolean.parseBoolean(val);
		} else if (line.startsWith("StarCraft2_use64bit")) {
			val = getValFromIniLine(line);
			SC2_64bit = Boolean.parseBoolean(val);
		} else if (line.startsWith("Heroes_protectMPQ")) {
			val = getValFromIniLine(line);
			HEROES_protectMPQ = Boolean.parseBoolean(val);
		} else if (line.startsWith("StarCraft2_protectMPQ")) {
			val = getValFromIniLine(line);
			SC2_protectMPQ = Boolean.parseBoolean(val);
		} else if (line.startsWith("buildUnprotectedToo")) {
			val = getValFromIniLine(line);
			buildUnprotectedToo = Boolean.parseBoolean(val);
		}
	}
	
	/**
	 * Reads the Value from a line from the Settings file.
	 * 
	 * @param line
	 * @return
	 */
	private String getValFromIniLine(final String line) {
		return line.substring(line.indexOf('=') + 1);
	}
	
	public String getSC2Path() {
		return SC2_Path;
	}
	
	public String getHeroesPath() {
		return HEROES_Path;
	}
	
	public String getHeroesPtrPath() {
		return HEROES_PTR_Path;
	}
	
	public boolean isPtrActive() {
		return ptrActive;
	}
	
	public boolean isSC264bit() {
		return SC2_64bit;
	}
	
	public boolean isHeroesPtr64bit() {
		return HEROES_PTR_64bit;
	}
	
	public boolean isHeroes64bit() {
		return HEROES_64bit;
	}
	
	public boolean isHeroesProtectMPQ() {
		LOGGER.debug("Heroes protection is: " + HEROES_protectMPQ);
		return HEROES_protectMPQ;
	}
	
	public boolean isSC2ProtectMPQ() {
		LOGGER.debug("SC2 protection is: " + SC2_protectMPQ);
		return SC2_protectMPQ;
	}
	
	public boolean isBuildUnprotectedToo() {
		return buildUnprotectedToo;
	}
}
