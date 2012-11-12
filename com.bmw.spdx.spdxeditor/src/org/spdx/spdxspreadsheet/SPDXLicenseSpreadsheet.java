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
package org.spdx.spdxspreadsheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXStandardLicense;
import org.spdx.rdfparser.IStandardLicenseProvider;

/**
 * A spreadhseet containing license information
 * @author Source Auditor
 *
 */
public class SPDXLicenseSpreadsheet extends AbstractSpreadsheet implements IStandardLicenseProvider  {
	
	public class LicenseIterator implements Iterator<SPDXStandardLicense> {

		private int currentRowNum;
		SPDXStandardLicense currentLicense;
		public LicenseIterator() throws SpreadsheetException {
			this.currentRowNum = licenseSheet.getFirstDataRow();	// skip past header row
			try {
                currentLicense = licenseSheet.getLicense(currentRowNum);
            } catch (InvalidSPDXAnalysisException e) {
                throw new SpreadsheetException(e.getMessage());
            }
		}
		@Override
		public boolean hasNext() {
			return currentLicense != null;
		}

		@Override
		public SPDXStandardLicense next() {
			SPDXStandardLicense retval = currentLicense;
			currentRowNum++;
			try {
                currentLicense = licenseSheet.getLicense(currentRowNum);
            } catch (InvalidSPDXAnalysisException e) {
                throw new RuntimeException(e.getMessage());
            }
			return retval;
		}

		@Override
		public void remove() {
			// not implementd
		}
		
	}
	static final String LICENSE_SHEET_NAME = "Licenses";
	
	private LicenseSheet licenseSheet;

	public SPDXLicenseSpreadsheet(File spreadsheetFile, boolean create, boolean readonly)
			throws SpreadsheetException {
		super(spreadsheetFile, create, readonly);
		this.licenseSheet = new LicenseSheet(this.workbook, LICENSE_SHEET_NAME, spreadsheetFile);
		String verifyMsg = verifyWorkbook();
		if (verifyMsg != null) {
			logger.error(verifyMsg);
			throw(new SpreadsheetException(verifyMsg));
		}
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.AbstractSpreadsheet#create(java.io.File)
	 */
	@Override
	public void create(File spreadsheetFile) throws IOException,
			SpreadsheetException {
		create(spreadsheetFile, "Unknown", "Unknown");
	}
	
	public void create(File spreadsheetFile, String version, String releaseDate) throws IOException,
	SpreadsheetException {
if (!spreadsheetFile.createNewFile()) {
	logger.error("Unable to create "+spreadsheetFile.getName());
	throw(new SpreadsheetException("Unable to create "+spreadsheetFile.getName()));
}
FileOutputStream excelOut = null;
try {
	excelOut = new FileOutputStream(spreadsheetFile);
	Workbook wb = new HSSFWorkbook();
	LicenseSheet.create(wb, LICENSE_SHEET_NAME, version, releaseDate);
	wb.write(excelOut);
} finally {
	excelOut.close();
}
}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.AbstractSpreadsheet#clear()
	 */
	@Override
	public void clear() {
		this.licenseSheet.clear();
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.AbstractSpreadsheet#verifyWorkbook()
	 */
	@Override
	public String verifyWorkbook() {
		return this.licenseSheet.verify();
	}

	/**
	 * @return the licenseSheet
	 */
	public LicenseSheet getLicenseSheet() {
		return licenseSheet;
	}

	public Iterator<SPDXStandardLicense> getIterator() {
		try {
            return new LicenseIterator();
        } catch (SpreadsheetException e) {
            throw new RuntimeException(e);
        }
	}
}
