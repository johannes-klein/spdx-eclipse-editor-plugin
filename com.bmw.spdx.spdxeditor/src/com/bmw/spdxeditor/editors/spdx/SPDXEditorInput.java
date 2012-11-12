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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.spdx.rdfparser.SPDXDocument;

import com.bmw.spdxeditor.listener.SPDXModelChangedListener;


/**
 * Input data for SPDXEditor component, wraps the SPDXDocument model
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class SPDXEditorInput implements IEditorInput {
	public final String ID;
	private List<SPDXModelChangedListener> spdxModelChangedListeners = new ArrayList<SPDXModelChangedListener>();
	private SPDXDocument associatedSPDXDocument;
	private File associatedFile;
	public SPDXEditorInput(String id, SPDXDocument spdxFileObject, File filePathObject) {
		this.ID = id;
		associatedSPDXDocument = spdxFileObject;
		setAssociatedFile(filePathObject);
	}

	/**
	 * Open only one editor for each file in filesystem.
	 */
	public boolean equals(Object input) {
		String thisFileName = this.getAssociatedFile().getAbsolutePath();
		String otherFileName = null;
		if(input instanceof SPDXEditorInput) {
			otherFileName = ((SPDXEditorInput)input).getAssociatedFile().getAbsolutePath();
		} else if(input instanceof org.eclipse.ui.part.FileEditorInput) {
			otherFileName = ((org.eclipse.ui.part.FileEditorInput)input).getFile().getRawLocation().toOSString();
		}
		boolean result = otherFileName != null && thisFileName.equals(otherFileName);
		return result;
		
	}
	
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		if(this.getAssociatedFile() != null) {
			return getAssociatedFile().getName();
		} else {
			return "NEW DOCUMENT";
		}
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "Tool tip text";
	}

	public SPDXDocument getAssociatedSPDXFile() {
		return associatedSPDXDocument;
	}

	public void setAssociatedSPDXFile(SPDXDocument associatedSPDXFile) {
		this.associatedSPDXDocument = associatedSPDXFile;
	}

	public File getAssociatedFile() {
		return associatedFile;
	}

	public void setAssociatedFile(File associatedFile) {
		this.associatedFile = associatedFile;
	}

	public void dataModelChanged() {
		for(SPDXModelChangedListener listener : this.spdxModelChangedListeners) {
			listener.resourceChanged(null);
		}
		
	}
	
	public void addChangeListener(SPDXModelChangedListener listener) {
		this.spdxModelChangedListeners.add(listener);
	}
	
	public void removeChangeListener(SPDXModelChangedListener listener) {
		this.spdxModelChangedListeners.remove(listener);
	}
	
}
