package yoonsung.odk.spreadsheet.DataStructure;

public class MessageShortcut {
    
    private int id;
    private String name;
    private String input;
    private String output;
    
    public MessageShortcut(int id, String name, String input, String output) {
        this.id = id;
        this.name = name;
        this.input = input;
        this.output = output;
    }
    
    public int getId() { return id; }
    
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
