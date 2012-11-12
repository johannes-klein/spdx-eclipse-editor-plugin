/**
 * Copyright (c) 2011 Source Auditor Inc.
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

import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Abstract class for sheet containing information about the origins of an SPDX document
 * Specific versions implemented as subclasses
 * @author Gary O'Neall
 *
 */
public abstract class OriginsSheet extends AbstractSheet {
	static final int SPREADSHEET_VERSION_COL = 0;
	static final int DATA_ROW_NUM = 1;
	
	protected String version;

	public OriginsSheet(Workbook workbook, String sheetName, String version) {
		super(workbook, sheetName);
		this.version = version;
	}

	public static void create(Workbook wb, String sheetName) {
		//NOTE: this must be updated to the latest version
		OriginsSheetV1d1.create(wb, sheetName);
	}

	/**
	 * Open an existing worksheet
	 * @param workbook
	 * @param originSheetName
	 * @param version Spreadsheet version
	 * @return
	 */
	public static OriginsSheet openVersion(Workbook workbook,
			String originSheetName, String version) {
		if (version.compareTo(SPDXSpreadsheet.VERSION_0_9_4) <= 0) {
			return new OriginsSheetV0d9d4(workbook, originSheetName, version);
		} else {
			return new OriginsSheetV1d1(workbook, originSheetName, version);
		}
	}
	
	protected Row getDataRow() {
		Row dataRow = sheet.getRow(firstRowNum + DATA_ROW_NUM);
		if (dataRow == null) {
			dataRow = sheet.createRow(firstRowNum + DATA_ROW_NUM);
		}
		return dataRow;
	}
	
	protected Cell getOrCreateDataCell(int colNum) {
		Cell cell = getDataRow().getCell(colNum);
		if (cell == null) {
			cell = getDataRow().createCell(colNum);
		}
		cell.setCellType(Cell.CELL_TYPE_NUMERIC);
//TODO: add date format		cell.setCellStyle(dateStyle);
		return cell;
	}
	
	protected void setDataCellStringValue(int colNum, String value) {
		getOrCreateDataCell(colNum).setCellValue(value);
	}
	
	protected void setDataCellDateValue(int colNum, Date value) {
		Cell cell = getOrCreateDataCell(colNum);
		cell.setCellValue(value);
		cell.setCellStyle(dateStyle);
		
	}
	
	protected Date getDataCellDateValue(int colNum) {
		Cell cell = getDataRow().getCell(colNum);
		if (cell == null) {
			return null;
		} else {
			return cell.getDateCellValue();
		}
	}
	
	protected String getDataCellStringValue(int colNum) {
		Cell cell = getDataRow().getCell(colNum);
		if (cell == null) {
			return null;
		} else {
			return cell.getStringCellValue();
		}
	}

	/**
	 * @param spdxVersion
	 */
	public abstract void setSPDXVersion(String spdxVersion);

	/**
	 * @param createdBys
	 */
	public abstract void setCreatedBy(String[] createdBys);

	/**
	 * @param id
	 */
	public abstract void setDataLicense(String id);

	/**
	 * @param comments
	 */
	public abstract void setAuthorComments(String comments);

	/**
	 * @param parse
	 */
	public abstract void setCreated(Date parse);

	/**
	 * @return
	 */
	public abstract Date getCreated();

	/**
	 * @return
	 */
	public abstract String[] getCreatedBy();

	/**
	 * @return
	 */
	public abstract String getAuthorComments();

	/**
	 * @return
	 */
	public abstract String getSPDXVersion();

	/**
	 * @return
	 */
	public abstract String getDataLicense();

	/**
	 * @return
	 */
	public abstract String getDocumentDomment();

	/**
	 * @param docComment
	 */
	public abstract void setDocumentComment(String docComment);
	
}
