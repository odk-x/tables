package org.opendatakit.tables.DataStructure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.TableProperties;

import android.util.Log;

/**
 * From what I can tell, a ColumnColorRuler is essentially what is used to
 * determine the correct conditional coloring of a column. A ruler is 
 * composed of zero or more {@link ColColorRule} objects. 
 * <p>
 * The ColumnColorRuler is the access point for these rules.
 * @author sudar.sam@gmail.com
 * @author uknown
 *
 */
public class ColumnColorRuler {
  
  private static final String TAG = "ColumnColorRuler";
  
  /*****************************
   * Things needed for the key value store.
   *****************************/
  public static final String KVS_PARTITION = "ColumnColorRuler";
  public static final String KEY_COLOR_RULES = 
      "ColumnColorRuler.ruleList";
  public static final String DEFAULT_KEY_COLOR_RULES = "[]";
  
  private final TableProperties tp;
  private final ColumnProperties cp;
  // this remains its own field (which must always match cp.getElementKey())
  // b/c it is easier for the caller to just pass in the elementKey, and the 
  // code currently uses null to mean "don't get me a color ruler."
  private final String elementKey;
  private final ObjectMapper mapper;
  private final TypeFactory typeFactory;
  // This is the list of actual rules that make up the ruler.
  private List<ColColorRule> ruleList;
          
    private ColumnColorRuler(TableProperties tp, String elementKey) {
      this.tp = tp;
      this.elementKey = elementKey;
      this.mapper = new ObjectMapper();
      this.typeFactory = mapper.getTypeFactory();
      mapper.setVisibilityChecker(
          mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
      mapper.setVisibilityChecker(
          mapper.getVisibilityChecker()
          .withCreatorVisibility(Visibility.ANY));
      if (elementKey == null) {
        this.cp = null;
      } else {
        this.cp = tp.getColumnByElementKey(elementKey);
      }
      this.ruleList = loadSavedColorRules();
    }
    
    public static ColumnColorRuler getColumnColorRuler(TableProperties tp,
        String elementKey) {
      return new ColumnColorRuler(tp, elementKey);
    }
    
    /**
     * Parse the ColColorRules in from the database. This should only be
     * called on creation of the object ColumnColorRuler object, as it 
     * could be expensive with JSON parsing.
     * @return
     */
    private List<ColColorRule> loadSavedColorRules() {
      // do this here b/c indexed columns being null passes around null values.
      if (this.elementKey == null) {
        return new ArrayList<ColColorRule>();
      }
      String jsonRulesString = 
          tp.getObjectEntry(KVS_PARTITION,
          this.elementKey, KEY_COLOR_RULES);
      if (jsonRulesString == null) { // no values in the kvs
        return new ArrayList<ColColorRule>();
      }
      List<ColColorRule> reclaimedRules = new ArrayList<ColColorRule>();
      try {
        reclaimedRules = 
            mapper.readValue(jsonRulesString, 
                typeFactory.constructCollectionType(ArrayList.class, 
                    ColColorRule.class));
      } catch (JsonParseException e) {
        Log.e(TAG, "problem parsing json to colocolorrule");
        e.printStackTrace();
      } catch (JsonMappingException e) {
        Log.e(TAG, "problem mapping json to colocolorrule");
        e.printStackTrace();
      } catch (IOException e) {
        Log.e(TAG, "i/o problem with json to colocolorrule");
        e.printStackTrace();
      }
      return reclaimedRules;
    }
    
    /**
     * Return the list of rules that makes up this column. This should only be
     * used for displaying the rules. Any changes to the list should be made
     * via the add, delete, and update methods in ColumnColorRuler.
     * @return
     */
    public List<ColColorRule> getColorRules() {
      return ruleList;
    }
    
    /**
     * Replace the list of rules that define this ColumnColorRuler.
     * @param newRules
     */
    public void replaceColorRuleList(List<ColColorRule> newRules) {
      this.ruleList = newRules;
    }
    
    /**
     * Persist the rule list into the key value store. Does nothing if there are
     * no rules, so will not pollute the key value store unless something has
     * been added.
     */
    public void saveRuleList() {
      if (elementKey == null) { // this should never happen...
        Log.e(TAG, "tried to save a rule list from a ruler with a null " +
              " element key.");
        return;
      }
      // if there are no rules, we want to remove the key from the kvs.
      if (ruleList.size() == 0) {
        tp.removeEntry(KVS_PARTITION, elementKey, 
            KEY_COLOR_RULES);
        return;
      }
      // set it to this default just in case something goes wrong and it is 
      // somehow set. this way if you manage to set the object you will have
      // something that doesn't throw an error when you expect to get back 
      // an array list. it will just be of length 0. not sure if this is a good
      // idea or not.
      String ruleListJson = DEFAULT_KEY_COLOR_RULES;
      try {
        ruleListJson = mapper.writeValueAsString(ruleList);
        tp.setObjectEntry(KVS_PARTITION, elementKey, 
            KEY_COLOR_RULES, ruleListJson);
      } catch (JsonGenerationException e) {
        Log.e(TAG, "problem parsing list of color rules");
        e.printStackTrace();
      } catch (JsonMappingException e) {
        Log.e(TAG, "problem mapping list of color rules");
        e.printStackTrace();
      } catch (IOException e) {
        Log.e(TAG, "i/o problem with json list of color rules");
        e.printStackTrace();
      }
    }
//    
//    private void addRule(ColColorRule.RuleType compType, String val, 
//        int foreground,
//            int background) {
//        ruleComps.add(compType);
//        ruleVals.add(val);
//        foregroundColors.add(foreground);
//        backgroundColors.add(background);
//    }
    
    private void addRule(ColColorRule rule) {
//      ruleComps.add(rule.compType);
//      ruleVals.add(rule.val);
//      foregroundColors.add(rule.foreground);
//      backgroundColors.add(rule.background);
      ruleList.add(rule);
    }
    
    /**
     * Replace the rule matching updatedRule's id with updatedRule.
     * @param updatedRule
     */
    public void updateRule(ColColorRule updatedRule) {
      for (int i = 0; i < ruleList.size(); i++) {
        if (ruleList.get(i).getId().equals(updatedRule.getId())) {
          ruleList.set(i, updatedRule);
          return;
        }
      }
      Log.e(TAG, "tried to update a rule that matched no saved ids");
    }
    
    /**
     * Remove the given rule from the rule list.
     * @param rule
     */
    public void removeRule(ColColorRule rule) {
      for (int i = 0; i < ruleList.size(); i++) {
        if (ruleList.get(i).getId().equals(rule.getId())) {
          ruleList.remove(i);
          return;
        }
      }
      Log.d(TAG, "a rule was passed to deleteRule that did not match" +
           " the id of any rules in the list");
    }
    
    public int getRuleCount() {
//        return ruleComps.size();
      return ruleList.size();
    }
    
    public int getForegroundColor(String val, int defVal) {
        for(int i=0; i<ruleList.size(); i++) {
            if(checkMatch(val, i)) {
                return ruleList.get(i).getForeground();
            }
        }
        return defVal;
    }
    
    public int getBackgroundColor(String val, int defVal) {
        for(int i=0; i<ruleList.size(); i++) {
            if(checkMatch(val, i)) {
                return ruleList.get(i).getBackground();
            }
        }
        return defVal;
    }
    
    private boolean checkMatch(String val, int index) {
      try {
        int compVal;
          String ruleVal = ruleList.get(index).getVal();
          if((cp.getColumnType() == ColumnType.NUMBER ||
             cp.getColumnType() == ColumnType.INTEGER)){
            if (val.equals("")) {
              return false;
            }
              double doubleValue = Double.parseDouble(val);
              double doubleRule = Double.parseDouble(ruleVal);
              Log.d("DP", "doubleValue:" + doubleValue);
              Log.d("DP", "doubleRule:" + doubleRule);
              compVal = (Double.valueOf(val)).compareTo(Double.valueOf(ruleVal));
          } else {
              compVal = val.compareTo(ruleVal);
          }
          Log.d("DP", "ruleVal:" + ruleVal);
          Log.d("DP", "val:" + val);
          Log.d("DP", "compVal:" + compVal);
          switch(ruleList.get(index).getOperator()) {
          case LESS_THAN:
            return (compVal < 0);
          case LESS_THAN_OR_EQUAL:
            return (compVal <= 0);
          case EQUAL:
              return (compVal == 0);
          case GREATER_THAN_OR_EQUAL:
              return (compVal >= 0);
          case GREATER_THAN:
              return (compVal > 0);
          default:
              Log.e(TAG, "unrecongized op passed to checkMatch: " + 
                  ruleList.get(index).getOperator());
              throw new IllegalArgumentException("unrecognized op passed " +
                  "to checkMatch: " + ruleList.get(index).getOperator());
          }
      } catch (NumberFormatException e) {
        ruleList.remove(index);
        saveRuleList();
        e.printStackTrace();
        Log.e(TAG, "was an error parsing value to a number, removing the " +
        		"offending rule");
      }
      return false;
    }
    
}
