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


package com.bmw.spdxeditor;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;

import com.bmw.spdxeditor.editors.spdx.SPDXEditor;

/**
 * 
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class SPDXPreferencesPage extends PreferencePage implements
		IWorkbenchPreferencePage {
	private Combo creatorTypes;
	private Text creatorText;
	private static String DEFAULT_FILE_CREATOR = "SPDX_DEFAULT_FILE_CREATOR";
	private static Logger logger = LoggerFactory.getLogger(SPDXPreferencesPage.class);

	public SPDXPreferencesPage() {
		
	}

	public SPDXPreferencesPage(String title) {
		super(title);
		
	}

	public SPDXPreferencesPage(String title, ImageDescriptor image) {
		super(title, image);

	}

	@Override
	public void init(IWorkbench workbench) {
	}
	
	 protected IPreferenceStore doGetPreferenceStore() {
	      return Activator.getDefault().getPreferenceStore();
	 }

	 
	@Override
	protected Control createContents(Composite parent) {
		IPreferenceStore store = getPreferenceStore();
		String fileCreatorValue = store.getString(DEFAULT_FILE_CREATOR);
		String fileCreatorType = null;
		if(StringUtils.isEmpty(fileCreatorValue)) {
			fileCreatorValue = "NOASSERTION";
			fileCreatorType = "";
		} else {
			fileCreatorType = StringUtils.substringBefore(fileCreatorValue, ":");
			fileCreatorValue = StringUtils.substringAfter(fileCreatorValue, ":").trim();
		}
		
		// Describe layout for this group panel
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		parent.setLayout(gridLayout);

		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Package creator default");
		
		// Person, Organization, Tool
	    String items[] = { "Person", "Organization", "Tool","" };
	    creatorTypes = new Combo(parent, SWT.READ_ONLY);
	    creatorTypes.setItems(items);
	    creatorTypes.setText(fileCreatorType);

	    
		creatorText = new Text(parent, SWT.BORDER);
		creatorText.setText(fileCreatorValue);
		
		 
		return null;
	}

	protected void performDefaults() {
		creatorTypes.setText("");
		creatorText.setText("NOASSERTION");
    }
	protected void performApply() {
		System.out.println("performApply");
		IPreferenceStore store = getPreferenceStore();
        if(creatorText.getText().equalsIgnoreCase("noassertion")) {
        	store.setValue(DEFAULT_FILE_CREATOR, "NOASSERTION");
        } else if(StringUtils.isEmpty(creatorTypes.getText())) {
        	MessageDialog.openError(getShell(), "Error", "Select value in dropdown box first.");
        } else if(StringUtils.isEmpty(creatorText.getText())){
        	MessageDialog.openError(getShell(), "Error", "Enter value for creator name.");
        } else {
        	String defaultCreator = creatorTypes.getText() + ":" + creatorText.getText();
        	logger.info("Default creator set to: {}", defaultCreator);
        	store.setValue(DEFAULT_FILE_CREATOR, defaultCreator);
        	MessageDialog.openInformation(getShell(), "Creator set", "Default SPDX file creator has been set.");
        }
        System.out.println("storeNeedsSaving: " + store.needsSaving());
    }


}
