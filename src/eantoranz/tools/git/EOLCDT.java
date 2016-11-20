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
	
	private static final int EOL_LF = 1;
	private static final int EOL_CRLF = 2;
	private static final int EOL_MIXED = 253;
	private static final int EOL_UNKNOWN = 254;
	private static final int BINARY = 255; // file is binary so no EOL
	
	private static int getOutput(String gitCommand, List<String> output) throws IOException, InterruptedException {
		return getOutput(gitCommand, output, null);
	}
	
	/**
	 * Create a git command process
	 * @param gitCommand
	 * @param args if args is null no argument will be added, only the <b>git command</b> will be used
	 * @return
	 * @throws IOException
	 */
	private static Process createProcess(String gitCommand, String[] args) throws IOException {
		ArrayList<String> commands = new ArrayList<>();
		commands.add("git");
		commands.add(gitCommand);
		if (args != null) {
			commands.addAll(Arrays.asList(args));
		}
		return Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));
	}
	
	private static int getOutput(String gitCommand, List<String> output, String[] args) throws IOException, InterruptedException {
		Process gitProcess = createProcess(gitCommand, args);
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
	
	/**
	 * 
	 * @param gitCommand
	 * @param output has to be an existing and empty ArrayList
	 * @param args
	 * @param limit reading up to limit chars (-1 means no limit) 
	 * @return
	 * @throws IOException
	 */
	private static int getBinaryOutput(String gitCommand, ArrayList<Character> output, String[] args, int limit) throws IOException, InterruptedException {
		Process gitProcess = createProcess(gitCommand, args);
		
		InputStreamReader stdoutReader = new InputStreamReader(gitProcess.getInputStream());
		char[] buffer = new char[limit == -1 ? 1024 : limit];
		int bytesRead;
		int totalBytesRead = 0;
		do {
			bytesRead = stdoutReader.read(buffer);
			for (int i = 0; i < bytesRead; i++) {
				output.add(buffer[i]);
				totalBytesRead++;
				if (limit >= 0 && totalBytesRead >= limit) {
					// let's stop reading
					while (stdoutReader.read(buffer) >= 0); // read whatever is left on the pipe TODO Can we do it some other way?
					break;
				}
			}
		} while (bytesRead >= 0 && (limit == -1 || totalBytesRead < limit));
		stdoutReader.close();
		
		int result = gitProcess.waitFor();
		return result;
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
	
	private static int getEolFormat(String treeish, String path) throws IOException, InterruptedException {
		ArrayList<Character> content = new ArrayList<>();
		
		int result = getBinaryOutput("show", content, new String[]{treeish + ":" + path}, 5000);
		if (result != 0) {
			throw new IOException("Error while reading content from " + treeish + ":" + path);
		}
		boolean hasCRLF = false;
		boolean hasLF = false;
		for (int i = 0; i < content.size(); i++) {
			Character aChar = content.get(i);
			switch (aChar.charValue()) {
			case 0:
				// binary file
				return BINARY;
			case 10:
				if (i == 0) {
					hasLF = true;
				} else {
					if (content.get(i - 1) == 13) {
						hasCRLF = true;
					} else {
						hasLF = true;
					}
				}
			}
		}
		if (hasLF) {
			if (hasCRLF) {
				return EOL_MIXED;
			} else {
				return EOL_LF;
			}
		} else {
			if (hasCRLF) {
				return EOL_CRLF;
			}
		}
		// couldn't figure it out
		return EOL_UNKNOWN;
	}
	
	public static String getEOlFormatString(int eolType) {
		switch(eolType) {
		case EOL_LF:
			return "LF";
		case EOL_CRLF:
			return "CRLF";
		}
		return "UNKNOWN";
	}
	
	private static void analyzeFile(String mergeBase, String treeish1, String treeish2, String filename) throws IOException, InterruptedException {
		// need to get the EOL format of the merge-base file and the two branches to see if there's a difference
		int eolMergeBase = getEolFormat(mergeBase, filename);
		switch (eolMergeBase) {
		case BINARY:
			System.out.println(filename + " is binary on merge base");
			return;
		case EOL_LF:
		case EOL_CRLF:
			int eolTreeish1 = getEolFormat(treeish1, filename);
			int eolTreeish2 = getEolFormat(treeish2, filename);
			System.out.print(filename + ": ");
			if (eolMergeBase == eolTreeish1) {
				if (eolMergeBase == eolTreeish2) {
					System.out.print("OK (" + getEOlFormatString(eolMergeBase) + ")");
				} else {
					System.out.print("Changed on treeish2 (" + getEOlFormatString(eolMergeBase) + " -> " + getEOlFormatString(eolTreeish2) + ")");
				}
			} else {
				if (eolMergeBase == eolTreeish2) {
					System.out.println("Changed on treeish1 (" + getEOlFormatString(eolMergeBase) + " -> " + getEOlFormatString(eolTreeish1) + ")");
				} else {
					System.out.println("Changed on both branches ("
							+ getEOlFormatString(eolMergeBase)
							+ " -> [treeish1 " + getEOlFormatString(eolTreeish1)
							+ ", treeish2 " + getEOlFormatString(eolTreeish1)
							+ "])");
				}
			}
			System.out.println();
			break;
		case EOL_MIXED:
			System.out.println(filename + " is of mixed EOL format on merge base");
		case EOL_UNKNOWN:
			System.err.println(filename + " is of type UNKNOWN on merge base");
			return;
		}
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
		System.out.println("Merge base: " + mergeBase);
		System.out.println("Treeish1: " + args[0]);
		System.out.println("Treeish2: " + args[1]);
		
		// have to look for the files hat changed between merge-base and each branch
		ArrayList<String> modifiedFilesBranch1 = getModifiedFiles(getDiff(mergeBase, args[0]));
		ArrayList<String> modifiedFilesBranch2 = getModifiedFiles(getDiff(mergeBase, args[1]));
		
		// now let's get the list of files that are on both lists
		ArrayList<String> commonFiles = new ArrayList(modifiedFilesBranch1);
		commonFiles.retainAll(modifiedFilesBranch2);
		
		for (String filename: commonFiles) {
			analyzeFile(mergeBase, args[0], args[1], filename); 
		}
	}

}
