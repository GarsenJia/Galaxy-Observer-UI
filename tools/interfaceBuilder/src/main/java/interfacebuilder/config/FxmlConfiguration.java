package interfacebuilder.config;

import interfacebuilder.ui.browse.BrowseController;
import interfacebuilder.ui.browse.BrowseTabController;
import interfacebuilder.ui.home.AddProjectController;
import interfacebuilder.ui.home.HomeController;
import interfacebuilder.ui.home.ViewRuleSetController;
import interfacebuilder.ui.navigation.NavigationController;
import interfacebuilder.ui.progress.CompressionMiningController;
import interfacebuilder.ui.progress.TabPaneController;
import interfacebuilder.ui.settings.SettingsCommandLineToolController;
import interfacebuilder.ui.settings.SettingsController;
import interfacebuilder.ui.settings.SettingsGamesPathsController;
import interfacebuilder.ui.settings.SettingsGuiToolController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

@Lazy
@Configuration
public class FxmlConfiguration {
	
	@Bean
	@Scope ("prototype")
	public ViewRuleSetController viewRuleSetController() {
		return new ViewRuleSetController();
	}
	
	@Bean
	public NavigationController navigationController() {
		return new NavigationController();
	}
	
	@Bean
	@Scope ("prototype")
	public HomeController homeController() {
		return new HomeController();
	}
	
	@Bean
	@Scope ("prototype")
	public SettingsCommandLineToolController settingsCommandLineToolController() {
		return new SettingsCommandLineToolController();
	}
	
	@Bean
	@Scope ("prototype")
	public SettingsController settingsController() {
		return new SettingsController();
	}
	
	@Bean
	@Scope ("prototype")
	public SettingsGamesPathsController settingsGamesPathsController() {
		return new SettingsGamesPathsController();
	}
	
	@Bean
	@Scope ("prototype")
	public SettingsGuiToolController settingsGuiToolController() {
		return new SettingsGuiToolController();
	}
	
	@Bean
	public TabPaneController tabPaneController() {
		return new TabPaneController();
	}
	
	@Bean
	@Scope ("prototype")
	public AddProjectController addProjectController() {
		return new AddProjectController();
	}
	
	@Bean
	@Scope ("prototype")
	public CompressionMiningController compressionMiningController() {
		return new CompressionMiningController();
	}
	
	@Bean
	@Scope ("prototype")
	public BrowseController browseController() {
		return new BrowseController();
	}
	
	@Bean
	public BrowseTabController browseTabController() {
		return new BrowseTabController();
	}
}