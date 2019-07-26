package org.opendatakit.tables.views;

/*
 * @author belendia@gmail.com
 */

import android.content.Context;
import android.graphics.Canvas;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

public class CompassView extends AppCompatImageView {
  private float deg = 0;

  public CompassView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    int width = getWidth();
    int height = getHeight();

    canvas.rotate(360 - deg, width/2, height/2);
    super.onDraw(canvas);
  }

  public void setDegrees(float degrees) {
    deg = degrees;
    invalidate();
  }
}
