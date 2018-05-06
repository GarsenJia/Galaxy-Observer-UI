package com.ahli.galaxy.ui;

import com.ahli.galaxy.ui.abstracts.UIElement;
import com.ahli.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ahli
 */
public class UIController extends UIElement {
	/**
	 *
	 */
	private static final long serialVersionUID = -5133613746543071378L;
	private final List<Pair<String, String>> values;
	private List<UIAttribute> keys;
	private boolean nextAdditionShouldOverride;
	private boolean nameIsImplicit = true;
	
	/**
	 * @param name
	 */
	public UIController(final String name) {
		super(name);
		values = new ArrayList<>();
		keys = new ArrayList<>();
	}
	
	/**
	 * @param name
	 */
	public UIController(final String name, final int initialValuesMaxCapacity, final int initialKeysMaxCapacity) {
		super(name);
		values = new ArrayList<>(initialValuesMaxCapacity);
		keys = new ArrayList<>(initialKeysMaxCapacity);
	}
	
	/**
	 * Returns a deep clone of this.
	 */
	@Override
	public Object deepCopy() {
		final UIController clone = new UIController(getName(), values.size(), keys.size());
		for (int i = 0, len = keys.size(); i < len; i++) {
			clone.keys.add((UIAttribute) keys.get(i).deepCopy());
		}
		for (int i = 0, len = values.size(); i < len; i++) {
			final Pair<String, String> p = values.get(i);
			clone.values.add(new Pair<>(p.getKey(), p.getValue()));
		}
		clone.nextAdditionShouldOverride = nextAdditionShouldOverride;
		clone.nameIsImplicit = nameIsImplicit;
		return clone;
	}
	
	/**
	 * @return the keys
	 */
	public List<UIAttribute> getKeys() {
		return keys;
	}
	
	/**
	 * @param keys
	 * 		the keys to set
	 */
	public void setKeys(final List<UIAttribute> keys) {
		this.keys = keys;
	}
	
	/**
	 * @return the nextAdditionShouldOverride
	 */
	public boolean isNextAdditionShouldOverride() {
		return nextAdditionShouldOverride;
	}
	
	/**
	 * @param nextAdditionShouldOverride
	 * 		the nextAdditionShouldOverride to set
	 */
	public void setNextAdditionShouldOverride(final boolean nextAdditionShouldOverride) {
		this.nextAdditionShouldOverride = nextAdditionShouldOverride;
	}
	
	/**
	 * @param key
	 * @param value
	 */
	public String addValue(final String key, final String value) {
		final Pair<String, String> newPair = new Pair<>(key, value);
		final int i = values.indexOf(newPair);
		if (i == -1) {
			values.add(newPair);
			return null;
		} else {
			return values.set(i, newPair).getValue();
		}
	}
	
	/**
	 * @param key
	 * @return
	 */
	public String getValue(final String key) {
		int i;
		Pair<String, String> p;
		for (i = 0; i < values.size(); i++) {
			p = values.get(i);
			if (p.getKey().equals(key)) {
				return p.getValue();
			}
		}
		return null;
	}
	
	/**
	 * @return the nameIsImplicit
	 */
	public boolean isNameIsImplicit() {
		return nameIsImplicit;
	}
	
	/**
	 * @param nameIsImplicit
	 * 		the nameIsImplicit to set
	 */
	public void setNameIsImplicit(final boolean nameIsImplicit) {
		this.nameIsImplicit = nameIsImplicit;
	}
	
	/**
	 * @param path
	 * @return
	 */
	@Override
	public UIElement receiveFrameFromPath(final String path) {
		return (path == null || path.isEmpty()) ? this : null;
	}
	
	@Override
	public String toString() {
		return "<Controller name='" + getName() + "'>";
	}
}