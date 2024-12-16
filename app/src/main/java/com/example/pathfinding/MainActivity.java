package com.example.pathfinding;

import static com.example.pathfinding.Constants.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private Bitmap mapBitmap;  // Global Bitmap to store the user's uploaded map

    private ImageView mapImageView;
    private static TextView tvStatus;

    Matrix matrix = new Matrix();
    private Matrix inverseMatrix = new Matrix();

    private ProgressBar progressBar;
    private static scaleanddrag scaleanddrag;

    //JPS
    private boolean isFirstJpsRun = true;  // Track if it's the first JPS run
    private JumpPointPreprocessor jpp;    // JPS Preprocessor instance
    private int[][] precomputedGrid;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnUploadMap = findViewById(R.id.btnUploadMap);
        Button btnSelectPoints = findViewById(R.id.btnSelectPoints);
        Button btnCalculatePath = findViewById(R.id.btnCalculatePath);
        Button btnRunJps = findViewById(R.id.btnRunJps);  // JPS Button


        mapImageView = findViewById(R.id.mapImageView);
        scaleanddrag = new scaleanddrag();
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);

        // Upload Map functionality
        btnUploadMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        btnSelectPoints.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset startPoint and endPoint to null
                Constants.startPoint = null;
                Constants.endPoint = null;

                // Optionally update the UI to reflect that points are reset
                updateStatus("Points reset. Please select start and end points.");
            }
        });
        btnRunJps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapBitmap == null) {
                    updateStatus("Please upload a map first.");
                    return;
                }

                if (Constants.startPoint == null || Constants.endPoint == null) {
                    updateStatus("Please select start and end points.");
                    return;
                }

                if (isFirstJpsRun) {
                    // Preprocess jump points on the first run
                    precomputedGrid = getGridFromBitmap(mapBitmap);
                    JpsGrid jpsGrid = new JpsGrid(precomputedGrid);
                    jpp = new JumpPointPreprocessor(jpsGrid);

                    updateStatus("Preprocessing jump points...");
                    new PreprocessJumpPointsTask().execute();
                } else {
                    // Perform the JPS search on subsequent runs
                    updateStatus("Running JPS...");
                    new RunJpsTask().execute();
                }
            }
        });
        // Enable touch interaction for point selection
        mapImageView.setOnTouchListener(scaleanddrag::onTouch);

//        mapImageView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                ImageView view = (ImageView) v;
//                float[] values = new float[9];
//                matrix.getValues(values);
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    float[] touchPoint = new float[]{event.getX(), event.getY()};
//                    view.getImageMatrix().invert(inverseMatrix);
//                    inverseMatrix.mapPoints(touchPoint);
//                    float x = touchPoint[0];
//                    float y = touchPoint[1];
//                    Drawable drawable = view.getDrawable();
//                    if (drawable != null) {
//
//                        // Just for debug
//                        int intrinsicWidth = drawable.getIntrinsicWidth();
//                        int intrinsicHeight = drawable.getIntrinsicHeight();
//                        float normalizedX = x / intrinsicWidth;
//                        float normalizedY = y / intrinsicHeight;
////                        float[] realworldXY = { (normalizedX *Constants.mapWidth),  (normalizedY *Constants.mapHeight)};
////                        Log.d("scaled_xy", "x = "+realworldXY[0]+", y = "+realworldXY[1]);
//
//                        if (startPoint == null) {
//                            startPoint = new PointF(normalizedX * 928, normalizedY * 1167);
//                            updateStatus("Start point selected at: (" + normalizedX * 928 + ", " + normalizedY * 1167 + ")");
//                        } else if (endPoint == null) {
//                            endPoint = new PointF(normalizedX * 928, normalizedY * 1167);
//                            updateStatus("End point selected at: (" + normalizedX * 928 + ", " + normalizedY * 1167 + ")");
//                        }
//                    } else {
//                        updateStatus("Touch outside image bounds. Please select a point within the image.");
//                    }
//                }
//
//                return true;
//            }
//        });

        // Calculate Path functionality
        btnCalculatePath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Constants.startPoint != null && Constants.endPoint != null) {
                    new CalculatePathTask().execute();  // Use AsyncTask to calculate path
                } else {
                    updateStatus("Please select both start and end points.");
                }
            }
        });
    }

    private class PreprocessJumpPointsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            jpp.precomputeJumpPoints();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBar.setVisibility(View.INVISIBLE);
            isFirstJpsRun = false;  // Update flag after preprocessing
            updateStatus("Jump points preprocessed. Ready for JPS search.");
        }
    }
    private class RunJpsTask extends AsyncTask<Void, Void, List<JpsNode>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<JpsNode> doInBackground(Void... voids) {
            JpsNode startNode = new JpsNode((int) Constants.startPoint.x, (int) Constants.startPoint.y);
            JpsNode endNode = new JpsNode((int) Constants.endPoint.x, (int) Constants.endPoint.y);

            return jpp.searchWithPrecomputedJPS(startNode, endNode);
        }

        @Override
        protected void onPostExecute(List<JpsNode> path) {
            super.onPostExecute(path);
            progressBar.setVisibility(View.INVISIBLE);

            if (path != null && !path.isEmpty()) {
                updateStatus("Path found with " + path.size() + " steps.");
                drawJPSPathOnMap(mapBitmap, path);
//                drawPathOnMap(mapBitmap, path);  // Draw the path on the map
            } else {
                updateStatus("No path found.");
            }
        }
    }
    // Method to open the gallery and allow the user to pick an image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");  // Filter to allow only images
        startActivityForResult(intent, PICK_IMAGE_REQUEST);  // Start activity for result
    }

    // Handle the result of the image selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            loadMapImage(imageUri);  // Load the image after selection
        }
    }

    // Load the image from the selected URI and display it in ImageView
    private void loadMapImage(Uri imageUri) {
        try {
            mapBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            mapImageView.setImageBitmap(mapBitmap);  // Display the Bitmap in ImageView

            int[][] grid = getGridFromBitmap(mapBitmap); // Method to convert bitmap to grid

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateStatus(String message) {
        tvStatus.setText("Status: " + message);
    }

    // AsyncTask to calculate the path on a background thread
    private class CalculatePathTask extends AsyncTask<Void, Void, List<Node>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);  // Show the progress bar before task starts
        }

        @Override
        protected List<Node> doInBackground(Void... voids) {
            // Perform the pathfinding work in the background
            return calculateAndDisplayPath();
        }

        @Override
        protected void onPostExecute(List<Node> path) {
            super.onPostExecute(path);
            progressBar.setVisibility(View.INVISIBLE);  // Hide progress bar once task is done

            if (path != null && !path.isEmpty()) {
                updateStatus("Path found with " + path.size() + " steps.");
                drawPathOnMap(mapBitmap, path);  // Draw the path on the map
            } else {
                updateStatus("No path found.");
            }
        }
    }

    private List<Node> calculateAndDisplayPath() {
        int[][] grid = getGridFromBitmap(mapBitmap); // Method to convert bitmap to grid
        Node startNode = new Node((int) Constants.startPoint.x, (int) Constants.startPoint.y);
        Node endNode = new Node((int) Constants.endPoint.x, (int) Constants.endPoint.y);

        // Log grid and points for debugging
        for (int[] row : grid) {
            Log.d("Pathfinding", "Grid Row: " + Arrays.toString(row));
        }
        Log.d("Pathfinding", "Start: (" + startNode.x + ", " + startNode.y + ")");
        Log.d("Pathfinding", "End: (" + endNode.x + ", " + endNode.y + ")");

        // Using AstarAlgorithm to find the path
        return AstarAlgorithm.findPath(startNode, endNode, grid);
    }

    // Convert the Bitmap to a 2D grid for pathfinding
    public int[][] getGridFromBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Constants.mapwidth = width;
        Constants.mapheight = height;
        Log.d("bitmap", String.valueOf(width));
        Log.d("bitmap", String.valueOf(height));

        // Create a 2D grid to represent the map
        int[][] grid = new int[height][width];

        // Loop through each pixel in the bitmap
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                // Check if the pixel is light (walkable) or dark (non-walkable)
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                // If the pixel is light enough, it's walkable
                if (red < 50 && green < 50 && blue < 50) {
                    grid[y][x] = 1; // non-Walkable
                } else {
                    grid[y][x] = 0; // -walkable
                }
            }
        }
        return grid;
    }

    // Draw the path on the uploaded map
    public void drawPathOnMap(Bitmap originalMap, List<Node> path) {
        // Create a mutable bitmap from the original map (so we can modify it)
        Bitmap mutableMap = originalMap.copy(Bitmap.Config.ARGB_8888, true);

        // Create a Canvas to draw on the mutable bitmap
        Canvas canvas = new Canvas(mutableMap);

        // Set up the paint for the path (for example, red color and stroke width)
        Paint paint = new Paint();
        paint.setColor(Color.RED);   // Color of the path (you can customize)
        paint.setStrokeWidth(1);     // Line width for the path

        // Loop through the nodes in the path and draw a circle for each
        for (Node node : path) {
            float x = node.x;  // Get the X coordinate
            float y = node.y;  // Get the Y coordinate
            canvas.drawCircle(x, y, 1, paint);  // Draw a small circle at each node's position
        }

        // Set the modified bitmap with the drawn path back to the ImageView
        mapImageView.setImageBitmap(mutableMap);
    }

    public void drawJPSPathOnMap(Bitmap originalMap, List<JpsNode> path) {
        // Create a mutable bitmap from the original map (so we can modify it)
        Bitmap mutableMap = originalMap.copy(Bitmap.Config.ARGB_8888, true);

        // Create a Canvas to draw on the mutable bitmap
        Canvas canvas = new Canvas(mutableMap);

        // Set up the paint for the path (for example, red color and stroke width)
        Paint paint = new Paint();
        paint.setColor(Color.RED);   // Color of the path (you can customize)
        paint.setStrokeWidth(1);     // Line width for the path

        // Loop through the nodes in the path and draw a circle for each
        for (JpsNode jpsNode : path) {
            float x = jpsNode.x;  // Get the X coordinate
            float y = jpsNode.y;  // Get the Y coordinate
            canvas.drawCircle(x, y, 1, paint);  // Draw a small circle at each node's position
        }

        // Set the modified bitmap with the drawn path back to the ImageView
        mapImageView.setImageBitmap(mutableMap);
    }

}
