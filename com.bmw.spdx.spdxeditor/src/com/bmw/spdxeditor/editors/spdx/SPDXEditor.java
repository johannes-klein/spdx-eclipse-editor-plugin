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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocument;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.SPDXFile;
import org.spdx.rdfparser.SPDXLicenseInfo;
import org.spdx.rdfparser.SPDXLicenseSet;
import org.spdx.rdfparser.SPDXNonStandardLicense;
import org.spdx.rdfparser.SPDXNoneLicense;
import org.spdx.rdfparser.SPDXStandardLicense;
import org.spdx.rdfparser.SpdxNoAssertionLicense;


/**
 * The main SPDX editor UI form.
 * @author Johannes Klein (johannes.klein@bmw.de)
 *
 */
public class SPDXEditor extends EditorPart {
	public static final String ID = "BMWSpdxEditor2.spdxEditor";
	private static final String VALUE_NOT_SET_IN_SPDX_FILE_TEXT = "Value missing in file";
	private boolean isDirty;
	// Hold all licenses referenced from SPDX document
	private Map<String, SPDXLicenseInfo> allLicensingInfoInSPDXFile;
	private Combo concludedLicenseChoice, declaredLicenseChoice;
	private Color SPDX_FIELD_INVALID_COLOR, SPDX_FIELD_VALID_COLOR;
	
	private static Logger logger = LoggerFactory.getLogger(SPDXEditor.class);


	public SPDXEditor() {
		allLicensingInfoInSPDXFile = new HashMap<String, SPDXLicenseInfo>();
		Device device = Display.getCurrent ();
		SPDX_FIELD_INVALID_COLOR = new Color(device, 255,159,128);
		SPDX_FIELD_VALID_COLOR = new Color(device, 255,255,255);

	}
	
	private SPDXEditorInput getInput() {
		return (SPDXEditorInput) this.getEditorInput();
	}

	@Override
	/**
	 * Called when SPDX file is saved from GUI.
	 */
	public void doSave(IProgressMonitor monitor) {
		try {
			// Do validation and report errors
			ArrayList<String> validationErrors = getInput()
					.getAssociatedSPDXFile().verify();


			if (validationErrors.size() > 0) {
				// Prepare string for MessageDialog
				StringBuilder validationErrorMessage = new StringBuilder();
				
				for (String s : validationErrors) {
					validationErrorMessage.append(s).append("\n");
				}

				MessageDialog.openError(this.getSite().getShell(),
						"Validation Report", validationErrorMessage.toString());
			} else {
				// No errors reported
				SPDXEditorUtility.saveModelToFile(getInput()
						.getAssociatedSPDXFile(), this.getInput()
						.getAssociatedFile());

				// Clear dirty flag when saved
				isDirty = false;
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		} catch (FileNotFoundException e) {
			MessageDialog.openError(this.getSite().getShell(), "Error",
					e.getMessage());
		}
	}

	// TODO: Implement later
	@Override
	public void doSaveAs() {
		MessageDialog.openError(getSite().getShell(), "Not implemented", "Save As not implemented");
	}

	/**
	 * Return path from current editor input
	 * 
	 * @param input
	 * @return
	 */
	private IPath getPathFromEditorInput(IEditorInput input) {
		IPath path = null;
		if (input instanceof FileStoreEditorInput) {
			FileStoreEditorInput fileStoreEditorInput = (FileStoreEditorInput) input;
			path = new Path(fileStoreEditorInput.getURI().getPath());
		} else if (input instanceof FileEditorInput) {
			FileEditorInput newInput = (FileEditorInput) input;
			path = newInput.getFile().getLocation();
		}
		return path;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

		IPath path = this.getPathFromEditorInput(input);
		if (path == null) {
			throw new PartInitException("Invalid editor input");
		}

		File inputFile = path.toFile();
		SPDXDocument spdxDoc;
		SPDXEditorInput editorInput = null;
		try {
			// Create the SPDX RDF model
			spdxDoc = SPDXDocumentFactory.creatSpdxDocument(inputFile
					.getAbsolutePath());
			editorInput = new SPDXEditorInput("", spdxDoc, inputFile);

			setSite(site);
			setInput(editorInput);

			// Load licenses referenced in SPDX file
			loadLicenseDataFromSPDXFile();

		} catch (IOException | InvalidSPDXAnalysisException e) {
			throw new PartInitException(e.getMessage());
		}

	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		parent.setLayout(layout);

		GridData parentGridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		parentGridData.verticalAlignment = GridData.BEGINNING;
		parentGridData.grabExcessVerticalSpace = true;
		
		parent.setLayoutData(parentGridData);
		
		try {
			createSPDXDetailsPanel(parent);
			createLinkedFilesDetailsPanel(parent);
			createConcludedAndDeclaredLicensePanel(parent);
			createLicenseTextEditor(parent);
			this.isDirty = false;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		} catch (InvalidSPDXAnalysisException e) {
			MessageDialog.openError(getSite().getShell(), "Invalid SPDX Analysis", e.getMessage());
		}
	}

	private void loadLicenseDataFromSPDXFile()
			throws InvalidSPDXAnalysisException {
		// Prepare all available licenses and IDs
		allLicensingInfoInSPDXFile.clear();
		List<String> standardLicenseIDList = Arrays
				.asList(SPDXEditorUtility.STANDARD_LICENSE_IDS);
		for (String standardLicenseID : standardLicenseIDList) {
			allLicensingInfoInSPDXFile.put(standardLicenseID, null);
		}
		// Get all custom licenses from SPDX file and store in allLicensingInfo
		for (SPDXNonStandardLicense nonStandardLicense : getInput()
				.getAssociatedSPDXFile().getExtractedLicenseInfos()) {
			allLicensingInfoInSPDXFile.put(nonStandardLicense.getId(),
					nonStandardLicense);
		}
		SPDXLicenseInfo noAssertionLicense = new SpdxNoAssertionLicense();
		allLicensingInfoInSPDXFile.put("NOASSERTION", noAssertionLicense);
		allLicensingInfoInSPDXFile.put("NONE", new SPDXNoneLicense());
		
	}

	/**
	 * Create the license text editor component
	 * 
	 * @param parent
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	private Group createLicenseTextEditor(Composite parent)
			throws InvalidSPDXAnalysisException {
		Group licenseTextEditorGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
		licenseTextEditorGroup.setText("Licenses referenced in SPDX file");
		GridLayout licenseTextEditorGroupLayout = new GridLayout();
		licenseTextEditorGroupLayout.numColumns = 2;
		licenseTextEditorGroup.setLayout(licenseTextEditorGroupLayout);

		// Add table viewer
		final TableViewer tableViewer = new TableViewer(licenseTextEditorGroup,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION
						| SWT.BORDER);

		// The list of available extracted license texts
		final Table table = tableViewer.getTable();
		table.setToolTipText("Right click to add/remove licenses.");

		// Build a drop down menu to add/remove licenses
		Menu licenseTableMenu = new Menu(parent.getShell(), SWT.POP_UP);
		MenuItem addNewLicenseText = new MenuItem(licenseTableMenu, SWT.PUSH);
		addNewLicenseText.setText("Add license text");

		addNewLicenseText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Retrieve the corresponding Services
				IHandlerService handlerService = (IHandlerService) getSite()
						.getService(IHandlerService.class);
				ICommandService commandService = (ICommandService) getSite()
						.getService(ICommandService.class);

				// Retrieve the command
				Command generateCmd = commandService
						.getCommand("SPDXEditor.addNewLicenseText");

				// Create an ExecutionEvent
				ExecutionEvent executionEvent = handlerService
						.createExecutionEvent(generateCmd, new Event());

				// Launch the command
				try {
					generateCmd.executeWithChecks(executionEvent);
				} catch (ExecutionException | NotDefinedException
						| NotEnabledException | NotHandledException e1) {
					MessageDialog.openError(getSite().getShell(), "Execution failed", e1.getMessage());
				}
			}
		});

		// Listen for changes on model
		this.getInput().addChangeListener(new IResourceChangeListener() {
			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					SPDXNonStandardLicense[] nonStandardLicenses = getInput()
							.getAssociatedSPDXFile().getExtractedLicenseInfos();
					tableViewer.setInput(nonStandardLicenses);
					tableViewer.refresh();
					loadLicenseDataFromSPDXFile();
					
					setLicenseComboBoxValue(concludedLicenseChoice, getInput().getAssociatedSPDXFile().getSpdxPackage().getConcludedLicenses());					

//					populateLicenseSelectorWithAvailableLicenses(declaredLicenseChoice);
					setLicenseComboBoxValue(declaredLicenseChoice, getInput().getAssociatedSPDXFile().getSpdxPackage().getDeclaredLicense());					
					setDirtyFlag(true);
				} catch (InvalidSPDXAnalysisException e) {
					e.printStackTrace();
				}
			}
		});

		final MenuItem deleteSelectedLicenseTexts = new MenuItem(
				licenseTableMenu, SWT.PUSH);
		deleteSelectedLicenseTexts.setText("Delete licenses");
		deleteSelectedLicenseTexts.setEnabled(false);

		tableViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						// Never enable, not yet implemented
						// deleteSelectedLicenseTexts.setEnabled(table.getSelectionCount()
						// != 0);
					}
				});

		table.setMenu(licenseTableMenu);

		// Make headers and borders visible
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());

		// Create TableViewerColumn for each column
		TableViewerColumn viewerNameColumn = new TableViewerColumn(tableViewer,
				SWT.NONE);
		viewerNameColumn.getColumn().setText("License ID");
		viewerNameColumn.getColumn().setWidth(100);

		// Set LabelProvider for each column
		viewerNameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				SPDXNonStandardLicense licenseInCell = (SPDXNonStandardLicense) cell
						.getElement();
				cell.setText(licenseInCell.getId());
			}
		});

		viewerNameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		viewerNameColumn.getColumn().setText("License text");
		viewerNameColumn.getColumn().setWidth(100);

		viewerNameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				SPDXNonStandardLicense iteratorLicense = (SPDXNonStandardLicense) cell
						.getElement();
				cell.setText(iteratorLicense.getText());
			}
		});

		/*
		 * All ExtractedLicensingInfo is contained in the SPDX file assigned to
		 * the input.
		 */
		tableViewer.setInput(getInput().getAssociatedSPDXFile()
				.getExtractedLicenseInfos());

		GridData spdxDetailsPanelGridData = new GridData(SWT.FILL,
				SWT.BEGINNING, true, false);
		spdxDetailsPanelGridData.horizontalSpan = 1;
		spdxDetailsPanelGridData.heightHint = 150;
		licenseTextEditorGroup.setLayoutData(spdxDetailsPanelGridData);

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.heightHint=100;
		gridData.minimumWidth = 70;
		tableViewer.getControl().setLayoutData(gridData);

		/*
		 * Text editor field for editing license texts.
		 */
		final Text licenseEditorText = new Text(licenseTextEditorGroup,
				SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		licenseEditorText.setText("");
		licenseEditorText.setEditable(true);
		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.minimumHeight = 70;
		gd.minimumWidth = 200;
		gd.heightHint = 100;
		gd.horizontalSpan = 1;
		licenseEditorText.setLayoutData(gd);
		licenseEditorText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					SPDXNonStandardLicense[] nonStandardLicenses;
					nonStandardLicenses = getInput().getAssociatedSPDXFile().getExtractedLicenseInfos();
					nonStandardLicenses[table.getSelectionIndex()].setText(licenseEditorText.getText());
					setDirtyFlag(true);
				} catch (InvalidSPDXAnalysisException e1) {
					MessageDialog.openError(getSite().getShell(), "SPDX Analysis error", e1.getMessage());
				}
			}
		});

		/*
		 * Listener for updating text editor selection based on selected table
		 * entry.
		 */
		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SPDXNonStandardLicense[] nonStandardLicenses;
				try {
					nonStandardLicenses = getInput().getAssociatedSPDXFile().getExtractedLicenseInfos();
					licenseEditorText.setText(nonStandardLicenses[table.getSelectionIndex()].getText());
				} catch (InvalidSPDXAnalysisException e1) {
					MessageDialog.openError(getSite().getShell(), "SPDX Analysis invalid", e1.getMessage());
				}
			}
		});

		// Style group panel
		GridData licenseTextEditorGroupGridData = new GridData(SWT.FILL,
				SWT.BEGINNING, true, false);
		licenseTextEditorGroupGridData.horizontalSpan = 2;
		licenseTextEditorGroup.setLayoutData(licenseTextEditorGroupGridData);

		return licenseTextEditorGroup;
	}

	/**
	 * Populate given combo with all licenses available
	 * (ExtractedLicenseReference and standard SPDX licenses)
	 * 
	 * @param selectorCombo
	 */
	private void populateLicenseSelectorWithAvailableLicenses(
			Combo selectorCombo) {
		List<String> availableLicenses = new ArrayList<String>(
				allLicensingInfoInSPDXFile.keySet());
		Collections.sort(availableLicenses);
		String[] availableLicenseChoicesAsArray = new String[availableLicenses
				.size()];
		selectorCombo.setItems(availableLicenses
				.toArray(availableLicenseChoicesAsArray));
		
		
	}

	/**
	 * Create panel for setting the package licenses
	 * 
	 * @param parent
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 * @throws InvalidLicenseStringException
	 */
	private Group createConcludedAndDeclaredLicensePanel(Composite parent)
			throws InvalidSPDXAnalysisException {
		Group packageLicenseDetails = new Group(parent, SWT.SHADOW_ETCHED_IN);
		packageLicenseDetails.setText("Package license details");

		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.verticalAlignment = SWT.TOP;
		packageLicenseDetails.setLayoutData(gd);

		
		// Describe layout for this group panel
		GridLayout packageLicenseDetailsLayout = new GridLayout();
		packageLicenseDetailsLayout.numColumns = 2;
		packageLicenseDetails.setLayout(packageLicenseDetailsLayout);

		// Set concluded and declared license info
		Label licenseConcludedLabel = new Label(packageLicenseDetails, SWT.NONE);
		licenseConcludedLabel.setText("License concluded");
		concludedLicenseChoice = new Combo(packageLicenseDetails, SWT.BORDER
				| SWT.DROP_DOWN | SWT.READ_ONLY);
		this.populateLicenseSelectorWithAvailableLicenses(concludedLicenseChoice);
		setLicenseComboBoxValue(concludedLicenseChoice, getInput().getAssociatedSPDXFile().getSpdxPackage().getConcludedLicenses());

		concludedLicenseChoice.setData(allLicensingInfoInSPDXFile);

				concludedLicenseChoice
				.addModifyListener(new SelectedLicenseInfoModifyListener(
						getInput().getAssociatedSPDXFile().getSpdxPackage(),
						"ConcludedLicenses", getInput().getAssociatedSPDXFile()
								.getModel()));
		concludedLicenseChoice.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setDirtyFlag(true);
			}
		});
		
		
		gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.widthHint = 150;
		concludedLicenseChoice.setLayoutData(gd);

		Label licenseDeclaredLabel = new Label(packageLicenseDetails, SWT.NONE);
		licenseDeclaredLabel.setText("License declared");
		declaredLicenseChoice = new Combo(packageLicenseDetails, SWT.BORDER
				| SWT.DROP_DOWN | SWT.READ_ONLY);
		this.populateLicenseSelectorWithAvailableLicenses(declaredLicenseChoice);
		declaredLicenseChoice.setData(allLicensingInfoInSPDXFile);
		
		setLicenseComboBoxValue(declaredLicenseChoice, getInput().getAssociatedSPDXFile().getSpdxPackage().getDeclaredLicense());
		
		// Add listener to update UI when selection changes
		declaredLicenseChoice.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setDirtyFlag(true);
			}
		});
		gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.widthHint = 150;
		declaredLicenseChoice.setLayoutData(gd);
		
		// Set Layout data for ConcludedLicense text field
		gd = new GridData(SWT.FILL,
				SWT.BEGINNING, true, false);
		concludedLicenseChoice.setLayoutData(gd);
		return packageLicenseDetails;
	}

	/**
	 * Set the display value of the given Combo box to the given licensing info.
	 * @param combo
	 * @param licensingInfo
	 * @throws InvalidSPDXAnalysisException
	 */
	private void setLicenseComboBoxValue(Combo combo, SPDXLicenseInfo licensingInfo) throws InvalidSPDXAnalysisException {
		combo.setEnabled(true);
		combo.setToolTipText(null);
		combo.remove(0);
		if(licensingInfo instanceof SPDXStandardLicense) {
			combo.setText(getLicenseIdentifierTextForLicenseInfo(licensingInfo));
		} else if (licensingInfo instanceof SPDXNonStandardLicense) {
			combo.setText(getLicenseIdentifierTextForLicenseInfo(licensingInfo));
		} else if (licensingInfo instanceof SpdxNoAssertionLicense) {
			combo.setText(getLicenseIdentifierTextForLicenseInfo(licensingInfo));
		} else if (licensingInfo instanceof SPDXNoneLicense) {
			combo.setText(getLicenseIdentifierTextForLicenseInfo(licensingInfo));
		} else if (licensingInfo instanceof SPDXLicenseSet) {
			combo.add("License set not supported", 0);
			combo.select(0);
			combo.setText("License set not supported yet");
			combo.setEnabled(false);
			combo.setToolTipText("License set not supported yet");
			
		} else {
			combo.setText("?UNKNOWN?");
		}

}
	
	/*
	 * Find the license identifier for the given licenseInfo
	 */
	private String getLicenseIdentifierTextForLicenseInfo(
			SPDXLicenseInfo licenseInfo) {
		String licenseIdentifier = null;
		// Identify type of license used and set data

		if (licenseInfo instanceof SPDXStandardLicense) {
			SPDXStandardLicense standardLicense = (SPDXStandardLicense) licenseInfo;
			licenseIdentifier = standardLicense.getId();
		} else if (licenseInfo instanceof SPDXNonStandardLicense) {
			SPDXNonStandardLicense nonStandardLicense = (SPDXNonStandardLicense) licenseInfo;
			licenseIdentifier = nonStandardLicense.getId();
		} else if (licenseInfo instanceof SPDXLicenseSet) {
			licenseIdentifier = "LICENSE SET NOT SUPPORTED";
		} else if (licenseInfo instanceof SpdxNoAssertionLicense) {
			licenseIdentifier = "NOASSERTION";
		} else if (licenseInfo instanceof SPDXNoneLicense) {
			licenseIdentifier = "NONE";
		} else {
			licenseIdentifier = "?UNKNOWN?";
		}
		return licenseIdentifier;
	}

	/**
	 * Create the linked files group panel
	 * 
	 * @param parent
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	private Composite createLinkedFilesDetailsPanel(Composite parent)
			throws InvalidSPDXAnalysisException {
		Group linkedFilesDetailsGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
		linkedFilesDetailsGroup.setText("Referenced files");
		GridLayout linkedFilesDetailsGroupLayout = new GridLayout();
		linkedFilesDetailsGroup.setLayout(linkedFilesDetailsGroupLayout);

		// Add table viewer
		final TableViewer tableViewer = new TableViewer(
				linkedFilesDetailsGroup, SWT.MULTI | SWT.H_SCROLL
						| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.heightHint = 80;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		tableViewer.getControl().setLayoutData(gridData);

		// SWT Table
		final Table table = tableViewer.getTable();
		
		table.setToolTipText("Right-click to add or remove files");

		// Make header and columns visible
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());

		// Add context menu to TableViewer
		Menu linkesFileTableMenu = new Menu(parent.getShell(), SWT.POP_UP);
		MenuItem addNewLicenseText = new MenuItem(linkesFileTableMenu, SWT.PUSH);
		addNewLicenseText.setText("Add source code archive");
		addNewLicenseText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Retrieve the corresponding Services
				IHandlerService handlerService = (IHandlerService) getSite()
						.getService(IHandlerService.class);
				ICommandService commandService = (ICommandService) getSite()
						.getService(ICommandService.class);

				// Retrieve the command
				Command generateCmd = commandService
						.getCommand("SPDXEditor.addLinkedSourceCodeFile");

				// Create an ExecutionEvent
				ExecutionEvent executionEvent = handlerService
						.createExecutionEvent(generateCmd, new Event());

				// Launch the command
				try {
					generateCmd.executeWithChecks(executionEvent);
				} catch (ExecutionException | NotDefinedException
						| NotEnabledException | NotHandledException e1) {
					MessageDialog.openError(getSite().getShell(), "Error", e1.getMessage());
				}

			}
		});

		final MenuItem deleteSelectedLicenseTexts = new MenuItem(
				linkesFileTableMenu, SWT.PUSH);
		deleteSelectedLicenseTexts.setText("Delete selected source code archive");
		deleteSelectedLicenseTexts.setEnabled(false);
		deleteSelectedLicenseTexts.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Retrieve the corresponding Services
				IHandlerService handlerService = (IHandlerService) getSite()
						.getService(IHandlerService.class);
				ICommandService commandService = (ICommandService) getSite()
						.getService(ICommandService.class);
				// Retrieve the command
				Command generateCmd = commandService
						.getCommand("SPDXEditor.removedLinkedSourceCodeFile");
				// Create an ExecutionEvent
				ExecutionEvent executionEvent = handlerService
						.createExecutionEvent(generateCmd, new Event());
				// Launch the command
				try {
					generateCmd.executeWithChecks(executionEvent);
				} catch (ExecutionException | NotDefinedException
						| NotEnabledException | NotHandledException e1) {
					MessageDialog.openError(getSite().getShell(), "Error", e1.getMessage());
				}
			}
		});

		table.setMenu(linkesFileTableMenu);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						deleteSelectedLicenseTexts.setEnabled(table.getSelectionCount() != 0);
					}
				});

		// Create TableViewer for each column
		TableViewerColumn viewerNameColumn = new TableViewerColumn(tableViewer,
				SWT.NONE);
		viewerNameColumn.getColumn().setText("File name");
		viewerNameColumn.getColumn().setWidth(160);

		viewerNameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				SPDXFile currentFile = (SPDXFile) cell.getElement();
				cell.setText(currentFile.getName());
			}
		});

		viewerNameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		viewerNameColumn.getColumn().setText("Type");
		viewerNameColumn.getColumn().setWidth(120);

		viewerNameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				SPDXFile currentFile = (SPDXFile) cell.getElement();
				cell.setText(currentFile.getType());
			}
		});
		// License choice for file
		viewerNameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		viewerNameColumn.getColumn().setText("Concluded License");
		viewerNameColumn.getColumn().setWidth(120);

		// // LabelProvider für jede Spalte setzen
		viewerNameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				SPDXFile currentFile = (SPDXFile) cell.getElement();
				SPDXLicenseInfo spdxConcludedLicenseInfo = currentFile
						.getConcludedLicenses();
				cell.setText(getLicenseIdentifierTextForLicenseInfo(spdxConcludedLicenseInfo));

			}
		});

		viewerNameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		viewerNameColumn.getColumn().setText("SHA1 hash");
		viewerNameColumn.getColumn().setWidth(120);

		// LabelProvider für jede Spalte setzen
		viewerNameColumn.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				SPDXFile currentFile = (SPDXFile) cell.getElement();
				cell.setText(currentFile.getSha1());
			}
		});

		SPDXFile[] referencedFiles = getInput().getAssociatedSPDXFile()
				.getSpdxPackage().getFiles();
		tableViewer.setInput(referencedFiles);
		getSite().setSelectionProvider(tableViewer);

		this.getInput().addChangeListener(new IResourceChangeListener() {
			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				SPDXFile[] referencedFiles;
				try {
					referencedFiles = getInput().getAssociatedSPDXFile().getSpdxPackage().getFiles();
					tableViewer.setInput(referencedFiles);
					tableViewer.refresh();
					setDirtyFlag(true);
				} catch (InvalidSPDXAnalysisException e) {
					MessageDialog.openError(getSite().getShell(), "SPDX Analysis invalid", e.getMessage());
				}
			}
		});

		GridData spdxDetailsPanelGridData = new GridData(SWT.FILL,
				SWT.BEGINNING, true, true);
		spdxDetailsPanelGridData.horizontalSpan = 1;

		spdxDetailsPanelGridData.grabExcessVerticalSpace = true;
		spdxDetailsPanelGridData.grabExcessHorizontalSpace = true;
		spdxDetailsPanelGridData.minimumHeight = 90;
		linkedFilesDetailsGroup.setLayoutData(spdxDetailsPanelGridData);

		return linkedFilesDetailsGroup;
	}

	/**
	 * Set up the panel showing details for the SPDX package declared in the SPDX file.
	 * @param parent
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	private Group createSPDXDetailsPanel(Composite parent) throws InvalidSPDXAnalysisException {
		Group spdxFileDetails = new Group(parent, SWT.SHADOW_ETCHED_IN);
		spdxFileDetails.setText("Package details");
		GridLayout spdxFileDetailsLayout = new GridLayout();
		spdxFileDetailsLayout.numColumns = 4;

		spdxFileDetails.setLayout(spdxFileDetailsLayout);

		addLabelAndTextInput(spdxFileDetails, this.getInput()
				.getAssociatedSPDXFile().getSpdxPackage(), "Declared Name",
				"DeclaredName",SWT.BORDER,"Given name of software as stated in the documentation or on website");
		addLabelAndTextInput(spdxFileDetails, this.getInput()
				.getAssociatedSPDXFile().getSpdxPackage(), "Description",
				"Description", SWT.BORDER,"General description of software");
		addLabelAndTextInput(spdxFileDetails, this.getInput()
				.getAssociatedSPDXFile().getSpdxPackage(), "Download URL",
				"DownloadUrl", SWT.BORDER, "URL software was originally retrieved from");
		addLabelAndTextInput(spdxFileDetails, this.getInput()
				.getAssociatedSPDXFile().getSpdxPackage(), "Supplier",
				"Supplier", SWT.BORDER, "Information on original supplier of software");
		addLabelAndTextInput(spdxFileDetails, this.getInput()
				.getAssociatedSPDXFile().getSpdxPackage(), "Originator",
				"Originator", SWT.BORDER, "Information on originator of software.");
		addLabelAndTextInput(spdxFileDetails, this.getInput()
				.getAssociatedSPDXFile().getSpdxPackage(), "Version",
				"VersionInfo", SWT.BORDER, "Version information of original software download package");
		addLabelAndTextInput(spdxFileDetails, this.getInput()
				.getAssociatedSPDXFile().getSpdxPackage(), "File Name",
				"FileName", SWT.BORDER, "File name of original download archive.");
		addLabelAndTextInput(spdxFileDetails, this.getInput()
				.getAssociatedSPDXFile().getSpdxPackage(), "Source Info",
				"SourceInfo", SWT.BORDER, "Source info");

		// Add copyright information text
		Text text = (Text) addLabelAndTextInput(spdxFileDetails, this.getInput()
				.getAssociatedSPDXFile().getSpdxPackage(), 
				"Copyright",
				"DeclaredCopyright", SWT.MULTI | SWT.BORDER | SWT.V_SCROLL, "Information on copyright on package level.");
		GridData gd = (GridData) text.getLayoutData();
		gd.minimumHeight = 70;
		gd.heightHint = 70;
		gd.minimumWidth = 200;
		gd.widthHint = 200;

		text.setLayoutData(gd);

		// Add notice for license comments
		Text licenseComments = (Text) addLabelAndTextInput(spdxFileDetails, getInput().getAssociatedSPDXFile().getSpdxPackage(),
				"License Comment", "LicenseComment", SWT.MULTI | SWT.BORDER
						| SWT.V_SCROLL, "Information on how package licensing information was derived by the author of this file.");
		gd = (GridData) text.getLayoutData();
		gd.minimumHeight = 70;
		gd.heightHint = 70;
		gd.minimumWidth = 200;
		gd.widthHint = 200;

		licenseComments.setLayoutData(gd);

		GridData spdxDetailsPanelGridData = new GridData(SWT.FILL,
				SWT.BEGINNING, true, false);
		spdxDetailsPanelGridData.horizontalSpan = 2;
		spdxFileDetails.setLayoutData(spdxDetailsPanelGridData);

		return spdxFileDetails;
	}


	/**
	 * Add labels and input text fields to panels
	 * 
	 * @param panel
	 * @param spdxEntity
	 * @param labelText
	 * @param valueName
	 * @param textFieldStyle
	 * @return
	 */
	private Text addLabelAndTextInput(Composite parent, final Object spdxEntity,
			String labelText, final String valueName, int style, String tooltip) {
		Label controlWidgetLabel = new Label(parent, SWT.NONE);
		controlWidgetLabel.setToolTipText(tooltip);
		
		Text text = new Text(parent, style);
		text.setToolTipText(tooltip);
		try {
			// Get text from corresponding SPDX fields
			
			Object result = spdxEntity.getClass().getMethod("get" + valueName, null).invoke(spdxEntity, null);
			String resultString = (String) result;
			
			controlWidgetLabel.setText(labelText);

			//text = new Text(panel, textFieldStyle);
			if (resultString != null) {
				text.setText(resultString);
				text.setBackground(SPDX_FIELD_VALID_COLOR);
			} else {
				text.setText(VALUE_NOT_SET_IN_SPDX_FILE_TEXT);
				text.setBackground(SPDX_FIELD_INVALID_COLOR);
			}
			text.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
					false));
			this.addSPDXInputChangeListener(text, spdxEntity, valueName);
		} catch (NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException e1) {
			MessageDialog.openError(getSite().getShell(),
					e1.getClass().getSimpleName(), e1.getCause().getMessage());
		} catch(InvocationTargetException e2) {
			MessageDialog.openError(getSite().getShell(),
					"Invalid argument", e2.getCause().getMessage());
		}
		return text;

	}
	
	public void addSPDXInputChangeListener(Control widget, final Object spdxEntity, final String valueName) {
		// Update value of SPDX field when focus changes
		widget.addFocusListener(new FocusAdapter() {
			String modelValue;
			Text textSource;
			String widgetValue;
			@Override
			public void focusLost(FocusEvent e) {
				try {
					Object result = spdxEntity.getClass().getMethod("get" + valueName, null).invoke(spdxEntity, null);
					modelValue = (String) result;
					textSource = ((Text) e.getSource());
					widgetValue = textSource.getText();

					if(modelValue == null && widgetValue == null) {
						// Both null, do not update
						return;
					}
					
					if (modelValue != null && widgetValue != null && modelValue.equals(widgetValue)) {
						// Do not update if value has not changed
						return;
					}		
					
					if(modelValue == null && widgetValue.equals(SPDXEditor.VALUE_NOT_SET_IN_SPDX_FILE_TEXT)) {
						// Do not update as placeholder text for empty model values has not been replaced
						return;
					}

					Method setterMethod = spdxEntity.getClass().getMethod(
							"set" + valueName, String.class);
					setterMethod.invoke(spdxEntity, widgetValue);
					setDirtyFlag(true);
					textSource.setBackground(SPDX_FIELD_VALID_COLOR);
				} catch (NoSuchMethodException | SecurityException
						| IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e1) {
					MessageDialog.openError(getSite().getShell(),
							valueName, e1.getCause().getMessage());
					// Undo input, but avoid null
					if(modelValue == null && widgetValue != null && (!widgetValue.equals(SPDXEditor.VALUE_NOT_SET_IN_SPDX_FILE_TEXT))) {
						// The input given by user is still invalid but has been modified.
						// Mark input as invalid but preserve input
						textSource.setBackground(SPDX_FIELD_INVALID_COLOR);
					} else {
						textSource.setText(modelValue);
					}
					
				}
				
			}
			
		});
	}

	@Override
	public String getPartName() {
		return getInput().getName();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	/**
	 * Set flag indicating that this editor is in "dirty" state and must be saved
	 * to be consistend with version on disk.
	 * @param isDirty
	 */
	private void setDirtyFlag(boolean isDirty) {
		this.isDirty = isDirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
}
