package eantoranz.tools.git;

/*
 * Copyright 2016 Edmundo Carmona Antoranz <eantoranz@gmail.com>
 * 
 * Released under the terms of GPLv3
 *
 */

/**
 * EOL Change Detection Tool
 * 
 * This tool will detect when a file has got its EOL format changed
 * so that it can be corrected before merging 2 branches
 *
 */
public class EOLCDT {

	public static void main(String[] args) {
		System.out.println("EOL Change Detection Tool");
		System.out.println("Copyright 2016 Edmundo Carmona Antoranz <eantoranz@gmail.com>");
		
		if (args.length < 2) {
			System.err.println("Not enough parameters");
			System.err.println("Have to provide at least 2 branches (that will be merged)");
			System.exit(1);
		}
		
	}

}
