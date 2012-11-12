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

package com.bmw.spdxeditor.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.osgi.service.prefs.Preferences;

import com.bmw.spdxeditor.Activator;

/**
 * An Eclipse wizard for creating new SPDX files.
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class NewSPDXFileWizard extends Wizard implements INewWizard {
	private NewSPDXFileWizardPage page;
	private ISelection selection;

	/**
	 * Constructor for SampleNewWizard.
	 */
	public NewSPDXFileWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new NewSPDXFileWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */
	private void doFinish(
			String containerName,
			String fileName,
			IProgressMonitor monitor)
					throws CoreException {

		// create a sample file
		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		final IFile file = container.getFile(new Path(fileName));
		try {
			InputStream stream = openContentStream(this.page.getSpdxFileURL());
			if (file.exists()) {
				// Overwrite
				throwCoreException("File does already exist.");
				//file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}

			stream.close();
		} catch (IOException e) {
		}

		// Start monitor thread
		monitor.worked(1);	//  a non-negative number of work units just completed
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page =
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1); //  a non-negative number of work units just completed
	}

	private InputStream openContentStream(URL spdxUrl) throws CoreException {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

		Preferences prefs = new InstanceScope().getNode(Activator.PLUGIN_ID);

		// you might want to call prefs.sync() if you're worried about others changing your settings
		String spdxFileCreator = prefs.get("SPDX_DEFAULT_FILE_CREATOR", "");
		if(StringUtils.isEmpty(spdxFileCreator)) {
			this.throwCoreException("Please configure SPDX_DEFAULT_FILE_CREATOR first");
		}

		// Initialize a basic SPDX file from String 
		String contents = "<?xml version=\"1.0\"?>\n<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+ 
				"xmlns=\"http://spdx.org/rdf/terms#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"><SpdxDocument rdf:about=\""+ spdxUrl.toString() +"#SPDXANALYSIS\">" +
				"<dataLicense rdf:resource=\"http://spdx.org/licenses/PDDL-1.0\" />"+
				"<specVersion>SPDX-1.0</specVersion>" +
				"<creationInfo><CreationInfo><creator>" + spdxFileCreator +"</creator><created>" + df.format(new Date()) + "</created></CreationInfo></creationInfo>" +
				"<describesPackage><Package rdf:about=\""+ spdxUrl.toString() +"#SPDXANALYSIS?package\">" +
				"<packageVerificationCode><PackageVerificationCode><packageVerificationCodeValue>0000000000000000000000000000000000000000</packageVerificationCodeValue></PackageVerificationCode></packageVerificationCode>" +
				"<licenseConcluded rdf:resource=\"http://spdx.org/rdf/terms#noassertion\" />" +
				"<licenseDeclared rdf:resource=\"http://spdx.org/rdf/terms#noassertion\" />" +
				"<licenseInfoFromFiles rdf:resource=\"http://spdx.org/rdf/terms#noassertion\" />" +
				"</Package>" +
				"</describesPackage></SpdxDocument>"+
				"</rdf:RDF>\n";
		return new ByteArrayInputStream(contents.getBytes());
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status =
				new Status(IStatus.ERROR, "SPDXEditor", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}