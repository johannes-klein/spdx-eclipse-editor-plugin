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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXFile;
import org.spdx.rdfparser.SPDXLicenseInfo;
import org.spdx.rdfparser.SPDXLicenseInfoFactory;

/**
 * 
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class SPDXTableCellLicenseSelector extends EditingSupport {
	private static Logger logger = LoggerFactory.getLogger(SPDXTableCellLicenseSelector.class);

	private ComboBoxViewerCellEditor cellEditor = null;
	private Map<String, SPDXLicenseInfo> allLicenseInfoInSPDXFile = null;
	private ColumnViewer viewer = null;
	public SPDXTableCellLicenseSelector(ColumnViewer viewer, Map<String, SPDXLicenseInfo> licenseInfoInSPDXFile) {
        super(viewer);
        this.viewer = viewer;
        
        allLicenseInfoInSPDXFile = licenseInfoInSPDXFile;
        
        cellEditor = new ComboBoxViewerCellEditor((Composite) getViewer().getControl(), SWT.READ_ONLY);
        cellEditor.setLabelProvider(new LabelProvider());
        cellEditor.setContenProvider(new ArrayContentProvider());
//        cellEditor.setInput(Value.values());
		List<String> availableLicenses = new ArrayList<String>();
		availableLicenses.addAll(allLicenseInfoInSPDXFile.keySet());
		Collections.sort(availableLicenses);
		this.cellEditor.setInput(availableLicenses);
    }
	
	@Override
	protected CellEditor getCellEditor(Object element) {
		return cellEditor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		SPDXFile spdxReferencedFile = (SPDXFile) element;
		return spdxReferencedFile.getConcludedLicenses();
	}

	@Override
	protected void setValue(Object element, Object value) {
		logger.debug("Set element {} to value {}", element, value);
		SPDXFile spdxReferencedFile = (SPDXFile) element;
		String licenseIdentifier = (String) value;
		SPDXLicenseInfo licenseInfo;
		try {
			licenseInfo = SPDXLicenseInfoFactory.getStandardLicenseById(licenseIdentifier);
		} catch (InvalidSPDXAnalysisException e) {
			licenseInfo = allLicenseInfoInSPDXFile.get("NOASSERTION");
		}
		spdxReferencedFile.setConcludedLicenses(licenseInfo);
		this.viewer.refresh();

	}

}
