package net.paulhertz.glitchsort;

import static net.paulhertz.glitchsort.GlitchConstants.*;

/**
 * Interface for classes that scan the screen coordinates of an array of pixels 
 * in orders other than the usual left to right, top to bottom x,y scanlines.
 * Typically I use the scans to create one-dimensional arrays that are 
 * processed (perhaps as audio signals) and written back to the screen. 
 *
 */
public interface PixelScannerINF {
	
	/** flip the order of the x coordinates */
	abstract void flipX();
	
	/** flip the order of the y coordinates */
	abstract void flipY();
	
	/** swap the x and y coordinates in the map */
	abstract void swapXY();
	
	
	/**
	 * @param pix   an array of pixels
	 * @param w     width of the image represented by the array of pixels
	 * @param h     height of the image represented by the array of pixels
	 * @param x     x-coordinate of the location in the image to scan
	 * @param y     y-coordinate of the location in the image to scan
	 * @return      an array in the order determined by the Hilbert scan
	 */
	abstract int[] pluck(int[] pix, int w, int h, int x, int y);
	
	/**
	 * @param pix      an array of pixels
	 * @param sprout   an array of d * d pixels to write to the array of pixels
	 * @param w        width of the image represented by the array of pixels
	 * @param h        height of the image represented by the array of pixels
	 * @param x        x-coordinate of the location in the image to write to
	 * @param y        y-coordinate of the location in the image to write to
	 */
	abstract void plant(int[] pix, int[] sprout, int w, int h, int x, int y);
	
	/** return a string representation of our data, possibly partial */
	abstract String toString();
	  
	/** return a number representing recursion depth. If not applicable, return -1 */
	abstract int getDepth();
	  
	/** return the width of the pixel array mapped in this PixelScannerINF instance */
	abstract int getBlockWidth();  
	  
	/** return the number of pixel values mapped in this PixelScannerINF instance */
	abstract int getSize();
  
	/** return the index of a point in the mapped representation */ 
	abstract int lookup(int x, int y);
  
	/** return the x coordinate at a specified index in the map */
	abstract int xcoord(int pos);
  
	/** return the y coordinate at a specified index in the map */
	abstract int ycoord(int pos);
	
	/*
	 * 
	//
	// template for processing PixelScanner arrays
	//
	  public void processScan(int val) {
    	int order = (int) Math.sqrt(statBufferSize);
    	this.statFFTBlockWidth = order;
    	PixelScanner zz;
    	if (isHilbertScan) {
    		int depth = (int) (Math.log(order)/Math.log(2));
    		zz = new HilbertScanner(depth);
    		println("Hilbert depth = "+ depth);
    	}
    	else {
    		zz = new Zigzagger(order);
    		println("Zigzag order = "+ order);
    	}
    	int dw = (img.width / order);
    	int dh = (img.height / order);
    	int w = dw * order;
    	int h = dh * order;
    	int ow = (img.width - w) / 2;
    	int oh = (img.height - h) / 2;
		  backup();
		  img.loadPixels();
			// processing loop
    	for (int y = 0; y < dh; y++) {
    		for (int x = 0; x < dw; x++) {
    			int mx = x * order + ow;
    			int my = y * order + oh;
    			int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
    			// do stuff to pix here
    			
    			zz.plant(img.pixels, pix, img.width, img.height, mx, my);
    		}
    	}
		  // write to the image
		  img.updatePixels();
		  fitPixels(isFitToScreen, false);
	  }
	 * 
	 */
	
	
}
