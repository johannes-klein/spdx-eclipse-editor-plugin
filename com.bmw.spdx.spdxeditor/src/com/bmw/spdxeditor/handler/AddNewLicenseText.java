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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocument;

import com.bmw.spdxeditor.editors.spdx.SPDXEditorInput;

/**
 * Add a new custom license text to SPDX file.
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class AddNewLicenseText implements IHandler {

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {

	}

	@Override
	public void dispose() {

	}

	/**
	 * Add license text as "extracted license text" to SPDX document
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
			spdxInput = (SPDXEditorInput) input;
			try {
				SPDXDocument spdxDocument = spdxInput.getAssociatedSPDXFile();
				spdxDocument.addNewExtractedLicenseInfo("New extracted license text");
				spdxInput.dataModelChanged();
			} catch (InvalidSPDXAnalysisException e) {
				e.printStackTrace();
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
