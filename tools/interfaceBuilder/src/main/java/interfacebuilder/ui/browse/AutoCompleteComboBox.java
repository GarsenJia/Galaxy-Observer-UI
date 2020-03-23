// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package interfacebuilder.ui.browse;

import javafx.geometry.Side;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is a ComboBox which implements a search functionality which appears in a context menu.
 * <p>
 * This implementation is based on Caleb Brinkman's AutoCompleteTextField.
 */
public class AutoCompleteComboBox extends ComboBox<String> {
	/** The popup used to select an entry. */
	private final ContextMenu entriesPopup;
	
	/** Construct a new AutoCompleteTextField. */
	public AutoCompleteComboBox() {
		super();
		setEditable(true);
		
		entriesPopup = new ContextMenu();
		
		TextField editor = this.getEditor();
		if (editor != null) {
			editor.textProperty().addListener((observableValue, oldVal, newVal) -> {
				if (newVal.length() == 0 || !AutoCompleteComboBox.this.isFocused()) {
					entriesPopup.hide();
				} else {
					// TODO visually show the matching area in contextmenu's texts
					final List<CustomMenuItem> menuItems = new LinkedList<>();
					final int maxEntries = 20;
					int curEntries = 0;
					for (String item : getItems()) {
						if (containsIgnoreCase(item, newVal)) {
							final Label entryLabel = new Label(item);
							final CustomMenuItem menuItem = new CustomMenuItem(entryLabel, true);
							menuItem.setOnAction(event -> {
								AutoCompleteComboBox.this.getSelectionModel().select(item);
								entriesPopup.hide();
							});
							menuItems.add(menuItem);
							
							++curEntries;
							if (curEntries >= maxEntries) {
								break;
							}
						}
					}
					
					entriesPopup.getItems().clear();
					if (!menuItems.isEmpty()) {
						entriesPopup.getItems().addAll(menuItems);
						entriesPopup.show(AutoCompleteComboBox.this, Side.BOTTOM, 0, 0);
					} else {
						entriesPopup.hide();
					}
				}
			});
		}
		
		focusedProperty().addListener((observableValue, aBoolean, aBoolean2) -> entriesPopup.hide());
		
	}
	
	/**
	 * Code by Icza from https://stackoverflow.com/a/25379180/12849006
	 *
	 * @param src
	 * @param what
	 * @return
	 */
	public static boolean containsIgnoreCase(final String src, final String what) {
		final int length = what.length();
		if (length == 0) {
			return true; // Empty string is contained
		}
		
		final char firstLo = Character.toLowerCase(what.charAt(0));
		final char firstUp = Character.toUpperCase(what.charAt(0));
		
		for (int i = src.length() - length; i >= 0; i--) {
			// Quick check before calling the more expensive regionMatches() method:
			final char ch = src.charAt(i);
			if (ch != firstLo && ch != firstUp) {
				continue;
			}
			
			if (src.regionMatches(true, i, what, 0, length)) {
				return true;
			}
		}
		return false;
	}
	
}