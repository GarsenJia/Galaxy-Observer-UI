package com.ahli.galaxy.ui;

import java.util.ArrayList;
import java.util.List;

import com.ahli.util.Pair;

/**
 * @author Ahli
 */
public class UIController extends UIElement {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5133613746543071378L;
	
	private List<UIAttribute> keys = null;
	// private Map<String, String> values = null;
	private ArrayList<Pair<String, String>> values = null;
	private boolean nextAdditionShouldOverride = false;
	private boolean nameIsImplicit = true;
	
	/**
	 * @param name
	 */
	public UIController(final String name) {
		super(name);
		// values = new HashMap<>(1, 1f);
		values = new ArrayList<>();
		keys = new ArrayList<>();
	}
	
	/**
	 * @param name
	 */
	public UIController(final String name, final int initialValuesMaxCapacity, final int initialKeysMaxCapacity) {
		super(name);
		// values = new HashMap<>(initialValuesMaxCapacity, 1f);
		values = new ArrayList<>(initialValuesMaxCapacity);
		keys = new ArrayList<>(initialKeysMaxCapacity);
	}
	
	/**
	 * Returns a deep clone of this.
	 */
	@Override
	public Object clone() {
		final UIController clone = new UIController(getName(), values.size(), keys.size());
		for (int i = 0; i < keys.size(); i++) {
			clone.keys.add((UIAttribute) keys.get(i).clone());
		}
		// final Object[] entries = values.entrySet().toArray();
		// final Map<String, String> clonedMap = clone.values;
		// for (int fix = 0, i = fix; i < entries.length; i++) {
		// @SuppressWarnings("unchecked")
		// final Entry<String, String> entry = (Entry<String, String>) entries[i];
		// clonedMap.put(entry.getKey(), entry.getValue());
		// }
		for (int i = 0; i < values.size(); i++) {
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
	 *            the keys to set
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
	 *            the nextAdditionShouldOverride to set
	 */
	public void setNextAdditionShouldOverride(final boolean nextAdditionShouldOverride) {
		this.nextAdditionShouldOverride = nextAdditionShouldOverride;
	}
	
	// /**
	// * @return the values
	// */
	// public Map<String, String> getValues() {
	// return values;
	// }
	//
	// /**
	// * @param values
	// * the values to set
	// */
	// public void setValues(final Map<String, String> values) {
	// this.values = values;
	// }
	
	/**
	 * @return the values
	 */
	public ArrayList<Pair<String, String>> getValues() {
		return values;
	}
	
	/**
	 * @param values
	 *            the values to set
	 */
	public void setValues(final ArrayList<Pair<String, String>> values) {
		this.values = values;
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
		Pair<String, String> p = null;
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
	 *            the nameIsImplicit to set
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
