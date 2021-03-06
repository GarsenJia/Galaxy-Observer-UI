// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package interfacebuilder.ui.progress;

import com.ahli.galaxy.game.GameDef;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import interfacebuilder.base_ui.BaseUiService;
import interfacebuilder.base_ui.ExtractBaseUiTask;
import interfacebuilder.compress.GameService;
import interfacebuilder.projects.enums.Game;
import interfacebuilder.ui.AppController;
import interfacebuilder.ui.Updateable;
import interfacebuilder.ui.navigation.NavigationController;
import interfacebuilder.ui.progress.appender.Appender;
import interfacebuilder.ui.progress.appender.TextFlowAppender;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.concurrent.ForkJoinPool;

public class BaseUiExtractionController implements Updateable {
	
	private static int threadCount;
	private final String[] threadNames;
	private final BaseUiService baseUiService;
	private final GameService gameService;
	private final NavigationController navigationController;
	private final ForkJoinPool executor;
	private final AppController appController;
	
	@FXML
	private FontAwesomeIconView stateImage1;
	@FXML
	private FontAwesomeIconView stateImage2;
	@FXML
	private FontAwesomeIconView stateImage3;
	@FXML
	private ScrollPane scrollPane1;
	@FXML
	private ScrollPane scrollPane2;
	@FXML
	private ScrollPane scrollPane3;
	@FXML
	private VBox loggingArea;
	@FXML
	private Label titleLabel;
	@FXML
	private TextFlow txtArea1;
	@FXML
	private Label areaLabel1;
	@FXML
	private TextFlow txtArea2;
	@FXML
	private Label areaLabel2;
	@FXML
	private TextFlow txtArea3;
	@FXML
	private Label areaLabel3;
	private ErrorTabController errorTabController;
	
	public BaseUiExtractionController(
			final BaseUiService baseUiService,
			final GameService gameServic,
			final NavigationController navigationController,
			final ForkJoinPool executor,
			final AppController appController) {
		this.baseUiService = baseUiService;
		gameService = gameServic;
		this.navigationController = navigationController;
		this.executor = executor;
		this.appController = appController;
		threadNames = new String[3];
		threadNames[0] = "extractThread_" + ++threadCount;
		threadNames[1] = "extractThread_" + ++threadCount;
		threadNames[2] = "extractThread_" + ++threadCount;
	}
	
	public VBox getLoggingArea() {
		return loggingArea;
	}
	
	/**
	 * Automatically called by FxmlLoader
	 */
	public void initialize() {
		// auto-downscrolling
		scrollPane1.vvalueProperty().bind(txtArea1.heightProperty());
		scrollPane2.vvalueProperty().bind(txtArea2.heightProperty());
		scrollPane3.vvalueProperty().bind(txtArea3.heightProperty());
		stateImage1.setVisible(false);
		stateImage2.setVisible(false);
		stateImage3.setVisible(false);
	}
	
	@Override
	public void update() {
		// nothing to do
	}
	
	public void start(final Game game, final boolean usePtr) {
		errorTabController.setRunning(true);
		final GameDef exportedGameDef = gameService.getGameDef(game);
		final String ptrString = usePtr ? " PTR" : "";
		titleLabel.setText(String.format("Extract %s's Base UI", exportedGameDef.getName() + ptrString));
		txtArea1.getChildren().clear();
		txtArea2.getChildren().clear();
		txtArea3.getChildren().clear();
		final Appender[] appenders = new Appender[3];
		appenders[0] = new TextFlowAppender(txtArea1);
		appenders[1] = new TextFlowAppender(txtArea2);
		appenders[2] = new TextFlowAppender(txtArea3);
		appenders[0].endedProperty().addListener(new EndedListener(stateImage1, txtArea1));
		appenders[1].endedProperty().addListener(new EndedListener(stateImage2, txtArea2));
		appenders[2].endedProperty().addListener(new EndedListener(stateImage3, txtArea3));
		
		final String[] queryMasks = BaseUiService.getQueryMasks(game);
		final String msg = "Extracting %s files";
		areaLabel1.setText(String.format(msg, queryMasks[0]));
		areaLabel2.setText(String.format(msg, queryMasks[1]));
		areaLabel3.setText(String.format(msg, queryMasks[2]));
		
		stateImage1.setIcon(FontAwesomeIcon.SPINNER);
		stateImage2.setIcon(FontAwesomeIcon.SPINNER);
		stateImage3.setIcon(FontAwesomeIcon.SPINNER);
		stateImage1.setFill(Color.GAINSBORO);
		stateImage2.setFill(Color.GAINSBORO);
		stateImage3.setFill(Color.GAINSBORO);
		stateImage1.setVisible(true);
		stateImage2.setVisible(true);
		stateImage3.setVisible(true);
		
		final ExtractBaseUiTask task = new ExtractBaseUiTask(appController,
				baseUiService,
				game,
				usePtr,
				appenders,
				errorTabController,
				navigationController);
		executor.execute(task);
	}
	
	public String[] getThreadNames() {
		return threadNames;
	}
	
	public void setErrorTabControl(final ErrorTabController errorTabCtrl) {
		errorTabController = errorTabCtrl;
	}
	
	public ErrorTabController getErrorTabController() {
		return errorTabController;
	}
	
	private static final class EndedListener implements ChangeListener<Boolean> {
		private static final String ERROR = "ERROR:";
		private static final String WARNING = "WARNING:";
		private final FontAwesomeIconView stateImage;
		private final TextFlow txtArea;
		
		private EndedListener(final FontAwesomeIconView stateImage, final TextFlow txtArea) {
			this.stateImage = stateImage;
			this.txtArea = txtArea;
		}
		
		@Override
		public void changed(
				final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
			// the text children are added after the UI updates
			Platform.runLater(() -> {
				boolean isError = false;
				boolean isWarning = false;
				final ObservableList<Node> children = txtArea.getChildren();
				if (children != null && !children.isEmpty()) {
					for (final Node child : children) {
						if (child instanceof Text) {
							final Text text = (Text) child;
							if (text.getText().contains(ERROR)) {
								isError = true;
								break;
							}
							if (text.getText().contains(WARNING)) {
								isWarning = true;
								break;
							}
						}
					}
				} else {
					isError = true;
				}
				if (isError) {
					stateImage.setIcon(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
					stateImage.setFill(Color.RED);
				} else if (isWarning) {
					stateImage.setIcon(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
					stateImage.setFill(Color.YELLOW);
				} else {
					stateImage.setIcon(FontAwesomeIcon.CHECK);
					stateImage.setFill(Color.LAWNGREEN);
				}
			});
		}
	}
}
