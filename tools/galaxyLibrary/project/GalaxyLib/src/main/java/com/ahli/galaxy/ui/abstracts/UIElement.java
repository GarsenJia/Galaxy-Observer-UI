// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.galaxy.ui.abstracts;

import com.ahli.util.DeepCopyable;
import com.ahli.util.StringInterner;

import java.util.List;

/**
 * @author Ahli
 */
public abstract class UIElement implements DeepCopyable {
	
	// for hashcode caching
	protected boolean hashIsDirty;
	protected boolean hashIsZero;
	protected int hash;
	private String name;
	
	/**
	 * Constructor.
	 *
	 * @param name
	 * 		element's name
	 */
	public UIElement(final String name) {
		this.name = name != null ? StringInterner.intern(name) : null;
	}
	
	/**
	 * Removes the left/top-most level of the specified path and returns the remaining.
	 *
	 * @param path
	 * @return
	 */
	public static String removeLeftPathLevel(final String path) {
		final int i = path.indexOf('/');
		return (i == -1) ? null : path.substring(i + 1);
	}
	
	/**
	 * @param path
	 * @return
	 */
	public static String getLeftPathLevel(final String path) {
		final int i = path.indexOf('/');
		return (i == -1) ? path : path.substring(0, i);
	}
	
	/**
	 * Returns the correct frame based on a specified path. For example, the frame that a path in a template
	 * references.
	 *
	 * @param path
	 * 		path of an element
	 * @return UIFrame element
	 */
	public abstract UIElement receiveFrameFromPath(String path);
	
	@Override
	public String toString() {
		return "<UIElement name='" + name + "'>";
	}
	
	/**
	 * Returns all child UIElements. Changes to the returned list are reflected in this Object.
	 * <p>
	 * The returned list may be empty. Calling this may cause the allocated memory to grow as the empty list is saved.
	 * To avoid allocating more memory, call getChildrenRaw().
	 *
	 * @return List with child elements. Never returns null. The list is not a duplicate.
	 */
	public abstract List<UIElement> getChildren();
	
	/**
	 * Returns all child UIElements or null. Changes to the returned list are reflected in this Object.
	 *
	 * @return List with child elements. May return null instead of an empty list. The list is not a duplicate.
	 */
	public abstract List<UIElement> getChildrenRaw();
	
	@Override
	public boolean equals(final Object obj) {
		// based on lombok
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof UIElement)) {
			return false;
		}
		final UIElement other = (UIElement) obj;
		if (!other.canEqual(this)) {
			return false;
		}
		return name == null ? other.name == null : name.equals(other.name);
	}
	
	protected boolean canEqual(final Object other) {
		return other instanceof UIElement;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name
	 * 		the name to set
	 */
	public void setName(final String name) {
		this.name = name != null ? StringInterner.intern(name) : null;
	}
	
	@Override
	public int hashCode() {
		// based on lombok
		return 59 + (name == null ? 43 : name.hashCode());
	}
	
	public void invalidateHashcode() {
		hashIsDirty = true;
	}
}
