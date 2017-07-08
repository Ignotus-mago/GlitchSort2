package net.paulhertz.glitchsort;

import static net.paulhertz.glitchsort.GlitchConstants.*;

public interface PixelScannerINF {
	
	abstract void flipX();
	
	abstract void flipY();
	
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
	
	abstract String toString();
	
	abstract int getDepth();
	
	abstract int getBlockWidth();
	
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
