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

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author Mario Enrico Ragucci, mario@whisperedshouts.de
 * @goal generate-xml
 * @phase process-resources
 */
public class GenerateIIQEntityMojo extends AbstractMojo {
	/**
	 * Folder where the XML file shall be stored.
	 *
	 * @parameter 	property="artifactDirectory"
	 * 				expression=${project.build.directory}/${project.artifactId}-${project.version}/WEB-INF/config/custom-artifacts
	 * @required
	 */
	private File artifactDirectory;

	/**
	 * Name of the XML file
	 *
	 * @parameter property="xmlEntityFile"
	 * @required
	 */
	private String xmlEntityFile;

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	public void execute() throws MojoExecutionException {
	    // Some debugging output
		getLog().debug(String.format("Value of Property %s : %s", "outputDirectory", (artifactDirectory == null)? "NULL" : artifactDirectory));
		getLog().debug(String.format("Value of Property %s : %s", "xmlEntityFile", (xmlEntityFile == null)? "NULL" : xmlEntityFile));
		
		// Check if the output directory already exists
		// If it does not, we are creating it.
		if (!artifactDirectory.exists()) {
			getLog().debug("created output directory " + artifactDirectory);
			boolean dirCreated = artifactDirectory.mkdirs();
			
			// If directory creation had not been successfull, we throw a new exception
			if(!dirCreated) {
				throw new MojoExecutionException(String.format(
				        "Directory %s does not exist and could not be created.", 
				        artifactDirectory.getAbsoluteFile()));
			}
		}

		// Initializing the list of files we are going to process
		ArrayList<File> fileList = null;
		// Check if the config folder exists. If not we throw a new exception
		if (artifactDirectory != null
				&& !artifactDirectory.exists()) {
			throw new MojoExecutionException("entity Folder does not exist!");
		} 
		// But if it does, we add all files to the list
		else {
			fileList = new ArrayList<File>();
			getLog().debug("traversing directory " + artifactDirectory);
			IIQHelper.traverseDirectory(artifactDirectory, "xml", fileList);
		}

		// After the list of files has been populated we are going to create
		// the XML file that will contain the import instructions
		try {
			getLog().info("generating deployment XML: " + xmlEntityFile);
			File f = new File(String.format("%s%s%s",
			        artifactDirectory.getAbsolutePath(),
					System.getProperty("file.separator"),
					xmlEntityFile));
			getLog().info("Full Path: " + f.getAbsolutePath());
			
			// Actually does the stuff...
			IIQHelper.createDeploymentXml(
					f, 
					fileList, 
					artifactDirectory.getAbsolutePath().toString());
		} catch (Exception e) {
			getLog().error(e);
		}
	}
}
