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

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author mario_000
 * @goal generate-xml
 * @phase process-resources
 */
public class GenerateEntityMojo extends AbstractMojo {
	/**
	 * Folder where the XML file shall be stored.
	 * 
	 * @parameter property="outputDirectory"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * Name of the XML file
	 * 
	 * @parameter property="xmlEntityFile"
	 * @required
	 */
	private String xmlEntityFile;

	/**
	 * Full path to a token file
	 * 
	 * @parameter property="tokenFile"
	 * @required
	 */
	private File tokenFile;

	/**
	 * Directory where all XML entities can be found
	 * 
	 * @parameter property="entityFolder"
	 * @required
	 */
	private File entityFolder;

	/**
	 * Whether or not to create an import command xml
	 * 
	 * @parameter property="createImportCommandXml"
	 * @required
	 */
	private boolean createImportCommandXml;

	public void execute() throws MojoExecutionException {
		if (!outputDirectory.exists()) {
			getLog().debug("created output directory " + outputDirectory);
			outputDirectory.mkdirs();
		}

		ArrayList<File> fileList = null;
		if (!entityFolder.exists()) {
			throw new MojoExecutionException("entity Folder does not exist!");
		} else {
			fileList = new ArrayList<File>();
			getLog().debug("traversing directory " + entityFolder);
			IIQHelper.traverseDirectory(entityFolder, "xml", fileList);
		}

		try {
			getLog().info("generating deployment XML " + xmlEntityFile);
			IIQHelper.createDeploymentXml(
					new File(String.format("%s%s%s",
							outputDirectory.getAbsolutePath(),
							System.getProperty("folder.separator"),
							xmlEntityFile)), fileList, IIQHelper
							.createTokenMap(tokenFile), createImportCommandXml);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
