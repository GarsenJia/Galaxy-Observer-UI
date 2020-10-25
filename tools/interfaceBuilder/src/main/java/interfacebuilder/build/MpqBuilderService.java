// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package interfacebuilder.build;

import com.ahli.galaxy.ModData;
import com.ahli.galaxy.archive.ComponentsListReaderDom;
import com.ahli.galaxy.archive.DescIndexData;
import com.ahli.galaxy.game.GameData;
import com.ahli.galaxy.game.def.abstracts.GameDef;
import com.ahli.galaxy.ui.DescIndexReader;
import com.ahli.mpq.MpqEditorInterface;
import com.ahli.mpq.MpqException;
import com.ahli.mpq.mpqeditor.MpqEditorCompression;
import interfacebuilder.InterfaceBuilderApp;
import interfacebuilder.base_ui.BaseUiService;
import interfacebuilder.compile.CompileService;
import interfacebuilder.compress.RuleSet;
import interfacebuilder.config.ConfigService;
import interfacebuilder.integration.FileService;
import interfacebuilder.integration.SettingsIniInterface;
import interfacebuilder.projects.Project;
import interfacebuilder.projects.ProjectService;
import interfacebuilder.projects.enums.Game;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.time.LocalDateTime;
import java.util.List;

public class MpqBuilderService {
	private static final Logger logger = LogManager.getLogger(MpqBuilderService.class);
	
	@Autowired
	private ConfigService configService;
	@Autowired
	private CompileService compileService;
	@Autowired
	private FileService fileService;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private BaseUiService baseUiService;
	@Autowired
	@Qualifier("sc2BaseGameData")
	private GameData sc2BaseGameData;
	@Autowired
	@Qualifier("heroesBaseGameData")
	private GameData heroesBaseGameData;
	
	/**
	 * Schedules a task to find and build a project based on the specified path.
	 *
	 * @param path
	 */
	public void build(final String path) {
		final File f = new File(path);
		final Project project;
		
		final List<Project> projectsOfPath = projectService.getProjectsOfPath(path);
		if (projectsOfPath.isEmpty()) {
			final Game game;
			if (projectService.pathContainsCompileableForGame(path, heroesBaseGameData)) {
				game = Game.HEROES;
			} else if (projectService.pathContainsCompileableForGame(path, sc2BaseGameData)) {
				game = Game.SC2;
			} else {
				throw new IllegalArgumentException("Specified path '" + path + "' did not contain any project.");
			}
			project = new Project(f.getName(), f.getAbsolutePath(), game);
		} else {
			project = projectsOfPath.get(0);
		}
		
		build(project, true);
	}
	
	/**
	 * Schedules a task to build the mpq archive file for a project.
	 *
	 * @param project
	 */
	public void build(final Project project, final boolean useCmdLineSettings) {
		final BuildTask task = new BuildTask(project, useCmdLineSettings, this, baseUiService);
		InterfaceBuilderApp.getInstance().getExecutor().execute(task);
	}
	
	/**
	 * Returns the GameData definition for the specified game.
	 *
	 * @param game
	 * @return
	 */
	public GameData getGameData(final Game game) {
		return switch (game) {
			case SC2 -> sc2BaseGameData;
			case HEROES -> heroesBaseGameData;
		};
	}
	
	/**
	 * Builds a specific Interface in a Build Thread.
	 *
	 * @param interfaceDirectory
	 * 		folder of the interface file to be built
	 * @param game
	 * @param useCmdLineSettings
	 */
	void buildSpecificUI(final File interfaceDirectory, final GameData game, final boolean useCmdLineSettings,
			final Project project) {
		if (InterfaceBuilderApp.getInstance().getExecutor().isShutdown()) {
			logger.error("ERROR: Executor shut down. Skipping building a UI...");
			return;
		}
		if (!interfaceDirectory.exists() || !interfaceDirectory.isDirectory()) {
			logger.error("ERROR: Can't build UI from file '{}', expected an existing directory.", interfaceDirectory);
			return;
		}
		final boolean verifyLayout;
		final SettingsIniInterface settings = configService.getIniSettings();
		if (useCmdLineSettings) {
			verifyLayout = settings.isCmdLineVerifyLayout();
		} else {
			verifyLayout = settings.isGuiVerifyLayout();
		}
		if (game.getUiCatalog() == null && verifyLayout) {
			// parse default UI
			throw new IllegalStateException(
					String.format("Base UI of game '%s' has not been parsed.", game.getGameDef().getName()));
		}
		
		// create tasks for the worker pool
		try {
			InterfaceBuilderApp.getInstance()
					.addThreadLoggerTab(Thread.currentThread().getName(), interfaceDirectory.getName(), false);
			// create unique cache path
			final MpqEditorInterface threadsMpqInterface =
					new MpqEditorInterface(configService.getMpqCachePath() + Thread.currentThread().getId(),
							configService.getMpqEditorPath());
			
			// work
			final boolean compressXml;
			final int compressMpqSetting;
			final boolean buildUnprotectedToo;
			final boolean repairLayoutOrder;
			final boolean verifyXml;
			if (useCmdLineSettings) {
				compressXml = settings.isCmdLineCompressXml();
				compressMpqSetting = settings.getCmdLineCompressMpq();
				buildUnprotectedToo = settings.isCmdLineBuildUnprotectedToo();
				repairLayoutOrder = settings.isCmdLineRepairLayoutOrder();
				verifyXml = settings.isCmdLineVerifyXml();
			} else {
				compressXml = settings.isGuiCompressXml();
				compressMpqSetting = settings.getGuiCompressMpq();
				buildUnprotectedToo = settings.isGuiBuildUnprotectedToo();
				repairLayoutOrder = settings.isGuiRepairLayoutOrder();
				verifyXml = settings.isGuiVerifyXml();
			}
			
			// load best compression ruleset
			if (compressXml && compressMpqSetting == 2) {
				final RuleSet ruleSet = projectService.fetchBestCompressionRuleSet(project);
				if (ruleSet != null) {
					// TODO verify/expand ruleset
					threadsMpqInterface.setCustomCompressionRules(ruleSet.getCompressionRules());
				}
			}
			threadsMpqInterface.clearCacheExtractedMpq();
			buildFile(interfaceDirectory, game, threadsMpqInterface, compressXml, compressMpqSetting,
					buildUnprotectedToo, repairLayoutOrder, verifyLayout, verifyXml, project);
			threadsMpqInterface.clearCacheExtractedMpq();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (final IOException e) {
			logger.error("ERROR: Exception while building UIs.", e);
		} catch (final Exception e) {
			logger.fatal("FATAL ERROR: ", e);
		}
	}
	
	/**
	 * Builds MPQ Archive File. Run this in its own thread! Conditions: - Specified MpqInterface requires a unique cache
	 * path for multithreading.
	 *
	 * @param sourceFile
	 * 		folder location
	 * @param game
	 * 		the game data with game definition
	 * @param mpqi
	 * 		MpqInterface with unique cache path
	 * @param compressXml
	 * @param compressMpq
	 * @param buildUnprotectedToo
	 * @param repairLayoutOrder
	 * @param verifyLayout
	 * @param verifyXml
	 * @param project
	 * @throws IOException
	 * 		when something goes wrong
	 * @throws InterruptedException
	 */
	private void buildFile(final File sourceFile, final GameData game, final MpqEditorInterface mpqi,
			final boolean compressXml, final int compressMpq, final boolean buildUnprotectedToo,
			final boolean repairLayoutOrder, final boolean verifyLayout, final boolean verifyXml, final Project project)
			throws IOException, InterruptedException {
		InterfaceBuilderApp.getInstance().printInfoLogMessageToGeneral(sourceFile.getName() + " started construction.");
		
		final GameDef gameDef = game.getGameDef();
		
		final String targetPath =
				configService.getDocumentsPath() + File.separator + gameDef.getDocumentsGameDirectoryName() +
						File.separator + gameDef.getDocumentsInterfaceSubdirectoryName();
		
		// init mod data
		final ModData mod = new ModData(game);
		mod.setSourceDirectory(sourceFile);
		final File targetFile = new File(targetPath);
		mod.setTargetFile(targetFile);
		
		// get and create cache
		final File cache = new File(mpqi.getMpqCachePath());
		if (!cache.exists() && !cache.mkdirs()) {
			final String msg = "Unable to create cache directory.";
			logger.error(msg);
			throw new IOException(msg);
		}
		int cacheClearAttempts;
		for (cacheClearAttempts = 0; cacheClearAttempts <= 100; cacheClearAttempts++) {
			if (!mpqi.clearCacheExtractedMpq()) {
				// sleep and hope the file gets released soon
				Thread.sleep(500);
			} else {
				// success
				break;
			}
		}
		if (cacheClearAttempts > 100) {
			final String msg = "ERROR: Cache could not be cleared";
			logger.error(msg);
			return;
		}
		mod.setMpqCacheDirectory(cache);
		// put files into cache
		int copyAttempts;
		for (copyAttempts = 0; copyAttempts <= 100; copyAttempts++) {
			try {
				fileService.copyFileOrDirectory(sourceFile, cache);
				break;
			} catch (final FileSystemException e) {
				if (copyAttempts == 0) {
					logger.warn("Attempt to copy directory failed.", e);
				} else if (copyAttempts >= 100) {
					final String msg = "Unable to copy directory after 100 copy attempts: " + e.getMessage();
					logger.error(msg, e);
					throw new FileSystemException(msg);
				}
				// sleep and hope the file gets released soon
				Thread.sleep(500);
			} catch (final IOException e) {
				final String msg = "Unable to copy directory";
				logger.error(msg, e);
			}
		}
		if (copyAttempts > 100) {
			// copy keeps failing -> abort
			final String msg = "Above code did not throw exception about copy attempt threshold reached.";
			logger.error(msg);
			throw new IOException(msg);
		}
		
		final File componentListFile = mpqi.getComponentListFile();
		mod.setComponentListFile(componentListFile);
		
		final DescIndexData descIndexData = new DescIndexData(mpqi);
		mod.setDescIndexData(descIndexData);
		
		try {
			descIndexData
					.setDescIndexPathAndClear(ComponentsListReaderDom.getDescIndexPath(componentListFile, gameDef));
		} catch (final ParserConfigurationException | SAXException | IOException e) {
			final String msg = "ERROR: unable to read DescIndex path.";
			logger.error(msg, e);
			throw new IOException(msg, e);
		}
		
		final File descIndexFile = mpqi.getFilePathFromMpq(descIndexData.getDescIndexIntPath()).toFile();
		try {
			descIndexData.addLayoutIntPath(DescIndexReader.getLayoutPathList(descIndexFile, false));
		} catch (final SAXException | ParserConfigurationException | IOException | MpqException e) {
			logger.error("unable to read Layout paths", e);
		}
		
		logger.info("Compiling... {}", sourceFile.getName());
		
		// perform checks/improvements on code
		compileService.compile(mod, configService.getRaceId(), repairLayoutOrder, verifyLayout, verifyXml,
				configService.getConsoleSkinId());
		
		logger.info("Building... {}", sourceFile.getName());
		
		try {
			mpqi.buildMpq(targetPath, sourceFile.getName(), compressXml, getCompressionModeOfSetting(compressMpq),
					buildUnprotectedToo);
			
			project.setLastBuildDateTime(LocalDateTime.now());
			final long size = new File(targetPath + File.separator + sourceFile.getName()).length();
			project.setLastBuildSize(size);
			logger.info("Finished building... {}. Size: {}kb", sourceFile.getName(), size / 1024);
			projectService.saveProject(project);
			InterfaceBuilderApp.getInstance()
					.printInfoLogMessageToGeneral(sourceFile.getName() + " finished construction.");
		} catch (final IOException | MpqException e) {
			logger.error("ERROR: unable to construct final Interface file.", e);
			InterfaceBuilderApp.getInstance()
					.printErrorLogMessageToGeneral(sourceFile.getName() + " could not be created.");
		}
	}
	
	/**
	 * @param compressMpqSetting
	 * @return
	 */
	private static MpqEditorCompression getCompressionModeOfSetting(final int compressMpqSetting) {
		return switch (compressMpqSetting) {
			case 0 -> MpqEditorCompression.NONE;
			case 1 -> MpqEditorCompression.BLIZZARD_SC2_HEROES;
			case 2 -> MpqEditorCompression.CUSTOM;
			case 3 -> MpqEditorCompression.SYSTEM_DEFAULT;
			default -> throw new IllegalArgumentException("Unsupported mpq compression mode.");
		};
	}
}
