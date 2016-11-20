package eantoranz.utils;

/**
 * Copyright 2016 Edmundo Carmona Antoranz
 * Released under the terms of GPLv3
 * <p>
 * Given an array of items 0 to n,
 * will look for the position i where F(Xi) becomes the
 * same value as F(Xn)
 * 
 */
public class Bisection {
	
	private Bisectable bisectable;
	private int value0;
	private int valueN;
	
	
	public Bisection(Bisectable bisectable) throws BisectionException {
		this.bisectable = bisectable;
		this.value0 = bisectable.getValue(0);
		this.valueN = bisectable.getValue(bisectable.size() - 1);
	}
	
	public int getChangeIndex() throws BisectionException {
		int lowIndex = 0;
		int highIndex = bisectable.size() - 1;
		// while highIndex is more than one spot away from lowIndex
		while (highIndex - lowIndex > 1) {
			int midPosition = (lowIndex + highIndex) / 2;
			int valueMid = bisectable.getValue(midPosition);
			if (valueMid == this.value0) {
				// have to change lowIndex over here
				lowIndex = midPosition;
			} else if (valueMid == this.valueN) {
				highIndex = midPosition;
			} else {
				return -1;
			}
		}
		if (highIndex == lowIndex + 1) {
			return highIndex;
		} else {
			return -1;
		}
	}

}
