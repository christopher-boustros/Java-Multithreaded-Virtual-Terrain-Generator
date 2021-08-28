
/*
 * Copyright (c) 2021 Christopher Boustros <github.com/christopher-boustros>
 */
import java.awt.Color;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates and executes the HeightAdjuster threads. The threads will use
 * fault-lines to randomly generate height values that will be used to create an
 * image of a virtual terrain.
 */
public class TerrainGenerator {
	// Parameters: width, height, t, k
	/**
	 * The width of the image (one unit above the maximum x-coorinate).
	 */
	private static int width;

	/**
	 * The height of the image (one unit above the maximum y-coordinate).
	 */
	private static int height;

	/**
	 * The number of HeightAdjuster threads used to modify height values.
	 */
	private static int t;

	/**
	 * The maximum number of fault lines to create.
	 */
	private static int k;

	/**
	 * The total number of fault lines that have been created by all HeightAdjuster
	 * threads. A thread-safe shared variable.
	 */
	private static AtomicInteger faultLineCount = new AtomicInteger(0);

	/**
	 * The list of all HeightAdjuster threads created
	 */
	private static ArrayList<HeightAdjuster> threads = new ArrayList<>();

	/**
	 * The time the HeightAdjuster threads started and terminated (used to measure
	 * execution time).
	 */
	private static long startTime, endTime;

	/**
	 * The global minimum and maximum height value in the grid after the
	 * HeightAdjuster threads terminate.
	 */
	private static int globalMin = Integer.MAX_VALUE, globalMax = 0;

	/**
	 * The maximum height adjustment value for a point on the grid
	 */
	public static final int MAX_HEIGHT_ADJUSTMENT_VALUE = 10;

	/**
	 * The name of the output image file
	 */
	public static final String OUTPUT_IMAGE_FILE_NAME = "terrain";

	/**
	 * An instance of Random to be used by the HeightAdjuster threads
	 */
	public static final Random rand = new Random();

	/**
	 * The grid of height values for all points (y, x) on the image, where x is the
	 * axis parallel to the width and y is the axis parallel to the height. Note:
	 * the order is (row, column) = (y, x). This grid is a matrix of AtomicInteger
	 * objects, which can be modified lock-free and thread-safe. This allows threads
	 * to modify different elements of the grid simultaneously, while a particular
	 * element cannot be modified by multiple threads simultaneously. The elements
	 * of this grid are shared variables. The grid itself is not modified by any
	 * thread, only its elements.
	 */
	private static AtomicInteger[][] grid;

	/**
	 * The main method. Expects four command-line arguments to specify the width,
	 * height, number of threads, and maximum number of fault lines.
	 * 
	 * @param args the command-line arguments.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		// Parse the command-line arguments w, h, t, k
		try {
			parseArguments(args);
		} catch (IllegalArgumentException e) {
			// Parse unsuccessful
			System.out.println("Error: " + e.getMessage()); // Print error message
			return; // Exit
		}

		// Display the parameters
		System.out.println("Parameters: width = " + width + ", height = " + height + ", number of threads = " + t
				+ ", maximum number of fault lines = " + k);

		// Initialize the grid with 0s for height values
		grid = new AtomicInteger[height][width];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				grid[y][x] = new AtomicInteger(0);
			}
		}

		// Get the current time before the threads start
		startTime = System.currentTimeMillis();

		// Instantiate and start the t threads
		createThreads();

		// Wait until all Threads have terminated
		for (Thread thread : threads) {
			thread.join();
		}

		// Get the current time now that all threads have terminated
		endTime = System.currentTimeMillis();

		// Display the execution time
		System.out.println("Execution time: " + (endTime - startTime) + " ms");

		// Determine the global minimum and maximum height value in the grid
		for (AtomicInteger[] row : grid) { // Traverse the grid
			for (AtomicInteger i : row) {
				int val = i.get();

				if (val > globalMax) {
					globalMax = val;
				}

				if (val < globalMin) {
					globalMin = val;
				}
			}
		}

		// Create an empty image of size width x height
		BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		// Set the RGB values of each pixel (x, y) using the corresponding height value
		// of the grid element (y, x)
		// mapped from range [globalMin, globalMax] to [0, 255]

		// For each pixel (x, y) (or grid element (y, x))
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int heightValue = grid[y][x].get(); // Get the height value of grid element (y, x)
				int rgb = heightValueToRGB(globalMin, globalMax, heightValue); // Compute the corresponding RGB value of
																				// pixel (x, y)
				outputImage.setRGB(x, y, rgb); // Set the RGB value of pixel (x, y)
			}
		}

		// Write the image to a file
		File outputFile = new File(OUTPUT_IMAGE_FILE_NAME + ".png");
		ImageIO.write(outputImage, "png", outputFile);
	}

	/*
	 * Helper methods
	 */

	/**
	 * Parses the four command-line arguments as integers.
	 * 
	 * @param args the command-line arguments w, h, t, and k (k > 8).
	 * @throws IllegalArgumentException if the parse is unsuccessful.
	 */
	private static void parseArguments(String args[]) throws IllegalArgumentException {
		// Verify number of arguments
		if (args.length != 4) {
			throw new IllegalArgumentException("There must be exactly 4 arguments"); // Invalid number of arguments
		}

		// Parse arguments
		int _w = Integer.parseInt(args[0]);
		int _h = Integer.parseInt(args[1]);
		int _t = Integer.parseInt(args[2]);
		int _k = Integer.parseInt(args[3]);

		// Verify argument values
		if (!(_w >= 1 && _h >= 1 && _t >= 1 && _k >= 1)) {
			throw new IllegalArgumentException("All argument values must be at least 1"); // Invalid argument values
		}

		// Set the argument values
		width = _w;
		height = _h;
		t = _t;
		k = _k;

		// Parse was successful
	}

	/**
	 * Instantiates and starts all t HeightAdjuster threads.
	 */
	private static void createThreads() {
		// Instantiate the threads
		for (int i = 0; i < t; i++) {
			threads.add(new HeightAdjuster());
		}

		// Start the threads
		for (int i = 0; i < t; i++) {
			threads.get(i).start();
		}
	}

	/**
	 * Converts a height value to an RGB value by mapping a given range [min, max]
	 * to [0, 255]. The result will be a grayscale RGB value that is brighter the
	 * closer 'value' is to the upper-bound of [min, max].
	 * 
	 * @param min   the lower bound of the given range.
	 * @param max   the upper bound of the given range.
	 * @param value the height value to be converted.
	 * @return an integer representing an RGB value between 0 and 255.
	 */
	private static int heightValueToRGB(int min, int max, int value) {
		if (!(min >= 0 && max >= min && value >= min && value <= max)) {
			throw new IllegalArgumentException(); // Invalid input
		}

		// Map value from [min, max] to [0, 255]
		int newValue = (int) ((value - min) * (255.0 / (max - min)));

		// Compute RGB value of (newValue, newValue, newValue)
		int rgb = new Color(newValue, newValue, newValue).getRGB();

		return rgb;
	}

	/**
	 * Given two endpoints of a line p0=(p0y, p0x) and p1=(p1y, p1x), and given a
	 * third point p2=(p2y, p2x), this method returns an integer that is greater
	 * than 0 if p2 is left of the line, less than 0 if p2 is right of the line, and
	 * 0 if p2 is on the line.
	 * 
	 * @param p0
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static int whichSideOfLine(int[] p0, int[] p1, int[] p2) {
		return (p1[1] - p0[1]) * (p2[0] - p0[0]) - (p2[1] - p0[1]) * (p1[0] - p0[0]);
	}

	/**
	 * Returns a random height adjustment value for a point in the grid.
	 * 
	 * @return
	 */
	public static int getRandomHeightAdjustmentValue() {
		return rand.nextInt(MAX_HEIGHT_ADJUSTMENT_VALUE + 1);
	}

	/**
	 * Increases the height value of a point (y, x) on the grid by h.
	 * 
	 * @param y
	 * @param x
	 * @param h
	 */
	public static void increaseGridValue(int y, int x, int h) {
		// If the parameters are invalid, throw exception
		if (!(x >= 0 && x < width && y >= 0 && y < height && h >= 0 && h <= MAX_HEIGHT_ADJUSTMENT_VALUE)) {
			throw new IllegalArgumentException();
		}

		grid[y][x].addAndGet(h); // Add h to the height value of point (y, x) atomically (thread-safe)
	}

	/**
	 * Increments the faultLineCount, or returns false if it has reached its
	 * maximum.
	 * 
	 * @return
	 */
	public static boolean incrementFaultLineCount() {
		if (faultLineCount.get() >= k) {
			return false;
		}

		faultLineCount.incrementAndGet(); // Increments faultLineCount atomically (thread-safe)
		return true;
	}

	/*
	 * Getter methods
	 */

	/**
	 * Getter method for width of the image.
	 * 
	 * @return
	 */
	public static int getWidth() {
		return width;
	}

	/**
	 * Getter method for height of the image.
	 * 
	 * @return
	 */
	public static int getHeight() {
		return height;
	}
}

/**
 * A thread that adjusts the heights of points on the grid by creating fault
 * lines. The grid is the matrix of height values assigned to each point (x, y)
 * on the image.
 */
class HeightAdjuster extends Thread {
	/**
	 * The chosen entry point of the fault line.
	 */
	private int[] entryPoint = new int[2];

	/**
	 * The chosen exit point of the fault line.
	 */
	private int[] exitPoint = new int[2];

	/**
	 * The random height adjustment chosen in range [0, MAX_HEIGHT_VALUE].
	 */
	private int heightAdjustment;

	/**
	 * Describes left and right.
	 */
	private enum Side {
		LEFT(1), RIGHT(-1);

		private int value;

		Side(int value) {
			this.value = value;
		}
	}

	/**
	 * The random side of the line chosen: left (1) or right (-1).
	 */
	Side side;

	@Override
	public void run() {
		// Repeat until the maximum number of fault-lines have been created
		while (TerrainGenerator.incrementFaultLineCount()) {
			// 1 - Choose a random entry and exit point from two different boundary edges of
			// the grid to define a fault line
			chooseRandomFaultLine(); // Initializes entryPoint and exitPoint

			// 2 - Choose a random integer height adjustment within a range of [0,
			// MAX_HEIGHT_VALUE]
			heightAdjustment = TerrainGenerator.getRandomHeightAdjustmentValue();

			// 3 - Choose a random side of the line: left or right
			side = TerrainGenerator.rand.nextInt(2) == 0 ? Side.LEFT : Side.RIGHT;

			// 4 - Add the height adjustment value to every point on the grid on the chosen
			// side of the line
			addHeightAdjustment();
		}
	}

	/*
	 * Helper methods
	 */

	/**
	 * Initializes entryPoint and exitPoint as the end points of a randomly-chosen
	 * fault line.
	 */
	private void chooseRandomFaultLine() {
		ArrayList<Integer> edges = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3)); // A list of edges to choose from

		// Choose random, distinct boundary edges for the entry and exit points
		int entryBoundary = TerrainGenerator.rand.nextInt(4);
		edges.remove(entryBoundary);
		int exitBoundary = edges.get(TerrainGenerator.rand.nextInt(3));

		boolean cornerPointAllowed = false; // Indicates whether a corner point can be randomly chosen
		// If the boundaries are not adjacent
		if (((entryBoundary == 0 || entryBoundary == 1) && (exitBoundary == 0 || exitBoundary == 1))
				|| ((entryBoundary == 2 || entryBoundary == 3) && (exitBoundary == 2 || exitBoundary == 3))) {
			// A corner point can be selected only if the boundaries are not adjacent,
			// because a corner point on an adjacent boundary will result in two points on
			// the same boundary
			cornerPointAllowed = true;
		}

		// Set the entry and exit points randomly according to the chosen boundary
		setPointOnBoundary(entryBoundary, entryPoint, cornerPointAllowed);
		setPointOnBoundary(exitBoundary, exitPoint, cornerPointAllowed);
	}

	/**
	 * Sets the entry or exit point randomly according to a chosen boundary. A
	 * boundary is an edge of the grid. There are four boundaries: the edges where y
	 * = 0, y = height, x = 0, and x = width
	 * 
	 * @param boundary           an integer representing one of the four boundaries
	 *                           of the grid
	 * @param point              the entry or exit point whose coordinates need to
	 *                           be set
	 * @param cornerPointAllowed is true if a corner point can be chosen at the
	 *                           entry/exit point, false otherwise
	 */
	private void setPointOnBoundary(int boundary, int[] point, boolean cornerPointAllowed) {
		// Set the entry/exit point randomly according to the chosen boundary
		switch (boundary) {
		case 0: // y = 0 boundary
			point[0] = 0;
			if (cornerPointAllowed) {
				point[1] = TerrainGenerator.rand.nextInt(TerrainGenerator.getWidth()); // Random x-coordinate
			} else {
				point[1] = TerrainGenerator.rand.nextInt(TerrainGenerator.getWidth() - 2) + 1; // Random x-coordinate,
																								// except 0 and width-1
			}
			break;
		case 1: // y = height boundary
			point[0] = TerrainGenerator.getHeight() - 1;
			if (cornerPointAllowed) {
				point[1] = TerrainGenerator.rand.nextInt(TerrainGenerator.getWidth()); // Random x-coordinate
			} else {
				point[1] = TerrainGenerator.rand.nextInt(TerrainGenerator.getWidth() - 2) + 1; // Random x-coordinate,
																								// except 0 and width-1
			}
			break;
		case 2: // x = 0 boundary
			if (cornerPointAllowed) {
				point[0] = TerrainGenerator.rand.nextInt(TerrainGenerator.getHeight()); // Random y-coordinate
			} else {
				point[0] = TerrainGenerator.rand.nextInt(TerrainGenerator.getHeight() - 2) + 1; // Random y-coordinate,
																								// except 0 and height-1
			}
			point[1] = 0;
			break;
		default: // x = width boundary
			if (cornerPointAllowed) {
				point[0] = TerrainGenerator.rand.nextInt(TerrainGenerator.getHeight()); // Random y-coordinate
			} else {
				point[0] = TerrainGenerator.rand.nextInt(TerrainGenerator.getHeight() - 2) + 1; // Random y-coordinate,
																								// except 0 and height-1
			}
			point[1] = TerrainGenerator.getWidth() - 1;
		}
	}

	/**
	 * Adds the chosen height adjustment value to every point on the chosen side of
	 * the chosen fault line.
	 */
	private void addHeightAdjustment() {
		// For each (y, x) point in the grid
		for (int y = 0; y < TerrainGenerator.getHeight(); y++) {
			for (int x = 0; x < TerrainGenerator.getWidth(); x++) {
				// If the point (y, x) is on the chosen side of the fault line (or on the line)
				if (TerrainGenerator.whichSideOfLine(entryPoint, exitPoint, new int[] { y, x }) * side.value >= 0) {
					// Increase the height value of the point by the chosen height adjustment value
					TerrainGenerator.increaseGridValue(y, x, heightAdjustment);
				}
			}
		}
	}
}
