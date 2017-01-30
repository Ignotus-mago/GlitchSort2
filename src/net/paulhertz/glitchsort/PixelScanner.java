package net.paulhertz.glitchsort;

import static net.paulhertz.glitchsort.GlitchConstants.*;

public interface PixelScanner {
	
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
	
}
