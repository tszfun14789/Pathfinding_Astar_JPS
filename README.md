# Pathfinding Visualization App

An Android application that demonstrates different pathfinding algorithms with interactive visualization. The app allows users to upload custom maps, select start/end points, and visualize pathfinding algorithms.

## Features

### Map Management
- Upload custom maps from device gallery
- Interactive map viewing with zoom and pan capabilities
- grid generation from bitmap images
- Black pixels are treated as obstacles, white pixels as walkable areas

### Pathfinding Algorithms
1. **A* (A-Star)**
   - Traditional pathfinding algorithm
   - Uses Manhattan distance heuristic
   - Supports both cardinal and diagonal movements
   - Cost: 10 for cardinal directions, 14 for diagonal

2. **JPS (Jump Point Search)**
   - Optimized pathfinding algorithm
   - Includes preprocessing for better performance
   - Reduces the number of nodes explored
   - Particularly efficient for open areas

## Usage

1. **Upload Map**
   - Click "Upload Map" button
   - Select an image from gallery
   - Map will be processed automatically

2. **Select Points**
   - Click "Select Points" to reset current points
   - Tap on the map to set start point (first tap)
   - Tap again to set end point (second tap)

3. **Calculate Path**
   - Choose between A* and JPS algorithms
   - Click respective button to calculate path
   - Path will be displayed on the map
   - Status updates show calculation progress

## Implementation Details

### Core Components
- `MainActivity`: Main UI and interaction handling
- `AstarAlgorithm`: A* pathfinding implementation
- `JumpPointPreprocessor`: JPS algorithm with preprocessing
- `scaleanddrag`: Touch interaction handling

### Data Structures
- `Node`: Basic pathfinding node for A*
- `JpsNode`: Specialized node for JPS
- `JpsGrid`: Grid representation for JPS
- `Constants`: Shared configuration values

## Performance

- JPS preprocessing improves subsequent pathfinding speed
- Asynchronous processing prevents UI freezing
- Optimized path visualization
