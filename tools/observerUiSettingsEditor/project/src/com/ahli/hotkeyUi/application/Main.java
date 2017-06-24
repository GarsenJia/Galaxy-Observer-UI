package com.ahli.hotkeyUi.application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.ahli.hotkeyUi.application.controller.MenuBarController;
import com.ahli.hotkeyUi.application.controller.TabsController;
import com.ahli.hotkeyUi.application.galaxy.ComponentsListReader;
import com.ahli.hotkeyUi.application.galaxy.DescIndexData;
import com.ahli.hotkeyUi.application.galaxy.DescIndexReader;
import com.ahli.hotkeyUi.application.galaxy.ext.LayoutExtensionReader;
import com.ahli.hotkeyUi.application.i18n.Messages;
import com.ahli.hotkeyUi.application.model.ValueDef;
import com.ahli.hotkeyUi.application.ui.ShowToUserException;
import com.ahli.hotkeyUi.application.ui.dialogs.Alerts;
import com.ahli.mpq.MpqException;
import com.ahli.mpq.MpqInterface;
import com.ahli.util.JarHelper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TabPane;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;

/**
 * Application
 * 
 * @author Ahli
 *
 */
public class Main extends Application {
	long appStartTime = System.nanoTime();
	static Logger LOGGER = LogManager.getLogger("Main"); //$NON-NLS-1$

	public final static String VERSION = "alpha";

	private Stage primaryStage;
	private BorderPane rootLayout;
	private MenuBarController mbarCtrl;
	private TabsController tabsCtrl;
	private String openedDocPath = null;
	private boolean isNamespaceHeroes = true;
	private MpqInterface mpqi = new MpqInterface();
	private DescIndexData descIndex = new DescIndexData(this, mpqi);
	private File basePath = null;
	private boolean hasUnsavedFileChanges = false;
	private LayoutExtensionReader layoutExtReader;

	/**
	 * Initialize the UI Application.
	 */
	@Override
	public void start(Stage primaryStage) {
		try {
			Thread.currentThread().setName("UI"); //$NON-NLS-1$
			LOGGER.warn("start function called after " + (System.nanoTime() - appStartTime) / 1000000 + "ms.");
			this.primaryStage = primaryStage;
			long time = System.nanoTime();
			primaryStage.show();
			LOGGER.warn("showing primary stage within " + (System.nanoTime() - time) / 1000000 + "ms.");

			setUserAgentStylesheet(STYLESHEET_MODENA);

			// if it fails to load the resource in as a jar, check the eclipse
			// settings
			this.primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("/res/ahliLogo.png"))); //$NON-NLS-1$

			time = System.nanoTime();
			initRootLayout();
			LOGGER.warn("initialized root layout within " + (System.nanoTime() - time) / 1000000 + "ms.");

			// Load Tab layout from fxml file
			time = System.nanoTime();
			FXMLLoader loader = new FXMLLoader();
			loader.setResources(Messages.getBundle());
			TabPane tabPane = (TabPane) loader.load(Main.class.getResourceAsStream("view/TabsLayout.fxml")); //$NON-NLS-1$
			LOGGER.warn("initialized tab layout within " + (System.nanoTime() - time) / 1000000 + "ms.");
			rootLayout.setCenter(tabPane);
			tabsCtrl = loader.getController();
			tabsCtrl.setMainApp(this);
			primaryStage.setMaximized(true);

			// ask to save on close
			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent event) {
					askToSaveUnsavedChanges();
				}
			});

			initPaths();
			initMpqInterface(mpqi);

			LOGGER.warn("finished app initialization after " + (System.nanoTime() - appStartTime) / 1000000 + "ms.");
		} catch (Exception e) {
			LOGGER.error("App Error: " + ExceptionUtils.getStackTrace(e), e); //$NON-NLS-1$
			e.printStackTrace();
			Alerts.buildExceptionAlert(primaryStage, e).showAndWait();
		}
	}

	/**
	 * Asks the user to decide on unsaved changes, if there are any.
	 */
	public void askToSaveUnsavedChanges() {
		if (hasUnsavedFileChanges()) {
			// ask to save changes in the file
			String title = Messages.getString("Main.unsavedChangesTitle"); //$NON-NLS-1$
			String content = String.format(Messages.getString("Main.hasUnsavedChanges"), openedDocPath); //$NON-NLS-1$

			Alert alert = Alerts.buildYesNoCancelAlert(primaryStage, title, title, content);

			Optional<ButtonType> result = alert.showAndWait();

			if ((result.isPresent())) {
				if (result.get() == ButtonType.YES) {
					saveUiMpq();
				}
			}
		}
	}

	/**
	 * Entry point of Application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LOGGER.trace("trace log visible"); //$NON-NLS-1$
		LOGGER.debug("debug log visible"); //$NON-NLS-1$
		LOGGER.info("info log visible"); //$NON-NLS-1$
		LOGGER.warn("warn log visible"); //$NON-NLS-1$
		LOGGER.error("error log visible"); //$NON-NLS-1$
		LOGGER.fatal("fatal log visible"); //$NON-NLS-1$

		LOGGER.trace("Configuration File of System: " + System.getProperty("log4j.configurationFile")); //$NON-NLS-1$ //$NON-NLS-2$

		// TEST Locale
		// Messages.setBundle(Locale.CHINA);

		launch(args);
	}

	/**
	 * Returns true, if the path of the current opened document is valid.
	 * Invalid usually means that no document has been opened.
	 * 
	 * @return
	 */
	public boolean isValidOpenedDocPath() {
		return openedDocPath != null && !openedDocPath.equals(""); //$NON-NLS-1$
	}

	/**
	 * Open File window and actions.
	 */
	public void openUiMpq() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("Main.openObserverInterfaceTitle")); //$NON-NLS-1$

		ExtensionFilter genExtFilter = new ExtensionFilter(
				Messages.getString("Main.sc2HeroesObserverInterfaceExtFilter"), "*.SC2Interface", //$NON-NLS-1$ //$NON-NLS-2$
				"*.StormInterface"); //$NON-NLS-1$

		fileChooser.getExtensionFilters().addAll(new ExtensionFilter(Messages.getString("Main.allFilesFilter"), "*.*"), //$NON-NLS-1$ //$NON-NLS-2$
				genExtFilter, new ExtensionFilter(Messages.getString("Main.sc2InterfaceFilter"), "*.SC2Interface"), //$NON-NLS-1$ //$NON-NLS-2$
				new ExtensionFilter(Messages.getString("Main.heroesInterfaceFilter"), "*.StormInterface")); //$NON-NLS-1$ //$NON-NLS-2$
		fileChooser.setSelectedExtensionFilter(genExtFilter);
		File f = fileChooser.showOpenDialog(primaryStage);

		openMpqFile(f);
	}

	/**
	 * Opens the specified MPQ file.
	 * 
	 * @param f
	 */
	public void openMpqFile(File f) {
		new Thread() {
			public void run() {
				this.setName(this.getName().replaceFirst("Thread", "Open")); //$NON-NLS-1$
				long time = System.nanoTime();
				if (f != null) {
					try {
						mpqi.extractEntireMPQ(f.getAbsolutePath());
						openedDocPath = f.getAbsolutePath();
						updateAppTitle();

						// load desc index from mpq
						try {
							isNamespaceHeroes = mpqi.isHeroesNamespace();
						} catch (MpqException e) {
							// special case to show readable error to user
							throw new ShowToUserException(Messages.getString("Main.OpenedFileNoComponentList"));
						}
						boolean ignoreRequiredToLoadEntries = true;

						File componentListFile = mpqi.getComponentListFile();
						if (componentListFile == null) {
							throw new ShowToUserException(Messages.getString("Main.OpenedFileNoComponentList")); //$NON-NLS-1$
						}
						descIndex.setDescIndexPathAndClear(ComponentsListReader.getDescIndexPath(componentListFile));

						File descIndexFile = mpqi.getCachedFile(descIndex.getDescIndexIntPath());
						descIndex.addLayoutIntPath(
								DescIndexReader.getLayoutPathList(descIndexFile, ignoreRequiredToLoadEntries));

						tabsCtrl.clearData();
						hasUnsavedFileChanges = false;

						boolean recursive = true;
						File cache = new File(mpqi.getMpqCachePath());
						Collection<File> layoutFiles = FileUtils.listFiles(cache, null, recursive);

						layoutExtReader = new LayoutExtensionReader();
						layoutExtReader.processLayoutFiles(layoutFiles);

						ArrayList<ValueDef> hotkeys = layoutExtReader.getHotkeys();
						tabsCtrl.getHotkeysData().addAll(hotkeys);

						ArrayList<ValueDef> settings = layoutExtReader.getSettings();
						tabsCtrl.getSettingsData().addAll(settings);

					} catch (MpqException | ShowToUserException e) {
						LOGGER.error("File could not be opened. MPQ-Error: " + ExceptionUtils.getStackTrace(e), e); //$NON-NLS-1$
						openedDocPath = null;
						updateAppTitle();
						showErrorAlert(e);
					} catch (Exception e) {
						LOGGER.error("File could not be opened. Error: " + ExceptionUtils.getStackTrace(e), e); //$NON-NLS-1$
						e.printStackTrace();
						openedDocPath = null;
						updateAppTitle();
						// Alert alert = new Alert(AlertType.ERROR);
						// alert.setTitle(Messages.getString("Main.errorOpeningFileTitle"));
						// //$NON-NLS-1$
						// alert.setHeaderText(Messages.getString("Main.errorOpeningFileTitle"));
						// //$NON-NLS-1$
						// alert.setContentText(Messages.getString("Main.anErrorOccured")
						// + e.getMessage()); //$NON-NLS-1$
						// alert.showAndWait();

						showExceptionAlert(e);
					}
					updateMenuBar();
				} else {
					LOGGER.trace("File to open was null, most likely due to 'cancel'."); //$NON-NLS-1$
				}
				LOGGER.warn("opened mpq within " + (System.nanoTime() - time) / 1000000 + "ms.");
			}
		}.start();
	}

	/**
	 * Closes the currently opened document.
	 */
	public void closeFile() {
		askToSaveUnsavedChanges();
		openedDocPath = null;
		mpqi.clearCacheExtractedMpq();
		updateAppTitle();
		updateMenuBar();
		tabsCtrl.clearData();
		layoutExtReader.clearData();
		hasUnsavedFileChanges = false;
	}

	/**
	 * Update the menu bar in the main window. E.g. this needs to be done after
	 * opening/closing a document to enable/disable the save buttons.
	 */
	private void updateMenuBar() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				// Update UI here
				mbarCtrl.updateMenuBar();
			}
		});
	}

	/**
	 * Initializes the root layout.
	 * 
	 * @throws IOException
	 */
	public void initRootLayout() throws IOException {
		// Load root layout from fxml file.
		long time = System.nanoTime();
		FXMLLoader loader = new FXMLLoader();
		loader.setResources(Messages.getBundle());
		rootLayout = (BorderPane) loader.load(Main.class.getResourceAsStream("view/RootLayout.fxml")); //$NON-NLS-1$
		LOGGER.trace("initialized root layout fxml within " + (System.nanoTime() - time) / 1000000 + "ms.");

		// get Controller
		time = System.nanoTime();
		mbarCtrl = loader.getController();
		mbarCtrl.setMainApp(this);
		LOGGER.trace("received root layout controller within " + (System.nanoTime() - time) / 1000000 + "ms.");

		// Show the scene containing the root layout.
		Scene scene = new Scene(rootLayout);
		time = System.nanoTime();
		scene.getStylesheets().add(Main.class.getResource("view/application.css").toExternalForm()); //$NON-NLS-1$
		LOGGER.trace("initialized root layout css within " + (System.nanoTime() - time) / 1000000 + "ms.");
		time = System.nanoTime();
		primaryStage.setTitle(Messages.getString("Main.observerUiSettingsEditorTitle")); //$NON-NLS-1$
		primaryStage.setScene(scene);
		LOGGER.trace("executed root layout setScene+title within " + (System.nanoTime() - time) / 1000000 + "ms.");
		time = System.nanoTime();
		primaryStage.show();
		LOGGER.trace("executed root layout stage.show() within " + (System.nanoTime() - time) / 1000000 + "ms.");

		time = System.nanoTime();
		updateMenuBar();
		LOGGER.trace("updateMenuBar within " + (System.nanoTime() - time) / 1000000 + "ms.");
	}

	/**
	 * Returns true, if it belongs to Heroes of the Storm, false otherwise.
	 * 
	 * @return
	 */
	public boolean isHeroesFile() {
		return isNamespaceHeroes;
	}

	/**
	 * Saves the currently opened document.
	 */
	public void saveUiMpq() {
		new Thread() {
			public void run() {
				this.setName(this.getName().replaceFirst("Thread", "Save")); //$NON-NLS-1$
				long time = System.nanoTime();

				// cannot save, if not valid
				if (!isValidOpenedDocPath()) {
					return;
				}

				try {
					compile();
					mpqi.buildMpq(openedDocPath, false, false);
					hasUnsavedFileChanges = false;
					updateAppTitle();
				} catch (IOException | InterruptedException | ParserConfigurationException | SAXException
						| MpqException e) {
					LOGGER.error(ExceptionUtils.getStackTrace(e), e);
					e.printStackTrace();
					showErrorAlert(e);
				}
				LOGGER.warn("opened mpq within " + (System.nanoTime() - time) / 1000000 + "ms.");
			}
		}.start();
	}

	/**
	 * Save As window + actions.
	 */
	public void saveAsUiMpq() {
		// cannot save, if not valid
		if (!isValidOpenedDocPath()) {
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("Main.saveUiTitle")); //$NON-NLS-1$

		ExtensionFilter genExtFilter = new ExtensionFilter(
				Messages.getString("Main.sc2HeroesObserverInterfaceExtFilter"), "*.SC2Interface", //$NON-NLS-1$ //$NON-NLS-2$
				"*.StormInterface"); //$NON-NLS-1$

		fileChooser.getExtensionFilters().addAll(new ExtensionFilter(Messages.getString("Main.allFilesFilter"), "*.*"), //$NON-NLS-1$ //$NON-NLS-2$
				genExtFilter, new ExtensionFilter(Messages.getString("Main.sc2InterfaceFilter"), "*.SC2Interface"), //$NON-NLS-1$ //$NON-NLS-2$
				new ExtensionFilter(Messages.getString("Main.heroesInterfaceFilter"), "*.StormInterface")); //$NON-NLS-1$ //$NON-NLS-2$
		fileChooser.setSelectedExtensionFilter(genExtFilter);

		File loadedF = new File(this.getOpenedDocPath());
		fileChooser.setInitialFileName(loadedF.getName());
		fileChooser.setInitialDirectory(loadedF.getParentFile());

		File f = fileChooser.showSaveDialog(primaryStage);

		if (f != null) {
			try {
				compile();
				mpqi.buildMpq(f.getParentFile().getAbsolutePath(), f.getName(), false, false);
				hasUnsavedFileChanges = false;
				openedDocPath = f.getAbsolutePath();
				updateAppTitle();
			} catch (IOException | InterruptedException | ParserConfigurationException | SAXException
					| MpqException e) {
				LOGGER.error(ExceptionUtils.getStackTrace(e), e);
				e.printStackTrace();
				showErrorAlert(e);
			}
		}
		updateMenuBar();
	}

	/**
	 * Shows an Error Alert with the specified message.
	 * 
	 * @param message
	 */
	private void showErrorAlert(Exception e) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				// Update UI here
				LOGGER.trace("showing error popup");
				String title = Messages.getString("Main.anErrorOccured"); //$NON-NLS-1$
				String content = e.getMessage();
				Alert alert = Alerts.buildErrorAlert(primaryStage, title, title, content);
				alert.showAndWait();
			}
		});
	}

	/**
	 * Shows an Exception Alert.
	 * 
	 * @param message
	 */
	private void showExceptionAlert(Exception e) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				// Update UI here
				LOGGER.trace("showing exception popup");
				Alert alert = Alerts.buildExceptionAlert(primaryStage, e);
				alert.showAndWait();
			}
		});
	}

	/**
	 * Returns the File path of the opened Document.
	 * 
	 * @return
	 */
	public String getOpenedDocPath() {
		return openedDocPath;
	}

	/**
	 * Compiles and updates the data in the cache.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public void compile() throws ParserConfigurationException, SAXException, IOException {
		File cache = new File(mpqi.getMpqCachePath());
		boolean recursive = true;
		String[] extensions = new String[] { "StormLayout", "SC2Layout" }; //$NON-NLS-1$ //$NON-NLS-2$
		Collection<File> layoutFiles = FileUtils.listFiles(cache, extensions, recursive);
		layoutExtReader.updateLayoutFiles(layoutFiles);
	}

	/**
	 * Causes the App to shut down. This includes asking to save unsaved
	 * changes.
	 */
	public void closeApp() {
		primaryStage.fireEvent(new WindowEvent(primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST));
	}

	/**
	 * Initialize Paths
	 */
	private void initPaths() {
		basePath = JarHelper.getJarDir(Main.class);
	}

	/**
	 * Initializes the MPQ Interface.
	 * 
	 * @param mpqi
	 */
	private void initMpqInterface(MpqInterface mpqi) {
		mpqi.setMpqEditorPath(
				basePath + File.separator + "plugins" + File.separator + "mpq" + File.separator + "MPQEditor.exe"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Whether the opened file was changed and the app notified about it or not.
	 * 
	 * @return
	 */
	public boolean hasUnsavedFileChanges() {
		return hasUnsavedFileChanges;
	}

	/**
	 * 
	 */
	public void notifyFileDataWasChanged() {
		if (!hasUnsavedFileChanges) {
			hasUnsavedFileChanges = true;
			updateAppTitle();
		}
	}

	/**
	 * Updates the title of the App.
	 */
	public void updateAppTitle() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				// Update UI here
				String title = Messages.getString("Main.observerUiSettingsEditorTitle"); //$NON-NLS-1$
				if (openedDocPath != null) {
					title += "- [" + openedDocPath + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (hasUnsavedFileChanges) {
					title += "*"; //$NON-NLS-1$
				}
				primaryStage.setTitle(title);
			}
		});

	}

	/**
	 * Returns the App's main window stage.
	 * 
	 * @return
	 */
	public Stage getPrimaryStage() {
		return primaryStage;
	}
}
