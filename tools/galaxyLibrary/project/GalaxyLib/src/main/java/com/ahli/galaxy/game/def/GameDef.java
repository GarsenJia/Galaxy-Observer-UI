// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.galaxy.game.def;

/**
 * @author Ahli
 */
public record GameDef(
		String name, String nameHandle, String modFileEnding, String[] coreModsOrDirectories, String defaultRaceId,
		String defaultConsoleSkinId, String documentsGameDirectoryName, String layoutFileEnding,
		String baseDataFolderName, String rootExeName, String switcherExeNameX32, String switcherExeNameX64,
		String supportDirectoryX32, String supportDirectoryX64, String documentsInterfaceSubdirectoryName,
		String modsSubDirectory, String ptrRootExeName) {
	
	public static GameDef getSc2GameDef() {
		return new GameDef("StarCraft II", "sc2", "SC2Mod", new String[] { "core.sc2mod" }, "Terr", "ClassicTerran",
				"StarCraft II", "SC2Layout", "base.sc2data", "StarCraft II.exe", "SC2Switcher.exe",
				"SC2Switcher_x64.exe", "Support", "Support64", "Interfaces", "mods", null);
	}
	
	public static GameDef getHeroesGameDef() {
		return new GameDef("Heroes of the Storm", "heroes", "stormmod",
				new String[] { "core.stormmod", "heroesdata.stormmod", "heromods" }, "Terr", "ClassicTerran",
				"Heroes of the Storm", "stormlayout", "base.stormdata", "Heroes of the Storm.exe", null,
				"HeroesSwitcher_x64.exe", null, "Support64", "Interfaces", "mods",
				"Heroes of the Storm Public Test.exe");
	}
	
	/**
	 * Creates a new instance of a GameDef for StarCraft II.
	 *
	 * @param gameDef
	 * @return
	 */
	public static boolean isSc2(final GameDef gameDef) {
		return gameDef.nameHandle().equals("sc2");
	}
	
	/**
	 * Creates a new instance of a GameDef for Heroes of the Storm.
	 *
	 * @param gameDef
	 * @return
	 */
	public static boolean isHeroes(final GameDef gameDef) {
		return gameDef.nameHandle().equals("heroes");
	}
}
