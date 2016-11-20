package eantoranz.utils;

/**
 * Copyright 2016 Edmundo Carmona Antoranz
 * released under the terms of GPLv3
 */

public interface Bisectable {
	
	/**
	 * Elements in the sample to bisect
	 */
	public int size();
	
	/**
	 * Calculated value for item i
	 * @param i index in the sample (0-based)
	 * @return
	 */
	public int getValue(int i) throws BisectionException;

}
