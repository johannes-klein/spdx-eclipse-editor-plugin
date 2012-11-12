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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Describes a license
 * 
 * All licenses have an ID.  
 * Subclasses should extend this class to add additional properties.
 * 
 * @author Gary O'Neall
 *
 */
public abstract class SPDXLicense extends SPDXLicenseInfo {
	protected String id;

	/**
	 * @param model
	 * @param licenseInfoNode
	 * @throws InvalidSPDXAnalysisException 
	 */
	SPDXLicense(Model model, Node licenseInfoNode) throws InvalidSPDXAnalysisException {
		super(model, licenseInfoNode);
		// id
		Node p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_LICENSE_ID).asNode();
		Triple m = Triple.createMatch(licenseInfoNode, p, null);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);	
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			this.id = t.getObject().toString(false);
		}
	}
	
	SPDXLicense(String id) {
		super();
		this.id = id;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
		if (licenseInfoNode != null) {
			// delete any previous created
			Property p = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_LICENSE_ID);
			model.removeAll(resource, p, null);
			// add the property
			p = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_LICENSE_ID);
			resource.addProperty(p, id);
		}
	}
	



	
	/**
	 * Create a basic SPDXLicense resource of a given type
	 * If a license with this ID already exists in the model, then that resource
	 * is returned.  For the case where a license ID already exists, the text in the resource will be updated
	 *  with the text of this license as long as the text is not null.
	 *  ID's are assumed to be unique.
	 * NOTE: the type must be a subclass of SPDXLicense
	 * @param model
	 * @param uri 
	 * @param typeURI
	 * @return
	 */
	protected Resource _createResource(Model model, Resource type, String uri) {
		Resource r = null;
		if (id != null) {
			// check to see if it exists
			Property idProperty = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, 
					SpdxRdfConstants.PROP_LICENSE_ID);
			Property typeProperty = this.model.getProperty(SpdxRdfConstants.RDF_NAMESPACE, 
					SpdxRdfConstants.RDF_PROP_TYPE);
			Triple m = Triple.createMatch(null, idProperty.asNode(), null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);	
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				if (t.getObject().toString(false).equals(this.id)) {
					Triple typeMatch = Triple.createMatch(t.getSubject(), typeProperty.asNode(), type.asNode());
					ExtendedIterator<Triple> typeTripleIter = model.getGraph().find(typeMatch);
					if (typeTripleIter.hasNext()) {
						// found it
						if (t.getSubject().isURI()) {
							r = model.createResource(t.getSubject().getURI());
						} else if (t.getSubject().isBlank()) {
							r = model.createResource(t.getSubject().getBlankNodeId());
						}
					}
				}
			}
		}
		if (r == null) {
			// need to create it
			if (uri == null || uri.isEmpty()) {
				r = model.createResource(type);
			} else {
				r = model.createResource(uri, type);
			}
			if (id != null) {
				Property idProperty = model.createProperty(SpdxRdfConstants.SPDX_NAMESPACE, 
						SpdxRdfConstants.PROP_LICENSE_ID);
				r.addProperty(idProperty, this.id);
			}
		}
		return r;
	}
	
	@Override
	public int hashCode() {
		if (this.getId() == null) {
			return 0;
		} else {
			return this.getId().hashCode();
		}
	}
	
	@Override
	public boolean equals(Object comp) {
		if (comp == this) {
			return true;
		}
		if (!(comp instanceof SPDXLicense)) {
			return false;
		}
		SPDXLicense compl = (SPDXLicense)comp;
		return compl.getId().equals(this.getId());
	}

	/**
	 * @param model
	 * @param type
	 * @return
	 */
	public Resource _createResource(Model model, Resource type) {
		return _createResource(model, type, null);
	}
}
