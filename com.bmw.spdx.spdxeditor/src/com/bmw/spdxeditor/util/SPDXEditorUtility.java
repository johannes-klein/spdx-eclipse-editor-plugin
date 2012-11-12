/**
 * Copyright (C) 2012, Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License. 
 **/


package com.bmw.spdxeditor.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.spdx.rdfparser.SPDXDocument;
import org.spdx.rdfparser.SPDXLicenseInfo;
import org.spdx.rdfparser.SPDXLicenseSet;
import org.spdx.rdfparser.SPDXStandardLicense;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;


/**
 * Various helper functions
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class SPDXEditorUtility {
	private Properties ossSourceIndex = new Properties();
	private String indexFile;

	private static SPDXEditorUtility _instance;
	private static String OSS_SOURCE_CODE_DIR = "X:\\10_ALL_SOURCE_CODE"; 
	//private static String OSS_SOURCE_CODE_DIR = "c:\\temp\\sourceCodeTest";
	
	private SPDXEditorUtility() throws Exception {
		indexFile = OSS_SOURCE_CODE_DIR + "\\SHA1INDEX.idx";
		ossSourceIndex.load(new FileInputStream(indexFile));

	}
	
	public static SPDXEditorUtility getInstance() throws Exception {
		if(_instance == null) {
			_instance = new SPDXEditorUtility();
		}
		return _instance;
	}
	
	/**
	 * Persist the RDF model given in document to file.
	 * @param document
	 * @param file
	 * @throws FileNotFoundException
	 */
	public static void saveModelToFile(SPDXDocument document, File file) throws FileNotFoundException {
		// Save document
		Model spdxFileModel = document.getModel();
		
		//RDFWriter w = spdxFileModel.getWriter("RDF/XML-ABBREV");
		RDFWriter w = spdxFileModel.getWriter("RDF/XML");
		w.setProperty("attribtueQuoteChar","'");
		w.setProperty("showXMLDeclaration","true");
		w.setProperty("tab","3");
		
		OutputStream fileOut = new FileOutputStream(file.getAbsoluteFile());
		w.write(spdxFileModel, fileOut, "");
	}
	
	/*
	 * Identify file with given hashsum in source code file repository.
	 */
	public File getFileForSHA1Hash(String hash) {
		String fileName = ossSourceIndex.getProperty(hash);
		fileName = OSS_SOURCE_CODE_DIR + "\\" + fileName;
		return new File(fileName);
	}
	
	public void setSHA1AndFile(String sha1, File file) throws Exception {
		ossSourceIndex.setProperty(sha1, file.getName());
		ossSourceIndex.store(new FileOutputStream(indexFile), "Index file");
	}

	// TODO: What is a Copyleft license should not be defined here
	/**
	 * Determine whether licInfo represents a license considered as GPL-Like/Copyleft.
	 * Currently these are MPL, GPL, LGPL
	 * @param licInfo
	 * @return
	 */
	public static boolean isCopyleftLicense(SPDXLicenseInfo licInfo) {
		if(licInfo== null) return false;
		
		if(licInfo instanceof SPDXStandardLicense) {
			SPDXStandardLicense stdLic = (SPDXStandardLicense) licInfo;
			if(StringUtils.containsIgnoreCase(stdLic.getId(), "GPL") || StringUtils.containsIgnoreCase(stdLic.getId(), "MPL")) {
				return true;
			}
		} else if(licInfo instanceof SPDXLicenseSet) {
			SPDXLicenseSet licSet = (SPDXLicenseSet) licInfo;
			SPDXLicenseInfo[] licenses = licSet.getSPDXLicenseInfos();
			boolean result = false;
			for(SPDXLicenseInfo license : licenses) {
				result = isCopyleftLicense(license);
				if(result) return true;
			}
		}
		return false;
	}
	
	/**
	 * Calculate SHA1 value for file at given path
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static String calculateSHA1(File path) throws Exception {
		    MessageDigest md = MessageDigest.getInstance("SHA1");
		    FileInputStream fis = new FileInputStream(path);
		    byte[] dataBytes = new byte[1024];
		 
		    int nread = 0; 
		 
		    while ((nread = fis.read(dataBytes)) != -1) {
		      md.update(dataBytes, 0, nread);
		    };
		 
		    byte[] mdbytes = md.digest();
		 
		    //convert the byte to hex format
		    StringBuffer sb = new StringBuffer("");
		    for (int i = 0; i < mdbytes.length; i++) {
		    	sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		    }
		 
		    return sb.toString();
	}

}
