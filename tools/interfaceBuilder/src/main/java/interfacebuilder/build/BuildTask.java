package interfacebuilder.build;

import com.ahli.galaxy.game.GameData;
import interfacebuilder.base_ui.BaseUiService;
import interfacebuilder.projects.Project;
import interfacebuilder.threads.CleaningForkJoinTask;

import java.nio.file.Path;

public class BuildTask extends CleaningForkJoinTask {
	
	private final Project project;
	private final boolean useCmdLineSettings;
	private final MpqBuilderService mpqBuilderService;
	private final BaseUiService baseUiService;
	
	public BuildTask(final Project project, final boolean useCmdLineSettings,
			final MpqBuilderService mpqBuilderService, final BaseUiService baseUiService) {
		this.project = project;
		this.useCmdLineSettings = useCmdLineSettings;
		this.mpqBuilderService = mpqBuilderService;
		this.baseUiService = baseUiService;
	}
	
	@Override
	protected boolean work() {
		final GameData gameData = mpqBuilderService.getGameData(project.getGame());
		baseUiService.parseBaseUiIfNecessary(gameData, useCmdLineSettings);
		
		final Path interfaceDirectory = Path.of(project.getProjectPath());
		mpqBuilderService.buildSpecificUI(interfaceDirectory, gameData, useCmdLineSettings, project);
		
		return true;
	}
}
