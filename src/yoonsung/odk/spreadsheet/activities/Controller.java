package yoonsung.odk.spreadsheet.activities;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.data.TableViewSettings;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/**
 * A controller for the elements common to the various table display
 * activities.
 * 
 * The general weirdness of how this package is structured (i.e., a Controller
 * class used by unrelated display activities instead of just having those
 * display activities subclass a common parent) is because the Google Maps API
 * requires that activities that use MapViews extend the Android MapActivity
 * (meaning that the MapDisplayActivity couldn't extend the common display
 * activity unless the common display activity extended the Android MapActivity
 * class, which seemed undesirable since that would require that all of the
 * display activities be children of MapActivity for no good reason).
 */
public class Controller {
    
    public static final String INTENT_KEY_TABLE_ID = "tableId";
    public static final String INTENT_KEY_SEARCH = "search";
    public static final String INTENT_KEY_IS_OVERVIEW = "isOverview";
    
    private final ViewGroup wrapper;
    private final EditText searchField;
    private final ViewGroup displayWrap;
    
    Controller(Context context, final DisplayActivity da) {
        LinearLayout controlWrap = new LinearLayout(context);
        searchField = new EditText(context);
        ImageButton searchButton = new ImageButton(context);
        searchButton.setImageResource(R.drawable.search_icon);
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                da.onSearch(searchField.getText().toString());
            }
        });
        ImageButton addRowButton = new ImageButton(context);
        addRowButton.setImageResource(R.drawable.addrow_icon);
        addRowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                da.onAddRow();
            }
        });
        LinearLayout.LayoutParams searchFieldParams =
                new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        searchFieldParams.weight = 1;
        controlWrap.addView(searchField, searchFieldParams);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.weight = 0;
        controlWrap.addView(searchButton, buttonParams);
        controlWrap.addView(addRowButton, buttonParams);
        displayWrap = new LinearLayout(context);
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams controlParams =
                new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        wrapper.addView(controlWrap, controlParams);
        LinearLayout.LayoutParams displayParams =
                new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT);
        wrapper.addView(displayWrap, displayParams);
        this.wrapper = wrapper;
    }
    
    public void setDisplayView(View dv) {
        displayWrap.removeAllViews();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT);
        displayWrap.addView(dv, params);
    }
    
    public void setSearchText(String searchText) {
        searchField.setText(searchText);
    }
    
    public View getWrapperView() {
        return wrapper;
    }
    
    public static void launchTableActivity(Context context, TableProperties tp,
            boolean isOverview) {
        TableViewSettings tvs = isOverview ? tp.getOverviewViewSettings() :
                tp.getCollectionViewSettings();
        Intent i;
        switch (tvs.getViewType()) {
        case TableViewSettings.Type.LIST:
            i = new Intent(context, ListDisplayActivity.class);
            break;
        case TableViewSettings.Type.BOX_STEM:
            i = new Intent(context, BoxStemGraphDisplayActivity.class);
            break;
        default:
            i = new Intent(context, BoxStemGraphDisplayActivity.class);
        }
        i.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
        i.putExtra(INTENT_KEY_IS_OVERVIEW, isOverview);
        context.startActivity(i);
    }
}
