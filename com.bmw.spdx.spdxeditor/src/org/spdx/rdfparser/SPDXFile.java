/**
 * Copyright (c) 2011 Source Auditor Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.spdx.rdfparser;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * @author Source Auditor
 *
 */
public class SPDXFile {
	
	static final Logger logger = Logger.getLogger(SPDXFile.class.getName());
	private Model model = null;
	private Resource resource = null;
	private String name;
	private SPDXLicenseInfo concludedLicenses;
	private String sha1;
	private String type;
	private SPDXLicenseInfo[] seenLicenses;
	private String licenseComments;
	private String copyright;
	private DOAPProject[] artifactOf;
	private String comment = null;
	
	public static HashMap<String, String> FILE_TYPE_TO_RESOURCE = new HashMap<String, String>();
	public static HashMap<String, String> RESOURCE_TO_FILE_TYPE = new HashMap<String, String>();

	static {
		FILE_TYPE_TO_RESOURCE.put(SpdxRdfConstants.FILE_TYPE_SOURCE, 
				SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.PROP_FILE_TYPE_SOURCE);
		RESOURCE_TO_FILE_TYPE.put(SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.PROP_FILE_TYPE_SOURCE, 
				SpdxRdfConstants.FILE_TYPE_SOURCE);
		FILE_TYPE_TO_RESOURCE.put(SpdxRdfConstants.FILE_TYPE_BINARY, 
				SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.PROP_FILE_TYPE_BINARY);
		RESOURCE_TO_FILE_TYPE.put(SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.PROP_FILE_TYPE_BINARY, 
				SpdxRdfConstants.FILE_TYPE_BINARY);
		FILE_TYPE_TO_RESOURCE.put(SpdxRdfConstants.FILE_TYPE_ARCHIVE, 
				SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.PROP_FILE_TYPE_ARCHIVE);
		RESOURCE_TO_FILE_TYPE.put(SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.PROP_FILE_TYPE_ARCHIVE, 
				SpdxRdfConstants.FILE_TYPE_ARCHIVE);
		FILE_TYPE_TO_RESOURCE.put(SpdxRdfConstants.FILE_TYPE_OTHER, 
				SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.PROP_FILE_TYPE_OTHER);
		RESOURCE_TO_FILE_TYPE.put(SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.PROP_FILE_TYPE_OTHER, 
				SpdxRdfConstants.FILE_TYPE_OTHER);
	};
	
	/**
	 * Convert a node to a resource
	 * @param cmodel
	 * @param cnode
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	private Resource convertToResource(Model cmodel, Node cnode) throws InvalidSPDXAnalysisException {
		if (cnode.isBlank()) {
			return cmodel.createResource(cnode.getBlankNodeId());
		} else if (cnode.isURI()) {
			return cmodel.createResource(cnode.getURI());
		} else {
			throw(new InvalidSPDXAnalysisException("Can not create a file from a literal"));
		}
	}
	/**
	 * Construct an SPDX File form the fileNode
	 * @param fileNode RDF Graph node representing the SPDX File
	 * @throws InvalidSPDXAnalysisException 
	 */
	public SPDXFile(Model model, Node fileNode) throws InvalidSPDXAnalysisException {
		this.model = model;
		this.resource = convertToResource(model, fileNode);
		// name
		Node p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_NAME).asNode();
		Triple m = Triple.createMatch(fileNode, p, null);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			this.name = t.getObject().toString(false);
		}
		// checksum - sha1
		p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_CHECKSUM).asNode();
		m = Triple.createMatch(fileNode, p, null);
		tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			SPDXChecksum cksum = new SPDXChecksum(model, t.getObject());
			if (cksum.getAlgorithm().equals(SpdxRdfConstants.ALGORITHM_SHA1)) {
				this.sha1 = cksum.getValue();
			}
		}
		// type
		p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_TYPE).asNode();
		m = Triple.createMatch(fileNode, p, null);
		tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			if (t.getObject().isLiteral()) {
				// the following is for compatibility with previous versions of the tool which used literals for the file type
				this.type = t.getObject().toString(false);
			} else if (t.getObject().isURI()) {
				this.type = RESOURCE_TO_FILE_TYPE.get(t.getObject().getURI());
				if (this.type == null) {
					throw(new InvalidSPDXAnalysisException("Invalid URI for file type resource - must be one of the individual file types in http://spdx.org/rdf/terms"));
				}
			} else {
				throw(new InvalidSPDXAnalysisException("Invalid file type property - must be a URI type specified in http://spdx.org/rdf/terms"));
			}			
		}
		// concluded License
		ArrayList<SPDXLicenseInfo> alLic = new ArrayList<SPDXLicenseInfo>();
		p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_LICENSE).asNode();
		m = Triple.createMatch(fileNode, p, null);
		tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			alLic.add(SPDXLicenseInfoFactory.getLicenseInfoFromModel(model, t.getObject()));
		}
		if (alLic.size() > 1) {
			throw(new InvalidSPDXAnalysisException("Too many concluded licenses for file"));
		}
		if (alLic.size() == 0) {
			throw(new InvalidSPDXAnalysisException("Missing required concluded license"));
		}
		this.concludedLicenses = alLic.get(0);
		// seenLicenses
		alLic.clear();		
		p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_SEEN_LICENSE).asNode();
		m = Triple.createMatch(fileNode, p, null);
		tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			alLic.add(SPDXLicenseInfoFactory.getLicenseInfoFromModel(model, t.getObject()));
		}
		this.seenLicenses = alLic.toArray(new SPDXLicenseInfo[alLic.size()]);
		//licenseComments
		p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_LIC_COMMENTS).asNode();
		m = Triple.createMatch(fileNode, p, null);
		tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			this.licenseComments = t.getObject().toString(false);
		}
		//copyright
		p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_COPYRIGHT).asNode();
		m = Triple.createMatch(fileNode, p, null);
		tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			if (t.getObject().isURI()) {
				// check for standard value types
				if (t.getObject().getURI().equals(SpdxRdfConstants.URI_VALUE_NOASSERTION)) {
					this.copyright = SpdxRdfConstants.NOASSERTION_VALUE;
				} else if (t.getObject().getURI().equals(SpdxRdfConstants.URI_VALUE_NONE)) {
					this.copyright = SpdxRdfConstants.NONE_VALUE;
				} else {
					this.copyright = t.getObject().toString(false);
				}
			} else {
				this.copyright = t.getObject().toString(false);
			}
		}
		//comment
		p = model.getProperty(SpdxRdfConstants.RDFS_NAMESPACE, SpdxRdfConstants.RDFS_PROP_COMMENT).asNode();
		m = Triple.createMatch(fileNode, p, null);
		tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			this.comment = tripleIter.next().getObject().toString(false);
		}
		//artifactOf
		ArrayList<DOAPProject> alProjects = new ArrayList<DOAPProject>();
		p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_ARTIFACTOF).asNode();
		m = Triple.createMatch(fileNode, p, null);
		tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			alProjects.add(new DOAPProject(model, t.getObject()));
		}
		this.artifactOf = alProjects.toArray(new DOAPProject[alProjects.size()]);
	}
	
	public Resource createResource(Model model) throws InvalidSPDXAnalysisException {
		Resource type = model.createResource(SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.CLASS_SPDX_FILE);
		Resource retval = model.createResource(type);
		populateModel(model, retval);
		this.resource = retval;
		return retval;
	}
	
	/**
	 * Populates a Jena RDF model with the information from this file declaration
	 * @param licenseResource
	 * @param model
	 * @throws InvalidSPDXAnalysisException 
	 */
	private void populateModel(Model model, Resource fileResource) throws InvalidSPDXAnalysisException {
		// name
		Property p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_NAME);
		fileResource.addProperty(p, this.getName());

		if (this.sha1 != null) {
			// sha1
			SPDXChecksum cksum = new SPDXChecksum(SpdxRdfConstants.ALGORITHM_SHA1, sha1);
			Resource cksumResource = cksum.createResource(model);

			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_CHECKSUM);
			fileResource.addProperty(p, cksumResource);
		}
		// type
		p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_TYPE);
		Resource fileTypeResource = fileTypeStringToTypeResource(this.getType(), model);
		fileResource.addProperty(p, fileTypeResource);

		// concludedLicenses
		if (this.concludedLicenses != null) {
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_LICENSE);
			Resource lic = this.concludedLicenses.createResource(model);
			fileResource.addProperty(p, lic);
		}

		// seenLicenses
		if (this.seenLicenses != null && this.seenLicenses.length > 0) {
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_SEEN_LICENSE);
			for (int i = 0; i < this.seenLicenses.length; i++) {
				Resource lic = this.seenLicenses[i].createResource(model);
				fileResource.addProperty(p, lic);
			}
		}
		//licenseComments
		if (this.licenseComments != null) {
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_LIC_COMMENTS);
			fileResource.addProperty(p, this.getLicenseComments());
		}
		//copyright
		if (this.copyright != null) {
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_COPYRIGHT);
			if (copyright.equals(SpdxRdfConstants.NONE_VALUE)) {
				Resource r = model.createResource(SpdxRdfConstants.URI_VALUE_NONE);
				fileResource.addProperty(p, r);
			} else if (copyright.equals(SpdxRdfConstants.NOASSERTION_VALUE)) {
				Resource r = model.createResource(SpdxRdfConstants.URI_VALUE_NOASSERTION);
				fileResource.addProperty(p, r);
			} else {
				fileResource.addProperty(p, this.getCopyright());
			}
		}
		//comment
		if (this.comment != null) {
			p = model.createProperty(SpdxRdfConstants.RDFS_NAMESPACE, SpdxRdfConstants.RDFS_PROP_COMMENT);
			fileResource.addProperty(p, this.comment);
		}

		//artifactof
		if (this.artifactOf != null) {
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_ARTIFACTOF);
			for (int i = 0; i < artifactOf.length; i++) {
				Resource projectResource = artifactOf[i].createResource(model);
				fileResource.addProperty(p, projectResource);
			}
		}		
		this.model = model;
		this.resource = fileResource;
	}
	
	public SPDXFile(String name, String type, String sha1,
			SPDXLicenseInfo concludedLicenses,
			SPDXLicenseInfo[] seenLicenses, String licenseComments,
			String copyright, DOAPProject[] artifactOf, String comment) {
		this.name = name;
		this.type = type;
		this.sha1 = sha1;
		this.concludedLicenses = concludedLicenses;
		this.seenLicenses = seenLicenses;
		this.licenseComments = licenseComments;
		this.copyright = copyright;
		this.artifactOf = artifactOf;
		this.comment = comment;
	}
	
	public SPDXFile(String name, String type, String sha1,
			SPDXLicenseInfo concludedLicenses,
			SPDXLicenseInfo[] seenLicenses, String licenseComments,
			String copyright, DOAPProject[] artifactOf) {
		this(name, type, sha1, concludedLicenses, seenLicenses,
				licenseComments, copyright, artifactOf, null);
	}
	
	/**
	 * @return the seenLicenses
	 */
	public SPDXLicenseInfo[] getSeenLicenses() {
		return seenLicenses;
	}
	/**
	 * @param seenLicenses the seenLicenses to set
	 */
	public void setSeenLicenses(SPDXLicenseInfo[] seenLicenses) {
		this.seenLicenses = seenLicenses;
		if (this.model != null && this.resource != null) {
			Property p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_SEEN_LICENSE);
			model.removeAll(this.resource, p, null);
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_SEEN_LICENSE);

			for (int i = 0; i < seenLicenses.length; i++) {
				Resource lic = seenLicenses[i].createResource(model);
				this.resource.addProperty(p, lic);
			}
		}
	}
	/**
	 * @return the licenseComments
	 */
	public String getLicenseComments() {
		return licenseComments;
	}
	/**
	 * @param licenseComments the licenseComments to set
	 */
	public void setLicenseComments(String licenseComments) {
		this.licenseComments = licenseComments;
		if (this.model != null && this.resource != null) {
			Property p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_LIC_COMMENTS);
			model.removeAll(this.resource, p, null);
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_LIC_COMMENTS);
			this.resource.addProperty(p, this.getLicenseComments());
		}	
	}
	
	public String getComment() {
		return this.comment;
	}
	
	public void setComment(String comment) {
	this.comment = comment;
		if (this.model != null && this.resource != null) {
			Property p = model.createProperty(SpdxRdfConstants.RDFS_NAMESPACE, SpdxRdfConstants.RDFS_PROP_COMMENT);
			model.removeAll(this.resource, p, null);
			p = model.createProperty(SpdxRdfConstants.RDFS_NAMESPACE, SpdxRdfConstants.RDFS_PROP_COMMENT);
			this.resource.addProperty(p, this.comment);
		}
	}
	/**
	 * @return the copyright
	 */
	public String getCopyright() {
		return copyright;
	}
	/**
	 * @param copyright the copyright to set
	 */
	public void setCopyright(String copyright) {
		this.copyright = copyright;
		if (this.model != null && this.resource != null) {
			Property p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_COPYRIGHT);
			model.removeAll(this.resource, p, null);
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_COPYRIGHT);
			if (copyright.equals(SpdxRdfConstants.NONE_VALUE)) {
				Resource r = model.createResource(SpdxRdfConstants.URI_VALUE_NONE);
				this.resource.addProperty(p, r);
			} else if (copyright.equals(SpdxRdfConstants.NOASSERTION_VALUE)) {
				Resource r = model.createResource(SpdxRdfConstants.URI_VALUE_NOASSERTION);
				this.resource.addProperty(p, r);
			} else {
				this.resource.addProperty(p, this.getCopyright());
			}
		}	
	}

	
	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
		if (this.model != null && this.resource != null) {
			Property p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_NAME);
			model.removeAll(this.resource, p, null);
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_NAME);
			this.resource.addProperty(p, this.getName());
		}
	}
	/**
	 * @return the fileLicenses
	 */
	public SPDXLicenseInfo getConcludedLicenses() {
		return this.concludedLicenses;
	}
	/**
	 * @param fileLicenses the fileLicenses to set
	 */
	public void setConcludedLicenses(SPDXLicenseInfo fileLicenses) {
		this.concludedLicenses = fileLicenses;
		if (this.model != null && this.resource != null) {
			Property p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_LICENSE);
			model.removeAll(this.resource, p, null);
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_LICENSE);
			Resource lic = fileLicenses.createResource(model);
			this.resource.addProperty(p, lic);
		}
	}
	/**
	 * @return the sha1
	 */
	public String getSha1() {
		return this.sha1;
	}
	/**
	 * @param sha1 the sha1 to set
	 */
	public void setSha1(String sha1) {
		this.sha1 = sha1;
		if (this.model != null && this.resource != null) {
			Property p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_CHECKSUM);
			model.removeAll(this.resource, p, null);
			SPDXChecksum cksum = new SPDXChecksum(SpdxRdfConstants.ALGORITHM_SHA1, sha1);
			Resource cksumResource = cksum.createResource(model);

			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_CHECKSUM);
			this.resource.addProperty(p, cksumResource);
		}
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return this.type;
	}
	/**
	 * @param type the type to set
	 * @throws InvalidSPDXAnalysisException 
	 */
	public void setType(String type) throws InvalidSPDXAnalysisException {
		this.type = type;
		if (this.model != null && this.resource != null) {
			Resource typeResource = fileTypeStringToTypeResource(type, this.model);
			Property p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_TYPE);
			model.removeAll(this.resource, p, null);
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_TYPE);
			this.resource.addProperty(p, typeResource);
		}
	}

	/**
	 * Converts a string file type to an RDF resource
	 * @param fileType
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static Resource  fileTypeStringToTypeResource(String fileType, Model model) throws InvalidSPDXAnalysisException {
		String resourceUri = FILE_TYPE_TO_RESOURCE.get(fileType);
		if (resourceUri == null) {
			// not sure if we want to throw an exception here or just set to "Other"
			throw(new InvalidSPDXAnalysisException("Invalid file type: "+fileType));
			//resourceUri = SpdxRdfConstants.PROP_FILE_TYPE_OTHER;
			
		}
		Resource retval = model.createResource(resourceUri);
		return retval;
	}
	
	public static String fileTypeResourceToString(Resource fileTypeResource) throws InvalidSPDXAnalysisException {
		if (!fileTypeResource.isURIResource()) {
			throw(new InvalidSPDXAnalysisException("File type resource must be a URI."));
		}
		String retval = fileTypeResource.getURI();
		if (retval == null) {
			throw(new InvalidSPDXAnalysisException("Not a recognized file type for an SPDX docuement."));
		}
		return retval;
	}

	/**
	 * @return the artifactOf
	 */
	public DOAPProject[] getArtifactOf() {
		return artifactOf;
	}

	/**
	 * @param artifactOf the artifactOf to set
	 */
	public void setArtifactOf(DOAPProject[] artifactOf) {
		this.artifactOf = artifactOf;
		if (this.model != null && this.resource != null && this.name != null) {
			Property p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_ARTIFACTOF);
			model.removeAll(this.resource, p, null);
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_ARTIFACTOF);
			for (int i = 0; i < artifactOf.length; i++) {
				// we need to check on these if it already exists
				Resource projectResource = null;
				String uri = artifactOf[i].getProjectUri();
				if (uri != null) {
					projectResource = model.createResource(uri);
				} else {
					projectResource = artifactOf[i].createResource(model);
				}
				this.resource.addProperty(p, projectResource);
			}
		}
	}

	/**
	 * @return
	 */
	public ArrayList<String> verify() {
		ArrayList<String> retval = new ArrayList<String>();
		// fileName
		String fileName = this.getName();
		if (fileName == null || fileName.isEmpty()) {
			retval.add("Missing required name for file");
			fileName = "UNKNOWN";
		}
		// fileType
		//TODO: Resolve whether mandatory or not
		String fileType = this.getType();
		if (fileType == null || fileType.isEmpty()) {
			retval.add("Missing required file type");
		} else {
			String verifyFileType = SpdxVerificationHelper.verifyFileType(fileType);
			if (verifyFileType != null) {
				retval.add(verifyFileType + "; File - "+fileName);
			}
		}
		// copyrightText
		//TODO: Resolve whether mandatory or not
		String copyrightText = this.getCopyright();
		if (copyrightText == null || copyrightText.isEmpty()) {
			retval.add("Missing required copyright text for file "+fileName);
		}
		// license comments
		@SuppressWarnings("unused")
		String comments = this.getLicenseComments();
		// license concluded
		SPDXLicenseInfo concludedLicense = this.getConcludedLicenses();
		if (concludedLicense == null) {
			retval.add("Missing required concluded license for file "+fileName);
		} else {
			retval.addAll(concludedLicense.verify());
		}
		// license info in files
		SPDXLicenseInfo[] licenseInfosInFile = this.getSeenLicenses();
		if (licenseInfosInFile == null || licenseInfosInFile.length == 0) {
			retval.add("Missing required license information in file for file "+fileName);
		} else {
			for (int i = 0; i < licenseInfosInFile.length; i++) {
				retval.addAll(licenseInfosInFile[i].verify());
			}
		}
		// checksum
		String checksum = this.getSha1();
		if (checksum == null || checksum.isEmpty()) {
			retval.add("Missing required checksum for file "+fileName);
		} else {
			String verify = SpdxVerificationHelper.verifyChecksumString(checksum);
			if (verify != null) {
				retval.add(verify + "; file "+fileName);
			}
		}
		// artifactOf - limit to one
		DOAPProject[] projects = this.getArtifactOf();
		if (projects != null) {
			if (projects.length > 1) {
				retval.add("To many artifact of's - limit to one per file.  File: "+fileName);
			}
			for (int i = 0;i < projects.length; i++) {
				retval.addAll(projects[i].verify());
			}
		}	
		// comment - verify there are not more than one
		if (this.model != null && this.resource != null) {
			Node p = model.getProperty(SpdxRdfConstants.RDFS_NAMESPACE, SpdxRdfConstants.RDFS_PROP_COMMENT).asNode();
			Triple m = Triple.createMatch(this.resource.asNode(), p, null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);	
			int count = 0;
			while (tripleIter.hasNext()) {
				tripleIter.next();
				count++;
			}
			if (count > 1) {
				retval.add("More than one file comment for file "+this.name);
			}
		}	
		return retval;
	}
}
