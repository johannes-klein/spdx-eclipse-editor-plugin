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

package com.bmw.spdxeditor.editors.spdx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.security.MessageDigest;
import java.util.Properties;
import org.spdx.rdfparser.SPDXDocument;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.RDFWriter;


/**
 * 
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 * Various utility methods
 */
public class SPDXEditorUtility {
	private Properties ossSourceIndex = new Properties();
	private String indexFile;

	private static SPDXEditorUtility _instance;
	private static String OSS_SOURCE_CODE_DIR = "X:\\10_ALL_SOURCE_CODE"; 
	public static  final String[] STANDARD_LICENSE_IDS = new String[] {
		"AAL", "AFL-1.1", "AFL-1.2", "AFL-2.0", "AFL-2.1", "AFL-3.0", "AGPL-3.0",
		"ANTLR-PD", "APL-1.0", "APSL-1.0", "APSL-1.1", "APSL-1.2", "APSL-2.0",
		"Apache-1.0", "Apache-1.1", "Apache-2.0", "Artistic-1.0", "Artistic-2.0",
		"BSD-2-Clause", "BSD-3-Clause", "BSD-4-Clause", "BSL-1.0", "CATOSL-1.1",
		"CC-BY-1.0", "CC-BY-2.0", "CC-BY-2.5", "CC-BY-3.0", "CC-BY-NC-1.0",
		"CC-BY-NC-2.0", "CC-BY-NC-2.5", "CC-BY-NC-3.0", "CC-BY-NC-ND-1.0",
		"CC-BY-NC-ND-2.0", "CC-BY-NC-ND-2.5", "CC-BY-NC-ND-3.0", "CC-BY-NC-SA-1.0",
		"CC-BY-NC-SA-2.0", "CC-BY-NC-SA-2.5", "CC-BY-NC-SA-3.0", "CC-BY-ND-1.0",
		"CC-BY-ND-2.0", "CC-BY-ND-2.5", "CC-BY-ND-3.0", "CC-BY-SA-1.0",
		"CC-BY-SA-2.0", "CC-BY-SA-2.5", "CC-BY-SA-3.0", "CC0-1.0", "CDDL-1.0",
		"CECILL-1.0", "CECILL-1.1English", "CECILL-2.0", "CECILL-B", "CECILL-C",
		"CPAL-1.0", "CPL-1.0", "CUA-OPL-1.0", "ClArtistic", "ECL-1.0", "ECL-2.0",
		"EFL-1.0", "EFL-2.0", "EPL-1.0", "EUDatagrid", "EUPL-1.0", "EUPL-1.1",
		"Entessa", "ErlPL-1.1", "Fair", "Frameworx-1.0", "GFDL-1.1", "GFDL-1.2",
		"GFDL-1.3", "GPL-1.0", "GPL-1.0+", "GPL-2.0", "GPL-2.0+",
		"GPL-2.0-with-GCC-exception", "GPL-2.0-with-autoconf-exception",
		"GPL-2.0-with-bison-exception", "GPL-2.0-with-classpath-exception",
		"GPL-2.0-with-font-exception", "GPL-3.0", "GPL-3.0+",
		"GPL-3.0-with-GCC-exception", "GPL-3.0-with-autoconf-exception",
		"HPND", "IPA", "IPL-1.0", "ISC", "LGPL-2.0", "LGPL-2.0+", "LGPL-2.1",
		"LGPL-2.1+", "LGPL-3.0", "LGPL-3.0+", "LPL-1.02", "LPPL-1.0", "LPPL-1.1",
		"LPPL-1.2", "LPPL-1.3c", "Libpng", "MIT", "MPL-1.0", "MPL-1.1", "MS-PL",
		"MS-RL", "MirOS", "Motosoto", "Multics", "NASA-1.3", "NCSA", "NGPL",
		"NPOSL-3.0", "NTP", "Naumen", "Nokia", "OCLC-2.0", "ODbL-1.0", "OFL-1.1",
		"OGTSL", "OLDAP-2.8", "OSL-1.0", "OSL-2.0", "OSL-3.0", "OpenSSL",
		"PDDL-1.0", "PHP-3.01", "PostgreSQL", "Python-2.0", "QPL-1.0",
		"RHeCos-1.1", "RPL-1.5", "RPSL-1.0", "RSCPL", "Ruby", "SAX-PD", "SPL-1.0",
		"SimPL-2.0", "Sleepycat", "SugarCRM-1.1.3", "VSL-1.0", "W3C", "WXwindows",
		"Watcom-1.0", "XFree86-1.1", "Xnet", "YPL-1.1", "ZPL-1.1", "ZPL-2.0",
		"ZPL-2.1", "Zimbra-1.3", "Zlib", "eCos-2.0", "gSOAP-1.3b"
	};
	
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
	
	public static String saveModelToString(SPDXDocument document) {
		// Save document
		Model spdxFileModel = document.getModel();
		
		//RDFWriter w = spdxFileModel.getWriter("RDF/XML-ABBREV");
		RDFWriter w = spdxFileModel.getWriter("RDF/XML");
		w.setProperty("attribtueQuoteChar","'");
		w.setProperty("showXMLDeclaration","true");
		w.setProperty("tab","3");
		
		OutputStream fileOut = new ByteArrayOutputStream();
		w.write(spdxFileModel, fileOut, "");
		return fileOut.toString();
	}
	
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
	
	public static InputStream getModelInputStream(SPDXDocument document) {
		// Save document
		Model spdxFileModel = document.getModel();
		InputStream spdxDocumentInputStream = new StringBufferInputStream("");
		RDFReader r = spdxFileModel.getReader("RDF/XML");
		r.setProperty("attribtueQuoteChar","'");
		r.setProperty("showXMLDeclaration","true");
		r.setProperty("tab","3");
		r.read(spdxFileModel, spdxDocumentInputStream, "");
		return spdxDocumentInputStream;
	}
	
	public File getFileForSHA1Hash(String hash) {
		String fileName = ossSourceIndex.getProperty(hash);
		fileName = OSS_SOURCE_CODE_DIR + "\\" + fileName;
		return new File(fileName);
	}
	
	public void setSHA1AndFile(String sha1, File file) throws Exception {
		ossSourceIndex.setProperty(sha1, file.getName());
		ossSourceIndex.store(new FileOutputStream(indexFile), "Index file");
	}
	
	/**
	 * Calculate the SHA1 value for the file given in path
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
