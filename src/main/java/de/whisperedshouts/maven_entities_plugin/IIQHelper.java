package de.whisperedshouts.maven_entities_plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * a small helper tool that creates deployable IIQ-XML artifacts
 *
 * @author Mario Enrico Ragucci <mario@whisperedshouts.de>
 *
 */
public class IIQHelper {
	private final static Logger logger = Logger.getLogger(IIQHelper.class
			.getName());

	public static String[] stripLineRegex = { "<\\?xml[ a-zA-Z0-9=\"'.-]*\\?>",
			"<!DOCTYPE [a-zA-Z]* PUBLIC [\"sailpoint.dtd\" ]{1,}>" };

	/**
	 * Creates a deployable XML artifact
	 * @param outputFile
	 *            the output file to be used
	 * @param entityList
	 *            the list of entities
	 * @param tokens
	 *            the map of tokens
	 * @throws Exception
	 */
	public static void createDeploymentXml(File outputFile,
			ArrayList<File> entityList, TreeMap<String, String> tokens,
			boolean createImportCommandXml) throws Exception {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "createDeploymentXml");
		}

		// Initializing
		BufferedWriter bw = null;
		StringWriter sw = new StringWriter();


		writeXmlHeader(sw);

		if (createImportCommandXml) {
			for (File file : entityList) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST, String.format(
							"Adding importcommand for file %s", file.getName()));
				}
				sw.write(String
						.format("<ImportAction name='include' value='WEB-INF/config/%s'/>%s",
								file.getName(),
								System.getProperty("line.separator")));
			}
		} else {
			for (File file : entityList) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.log(
							Level.FINEST,
							String.format("Stripping lines for file %s",
									file.getName()));
				}
				stripLines(file, sw, tokens);
			}
		}

		writeXmlFooter(sw);

		try {
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE,
						"Creating BufferedWriter and writing file");
			}
			bw = new BufferedWriter(new FileWriter(outputFile));
			bw.write(sw.toString());
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage());
		} finally {
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, "Trying to close BufferedWriter");
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					throw new Exception(e);
				}
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "createDeploymentXml");
		}
	}

	/**
	 * Writes a standard header and an opened root element to the supplied StringWriter
	 * @param writer
	 */
	private static void writeXmlHeader(StringWriter writer) {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "writeXmlHeader");
		}

		writer.write(String.format("%s%s",
				"<?xml version='1.0' encoding='UTF-8'?>",
				System.getProperty("line.separator")));
		writer.write(String
				.format("%s%s",
						"<!DOCTYPE sailpoint PUBLIC \"sailpoint.dtd\" \"sailpoint.dtd\">",
						System.getProperty("line.separator")));
		writer.write(String.format("%s%s", "<sailpoint>",
				System.getProperty("line.separator")));

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "writeXmlHeader");
		}
	}

	/**
	 * Writes a standard footer that closes the opened root element to the supplied StringWriter
	 * @param writer
	 */
	private static void writeXmlFooter(StringWriter writer) {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "writeXmlFooter");
		}

		writer.write("</sailpoint>");

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "writeXmlFooter");
		}
	}


	/**
	 * Strips some unwanted Attributes (id, created, modified)
	 * @param line
	 *            the line to be checked
	 * @return
	 */
	private static String stripAttributes(String line) {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "stripAttributes");
		}

		String regex = "(id|created|modified)=[\"']\\w+[\"']";
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, String.format(
					"Creating pattern matcher with regex %s", regex));
		}
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(line);

		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "Starting matcher");
		}
		while (m.find()) {
			String token = m.group();
			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, String.format("Found token %s", token));
			}
			line = line.replaceAll(token, "");

		}

		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "matcher finished");
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "stripAttributes");
		}
		return line;
	}

	/**
	 * Strips some lines from the file that we do not want to include
	 * @param file
	 *            the file to be used
	 * @param writer
	 *            a Writer object
	 * @param tokens
	 *            a map of tokens
	 * @throws Exception
	 */
	private static void stripLines(File file, Writer writer,
			TreeMap<String, String> tokens) throws Exception {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "stripLines");
		}

		BufferedReader br = null;
		try {
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, String.format(
						"Creating BufferedReader of file %s", file.getName()));
			}
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				boolean allowedLine = true;
				for (String s : IIQHelper.stripLineRegex) {
					if (line.matches(s)) {
						allowedLine = false;
						break;
					}
				}
				if (allowedLine) {
					if (logger.isLoggable(Level.FINE)) {
						logger.log(Level.FINE, "stripping ids");
					}
					String allowed = stripAttributes(String.format("%s%s", line,
							System.getProperty("line.separator")));

					String regex = "@@@[a-zA-Z0-9-_]*@@@";
					if (logger.isLoggable(Level.FINE)) {
						logger.log(
								Level.FINE,
								String.format(
										"Creating pattern matcher with regex %s",
										regex));
					}
					Pattern p = Pattern.compile(regex);
					Matcher m = p.matcher(allowed);
					while (m.find()) {
						String token = m.group();
						if (logger.isLoggable(Level.FINEST)) {
							logger.log(Level.FINEST,
									String.format("Found token %s", token));
						}
						if (tokens.containsKey(token)) {
							allowed = allowed.replaceAll(token,
									tokens.get(token));
						}
					}

					if (logger.isLoggable(Level.FINE)) {
						logger.log(Level.FINE, "appending line");
					}
					writer.write(allowed);

				}
			}
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, e.getMessage());
			throw new Exception(e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage());
			throw new Exception(e);
		} finally {
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, "Trying to close BufferedReader");
			}
			if (br != null) {
				br.close();
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "stripLines");
		}
	}

	/**
	 * It traverses a Directory and returns the supplied ArrayList containing matching files
	 * @param pathObject
	 *            the path to be used
	 * @param allowedExtension
	 *            the allowed file extension (i.E. xml)
	 * @param fileList
	 *            the list of files to be used
	 */
	public static void traverseDirectory(File pathObject,
			String allowedExtension, ArrayList<File> fileList) {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "traverseDirectory");
		}

		if (pathObject.isDirectory()) {
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, String.format("%s is a directory",
						pathObject.getName()));
			}
			File[] files = pathObject.listFiles();

			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, "Iterating files");
			}
			for (File file : files) {
				if (file.isDirectory()) {
					if (logger.isLoggable(Level.FINE)) {
						logger.log(Level.FINE, String.format(
								"%s is a directory, starting recursion",
								file.getName()));
					}
					traverseDirectory(file, allowedExtension, fileList);
				} else {

					if (file.getName().endsWith(allowedExtension)) {
						if (logger.isLoggable(Level.FINE)) {
							logger.log(Level.FINE, String.format(
									"%s is a file, adding to list",
									file.getName()));
						}
						fileList.add(file);
					}
				}
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "traverseDirectory");
		}

	}

	public static TreeMap<String, String> createTokenMap(File tokenFile) {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "createTokenMap");
		}

		// Initializing
		TreeMap<String, String> tokenMap = new TreeMap<String, String>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(tokenFile));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					String[] parts = line.split("=");
					tokenMap.put(parts[0], parts[1]);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// We died. What else could we do?
					e.printStackTrace();
				}
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "createTokenMap");
		}

		return tokenMap;
	}
}
