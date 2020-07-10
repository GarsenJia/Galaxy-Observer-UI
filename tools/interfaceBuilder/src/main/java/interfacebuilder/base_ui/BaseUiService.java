// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package interfacebuilder.base_ui;

import com.ahli.galaxy.game.GameData;
import com.ahli.galaxy.game.def.GameDef;
import com.ahli.galaxy.parser.UICatalogParser;
import com.ahli.galaxy.parser.XmlParserVtd;
import com.ahli.galaxy.ui.UICatalogImpl;
import com.ahli.galaxy.ui.interfaces.UICatalog;
import com.esotericsoftware.kryo.Kryo;
import interfacebuilder.InterfaceBuilderApp;
import interfacebuilder.compress.GameService;
import interfacebuilder.config.ConfigService;
import interfacebuilder.integration.FileService;
import interfacebuilder.integration.SettingsIniInterface;
import interfacebuilder.integration.kryo.KryoGameInfo;
import interfacebuilder.integration.kryo.KryoService;
import interfacebuilder.integration.log4j.StylizedTextAreaAppender;
import interfacebuilder.projects.enums.Game;
import interfacebuilder.ui.progress.appender.Appender;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.boris.pecoff4j.PE;
import org.boris.pecoff4j.ResourceDirectory;
import org.boris.pecoff4j.ResourceEntry;
import org.boris.pecoff4j.constant.ResourceType;
import org.boris.pecoff4j.io.PEParser;
import org.boris.pecoff4j.io.ResourceParser;
import org.boris.pecoff4j.resources.StringFileInfo;
import org.boris.pecoff4j.resources.StringTable;
import org.boris.pecoff4j.resources.VersionInfo;
import org.boris.pecoff4j.util.ResourceHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class BaseUiService {
	private static final Logger logger = LogManager.getLogger(BaseUiService.class);
	private static final String META_FILE_NAME = ".meta";
	
	@Autowired
	private ConfigService configService;
	@Autowired
	private GameService gameService;
	@Autowired
	private FileService fileService;
	@Autowired
	private DiscCacheService discCacheService;
	@Autowired
	private KryoService kryoService;
	
	/**
	 * Checks if the specified game's baseUI is older than the game files.
	 *
	 * @param game
	 * @param usePtr
	 * @return true, if outdated
	 */
	public boolean isOutdated(final Game game, final boolean usePtr) throws IOException {
		final GameDef gameDef = gameService.getGameDef(game);
		final File gameBaseUI = new File(configService.getBaseUiPath(gameDef));
		
		if (!gameBaseUI.exists() || fileService.isEmptyDirectory(gameBaseUI)) {
			return true;
		}
		
		final File baseUiMetaFileDir = new File(configService.getBaseUiPath(gameDef));
		final KryoGameInfo baseUiInfo;
		try {
			baseUiInfo = readMetaFile(baseUiMetaFileDir);
			if (baseUiInfo == null) {
				return true;
			}
		} catch (final IOException e) {
			final String msg = "Failed to read Game Info from extracted base UI.";
			logger.warn(msg);
			logger.trace(msg, e);
			return true;
		}
		final int[] versionBaseUi = baseUiInfo.version();
		final int[] versionExe = getVersion(gameDef, usePtr);
		boolean isUpToDate = true;
		for (int i = 0; i < versionExe.length && isUpToDate; ++i) {
			isUpToDate = versionExe[i] <= versionBaseUi[i];
		}
		if (logger.isTraceEnabled()) {
			logger.trace(
					"Exe version check - exe=" + Arrays.toString(versionExe) + " - " + Arrays.toString(versionBaseUi) +
							" - upToDate=" + isUpToDate);
		}
		
		return !isUpToDate;
	}
	
	private KryoGameInfo readMetaFile(final File directory) throws IOException {
		final Path path = Paths.get(directory.getAbsolutePath(), META_FILE_NAME);
		if (Files.exists(path)) {
			final Kryo kryo = kryoService.getKryoForBaseUiMetaFile();
			final List<Class<?>> payloadClasses = new ArrayList<>();
			payloadClasses.add(KryoGameInfo.class);
			return (KryoGameInfo) kryoService.get(path, payloadClasses, kryo).get(0);
		}
		return null;
	}
	
	public int[] getVersion(final GameDef gameDef, final boolean isPtr) {
		final int[] versions = new int[4];
		final Path path = Paths.get(gameService.getGameDirPath(gameDef, isPtr), gameDef.supportDirectoryX64(),
				gameDef.switcherExeNameX64());
		try {
			final PE pe = PEParser.parse(path.toFile());
			final ResourceDirectory rd = pe.getImageData().getResourceTable();
			
			final ResourceEntry[] entries = ResourceHelper.findResources(rd, ResourceType.VERSION_INFO);
			
			for (final ResourceEntry entry : entries) {
				final byte[] data = entry.getData();
				final VersionInfo version = ResourceParser.readVersionInfo(data);
				
				final StringFileInfo strings = version.getStringFileInfo();
				final StringTable table = strings.getTable(0);
				for (int j = 0; j < table.getCount(); ++j) {
					final String key = table.getString(j).getKey();
					if ("FileVersion".equals(key)) {
						final String value = table.getString(j).getValue();
						logger.trace("found FileVersion={}", () -> value);
						
						final String[] parts = value.split("\\.");
						for (int k = 0; k < 4; ++k) {
							versions[k] = Integer.parseInt(parts[k]);
						}
						return versions;
					}
				}
			}
		} catch (final IOException e) {
			logger.error("Error attempting to parse FileVersion from game's exe: ", e);
		}
		return versions;
	}
	
	/**
	 * Creates Tasks that will extract the base UI for a specified game.
	 *
	 * @param game
	 * @param usePtr
	 * @param outputs
	 * @return list of executable tasks
	 */
	public List<ForkJoinTask<Void>> extract(final Game game, final boolean usePtr, final Appender[] outputs) {
		logger.info("Extracting baseUI for {}", game);
		
		final GameDef gameDef = gameService.getGameDef(game);
		final File destination = new File(configService.getBaseUiPath(gameDef));
		
		try {
			if (!destination.exists() && !destination.mkdirs()) {
				logger.error("Directory {} could not be created.", destination);
				return null;
			}
			fileService.cleanDirectory(destination);
			discCacheService.remove(gameDef.name(), usePtr);
		} catch (final IOException e) {
			logger.error(String.format("Directory %s could not be cleaned.", destination), e);
			return null;
		}
		
		final List<ForkJoinTask<Void>> tasks = new ArrayList<>(4);
		final File extractorExe = configService.getCascExtractorExeFile();
		final String gamePath = gameService.getGameDirPath(gameDef, usePtr);
		final String[] queryMasks = getQueryMasks(game);
		int i = 0;
		for (final String mask : queryMasks) {
			final Appender outputAppender = outputs[i];
			++i;
			final ForkJoinTask<Void> task = new RecursiveAction() {
				@Override
				protected void compute() {
					try {
						if (extract(extractorExe, gamePath, mask, destination, outputAppender)) {
							Thread.sleep(50);
							if (extract(extractorExe, gamePath, mask, destination, outputAppender)) {
								logger.warn(
										"Extraction failed due to a file access. Try closing the Battle.net App, if it is running and this fails to extract all files.");
							}
						}
					} catch (final IOException e) {
						logger.error("Extracting files from CASC via CascExtractor failed.", e);
					} catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			};
			tasks.add(task);
		}
		
		final ForkJoinTask<Void> task = new RecursiveAction() {
			@Override
			protected void compute() {
				final int[] version = getVersion(gameDef, usePtr);
				try {
					writeToMetaFile(destination, gameDef.name(), version, usePtr);
				} catch (final IOException e) {
					logger.error("Failed to write metafile: ", e);
				}
			}
		};
		tasks.add(task);
		
		return tasks;
	}
	
	/**
	 * @param game
	 * @return
	 */
	public static String[] getQueryMasks(final Game game) {
		return switch (game) {
			case SC2 -> new String[] { "*.SC2Layout", "*Assets.txt", "*.SC2Style" };
			case HEROES -> new String[] { "*.StormLayout", "*Assets.txt", "*.StormStyle" };
		};
	}
	
	/**
	 * @param extractorExe
	 * @param gamePath
	 * @param mask
	 * @param destination
	 * @param out
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static boolean extract(final File extractorExe, final String gamePath, final String mask,
			final File destination,
			final Appender out) throws IOException, InterruptedException {
		final ProcessBuilder pb =
				new ProcessBuilder(extractorExe.getAbsolutePath(), gamePath + File.separator, mask,
						destination + File.separator);
		// put error and normal output into the same stream
		pb.redirectErrorStream(true);
		
		boolean retry = false;
		final Process process = pb.start();
		// empty output buffers and print to console
		try (final InputStream is = process.getInputStream()) {
			do {
				Thread.sleep(50);
				final String log = IOUtils.toString(is, Charset.defaultCharset());
				if (log.contains("Unhandled Exception: System.IO.IOException: The process cannot access the file")) {
					retry = true;
				} else {
					if (out != null) {
						out.append(log);
					} else {
						logger.info(log);
					}
				}
			} while (process.isAlive());
		}
		return retry;
	}
	
	private void writeToMetaFile(final File directory, final String gameName, final int[] version, final boolean isPtr)
			throws IOException {
		final Path path = Paths.get(directory.getAbsolutePath(), META_FILE_NAME);
		final KryoGameInfo metaInfo = new KryoGameInfo(version, gameName, isPtr);
		final List<Object> payload = new ArrayList<>();
		payload.add(metaInfo);
		final Kryo kryo = kryoService.getKryoForBaseUiMetaFile();
		kryoService.put(path, payload, kryo);
	}
	
	/**
	 * Parses the baseUI of the GameData, if that is required for the settings.
	 *
	 * @param gameData
	 * @param useCmdLineSettings
	 */
	public void parseBaseUiIfNecessary(final GameData gameData, final boolean useCmdLineSettings) {
		final boolean verifyLayout;
		final SettingsIniInterface settings = configService.getIniSettings();
		if (useCmdLineSettings) {
			verifyLayout = settings.isCmdLineVerifyLayout();
		} else {
			verifyLayout = settings.isGuiVerifyLayout();
		}
		if (gameData.getUiCatalog() == null && verifyLayout) {
			parseBaseUI(gameData);
		}
	}
	
	/**
	 * Parses the baseUI of the specified game. The parsing of the baseUI is synchronized.
	 *
	 * @param gameData
	 * 		game whose default UI is parsed
	 */
	public void parseBaseUI(final GameData gameData) {
		// lock per game
		synchronized (gameData.getGameDef().name()) {
			UICatalog uiCatalog = gameData.getUiCatalog();
			final String gameName = gameData.getGameDef().name();
			if (uiCatalog != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Aborting parsing baseUI for '{}' as was already parsed.", gameName);
				}
			} else {
				final long startTime = System.currentTimeMillis();
				logger.info("Loading baseUI for {}", gameName);
				boolean needToParseAgain = true;
				
				/*!(game.getNewGameDef() instanceof SC2GameDef) &&*/
				final String baseUiPath = configService.getBaseUiPath(gameData.getGameDef());
				boolean isPtr = false;
				try {
					isPtr = isPtr(new File(baseUiPath));
				} catch (final IOException e) {
					// do nothing
					if (logger.isTraceEnabled()) {
						logger.trace("Ignoring error in isPtr() check on baseUiPath.", e);
					}
				}
				try {
					if (cacheIsUpToDateCheckException(gameData.getGameDef(), isPtr)) {
						// load from cache
						uiCatalog = discCacheService.getCachedBaseUi(gameName, isPtr);
						gameData.setUiCatalog(uiCatalog);
						needToParseAgain = false;
						if (logger.isTraceEnabled()) {
							logger.trace("Loaded baseUI for '{}' from cache", gameName);
						}
					}
				} catch (final IOException e) {
					logger.warn("ERROR: loading cached base UI failed.", e);
				}
				if (needToParseAgain) {
					// parse baseUI
					uiCatalog = new UICatalogImpl();
					uiCatalog.setParser(new UICatalogParser(uiCatalog, new XmlParserVtd(), true));
					final var app = InterfaceBuilderApp.getInstance();
					app.printInfoLogMessageToGeneral("Starting to parse base " + gameName + " UI.");
					app.addThreadLoggerTab(Thread.currentThread().getName(),
							gameData.getGameDef().nameHandle() + "UI", false);
					final String gameDir = configService.getBaseUiPath(gameData.getGameDef()) + File.separator +
							gameData.getGameDef().modsSubDirectory();
					try {
						final WildcardFileFilter fileFilter =
								new WildcardFileFilter("descindex.*layout", IOCase.INSENSITIVE);
						for (final String modOrDir : gameData.getGameDef().coreModsOrDirectories()) {
							
							final File directory = new File(gameDir + File.separator + modOrDir);
							if (!directory.exists() || !directory.isDirectory()) {
								throw new IOException("BaseUI does not exist.");
							}
							
							final Collection<File> descIndexFiles =
									FileUtils.listFiles(directory, fileFilter, TrueFileFilter.INSTANCE);
							logger.info("number of descIndexFiles found: {}", descIndexFiles.size());
							
							for (final File descIndexFile : descIndexFiles) {
								logger.info("parsing descIndexFile '{}'", descIndexFile.getPath());
								uiCatalog.processDescIndex(descIndexFile, gameData.getGameDef().defaultRaceId(),
										gameData.getGameDef().defaultConsoleSkinId());
							}
						}
						uiCatalog.postProcessParsing();
						gameData.setUiCatalog(uiCatalog);
						if (logger.isTraceEnabled()) {
							logger.trace("Parsed BaseUI for {}", gameName);
						}
					} catch (final SAXException | IOException | ParserConfigurationException e) {
						logger.error("ERROR parsing base UI catalog for '" + gameName + "'.", e);
					} catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						uiCatalog.setParser(null);
					}
					final String msg = "Finished parsing base UI for " + gameName + ".";
					logger.info(msg);
					app.printInfoLogMessageToGeneral(msg);
					try {
						discCacheService.put(uiCatalog, gameName, isPtr, getVersion(gameData.getGameDef(), isPtr));
					} catch (final IOException e) {
						logger.error("ERROR when creating cache file of UI", e);
					}
				}
				final long executionTime = (System.currentTimeMillis() - startTime);
				logger.info("Loading BaseUI for {} took {}ms.", gameName, executionTime);
				if (needToParseAgain) {
					// set result in UI
					StylizedTextAreaAppender.finishedWork(Thread.currentThread().getName(), true, 50);
				}
			}
		}
	}
	
	/**
	 * Checks if the baseUiDirectory is PTR.
	 *
	 * @param baseUiDirectory
	 * @return true if PTR; false if not or no file is existing
	 * @throws IOException
	 */
	public boolean isPtr(final File baseUiDirectory) throws IOException {
		final KryoGameInfo kryoGameInfo = readMetaFile(baseUiDirectory);
		return kryoGameInfo != null && kryoGameInfo.isPtr();
	}
	
	public boolean cacheIsUpToDateCheckException(final GameDef gameDef, final boolean usePtr) {
		try {
			return cacheIsUpToDate(gameDef, usePtr);
		} catch (final NoSuchFileException e) {
			logger.trace("No cache exists for " + gameDef.name(), e);
		} catch (final IOException e) {
			logger.info("Failed to check cache status of " + gameDef.name() + ":", e);
		}
		return false;
	}
	
	public boolean cacheIsUpToDate(final GameDef gameDef, final boolean usePtr) throws IOException {
		final File baseUiMetaFileDir = new File(configService.getBaseUiPath(gameDef));
		final Path cacheFilePath = discCacheService.getCacheFilePath(gameDef.name(), usePtr);
		return cacheIsUpToDate(cacheFilePath, baseUiMetaFileDir);
	}
	
	/**
	 * @param cacheFile
	 * @param metaFileDir
	 * @return
	 */
	public boolean cacheIsUpToDate(final Path cacheFile, final File metaFileDir) throws IOException {
		// no cache -> not up to date
		if (!Files.exists(cacheFile)) {
			return false;
		}
		
		final KryoGameInfo cacheInfo = discCacheService.getCachedBaseUiInfo(cacheFile);
		final int[] versionCache = cacheInfo.version();
		
		final KryoGameInfo baseUiInfo = readMetaFile(metaFileDir);
		if (baseUiInfo == null) {
			return false;
		}
		final int[] versionBaseUi = baseUiInfo.version();
		
		boolean isUpToDate = true;
		for (int i = 0; i < versionCache.length && isUpToDate; ++i) {
			isUpToDate = versionCache[i] == versionBaseUi[i];
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Cache and baseUI versions match: {}", isUpToDate);
		}
		return isUpToDate;
	}
	
	public boolean isHeroesPtrActive() {
		final File baseUiMetaFileDir = new File(configService.getBaseUiPath(gameService.getGameDef(Game.HEROES)));
		try {
			final KryoGameInfo baseUiInfo = readMetaFile(baseUiMetaFileDir);
			if (baseUiInfo != null) {
				return baseUiInfo.isPtr();
			}
		} catch (final IOException e) {
			logger.error("ERROR while checking if ptr is active via baseUI cache file", e);
		}
		return false;
	}
}
