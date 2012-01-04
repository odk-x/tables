package yoonsung.odk.spreadsheet.view;

import yoonsung.odk.spreadsheet.Activity.TableActivity;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * A class for displaying data in list form.
 * 
 * @author hkworden
 */
public class ListDisplayView extends LinearLayout {
    
    private int BACKGROUND_COLOR = Color.WHITE;
    private int TEXT_COLOR = Color.BLACK;
    
    private TableActivity ta; // the table activity to call back to
    private Table table; // the table to display
    private int[] lineHeights;
    private String[][] lineTextSpecs;
    private int[][] lineColSpecs;
    private Paint[] colPaints;
    
    public static ListDisplayView buildView(Context context, DisplayPrefs dp,
            TableActivity ta, Table table) {
        return new ListDisplayView(context, dp, ta, table);
    }
    
    private ListDisplayView(Context context, DisplayPrefs dp,
            TableActivity ta, Table table) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        this.ta = ta;
        this.table = table;
        setFormatInfo();
        removeAllViews();
        setBackgroundColor(BACKGROUND_COLOR);
        buildList(context);
    }
    
    private void setFormatInfo() {
        String format = getDefaultFormat();
        String[] lines = format.split("\n");
        lineHeights = new int[lines.length];
        lineTextSpecs = new String[lines.length][];
        lineColSpecs = new int[lines.length][];
        colPaints = new Paint[lines.length];
        for (int i = 0; i < lines.length; i++) {
            String[] lineData = lines[i].split(":", 2);
            lineHeights[i] = 20;
            Paint paint = new Paint();
            paint.setTextSize(16);
            paint.setColor(TEXT_COLOR);
            for (int j = 0; j < lineData[0].length(); j++) {
                char c = lineData[0].charAt(j);
                if (c == 'b') {
                    paint.setFakeBoldText(true);
                } else if (c == 'u') {
                    paint.setUnderlineText(true);
                } else if (c == 'l') {
                    paint.setTextSize(24);
                    lineHeights[i] = 28;
                }
            }
            colPaints[i] = paint;
            String[] lineSplit = lineData[1].split("%");
            lineTextSpecs[i] = new String[(lineSplit.length / 2) +
                    (lineSplit.length % 2 == 0 ? 0 : 1)];
            lineColSpecs[i] = new int[lineSplit.length / 2];
            for (int j = 0; j < lineSplit.length; j += 2) {
                lineTextSpecs[i][j / 2] = lineSplit[j];
            }
            for (int j = 1; j < lineSplit.length; j += 2) {
                lineColSpecs[i][j / 2] = table.getColNum(lineSplit[j]);
            }
        }
    }
    
    private void buildList(Context context) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        int count = table.getHeight();
        for (int i = 0; i < count; i++) {
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            itemLp.weight = 1;
            wrapper.addView(new ItemView(context, i), itemLp);
        }
        scroll.addView(wrapper);
        addView(scroll);
    }
    
    private String getDefaultFormat() {
        StringBuilder builder = new StringBuilder();
        if (table.getWidth() > 0) {
            builder.append("bl:%" + table.getColName(0) + "%\n");
        }
        for (int i = 1; i < 4 && i < table.getWidth(); i++) {
            builder.append(":%" + table.getColName(i) + "%\n");
        }
        return builder.toString();
    }
    
    private class ItemView extends View {
        
        private int rowNum;
        
        public ItemView(Context context, int rowNum) {
            super(context);
            this.rowNum = rowNum;
            int height = 10;
            for (int i = 0; i < lineHeights.length; i++) {
                height += lineHeights[i];
            }
            setMinimumHeight(height);
        }
        
        @Override
        public void onDraw(Canvas canvas) {
            int y = 0;
            for (int i = 0; i < lineHeights.length; i++) {
                y += lineHeights[i];
                canvas.drawText(getText(i), 0, y, colPaints[i]);
            }
        }
        
        private String getText(int lineNum) {
            StringBuilder builder = new StringBuilder();
            int i;
            for (i = 0; i < lineColSpecs[lineNum].length; i++) {
                builder.append(lineTextSpecs[lineNum][i]);
                int cellLoc = (rowNum * table.getWidth()) +
                        lineColSpecs[lineNum][i];
                builder.append(table.getCellValue(cellLoc));
            }
            if (lineTextSpecs[lineNum].length > i) {
                builder.append(lineTextSpecs[lineNum][i]);
            }
            return builder.toString();
        }
    }
}
