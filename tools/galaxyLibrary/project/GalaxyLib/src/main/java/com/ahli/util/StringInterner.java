package com.ahli.util;

public final class StringInterner {
	private static final ConcurrentWeakWeakHashMap<String> map = new ConcurrentWeakWeakHashMap<>(8, 0.9F, 1);
	
	private StringInterner() {
		// no instances allowed
	}
	
	public static String intern(final String s) {
		final String exists = map.putIfAbsent(s, s);
		return (exists == null) ? s : exists;
	}
	
	public static void clear() {
		map.clear();
	}
	
	/**
	 * Removes obsolete WeakReference-Instances that remain after the VM garbage was collected.
	 */
	public static void cleanUpGarbage() {
		map.purgeKeys();
	}
}
