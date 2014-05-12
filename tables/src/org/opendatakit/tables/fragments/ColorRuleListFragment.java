package org.opendatakit.tables.fragments;

import java.util.List;

import org.opendatakit.common.android.data.ColorRule;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.views.components.ColorRuleAdapter;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class ColorRuleListFragment extends ListFragment {
  
  public ColorRuleListFragment() {
    // required for fragments.
  }
  
  /**
   * Retrieve a new instance of {@list ColorRuleListFragment} with the
   * appropriate values set in its arguments.
   * @param colorRuleType
   * @return
   */
  public static ColorRuleListFragment newInstance(
      ColorRuleGroup.Type colorRuleType) {
    ColorRuleListFragment result = new ColorRuleListFragment();
    Bundle bundle = new Bundle();
    IntentUtil.addColorRuleGroupTypeToBundle(bundle, colorRuleType);
    result.setArguments(bundle);
    return result;
  }
    
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof TableLevelPreferencesActivity)) {
      throw new IllegalArgumentException(
          "must be attached to a " +
              TableLevelPreferencesActivity.class.getSimpleName());
    }
  }
  
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(
        R.layout.fragment_color_rule_list,
        container,
        false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    ColorRuleGroup.Type colorRuleType = this.retrieveColorRuleType();
    List<ColorRule> ruleList = this.retrieveColorRulesForType(colorRuleType);
    ColorRuleAdapter adapter = new ColorRuleAdapter(
        getActivity(),
        R.layout.row_for_edit_view_entry,
        ruleList,
        this.retrieveTableLevelPreferencesActivity().getTableProperties(),
        colorRuleType);
    this.setListAdapter(adapter);
  }
  
  /**
   * Retrieve the {@link ColorRuleGroup.Type} from the arguments passed to this
   * fragment.
   * @return
   */
  ColorRuleGroup.Type retrieveColorRuleType() {
    ColorRuleGroup.Type result =
        IntentUtil.retrieveColorRuleTypeFromBundle(this.getArguments());
    return result;
  }
    
  /**
   * Retrieve the {@link TableLevelPreferencesActivity} hosting this fragment.
   * @return
   */
  TableLevelPreferencesActivity retrieveTableLevelPreferencesActivity() {
    TableLevelPreferencesActivity result =
        (TableLevelPreferencesActivity) this.getActivity();
    return result;
  }
  
  /**
   * Retrieve the list of {@link ColorRule}s for the given type. If the type
   * is {@link ColorRuleGroup.Type#COLUMN}, relies on the element key having
   * been set in the hosting activity.
   * @param type
   * @return
   */
  List<ColorRule> retrieveColorRulesForType(ColorRuleGroup.Type type) {
    TableProperties tableProperties =
        this.retrieveTableLevelPreferencesActivity().getTableProperties();
    List<ColorRule> result = null;
    switch (type) {
    case COLUMN:
      String elementKey =
        this.retrieveTableLevelPreferencesActivity().getElementKey();
      result = ColorRuleGroup.getColumnColorRuleGroup(
          tableProperties,
          elementKey).getColorRules();
      break;
    case STATUS_COLUMN:
      result = ColorRuleGroup.getStatusColumnRuleGroup(tableProperties)
        .getColorRules();
      break;
    case TABLE:
      result = ColorRuleGroup.getTableColorRuleGroup(tableProperties)
        .getColorRules();
      break;
    default:
      throw new IllegalArgumentException(
          "unrecognized color rule group type: " + type);
    }
    return result;
  }
  
}
