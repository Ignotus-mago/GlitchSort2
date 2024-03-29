package net.paulhertz.glitchsort;

import processing.core.PApplet;

/**
 * Facilitates the "zigzag" scanning of a square block of pixels with a variable edge dimension set by the user.
 * Zigzag scanning is used in the JPEG compression algorithm, and artifacts caused by it occasionally show up in JPEG errors (glitches).
 * Provides methods for reading (pluck) and writing (plant) from an array of pixels.
 *
 */
class Zigzagger implements PixelScannerINF {
	/** zigzag x coord */
	private int zagx;
	/** zigzag y coord */
	private int zagy;
	/** x coordinates */
	private int[] xcoords;
	/** y coordinates */
	private int[] ycoords;
	/**  */
	private int[] indexMap;
	/** the dimension of an edge of the square block of pixels */
	private int d;
	/** the total number of pixels in the block */
	private int n;
	/** counter variable f = d + d - 1: number of diagonals in zigzag */
	private int f;
	/** the verbose */
	public boolean verbose = false;

	/**
	 * @param order   the number of pixels on an edge of the scan block
	 */
	public Zigzagger(int order) {
		d = order;
		f = d + d - 1;
		n = d * d;
		xcoords = new int[n];
		ycoords = new int[n];
		indexMap = new int[n];
		generateCoords();
	}

	/**
	 * Generates coordinates of a block of pixels of specified dimensions, offset from (0,0).
	 */
	private void generateCoords() {
		int p = 0;
		int n = 0;
		int index = 0;
		for (int t = 0; t < f; t++) {
			if (t < d) {
				n++;
				if (n % 2 == 0) {
					for (int i = 0; i < n; i++) {
						zagx = n - i - 1;
						zagy = i;
						xcoords[p] = zagx;
						ycoords[p] = zagy;
						index = zagx + d * zagy;
						indexMap[index] = p;
						p++;
					}
				}
				else {
					for (int i = 0; i < n; i++) {
						zagx = i;
						zagy = n - i - 1;
						xcoords[p] = zagx;
						ycoords[p] = zagy;
						index = zagx + d * zagy;
						indexMap[index] = p;
						p++;
					}
				}
			} // end if t < d
			else {
				n--;
				if (n % 2 == 0) {
					for (int i = 0; i < n; i++) {
						zagx = d - i - 1;
						zagy = i + d - n;
						xcoords[p] = zagx ;
						ycoords[p] = zagy;
						index = zagx + d * zagy;
						indexMap[index] = p;
						p++;
					}
				}
				else {
					for (int i = 0; i < n; i++) {
						zagx = i + d - n;
						zagy = d - i - 1;
						xcoords[p] = zagx;
						ycoords[p] = zagy;
						index = zagx + d * zagy;
						indexMap[index] = p;
						p++;
					}
				}
			} // end if t > d
		}
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
	
	/**  */
	public void swapXY() {
		
	}
	
	/**
	 * @param pix   an array of pixels
	 * @param w     width of the image represented by the array of pixels
	 * @param h     height of the image represented by the array of pixels
	 * @param x     x-coordinate of the location in the image to scan
	 * @param y     y-coordinate of the location in the image to scan
	 * @return      an array in the order determined by the zigzag scan
	 */
	public int[] pluck(int[] pix, int w, int h, int x, int y) {
		int len = n;
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
	 * @param sprout   an array of n = d * d pixels to write to the array of pixels
	 * @param w        width of the image represented by the array of pixels
	 * @param h        height of the image represented by the array of pixels
	 * @param x        x-coordinate of the location in the image to write to
	 * @param y        y-coordinate of the location in the image to write to
	 */
	public void plant(int[] pix, int[] sprout, int w, int h, int x, int y) {
		for (int i = 0; i < n; i++) {
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
		buf.append("Zigzag order: "+ this.d +"\n  ");
		for (int i = 0; i < xcoords.length; i++) {
			buf.append("("+ xcoords[i] +", "+ ycoords[i] +") ");
		}
		buf.append("\n");
		return buf.toString();
	}
	
	public int getDepth() {
		return -1;
	}
	
	public int getBlockWidth() {
		return d;
	}
	
	/** return the number of pixel values mapped in this PixelScannerINF instance */
	public int getSize() {
		return indexMap.length;
	}
  
	/** return the index of a point in the mapped representation */ 
	public int lookup(int x, int y) {
		return indexMap[x + d * y];
	}
  
	/** return the x coordinate at a specified index in the map */
	public int xcoord(int pos) {
		return xcoords[pos];
	}
  
	/** return the y coordinate at a specified index in the map */
	public int ycoord(int pos) {
		return ycoords[pos];
	}

	
}
