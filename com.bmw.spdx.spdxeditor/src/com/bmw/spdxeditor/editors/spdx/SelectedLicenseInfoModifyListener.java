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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXLicenseInfo;
import org.spdx.rdfparser.SPDXLicenseInfoFactory;
import org.spdx.rdfparser.SPDXStandardLicense;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * Listener is called from UI when the license selected is changed and applies changed UI selection for given licenseType (concluded/declared)
 * to model.
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class SelectedLicenseInfoModifyListener implements  ModifyListener {
	private static Logger logger = LoggerFactory.getLogger(SelectedLicenseInfoModifyListener.class);

	private Object licenseContainer;
	private String licenseType;
	private Model model;
	
	public SelectedLicenseInfoModifyListener(Object licenseContainer, String licenseType, Model model) {
		this.licenseContainer = licenseContainer;
		this.licenseType = licenseType;
		this.model = model;
	}

	@Override
	public void modifyText(ModifyEvent e) {
		String licenseChoiceID = null;
		SPDXLicenseInfo selectedLicense = null;

		if(!(e.getSource() instanceof Combo)) return;
		Combo source = (Combo) e.getSource();
		Composite sourceParent = source.getParent();
		Map<String, SPDXLicenseInfo> allLicensingInfoInSPDXFile = (Map<String, SPDXLicenseInfo>) source.getData();

		try {
			// The chosen license ID (string)
			licenseChoiceID = source.getText();
			
			// The chosen license (SPDX license)
			selectedLicense = allLicensingInfoInSPDXFile.get(licenseChoiceID);
			// Lazy creation of license resources for standard license IDs
			if(selectedLicense == null) {
				selectedLicense = SPDXLicenseInfoFactory.getStandardLicenseById(licenseChoiceID);
				assert(selectedLicense != null);
			}
			// Remove concludedLicense information from model if old licenses is StandardLicense
			SPDXLicenseInfo oldLicenseInfo = this.getLicenseInfo();
			Resource oldConcludedLicenseResource = oldLicenseInfo.createResource(model);
			if(oldLicenseInfo instanceof SPDXStandardLicense) {
				logger.debug("Remove old license {}", oldLicenseInfo);
				oldConcludedLicenseResource.removeProperties();
			}
			
			this.setLicenseInfo(selectedLicense);
		} catch (InvalidSPDXAnalysisException e1) {
			MessageDialog.openError(sourceParent.getShell(), "Validation Error", e1.getMessage());					
		}
	}

	private void setLicenseInfo(SPDXLicenseInfo licenseInfo) {
		try {
			logger.debug("Setting license info to {}", licenseInfo);
			Method setterMethod = licenseContainer.getClass().getMethod("set" + licenseType, SPDXLicenseInfo.class);
			setterMethod.invoke(licenseContainer, licenseInfo);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}

	}
	/**
	 * Get license set for licenseType in model.
	 * @return
	 */
	private SPDXLicenseInfo getLicenseInfo() {
		SPDXLicenseInfo result = null;
		try {
			logger.debug("Getting license info to {}", this.licenseType);
			 result = (SPDXLicenseInfo) licenseContainer.getClass().getMethod("get" + this.licenseType, null).invoke(licenseContainer, null);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return result;
	}


}
