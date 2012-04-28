/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.csvie;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.opendatakit.tables.data.Table;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * A class for exporting tables to CSV files.
 * TODO: have it notify the user when the data has been exported
 */
public class CSVExporter {
	
	/**
	 * Exports a table to a CSV file.
	 * @param table the the table to export, beginning with timestamps and
	 * source phone numbers
	 * @param file the file to export to
	 * @param incTS whether to include the timestamps
     * @param incPN whether to include the source phone numbers
	 * @throws IOException
	 */
	public void exportTable(Table table, File file, boolean incTS,
			boolean incPN) throws CSVException {
		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter(file));
		} catch (IOException e) {
			throw new CSVException("Could not open file", e);
		}
        int rowLength = table.getWidth() - (incTS ? 0 : 1) - (incPN ? 0 : 1);
        String[] row = new String[rowLength];
        int index = 0;
        for (int i = 0; i < table.getWidth(); i++) {
            if ((!incTS && i == 0) || (!incPN && i == 1)) {
                continue;
            }
            row[index] = table.getHeader(i);
            index++;
        }
        writer.writeNext(row);
        for (int i = 0; i < table.getHeight(); i++) {
            index = 0;
            for (int j = 0; j < table.getWidth(); j++) {
                if ((!incTS && j == 0) || (!incPN && j == 1)) {
                    continue;
                }
                row[index] = table.getData(i, j);
                index++;
            }
            writer.writeNext(row);
        }
		try {
			writer.close();
		} catch (IOException e) {
			throw new CSVException("Could not close file", e);
		}
	}
}
