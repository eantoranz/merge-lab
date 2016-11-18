package eantoranz.tools.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	
	private static int getOutput(String gitCommand, List<String> output) throws IOException, InterruptedException {
		return getOutput(gitCommand, output, null);
	}
	
	private static int getOutput(String gitCommand, List<String> output, String[] args) throws IOException, InterruptedException {
		ArrayList<String> commands = new ArrayList<>();
		commands.add("git");
		commands.add(gitCommand);
		if (args != null) {
			commands.addAll(Arrays.asList(args));
		}
		Process gitProcess = Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(gitProcess.getInputStream()));
		String aLine;
		do {
			aLine = stdoutReader.readLine();
			if (aLine != null) {
				output.add(aLine);
			}
		} while (aLine != null);
		return gitProcess.waitFor();
	}
	
	private static String getMergeBase(String treeish1, String treeish2) throws IOException, InterruptedException {
		ArrayList<String> output = new ArrayList<String>();
		int exitCode = getOutput("merge-base", output, new String[]{treeish1, treeish2});
		if (exitCode == 0) {
			return output.get(0);
		} else {
			throw new IOException("Error getting merge base (" + treeish1 + ", " + treeish2 + ")");
		}
	}
	
	private static ArrayList<String> getDiff(String treeish1, String treeish2) throws IOException, InterruptedException {
		ArrayList<String> output = new ArrayList<String>();
		int exitCode = getOutput("diff", output, new String[]{"--name-status", treeish1, treeish2});
		if (exitCode == 0) {
			return output;
		} else {
			throw new IOException("Error getting diff (" + treeish1 + ", " + treeish2 + ")");
		}
	}
	
	/**
	 * List of files must be coming from diff --name-status
	 * @param listOfFiles
	 * @return
	 */
	private static ArrayList<String> getModifiedFiles(List<String> listOfFiles) {
		ArrayList<String> res = new ArrayList<>();
		for (String diffLine: listOfFiles){
			if (diffLine.charAt(0) == 'M') {
				res.add(diffLine.substring(2));
			}
		};
		return res;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("EOL Change Detection Tool");
		System.out.println("Copyright 2016 Edmundo Carmona Antoranz <eantoranz@gmail.com>");
		
		if (args.length < 2) {
			System.err.println("Not enough parameters");
			System.err.println("Have to provide at least 2 branches (that will be merged)");
			System.exit(1);
		}
		
		String mergeBase = getMergeBase(args[0], args[1]);
		
		// have to look for the files hat changed between merge-base and each branch
		ArrayList<String> modifiedFilesBranch1 = getModifiedFiles(getDiff(mergeBase, args[0]));
		ArrayList<String> modifiedFilesBranch2 = getModifiedFiles(getDiff(mergeBase, args[1]));
	}

}
