/**
 * 
 */
package net.paulhertz.glitchsort;

import java.util.Arrays;
import processing.core.*;
import net.paulhertz.aifile.*;

/**
 * Implement Hilbert curve scanning of a square block of pixels with an edge dimension set by the user.
 * Hilbert scanning provides a high degree of local coherence. Edge dimension must be a power of 2.
 * Provides methods for reading (pluck) and writing (plant) from an array of pixels.
 *
 */
class HilbertScanner implements PixelScannerINF {
	/** x coordinates */
	private int[] xcoords;
	/** y coordinates */
	private int[] ycoords;
	/** flipped x coordinates */
	private int[] flipXcoords;
	/** flipped y coordinates */
	private int[] flipYcoords;
	/** the depth of recursion of the Hilbert curve */
	private int depth = 1;
	/** the dimension of an edge of the square block of pixels */
	private int d;
	/** the verbose */
	private boolean verbose = false;
	

	/**
	 * @param depth   the depth of recursion that determines the number of pixels on an edge of the scan block
	 */
	public HilbertScanner(int depth) {
		this.depth = depth;
		d = (int) Math.round(Math.pow(2, depth));
		generateCoords();
		/*
		flipXcoords = Arrays.copyOf(xcoords, xcoords.length);
		flipYcoords = Arrays.copyOf(ycoords, ycoords.length);
		int m = d - 1;
		for (int i = 0; i < flipXcoords.length; i++) {
			flipXcoords[i] = m - flipXcoords[i];
		}
		for (int i = 0; i < flipYcoords.length; i++) {
			flipYcoords[i] = m - flipYcoords[i];
		}
		*/
	}

	/**
	 * Generates coordinates of a block of pixels of specified dimensions, offset from (0,0).
	 */
	private void generateCoords() {
		StringBuffer hilb = hilbert(depth);
		int step = 1;
		Turtle turtle = new Turtle();
		turtle.setTurtleX(0);
		turtle.setTurtleY(0);
		// PApplet.println("-------- d * d = "+ d * d);
		xcoords = new int[d * d];
		ycoords = new int[d * d];
		int pos = 0;
		xcoords[pos] = 0;
		ycoords[pos++] = 0;
		for (int i = 0; i < hilb.length(); i++) {
			char ch = hilb.charAt(i);
			switch(ch) {
			case '+' : {
				turtle.turn(PApplet.radians(90));
				break;
			}
			case '-' : {
				turtle.turn(PApplet.radians(-90));
				break;
			}
			case 'F' : {
				turtle.move(step);
				xcoords[pos] = (int) Math.round(turtle.getTurtleX());
				ycoords[pos++] = (int) Math.round(turtle.getTurtleY());
				break;
			}
			default: {
				
			}
			}
		}
	}
	
	/**
	 * Encode strings representing a Hilbert curve to supplied depth.
	 * To draw the actual curve, ignore the R and L symbols
	 *     + : 90 degrees CW
	 *     - : 90 degrees CCW
	 *     F : forward (n) units
	 */
	public StringBuffer hilbert(int depth) {
		Lindenmeyer lind = new Lindenmeyer();
		lind.put('L', "+RF-LFL-FR+");
		lind.put('R', "-LF+RFR+FL-");
		StringBuffer buf = new StringBuffer();
		lind.expandString("L", depth, buf);
		if (verbose && null != buf) System.out.println("Hibert L-system at depth "+ d +"\n"+ buf.toString());
		return buf;
	}
	
	
	public void flipX() {
		int m = d - 1;
		for (int i = 0; i < xcoords.length; i++) {
			xcoords[i] = m - xcoords[i];
		}
	}
	
	public void flipY() {
		int m = d - 1;
		for (int i = 0; i < ycoords.length; i++) {
			ycoords[i] = m - ycoords[i];
		}
	}
	
	
	/**
	 * @param pix   an array of pixels
	 * @param w     width of the image represented by the array of pixels
	 * @param h     height of the image represented by the array of pixels
	 * @param x     x-coordinate of the location in the image to scan
	 * @param y     y-coordinate of the location in the image to scan
	 * @return      an array in the order determined by the Hilbert scan
	 */
	public int[] pluck(int[] pix, int w, int h, int x, int y) {
		int len = d * d;
		int[] out = new int[len];
		for (int i = 0; i < len; i++) {
			int p = (y + ycoords[i]) * w + (x) + xcoords[i];
			if (verbose) PApplet.println("x = "+ x +", y = "+ y +", i = "+ i +", p = "+ p +", zigzag = ("+ xcoords[i] +", "+ ycoords[i] +")");
			out[i] = pix[p];
		}
		return out;
	}
	
	/**
	 * @param pix      an array of pixels
	 * @param sprout   an array of d * d pixels to write to the array of pixels
	 * @param w        width of the image represented by the array of pixels
	 * @param h        height of the image represented by the array of pixels
	 * @param x        x-coordinate of the location in the image to write to
	 * @param y        y-coordinate of the location in the image to write to
	 */
	public void plant(int[] pix, int[] sprout, int w, int h, int x, int y) {
		for (int i = 0; i < d * d; i++) {
 			int p = (y + ycoords[i]) * w + (x) + xcoords[i];
			pix[p] = sprout[i];
		}
	}
	
	/* (non-Javadoc)
	 * returns a list of coordinate points that define a zigzag scan of order d.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Hilbert order: "+ this.d +"\n  ");
		for (int i = 0; i < xcoords.length; i++) {
			buf.append("("+ xcoords[i] +", "+ ycoords[i] +") ");
		}
		buf.append("\n");
		return buf.toString();
	}

	public int getDepth() {
		return depth;
	}

	public int getBlockWidth() {
		return d;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	/**
	 * Rotates an array of ints left by d values. Uses efficient "Three Rotation" algorithm.
	 * @param arr   array of ints to rotate
	 * @param d     number of elements to shift
	 */
	public void rotateLeft(int[] arr, int d) {
		d = d % arr.length;
		reverseArray(arr, 0, d - 1);
		reverseArray(arr, d, arr.length - 1);
		reverseArray(arr, 0, arr.length - 1);
	}
	
	/**
	 * Reverses an arbitrary subset of an array.
	 * @param arr   array to modify
	 * @param l     left bound of subset to reverse
	 * @param r     right bound of subset to reverse
	 */
	public void reverseArray(int[] arr, int l, int r) {
		int temp;
		while (l < r) {
			temp = arr[l];
			arr[l] = arr[r];
			arr[r] = temp;
			l++;
			r--;
		}
	}

	public void rotateXLeft(int offset) {
		rotateLeft(xcoords, offset);
	}

	public void rotateYLeft(int offset) {
		rotateLeft(ycoords, offset);
	}

}
