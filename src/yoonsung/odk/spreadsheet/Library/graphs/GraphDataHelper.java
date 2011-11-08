package yoonsung.odk.spreadsheet.Library.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.DataTable;

public class GraphDataHelper {

	private DataTable data;
	
	public GraphDataHelper(DataTable data) {
		this.data = data;
	}
	
	public void prepareXYForLineGraph() {
		DescriptiveStatistics ds = new DescriptiveStatistics();
	}
	
	public HashMap<String, ArrayList<Double>> prepareYForStemGraph(Table currentTable, String colOne, String colTwo) {
		HashMap<String, ArrayList<Double>> result = new HashMap<String, ArrayList<Double>>();
		
		// X entries
    	ArrayList<String> x = currentTable.getCol(currentTable.getColNum(colOne));
    	
    	// Arrays for y values for each x entry
		ArrayList<Double> Q0s = new ArrayList<Double>();
		ArrayList<Double> Q1s = new ArrayList<Double>();
		ArrayList<Double> Q2s = new ArrayList<Double>();
		ArrayList<Double> Q3s = new ArrayList<Double>();
		ArrayList<Double> Q4s = new ArrayList<Double>();
    	
		Set<String> seenX = new HashSet<String>();
		
    	// For each x entry
    	for (int i = 0; i < x.size(); i++) {
    		
    	    String xVal = x.get(i);
    	    if (seenX.contains(xVal)) {
    	        continue;
    	    }
    	    seenX.add(xVal);
    	    
    		// Get corresponding y values 
    		Table table = data.getTableByVal(colOne, xVal);
    		ArrayList<String> y = table.getCol(table.getColNum(colTwo));
    		
    		// Take statistics
    		DescriptiveStatistics ds = new DescriptiveStatistics();
    		for (int j = 0; j < y.size(); j++) {
    			try {
    				double val = parseDouble(y.get(j));
    				ds.addValue(val);
    			} catch (NumberFormatException e) {
    				return null;
    			}
			}
			
    		// Add to the array. Each index corresponds to a x entry.	
    		Q0s.add(ds.getMin());
    		Q1s.add(ds.getPercentile(25.0));
    		Q2s.add(ds.getMean());
    		Q3s.add(ds.getPercentile(75));
    		Q4s.add(ds.getMax());
		
    	}

    	result.put("Q0s", Q0s);
    	result.put("Q1s", Q1s);
    	result.put("Q2s", Q2s);
    	result.put("Q3s", Q3s);
    	result.put("Q4s", Q4s);
    	
    	return result;
	}
	
	private double parseDouble(String str) throws NumberFormatException {
		if (str.startsWith("+")) {
			// Fone Astra
			return Double.parseDouble(str.substring(1));
		} else {
			return Double.parseDouble(str);
		}
	}
	
	public double[] arraylistToArray(ArrayList<Double> list) {
		double[] result = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			result[i] = list.get(i);
		}
		return result;
	}
	
	public void prepareForMap() {
		
	}
}
