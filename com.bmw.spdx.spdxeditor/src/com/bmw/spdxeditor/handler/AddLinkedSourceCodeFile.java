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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.SPDXDocument.SPDXPackage;
import org.spdx.rdfparser.SPDXFile;
import org.spdx.rdfparser.SPDXLicenseInfo;
import org.spdx.rdfparser.SpdxNoAssertionLicense;

import com.bmw.spdxeditor.editors.spdx.SPDXEditorInput;
import com.bmw.spdxeditor.editors.spdx.SPDXEditorUtility;

/**
 * Add a new source code file to the SPDX package.
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class AddLinkedSourceCodeFile implements IHandler {
	private static Logger logger = LoggerFactory.getLogger(AddLinkedSourceCodeFile.class);

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	/**
	 * Query for file, compute hash sum and add file + hash to current SPDX file.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = Display.getDefault().getActiveShell();
		final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IEditorInput input = HandlerUtil.getActiveEditorInput(event);
		SPDXEditorInput spdxInput = null;
		if(!(input instanceof SPDXEditorInput)) {
			return null;
		}			
		try {
			spdxInput = (SPDXEditorInput) input;
			SPDXPackage spdxPackage = spdxInput.getAssociatedSPDXFile().getSpdxPackage();

			FileDialog fileDialog = new FileDialog(shell);
			fileDialog.setText("Select source file to add");
			String pathToAddedFile = fileDialog.open();
			if(pathToAddedFile == null) return null;	// No path selected
			
			File resultFile = new File(pathToAddedFile);

			String shaHash = SPDXEditorUtility.calculateSHA1(resultFile);
			logger.info("Adding file {} with SHA1={} reference to SPDX package ",resultFile.getAbsolutePath(), shaHash);

			
			List<SPDXFile> assignedSourceCodeFiles = new ArrayList<SPDXFile>(Arrays.asList(spdxPackage.getFiles()));
			
			SPDXLicenseInfo noAssertionLicense = new SpdxNoAssertionLicense();
			SPDXFile addedFile = new SPDXFile(resultFile.getName(), "ARCHIVE", shaHash,
					noAssertionLicense,new SPDXLicenseInfo[] {noAssertionLicense}, "NOASSERTION","NOASSERTION", null);
			assignedSourceCodeFiles.add(addedFile);
			spdxPackage.setFiles((SPDXFile[]) assignedSourceCodeFiles.toArray(new SPDXFile[]{}));

			spdxInput.dataModelChanged();
		} catch (Exception e1) {
			MessageDialog.openError(shell, "Error adding file reference", e1.getMessage());
			
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
	}

}
