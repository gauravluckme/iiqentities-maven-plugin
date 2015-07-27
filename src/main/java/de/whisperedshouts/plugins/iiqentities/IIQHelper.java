package de.whisperedshouts.plugins.iiqentities;

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * a small helper tool that creates deployable IIQ-XML artifacts
 *
 * @author Mario Enrico Ragucci, mario@whisperedshouts.de
 *
 */
public class IIQHelper {
    // A logger. Not much to say about this one
	private final static Logger logger = Logger.getLogger(IIQHelper.class
			.getName());
	
	// This regular expression is used to remove certain attribute/value pairs
	// from the given line.
	private final static String stripAttributeRegex = "(id|created|modified)=[\"']\\w+[\"']";

	/**
	 * Creates a deployable XML artifact
	 * @param outputFile
	 *            the output file to be used
	 * @param entityList
	 *            the list of entities
	 * @param tokens
	 *            the map of tokens
	 * @throws Exception thrown when there is an exception
	 */
	public static void createDeploymentXml(File outputFile,
			ArrayList<File> entityList, 
			String baseDirectory) throws Exception {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "createDeploymentXml");
		}

		// Initializing
		BufferedWriter bw = null;
		StringWriter sw = new StringWriter();

		// Write a standard XML header to our file
		writeXmlHeader(sw);

		// We want to create an import file instead of concatenating it to a deployable file
        // Doing this for every file we of our configuration files
        for (File file : entityList) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, String.format(
                        "Adding importcommand for file %s", file.getName()));
            }
            
            stripAttributesCopy(file);
            
            // This will write the ImportAction to the XML file
            sw.write(String
                    .format("<ImportAction name='include' value='WEB-INF/config/custom-artifacts/%s'/>%s",
                            file.getAbsoluteFile().toString().replaceAll(
                                    String.format("%s/", baseDirectory), ""),
                            System.getProperty("line.separator")));
        }
    
        // Write a standard XML footer to our file
		writeXmlFooter(sw);

		try {
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE,
						"Creating BufferedWriter and writing file");
			}
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8")));
			bw.write(sw.toString());
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage());
			throw new IOException(e);
		} finally {
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, "Trying to close BufferedWriter");
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
				    logger.log(Level.SEVERE, e.getMessage());
					throw new Exception(e);
				}
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "createDeploymentXml");
		}
	}

	/**
	 * Creates a copy of a file, stripped of all corrupting attributes
	 * @param file the file to be copied and stripped
	 * @throws IOException whenever there was an error with the I/O
	 */
	private static void stripAttributesCopy(File file) throws IOException {
	    if (logger.isLoggable(Level.FINE)) {
            logger.entering(IIQHelper.class.getName(), "stripAttributesCopy");
        }
	    File tempFile = null;
	    BufferedReader br = null;
	    BufferedWriter wr = null;
        try {
            tempFile = File.createTempFile("identityIQ", String.valueOf(Math.random()));
            Files.copy(file.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            br = new BufferedReader(new FileReader(tempFile));
            wr = new BufferedWriter(new FileWriter(file));
            String line = null;
            while((line = br.readLine()) != null) {
                wr.write(String.format("%s%s", 
                        stripAttributes(line), 
                        System.getProperty("line.separator")));
            }
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new IOException(e);
        }finally {
            if(br != null) {
                br.close();
            }
            
            if(wr != null) {
                wr.close();
            }
            
            tempFile.delete();
        }
        
        if (logger.isLoggable(Level.FINE)) {
            logger.exiting(IIQHelper.class.getName(), "stripAttributesCopy");
        }
    }

    /**
	 * Writes a standard header and an opened root element to the supplied StringWriter
	 * @param writer the {@link StringWriter} to be used
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
	 * @param writer the {@link StringWriter} to be used 
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
	 * 			  the line stripped from the unwanted attributes
	 */
	private static String stripAttributes(String line) {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "stripAttributes");
		}

		
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, String.format(
					"Creating pattern matcher with regex %s", IIQHelper.stripAttributeRegex));
		}
		Pattern p = Pattern.compile(IIQHelper.stripAttributeRegex);
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
	 * traverses a Directory and returns the supplied ArrayList containing matching files
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
			if(files != null) {
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
							if(fileList == null) {
								if (logger.isLoggable(Level.FINE)) {
									logger.log(Level.FINE, String.format(
											"%s is currently null, which should not happen. Creating a new instance",
											"fileList"));
								}
								fileList = new ArrayList<File>();
							}
							fileList.add(file);
						}
					}
				}
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "traverseDirectory");
		}

	}

	/**
	 * Creates a {@link TreeMap} with tokens taken from the supplied tokenFile
	 * @param tokenFile a {@link File} containing tokens (key=value)
	 * @return a {@link TreeMap} containing tokens
	 * @throws Exception if anything cannot be caught correct
	 */
	@Deprecated
	public static TreeMap<String, String> createTokenMap(File tokenFile) throws Exception {
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(IIQHelper.class.getName(), "createTokenMap");
		}

		// Initializing
		TreeMap<String, String> tokenMap = new TreeMap<String, String>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(tokenFile), Charset.forName("UTF-8")));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					String[] parts = line.split("=");
					tokenMap.put(parts[0], parts[1]);
				}

			}
		} catch (Exception e) {
		    logger.log(Level.SEVERE, e.getMessage());
		    throw new Exception(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// We died. What else could we do?
					logger.log(Level.SEVERE, e.getMessage());
					throw new IOException(e);
				}
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(IIQHelper.class.getName(), "createTokenMap");
		}

		return tokenMap;
	}
}
