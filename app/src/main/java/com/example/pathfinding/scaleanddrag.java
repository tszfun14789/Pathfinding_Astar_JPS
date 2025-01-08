package com.example.pathfinding;

import static java.lang.Math.sqrt;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class scaleanddrag extends AppCompatActivity {
    private ImageView imageView_map;
    private FrameLayout mPhotoBox;
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    private Matrix inverseMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    static final int PRESS = 3;
    int mode = NONE;
    int drag_count = 0;
    float map_scale = 0.0f;
    private PointF startPoint, endPoint;

    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    public boolean onTouch(View v, MotionEvent event) {
        ImageView view = (ImageView) v;
        float[] values = new float[9];
        matrix.getValues(values);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = PRESS;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_UP:
                drag_count = 0;
                if (mode == PRESS) {
                    //new version
                    float[] touchPoint = new float[]{event.getX(), event.getY()};
                    view.getImageMatrix().invert(inverseMatrix);
                    inverseMatrix.mapPoints(touchPoint);
                    float x = touchPoint[0];
                    float y = touchPoint[1];
                    Drawable drawable = view.getDrawable();
                    if (drawable != null) {

                        // Just for debug
                        int intrinsicWidth = drawable.getIntrinsicWidth();
                        int intrinsicHeight = drawable.getIntrinsicHeight();
                        float normalizedX = x / intrinsicWidth;
                        float normalizedY = y / intrinsicHeight;
//                        float[] realworldXY = { (normalizedX *Constants.mapWidth),  (normalizedY *Constants.mapHeight)};
//                        Log.d("scaled_xy", "x = "+realworldXY[0]+", y = "+realworldXY[1]);
                        if (normalizedX >= 0 && normalizedX <= 1 && normalizedY >= 0 && normalizedY <= 1){
                            if (Constants.startPoint == null) {
                                Constants.startPoint = new PointF(normalizedX *Constants.mapwidth, normalizedY *Constants.mapheight);
                                MainActivity.updateStatus("Start point selected at: (" + normalizedX *Constants.mapwidth + ", " + normalizedY *Constants.mapheight + ")");
                            } else if (Constants.endPoint == null) {
                                Constants.endPoint = new PointF(normalizedX *Constants.mapwidth, normalizedY *Constants.mapheight);
                                MainActivity.updateStatus("End point selected at: (" + normalizedX *Constants.mapwidth + ", " + normalizedY *Constants.mapheight + ")");
                            }
                        }
                        else {
                            MainActivity.updateStatus("Touch outside image bounds. Please select a point within the image.");
                        }


                    }

                    break;
                }

            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                drag_count += 1;
                if ((mode == PRESS || mode == DRAG) && drag_count >= 5) {
                    mode = DRAG;
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x,
                            event.getY() - start.y);
                }
                else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        map_scale = newDist / oldDist;
                        matrix.postScale(map_scale, map_scale, mid.x, mid.y);
                    }
                }
                break;
        }

        view.setImageMatrix(matrix);
        return true; // indicate event was handled
    }

    public void resetScaleAndDrag() {
        // Reset the matrix to its default state (identity matrix)
        matrix.reset();
        savedMatrix.reset();
        inverseMatrix.reset();



        // Reset related variables
        mode = NONE;
        drag_count = 0;
        map_scale = 1.0f; // Optional: Reset the scale to the default value
        startPoint = null;
        endPoint = null;
        // Apply the reset matrix to the ImageView
        if (imageView_map != null) {
            imageView_map.setImageMatrix(matrix);
            imageView_map.invalidate(); // Force a redraw of the ImageView
            imageView_map.requestLayout(); // Ensure layout adjustments if needed

        }
        // Optionally update the UI or notify the user
        MainActivity.updateStatus("Scale and drag reset.");
    }

}
