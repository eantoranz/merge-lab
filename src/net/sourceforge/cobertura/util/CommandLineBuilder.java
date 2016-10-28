/*
 * Cobertura - http://cobertura.sourceforge.net/
 *
 * Copyright (C) 2005 Grzegorz Lukasik
 *
 * Note: This file is dual licensed under the GPL and the Apache
 * Source License (so that it can be used from both the main
 * Cobertura classes and the ant tasks).
 *
 * Cobertura is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * Cobertura is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cobertura; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 */

package net.sourceforge.cobertura.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for storing long command lines inside temporary file.
 * <p>
 * Typical usage:
 * 
 * <pre>
 *  builder = new CommandLineBuilder();
 *  builder.addArg("--someoption");
 *  builder.addArg("optionValue");
 *  ...
 *  builder.saveArgs();
 *  doSomething(builder.getCommandLineFile());
 *  builder.dispose();
 * </pre>
 * 
 * It will save options in <code>builder.getCommandLineFile(). Options
 * will be stored one in a line. To retrieve options from file helper method can
 * be used (see documentation):
 * 
 * <pre>
 * String[] args = CommandLineBuilder.preprocessCommandLineArguments(args);
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * NOTICE: No protection against line separators in arguments, should be OK for
 * Cobertura needs.
 * </p>
 * <p>
 * NOTICE: This class depends on local machine settings (line separator, default
 * encoding). If arguments are saved on different machine than they are loaded,
 * results are unspecified. No problem in Cobertura.
 * </p>
 * 
 * @author Grzegorz Lukasik
 */
public class CommandLineBuilder {
	private static final String LINESEP = System.getProperty("line.separator");

	// File that will be used to store arguments
	private File commandLineFile = null;

	// Writer that will be used to write arguments to the file
	private FileWriter commandLineWriter = null;

	/**
	 * Creates a new instance of the builder. Instances of this class should not
	 * be reused to create many command lines.
	 * 
	 * @throws IOException
	 *             if problems with creating temporary file for storing command
	 *             line occur
	 */
	public CommandLineBuilder() throws IOException {
		commandLineFile = File.createTempFile("cobertura.", ".cmdline");
		commandLineFile.deleteOnExit();
		commandLineWriter = new FileWriter(commandLineFile);
	}

	/**
	 * Adds command line argument. Each argument can be thought as a single cell
	 * in array passed to main method. This method should not be used after
	 * arguments were saved.
	 * 
	 * @param arg command line argument to save
	 * @throws IOException
	 *             if problems with temporary file occur
	 * @throws NullPointerException 
	 *             if <code>arg is null            
	 */
	public void addArg(String arg) throws IOException {
		if( arg==null)
			throw new NullPointerException();
		commandLineWriter.write(arg + LINESEP);
	}

	
	/**
	 * Adds two command line arguments. Convienience function, calls
	 * {@link #addArg(String)} two times.   
	 * 
	 * @param arg1 first command line argument to save
	 * @param arg2 second command line argument to save
	 * @throws IOException
	 *             if problems with temporary file occur
	 * @throws NullPointerException 
	 *             if any <code>arg is null            
	 */
    public void addArg(String arg1, String arg2) throws IOException {
        addArg(arg1);
        addArg(arg2);
    }

    
	/**
	 * Saves options and made file available to use. Use method
	 * {@link #getCommandLineFile} to get the file the arguments are saved in.
	 * 
	 * @throws IOException
	 *             if problems with temporary file occur
	 */
	public void saveArgs() throws IOException {
		commandLineWriter.flush();
		commandLineWriter.close();
	}

	/**
	 * Gets absolute path to the file with saved arguments. Notice, that however
	 * this method can be used as soon as an instance of this class is created,
	 * arguments should be read from the file after a call to
	 * {@link #saveArgs} method.
	 * 
	 * @return absolute path to the file with arguments
	 */
	public String getCommandLineFile() {
		return commandLineFile.getAbsolutePath();
	}
	
	private void methodOnBranchA() {
		System.out.println("This is a method that exists on branch A");
		System.out.println("This is all right");
	}

	/**
	 * Explicity frees all resources associated with this instance. Result of
	 * any other method call after disposing an instance of this class is
	 * unspecified.
	 */
	public void dispose() {
		// some call to method on branch A
		methodOnBranchA();
		commandLineFile.delete();
	}

	/**
	 * Loads arguments from file if <code>--commandsfile option is used. Checks
	 * if passed array contains <code>--commandsfile String, and if
	 * so arguments from file specified in the very next array cell are read. If
	 * there are more then one <code>--commandsfile the result is unspecified. 
	 *
	 * @return The list of arguments read from commandsfile, or
	 *         <code>args if commandsfile option was not specified
	 *         or the file cannot be read.
	 * @throws NullPointerException if args is null, or any argument is null
	 * @throws IllegalArgumentException if --commandsfile is specified as last option
	 * @throws IOException if I/O related error with temporary command line file occur
	 */
	public static String[] preprocessCommandLineArguments(String[] args) throws IOException {
		boolean hasCommandsFile = false;
		String commandsFileName = null;
		for (int i = 0; i < args.length; i++) {
			if ( args[i].equals( "--commandsfile")) {
				if( i==args.length-1) {
					throw new IllegalArgumentException("'--commandsfile' specified as last option.");
				}
				hasCommandsFile = true;
				commandsFileName = args[++i];
			}
		}

		if (hasCommandsFile) {
			List arglist = new ArrayList();
			BufferedReader bufferedReader = null;

			try {
				bufferedReader = new BufferedReader(new FileReader(
						commandsFileName));
				String line;

				while ((line = bufferedReader.readLine()) != null)
					arglist.add(line);

			} catch (IOException e) {
				throw new IOException( "Unable to read temporary commands file "
						+ commandsFileName + ".");
			} finally {
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException e) {
					}
				}
			}

			args = (String[]) arglist.toArray(new String[arglist.size()]);
		}
		return args;
	}
}