package com.ahli.galaxy.ui;

import com.ahli.galaxy.ui.abstracts.UIElement;
import com.ahli.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ahli
 */
public class UIAnimation extends UIElement {
	/**
	 *
	 */
	private static final long serialVersionUID = 7493401910318905210L;
	
	private List<UIController> controllers;
	private List<Pair<String, UIAttribute>> events;
	private boolean nextEventsAdditionShouldOverride;
	private UIAttribute driver;
	
	/**
	 * @param name
	 */
	public UIAnimation(final String name) {
		super(name);
		events = new ArrayList<>();
		controllers = new ArrayList<>();
	}
	
	/**
	 * Constructor.
	 *
	 * @param name
	 * 		Element's name
	 */
	public UIAnimation(final String name, final int minEventsCapacity, final int minControllerCapacity) {
		super(name);
		events = new ArrayList<>(minEventsCapacity);
		controllers = new ArrayList<>(minControllerCapacity);
	}
	
	/**
	 * Returns a deep clone of this.
	 */
	@Override
	public Object deepCopy() {
		final UIAnimation clone = new UIAnimation(getName(), events.size(), controllers.size());
		for (int i = 0, len = controllers.size(); i < len; i++) {
			clone.controllers.add((UIController) controllers.get(i).deepCopy());
		}
		for (int i = 0, len = events.size(); i < len; i++) {
			final Pair<String, UIAttribute> p = events.get(i);
			clone.events.add(new Pair<>(p.getKey(), (UIAttribute) p.getValue().deepCopy()));
		}
		clone.nextEventsAdditionShouldOverride = nextEventsAdditionShouldOverride;
		if (driver != null) {
			clone.driver = (UIAttribute) driver.deepCopy();
		}
		return clone;
	}
	
	/**
	 * @return the controllers
	 */
	public List<UIController> getControllers() {
		return controllers;
	}
	
	/**
	 * @param controllers
	 * 		the controllers to set
	 */
	public void setControllers(final List<UIController> controllers) {
		this.controllers = controllers;
	}
	
	/**
	 * @return the events
	 */
	public List<Pair<String, UIAttribute>> getEvents() {
		return events;
	}
	
	/**
	 * @param events
	 * 		the events to set
	 */
	public void setEvents(final List<Pair<String, UIAttribute>> events) {
		this.events = events;
	}
	
	/**
	 * @param key
	 * @param value
	 */
	public UIAttribute addEvent(final String key, final UIAttribute value) {
		final Pair<String, UIAttribute> newPair = new Pair<>(key, value);
		final int i = events.indexOf(newPair);
		if (i == -1) {
			events.add(newPair);
			return null;
		} else {
			return events.set(i, newPair).getValue();
		}
	}
	
	/**
	 * @param key
	 * @return
	 */
	public UIAttribute getValue(final String key) {
		int i;
		Pair<String, UIAttribute> p;
		for (i = 0; i < events.size(); i++) {
			p = events.get(i);
			if (p.getKey().equals(key)) {
				return p.getValue();
			}
		}
		return null;
	}
	
	/**
	 * @return
	 */
	public boolean isNextEventsAdditionShouldOverride() {
		return nextEventsAdditionShouldOverride;
	}
	
	/**
	 * @param nextEventsAdditionShouldOverride
	 */
	public void setNextEventsAdditionShouldOverride(final boolean nextEventsAdditionShouldOverride) {
		this.nextEventsAdditionShouldOverride = nextEventsAdditionShouldOverride;
	}
	
	/**
	 * @return the driver
	 */
	public UIAttribute getDriver() {
		return driver;
	}
	
	/**
	 * @param driver
	 * 		the driver to set
	 */
	public void setDriver(final UIAttribute driver) {
		this.driver = driver;
	}
	
	/**
	 * @param path
	 * @return
	 */
	@Override
	public UIElement receiveFrameFromPath(final String path) {
		if (path == null || path.isEmpty()) {
			// end here
			return this;
		} else {
			// go deeper
			final String curName = UIElement.getLeftPathLevel(path);
			for (final UIElement curElem : controllers) {
				if (curName.equalsIgnoreCase(curElem.getName())) {
					// found right frame -> cut path
					final String newPath = UIElement.removeLeftPathLevel(path);
					return curElem.receiveFrameFromPath(newPath);
				}
			}
			return null;
		}
	}
	
	@Override
	public String toString() {
		return "<Animation name='" + getName() + "'>";
	}
}