package yoonsung.odk.spreadsheet.DataStructure;

public class MessageShortcut {
    
    private String name;
    private String input;
    private String output;
    
    public MessageShortcut(String name, String input, String output) {
        this.name = name;
        this.input = input;
        this.output = output;
    }
    
    public String getName() { return name; }
    
    public String getInput() { return input; }
    
    public String getOutput() { return output; }
    
    public void setName(String newName) {
        this.name = newName;
    }
    
    public void setInput(String newInput) {
        this.input = newInput;
    }
    
    public void setOutput(String newOutput) {
        this.output = newOutput;
    }
    
}
