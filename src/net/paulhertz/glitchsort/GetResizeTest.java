package net.paulhertz.glitchsort;

import java.awt.Container;
import java.awt.Frame;
import java.io.File;

import processing.core.*;

// Test for possible bug in PImage.get method.
// Bug with alpha channel was fixed in a later release of processing.
// by Paul Hertz, 2014
// http://paulhertz.net/

public class GetResizeTest extends PApplet {
	/** the primary image to display and glitch */
	PImage img;
	/** a version of the image scaled to fit the screen dimensions */
	PImage fitImg;
	/** true if image should fit screen, otherwise false */
	boolean isFitToScreen = false;
	/** maximum width for the display window */
	int maxWindowWidth;
	/** maximum height for the display window */
	int maxWindowHeight;
	/** width of the image when scaled to fit the display window */
	int scaledWidth;
	/** height of the image when scaled to fit the display window */
	int scaledHeight;
	/** image width, set on loading image */
	int imageWidth;
	/** image height, set on loading image */
	int imageHeight;
	/** reference to the frame (display window) */
	Frame myFrame;
	/** current width of the frame (display window) */
	int frameWidth;
	/** current height of the frame (display window) */
	int frameHeight;
	/** the selected file */
	File displayFile;
	/** toggle use of get() or of copyImagePixels() */
	boolean isUseGet = true;
	/** display verbose messages if verbose == true */
	boolean verbose = false;



	public void setup() {
		println("Display: "+ displayWidth +", "+ displayHeight);
		size(640, 480);
		smooth();
		// max window width is the screen width
		maxWindowWidth = displayWidth;
		// leave window height some room for title bar, etc.
		maxWindowHeight = displayHeight - 56;
		// set values for scaledWidth and scaledHeight
		scaledWidth = width;
		scaledHeight = height;
		// image to display
		img = createImage(width, height, ARGB);
		chooseFile();
		// Processing initializes the frame and hands it to you in the "frame" field.
		// Eclipse does things differently. Use findFrame method to get the frame in Eclipse.
		myFrame = findFrame();
		myFrame.setResizable(true);
		// the first time around, window won't be resized, a reload should resize it
		if (null != displayFile) {
			loadFile();
			if (isFitToScreen) fitPixels(true);
		}
		printHelp();
	}


	/**
	 * @return   Frame where Processing draws, useful method in Eclipse
	 */
	public Frame findFrame() {
		Container f = this.getParent();
		while (!(f instanceof Frame) && f!=null)
			f = f.getParent();
		return (Frame) f;
	}


	public void printHelp() {
		println("Test for possible bug in PImage.get method.");
		println("Press 'F' to toggle image size to screen.");
		println("Press 'G' to toggle use of Pimage.get or arrayCopy.");
		println("Press 'O' to open a new file.");
	}


	public void draw() {
		if (isFitToScreen) {
			image(fitImg, 0, 0);
		}
		else {
			background(255);
			image(img, 0, 0);
		}
	}


	public void keyPressed() {
		if (key == 'f' || key == 'F') {
			fitPixels(!isFitToScreen);
		}
		else if (key == 'o' || key == 'O') {
			chooseFile();
		}
		else if (key == 'g' || key == 'G') {
			isUseGet = !isUseGet;
			if (isUseGet) println("Using PImage.get method.");
			else println("Using arrayCopy method");
		}
	}

	// test scaling of mouse location
	public void mousePressed() {
		// we could use integer math alone for speed, but numbrs ar turncated rather than rounded
		int scaledX = Math.round((mouseX * imageWidth)/(float)this.scaledWidth);
		int scaledY = Math.round((mouseY * imageHeight)/(float)this.scaledHeight);
		println("x =", mouseX, ", scaledX =", scaledX, "-- y =", mouseY, ", scaledY =", scaledY);
	}


	/**
	 * Copy an image pixel by pixel, test code to work around problems in Processing 2.1 with PImage.get.
	 * @param image   image to copy
	 * @return        a copy of the image submitted
	 */
	public PImage copyImagePixels(PImage image) {
		int h = image.height;
		int w = image.width;
		PImage newImage = createImage(w, h, ARGB);
		newImage.loadPixels();
		arrayCopy(image.pixels, newImage.pixels);
		newImage.updatePixels();
		return newImage;
	}


	/**
	 * Creates a Processing ARGB color from r, g, b, and alpha channel values. Note the order
	 * of arguments, the same as the Processing color(value1, value2, value3, alpha) method. 
	 * @param r   red component 0..255
	 * @param g   green component 0..255
	 * @param b   blue component 0..255
	 * @param a   alpha component 0..255
	 * @return    a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static int composeColor(int r, int g, int b, int a) {
		return a << 24 | r << 16 | g << 8 | b;
	}


	/**
	 * Breaks a Processing color into R, G and B values in an array.
	 * @param argb   a Processing color as a 32-bit integer 
	 * @return       an array of integers in the intRange 0..255 for 3 primary color components: {R, G, B}
	 */
	public static int[] rgbComponents(int argb) {
		int[] comp = new int[3];
		comp[0] = (argb >> 16) & 0xFF;  // Faster way of getting red(argb)
		comp[1] = (argb >> 8) & 0xFF;   // Faster way of getting green(argb)
		comp[2] = argb & 0xFF;          // Faster way of getting blue(argb)
		return comp;
	}

	/**
	 * Returns alpha channel value of a color.
	 * @param argb   a Processing color as a 32-bit integer 
	 * @return       an int for alpha channel
	 */
	public static int alphaComponent(int argb) {
		return (argb >> 24);
	}

	/**
	 * Copy an image pixel by pixel, test code to work around problems in Processing 2.1 with PImage.get.
	 * @param image   image to copy
	 * @return        the image submitted with apha channel set to desired value
	 */
	public PImage loadImageAlpha(PImage image, int alpha) {
		int i = 0;
		image.loadPixels();
		for (i = 0; i < image.pixels.length; i++) {
			// int[] rgb = rgbComponents(image.pixels[i]);
			// image.pixels[i] = alpha << 24 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
			image.pixels[i] = alpha << 24 | image.pixels[i];
		}
		image.updatePixels();
		return image;
	}


	/**
	 * Fits images that are too big for the screen to the screen, or displays as much of a large image 
	 * as fits the screen if every pixel is displayed. There is still some goofiness in getting the whole
	 * image to display--bottom edge gets hidden by the window. It would be good to have a scrolling window.
	 * 
	 * @param fitToScreen   true if image should be fit to screen, false if every pixel should displayed
	 */
	public void fitPixels(boolean fitToScreen) {
		if (fitToScreen) {
			fitImg = createImage(img.width, img.height, ARGB);
			scaledWidth = fitImg.width;
			scaledHeight = fitImg.height;
			// TODO this is where the fun begins
			if (isUseGet) {
				fitImg.loadPixels();
				fitImg = img.get();
				fitImg.updatePixels();
			}
			else {
				fitImg = copyImagePixels(img);
			}
			// We calculate the relative proportions of window and image. 
			// ratio of the window height to the window width
			float windowRatio = maxWindowHeight/(float)maxWindowWidth;
			// ratio of the image height to the image width
			float imageRatio = fitImg.height/(float)fitImg.width;
			if (verbose) {
				println("maxWindowWidth "+ maxWindowWidth +", maxWindowHeight "+ maxWindowHeight +", screen ratio "+ windowRatio);
				println("image width "+ fitImg.width +", image height "+ fitImg.height +", image ratio "+ imageRatio);
			}
			if (imageRatio > windowRatio) {
				// image is proportionally taller than the display window, 
				// so scale image height to fit the window height
				scaledHeight = maxWindowHeight;
				// and scale image width by window height divided by image height
				scaledWidth = Math.round(fitImg.width * (maxWindowHeight / (float)fitImg.height));
			}
			else {
				// image is proportionally equal to or wider than the display window, 
				// so scale image width to fit the window width
				scaledWidth = maxWindowWidth;
				// and scale image height by window width divided by image width
				scaledHeight = Math.round(fitImg.height * (maxWindowWidth / (float)fitImg.width));
			}
			fitImg.resize(scaledWidth, scaledHeight);
			// a workaround
			loadImageAlpha(fitImg, 255);
			// 48 pixels compensate for window decorations such as bar, etc.
			if (null != myFrame) myFrame.setSize(scaledWidth, scaledHeight + 48);
		}
		else {
			scaledWidth = img.width;
			scaledHeight = img.height;
			if (null != myFrame) {
				frameWidth = scaledWidth <= maxWindowWidth ? scaledWidth : maxWindowWidth;
				frameHeight = scaledHeight <= maxWindowHeight ? scaledHeight : maxWindowHeight;
				myFrame.setSize(frameWidth, frameHeight + 38);
			}
		}
		if (verbose) println("scaledWidth = "+ scaledWidth +", scaledHeight = "+ scaledHeight +", frameWidth = "+ frameWidth +", frameHeight = "+ frameHeight);
		isFitToScreen = fitToScreen;
	}


	/**
	 * @return   true if a file reference was successfully returned from the file dialogue, false otherwise
	 */
	public void chooseFile() {
		selectInput("Choose an image file.", "displayFileSelected");
	}

	public void displayFileSelected(File selectedFile) {
		File oldFile = displayFile;
		if (null != selectedFile && oldFile != selectedFile) {
			noLoop();
			displayFile = selectedFile;
			loadFile();
			if (isFitToScreen) fitPixels(true);
			loop();
		}
		else {
			println("No file was selected");
		}
	}


	/**
	 * loads a file into variable img.
	 */
	public void loadFile() {
		println("\nselected file "+ displayFile.getAbsolutePath());
		img = loadImage(displayFile.getAbsolutePath());
		imageWidth = img.width;
		imageHeight = img.height;
		fitPixels(isFitToScreen);
		println("image width "+ imageWidth +", image height "+ imageHeight);
	}

}
