package yoonsung.odk.spreadsheet.SMS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.TableList;
import yoonsung.odk.spreadsheet.Database.TableList.TableInfo;

/**
 * A class for handling incoming messages.
 */
public class MsgHandler {
    
    private List<String> dtIDs;
    private List<String> dtNames;
    private List<String> msNames;
    private List<String> msInputs;
    private List<String> msOutputs;
    
    public MsgHandler() {
        dtIDs = new ArrayList<String>();
        dtNames = new ArrayList<String>();
        msNames = new ArrayList<String>();
        msInputs = new ArrayList<String>();
        msOutputs = new ArrayList<String>();
        List<TableInfo> tiList = (new TableList()).getTableList();
        for(TableInfo ti : tiList) {
            if(ti.getTableType() == TableList.TABLETYPE_DATA) {
                dtIDs.add(ti.getTableID());
                dtNames.add(ti.getTableName());
            } else if(ti.getTableType() == TableList.TABLETYPE_SHORTCUT) {
                DataTable dt = new DataTable(ti.getTableID());
                Table table = dt.getTable();
                int numRows = table.getHeight();
                List<String> data = table.getData();
                for(int i=0; i<(numRows * 3); i+=3) {
                    msNames.add(data.get(i));
                    msInputs.add(data.get(i+1));
                    msOutputs.add(data.get(i+2));
                }
            }
        }
    }
    
    /**
     * Attempts to convert a message to the standard format.
     * @param msg the message to handle
     * @return the request in the standard format
     * @throws InvalidQueryException if the target does not exist
     */
    public String translateMessage(String msg) throws InvalidQueryException {
        int iters = 0;
        String temp = msg.trim();
        String nameToken;
        String request;
        do {
            iters++;
            msg = temp;
            temp = null;
            String[] spl = msg.split(" ", 2);
            nameToken = spl[0].substring(1);
            request = spl[1];
            int i = 0;
            while((i<msNames.size()) && (temp == null)) {
                if(msNames.get(i).equals(nameToken)) {
                    temp = tryShortcut(i, request);
                }
                i++;
            }
        } while((temp != null) && (iters < msNames.size()));
        int dtIndex = dtNames.indexOf(nameToken);
        if(dtIndex < 0) {
            throw new InvalidQueryException(
                    InvalidQueryException.NONEXISTENT_TARGET, nameToken, null,
                    null);
        }
        return msg;
    }
    
    /**
     * Tries to parse a message using a shortcut format.
     * @param msIndex the message shortcut index
     * @param msg the message
     * @return the output, or null if the message could not be parsed
     */
    private String tryShortcut(int msIndex, String msg) {
        Map<String, String> inputVals = parseMSInputVals(msg,
                msInputs.get(msIndex));
        if(inputVals == null) {
            return null;
        }
        String res = fillOutputFormat(msOutputs.get(msIndex), inputVals);
        if(res == null) {
            return null;
        }
        return res;
    }
    
    /**
     * Parses a message for arguments.
     * @param msg the message
     * @param input the input format
     * @return a map from keys to values, or null if the message does not match
     * the format
     */
    private Map<String, String> parseMSInputVals(String msg, String input) {
        Map<String, String> valMap = new HashMap<String, String>();
        String[] inputSplit = input.split("%");
        int inputIndex = 1;
        int msgIndex = 0;
        if (!msg.startsWith(inputSplit[0], msgIndex)) {
            return null;
        } else if(!input.startsWith("%")) {
            msgIndex = inputSplit[0].length();
        }
        int splLength = inputSplit.length;
        for(int i=inputIndex; i<splLength; i+=2) {
            int endIndex;
            int newMsgIndex;
            if(i == splLength - 1) {
                endIndex = msg.length();
                newMsgIndex = endIndex;
            } else {
                endIndex = msg.indexOf(inputSplit[i+1], msgIndex);
                if(endIndex < 0) {
                    return null;
                }
                newMsgIndex = endIndex + (inputSplit[i+1]).length();
            }
            valMap.put(inputSplit[i], msg.substring(msgIndex, endIndex));
            msgIndex = newMsgIndex;
        }
        if(msgIndex != msg.length()) {
            return null;
        }
        return valMap;
    }
    
    /**
     * Fills an output format with values.
     * @param output the output format
     * @param inputVals a map from input keys to values
     * @return the result string, or null if the format did not match
     */
    private String fillOutputFormat(String output,
            Map<String, String> inputVals) {
        String[] split = output.split("%");
        int index;
        if(output.startsWith("%")) {
            index = 0;
        } else {
            index = 1;
        }
        for(int i=index; i<split.length; i+=2) {
            String val = inputVals.get(split[i]);
            if(val == null) {
                return null;
            }
            split[i] = val;
        }
        String res = split[0];
        for(int i=1; i<split.length; i++) {
            res += split[i];
        }
        return res;
    }
    
}
