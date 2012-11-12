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


package com.bmw.spdxeditor.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.SPDXDocument.SPDXPackage;
import org.spdx.rdfparser.SPDXFile;

import com.bmw.spdxeditor.editors.spdx.SPDXEditorInput;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Handler called when a source code file referenced from SPDX file is removed.
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class RemoveLinkedSourceCodeFile implements IHandler {
	private static Logger logger = LoggerFactory.getLogger(RemoveLinkedSourceCodeFile.class);

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorInput input = HandlerUtil.getActiveEditorInput(event);
		SPDXEditorInput spdxInput = null;
		Shell shell = Display.getDefault().getActiveShell();

		if(!(input instanceof SPDXEditorInput)) {
			return null;
		}			
		
		// Get selected source code file
		try {
			spdxInput = (SPDXEditorInput) input;
			
			SPDXPackage spdxPackage = spdxInput.getAssociatedSPDXFile().getSpdxPackage();
			IStructuredSelection currentSelection = (IStructuredSelection) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
			List<SPDXFile> selectionList = currentSelection.toList();
			
			List<SPDXFile> assignedSourceCodeFiles = new ArrayList<SPDXFile>(Arrays.asList(spdxPackage.getFiles()));
			
			/*
			 * Delete file from list of assigned files (SPDXFile does not overwrite equals()):
			 * Compare name + checksum
			 * 
			 */
			Model model = spdxInput.getAssociatedSPDXFile().getModel();
			
			for(SPDXFile file : selectionList) {
				logger.info("Remove file {}, hash={}",  file.getName(), file.getSha1());
						
				// Remove link from package -> SPDX File
				SPDXFile fileToBeUnlinked = getSPDXFileByNameAndChecksumInList(assignedSourceCodeFiles, file.getName(), file.getSha1());				
				assignedSourceCodeFiles.remove(fileToBeUnlinked);
				
				Resource resourceToBeUnlinked = fileToBeUnlinked.createResource(model);

				String SPDX_NAMESPACE = "http://spdx.org/rdf/terms#";
				String PROP_SPDX_FILE = "referencesFile";
				Node p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_FILE).asNode();
								
			}
			logger.info("# Remaining files after deletion {}", assignedSourceCodeFiles.size());
			
			spdxPackage.setFiles((SPDXFile[]) assignedSourceCodeFiles.toArray(new SPDXFile[]{}));
			String SPDX_NAMESPACE = "http://spdx.org/rdf/terms#";
			
			spdxInput.dataModelChanged();
		} catch (Exception e1) {
			MessageDialog.openError(shell, "Error when removing linked file",e1.getMessage());
		}

		return null;
	}
	
		private SPDXFile getSPDXFileByNameAndChecksumInList(List<SPDXFile> listToSearchIn, String name, String checksumValue) {
		for(SPDXFile singleFile : listToSearchIn) {
			if(singleFile.getName().equals(name) && singleFile.getSha1().equals(checksumValue)) return singleFile; 
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

}

