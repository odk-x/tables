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
package org.opendatakit.hope.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SS: I can't actually remember if I wrote this or if someone else did. IT is 
 * being migrated out of the defunct TableViewSettings object.
 * @author sudar.sam@gmail.com
 *
 */
public class ConditionalRuler {

  private static final String JSON_KEY_COMPARATORS = "comparators";
  private static final String JSON_KEY_VALUES = "values";
  private static final String JSON_KEY_SETTINGS = "settings";

  public class Comparator {
    public static final int EQUALS = 0;
    public static final int LESS_THAN = 1;
    public static final int LESS_THAN_EQUALS = 2;
    public static final int GREATER_THAN = 3;
    public static final int GREATER_THAN_EQUALS = 4;
    public static final int COUNT = 5;

    private Comparator() {
    }
  }

  private final List<Integer> comparators;
  private final List<String> values;
  private final List<Integer> settings;

  ConditionalRuler() {
    comparators = new ArrayList<Integer>();
    values = new ArrayList<String>();
    settings = new ArrayList<Integer>();
  }

  ConditionalRuler(Map<String, Object> jo) {
    comparators = new ArrayList<Integer>();
    values = new ArrayList<String>();
    settings = new ArrayList<Integer>();
    ArrayList<Integer> comparatorsArr = (ArrayList<Integer>) jo.get(JSON_KEY_COMPARATORS);
    ArrayList<String> valuesArr = (ArrayList<String>) jo.get(JSON_KEY_VALUES);
    ArrayList<Integer> colorsArr = (ArrayList<Integer>) jo.get(JSON_KEY_SETTINGS);
    for (int i = 0; i < comparatorsArr.size(); i++) {
      comparators.add(comparatorsArr.get(i));
      values.add((String) valuesArr.get(i));
      settings.add((Integer) colorsArr.get(i));
    }
  }

  public int getSetting(String value, int defaultSetting) {
    for (int i = 0; i < comparators.size(); i++) {
      if (checkMatch(i, value)) {
        return settings.get(i);
      }
    }
    return defaultSetting;
  }

  private boolean checkMatch(int index, String value) {
    switch (comparators.get(index)) {
    case Comparator.EQUALS:
      return value.equals(values.get(index));
    case Comparator.LESS_THAN:
      return (value.compareTo(values.get(index)) < 0);
    case Comparator.LESS_THAN_EQUALS:
      return (value.compareTo(values.get(index)) <= 0);
    case Comparator.GREATER_THAN:
      return (value.compareTo(values.get(index)) > 0);
    case Comparator.GREATER_THAN_EQUALS:
      return (value.compareTo(values.get(index)) >= 0);
    default:
      throw new RuntimeException();
    }
  }

  public void addRule(int comparator, String value, int setting) {
    comparators.add(comparator);
    values.add(value);
    settings.add(setting);
//      set();
  }

  public int getRuleCount() {
    return comparators.size();
  }

  public int getRuleComparator(int index) {
    return comparators.get(index);
  }

  public void setRuleComparator(int index, int comparator) {
    comparators.set(index, comparator);
//      set();
  }

  public String getRuleValue(int index) {
    return values.get(index);
  }

  public void setRuleValue(int index, String value) {
    values.set(index, value);
//      set();
  }

  public int getRuleSetting(int index) {
    return settings.get(index);
  }

  public void setRuleSetting(int index, int setting) {
    settings.set(index, setting);
//      set();
  }

  public void deleteRule(int index) {
    comparators.remove(index);
    values.remove(index);
    settings.remove(index);
//      set();
  }

  Map<String, Object> toJsonObject() {
    Map<String, Object> jo = new HashMap<String, Object>();
    jo.put(JSON_KEY_COMPARATORS, comparators);
    jo.put(JSON_KEY_VALUES, values);
    jo.put(JSON_KEY_SETTINGS, settings);
    return jo;
  }
}