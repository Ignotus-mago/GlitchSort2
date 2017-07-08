package net.paulhertz.glitchsort;
/*
 * Copyright (c) 2012, Paul Hertz This code is free software; you can
 * redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version
 * 3.0 of the License, or (at your option) any later version.
 * http://www.gnu.org/licenses/lgpl.html This software is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details. You should have
 * received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301, USA
 * 
 * @author		Paul Hertz
 * @created		July 13, 2012 
 * @modified	June 6, 2013
 * @version  	1.0b10 for Processing 2.0
 *				- various minor changes. The biggest change is that this version runs in Processing 2.0
 *				- Same old manual
 *				- save (s) and save a copy (S) commands, revert to saved (r) and revert to original (R)
 *				- open file (o) and load file to snapshot buffer (O)
 *				- rotate right (t) and rotate left (T)
 *				- added zigzag percent number box, sets percent of blocks that get zigzag sorted on each pass
 *				- ***** requires Processing 2.0, processing.org  *****
 *				- ***** requires ControlP5 2.0.4, www.sojamo.de/code ***** 
 * version		1.0b9 for Processing 1.5.1
 *				- added '_' (underscore) key command to turn 4 times repeating lastCommand
 *				- fixed dragging to work both when control panel is visible and when it is not, 
 *				  without shift key--covers most cases, but doesn't track collapsed panels
 *				- added scaledLowPass method, low pass filter each RGB channel with different 
 *				  FFT block size (64, 32, 16), component order depends on current Component Sorting Order setting
 *                Currently only triggered with ')' key command, works best when pixel dimension are multiples of 64.
 *				- added ZigzagStyle enum and zigzagStyle variable to set zigzag sorting to random angles, aligned angles, 
 *				  or angles permuted in blocks of four
 *				- added global variables for control panel location and width
 *				- added flipX and flipY methods to Zigzagger to handle changing zigzag angle
 *				- changed default glitchSettings of statistical FFT
 *				- various small fixes 
 * version 	1.0b8a
 * 				- changes from last version
 *				- fixed denoise command to include edge and corner pixels
 *				- added lastCommand variable, tracks last key command in "gl<>9kjdGLKJD" 
 * This version has a new reference manual for version 1.0b8. 
 * If it wasn't included, see http://paulhertz.net/factory/2012/08/glitchsort2/.
 * 				
 *
 */ 

// uses pixel sorting to imitate wild glitches
// by Paul Hertz, 2012
// http://paulhertz.net/
// updates: http://paulhertz.net/factory/2012/08/glitchsort2/
// requires: 
//   Processing: http://processing.org/
//   ControlP5 library for Processing: http://www.sojamo.de/libraries/controlP5/


// ISSUES
// 0. Processing 2.0 resolved the image memory leak problem: this version of GlitchSort runs in Processing 2.0 (not 1.5.1!).
// 1. Type 'r' (reload) or 'f' fit to screen after loading the first picture to get correct window size. (Processing 1.5.1).
// 2. Using return key to load a file from the file dialog sometimes causes application to hang. Double-click works.
// 3. The display window may still hide a row or two of pixels, though I think I have 
//    fixed this. You can drag it a little bigger.
// 4. Audify ('/' and '\' is new and still kludgy, but the bugs that would cause a crash in 
//   1.0b7 pre-release "c" seem to have been fixed.
// 5. The Minim library routines I use for FFT are now deprecated, but functional. 
// 6. There must be other issues. 


import java.awt.Container;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.*;
import java.awt.Toolkit;
import java.awt.event.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import processing.core.*;
import ddf.minim.analysis.*;
import ddf.minim.*;
import controlP5.*;
import controlP5.Controller;
import net.paulhertz.aifile.*;
import net.paulhertz.util.Permutator;
// static import statement (Java 1.5) allows us to use unqualified constant names
import static net.paulhertz.glitchsort.GlitchConstants.*;


// uses pixel sorting, quantization, FFT, etc. to imitate wild glitches, audifies pixels
// by Paul Hertz, 2012
// http://paulhertz.net/

// press spacebar to show or hide the control panel
// option-drag on control panel bar to move control panel
// shift-drag on image to pan large image (no shift key needed if control panel is hidden)
// press 'f' to toggle fit image to screen
// press 'o' to open a file
// press 'O' to load a file to the snapshot buffer
// press 's' to save display to a timestamped .png file
// press 'S' to save display to a timestamped .png file as a copy
// press 'r' to revert to the most recently saved version of the file
// press 'R' to revert to the oldest version of the file
// press 't' to turn the image 90 clockwise
// press 'g' to sort the pixels (glitch)
// press 'l' to sort in zigzag-scanned blocks
// press 'z' to undo the last action
// press '1' to select quick sort
// press '2' to select shell sort 
// press '3' to select bubble sort
// press '4' to select insert sort
// press 'a' to change sort order to ascending or descending
// press 'b' to toggle random breaks in sorters
// press 'x' to toggle color channel swapping (glitchy!)
// press 'c' to step through the color channels swaps
// press '+' or '-' to step through color component orderings used for sorting
// press 'y' to turn glitch cycling on and off (for glitch steps > 1)
// press '[' or ']' to decrease or increase glitch steps
// press '{' or '}' to cycle through Shell sort glitchSettings
// press 'd' to degrade the image with low quality JPEG compression
// press UP or DOWN arrow keys to change degrade quality
// press 'p' to reduce (quantize) the color palette of the image
// press LEFT or RIGHT arrow keys to change color quantization
// press '<' to shift selected color channel one pixel left
// press '>' to shift selected color channel one pixel right
// press '9' to denoise image with a median filter
// press 'n' to grab a snapshot of the current image
// press 'u' to load the most recent snapshot
// press 'm' to munge the current image with the most recent snapshot and the undo buffer
// press 'j' to apply equalizer FFT
// press 'k' to apply statistical FFT
// press '/' to turn audify on and execute commands on a single block of pixels
// press '\' to turn audify off
// press '_' to turn 90 degrees and execute last command, four times
// press ')' to run scaled low pass filter
// press 'v' to turn verbose output on and off
// press 'h' to show help message


/* TODO  teh list
 * 
 * sort, fft, munge, jpeg, audio/realtime control panels
 * save, save increment, save as, save copy
 *   increment as check box, default checked
 *   save - first time with dialog, then automatic if increment is on, otherwise replace
 *   save as - dialog, then automatic if increment is on, otherwise replace
 * record and play back commands
 * bug in quick sort
 * bug fix for sorting - blocks or lines get skipped, probably an off-by-one error in RangeManager
 * a better color quantization method (median cut)
 * "real-time" compositing preview
 * break out commands into command pattern class that can be effectively journaled
 * create a genetic algorithm driven version
 * sorting breaks at user-selected pixel value
 * interactive channel-shifting
 * interactive compositing (munge)
 * more buffers
 * save and load FFT glitchSettings
 * termites
 * performance interface
 *     load image list and step through it (with programmable Markov chaining)
 *     multiple buffers
 *     pass audio to another app with osc
 *     save audio to file (dependent on FFT block size)
 *     perhaps: use new version of minim library
 *     "Stick" audio sources to locations, select multiple blocks
 *     real time glitch from camera
 * 
 * 
 */

/**
 * @author Paul Hertz
 *
 */
@SuppressWarnings("serial")
public class GlitchSort extends PApplet {
	/** the current component order to use for sorting */
	CompOrder compOrder;
	/** handy variable for stepping through the CompOrder enum */
	int compOrderIndex = 0;
	/** the current channel swapping scheme for glitchy color fx */
	SwapChannel swap = SwapChannel.BB;
	/** ordering of pixels in subarray to be sorted, relative to source array. Not yet in use. */
	public static enum SortFormat {ROW, SQUARE, DIAGONAL;}
	/** current format of pixels to use in sorting a subarray of pixels from an image */
	SortFormat sortFormat = SortFormat.ROW;
	/** a SortSelector manages the selection of different sorting algorithms */
	SortSelector sortTool;
	
	// -------- file i/o variables -------- //
	/** the most recently saved version of the selected file */
	File displayFile;
	/** the original file, selected with the 'open' command */
	File originalFile;
	/** path to the file */
	String filePath;
	/** short name of the file */
	String fileBaseName;
	/** file count for filenames in sequence */
	int fileCount = 0;
	/** timestamp for filenames */
	String timestamp; 
	/** flag for the first time saving a file ina session */
	boolean isFirstSave = true;
	/** flag for new file, after open or save as */
	boolean isNewFile = true;
	
	// -------- image variables -------- //
	/** the primary image to display and glitch */
	PImage img;
	/** an image buffer used to undo the most recent operation, usually contains an earlier version of the primary image */
	PImage bakImg;
	/** a version of the image scaled to fit the screen dimensions, for display only */
	PImage fitImg;
	/** a snapshot of the primary image, used as an extended undo buffer and for the "munge" operation */
	PImage snapImg;
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
	/** current width of the frame (display window) */
	int frameWidth;
	/** current height of the frame (display window) */
	int frameHeight;
	/** flags when a large image can be dragged (translated) in the display window */
	boolean isDragImage = false;
	/** translation on the x-axis for an image larger than the display window */
	int transX = 0;
	/** translation on the y-axis for an image larger than the display window */
	int transY = 0;
	/** reference to the frame (display window) */
	Frame myFrame; 
	
	// --------- state variables for commands -------- //
	/** true if sorting is interrupted, causing glitches. if false, horizontal lines of pixels are completely sorted */
	boolean randomBreak = true;
	/** true if lots of output to the monitor is desired (useful for debugging) */
	boolean verbose = false;
	/** true if pixels values are sorted in ascending numeric order */
	boolean isAscendingSort = false;
	/** true if pixels that are exchanged in sorting swap a pair of channels, creating color artifacts */
	boolean isSwapChannels = false;
	float swapWeight = 1.0f;
	/** an array of row numbers for the horizontal lines of pixels, used when sorting */
	int[] rowNums;
	/** the current row of pixels being sorted */
	int row = 0;	
	/** the current column of pixels being sorted */
	int column = 0;	
	/** 
	 * a value from 1..999 that determines how often a sorting method is interrupted.  
	 * In general, higher values decrease the probability of interruption, and so do more sorting. 
	 * However, each sorting method behaves differently. Quick sort is sensitive from 1..999. 
	 * Shell sort seems to do best from 900..999; lower values result in sorting only at the the image edge.
	 * Bubble sort does well from 990..999: at 999 it will diffuse the pixels. Insert sort seems 
	 * to be effective from 990..999. 
	 * TODO: create a more intuitive setting for breakpoint, with greater precision where needed.
	 */
	float breakPoint = 500;
	/** number of partitions of pixel rows, (1/glitchSteps * number of rows) rows of pixels 
	    are sorted (glitched) each time the sort command is executed */
	float glitchSteps = 1;
	/** if true, sorting steps are cyclical: rows will be sorted by successive sort commands
	    and the order in which rows are sorted will not be shuffled until all rows are sorted. 
	    if false, the 1/glitchSteps rows are sorted and then the row order is shuffled */
	boolean isCycleGlitch = false;
	/** step value for number of lines to sort at once */
	int lineTick = 0;
	/** integer added to lineTick to determine number of lines to sort at once */
	int lineAdd = 1;
	/** number of lines to sort at once */
	int lineCount = lineTick + lineAdd;
	RangeManager ranger; 
	RangeManager zigzagRanger; 
	/** the JPEG quality setting for degrading the image by saving it as a JPEG and then reloading it */
	float degradeQuality = 0.125f;
	/** the value below which the maximum absolute difference between color channel values induces munging */
	int mungeThreshold = 16;
	/** "munging" compares the current image with the undo buffer. When isMungeInverted is false, 
	 * pixels outside the difference threshold are replaced with corresponding pixels from the snapshot buffer.
	 * When isMungeInverted is true, pixels within the threshold are replaced. */
	boolean isMungeInverted = false;
	/** Color quantizer, for color reduction, using an octree.  */
	ImageColorQuantizer quant;
	/** The number of colors to reduce to, should not exceed 255 limit imposed by octree */
	int colorQuantize = 32;
	// ratio and divisor values for Shell sort, feel free to add your own pairs
	int[] shellParams = {2,3, 2,5, 3,5, 3,7, 3,9, 4,7, 4,9, 5,7, 5,9, 5,11, 8,13};
	// shell params index
	int shellIndex = 8;
	boolean isShiftR = true;
	boolean isShiftG = false;
	boolean isShiftB = false;
	int shift = 1;

	// ASDFPixelSort, not yet implemented
	int blackValue = -10000000;
	int brigthnessValue = 60;
	int whiteValue = -6000000;

	// -------- FFT -------- //
	Minim minim;
	Minim minim2;
	FFT statFFT;
	FFT eqFFT;
	int zigzagBlockWidth = 128;
	int statFFTBlockWidth = 64;
	int eqFFTBlockWidth = 64;
	int statBufferSize;
	// TODO implement separate FFTBuffer for eqch operation
	// right now we use statBufferSize for all fft buffer sizes
	int eqBufferSize;
	// float sampleRate = 44100.0f;
	float sampleRate = 262144.0f; // (1024^2)/4
	public int eqH = 100;
	public float eqMax = 1;
	public float eqMin = -1;
	public float eqScale = 1;
	public float eqGain = 1;
	float[] eq;
	//* array to store average amplitude for each range of bands in averaged FFT */
	double[] binTotals;
	//* array to store band indices in averaged FFT band, has some problems */
	ArrayList<IntRange> bandList;
	//* array to store band low and high frequencies for each averaged FFT band */
	ArrayList<FloatRange> freqList;
	int minBandWidth = 11;
	int bandsPerOctave = 3;
	/** when we call calculateEqBands, the actual number of bands may differ from the internal value  */
	int calculatedBands;
	/** default maximum number of eq bands */
	int eqBands = 33;
	boolean isEqGlitchBrightness = true;
	boolean isEqGlitchHue = false;
	boolean isEqGlitchSaturation = false;
	boolean isEqGlitchRed = false;
	boolean isEqGlitchGreen = false;
	boolean isEqGlitchBlue = false;
	int eqPos = 0;
	boolean isStatGlitchBrightness = true;
	boolean isStatGlitchHue = false;
	boolean isStatGlitchSaturation = false;
	boolean isStatGlitchRed = false;
	boolean isStatGlitchGreen = false;
	boolean isStatGlitchBlue = false;
	/** for statistical FFT, number of standard deviations to the left of mean */
	public float leftBound = -0.25f, defaultLeftBound = leftBound;
	/** for statistical FFT, number of standard deviations to the right of mean */
	public float rightBound = 5.0f, defaultRightBound = rightBound;
	/** factor to multiply values within left and right bounds of mean */
	float boost = 2.0f, defaultBoost = boost;
	/** factor to multiply values outside left and right bounds of mean */
	float cut = 0.5f, defaultCut = cut;
	public boolean isCutLinkedToBoost = false;
	float[] audioBuf;

	// -------- Control Panel -------- //
	ControlP5 controlP5;
	DecimalFormat twoPlaces;
	DecimalFormat noPlaces;
	DecimalFormat fourFrontPlaces;
	List<ControllerInterface<?>> mouseControls;
	// Control Panel Manager handles setup of panels
	ControlPanelManager cpm;
	/** reference to the control panel frame (display window for controls) */
	Frame controlFrame;

	// -------- Audio -------- //
	char lastCommand;
	public GlitchSignal glitchSignal;
	public AudioOutput out;
	boolean audioIsRunning;
	boolean screenNeedsUpdate = false;
	boolean isTrackMouse = true;
	boolean isFollowArrows ;
	boolean isAutoPilot;
	boolean isMuted;
	boolean isFrozen;
	boolean isHidden;
	boolean isPipeToOSC;
	
	// -------- File Tracker -------- //
	
	// -------- ZigZag variables -------- //
	/** maximum dimension of zigzag sorting block */
	int zigzagCeiling = 64;
	/** minimum dimension of zigzag sorting block */
	int zigzagFloor = 8;
	/** percentage of blocks that will be zigzag sorted */
	float zigzagPercent = 100.0f;
	/** default is random orientations for zigzag sorting */
	ZigzagStyle zigzagStyle = ZigzagStyle.RANDOM;
	
	

	
	/** IgnoCodeLib library */
	IgnoCodeLib igno;
	
	/** workaround for Eclipse and Processing differences, MacOS Mavericks issues */
	String projPath = "/Users/paulhz/Developer/workspace/GlitchSort/bin"; 
	static boolean isEclipseIDE = true;
   
	/**
	 * @param args
	 * Entry point used in Eclipse IDE
	 */
	public static void main(String[] args) {
		PApplet.main(new String[] { "--present", "GlitchSort" });
	}

	public void setup() {
		println("Screen: "+ displayWidth +", "+ displayHeight);		
		// println("Display: "+ displayWidth +", "+ displayHeight);
		size(640, 480);
		// we generally don't want to smooth. We want nearest neighbor scaling.
		// Unfortunately, Processing only seems to apply noSmooth to shapes, not to images.
		noSmooth();
		// max window width is the screen width
		maxWindowWidth = displayWidth;
		// leave window height some room for title bar, etc.
		maxWindowHeight = displayHeight - 56;
		// image to display
		img = createImage(width, height, ARGB);
		// image for undo 
		bakImg = createImage(width, height, ARGB);
		// the primary tool for sorting
		sortTool = new SortSelector(this);
		sortTool.setRandomBreak(randomBreak);
		// initial order of color channels for sorting
		compOrder = CompOrder.values()[compOrderIndex];
		// initialize number formatters
		initDecimalFormat();
		initFFT();
	  // initialize ControlP5 and build our control panels
		cpm = setupControlPanel("Control", 300, 480);
		printHelp();
		// TODO include version number here
		println("GlitchSort version 1.0b10, created June 6, 2013, for Processing 2.x");
		// Processing initializes the frame and hands it to you in the "frame" field.
		// Eclipse does things differently. Use findFrame method to get the frame in Eclipse.
		myFrame = findFrame();
		myFrame.setResizable(true);
		myFrame.setTitle("GlitchSort");
		// okay now to open an image file
		chooseFile();
		// the first time around, window won't be resized, a reload should resize it
		revert(false);
		// @NEW formant list
		loadFormantList();
		// initialize timestamp, used in filename
		// timestamp = year() + nf(month(),2) + nf(day(),2) + "-"  + nf(hour(),2) + nf(minute(),2) + nf(second(),2);
		timestamp = getTimestamp();
		// fix output problem with Eclipse and MacOSX Mavericks
		if (!isEclipseIDE) {
			projPath = sketchPath;
		}
		igno = new IgnoCodeLib(this);
		println("Project Path = "+ projPath);
		println("Sketch Path = "+ sketchPath);
	}
	
	/* (non-Javadoc)
	 * @see processing.core.PApplet#stop()
	 * we need this to assure that minim stops on exit
	 */
	public void stop() {
		minim.stop();
		minim2.stop();
		super.stop();
	}
	
	public String getTimestamp() {
		return nf(day(),2) + nf(hour(),2) + nf(minute(),2) + nf(second(),2);
	}
	
	/**
	 * set up the control panels managed by ControlP5 and the ControlPanelManager
	 */
	public ControlPanelManager setupControlPanel(String panelName, int width, int height) {
		// controlP5 = new ControlP5(this);
		cpm = new ControlPanelManager(this, width, height);
		controlFrame = new Frame(panelName);
		controlFrame.add(cpm);
		cpm.init();
		controlFrame.setTitle(panelName);
		controlFrame.setSize(width, height);
	  controlFrame.setLocation(100, 100);
	  controlFrame.setResizable(false);
	  controlFrame.setVisible(true);
	  return cpm;
	}
	
	/**
	 * Sets up FFT buffer at fftBlockWidth * fftBlockWidth, calls calculateEqBands to calculates the number  
	 * of equalizer bands available for current buffer size, initializes eq array.
	 */
	public void initFFT() {
		minim = new Minim(this);
		// we process square blocks of pixels as if they were an audio signal
		statBufferSize = statFFTBlockWidth * statFFTBlockWidth;
		statFFT = new FFT(statBufferSize, sampleRate);
		// we do our own calculation of logarithmic bands
		calculateEqBands();
		// calculateEqBands sets the variable calculatedBands
		eq = new float[calculatedBands];
		java.util.Arrays.fill(eq, 0);
	}
	
	/**
	 * @param newSize   new size for edge of a pixel block, must be a power of 2
	 */
	public void resetFFT(int newSize) {
		statFFTBlockWidth = newSize;
		if (true) println("-- fftBlockWidth = "+ statFFTBlockWidth);
		// reset the slider, usually redundant call, should have no side effects if broadcast is off
		Slider s4 = (Slider) cpm.getControl().getController("setFFTBlockWidth"); 
		s4.setBroadcast(false);
		// TODO correct this calculation DONE
		int powTwo = (int) (Math.log(statFFTBlockWidth)/Math.log(2));
		// TODO, why is the + 1 fudge factor necessary?
		s4.setValue(powTwo + 1);
		s4.setBroadcast(true);
		// redraw the text label for the block size, on our control panel
		Textlabel l11 = (Textlabel) cpm.getControl().getController("blockSizeLabel");
		l11.setText("Block Size = "+ statFFTBlockWidth);
		// we process square blocks of pixels as if they were an audio signal
		statBufferSize = statFFTBlockWidth * statFFTBlockWidth;
		statFFT = new FFT(statBufferSize, sampleRate);
		println("  fft timesize = "+ statFFT.timeSize());
		// we do our own calculation of logarithmic bands
		calculateEqBands();
		// calculateEqBnds sets the variable calculatedBands for the new size of pixel block
		eq = new float[calculatedBands];
		java.util.Arrays.fill(eq, 0);
		float[] bins = cpm.getEqualizerValues();
		for (int i = 0; i < eq.length; i++) {
			eq[i] = bins[i];
		}
		// create a new equalizer on the FFT control panel
		// setupEqualizer(eqPos, eqH, eqMax, eqMin);
		cpm.showEqualizerBands();
	}
	
	public void setupFormants() {
		resetFFT(1024);
		setLink(true, false);
		setEqChan(false, false, false, true, true, true, false);
		setStatChan(false, false, false, true, true, true, false);
		int newSize = (int) (Math.log(1024)/Math.log(2));
		println("setup formants, new size: "+	newSize);
	}
	
	/**
	 * initializes the zero place and two place decimal number formatters
	 */
	public void initDecimalFormat() {
		// DecimalFormat sets formatting conventions from the local system, unless we tell it otherwise.
		// make sure we use "." for decimal separator, as in US, not a comma, as in many other countries 
		Locale loc = Locale.US;
		DecimalFormatSymbols dfSymbols = new DecimalFormatSymbols(loc);
		dfSymbols.setDecimalSeparator('.');
		twoPlaces = new DecimalFormat("0.00", dfSymbols);
		noPlaces = new DecimalFormat("00", dfSymbols);
		fourFrontPlaces = new DecimalFormat("0000", dfSymbols);
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
	
	/**
	 * Prints help message to the console
	 */
	public void printHelp() {
		println("press spacebar to show or hide the control panel");
		println("option-drag on control panel bar to move control panel");
		println("shift-drag on image to pan large image (no shift key needed if control panel is hidden)");
		println("press 'f' to toggle fit image to screen");
		println("press 'o' to open a file");
		println("press 'O' to load a file to the snapshot buffer");
		println("press 's' to save display to a timestamped .png file");
		println("press 'S' to save display to a timestamped .png file as a copy");
		println("press 'r' to revert to the most recently saved version of the file");
		println("press 'R' to revert to the oldest version of the file");
		println("press 't' to turn the image 90 clockwise");
		println("press 'g' to sort the pixels (glitch)");
		println("press 'l' to sort in zigzag-scanned blocks");
		println("press 'z' to undo the last action");
		println("press '1' to select quick sort");
		println("press '2' to select shell sort");
		println("press '3' to select bubble sort");
		println("press '4' to select insert sort");
		println("press 'a' to change sort order to ascending or descending");
		println("press 'b' to toggle random breaks in sorters");
		println("press 'x' to toggle color channel swapping (glitchy!)");
		println("press 'c' to step through the color channels swaps");
		println("press '+' or '-' to step through color component orderings used for sorting");
		println("press 'y' to turn glitch cycling on and off (for glitch steps > 1)");
		println("press '[' or ']' to decrease or increase glitch steps");
		println("press '{' or '}' to cycle through Shell sort glitchSettings");
		println("press 'd' to degrade the image with low quality JPEG compression");
		println("press UP or DOWN arrow keys to change degrade quality");
		println("press 'p' to reduce (quantize) the color palette of the image");
		println("press LEFT or RIGHT arrow keys to change color quantization");
		println("press '<' to shift selected color channel one pixel left");
		println("press '>' to shift selected color channel one pixel right");
		println("press ',' to increment number of lines sorted at once");
		println("press '.' to decrement number of lines sorted at once");
		println("press '9' to denoise image with a median filter");
		println("press 'n' to grab a snapshot of the current image");
		println("press 'u' to load the most recent snapshot");
		println("press 'm' to munge the current image with the most recent snapshot and the undo buffer");
		println("press 'j' to apply equalizer FFT");
		println("press 'k' to apply statistical FFT");
		println("press '/' to turn audify on and execute commands on a single block of pixels");
		println("press '\' to turn audify off");
		println("press '_' to turn 90 degrees and execute last command, four times");
		println("press ')' to run scaled low pass filter");
		println("press 'v' to turn verbose output on and off");
		println("press 'h' to show help message");
	}

	
	public void draw() {
		if (screenNeedsUpdate) {
			fitPixels(isFitToScreen, false);
			screenNeedsUpdate = false;
		}
		if (isFitToScreen) {
			image(fitImg, 0, 0);
			if (audioIsRunning && !isHidden) {
				noFill();
				stroke(255);
				int x = (int) map(glitchSignal.getMapX(), 0, img.width, 0, fitImg.width);
				int y = (int) map(glitchSignal.getMapY(), 0, img.height, 0, fitImg.height);
				int w = (int) map(glitchSignal.getBlockEdgeSize(), 0, img.height, 0, fitImg.height);
				rect(x, y, w, w);
			}
		}
		else {
			background(255);
			image(img, -transX, -transY);
			if (audioIsRunning && !isHidden) {
				noFill();
				stroke(255);
				int x = glitchSignal.getMapX();
				int y = glitchSignal.getMapY();
				int w = glitchSignal.getBlockEdgeSize();
				rect(x, y, w, w);
			}
		}
		// testing
		// if (this.isShiftKeyDown()) println("Shift key down");
	}
	
	/**
	 * Detects Caps Lock state. We use Caps Lock state to switch between audio and graphics command sets. 
	 * @return true if Caps Lock is down, false otherwise.
	 */
	public boolean isCapsLockDown() {
		boolean isDown = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
		return isDown;
	}
	
	public boolean isShiftKeyDown() {
		return (isShiftKeyDown);
	}

	/**
	 * Experimental tool for setting a command sequence in variable cmd
	 */
	public void commandSequence() {
		String cmd;
		cmd = "t";
		/*// do the animation process
		process001();*/
		// or just do a windowedFFT
		windowedFFT();
//		cmd = "g";
//		for (int i = 0; i < 128; i++) {
//			setLineCount(233);
//			exec(cmd);
//			setLineCount(144);
//			exec(cmd);
//			setLineCount(89);
//			exec(cmd);
//			setLineCount(55);
//			exec(cmd);
//		}
		// (rotate zigzag save) 4 rotation sequence
		// cmd = "lcstlctttsttlcttstttlcts";
		// (degrade undo munge) 8 times
		// cmd = "dzmdzmdzmdzmdzmdzmdzmdzm";
		// (FFT:4 turn FFT:4 turn:3)
		// cmd = "kkkktkkkkttt";
		// (FFT turn):4
		// cmd = "ktktktkt";
		// (zigzag turn munge cycle)
		// cmd = "lmtltttmttlttmtttltm";
		// (zigzag turn cycle)
		// cmd = "ltltltlt";
		// cmd = "lzms";
		// cmd = "kmtkTmttkttmTktm";
//		cmd = "3gagagagagagagaga";
//		setLineCount(377);
//		exec(cmd);
//		setLineCount(233);
//		exec(cmd);
//		setLineCount(199);
//		exec(cmd);
//		cmd = "3gagagaga";
//		setLineCount(144);
//		exec(cmd);
		/*
		int oldLineCount = lineCount;
		setLineCount(5);
		cmd = "gtgt";
		exec(cmd);
		setLineCount(13);
		cmd = "gtgt";
		exec(cmd);
		setLineCount(21);
		cmd = "gtgT";
		exec(cmd);
		setLineCount(oldLineCount);*/
		//
		// zzBlox();
		// zzBloxMulti();
		//
		
		// rpzimi[dzm]:
		// exec(cmd);
	}
	
	
	public void windowedFFT() {
		// windowed FFT using current eq settings
		int shift = statFFTBlockWidth/2;
		exec("jtjT");
		shiftAllLeft(shift);
		exec("t");
		shiftAllLeft(shift);
		exec("T");
		exec("jtjT");
		shiftAllLeft(-shift);
		exec("t");
		shiftAllLeft(-shift);
		exec("T");
	}

	
	int hotbin = 32;
	public void process001() {
		// setup here
		hotbin = 32;
		for (int j = 0; j < 19; j++) {
			resetEq();
			if (hotbin <= 1) hotbin = 32;
			// set the slider
			for (int i = 0; i < eq.length; i++) {
				String token = cpm.sliderIdentifier + noPlaces.format(i);
				Slider slider = (Slider) cpm.getControl().getController(token);
				slider.setValue(0.0f);
				if (i == hotbin) {
					slider.setValue(0.667f);
				}
				if (i == hotbin - 1) {
					slider.setValue(-0.67f);
				}
			}
			hotbin--;
			// do a windowed FFT, save and revert
			for (int i = 0; i < 2; i++) {
				windowedFFT();
			}
			if (hotbin % 2 == 1) {
				exec("dtgTc");
			}
			else {
				exec("dTgtc");			
			}
			//exec("s&");
			exec("sR");
		}
	}
	
	public void formantTest() {
		String oldFileBaseName = fileBaseName;
		setColorQuantize(2);
		cpm.getControl().getController("setColorQuantize").setBroadcast(false);
		cpm.getControl().getController("setColorQuantize").setValue(colorQuantize);
		cpm.getControl().getController("setColorQuantize").setBroadcast(true);
		isHilbertScan = false;
		for (int j = 0; j < 11; j++) {
			formantIndex = j;
			theFormant = formantList[formantIndex];
			for (int i = 0; i < 32; i++) {
				exec("6");
			}
			fileBaseName = theFormant.symbol +"_"+ theFormant.example;
			exec("s");
			exec("9ps9ps99ps9999ps99999999999999999999999999999999p");
			exec("sR");
		}
		isHilbertScan = true;
		for (int j = 0; j < 11; j++) {
			formantIndex = j;
			theFormant = formantList[formantIndex];
			for (int i = 0; i < 8; i++) {
				exec("6");
			}
			fileBaseName = theFormant.symbol +"_"+ theFormant.example;
			exec("s");
			exec("9ps9ps99ps9999ps99999999999999999999999999999999p");
			exec("sR");
		}
		fileBaseName = oldFileBaseName;
	}
	
	public void scaleTest() {
		String oldFileBaseName = fileBaseName;
		setColorQuantize(2);
		cpm.getControl().getController("setColorQuantize").setBroadcast(false);
		cpm.getControl().getController("setColorQuantize").setValue(colorQuantize);
		cpm.getControl().getController("setColorQuantize").setBroadcast(true);
		float baseFreq = 1.1484375f * 4.0f;    // 1.1484375 = 44100 divided by (800 * 48)
		famp1 = 3.0f;
		famp2 = 3.0f;
		famp3 = 3.0f;
		int octave = 1;
		for (int k = 0; k < 10; k++) {
			ffreq1 = (baseFreq * octave);
			if (ffreq1 > 22050) {
				println("----->>>-------->>>>> end of scale test <<<<<--------<<<-----");
				break;
			}
	    ffreq2 = ffreq1 * (float) (5.0/4.0);			// M3 in just intonation
	    ffreq3 = ffreq1 * (float) (4.0/3.0); 			// P5 in just intonation;
			// ffreq2 = ffreq1 * (float) Math.pow(2, 6/12.0); //  tritone;
			// ffreq3 = ffreq1 * (float) Math.pow(2, 11/12.0); // fourth higher, for a 7-10-13 voicing;
			loadFormantList();
			isHilbertScan = true;
			// last frequency of an iteration is first frequency of the next iteration, so skip last
			for (int j = 11; j < formantList.length - 1; j++) {
				formantIndex = j;
				theFormant = formantList[formantIndex];
				if (theFormant.freq1 > 22050) break;
				for (int i = 0; i < 8; i++) {
					exec("6");
				}
				// fileBaseName = "unison_"+ theFormant.symbol +"_"+ noPlaces.format(theFormant.freq1);
				fileBaseName = "rgbformant_"+ theFormant.symbol +"_"+ noPlaces.format(theFormant.freq1);
				// exec("s");
				// exec("9ps9ps99ps9999ps99999999999999999999999999999999p");
				exec("sR");
			}
			octave *= 2;
		}
		fileBaseName = oldFileBaseName;
	}

	public void shiftTest() {
		// String oldFileBaseName = fileBaseName;
		for (int k = 0; k < 1024; k++) {
			shiftScanLeft(1024);
			exec("s");
		}
		// fileBaseName = oldFileBaseName;
	}
	
	public void zzBlox() {
		String cmd = "lt";
		zigzagFloor = zigzagCeiling = 128;
		exec(cmd);
		zigzagFloor = zigzagCeiling = 64;
		exec(cmd);
		zigzagFloor = zigzagCeiling = 32;
		exec(cmd);
		zigzagFloor = zigzagCeiling = 16;
		exec(cmd);
	}
	
	public void zzBloxSeries() {
		zzBlox();
		exec("((psR");
		//
		zzBlox();
		zzBlox();
		exec("((psR");
		//
		zzBlox();
		zzBlox();
		zzBlox();
		exec("((psR");
		//
		zzBlox();
		zzBlox();
		zzBlox();
		zzBlox();
		exec("((psR");
	}
	
	public void zzBloxMulti() {
		int dec = 5;
		int breakval = 999;
		exec("3");
		for (int i = 0; i < 4; i++) {
			this.setBreakpoint(breakval);
			zzBloxSeries();
			exec("s");
			breakval -= dec;
		}
		this.setBreakpoint(breakval);
		zzBloxSeries();
	}
	
	ArrayList<PVector> hilbertPoints = new ArrayList<PVector>();
	boolean isHilbertScan = false;
	public void runHilbert(int depth) {
		StringBuffer hilb = hilbert(depth);
		int step = 1;
		Turtle turtle = new Turtle();
		turtle.setTurtleX(0);
		turtle.setTurtleY(0);
		hilbertPoints.add(new PVector((float) turtle.getTurtleX(), (float) turtle.getTurtleY()));
		for (int i = 0; i < hilb.length(); i++) {
			char ch = hilb.charAt(i);
			switch(ch) {
			case '+' : {
				turtle.turn(radians(90));
				break;
			}
			case '-' : {
				turtle.turn(radians(-90));
				break;
			}
			case 'F' : {
				turtle.move(step);
				hilbertPoints.add(new PVector((float) turtle.getTurtleX(), (float) turtle.getTurtleY()));
				break;
			}
			default: {
				
			}
			}
		}
		/*
		for (PVector vec : hilbertPoints) {
			println(Math.round(vec.x), Math.round(vec.y));
		}
		*/
		println("-------- hilbertPoints depth = "+ depth);
		println("-------- hilbertPoints length = "+ hilbertPoints.size());
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
		if (null != buf) println(buf.toString());
		return buf;
	}

	/**
	 * Experimental tool for executing a command sequence
	 */
	public void commandSequence(char cue) {
		String cmd;
		// (char in turn cycle)
		cmd = "t"+ cue +"t"+ cue +"t"+ cue +"t"+ cue;
		// rpzimi[dzm]:
		exec(cmd);
	}

	/**
	 * Executes a supplied commend sequence
	 * @param cmd   a command sequence
	 */
	public void exec(String cmd) {
		char[] cycle = cmd.toCharArray();
		for (char ch : cycle) {
			decode(ch);
		}
	}
	
	/**
	 * Demo method of a brief animation output as PNG files 
	 */
	public void anim() {
		zigzagFloor = 24;
		zigzagCeiling = 96;
		this.setAscending(true, false);
		this.setCompOrder(CompOrder.HSB.ordinal(), false);
		this.setSwap(SwapChannel.BB, false);
		this.setIsSwapChannels(true, false);
		String cmd = "nskl stlttt sttltt stttltmc";
		for (int i = 0; i < 8; i++) {
			exec(cmd);
		}
	}

	
	boolean isShiftKeyDown = false;
	/* (non-Javadoc)
	 * @see processing.core.PApplet#mousePressed()
	 * if the ControlP5 control panel is hidden, permit image panning
	 * also allow panning if both tabs are not active, as at startup
	 * or any time the mouse is not within an active tab
	 * I have not handled the cases where tabs are collapsed.
	 */
//	public void mousePressed(MouseEvent event) {
//		if (event.isShiftDown()) isShiftKeyDown = true;
//		else isShiftKeyDown = false;
//		if (cpm.panelIsInactive()) isDragImage = true;
//		else {
//			isDragImage = !(cpm.controlPanelRect().contains(mouseX, mouseY));
//		}
//	}

	public void mousePressed() {
		if (cpm.panelIsInactive()) isDragImage = true;
		else {
			isDragImage = !(cpm.controlPanelRect().contains(mouseX, mouseY));
		}
	}

	
// commented out to allow image translation in mouseDragged(): this method does not refresh mouse vars	
//	public void mouseReleased(MouseEvent event) {
//		if (event.isShiftDown()) isShiftKeyDown = true;
//		else isShiftKeyDown = false;
//	}
	
	// handle dragging to permit large images to be panned
	// shift-drag will always work, shift key is not needed if control panel is hidden 
//	public void mouseDragged(MouseEvent event) {
//		if (event.isShiftDown()) isShiftKeyDown = true;
//		else isShiftKeyDown = false;
//		println("mouseX", mouseX, "pmouseX", pmouseX, "mouseY", mouseY, "pmouseY", pmouseY);
//		translateImage(-mouseX + pmouseX, -mouseY + pmouseY);
//		// output: mouseX 0 pmouseX 0 mouseY 0 pmouseY 0
//	}
	
	// TODO the translation code is misbehaving, jumping on mousedowns
	// mouseX and other variables apparently don't get set in the mouseDragged(MouseEvent event) method
	public void mouseDragged() {
		if (isDragImage) {
			// println("mouseX", mouseX, "pmouseX", pmouseX, "mouseY", mouseY, "pmouseY", pmouseY);
			translateImage(-mouseX + pmouseX, -mouseY + pmouseY);
		}		
	} 
	
// commented out to allow image translation in mouseDragged(): this method does not refresh mouse vars	
//	public void mouseMoved(MouseEvent event) {
//		if (event.isShiftDown()) isShiftKeyDown = true;
//		else isShiftKeyDown = false;
//	}
	
	/* (non-Javadoc)
	 * handles key presses intended as commands
	 * @see processing.core.PApplet#keyPressed()
	 */
	public void keyPressed() {
		parseKey(key, keyCode);
	}

	public void parseKey(char key, int keyCode) {
		if (key == '_') {
			commandSequence(lastCommand);
		}
		else if (key == '+') {
			commandSequence();
		}
		else if (key == ')') {
			scaledLowPass();
		}
		else if (key != CODED) {
			decode(key);
		}
		// TODO audio navigation
		else {
			if (keyCode == UP) {
				if (audioIsRunning && this.isCapsLockDown()) this.glitchSignal.moveBlock(0, -1);
				else incrementDegradeQuality(true);      // increment degradeQuality
			}
			else if (keyCode == DOWN) {
				if (audioIsRunning && this.isCapsLockDown()) this.glitchSignal.moveBlock(0, 1);
				else incrementDegradeQuality(false);     // decrement degradeQuality 
			}
			else if (keyCode == RIGHT) {
				if (audioIsRunning && this.isCapsLockDown()) this.glitchSignal.moveBlock(1, 0);
				else incrementColorQuantize(true);       // incremeent colorQuantize
			}
			else if (keyCode == LEFT) {
				if (audioIsRunning && this.isCapsLockDown()) this.glitchSignal.moveBlock(-1, 0);
				else incrementColorQuantize(false);      // decrement colorQuantize
			}
		}
		if ("gl<>9kjdGLKJD6".indexOf(key) > -1) lastCommand = key;
		if (this.audioIsRunning) {
			this.glitchSignal.setIsUpdate(true);
		}
	}
	
	/**
	 * associates characters input from keyboard with commands
	 * @param ch   a char value representing a command
	 */
	public void decode(char ch) {
		if (ch == ' ') {
			toggleControlPanelVisibility();          // hide and show control panels
		}
		else if (ch == '7') {
			printFileInfo();
		}
		else if (ch == '&') {
			stepFile();
		}
		else if (ch == '*') {
			chooseFolder();                          // choose folder for importing files on command
		}
		else if (ch == '#') {
			setIsHilbertScan(!isHilbertScan, false);
		}
		else if (ch == '1') {
			setSorter(SorterType.QUICK, false);      // use quick sort 
		}
		else if (ch == '2') {
			setSorter(SorterType.SHELL, false);      // use shell sort
		}
		else if (ch == '3') {
			setSorter(SorterType.BUBBLE, false);     // use bubble sort
		}
		else if (ch == '4') {
			setSorter(SorterType.INSERT, false);     // use insert sort
		}
		else if (ch == 'g') {
			sortPixels();                            // 'g' for glitch: sort with current algorithm
		}
		else if (ch == 'G') {
			if (audioIsRunning) glitchSignal.decode(ch);
			else sortPixels();                       // 'g' for glitch: sort with current algorithm
		}
		else if (ch == 'o') {
			openFile();                              // open a new file
		}
		else if (ch == 'O') {
			loadFileToSnapshot();                    // load a file to the snapshot buffer
		}
		else if (ch == 'n' || ch == 'N') {
			snap();                                  // save display to snapshot buffer
		}
		else if (ch == 'u' || ch == 'U') {
			unsnap();                                // copy snapshot buffer to display
		}
		else if (ch == 'b' || ch == 'B') {
			setRandomBreak(!randomBreak, false);     // toggle random break on or off
		}
		else if (ch == 'v' || ch == 'V') {
			//verbose = !verbose;                      // toggle verbose on or off
			println("verbose is "+ verbose);
		}
		else if (ch == 's') {
			saveFile();                               // save to file
		}
		else if (ch == 'S') {
			// timestamp = getTimestamp();
			// File temp = new File(projPath +"/"+ getNewBaseName() +"_"+ timestamp);
			saveFileAs();
		}
		else if (ch == '=') {
			int n = (compOrderIndex + 1) % CompOrder.values().length;
			setCompOrder(n, false);                  // increment compOrderIndex
		}
		else if (ch == '-') {
			int n = (compOrderIndex + CompOrder.values().length - 1) % CompOrder.values().length;
			setCompOrder(n, false);                  // decrement compOrderIndex
		}
		else if (ch == 'a' || ch == 'A') {
			// ascending or descending sort
			setAscending(!isAscendingSort, false);   // toggle ascending/descending sort
		}
		else if (ch == 'r') {
			revert(false);                           // reload display from disk
		}
		else if (ch == 'R') {
			revert(true);                                // reload display from original file on disk
		}
		else if (ch == 't') {
			rotatePixels(true);                          // rotate display 90 degrees right (CW)
		}
		else if (ch == 'T') {
			rotatePixels(false);                         // rotate display 90 degrees left (CCW)
		}
		else if (ch == 'x' || ch == 'X') {
			setIsSwapChannels(!isSwapChannels, false);   // toggle channel swapping (color glitching)
		}
		else if (ch == 'c') {
			int n = (swap.ordinal() + 1) % SwapChannel.values().length;
			setSwap(SwapChannel.values()[n], false);     // increment swap channel settings
		}
		else if (ch == 'C') {
			int n = (swap.ordinal() - 1) % SwapChannel.values().length;
			if (n < 0) {setSwap(SwapChannel.values()[SwapChannel.values().length - 1], false);}
			else {setSwap(SwapChannel.values()[n], false);}     // increment swap channel settings
		}
		else if (ch == 'z' || ch == 'Z') {
			restore();                               // copy undo buffer to display
		}
		else if (ch == 'h') {
			printHelp();                             // print help message
		}
		else if (ch == 'H') {
			if (audioIsRunning) glitchSignal.decode(ch);
			else runHilbert(4);                     // print help message
		}
		else if (ch == 'f' || ch == 'F') {
			fitPixels(!isFitToScreen, false);        // toggle display window size to fit to screen or not
		}
		else if (ch == 'd' || ch == 'D') {
			degrade();                               // save and reload JPEG with current compression quality
		}
		else if (ch == 'm' || ch == 'M') {
			munge();                                 // composite display and snapshot with undo buffer difference mask 
		}
		else if (ch == 'i' || ch == 'I') {
			invertMunge(!isMungeInverted, false);    // invert functioning of the difference mask for munge command
		}
		else if (ch == 'y' || ch == 'Y') {
			setCycle(!isCycleGlitch, false);         // in multi-step sort, cycle through all lines in image
		}
		else if (ch == 'p' || ch == 'P') {
			reduceColors();                          // quantize colors
		}
		else if (ch == 'l') {
			zigzag();                                // perform a zigzag sort
		}
		else if (ch == 'L') {
			if (audioIsRunning) glitchSignal.decode(ch);
			else hilbertScan();                                // perform a zigzag sort
		}
		else if (ch == 'k') {
			statFFT();                         // perform an FFT using statistical interface settings
		}
		else if (ch == 'K') {
			if (audioIsRunning) glitchSignal.decode(ch);
			else statFFT();                    // perform an FFT using statistical interface settings
		}
		else if (ch == 'j') {
			eqFFT();                           // perform an FFT using equalizer interface settings
		}
		else if (ch == 'J') {
			if (audioIsRunning) glitchSignal.decode(ch);
			else eqFFT();                      // perform an FFT using equalizer interface settings
		}
		else if (ch == ';') {
			if (isCapsLockDown()) analyzeEq(true);   // perform analysis of frequencies in image
			else shiftEqBinsLeft();
		}
		else if (ch == ':') {
			if (isCapsLockDown()) testEq();          // run a test of the FFT
			else shiftEqBinsRight();
		}
		else if (ch == '{') {
			decShellIndex();                         // step to previous shell sort settings
		}
		else if (ch == '}') {
			incShellIndex();                         // step to next shell sort settings
		}
		else if (ch == '[') {
			incrementGlitchSteps(false);             // increase the glitchSteps value
		}
		else if (ch == ']') {
			incrementGlitchSteps(true);              // decrease the glitchSteps value
		}
		else if (ch == '/') {
			audify();                                // turn on audify
		}
		else if (ch == '\\') {
			audifyOff();                             // turn off audify
		}
		else if (ch == '9') {
			if (this.isCapsLockDown()) mean();
			else denoise();                              // denoise
		}
		else if (ch == '(') {
			if (this.isCapsLockDown()) for (int i = 0; i < 16; i++) mean(); // mucho mean
			else for (int i = 0; i < 16; i++) denoise(); // mucho denoise
		}
		else if (ch == '<') {
			shiftLeft();                            // shift selected color channel left
		}
		else if (ch == '>') {
			shiftRight();                           // shift selected color channel right
		}
		else if (ch == ',') {
			incrementLineCount(true); // inc lineCount
		}
		else if (ch == '.') {
			incrementLineCount(false); // dec lineCount
		}
		else if (ch == 'w' || ch == 'W') {
			if (audioIsRunning && this.isCapsLockDown()) glitchSignal.decode(ch);
			else {
				this.setLeftBound(-0.25f);
				this.setRightBound(5.0f);
				Range r1 = (Range) cpm.getControl().getController("setStatEqRange");
				r1.setBroadcast(false);
				r1.setHighValue(5.0f);
				r1.setLowValue(-0.25f);
				r1.setBroadcast(true);
			}
		}
		else if (ch == 'q' || ch == 'Q') {
			if (audioIsRunning && this.isCapsLockDown()) glitchSignal.decode(ch);
			else {
				// invert the settings used by resetStat to obtain a blur effect
				invertStat();
			}
		}
		else if (ch == '0') {
			if (audioIsRunning) glitchSignal.decode(ch);
		}
		else if (ch == '%') {
			formantIndex = (formantIndex - 1 < 0) ? formantList.length - 1 : (formantIndex - 1) % formantList.length;
			theFormant = formantList[formantIndex];
			loadFormant(theFormant);
			println("formant: ", theFormant.symbol, theFormant.example, theFormant.freq1, theFormant.freq2, theFormant.freq3);
		}
		else if (ch == '5') {
			formantIndex = (formantIndex + 1) % formantList.length;
			theFormant = formantList[formantIndex];
			loadFormant(theFormant);
			println("formant: ", theFormant.symbol, theFormant.example, theFormant.freq1, theFormant.freq2, theFormant.freq3);
		}
		else if (ch == '6') {
			if (audioIsRunning && this.isCapsLockDown()) glitchSignal.decode(ch);
			else this.formantFFT(theFormant);
		}
		else if (ch == '8') {
			// println("-- not saving to AI file, code is quoted out --");
			// this.saveToAI();
			getColors();
		}
		else if (ch == '\"') {
			initPanelSettings(true);
			// shiftTest();
		}
		else if (ch == '\'') {
			// scaleTest();
			// TODO shift negative values
			doShift();
		}
	}
	
		
    /********************************************/
    /*                                          */
    /*           >>> CONTROL PANEL <<<          */
    /*                                          */
    /********************************************/
	
	/**
	 * Shows or hides the control panel
	 */
	public void toggleControlPanelVisibility() {
			controlFrame.setVisible(!controlFrame.isVisible());
	}
	
	

	/**
	 * Once control panels have been created and drawn, set up initial positions and values
	 */
	public void initPanelSettings() {
		// simplest way to avoid some annoying errors is to set various control panel radio buttons
		// after panel has been constructed
		setSorter(SorterType.BUBBLE, false);
		this.setBreakpoint(999);
		setSorter(SorterType.SHELL, false);
		this.setBreakpoint(996);
		setSorter(SorterType.QUICK, false);
		this.setBreakpoint(1);
		setCompOrder(compOrderIndex, false);
		setSwap(SwapChannel.RR, false);
		setEqChan(true, false, false, false, false, false, false);
		setStatChan(true, false, false, false, false, false, false);
		setLink(false, false);
		Slider s4 = (Slider) cpm.getControl().getController("setFFTBlockWidth"); 
		s4.setSliderMode(Slider.FLEXIBLE);
		s4.setNumberOfTickMarks(8);
		setZigzagStyle(zigzagStyle, false);
	} 
	
	
	/**
	 * Once control panels have been created and drawn, set up initial positions and values
	 */
	public void initPanelSettings(boolean isFormantTest) {
		// simplest way to avoid some annoying errors is to set various control panel radio buttons
		// after panel has been constructed
		if (!isFormantTest) {
			initPanelSettings();
			return;
		}
		println("-------->>>>> custom FFT settings <<<<<--------");
		setSorter(SorterType.BUBBLE, false);
		this.setBreakpoint(999);
		setSorter(SorterType.SHELL, false);
		this.setBreakpoint(996);
		setSorter(SorterType.QUICK, false);
		this.setBreakpoint(1);
		setCompOrder(CompOrder.BHS.ordinal(), false);
		setSwap(SwapChannel.BB, false);
		setEqChan(true, false, false, false, false, false, false);
		setStatChan(true, false, false, false, false, false, false);
		setLink(false, false);
		Slider s4 = (Slider) cpm.getControl().getController("setFFTBlockWidth"); 
		s4.setSliderMode(Slider.FLEXIBLE);
		s4.setNumberOfTickMarks(8);
		setZigzagStyle(zigzagStyle, false);
	} 

	

    /********************************************/
    /*                                          */
    /*          >>> GLITCH COMMANDS <<<         */
    /*                                          */
    /********************************************/
	
	/**
	 * opens a user-specified file (JPEG, GIF or PNG only)
	 */
	public void openFile() {
		chooseFile();
	}
	
	/**
	 * reverts display and display buffer to last opened file
	 */
	public void revert(boolean toOriginalFile) {
		if (null != displayFile) {
			if (toOriginalFile) loadOriginalFile();	
			else loadFile();
			if (isFitToScreen) fitPixels(true, false);
			// reset row numbers and translation
			loadRowNums();
			resetRanger();
			shuffle(rowNums);
			clipTranslation();
		}
	}
	
	public void resetCount() {
		fileCount = 0;
		println("fileCount = "+ fileCount);
	}
	
	/**
	 * Sets the value above which the current sort method will randomly interrupt, when randomBreak 
	 * is true (the default). Each sorting method uses a distinct value from 1 to 999. Quick sort
	 * can use very low values, down to 1.0. The other sorting methods--shell sort, insert sort, 
	 * bubble sort--generally work best with higher values. 
	 * @param newBreakPoint   the breakpoint to set
	 */
	public void setBreakpoint(float newBreakPoint) {
		// println("---- setBreakpoint, value = "+ newBreakPoint);
		if (newBreakPoint == breakPoint) return;
		breakPoint = newBreakPoint;
		sortTool.sorter.setBreakPoint(breakPoint);
	}
	
	/**
	 * increments shellIndex, changes shell sort settings
	 */
	public void incShellIndex() {
		shellIndex = shellIndex < shellParams.length - 3 ? shellIndex + 2 : 0;
		int r = shellParams[shellIndex];
		int d = shellParams[shellIndex + 1];
		sortTool.shell.setRatio(r);
		sortTool.shell.setDivisor(d);
		println("ShellIndex = "+ shellIndex +", Shellsort ratio = "+ r +", divisor = "+ d);
	}
	/**
	 * decrements shellIndex, changes shell sort settings
	 */
	public void decShellIndex() {
		shellIndex = shellIndex > 1 ? shellIndex - 2 : shellParams.length - 2;
		int r = shellParams[shellIndex];
		int d = shellParams[shellIndex + 1];
		sortTool.shell.setRatio(r);
		sortTool.shell.setDivisor(d);
		println("ShellIndex = "+ shellIndex +", Shellsort ratio = "+ r +", divisor = "+ d);
	}
	
	/**
	 * Initializes rowNums array, used in stepped or cyclic sorting, with img.height elements.
	 */
	public void loadRowNums() {
		rowNums = new int[img.height];
		for (int i = 0; i < img.height; i++) rowNums[i] = i;
	}
	
	/**
	 * Initializes rowNums array to rowCount elements, shuffles it and sets row value to 0.
	 */
	public void resetRowNums(int rowCount) {
		rowNums = new int[rowCount];
		for (int i = 0; i < rowCount; i++) rowNums[i] = i;
		resetRowNums();
	}
	/**
	 * Shuffles rowNums array and sets row value to 0.
	 */
	public void resetRowNums() {
		shuffle(rowNums);
		row = 0;
		if (verbose) println("Row numbers shuffled");
	}
	
	/**
	 * Sets RangeManager stored in ranger to intial settings, with a range of img.height 
	 * and number of intervals equal to glitchSteps.
	 */
	public void resetRanger() {
		if (null == ranger) {
			ranger = new RangeManager(img.height, (int) glitchSteps);
		}
		else {
			ranger.resetCurrentIndex();
			ranger.setRange(img.height);
			ranger.setNumberOfIntervals((int) glitchSteps);
		}
		if (verbose) println("range index reset to 0");
	}

	int rCount = 0;
	/**
	 * rotates image and backup image 90 degrees clockwise
	 */
	public void rotatePixels(boolean isTurnRight) {
		if (null == img) return;
		if (isTurnRight) img = rotateImageRight(img);
		else img = rotateImageLeft(img);
		fitPixels(isFitToScreen, false);
		// rotate undo buffer image, don't rotate snapshot
		if (null != bakImg) {
			if (isTurnRight) bakImg = rotateImageRight(bakImg);
			else bakImg = rotateImageLeft(bakImg);
		}
		// load the row numbers
		loadRowNums();
		resetRanger();
		// shuffle the row numbers
		shuffle(rowNums);
		// clip translation to image bounds
		clipTranslation();
	}
	
	/**
	 * rotates image pixels 90 degrees clockwise
	 * @param image   the image to rotate
	 * @return        the rotated image
	 */
	public PImage rotateImageRight(PImage image) {
		// rotate image 90 degrees
		int h = image.height;
		int w = image.width;
		int i = 0;
		PImage newImage = createImage(h, w, ARGB);
		newImage.loadPixels();
		for (int ry = 0; ry < w; ry++) {
			for (int rx = 0; rx < h; rx++) {
				newImage.pixels[i++] = image.pixels[(h - 1 - rx) * image.width + ry];
			}
		}
		newImage.updatePixels();
		return newImage;
	}

	/**
	 * rotates image pixels 90 degrees clockwise
	 * @param image   the image to rotate
	 * @return        the rotated image
	 */
	public PImage rotateImageLeft(PImage image) {
		// rotate image 90 degrees
		int h = image.height;
		int w = image.width;
		int i = 0;
		PImage newImage = createImage(h, w, ARGB);
		newImage.loadPixels();
		for (int ry = w-1; ry >= 0; ry--) {
			for (int rx = h-1; rx >= 0; rx--) {
				newImage.pixels[i++] = image.pixels[(h - 1 - rx) * image.width + ry];
			}
		}
		newImage.updatePixels();
		return newImage;
	}
	
	/**
	 * Copy an image, work around problems in Processing 2.1 with PImage.get.
	 * Faster than get() anyway.
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
	 * Copy an image pixel by pixel, test code to work around problems in Processing 2.1 with PImage.get.
	 * @param image   image to copy
	 * @return        the image submitted with apha channel set to desired value
	 */
	public PImage loadImageAlpha(PImage image, int alpha) {
		int i = 0;
		image.loadPixels();
		for (i = 0; i < image.pixels.length; i++) {
			int[] rgb = rgbComponents(image.pixels[i]);
			image.pixels[i] = alpha << 24 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
		}
		image.updatePixels();
		return image;
	}
	
	/**
	 * tranlates the display image by a specified horizontal and vertical distances
	 * @param tx   distance to translate on x-axis
	 * @param ty   distance to translate on y-axis
	 */
	public void translateImage(int tx, int ty) {
		transX += tx;
		transY += ty;
		clipTranslation();
	}
	
	/**
	 * handles clipping of a translated image to the display window
	 */
	public void clipTranslation() {
		int limW = (frameWidth < img.width) ? img.width - frameWidth : 0;
		int limH = (frameHeight < img.height) ? img.height - frameHeight : 0;
		if (transX > limW) transX = limW;
		if (transX < 0) transX = 0;
		if (transY > limH) transY = limH;
		if (transY < 0) transY = 0;
		// println(transX +", "+ transY  +", limit width = "+ limW  +", limit height = "+ limH +", image width = "+ img.width +", image height = "+ img.height);		
	}

	/**
	 * Sorts the pixels line by line, in random order, using the current
	 * sorting method set in sortTool.
	 * @fix moved loadPixels outside loop
	 * TODO implement a cycle and row manager class
	 */
	public void sortPixels() {
		multiSortPixels();
		if (true) return;
		if (null == img || null == ranger) {
			println("No image is available for sorting or the ranger is not initialized (sortPixels method)");
			return;
		}
		// TODO implement methods to set sorter values for color swapping, sort order, component order
		// for the moment, we do it by providing the sortTool with top level access to the GlitchSort instance
		// and pulling the values from local variables. It would be more efficient to do this only on control panel changes. 
		sortTool.setControlState();
		backup();
		img.loadPixels();
		this.sortTool.setControlState();
		if (isCycleGlitch) {
			IntRange range;
			if (ranger.hasNext()) {
				range = ranger.getNext();
				println(range.toString());
			} 
			else {
				ranger.resetCurrentIndex();
				range = ranger.getNext();
				resetRowNums();
				println("starting a new cycle");
			}
			for (int i = range.lower; i < range.upper; i++) {
				int n = rowNums[i];
				if (verbose) println("sorting row "+ n +" at index "+ i);
				row++;
				int l = n * img.width;
				int r = l + img.width - 1;
				sortTool.sort(img.pixels, l, r);
			}
		}
		else {
			int rowMax = (int)(Math.round(rowNums.length / glitchSteps));
			for (int i = 0; i < rowMax; i++) {
				int n = rowNums[i];
				if (verbose) println("sorting row "+ n);
				int l = n * img.width;
				int r = l + img.width - 1;
				sortTool.sort(img.pixels, l, r);
			}
			shuffle(rowNums);
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	
	/**
	 * TODO this becomes the bottleneck sort method (ok) and is incorporated into SortSelector (?)
	 * Sorts the pixels in grouped lines in random order
	 */	
	public void multiSortPixels() {
		float partCount = 0;
		if (null == img || null == ranger) {
			println("No image is available for sorting or the ranger is not initialized (sortPixels method)");
			return;
		}
		// TODO implement methods to set sorter values for color swapping, sort order, component order
		// for the moment, we do it by providing the sortTool with top level access to the GlitchSort instance
		// and pulling the values from local variables. It would be more efficient to do this only on control panel changes. 
		backup();
		img.loadPixels();
		// set sorting to current settings
		this.sortTool.setControlState();
		if (isCycleGlitch) {
			println("multi cycle");
			IntRange range;
			if (ranger.hasNext()) {
				range = ranger.getNext();
				println(range.toString());
			} 
			else {
				ranger.resetCurrentIndex();
				range = ranger.getNext();
				resetRowNums();
				println("starting a new cycle");
			}
			// include upper range (@bugfix, 2014-1-8)
			if (lineCount == 1) {
				for (int i = range.lower; i <= range.upper; i++) {
					int n = rowNums[i];
					if (verbose) println("sorting row "+ n +" at index "+ i);
					row++;
					int l = n * img.width;
					int r = l + img.width - 1;
					sortTool.sort(img.pixels, l, r);
				}
			}
			else {
				// TODO random multi-line cycle sorting
				int rowMax = range.upper;
				int i = range.lower;
				//if (i < 0) i = 0;
				while (i <= rowMax) {
					int n = i + lineCount;
					if (n > rowMax) n = rowMax;
					int l = i * img.width;
					int r = (n * img.width);
//					if (verbose) println("sorting row "+ i +" to "+ n);
					println("sorting row "+ i +" to "+ n);
					sortTool.sort(img.pixels, l, r);
					i += lineCount;
				}
			}
		}
		else {
			int rowMax = img.height;
			int i = 0;
			while (i < rowMax) {
				int n = i + lineCount;
				if (n >= rowMax) n = rowMax - 1;
				int l = i * img.width;
				int r = (n * img.width) - 1;
				if (verbose) println("sorting row "+ i +" to "+ n);
				sortTool.sort(img.pixels, l, r);
				i += lineCount;
			}
			shuffle(rowNums);
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}

	/**
	 * Saves a copy of the currently displayed image in img to bakImg.
	 */
	public void backup() {
		bakImg = copyImagePixels(img);
	}

	/**
	 * Undoes the last command. Not applicable to munge command.
	 */
	public void restore() {
		// store a copy of the current image in tempImg
		PImage tempImg = copyImagePixels(img);
		img = bakImg;
		bakImg = tempImg;
		// println("--- restore");
		fitPixels(isFitToScreen, false);
		// if the display image and the backup image are different sizes, we need to reset rows and translation
		loadRowNums();
		resetRanger();
		shuffle(rowNums);
		clipTranslation();
	}
	
	/**
	 * Saves a copy of the currently displayed image in img to snapImg.
	 */
	public void snap() {
//		if (null == snapImg) snapImg = createImage(width, height, ARGB);
//		snapImg.resize(img.width, img.height);
//		snapImg.copy(img, 0, 0, img.width, img.height, 0, 0, img.width, img.height);
		snapImg = copyImagePixels(img);
		println("took a snapshot of current state");
	}
	
	/**
	 * copies snapImg to img, undo buffer bakImg is not changed
	 */
	public void unsnap() {
		if (null == snapImg) return;
		img = copyImagePixels(snapImg);
		fitPixels(isFitToScreen, false);
		// if the display image and the snapshot image are different sizes, we need to reset rows and translation
		loadRowNums();
		resetRanger();
		shuffle(rowNums);
		clipTranslation();
	}
	
	/**
	 * loads a file into snapshot buffer.
	 */
	public void loadFileToSnapshot() {
		selectInput("Image file for snapshot buffer:", "snapshotFileSelected");
	}
	
	public void snapshotFileSelected(File selectedFile) {
		if (null != selectedFile) {
			noLoop();
			File snapFile = selectedFile;
			// snapImg = createImage(width, height, ARGB);
			snapImg = loadImage(snapFile.getAbsolutePath()).get();
			println("loaded "+ snapFile.getName() +" to snapshot buffer: width = "+ snapImg.width +", height = "+ snapImg.height);
			loop();
		}
		else {
			println("No file was selected");
		}
	}
	
	/**
	 * Composites the current image (img) with the snapshot (snapImg) using the undo buffer (bakImg)
	 * as a mask. When the largest absolute difference between a pixel in the image and the same
	 * pixel in the undo buffer is greater than mungeThreshold, a pixel from the snapshot will be written
	 * to the image. The undo buffer and the snapshot will be resized to the image dimensions if
	 * necessary (it's not called "munge" for nothing).
	 */
	public void munge() {
		if (null == bakImg || null == snapImg) {
			println("To munge an image you need an undo buffer and a snapshot");
			return;
		}
		if (img.width != bakImg.width || img.height != bakImg.height) {
			bakImg.resize(img.width, img.height);
		}
		if (img.width != snapImg.width || img.height != snapImg.height) {
			snapImg.resize(img.width, img.height);
			// fix problem with files that are missing an alpha channel
			loadImageAlpha(snapImg, 255);
		}
		img.loadPixels();
		bakImg.loadPixels();
		snapImg.loadPixels();
		int alpha = 255 << 24;
		for (int i = 0; i < img.pixels.length; i++) {
			int src = Math.abs(img.pixels[i]);
			int targ = Math.abs(bakImg.pixels[i]);
			int diff = maxColorDiff(src, targ);
			if (isMungeInverted) {
				if (diff < mungeThreshold) {
					img.pixels[i] = snapImg.pixels[i] | alpha;
				}
				
			}
			else {
				if (diff > mungeThreshold) {
					img.pixels[i] = snapImg.pixels[i] | alpha;
				}
			}
		}
		println("munged -----");
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	

	/**
	 * parameterless method that ControlP5 button in FFT tab calls
	 * desaturates color using a standard formula, better results than setting saturation to 0
	 */
	public void desaturate() {
		println("-------- Desaturate --------");
		int[] comp = new int[3];
		backup();
		img.loadPixels();
		for (int i = 0; i < img.pixels.length; i++) {
			int px = img.pixels[i];
			comp[0] = (px >> 16) & 0xFF;  // Faster way of getting red(argb)
			comp[1] = (px >> 8) & 0xFF;   // Faster way of getting green(argb)
			comp[2] = px & 0xFF;          // Faster way of getting blue(argb)
			int gray = (int) (comp[0] * 0.59f + comp[1] * 0.3f + comp[2] * 0.11f);
			img.pixels[i] = color(gray);
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
     

	/**
	 * degrades the image by saving it as a low quality JPEG and loading the saved image
	 */
	public void degrade() {
		try {
			backup();
			println("degrading");
			degradeImage(img, degradeQuality);
			if (isFitToScreen) fitPixels(true, false);
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Quantizes colors in image to a user-specified value between 2 and 255
	 */
	public void reduceColors() {
		BufferedImage im = (BufferedImage) img.getNative();
		if (null == quant) {
			quant = new ImageColorQuantizer(colorQuantize);
		}
		else {
			quant.setColorCount(colorQuantize);
		}
		quant.filter(im, null);
		int[] px = quant.pixels;
		if (px.length != img.pixels.length) {
			println("---- pixel arrays are not equal (method reduceColors)");
			return;
		}
		backup();
		img.loadPixels();
		int alpha = 255 << 24;
		for (int i = 0; i < px.length; i++) {
			// provide the alpha channel, otherwise the image will vanish
			img.pixels[i] = px[i] | alpha;
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	
	/**
	 * implements a basic 3x3 denoise (median) filter
	 * TODO provide generalized filter for any edge dimension, tuned to individual color channels
	 */
	public void denoise() {
		int boxW = 3;
		int medianPos = 4;
		backup();
		PImage imgCopy = copyImagePixels(img);
		int w = img.width;
		int h = img.height;
		int[] pix = new int[boxW * boxW];
		img.loadPixels();
		for (int v = 1; v < h - 1; v++) {
			for (int u = 1; u < w - 1; u++) {
				int k = 0;
				for (int j = -1; j <= 1; j++) {
					for (int i = -1; i <= 1; i++) {
						pix[k] = imgCopy.get(u + i, v + j);
						k++;
					}
				}
				{
					Arrays.sort(pix);
					img.set(u, v, pix[medianPos]);
				}
			}
		}
		// prepare array for edges
		pix = new int[(boxW - 1) * boxW];
				// left edge
				for (int v = 1; v < h - 1; v++) {
					int u = 0;
					int k = 0;
					for (int j = -1; j <= 1; j++) {
						for (int i = 0; i <= 1; i++) {
							pix[k] = imgCopy.get(u + i, v + j);
							k++;
						}
					}
					Arrays.sort(pix);
					img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
				}
				// right edge
				for (int v = 1; v < h - 1; v++) {
					int u = w - 1;
					int k = 0;
					for (int j = -1; j <= 1; j++) {
						for (int i = 0; i <= 1; i++) {
							pix[k] = imgCopy.get(u - i, v + j);
							k++;
						}
					}
					Arrays.sort(pix);
					img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
				}
				// top edge
				for (int u = 1; u < w - 1; u++) {
					int v = 0;
					int k = 0;
					for (int j = 0; j <= 1; j++) {
						for (int i = -1; i <= 1; i++) {
							pix[k] = imgCopy.get(u + i, v + j);
							k++;
						}
					}
					Arrays.sort(pix);
					img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
				}
				// bottom edge 
				for (int u = 1; u < w - 1; u++) {
					int v = h - 1;
					int k = 0;
					for (int j = 0; j <= 1; j++) {
						for (int i = -1; i <= 1; i++) {
							pix[k] = imgCopy.get(u + i, v - j);
							k++;
						}
					}
					Arrays.sort(pix);
					img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
				}
				// prepare array for corners
				pix = new int[(boxW - 1) * (boxW - 1)];
				// do the corners
				pix[0] = imgCopy.get(0, 0);
				pix[1] = imgCopy.get(0, 1);
				pix[2] = imgCopy.get(1, 0);
				pix[3] = imgCopy.get(1, 1);
				Arrays.sort(pix);
				img.set(0, 0, GlitchSort.meanColor(pix[1], pix[2]));
				pix[0] = imgCopy.get(w - 1, 0);
				pix[1] = imgCopy.get(w - 1, 1);
				pix[2] = imgCopy.get(w - 2, 0);
				pix[3] = imgCopy.get(w - 2, 1);
				Arrays.sort(pix);
				img.set(w - 1, 0, GlitchSort.meanColor(pix[1], pix[2]));
				pix[0] = imgCopy.get(0, h - 1);
				pix[1] = imgCopy.get(0, h - 2);
				pix[2] = imgCopy.get(1, h - 1);
				pix[3] = imgCopy.get(1, h - 2);
				Arrays.sort(pix);
				img.set(0, h - 1, GlitchSort.meanColor(pix[1], pix[2]));
				pix[0] = imgCopy.get(w - 1, h - 1);
				pix[1] = imgCopy.get(w - 1, h - 2);
				pix[2] = imgCopy.get(w - 2, h - 1);
				pix[3] = imgCopy.get(w - 2, h - 1);
				Arrays.sort(pix);
				img.set(w - 1, h - 1, GlitchSort.meanColor(pix[1], pix[2]));
				img.updatePixels();
				fitPixels(isFitToScreen, false);
	}
	
	public void mean() {
		int boxW = 3;
		int medianPos = 4;
		backup();
		PImage imgCopy = copyImagePixels(img);
		int w = img.width;
		int h = img.height;
		int[] pix = new int[boxW * boxW];
		float len = pix.length;
		img.loadPixels();
		int r, g, b = 0;
		int farb;
		for (int v = 1; v < h - 1; v++) {
			for (int u = 1; u < w - 1; u++) {
				// int k = 0;
				r = g = b = 0;
				for (int j = -1; j <= 1; j++) {
					for (int i = -1; i <= 1; i++) {
						farb = imgCopy.get(u + i, v + j);;
						r += (farb >> 16) & 0xFF;
						g += (farb >> 8) & 0xFF;
						b += farb & 0xFF;
						// k++;
					}
				}
				{
					img.set(u, v, composeColor((int)(r/len), (int)(g/len), (int)(b/len), 255));
				}
			}
		}
		// prepare array for edges, where we use a median value instead of the mean
		pix = new int[(boxW - 1) * boxW];
				// left edge
				for (int v = 1; v < h - 1; v++) {
					int u = 0;
					int k = 0;
					for (int j = -1; j <= 1; j++) {
						for (int i = 0; i <= 1; i++) {
							pix[k] = imgCopy.get(u + i, v + j);
							k++;
						}
					}
					Arrays.sort(pix);
					img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
				}
				// right edge
				for (int v = 1; v < h - 1; v++) {
					int u = w - 1;
					int k = 0;
					for (int j = -1; j <= 1; j++) {
						for (int i = 0; i <= 1; i++) {
							pix[k] = imgCopy.get(u - i, v + j);
							k++;
						}
					}
					Arrays.sort(pix);
					img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
				}
				// top edge
				for (int u = 1; u < w - 1; u++) {
					int v = 0;
					int k = 0;
					for (int j = 0; j <= 1; j++) {
						for (int i = -1; i <= 1; i++) {
							pix[k] = imgCopy.get(u + i, v + j);
							k++;
						}
					}
					Arrays.sort(pix);
					img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
				}
				// bottom edge 
				for (int u = 1; u < w - 1; u++) {
					int v = h - 1;
					int k = 0;
					for (int j = 0; j <= 1; j++) {
						for (int i = -1; i <= 1; i++) {
							pix[k] = imgCopy.get(u + i, v - j);
							k++;
						}
					}
					Arrays.sort(pix);
					img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
				}
				// prepare array for corners
				pix = new int[(boxW - 1) * (boxW - 1)];
				// do the corners
				pix[0] = imgCopy.get(0, 0);
				pix[1] = imgCopy.get(0, 1);
				pix[2] = imgCopy.get(1, 0);
				pix[3] = imgCopy.get(1, 1);
				Arrays.sort(pix);
				img.set(0, 0, GlitchSort.meanColor(pix[1], pix[2]));
				pix[0] = imgCopy.get(w - 1, 0);
				pix[1] = imgCopy.get(w - 1, 1);
				pix[2] = imgCopy.get(w - 2, 0);
				pix[3] = imgCopy.get(w - 2, 1);
				Arrays.sort(pix);
				img.set(w - 1, 0, GlitchSort.meanColor(pix[1], pix[2]));
				pix[0] = imgCopy.get(0, h - 1);
				pix[1] = imgCopy.get(0, h - 2);
				pix[2] = imgCopy.get(1, h - 1);
				pix[3] = imgCopy.get(1, h - 2);
				Arrays.sort(pix);
				img.set(0, h - 1, GlitchSort.meanColor(pix[1], pix[2]));
				pix[0] = imgCopy.get(w - 1, h - 1);
				pix[1] = imgCopy.get(w - 1, h - 2);
				pix[2] = imgCopy.get(w - 2, h - 1);
				pix[3] = imgCopy.get(w - 2, h - 1);
				Arrays.sort(pix);
				img.set(w - 1, h - 1, GlitchSort.meanColor(pix[1], pix[2]));
				img.updatePixels();
				fitPixels(isFitToScreen, false);
	}

	
  /********************************************/
  /*                                          */
  /*          >>> ARRAY ROTATION <<<          */
  /*                                          */
  /********************************************/
	
	
	/**
	 * Shifts selected RGB color channel this.shift pixels left.
	 */
	public void shiftLeft() {
		shiftImageLeft(shift % img.width);
	}
	
	/**
	 * Shifts selected RGB channel this.shift pixels right.
	 */
	public void shiftRight() {
		shiftImageLeft(img.width - (shift % img.width));
	}
	
	/**
	 * Shifts a selected color channel left by an arbitrary number of pixels.
	 * @param shift   number of pixels to shift selected color channel
	 */
	public void shiftImageLeft(int shift) {
		int[] arr = new int[img.width];
		backup();
		img.loadPixels();
		for (int i = 0; i < rowNums.length; i++) {
			int l = i * img.width;
			int r = l + img.width - 1;
			int u = 0;
			for (int j = l; j < r; j++) {
				arr[u++] = img.pixels[j]; 
			}
			rotateLeft(arr, shift);
			u = 0;
			if (isShiftR) {
				for (int j = l; j <= r; j++) {
					img.pixels[j] = composeColor((arr[u++] >> 16) & 0xFF, (img.pixels[j] >> 8) & 0xFF, (img.pixels[j]) & 0xFF, 255);
				}
			}
			else if (isShiftG) {
				for (int j = l; j <= r; j++) {
					img.pixels[j] = composeColor((img.pixels[j] >> 16) & 0xFF, (arr[u++] >> 8) & 0xFF, (img.pixels[j]) & 0xFF, 255);
				}
			}
			else if (isShiftB) {
				for (int j = l; j <=  r; j++) {
					img.pixels[j] = composeColor((img.pixels[j] >> 16) & 0xFF, (img.pixels[j] >> 8) & 0xFF, (arr[u++]) & 0xFF, 255);
				}
			}
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	
	/**
	 * Shifts a pixels left by an arbitrary number of pixels.
	 * @param shift   number of pixels to shift
	 */
	public void shiftAllLeft(int shift) {
		int[] arr = new int[img.width];
		backup();
		img.loadPixels();
		if (shift < 0) {
			shift = img.width - ((-shift) % img.width);
		}
		else {
			shift = shift % img.width;			
		}
		// println("---- shift = "+ shift);
		for (int i = 0; i < rowNums.length; i++) {
			int l = i * img.width;
			int r = l + img.width - 1;
			int u = 0;
			for (int j = l; j <= r; j++) {
				arr[u++] = img.pixels[j]; 
			}
			rotateLeft(arr, shift);
			u = 0;
			for (int j = l; j <= r; j++) {
				img.pixels[j] = arr[u++];
			}
	}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
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
	
	public void doShift() {
  	boolean shiftDown = cpm.getControl().isShiftDown();
		if (this.isCapsLockDown()) {
			if (shiftDown) shiftScanLeft(-this.shift * 16);
			else shiftScanLeft(-this.shift);
		}
		else {
			if (shiftDown) shiftScanLeft(this.shift * 16);
			else shiftScanLeft(this.shift);
		}
	}
	
	/**
	 * Shifts pixel arrays from zigzag or Hilbert scans left by an arbitrary number of pixels.
	 * @param shift   number of pixels to shift 
	 */
	public void shiftScanLeft(int shift) {
		int order = (int) Math.sqrt(statBufferSize);
		this.statFFTBlockWidth = order;
		PixelScannerINF zz;
		if (isHilbertScan) {
			int depth = (int) (Math.log(order)/Math.log(2));
			zz = new HilbertScanner(depth);
			// println("Hilbert depth = "+ depth);
		}
		else {
			zz = new Zigzagger(order);
			// println("Zigzag order = "+ order);
		}
		int dw = (img.width / order);
		int dh = (img.height / order);
		int w = dw * order;
		int h = dh * order;
		int ow = (img.width - w) / 2;
		int oh = (img.height - h) / 2;
		backup();
		img.loadPixels();
		for (int y = 0; y < dh; y++) {
			for (int x = 0; x < dw; x++) {
				int mx = x * order + ow;
				int my = y * order + oh;
				int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
				// do stuff to pix here
	  		if (shift < 0) {
	  			shift = pix.length - ((-shift) % pix.length);
	  		}
	  		else {
	  			shift = shift % pix.length;			
	  		}
				if (isHilbertScan) {
					rotateLeft(pix, shift);    			
					//rotateLeft(pix, pix.length/16);
				}
				else {
					rotateLeft(pix, shift);    			
				}
				// end stuff
				zz.plant(img.pixels, pix, img.width, img.height, mx, my);
			}
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	
	
	
	/**
	 * TODO fit full image into frame, with no hidden pixels. Works when fitToScreen is true, fails in some 
	 * extreme instances when fitToScreen is false. 
	 * This method is a bottleneck for all screen display--keep it so. 
	 * Fits images that are too big for the screen to the screen, or displays as much of a large image 
	 * as fits the screen if every pixel is displayed. There is still some goofiness in getting the whole
	 * image to display--bottom edge gets hidden by the window. It would be good to have a scrolling window.
	 * 
	 * @param fitToScreen   true if image should be fit to screen, false if every pixel should displayed
	 * @param isFromControlPanel   true if the control panel dispatched the call, false otherwise
	 */
	public void fitPixels(boolean fitToScreen, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (null == cpm.getControl()) {
				println("cpm.getControl() is NULL");
				return;
			}
			if (fitToScreen) ((CheckBox) cpm.getControl().getGroup("fitPixels")).activate(0);
			else ((CheckBox) cpm.getControl().getGroup("fitPixels")).deactivate(0);
		}
		else {
			if (fitToScreen) {
				fitImg = createImage(img.width, img.height, ARGB);
				scaledWidth = fitImg.width;
				scaledHeight = fitImg.height;
				fitImg = copyImagePixels(img);
				// in Processing 2.1, get() no longer seems to copy as it did in Processing 2.0
				// fitImg = img.get();
				// calculate proportions of window and image, 
				// be sure to convert ints to floats to get the math right
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
					// so scale image width to fit the windwo width
					scaledWidth = maxWindowWidth;
					// and scale image height by window width divided by image width
					scaledHeight = Math.round(fitImg.height * (maxWindowWidth / (float)fitImg.width));
				}
				fitImg.resize(scaledWidth, scaledHeight);
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
			// println("scaledWidth = "+ scaledWidth +", scaledHeight = "+ scaledHeight +", frameWidth = "+ frameWidth +", frameHeight = "+ frameHeight);
			isFitToScreen = fitToScreen;
		}
	}
	
    /********************************************/
    /*                                          */
    /*      >>> CONTROL PANEL COMMANDS <<<      */
    /*                                          */
    /********************************************/
	

	/**
	 * Sets glitchSteps.
	 * @param val   the new value for glitchSteps
	 */
	public void setGlitchSteps(float val) {
		val = val < 1 ? 1 : (val > 100 ? 100 : val);
		if (val == glitchSteps) return;
		glitchSteps = val;
		glitchSteps = (int)  Math.floor(glitchSteps);
		((Textlabel)cpm.getControl().getController("glitchStepsLabel")).setValue("Steps = "+ (int)glitchSteps);
		if (null != ranger) {
			ranger.setNumberOfIntervals((int)glitchSteps);
			println("range intervals set to "+ (int) glitchSteps);
		}
		if (verbose) println("glitchSteps = "+ glitchSteps);
	}
	
	/**
	 * Sets glitchSteps.
	 * @param val   the new value for glitchSteps
	 */
	public void incrementGlitchSteps(boolean up) {
		// a workaround to permit us to call setGlitchSteps as a bottleneck method
		int steps = (int) glitchSteps;
		if (up) steps++;
		else steps--;
		setGlitchSteps(steps);
		cpm.getControl().getController("setGlitchSteps").setBroadcast(false);
		cpm.getControl().getController("setGlitchSteps").setValue(glitchSteps);
		cpm.getControl().getController("setGlitchSteps").setBroadcast(true);
	}
	
	/**
	 * Set the value of isGlitchCycle.
	 * @param isCycle   the value to set isCycleGlitch
	 * @param isFromControlPanel   true if called from the control panel, false otherwise
	 */
	public void setCycle(boolean isCycle, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (isCycle) ((CheckBox) cpm.getControl().getGroup("Glitchmode")).activate(0);
			else ((CheckBox) cpm.getControl().getGroup("Glitchmode")).deactivate(0);
		}
		else {
			isCycleGlitch = isCycle;
			if (null != rowNums) resetRowNums();
			// if isCycleGlitch was just set to true, reset ranger's index to 0
			if (isCycleGlitch && null != ranger) {
				ranger.resetCurrentIndex();
				println("range index reset to 0");
			}
			if (verbose) println("isCycleGlitch = "+ isCycleGlitch);
		}
	}
	
	/**
	 * Sets mungeThreshold
	 * @param val   the desired JPEG quality setting (* 100).
	 */
	public void setMungeThreshold(float val) {
		if ((int) val == mungeThreshold) return;
		mungeThreshold = (int) val;
		if (verbose) println("degrade quality = "+ degradeQuality);
	}

	
	/**
	 * Toggles value of isMungeInverted, changes how difference mask operates in munge operation.
	 * @param invert
	 * @param isFromControlPanel
	 */
	public void invertMunge(boolean invert, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (invert) ((CheckBox) cpm.getControl().getGroup("invertMunge")).activate(0);
			else ((CheckBox) cpm.getControl().getGroup("invertMunge")).deactivate(0);
		}
		else {
			isMungeInverted = invert;
			println("isMungeInverted = "+ isMungeInverted);		
		}
	}
	
	/**
	 * Sets degradeQuality
	 * @param val   the desired JPEG quality setting (* 100).
	 */
	public void setQuality(float val) {
		if (val == degradeQuality * 100) return;
		degradeQuality = val * 0.01f;
		println("degrade quality = "+ this.twoPlaces.format(degradeQuality * 100));
	}
	
	/**
	 * Increments or decrements and sets degradeQuality.
	 * @param up   true if increment, false if decrement
	 */
	public void incrementDegradeQuality(boolean up) {
		// a workaround to permit us to call setQuality as a bottleneck method
		float q = (degradeQuality * 100);
		if (up) q++; 
		else q--;
		setQuality(constrain(q, 0, 100));
		cpm.getControl().getController("setQuality").setBroadcast(false);
		cpm.getControl().getController("setQuality").setValue(degradeQuality * 100);
		cpm.getControl().getController("setQuality").setBroadcast(true);
	}
	

	
	/**
	 * TODO
	 * Sets the sorting method (QUICK, SHELL, BUBBLE, INSERT) used by sortTool.
	 * @param type   the type of sorting method to use
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setSorter(SorterType type, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			((RadioButton) cpm.getControl().getGroup("setSorter")).activate(type.name());
		}
		else {
			sortTool.setSorter(type);
			breakPoint = sortTool.sorter.getBreakPoint();
			cpm.getControl().getController("setBreakpoint").setBroadcast(false);
			cpm.getControl().getController("setBreakpoint").setValue(breakPoint);
			((Textlabel)cpm.getControl().getController("breakpointLabel")).setValue("Breakpoint: " + sortTool.sorter.getSorterType().toString());
			cpm.getControl().getController("setBreakpoint").setBroadcast(true);
			println(type.name() +" sorter loaded");
		}
	}

	/**
	 * Sets the order of components used to sort pixels.
	 * @param index   index number of CompOrder values 
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setCompOrder(int index, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			((RadioButton) cpm.getControl().getGroup("setCompOrder")).activate(index);
		}
		else {
			compOrderIndex = index;
			compOrder = CompOrder.values()[compOrderIndex];
			println("Color component order set to "+ compOrder.name());
		}
	}
	
	/**
	 * @param val   true if sorting should be in ascending order, false otherwise 
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setAscending(boolean val, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (val) ((CheckBox) cpm.getControl().getGroup("Sorting")).activate("Ascending");
			else ((CheckBox) cpm.getControl().getGroup("Sorting")).deactivate("Ascending");
		}
		else {
			if (isAscendingSort == val) return;
			isAscendingSort = val;
			println("Ascending sort order is "+ isAscendingSort);
		}
	}
		
	/**
	 * @param val   true if random breaks in sorting ("glitches") are desired, false otherwise.
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setRandomBreak(boolean val, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (val) ((CheckBox) cpm.getControl().getGroup("Sorting")).activate("Break");
			else ((CheckBox) cpm.getControl().getGroup("Sorting")).deactivate("Break");
		}
		else {
			if (randomBreak == val) return;
			randomBreak = val;
			sortTool.setRandomBreak(randomBreak);
			println("randomBreak is "+ randomBreak);
		}
	}

	/**
	 * @param val   true if color channels should be swapped when sorting (more glitching). 
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setIsSwapChannels(boolean val, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (val) ((CheckBox) cpm.getControl().getGroup("Sorting")).activate("Swap");
			else ((CheckBox) cpm.getControl().getGroup("Sorting")).deactivate("Swap");
		}
		else {
			if (isSwapChannels == val) return;
			isSwapChannels = val;
			println("Swap color channels is "+ isSwapChannels);
		}
	}
	
	/**
	 * @param newSwap   the swap value to set, determinse which channels are swapped.
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setSwap(SwapChannel newSwap, boolean isFromControlPanel) {
		if (swap == newSwap) return;
		if (!isFromControlPanel) {
			RadioButton rb1 = (RadioButton)cpm.getControl().getGroup("setSourceChannel");
			RadioButton rb2 = (RadioButton)cpm.getControl().getGroup("setTargetChannel");
			switch (newSwap) {
			case RR: {
				rb1.activate(0);
				rb2.activate(0);
				break;
			}
			case RG: {
				rb1.activate(0);
				rb2.activate(1);
				break;
			}
			case RB: {
				rb1.activate(0);
				rb2.activate(2);
				break;
			}
			case GR: {
				rb1.activate(1);
				rb2.activate(0);
				break;
			}
			case GG: {
				rb1.activate(1);
				rb2.activate(1);
				break;
			}
			case GB: {
				rb1.activate(1);
				rb2.activate(2);
				break;
			}
			case BR: {
				rb1.activate(2);
				rb2.activate(0);
				break;
			}
			case BG: {
				rb1.activate(2);
				rb2.activate(1);
				break;
			}
			case BB: {
				rb1.activate(2);
				rb2.activate(2);
				break;
			}
			}
		}
		else {
			swap = newSwap;
			println("swap is "+ swap.name());
		}
	}
	
	public void setSwapWeight(float newSwapWeight) {
		if (newSwapWeight == swapWeight) return;
		swapWeight = newSwapWeight/100;
		// println("-- new swap weight = "+ swapWeight);
	}

	
	public void setZigzagStyle(ZigzagStyle style, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			((RadioButton) cpm.getControl().getGroup("setZigzagStyle")).activate(style.ordinal());
		}
		else {
			zigzagStyle = style;
			println("-- zizagStyle = "+ style.name());
		}
	}	
	
	/**
	 * adjusts control panel text to reflect updated quantization value
	 * @param val   the new quantization value
	 */
	public void setColorQuantize(float val) {
		if (val == colorQuantize) return;
		colorQuantize = (int) val;
		((Textlabel)cpm.getControl().getController("colorQuantizeLabel")).setValue("Colors = "+ colorQuantize);
		// if (verbose) 
		println("colorQuantize = "+ colorQuantize);
	}
	
	/**
	 * Sets colorQuantize.
	 * @param val   the new value for colorQuantize
	 */
	public void incrementColorQuantize(boolean up) {
		// a workaround to permit us to call setColorQuantize as a bottleneck method
		int val = (int) colorQuantize;
		if (up && val < 128) val++;
		else if (val > 2) val--;
		setColorQuantize(val);
		cpm.getControl().getController("setColorQuantize").setBroadcast(false);
		cpm.getControl().getController("setColorQuantize").setValue(colorQuantize);
		cpm.getControl().getController("setColorQuantize").setBroadcast(true);
	}
	
	/**
	 * adjusts control panel text to reflect updated lineCount value
	 * @param val   the new lineCount value
	 * TODO fix somewhat elusive errors in setLineCount, setLineAdd and setLineTick behavior
	 */
	public void setLineCount(float val) {
		if (val == lineCount) return;
		lineCount = (int) val;
		((Textlabel)cpm.getControl().getController("lineCountLabel")).setValue("Lines = "+ lineCount);
		// if (verbose) 
		println("lineCount = "+ lineCount);
	}

	/**
	 * adjusts control panel text to reflect updated lineCount value
	 * @param val   the new lineTick value
	 */
	public void setLineTick(float val) {
		if (val == lineTick) return;
		lineTick = (int) val;
		if (lineTick == 0 && lineAdd == 0) {
			lineAdd = 1;
			cpm.getControl().getController("setLineAdd").setBroadcast(false);
			cpm.getControl().getController("setLineAdd").setValue(lineAdd);
			cpm.getControl().getController("setLineAdd").setBroadcast(true);
		}
		setLineCount(lineTick + lineAdd);
	}

	/**
	 * adjusts control panel text to reflect updated lineCount value
	 * @param val   the new lineTick value
	 */
	public void setLineAdd(float val) {
		if (val == lineAdd) return;
		lineAdd = (int) val;
		if (lineTick == 0 && lineAdd == 0) {
			lineAdd = 1;
		}
		setLineCount(lineTick + lineAdd);
	}

	/**
	 * Sets LineCount.
	 * @param val   the new value for lineCount
	 */
	public void incrementLineCount(boolean up) {
		// a workaround to permit us to call setGlitchSteps as a bottleneck method
		int val = (int) lineCount;
		if (up && val < 1024) val++;
		else if (val > 1) val--;
		lineTick = (int) Math.floor(val / 128) * 128;
		lineAdd = val - lineTick;
		if (lineTick == 0 && lineAdd == 0) {
			lineAdd = 1;
		}
		setLineCount(lineAdd + lineTick);
		cpm.getControl().getController("setLineTick").setBroadcast(false);
		cpm.getControl().getController("setLineTick").setValue(lineTick);
		cpm.getControl().getController("setLineTick").setBroadcast(true);
		cpm.getControl().getController("setLineAdd").setBroadcast(false);
		cpm.getControl().getController("setLineAdd").setValue(lineAdd);
		cpm.getControl().getController("setLineAdd").setBroadcast(true);
	}
	
	
	/******************************************************************************/
	/*                                                                            */
	/*                      FFT Control Panel Handlers                            */
	/*                                                                            */
	/******************************************************************************/

	
	/**
	 * Sets the values of equalizer-controlled FFT settings.
	 * @param isBrightness   true if brightness channel is affect by FFT, false otherwise
	 * @param isHue          true if hue channel is affect by FFT, false otherwise
	 * @param isSaturation   true if saturation channel is affect by FFT, false otherwise
	 * @param isRed          true if red channel is affect by FFT, false otherwise
	 * @param isGreen        true if green channel is affect by FFT, false otherwise
	 * @param isBlue         true if blue channel is affect by FFT, false otherwise
	 * @param isFromControlPanel   true if called from the control panel, false otherwise
	 */
	public void setEqChan(boolean isBrightness, boolean isHue, boolean isSaturation,
			boolean isRed, boolean isGreen, boolean isBlue, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			// println("setting equalizer HSB/RGB");
			if (isBrightness) ((CheckBox) cpm.getControl().getGroup("ChanEq")).activate(0);
			else ((CheckBox) cpm.getControl().getGroup("ChanEq")).deactivate(0);
			if (isHue) ((CheckBox) cpm.getControl().getGroup("ChanEq")).activate(1);
			else ((CheckBox) cpm.getControl().getGroup("ChanEq")).deactivate(1);
			if (isSaturation) ((CheckBox) cpm.getControl().getGroup("ChanEq")).activate(2);
			else ((CheckBox) cpm.getControl().getGroup("ChanEq")).deactivate(2);
			if (isRed) ((CheckBox) cpm.getControl().getGroup("ChanEq")).activate(3);
			else ((CheckBox) cpm.getControl().getGroup("ChanEq")).deactivate(3);
			if (isGreen) ((CheckBox) cpm.getControl().getGroup("ChanEq")).activate(4);
			else ((CheckBox) cpm.getControl().getGroup("ChanEq")).deactivate(4);
			if (isBlue) ((CheckBox) cpm.getControl().getGroup("ChanEq")).activate(5);
			else ((CheckBox) cpm.getControl().getGroup("ChanEq")).deactivate(5);
		}
		else {
			isEqGlitchBrightness = isBrightness;
			isEqGlitchHue = isHue;
			isEqGlitchSaturation = isSaturation;
			isEqGlitchRed = isRed;
			isEqGlitchGreen = isGreen;
			isEqGlitchBlue = isBlue;
			if (verbose) 
			{
				println("Equalizer FFT: ");
				print("  Brightness = "+ isEqGlitchBrightness);
				print(", Hue = "+ isEqGlitchHue);
				print(", Saturation = "+ isEqGlitchSaturation);
				print(", Red = "+ isEqGlitchRed);
				print(", Green = "+ isEqGlitchGreen);
				println(", Blue = "+ isEqGlitchBlue);
			}
		}
	}
	
	/**
	 * Sets the values of statistically controlled FFT settings.
	 * @param isBrightness   true if brightness channel is affect by FFT, false otherwise
	 * @param isHue          true if hue channel is affect by FFT, false otherwise
	 * @param isSaturation   true if saturation channel is affect by FFT, false otherwise
	 * @param isRed          true if red channel is affect by FFT, false otherwise
	 * @param isGreen        true if green channel is affect by FFT, false otherwise
	 * @param isBlue         true if blue channel is affect by FFT, false otherwise
	 * @param isFromControlPanel   true if called from the control panel, false otherwise
	 */
	public void setStatChan(boolean isBrightness, boolean isHue, boolean isSaturation,
			boolean isRed, boolean isGreen, boolean isBlue, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			// println("setting statistical HSB/RGB");
			if (isBrightness) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(0);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(0);
			if (isHue) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(1);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(1);
			if (isSaturation) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(2);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(2);
			if (isRed) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(3);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(3);
			if (isGreen) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(4);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(4);
			if (isBlue) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(5);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(5);
		}
		else {
			isStatGlitchBrightness = isBrightness;
			isStatGlitchHue = isHue;
			isStatGlitchSaturation = isSaturation;
			isStatGlitchRed = isRed;
			isStatGlitchGreen = isGreen;
			isStatGlitchBlue = isBlue;
			if (verbose) 
			{
				println("Statistical FFT: ");
				print("  Brightness = "+ isStatGlitchBrightness);
				print(", Hue = "+ isStatGlitchHue);
				print(", Saturation = "+ isStatGlitchSaturation);
				print(", Red = "+ isStatGlitchRed);
				print(", Green = "+ isStatGlitchGreen);
				println(", Blue = "+ isStatGlitchBlue);
			}
		}
	}
	
	public void setStatChan(boolean[] state, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			// println("setting statistical HSB/RGB");
			if (state[0]) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(0);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(0);
			if (state[1]) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(1);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(1);
			if (state[2]) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(2);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(2);
			if (state[3]) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(3);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(3);
			if (state[4]) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(4);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(4);
			if (state[5]) ((CheckBox) cpm.getControl().getGroup("ChanStat")).activate(5);
			else ((CheckBox) cpm.getControl().getGroup("ChanStat")).deactivate(5);
		}
		else {
			isStatGlitchBrightness = state[0];
			isStatGlitchHue = state[1];
			isStatGlitchSaturation = state[2];
			isStatGlitchRed = state[3];
			isStatGlitchGreen = state[4];
			isStatGlitchBlue = state[5];
			if (verbose) 
			{
				println("Statistical FFT: ");
				print("  Brightness = "+ isStatGlitchBrightness);
				print(", Hue = "+ isStatGlitchHue);
				print(", Saturation = "+ isStatGlitchSaturation);
				print(", Red = "+ isStatGlitchRed);
				print(", Green = "+ isStatGlitchGreen);
				println(", Blue = "+ isStatGlitchBlue);
			}
		}
	}


	/**
	 * Gets the values of statistically controlled FFT settings and returns them as an array of boolean.
	 * return   an array of boolean values for brightness, hue, saturation, red, green, blue. 
	 */
	public boolean[] getStatChan() {
		boolean[] statFFTSettings = new boolean[6];
		statFFTSettings[0] = isStatGlitchBrightness;
		statFFTSettings[1] = isStatGlitchHue;
		statFFTSettings[2] = isStatGlitchSaturation;
		statFFTSettings[3] = isStatGlitchRed;
		statFFTSettings[4] = isStatGlitchGreen;
		statFFTSettings[5] = isStatGlitchBlue;
		return statFFTSettings;
	}


//  no longer in use, here for backup
//	/**
//	 * Toggles low frequency cut setting in statistical FFT control.
//	 * @param isCut
//	 * @param isFromControlPanel
//	 */
//	public void setLowFrequencyCut(boolean isCut, boolean isFromControlPanel) {
//		if (!isFromControlPanel) {
//			// println("setting low cut");
//			if (isCut) ((CheckBox) cpm.getControl().getGroup("LowFreqCut")).activate(0);
//			else ((CheckBox) cpm.getControl().getGroup("LowFreqCut")).deactivate(0);
//		}
//		else {
//			isLowFrequencyCut = isCut;
//			println("isLowFrequencyCut = "+ isLowFrequencyCut);
//		}
//	}

	/**
	 * Toggles linked ratios statistical FFT control.
	 * @param isCut
	 * @param isFromControlPanel
	 */
	public void setLink(boolean isLink, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			// println("setting link");
			if (isLink) ((CheckBox) cpm.getControl().getGroup("Link")).activate(0);
			else ((CheckBox) cpm.getControl().getGroup("Link")).deactivate(0);
		}
		else {
			isCutLinkedToBoost = isLink;
			println("isLink = "+ isCutLinkedToBoost);
		}
	}

	/**
	 * Sets fftBlockWidth. 
	 * @param val   the new value for eqGain
	 */
	public void setFFTBlockWidth(float val) {
		val = val < 3 ? 3 : (val > 10 ? 10 : val);
		int temp = (int) Math.pow(2, (int) val);
		if (temp == statFFTBlockWidth) return;
		resetFFT(temp);
	}
	
    /**
     * Sets left and right eQ bounds in response to control panel.
     * @param val   a value forwarded by ControlP5 that we will ignore (just in this case)
     */
	public void setStatEqRange(float val) {
		// here's one way to retrieve the values of the range controller
		Range r1 = (Range) cpm.getControl().getController("setStatEqRange");
		if (!r1.isInside()) {
			return;
		}
		leftBound = r1.getArrayValue()[0];
		rightBound = r1.getArrayValue()[1];
	}

	/**
	 * Sets value of leftBound used in statistical FFT interface
	 * @param newLeftBound
	 */
	public void setLeftBound(float newLeftBound) {
		if (newLeftBound == leftBound) return;
		leftBound = newLeftBound;
	}

	/**
	 * Sets value of rightBound used in statistical FFT interface
	 * @param newRightBound
	 */
	public void setRightBound(float newRightBound) {
		if (newRightBound == rightBound) return;
		rightBound = newRightBound;
	}

	/**
	 * Sets value of boost used in statistical FFT interface
	 * @param newBoost
	 */
	public void setBoost(float newBoost) {
		newBoost = roundToPlaces(newBoost, 4);
		if (this.isCutLinkedToBoost) {
			if (newBoost == boost) return;
			boost = newBoost;		
			if (newBoost != 0) {
				cut = (1.0f/newBoost);
				Numberbox n5 = (Numberbox) cpm.getControl().getController("setCut");
				if (n5.getValue() != cut) {
					n5.setBroadcast(false);
					n5.setValue(cut);
					n5.setBroadcast(true);
					// println("IN:  cut =", cut, "boost =", boost);
				}
			}
		}
		else {
			if (newBoost == boost) return;
			boost = newBoost;		
		}
	}
	
	/**
	 * Sets value of cut used in statistical FFT interface
	 * @param newCut
	 */
	public void setCut(float newCut) {
		newCut = roundToPlaces(newCut, 4);
		if (this.isCutLinkedToBoost) {
			if (newCut == cut) return;
			cut = newCut;
			if (newCut != 0) {
				boost = (1.0f/newCut);
				Numberbox n4 = (Numberbox) cpm.getControl().getController("setBoost");
				if (n4.getValue() != boost) {
					n4.setBroadcast(false);
					n4.setValue(boost);
					n4.setBroadcast(true);
					// println("OUT cut =", cut, "boost =", boost);
				}
			}
		}
		else {
			if (newCut == cut) return;
			cut = newCut;
		}
	}
	
	/**
	 * @param val   true if Hilbert curve should be used for FFT, false if zigzag should be used. 
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setIsHilbertScan(boolean val, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (val) ((CheckBox) cpm.getControl().getGroup("hilbert")).activate(0);
			else ((CheckBox) cpm.getControl().getGroup("hilbert")).deactivate(0);
		}
		else {
			if (isHilbertScan == val) return;
			isHilbertScan = val;
			if (isHilbertScan) println("Hilbert scan will be used for FFT");
			else println("Zigzag scan will be used for FFT");
		}
	}
	
	public void shiftEqBinsLeft() {
		float[] bins = cpm.getEqualizerValues();
		for (int i = 0; i < bins.length - 1; i++) {
			bins[i] = bins[i + 1];
		}
		setEqBins(bins);
	}

	public void shiftEqBinsRight() {
		float[] bins = cpm.getEqualizerValues();
		for (int i = bins.length -1; i > 0; i--) {
			bins[i] = bins[i - 1];
		}
		setEqBins(bins);
	}

	public void setEqBins(float[] bins) {
		for (int i = 0; i < bins.length; i++) {
			String token = cpm.sliderIdentifier + noPlaces.format(i);
			Slider slider = (Slider) cpm.getControl().getController(token);
			slider.setValue(bins[i]);
		}	
	}
	
	
  /********************************************/
  /*                                          */
  /*            >>> Color Shift <<<           */
  /*                                          */
  /********************************************/
	
	
	
	/**
	 * Sets the shift value used when shifting a color channel right or left. 
	 * See loadMungePanel in ControlPanelManager.
	 * @param newShift   new value for shift
	 */
	public void setShift(float newShift) {
		int val = Math.round(newShift);
		if (val == shift) return;
		shift = val;
	}
	
	private float twoPlacesRound(float v) {
		return Math.round(v * 100.0f) / 100.0f;
	}
	private float threePlacesRound(float v) {
		return Math.round(v * 1000.0f) / 1000.0f;
	}
	private float roundToPlaces(float v, float places) {
		double m = Math.pow(10, places);
		return (float) (Math.round(v * m) / m);
	}
	
	
    /********************************************/
    /*                                          */
    /*              >>> UTILITY <<<             */
    /*                                          */
    /********************************************/
	
	/**
	 * Shuffles an array of integers into random order.
	 * Implements Richard Durstenfeld's version of the Fisher-Yates algorithm, popularized by Donald Knuth.
	 * see http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
	 * @param intArray an array of <code>int</code>s, changed on exit
	 */
	public void shuffle(int[] intArray) {
		for (int lastPlace = intArray.length - 1; lastPlace > 0; lastPlace--) {
			// Choose a random location from 0..lastPlace
			int randLoc = (int) (random(lastPlace + 1));
			// Swap items in locations randLoc and lastPlace
			int temp = intArray[randLoc];
			intArray[randLoc] = intArray[lastPlace];
			intArray[lastPlace] = temp;
		}
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
	 * Creates a Processing ARGB color from r, g, b, values in an array. 
	 * @param comp   array of 3 integers in range 0..255, for red, green and blue components of color
	 *               alpha value is assumed to be 255
	 * @return       a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static int composeColor(int[] comp) {
		return 255 << 24 | comp[0] << 16 | comp[1] << 8 | comp[2];
	}
	
	/**
	 * Returns the largest difference between the components of two colors. 
	 * If the value returned is 0, colors are identical.
	 * @param color1
	 * @param color2
	 * @return
	 */
	public static int maxColorDiff(int color1, int color2) {
		int rDiff = Math.abs(((color1 >> 16) & 0xFF) - ((color2 >> 16) & 0xFF));
		int gDiff = Math.abs(((color1 >> 8) & 0xFF) - ((color2 >> 8) & 0xFF));
		int bDiff = Math.abs(((color1) & 0xFF) - ((color2) & 0xFF));
		return Math.max(Math.max(rDiff, gDiff), bDiff);
	}
	
	public static int meanColor(int argb1, int argb2) {
		int[] comp1 = GlitchSort.rgbComponents(argb1);
		int[] comp2 = GlitchSort.rgbComponents(argb2);
		for (int i = 0; i < comp1.length; i++) {
			comp1[i] = (int) ((comp1[i] + comp2[i]) * 0.5f);
		}
		return GlitchSort.composeColor(comp1);
	}
	

    /********************************************/
    /*                                          */
    /*             >>> FILE I/O <<<             */
    /*                                          */
    /********************************************/
	

	/**
	 * Bottleneck method to update file tracking variables for automated saving, reloading, etc. 
	 * @param selectedFile
	 */
	public void setNewFileState(File selectedFile) {
		displayFile = selectedFile;
		String path = selectedFile.getAbsolutePath();
		filePath = path.substring(0, path.lastIndexOf("/")) +"/";
		fileBaseName = getNewBaseName(selectedFile);
		printFileInfo();
	}
	
	/**
	 * Outputs information on the current displayFile and orignalFile and associated variables.
	 */
	public void printFileInfo() {
		println("fileBaseName = "+ fileBaseName);
		println("next file name is "+ getNextFileName());
		println("filePath = "+ filePath);
		println("displayFile = "+ displayFile.getAbsolutePath());
		if (null != originalFile) println("originalFile = "+ originalFile.getAbsolutePath());
		if (null == fileList) getAllFiles(new File(filePath));
	}
	
	File[] fileList;
	int filePtr = 0;
	private void getAllFiles(File dir) {
		fileList = dir.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".png");
			}
		});
		Arrays.sort(fileList, new Comparator<File>(){
			public int compare(File f1, File f2){
				// for descending order
				/* return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified()); */
				// sort by name
				return f1.getName().compareToIgnoreCase(f2.getName());
			}
		});
		int count = 0;
		for(File f : fileList) {
			if (f.isDirectory()) {
				// skip
			}
			else if (f.isFile()) {
				println(f.getName());
				if (count++ > 10) return;
			}
		}
	}
	
	private void stepFile() {
		if (null != fileList) {
			if (filePtr >= fileList.length - 1) filePtr = 0;
			else filePtr++;
			displayFileSelected(fileList[filePtr]);
		}
	}
	
	/**
	 * @return   a new, probably unique, file name composed from fileBaseName, timestamp and fileCount.
	 */
	public String getNextFileName() {
		return fileBaseName +"_"+ timestamp +"_"+ fourFrontPlaces.format(fileCount) +".png";
	}
	
	/**
	 * Parses selectedFile's short name to obtain a value for fileBaseName. 
	 * Parsing truncates all text chunks that consist of an underline followed
	 * by one or more numbers, stopping at the first non-matched chunk.
	 * @param selectedFile
	 * @return
	 */
	public String getNewBaseName(File selectedFile) {
		String path = selectedFile.getAbsolutePath();
		// get the short file name minus its extension 
		String newName = path.substring(path.lastIndexOf("/") + 1, path.length()).split("\\.")[0];
		Pattern p = Pattern.compile("\\s");
		Matcher m = p.matcher(newName);
		// remove all spaces
		newName = m.replaceAll("");
		p = Pattern.compile("_\\d+");
		m = p.matcher(newName);
		// look for pattern (underscore followed by numbers)
		if (m.find()) {
			// if we found the pattern, delete trailing instances to form the new base name
			int i = 0;
			String parts[] = newName.split("_");
			// ignore leading underscores
			while (i < parts.length && "".equals (parts[i])) i++;
			// put the first non-underscore part into buf
			StringBuffer buf = new StringBuffer(parts[i]);
			p = Pattern.compile("\\d+");
			// look for parts that only contain numbers, starting at the end
			for (i = parts.length - 1; i > 0; i--) {
				String part = parts[i];
				m = p.matcher(part);
				// break at the first non-matching part
				if (!m.matches()) {
					break;
				}
			}
			// assemble the new name from parts up to the 
			// first trailing (underscore followed by numbers) pattern
			int stop = i;
			for (i = 1; i <= stop; i++) {
				String part = parts[i];
				if (!"".equals(part)) buf.append("_"+ part);
			}
			newName = buf.toString();
		}
		return newName;
	}
	
	
	/**
	 * Saves current display image automatically, without a dialog, using fileBaseName
	 * timestamp and fileCount to create a unique file name. Increments fileCount.
	 * If fileBaseName is null or this is a newly opened file, shows a "save as..." dialog.
	 */
	public void saveFile() {
		if (null == fileBaseName || isNewFile) {
			saveFileAs();
		}
		else {
			String fName = getNextFileName();
			fileCount++;
			println("saving to "+ fName);
			img.save(filePath + fName);
			displayFile = new File(filePath + fName);
		}
	}

	/**
	 * Saves current display to a file without changing the value of dispalyFile.
	 * If fileBaseName is null, shows a "Save as..." dialog, otherwise no dialog appears. 
	 */
	public void saveFileCopy() {
		if (null == fileBaseName || isNewFile) {
			saveFileAs();
		}
		else {
			String fName = getNextFileName();
			fileCount++;
			println("saving copy to "+ fName);
			img.save(filePath + fName);
		}
	}

	/**
	 * Public method for saving current image into a file with a new name obtained from 
	 * a save file dialog. Calls private bottleneck method saveFileAs(File selectedFile).
	 */
	public void saveFileAs() {
		if (null == fileBaseName) fileBaseName = "untitled";
		if (null == filePath) filePath = sketchPath +"/";
		if (null == displayFile) {
			String fName = filePath + fileBaseName +"_"+ timestamp;
			saveFileAs(new File(fName));
		}
		else {
			// usual formula is: String fName = fileBaseName +"_"+ timestamp +"_"+ fileCount +".png";
			// but we're creating a new basename (offering the old one as a suggestion)
			String fName = filePath + getNewBaseName(displayFile) +"_"+ timestamp;
			saveFileAs(new File(fName));
		}
	}
	
	/**
	 * Bottleneck method called by saveFileAs().
	 * @param selectedFile
	 */
	private void saveFileAs(File selectedFile) {
		selectOutput("New file (base name + timestamp):", "outputSelected", selectedFile);
	}
	
	/**
	 * Callback method for selectOutput(), saves selectedFile to a location chosen by the user, 
	 * sets default path for output to directory where file is saved. The name of the file will 
	 * be used as the "base name" in subsequent sequential saves. 
	 * @param selectedFile  file the use selected from a dialog
	 */
	public void outputSelected(File selectedFile) {
		if (selectedFile == null) {
			println("Window was closed or the user hit cancel.");
		} 
		else {
			noLoop();
			String oldFileBaseName = fileBaseName;
			setNewFileState(selectedFile);
			// must set isNewFile to false before calling save
			// bad quirk??
			isNewFile = false;			
			// save file, but not as a copy
			saveFile();
			// now, if the fileBaseName is null or has changed, set new file
			// note that we do not reset fileCount except manually
			if (null == oldFileBaseName 
					|| !(oldFileBaseName.equals(fileBaseName))) {
				isNewFile = true;
				// new timestamp, too
				timestamp = getTimestamp();
			}
			// set originalFile to new displayFile to permit reloading
			// originalFile = displayFile;
			loop();
		}
	}

	/**
	 * Callback method for chooseFolder(), handles user's actions in open file dialog.
	 * If user selected a file, it is used to construct the fileList variable. filePtr is reset to 0.
	 * Call to getAllFiles outputs first ten files in folder.
	 * @param selectedFile
	 */
	public void setFileList(File selectedFile) {
		if (null != selectedFile) {
			noLoop();
			String path = selectedFile.getAbsolutePath();
			String folderPath = path.substring(0, path.lastIndexOf("/")) +"/";
			this.getAllFiles(new File(folderPath));
			loop();
		}
		else {
			println("No file was selected");
		}
	}

	/**
	 * present a file selection dialog to open a new file
	 */
	public void chooseFile() {
		selectInput("Choose an image file:", "displayFileSelected");
	}
	
	/**
	 * Callback method for selectInput(), handles user's actions in open file dialog.
	 * If user selected a file, it is opened and several variables are set, including 
	 * originalFile, which should always contain most recently opened file, and 
	 * fileBaseName, used to construct file names for automatic saves. The fileCount
	 * variable is not changed. 
	 * @param selectedFile
	 */
	public void displayFileSelected(File selectedFile) {
		File oldFile = displayFile;
		if (null != selectedFile && oldFile != selectedFile) {
			noLoop();
			// a new file was opened, it will be the new original
			originalFile = selectedFile;
			String oldFileBaseName = fileBaseName;
			setNewFileState(selectedFile);
			// set isNewFile to true every time we open a new file with a different base name
			if (null == oldFileBaseName 
					|| !(oldFileBaseName.equals(fileBaseName))) {
				isNewFile = true;
				// new timestamp, too
				timestamp = getTimestamp();
			}
			loadFile();
			if (isFitToScreen) fitPixels(true, false);
			loop();
		}
		else {
			println("No file was selected");
		}
	}
	
	/**
	 * present a file selection dialog to open a new file
	 */
	public void chooseFolder() {
		selectInput("Choose a file in desired folder:", "setFileList");
	}
	
	/**
	 * loads a file into variable img.
	 */
	public void loadFile() {
		println("\nselected file "+ displayFile.getAbsolutePath());
		img = loadImage(displayFile.getAbsolutePath());
		transX = transY = 0;
		fitPixels(isFitToScreen, false);
		println("image width "+ img.width +", image height "+ img.height);
		resetRowNums(img.height);
		if (null == ranger) {
			ranger = new RangeManager(img.height, (int) glitchSteps);
		}
		else {
			ranger.setRange(img.height);
		}
		analyzeEq(false);
	}

	/**
	 * loads original file into variable img.
	 */
	public void loadOriginalFile() {
		if (null == originalFile) {
			println("No orignal file is available for reloading. Try opening a file first.");
			return;
		}
		println("\noriginal file "+ originalFile.getAbsolutePath());
		displayFile = originalFile;
		img = loadImage(displayFile.getAbsolutePath());
		transX = transY = 0;
		fitPixels(isFitToScreen, false);
		println("image width "+ img.width +", image height "+ img.height);
		resetRowNums(img.height);
		if (null == ranger) {
			ranger = new RangeManager(img.height, (int) glitchSteps);
		}
		else {
			ranger.setRange(img.height);
		}
		analyzeEq(false);
	}


	/**
	 * @param image          the image to degrade
	 * @param quality        the desired JPEG quality
	 * @throws IOException   error thrown by file i/o
	 */
	public void degradeImage(PImage image, float quality) throws IOException {
		Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
		ImageWriter writer = (ImageWriter)iter.next();
		ImageWriteParam iwp = writer.getDefaultWriteParam();
		iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwp.setCompressionQuality(quality);   	
		try {
			BufferedImage bi =  (BufferedImage) image.getNative();
			String shortName = displayFile.getName();
			String[] parts = shortName.split("\\.");
			// String fName = parts[0] +"_q"+ Math.round(quality * 100) +".jpg";
			// just save one degrade file per image
			String fName = projPath +"/"+ parts[0] +"_degrade" +".jpg";
			File temp = new File(savePath(fName));
			FileImageOutputStream output = new FileImageOutputStream(temp);
			writer.setOutput(output);
			IIOImage outImage = new IIOImage(bi, null, null);
			writer.write(null, outImage, iwp);
			writer.dispose();
			PImage newImage = loadImage(temp.getAbsolutePath());
			img = newImage;
			println("degraded "+ fName);
		}
		catch (FileNotFoundException e) {
			println("file not found error " + e);
		}
		catch (IOException e) {
			println("IOException "+ e);
		}
	}
	
	public void saveToAI() {
		String aiFilename = fileBaseName +"_"+ timestamp +"_"+ fileCount +".ai";
	  println("saving Adobe Illustrator file " + aiFilename + "...");
	  String path = (null == filePath) ? projPath +"/" : filePath;
	  PrintWriter output = createWriter(path + aiFilename);
	  DocumentComponent doc = new DocumentComponent("GlitchSort Export");
	  // get its palette
	  Palette pal = doc.getPalette();
	  // add black, white, and gray to the palette
	  pal.addBlackWhiteGray();
	  // include some information for Illustrator's header
	  doc.setCreator("Ignotus");
	  doc.setOrg("IgnoStudio");
	  // set width and height of the document
	  doc.setWidth(img.width);
	  doc.setHeight(img.height);
		int h = img.height;
		int w = img.width;
		pushStyle();
		/*
		// use stroke to compensate for rasterizing artifacts
		strokeWeight(0.05f);
		*/
		img.loadPixels();
		// TODO count pixels with identical color
		// int r = 0;
	  for (int y = 0; y < h; y++) {
	  	int yw = y * w;
	  	for (int x = 0; x < w; x++) {
	  		int i = x + yw;
	  		int c = img.pixels[i];
	  		fill(c);
	  		stroke(c);
	  		// enlarge rectangle slightly to reduce anti-aliasing artifacts when rasterizing
	  		doc.add(BezRectangle.makeLeftTopWidthHeight(x, y, 1.05f, 1.05f));
	  	}
	  }
	  img.updatePixels();
	  popStyle();
	  doc.write(output);
	}
	
	public void getColors() {
		String aiFilename = fileBaseName +"_"+ timestamp +"_"+ fileCount +".ai";
		println("saving Adobe Illustrator file " + aiFilename + "...");
		String path = (null == filePath) ? projPath +"/" : filePath;
		PrintWriter output = createWriter(path + aiFilename);
		DocumentComponent doc = new DocumentComponent("GlitchSort Export");
		// get its palette
		Palette pal = doc.getPalette();
		// add black, white, and gray to the palette
		pal.addBlackWhiteGray();
		// include some information for Illustrator's header
		doc.setCreator("Ignotus");
		doc.setOrg("IgnoStudio");
		// set width and height of the document
		doc.setWidth(img.width);
		doc.setHeight(img.height);
		int h = img.height;
		int w = img.width;
		pushStyle();
		img.loadPixels();
		for (int y = 0; y < h; y++) {
			int yw = y * w;
			for (int x = 0; x < w; x++) {
				int i = x + yw;
				int c = img.pixels[i];
				pal.addColor(c);
				if (pal.getColors().length > 254) {
					println("255 color limit reached");
					break;
				}
			}
		}
		img.updatePixels();
		popStyle();
		doc.write(output);
		int[] palette = pal.getColors();
		print("int[] pal = { ");
		for (int i = 0; i < palette.length; i++) {
			int[] comp = rgbComponents(palette[i]);
			print(comp[0] +","+ comp[1] +","+ comp[2]);
			if (i < palette.length - 1) print(", ");
		}
		println(" };");
	}



    /********************************************/
    /*                                          */
    /*           >>> RANGE MANAGER <<<          */
    /*                                          */
    /********************************************/
    
    public void testRangeManager() {
    	int lower = (int)random(10, 100);
    	lower = 0;
    	int upper = lower + (int)random(50, 1000);
    	int count = (int)random(2, 13);
    	RangeManager rm = new RangeManager(lower, upper, count);
    	println(rm.toString());
    }
    
    /**
     * Mini-class for storing bounds of an integer range.
     *
     */
    class IntRange {
    	int lower;
    	int upper;
    	
    	public IntRange(int lower, int upper) {
    		this.lower = lower;
    		this.upper = upper;
    	}
    	
    	public IntRange() {
    		this(0, 0);
    	}
    			
		public String toString() {
			return "("+ lower +", "+ upper +")";
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof IntRange && (((IntRange) o).lower == this.lower) && (((IntRange) o).upper == this.upper);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			// cf. Effective Java, 2nd edition, ch. 3, item 9.
			int result = 17;
			result = 31 * result + Float.floatToIntBits(lower);
			result = 31 * result + Float.floatToIntBits(upper);
			return result;
		}
    	
    }
    
    /**
     * Mini-class for storing bounds of a float range.
     *
     */
    class FloatRange {
    	float lower;
    	float upper;
    	
    	public FloatRange(float lower, float upper) {
    		this.lower = lower;
    		this.upper = upper;
    	}
    	
    	public FloatRange() {
    		this(0, 0);
    	}
    			
		public String toString() {
			return "("+ lower +", "+ upper +")";
		}
    	
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof FloatRange && (((FloatRange) o).lower == this.lower) && (((FloatRange) o).upper == this.upper);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			// cf. Effective Java, 2nd edition, ch. 3, item 9.
			int result = 17;
			result = 31 * result + Float.floatToIntBits(lower);
			result = 31 * result + Float.floatToIntBits(upper);
			return result;
		}

    }
    
    
    /**
     * A utility class to assist in stepping through a array divided into a specified number of equal segments.
     *
     */
    class RangeManager {
    	ArrayList<IntRange> intervals;
    	Iterator<IntRange> iter;
    	IntRange intRange;
    	int numberOfIntervals;
    	int currentIndex;
    	
    	/**
    	 * Divides an intRange of integers, from a lower bound up to but not including an upper bound,
    	 * into a given number of equal intervals. 
    	 * 
     	 * @param lower   lower index of intRange
    	 * @param upper   upper index of intRange
    	 * @param count   number of intervals in which to divide the intRange
    	 */
    	public RangeManager(int lower, int upper, int count) {
    		this.intervals = new ArrayList<IntRange>(count);
    		this.intRange = new IntRange(lower, upper);
    		this.setNumberOfIntervals(count);
    	}
    	
    	/**
    	 * Divides a intRange of integers from 0 up to but not including an upper bound
    	 * into a given number of equal intervals. The upper bound would typically be 
    	 * the length of an array.
    	 * 
    	 * @param length
    	 * @param count
    	 */
    	public RangeManager(int upper, int count) {
    		this(0, upper, count);
    	}
    	
    	public Iterator<IntRange> getIter() {
    		if (null == iter) {
    			iter = intervals.iterator();
    		}
    		return iter;
    	}
    	
    	public IntRange get(int i) {
    		return intervals.get(i);
    	}
    	public IntRange getNext() {
    		return intervals.get(currentIndex++);
    	}
    	public boolean hasNext() {
    		return currentIndex < numberOfIntervals;
    	}
 
    	public int getCurrentIndex() {
    		return currentIndex;
    	}
    	public void resetCurrentIndex() {
    		currentIndex = 0;
    	}

    	public int getUpper() {
    		return intRange.upper;
    	}  	
    	public int getLower() {
    		return intRange.lower;
    	}
    	
    	/**
		 * @return the numberOfIntervals
		 */
		public int getNumberOfIntervals() {
			return numberOfIntervals;
		}

		/**
		 * Sets the value of numberOfIntervals and creates a new series of intervals.
		 * @param numberOfIntervals the numberOfIntervals to set
		 */
		public void setNumberOfIntervals(int numberOfIntervals) {
			this.numberOfIntervals = numberOfIntervals;
			adjustIntervals();
			resetCurrentIndex();
		}
	
		public void adjustIntervals() {
			this.intervals.clear();
    		int u = 0;
    		int l = getLower();
    		float pos = l;
    		float delta = (getUpper() - l) / (float) this.numberOfIntervals;
    		for (int i = 1; i <= numberOfIntervals; i++) {
    			pos += delta;
    			u = Math.round(pos) - 1;
    			intervals.add(new IntRange(l, u));
    			l = u + 1;
    		}
		}
		
		public void setRange(int lower, int upper) {
	   		this.intRange = new IntRange(lower, upper);
			adjustIntervals();
			resetCurrentIndex();
		}
		public void setRange(int upper) {
			setRange(0, upper);
		}

		@Override
    	public String toString() {
    		StringBuffer buf = new StringBuffer();
    		Iterator<IntRange> it = this.getIter();
    		buf.append("RangeManager: " + intervals.size() +" intervals from "+ intRange.lower +" to "+ intRange.upper + "\n  ");
    		while (it.hasNext()) {
    			IntRange r = it.next();
    			buf.append(r.toString() + ", ");
    		}
    		buf.delete(buf.length() - 2, buf.length() - 1);
    		return buf.toString();
    	}
    	
    }
 
    
    /********************************************/
    /*                                          */
    /*                >>> FFT <<<               */
    /*                                          */
    /********************************************/

    /**
     * Uses statistical FFT to reduce high frequencies in R, G and B channels. 
     * Usually when calling this method, the statistical FFT tool is inverted (key command 'q') 
     * to create a blurring effect.
     * Each channel is processed separately at a different scale (i.e., 16, 32, 64) The order
     * of the channels is determined by the current Component Sorting Order settings. If the setting
     * uses the RGB channels, that setting determines the channel order; otherwise, a random RGB order
     * is used. 
     * Once processed, the image is ready to be sharpened again with the statistical FFT
     * (key command 'k'), selected by default. Amazingly, the information thrown out by the lowpass 
     * can be reasonably well reconstructed, but of course it's glitchy. If the image dimensions 
     * are not evenly divisible by a power of 2, further artifacts will result. 
     */
    public void scaledLowPass() {
    	boolean[] savedChannelState = this.getStatChan();
    	int powTwo = (int) (Math.log(statFFTBlockWidth)/Math.log(2));
    	// println("Power of two = "+ powTwo);
    	int blockWidth = powTwo;
    	if (blockWidth < 5) blockWidth = 5;
    	String ordStr;
    	// We use the current sort order of color channels stored in compOrderIndex to determine 
    	// the order of operations here. If the components for sorting are H, S and L, 
    	// we chose instead a random ordering of R, G and B channels.
    	if (compOrderIndex > CompOrder.BGR.ordinal()) {
    		ordStr = CompOrder.values()[(int) random(6)].toString();
    	}
    	else {
      	ordStr = CompOrder.values()[compOrderIndex].toString();
    	}
    	// command = apply statistical FFT and rotate 90 degrees, four times
     	String cmd = "tktktktk";
    	for (int i = 0; i < 3; i++) {
      	setStatChan(ordStr.charAt(i)); 
      	setFFTBlockWidth(blockWidth - 2 + i);
      	exec(cmd);
    	}
    	setFFTBlockWidth(powTwo - 2);
    	// set the slider for the statistical range FFT tool to effectively sharpen the image 
    	this.resetStatRange();
    	this.setStatChan(savedChannelState, false);
    	println("Channel order: "+ ordStr);
    }
    
    public void setStatChan(char ch) {
    	// setStatChan(boolean isBrightness, boolean isHue, boolean isSaturation,
			//   boolean isRed, boolean isGreen, boolean isBlue, boolean isFromControlPanel)
    	switch(ch) {
    	case 'R': {
    		setStatChan(false, false, false, true, false, false, false);
    		break;
    	}
    	case 'G': {
    		setStatChan(false, false, false, false, true, false, false);
    		break;
    	}
    	case 'B': {
    		setStatChan(false, false, false, false, false, true, false);
    		break;
    	}
    	case 'H': {
    		setStatChan(false, true, false, false, false, false, false);
    		break;
    	}
    	case 'S': {
    		setStatChan(false, false, true, false, false, false, false);
    		break;
    	}
    	case 'L': {
    		setStatChan(true, false, false, false, false, false, false);
    		break;
    	}
    	}
    }

    // TODO create a class to manage FFT operations  
    
    /**
     * Scales a frequency by a factor.
     * 
     * @param freq
     * @param fac
     */
    public void fftScaleFreq(float freq, float fac) {
    	statFFT.scaleFreq(freq, fac);
    }



    /**
     * Scales an array of frequencies by an array of factors.
     * @param freqs
     * @param facs
     */
    public void fftScaleFreq(float[] freqs, float[] facs) {
    	for (int i = 0; i < freqs.length; i++) {
    		statFFT.scaleFreq(freqs[i], facs[i]);
    	}
    }

    /**
     * Scales a single frequency bin (index number) by a factor.
     * 
     * @param bin
     * @param fac
     */
    public void fftScaleBin(int bin, float fac) {
    	statFFT.scaleBand(bin, fac);
    }



    /**
     * Scales an array of frequency bins (index numbers) by an array of factors.
     * @param bins
     * @param facs
     */
    public void fftScaleBin(int[] bins, float[] facs) {
    	for (int i = 0; i < bins.length; i++) {
    		statFFT.scaleBand(bins[i], facs[i]);
    	}
    }

    /**
     * placeholder for future development
     */
    public void fftScaleFreqsTest() {
    	float[] freqs = {};
    	float[] facs = {};
    }
    
    /********************************************/
    /*                                          */
    /*           >>> FFT FORMANTS <<<           */
    /*                                          */
    /********************************************/
    
    float oneCent = (float) Math.pow(2, 1/1200.0);
    // ffreq1, ffreq2, ffreq3 will also be used as f0, f1, f2 
    // in the second part of the formant menu
    // float ffreq1 = 1033.0f;
    // float ffreq1 = 1.1484375f * 0.75f;    // 1.1484375 = 44100 divided by (800 * 48)
    // float ffreq2 = ffreq1 * (float) (5.0/4.0);			// M3 in just intonation
    // float ffreq3 = ffreq1 * (float) (4.0/3.0); 			// P5 in just intonation;
//    float ffreq2 = ffreq1 * (float) Math.pow(2, 6/12.0); //  tritone;
//    float ffreq3 = ffreq1 * (float) Math.pow(2, 11/12.0); // fourth higher, for a 7-10-13 voicing;
//    float ffreq2 = ffreq1 * (float) Math.pow(2, 2/1200.0); //  two cents;
//    float ffreq3 = ffreq1 * (float) Math.pow(2, 4/1200.0); // four cents;
    // some interesting frequencies: 18837, 1033, 7920, 8890, 6286
//    float ffreq1 = 1033;    //
//    float ffreq2 = 6286;			// amp: 1.0, 3.68, 3.30; 2.93, 1.0, 3.30
//    float ffreq3 = 18837;
//    float ffreq1 = 55.125f * 100/1920.0f;    //
//    float ffreq2 = 55.125f * 100 * (5/4.0f)/1920.0f;			// amp: 1.0, 3.68, 3.30; 2.93, 1.0, 3.30
//    float ffreq1 = 7367.1875f;   // 23 * 44100 / 128
//    float ffreq2 = 3330.0f;
//    float ffreq3 = 1033.0f;
    float ffreq1 = 0.9870f;   
    float fmult1 = 1;
    float fmult2 = 1;
    float ffreq2 = 1.5970f;
    float ffreq3 = 2.5840f;
    float famp1 = 3.0f;
    float famp2 = 3.0f;
    float famp3 = 3.0f;
    boolean isMuteF1 = false;
    boolean isMuteF2 = false;
    boolean isMuteF3 = false;
    // formant scaling factor
    float formantScale = 4.0f;
    // DC Bias
    float fDCBias = 0.0f;
    
    Formant theFormant = new Formant(400, 600, 800, "default", "default");
    // not used
    boolean isRunFormantRGB = false;
    
    public void setFfreq1(float ffreq1) {
			this.ffreq1 = ffreq1;
			theFormant.freq1 = ffreq1;
			Numberbox nb = (Numberbox) cpm.getControl().getController("setFfreq1");
			boolean shiftDown = cpm.getControl().isShiftDown();
//			boolean controlDown = cpm.getControl().isControlDown();
//			boolean optionDown = cpm.getControl().isAltDown();
//			boolean metaDown = cpm.getControl().isMetaDown();
//			println("control", controlDown, "option", optionDown, "meta", metaDown);
			if (nb.getMultiplier() < 10.0f && shiftDown) nb.setMultiplier(10.0f);
			if (nb.getMultiplier() == 10.0f && !shiftDown) {
				nb.setMultiplier(ffreq1 < 100 ? 0.01f : 0.1f);
				nb.setDecimalPrecision(ffreq1 < 100 ? 2 : 1);
			}
//			if (controlDown) {
//				println("---- control key is down -----");
//				nb.setMultiplier(0.01f);
//			}
			nb.setBroadcast(false);
			nb.setValue(ffreq1);
			nb.setBroadcast(true);
		}

		public void setFfreq2(float ffreq2) {
			this.ffreq2 = ffreq2;
			theFormant.freq2 = ffreq2;
			Numberbox nb = (Numberbox) cpm.getControl().getController("setFfreq2");
			boolean shiftDown = cpm.getControl().isShiftDown();
			if (nb.getMultiplier() < 10.0f && shiftDown) nb.setMultiplier(10.0f);
			if (nb.getMultiplier() == 10.0f && !shiftDown) {
				nb.setMultiplier(ffreq2 < 100 ? 0.01f : 0.1f);
				nb.setDecimalPrecision(ffreq2 < 100 ? 2 : 1);
			}
			nb.setBroadcast(false);
			nb.setValue(ffreq2);
			nb.setBroadcast(true);
		}

		public void setFfreq3(float ffreq3) {
			this.ffreq3 = ffreq3;
			theFormant.freq3 = ffreq3;
			Numberbox nb = (Numberbox) cpm.getControl().getController("setFfreq3");
			boolean shiftDown = cpm.getControl().isShiftDown();
			if (nb.getMultiplier() < 10.0f && shiftDown) nb.setMultiplier(10.0f);
			if (nb.getMultiplier() == 10.0f && !shiftDown) {
				nb.setMultiplier(ffreq3 < 100 ? 0.01f : 0.1f);
				nb.setDecimalPrecision(ffreq3 < 100 ? 2 : 1);
			}
			nb.setBroadcast(false);
			nb.setValue(ffreq3);
			nb.setBroadcast(true);
		}

		public void setFamp1(float famp1) {
			this.famp1 = famp1;
			cpm.getControl().getController("setFamp1");
			cpm.getControl().getController("setFamp1").setBroadcast(false);
			cpm.getControl().getController("setFamp1").setValue(famp1);
			cpm.getControl().getController("setFamp1").setBroadcast(true);
		}

		public void setFamp2(float famp2) {
			this.famp2 = famp2;
			cpm.getControl().getController("setFamp2");
			cpm.getControl().getController("setFamp2").setBroadcast(false);
			cpm.getControl().getController("setFamp2").setValue(famp2);
			cpm.getControl().getController("setFamp2").setBroadcast(true);
		}

		public void setFamp3(float famp3) {
			this.famp3 = famp3;
			cpm.getControl().getController("setFamp3");
			cpm.getControl().getController("setFamp3").setBroadcast(false);
			cpm.getControl().getController("setFamp3").setValue(famp3);
			cpm.getControl().getController("setFamp3").setBroadcast(true);
		}

		public void setFormantScale(float fs) {
			this.formantScale = fs;
			cpm.getControl().getController("setFormantScale");
			cpm.getControl().getController("setFormantScale").setBroadcast(false);
			cpm.getControl().getController("setFormantScale").setValue(formantScale);
			cpm.getControl().getController("setFormantScale").setBroadcast(true);
		}


	   public void setFDCBias(float val) {
				this.fDCBias = (int) (Math.round(val));
				cpm.getControl().getController("setFDCBias");
				cpm.getControl().getController("setFDCBias").setBroadcast(false);
				cpm.getControl().getController("setFDCBias").setValue(val);
				cpm.getControl().getController("setFDCBias").setBroadcast(true);
			}
	   
	   public void setMuteF1() {
	  	 isMuteF1 = !isMuteF1;
	  	 println("isMuteF1 = "+ isMuteF1);
	   }

	   public void setMuteF2() {
	  	 isMuteF2 = !isMuteF2;
	  	 println("isMuteF2 = "+ isMuteF2);
	   }

	   public void setMuteF3() {
	  	 isMuteF3 = !isMuteF3;
	  	 println("isMuteF3 = "+ isMuteF3);
	   }

	   // formant base frequency and ratios
		
		public void loadFormant(Formant f) {
			setFfreq1(f.freq1);
			setFfreq2(f.freq2);
			setFfreq3(f.freq3);
		}

		/*
     * some useful information
     * 
     * FORMANTS
     * i beet 270 2290 3010 
     * I bit 390 1990 2550
     * e bet 530 1840 2480
     * ae bat 660 1720 2410
     * a father 730 1090 2440
     * U book 440 1020 2240
     * u boot 300 870 2240
     * L but 640 1190 2390
     * r bird 490 1350 1690
     * aw bought 570 840 2410
     *   
     */
    public class Formant {
     	public float freq1;
     	public float freq2;
     	public float freq3;
     	public float[] formants;
    	public String example;
    	public String symbol;
    	
    	public Formant(float f1, float f2, float f3, String sym, String ex) {
    		this.freq1 = f1;
    		this.freq2 = f2;
    		this.freq3 = f3;
    		this.symbol = sym;
    		this.example = ex;
    	}
    	
    }
    
    public Formant[] formantList = new Formant[24];
    public int formantIndex = 0;
    public void loadFormantList() {
    	formantList[0] = new Formant(270, 2290, 3010, "i", "beet"); 
    	formantList[1] = new Formant(390, 1990, 2550, "I", "bit");
    	formantList[2] = new Formant(530, 1840, 2480, "e", "bet");
    	formantList[3] = new Formant(660, 1720, 2410, "ae", "bat");
    	formantList[4] = new Formant(730, 1090, 2440, "a", "father");
    	formantList[5] = new Formant(440, 1020, 2240, "U", "book");
    	formantList[6] = new Formant(300, 870, 2240, "u", "boot");
    	formantList[7] = new Formant(640, 1190, 2390, "L", "but");
    	formantList[8] = new Formant(490, 1350, 1690, "r", "bird");
    	formantList[9] = new Formant(570, 840, 2410, "aw", "bought");
     	formantList[10] = new Formant(490, 910, 2450, "o", "coat");
     	loadFormantOctave();
    }
    
    public void loadFormantOctave() {
     	// formantList[10] = new Formant(6785, 10946, 15127, "xx", "--synth--");
    	// formantList[10] = new Formant(10569, 10946, 11179, "xx", "--synth--");
  		/* 		  
    	// float fac = 0.05572809000085f;
    	float fac = 0.6180339887499f;
    	// float fac = 0.96f;
    	// Golden mean series
    	// starting values 22050, 9349, 10946, 6765
    	int x0 = 1, x1 = 2, x2 = 3, x3 = 5, x4 = 8, x5 = 13, x6 = 21, x7 = 34, x8 = 55, x9 = 89;
    	int seed = 10946 + 1597 + 1597 + 610;
    	seed = (int)(22050);
    	int f0, f1, f2;
    	f1 = seed;
    	f0 = Math.round(f1 * fac);
    	f2 = Math.round(f1 * (fac + 1));
    	//formantList[10] = new Formant(f0/x0, f1/x0, f2/x0, "x0", "--synth--");
     	formantList[10] = new Formant(490, 910, 2450, "o", "coat");
    	formantList[11] = new Formant(f0/x1, f1/x1, f2/x1, "x1", "--synth--");
     	formantList[12] = new Formant(f0/x2, f1/x2, f2/x2, "x2", "--synth--");
     	formantList[13] = new Formant(f0/x3, f1/x3, f2/x3, "x3", "--synth--");
     	formantList[14] = new Formant(f0/x4, f1/x4, f2/x4, "x4", "--synth--");
     	formantList[15] = new Formant(f0/x5, f1/x5, f2/x5, "x5", "--synth--");
     	formantList[16] = new Formant(f0/x6, f1/x6, f2/x6, "x6", "--synth--");
     	formantList[17] = new Formant(f0/x7, f1/x7, f2/x7, "x7", "--synth--");
     	formantList[18] = new Formant(f0/x8, f1/x8, f2/x8, "x8", "--synth--");
     	formantList[19] = new Formant(f0/x9, f1/x9, f2/x9, "x9", "--synth--");
     	formantList[20] = new Formant(233, 377, 610, "x10", "--synth--");
     	formantList[21] = new Formant(199, 322, 521, "x11", "--synth--");
     	formantList[22] = new Formant(144, 233, 377, "x11", "--synth--");
     	*/
    	// chromatic scale
    	float fac = (float) Math.pow(2, 1/12.0); // one semitone
    	// println("------------->>>>>> semitone fac = "+ fac);
    	float f0 = ffreq1;
    	float f1 = ffreq2;
    	float f2 = ffreq3;
    	println("-------------formant scale base:", f0, f1, f2);
    	int count = 0;
    	for (int i = 11; i < formantList.length; i++) {
    		formantList[i] = new Formant(f0, f1, f2, "x"+count, "--scale--");
    		f0 *= fac;
    		f1 *= fac;
    		f2 *= fac;
    		count++;
    	}
    	/*
    	// harmonic series
    	int x0 = 1, x1 = 2, x2 = 3, x3 = 4, x4 = 5, x5 = 6, x6 = 7, x7 = 8, x8 = 9, x9 = 10;
    	float fac = 0.05572809000085f;
    	int f0 = 440;
    	int f1 = Math.round(f0 + f0 * fac);
    	int f2 = Math.round(f1 + f1 * fac);
    	formantList[10] = new Formant(f0 * x0, f1 * x0, f2 * x0, "x0", "--synth--");
     	formantList[11] = new Formant(f0 * x1, f1 * x1, f2 * x1, "x1", "--synth--");
     	formantList[12] = new Formant(f0 * x2, f1 * x2, f2 * x2, "x2", "--synth--");
     	formantList[13] = new Formant(f0 * x3, f1 * x3, f2 * x3, "x3", "--synth--");
     	formantList[14] = new Formant(f0 * x4, f1 * x4, f2 * x4, "x4", "--synth--");
     	formantList[15] = new Formant(f0 * x5, f1 * x5, f2 * x5, "x5", "--synth--");
     	formantList[16] = new Formant(f0 * x6, f1 * x6, f2 * x6, "x6", "--synth--");
     	formantList[17] = new Formant(f0 * x7, f1 * x7, f2 * x7, "x7", "--synth--");
     	formantList[18] = new Formant(f0 * x8, f1 * x8, f2 * x8, "x8", "--synth--");
     	formantList[19] = new Formant(f0 * x9, f1 * x9, f2 * x9, "x9", "--synth--");
     	*/    	
    }
    
    public int[] threePerm = {0, 1, 2};
    public float[] freqs = null;
    public void permuteFormantValues() {
    	boolean shiftDown = cpm.getControl().isShiftDown();
    	if (shiftDown || null == freqs) {
    		freqs = new float[3];
    		freqs[0] = ffreq1;
    		freqs[1] = ffreq2;
    		freqs[2] = ffreq3;
    	}
    	boolean hasNextPerm = Permutator.nextPerm(threePerm);
    	if (!hasNextPerm) {
    		for (int i = 0; i < threePerm.length; i++) {
    			threePerm[i] = i;
    		}
    	}
    	// println("-------- permutation:", threePerm[0], threePerm[1], threePerm[2]);
    	setFfreq1(freqs[threePerm[0]]);
    	setFfreq2(freqs[threePerm[1]]);
    	setFfreq3(freqs[threePerm[2]]);
     }
    
    public void halfFormantValues() {
    	boolean shiftDown = cpm.getControl().isShiftDown();
    	float fac = 0.5f;
    	if (shiftDown) fac = 2.0f;
    	setFfreq1(ffreq1 * fac);
    	setFfreq2(ffreq2 * fac);
    	setFfreq3(ffreq3 * fac);
    }
    
    /**
     * Performs a zigzag or Hilbert scan, centered in the image, and passes blocks 
     * to an FFT transform that uses a user-supplied formant.
     * 
     */
    public void formantFFT(Formant formant) {
    	int order = (int) Math.sqrt(statBufferSize);
    	this.statFFTBlockWidth = order;
    	PixelScannerINF zz;
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
    	for (int y = 0; y < dh; y++) {
    		for (int x = 0; x < dw; x++) {
    			int mx = x * order + ow;
    			int my = y * order + oh;
    			int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
    			if (isRunFormantRGB) {
    				fftRGBFormantGlitch(pix, formant);
    			}
    			else {
    				// the samples are returned by fftFormantGlitch, but they are modified already
    				// TODO: HSL processing modifies the pix buffer as we go, we should try to avoid that
    				if (isEqGlitchBrightness) fftFormantGlitch(pix, ChannelNames.L, formant);
    				if (isEqGlitchHue) fftFormantGlitch(pix, ChannelNames.H, formant);
    				if (isEqGlitchSaturation) fftFormantGlitch(pix, ChannelNames.S, formant);
    				if (isEqGlitchRed) fftFormantGlitch(pix, ChannelNames.R, formant);
    				if (isEqGlitchGreen) fftFormantGlitch(pix, ChannelNames.G, formant);
    				if (isEqGlitchBlue) fftFormantGlitch(pix, ChannelNames.B, formant);
    			}
    			zz.plant(img.pixels, pix, img.width, img.height, mx, my);
    		}
    	}
    	img.updatePixels();
    	// necessary to call fitPixels to show updated image
    	fitPixels(isFitToScreen, false);
    	//		analyzeEq(false);
    }

    /**
     * Performs an FFT on a supplied array of samples, scales frequencies using settings in the 
     * formant interface, modifies the samples and also returns the modified samples. 
     * The RGB channels are modified by the three formant frequencies.
     * 
     * @param samples   an array of RGB values
     * @return          the modified samples
     */
    public int[] fftRGBFormantGlitch(int[] samples, Formant formant) {
    	// println(">>>>-------->>> fftRGBFormantGlitch <<<--------<<<<<");
    	float fac = formantScale;
    	float[] buf1 = null;
    	float[] buf2 = null;
    	float[] buf3 = null;
    	if (!isMuteF1) {
    		// convert R channel to an array of floats
    		buf1 = pullChannel(samples, ChannelNames.R);
    		// do a forward transform on the array of floats
    		statFFT.forward(buf1);
    		// scale the first frequency
    		this.fftScaleFreq(formant.freq1, fac * famp1);
    		// inverse the transform
    		statFFT.inverse(buf1);
    	}
    	if (!isMuteF2) {
    		// convert G channel to an array of floats
    		buf2 = pullChannel(samples, ChannelNames.G);
    		// do a forward transform on the array of floats
    		statFFT.forward(buf2);
    		// scale the second frequency
    		this.fftScaleFreq(formant.freq2, fac * famp2);
    		// inverse the transform
    		statFFT.inverse(buf2);
    	}
    	if (!isMuteF3) {
    		// convert B channel to an array of floats
    		buf3 = pullChannel(samples, ChannelNames.B);
    		// do a forward transform on the array of floats
    		statFFT.forward(buf3);
    		// scale the second frequency
    		this.fftScaleFreq(formant.freq3, fac * famp3);
    		// inverse the transform
    		statFFT.inverse(buf3);
    	}
    	// conditionally scale by the bias
    	if (fDCBias != 0) {
    		// println("-------- fDCBias = "+ fDCBias/1000.0f);
    		for (int j = 0; j < 16; j++) {
    			this.fftScaleBin(0, 1 + fDCBias/1000.0f);
    		}
    	}
    	// write RED samples back to buffer
    	if (!isMuteF1 && null != buf1) pushChannel(samples, buf1, ChannelNames.R);
    	// write GREEN samples back to buffer
    	if (!isMuteF2 && null != buf2) pushChannel(samples, buf2, ChannelNames.G);
    	// write BLUE samples back to buffer
    	if (!isMuteF3 && null != buf3) pushChannel(samples, buf3, ChannelNames.B);
    	// return the modified samples
    	return samples;
    }
    
    /**
     * Performs an FFT on a supplied array of samples, scales frequencies using settings in the 
     * formant interface, modifies the samples and also returns the modified samples. 
     * 
     * @param samples   an array of RGB values
     * @param chan      the channel to pass through the FFT
     * @return          the modified samples
     */
    public int[] fftFormantGlitch(int[] samples, ChannelNames chan, Formant formant) {
    	// convert the selected channel to an array of floats
    	float[] buf = pullChannel(samples, chan);
    	// do a forward transform on the array of floats
    	statFFT.forward(buf);
    	// scale the frequencies in the fft formant frequencies
    	// TODO better code than this kludge for scaling fac DONE
    	// float fac = 2.0f * (cpm.getEqBinValue(1) + 1);
    	// float fac = formantScale > 0 ? (map(formantScale, 0, 4, 1, famp1)) : 1 + formantScale;
    	// fac = map(formantScale, -1, 4, 0, famp1);
    	float fac = formantScale;
    	if (!isMuteF1) this.fftScaleFreq(formant.freq1, fac * famp1);
    	if (!isMuteF2) this.fftScaleFreq(formant.freq2, fac * famp2);
    	if (!isMuteF3) this.fftScaleFreq(formant.freq3, fac * famp3);
    	if (fDCBias != 0) {
    		// println("-------- fDCBias = "+ fDCBias/1000.0f);
    		for (int j = 0; j < 16; j++) {
    			this.fftScaleBin(0, 1 + fDCBias/1000.0f);
    		}
    	}
    	// inverse the transform
    	statFFT.inverse(buf);
    	pushChannel(samples, buf, chan);
    	return samples;
    }

    
    
	
  /*             >>> END FORMANT SECTION <<<             */
    
    
    /**
     * Calculates statistical variables from frequencies in the current FFT and returns then in an array.
     * 
     * @param l         left bound of bin index numbers
     * @param r         right bound of bin index numbers
     * @param verbose   true if output to consoles is desired, false otherwise
     * @param msg       a message to include with output
     * @return          an array of derived values: minimum, maximum, sum, mean, median, standard deviation, skew.
     */
    public float[] fftStatistics(int l, int r, boolean verbose, String msg) {
    	double sum = 0;
    	double squareSum = 0;
    	float[] values = new float[r - l];
    	int index = 0;
    	for (int i = l; i < r; i++) {
    		float val = statFFT.getBand(i);
    		sum += val;
    		squareSum += val * val;
    		values[index++] = val;
    	}
    	int mid = values.length/2;
    	java.util.Arrays.sort(values);
    	float median = (values[mid - 1] + values[mid])/2;
    	float min = values[0];
    	float max = values[values.length -1];
    	float mean = (float) sum/(r - l);
    	float variance = (float) squareSum/(r - l) - mean * mean;
    	float standardDeviation = (float) Math.sqrt(variance);
    	// Pearson's skew measure
    	float skew = 3 * (mean - median)/standardDeviation;
    	if (verbose) {
    		println(msg);
    		print("  min = "+ min);
    		print("  max = "+ max);
    		print("  sum = "+ (float) sum);
    		print("  mean = "+ mean);
    		print("  median = "+ median);
    		println("  sd = "+ standardDeviation);
    		println("  skew = "+ skew);
    	}
    	float[] results = new float[6];
    	results[0] = min;
    	results[1] = max;
    	results[2] = mean;
    	results[3] = median;
    	results[4] = standardDeviation;
    	results[5] = skew;
    	return results;
    }

    /**
     * Extracts a selected channel from an array of rgb values.
     * 
     * @param samples   rgb values in an array of int
     * @param chan      the channel to extract 
     * @return          the extracted channel values as an array of floats
     */
    public float[] pullChannel(int[] samples, ChannelNames chan) {
    	// convert sample channel to float array buf
    	float[] buf = new float[samples.length];
    	int i = 0;
    	switch (chan) {
    	case L: {
    		for (int argb : samples) buf[i++] = brightness(argb);
    		break;
    	}
    	case H: {
    		for (int argb : samples) buf[i++] = hue(argb);
    		break;
    	}
    	case S: {
    		for (int argb : samples) buf[i++] = saturation(argb);
    		break;
    	}
    	case R: {
    		for (int argb : samples)  buf[i++] = (argb >> 16) & 0xFF;
    		break;
    	}
    	case G: {
    		for (int argb : samples) buf[i++] = (argb >> 8) & 0xFF;
    		break;
    	}
    	case B: {
    		for (int argb : samples) buf[i++] = argb & 0xFF;
    		break;
    	}
    	}
    	return buf;
    }

    /**
     * Replaces a specified channel in an array of pixel values with a value 
     * derived from an array of floats and clipped to the range 0..255.
     * 
     * @param samples   an array of pixel values
     * @param buf       an array of floats
     * @param chan      the channel to replace
     */
    public void pushChannel(int[] samples, float[] buf, ChannelNames chan) {
    	// convert sample channel to float array buf
    	int i = 0;
    	switch (chan) {
    	case L: {
    		colorMode(HSB, 255);
    		for (float component : buf) {
    			int comp = Math.round((int) component); 
    			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
    			int argb = samples[i];
    			samples[i++] = color(Math.round(hue(argb)), Math.round(saturation(argb)), comp, 255);
    		}
    		break;
    	}
    	case H: {
    		colorMode(HSB, 255);
    		for (float component : buf) {
    			int comp = Math.round((int) component); 
    			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
    			int argb = samples[i];
    			samples[i++] = color(comp, Math.round(saturation(argb)), Math.round(brightness(argb)), 255);
    		}
    		break;
    	}
    	case S: {
    		colorMode(HSB, 255);
    		for (float component : buf) {
    			int comp = Math.round((int) component); 
    			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
    			int argb = samples[i];
    			samples[i++] = color(Math.round(hue(argb)), comp, Math.round(brightness(argb)), 255);
    		}
    		break;
    	}
    	case R: {
    		colorMode(RGB, 255);
    		for (float component : buf)  {
    			int comp = Math.round((int) component); 
    			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
    			int argb = samples[i];
    			samples[i++] = 255 << 24 | comp << 16 | ((argb >> 8) & 0xFF) << 8 | argb & 0xFF;
    		}
    		break;
    	}
    	case G: {
    		colorMode(RGB, 255);
    		for (float component : buf) {
    			int comp = Math.round((int) component); 
    			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
    			int argb = samples[i];
    			samples[i++] = 255 << 24 | ((argb >> 16) & 0xFF) << 16 | comp << 8 | argb & 0xFF;
    		}
    		break;
    	}
    	case B: {
    		colorMode(RGB, 255);
    		for (float component : buf) {
    			int comp = Math.round((int) component); 
    			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
    			int argb = samples[i];
    			samples[i++] = 255 << 24 | ((argb >> 16) & 0xFF) << 16 | ((argb >> 8) & 0xFF) << 8 | comp & 0xFF;
    		}
    		break;
    	}
    	}
    }
	
	/**
	 * Performs an FFT on a supplied array of samples, scales frequencies using settings in the 
	 * equalizer interface, modifies the samples and also returns the modified samples. 
	 * 
	 * @param samples   an array of RGB values
	 * @param chan      the channel to pass through the FFT
	 * @return          the modified samples
	 */
	public int[] fftEqGlitch(int[] samples, ChannelNames chan) {
		// convert the selected channel to an array of floats
		float[] buf = pullChannel(samples, chan);
		// do a forward transform on the array of floats, 
		// statFFT (confusing name TODO change name or varaible use) handles transform
		statFFT.forward(buf);
		// scale the frequencies in the fft by user-selected values from the equalizer interface
		for (int i = 0; i < calculatedBands; i++) {
			// get indices of the range of bands covered by each slider
			int pos = eq.length - i - 1;
			IntRange ir = bandList.get(pos);
			// get the scaling value set by the user
			float scale = eq[pos];
			// scale all bands between lower and upper index
			for (int j = ir.lower; j <= ir.upper; j++) {
				statFFT.scaleBand(j, scale);
				// if (scale != 1) println("----- fftEqGlitch: bin = "+ j +", scale = "+ scale);
			}
		}
		// inverse the transform
		statFFT.inverse(buf);
		pushChannel(samples, buf, chan);
		return samples;
	}
		
	/**
     * Performs a zigzag or Hilbert scan, centered in the image, and passes blocks 
     * to an FFT transform that uses a user-supplied equalization curve.
     * 
     */
	public void eqFFT() {
		int order = (int) Math.sqrt(statBufferSize);
		this.statFFTBlockWidth = order;
		PixelScannerINF zz;
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
		for (int y = 0; y < dh; y++) {
			for (int x = 0; x < dw; x++) {
				int mx = x * order + ow;
				int my = y * order + oh;
//				     			if (random(1) > 0.5f) {
//				    				zz.flipX();
//				    			}
//				     			if (random(1) > 0.5f) {
//				    				zz.flipY();
//				    			}
				int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
				// the samples are returned by fftEqGlitch, but they are modified already
				if (isEqGlitchBrightness) fftEqGlitch(pix, ChannelNames.L);
				if (isEqGlitchHue) fftEqGlitch(pix, ChannelNames.H);
				if (isEqGlitchSaturation) fftEqGlitch(pix, ChannelNames.S);
				if (isEqGlitchRed) fftEqGlitch(pix, ChannelNames.R);
				if (isEqGlitchGreen) fftEqGlitch(pix, ChannelNames.G);
				if (isEqGlitchBlue) fftEqGlitch(pix, ChannelNames.B);
				zz.plant(img.pixels, pix, img.width, img.height, mx, my);
			}
		}
		img.updatePixels();
		// necessary to call fitPixels to show updated image
		fitPixels(isFitToScreen, false);
		//		analyzeEq(false);
	}
    
	/**
	 * Performs an FFT on a supplied array of samples, scales frequencies using settings in the 
	 * statistical interface, modifies the samples and also returns the modified samples. 
	 * 
	 * @param samples   an array of RGB values
	 * @param chan      the channel to pass through the FFT
	 * @return          the modified samples
	 */
	public float[] fftStatGlitch(int[] samples, ChannelNames chan) {
		// convert the selected channel to an array of floats
		float[] buf = pullChannel(samples, chan);
		// do a forward transform on the array of floats
		statFFT.forward(buf);
		// ignore first bin, the "DC component" if low frequency is cut
		// function removed, didn't seem particularly useful
		float[] stats = fftStatistics(0, buf.length, false, "fft "+ chan.name());
		float min = stats[0];
		float max = stats[1];
		float mean = stats[2];
		float median = stats[3];
		float sd = stats[4];
		float skew = stats[5];
		int t = samples.length / 2;
		// typical values: left = 0.5f, right = 2.0f
		//		float leftEdge = mean - sd * leftBound;
		//		float rightEdge = mean + sd * rightBound;
		float leftEdge = leftBound < 0 ? mean - sd * -leftBound : mean + sd * leftBound;
		float rightEdge = rightBound < 0 ? mean - sd * -rightBound : mean + sd * rightBound;
		//		println("min = "+ min +", max = "+ max +", mean = "+ mean +", median = "+ median +", sd = " + sd  +", skew = "+ skew +", leftBound = "+ leftBound +", rightBound = "+ rightBound);		
		//		println("-- leftEdge = "+ leftEdge +", rightEdge = "+ rightEdge	);
		// scale the frequencies in the fft, skipping band 0
		for (int i = 1; i < t; i++) {
			float val = statFFT.getBand(i);
			// frequencies whose amplitudes lie outside the bounds are scaled by the cut value
			if (val < leftEdge || val > rightEdge) statFFT.scaleBand(i, cut);
			// frequencies whose amplitudes lie inside the bounds are scaled by the boost value
			else {
				statFFT.scaleBand(i, boost);
			}
		}
		// inverse the transform
		statFFT.inverse(buf);
		pushChannel(samples, buf, chan);
		return stats;
	}

	/**
	 * Performs a zigzag or Hilbert scan, centered in the image, and passes blocks 
	 * to an FFT transform that uses statistical analysis to determine frequency scaling.
	 * 
	 * @param order   the width/height of each pixel block to sort
	 */
	public void statFFT() {
		int order = (int) Math.sqrt(statBufferSize);
		this.statFFTBlockWidth = order;
		// eliminate fft averaging, don't need it
		// fft.logAverages(minBandWidth, bandsPerOctave);
		PixelScannerINF zz;
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
		int totalBlocks = dw * dh;
		int w = dw * order;
		int h = dh * order;
		int ow = (img.width - w) / 2;
		int oh = (img.height - h) / 2;
		float min = 0, max = 0, mean = 0, median = 0, sd = 0, skew = 0;
		float[] stats = new float[6];
		backup();
		img.loadPixels();
		for (int y = 0; y < dh; y++) {
			for (int x = 0; x < dw; x++) {
				int mx = x * order + ow;
				int my = y * order + oh;
//				     			if (random(1) > 0.5f) {
//				    				zz.flipX();
//				    			}
//				     			if (random(1) > 0.5f) {
//				    				zz.flipY();
//				    			}
				int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
				if (isStatGlitchBrightness) stats = fftStatGlitch(pix, ChannelNames.L);
				if (isStatGlitchHue) stats = fftStatGlitch(pix, ChannelNames.H);
				if (isStatGlitchSaturation) stats = fftStatGlitch(pix, ChannelNames.S);
				if (isStatGlitchRed) stats = fftStatGlitch(pix, ChannelNames.R);
				if (isStatGlitchGreen) stats = fftStatGlitch(pix, ChannelNames.G);
				if (isStatGlitchBlue) stats = fftStatGlitch(pix, ChannelNames.B);
				min += stats[0];
				max += stats[1];
				mean += stats[2];
				median += stats[3];
				sd += stats[4];
				skew += stats[5];
				zz.plant(img.pixels, pix, img.width, img.height, mx, my);
			}
		}
		min /= totalBlocks;
		max /= totalBlocks;
		mean /= totalBlocks;
		median /= totalBlocks;
		sd /= totalBlocks;
		skew /= totalBlocks;
		float leftEdge = leftBound < 0 ? mean - sd * -leftBound : mean + sd * leftBound;
		float rightEdge = rightBound < 0 ? mean - sd * -rightBound : mean + sd * rightBound;
		println("---- Average statistical values for image before FFT ----");
		println("  min = "+ twoPlaces.format(min) +", max = "+ twoPlaces.format(max) +", mean = "+ twoPlaces.format(mean) 
				+", median = "+ twoPlaces.format(median) +", sd = " + twoPlaces.format(sd)  +", skew = "+ twoPlaces.format(skew));		
		println("  leftEdge = "+ twoPlaces.format(leftEdge) +", rightEdge = "+ twoPlaces.format(rightEdge) 
				+", leftBound = "+ leftBound +", rightBound = "+ rightBound);
		img.updatePixels();
		// necessary to call fitPixels to show updated image
		fitPixels(isFitToScreen, false);
		//		analyzeEq(false);
	}

    
	/**
	 * Resets equalizer FFT controls
	 */
	public void resetEq() {
		for (int i = 0; i < eq.length; i++) {
			String token = cpm.sliderIdentifier + noPlaces.format(i);
			Slider slider = (Slider) cpm.getControl().getController(token);
			slider.setValue(0);
		}
		analyzeEq(false);
	}

	/**
	 * Resets statistical FFT controls, produces a sharpen effect
	 */
	public void resetStat() {
		this.resetStatRange();
		Numberbox n4 = (Numberbox) cpm.getControl().getController("setBoost");
		n4.setValue(defaultBoost);
		Numberbox n5 = (Numberbox) cpm.getControl().getController("setCut");
		n5.setValue(defaultCut);
	}
	
	/**
	 * inverts the settings of the restStat() method, produces a blur effect
	 */
	public void invertStat() {
		this.setLeftBound(-5.0f);
		this.setRightBound(-0.25f);
		Range r1 = (Range) cpm.getControl().getController("setStatEqRange");
		r1.setBroadcast(false);
		r1.setHighValue(-0.25f);
		r1.setLowValue(-5.0f);
		r1.setBroadcast(true);
	}

	/**
	 * Resets statistical FFT range controls
	 */
	public void resetStatRange() {
		Range r02 = (Range) cpm.getControl().getController("setStatEqRange");
		r02.setBroadcast(false);
		r02.setLowValue(defaultLeftBound);
		r02.setHighValue(defaultRightBound);
		r02.setArrayValue(0, defaultLeftBound);
		r02.setArrayValue(1, defaultRightBound);
		rightBound = defaultRightBound;
		leftBound = defaultLeftBound;
		r02.setBroadcast(true);
	}

	/**
	 * parameterless method that ControlP5 button in FFT tab calls (a workaround)
	 */
	public void analyzeEqBands() {
		analyzeEq(true);
	}
	
	// TODO calculate accurate center frequency values for the bands we actually have
	/**
	 * Examines display buffer Brightness channel and outputs mean 
	 * amplitudes of frequency bands shown in equalizer.
	 * 
	 * @param isPrintToConsole   if true, prints information to console
	 */
	public void analyzeEq(boolean isPrintToConsole) {
		int order = (int) Math.sqrt(statBufferSize);
		this.statFFTBlockWidth = order;
		//    	if (8 != order && 16 != order && 32 != order && 64 != order && 128 != order && 256 != order && 512 != order) {
		//    		println("block size must be 8, 16, 32, 64, 128, 256 or 512 for FFT glitching");
		//    		return;
		//    	}
		PixelScannerINF zz;
		if (isHilbertScan) {
			int depth = (int) (Math.log(order)/Math.log(2));
			zz = new HilbertScanner(depth);
			println("Hilbert depth = "+ depth);
		}
		else {
			zz = new Zigzagger(order);
			println("Zigzag order = "+ order);
		}
		// calculate how many complete blocks will fit horizontally and vertically
		int dw = (img.width / order);
		int dh = (img.height / order);
		int howManyBlocks =  dw * dh;
		// calculate the number of pixels in the vertical and horizontal block extents
		int w = dw * order;
		int h = dh * order;
		// calculate offsets towards the center, if blocks don't completely cover the image
		int ow = (img.width - w) / 2;
		int oh = (img.height - h) / 2;
		img.loadPixels();
		int blockNum = 0;
		binTotals = new double[calculatedBands];
		// minimum brightness value in image
		float min = -1;
		// maximum brightness value in image
		float max = 0;
		java.util.Arrays.fill(binTotals, 0);
		for (int y = 0; y < dh; y++) {
			for (int x = 0; x < dw; x++) {
				int mx = x * order + ow;
				int my = y * order + oh;
				int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
				float[] buf = new float[pix.length];
				colorMode(HSB, 255);
				// load buf with brightness values from block at mx, my
				for (int i = 0; i < pix.length; i++) {
					int c = pix[i];
					buf[i] = brightness(c);
					if (verbose) println(pix[i]);
				}
				statFFT.forward(buf);
				float[] stats = fftStatistics(0, buf.length, false, "fft brightness in frequency domain");
				if (min == -1) min = stats[0];
				else if (min > stats[0]) min = stats[0];
				if (max < stats[1]) max = stats[1];
				// sum the values in each band in our band list and stash the mean value in binTotals
				for (int i = 0; i < calculatedBands; i++) {
					IntRange ir = bandList.get(i);
					float sum = 0;
					for (int j = ir.lower; j <= ir.upper; j++) {
						sum += statFFT.getBand(j);
					}
					// divide sum by (number of bins in band i) *  (total number of blocks)
					binTotals[i] += sum/((ir.upper - ir.lower + 1) * howManyBlocks);
				}
				blockNum++;
			}
		}
		if (isPrintToConsole) {
			println("--- "+ blockNum +" blocks read, min = "+ min +", max = "+ max);
			for (int i = 0; i <calculatedBands; i++) {
				// divide the accumlated mean values from each block's band ranges
				// by the total number of blocks to get the normalized average over the image
				println("  band "+ i +": "+ twoPlaces.format(binTotals[i]));
			}
			float[] bins = cpm.getEqualizerValues();
			for (int i = 0; i < bins.length; i++) {
				println("  band "+ i +": "+ twoPlaces.format(bins[i] + 1));
			}
		}
	}
    
	// TODO output accurate center frequency values 
	/**
	 * Calculates available frequency bands for current FFT buffer, returns an array of integer ranges
	 * representing frequency bin index numbers. Sets calculatedBands to size of bandList array.
	 * 
	 * @return   array of integer ranges corresponding to frequency bin index numbers
	 */
	public ArrayList<IntRange> calculateEqBands() {
		bandList = new ArrayList<IntRange>();
		int slots = minBandWidth * (bandsPerOctave);
		ArrayList<FloatRange> freqList = new ArrayList<FloatRange>(slots);
		// we can obtain frequencies up to the Nyquist limit, which is half the sample rate
		float hiFreq = sampleRate / 2.0f, loFreq = 0;
		// bandsPerOctave = 3
		FloatRange fr;
		int pos = slots - 1;
		for (int i = 0; i < minBandWidth; i++) {
			loFreq = hiFreq * 0.5f;
			float incFreq = (hiFreq - loFreq)/bandsPerOctave;
			// inner loop could be more efficient
			for (int j = bandsPerOctave; j > 0; j--) {
				fr = new FloatRange(loFreq + (j - 1) * incFreq, loFreq + j * incFreq);
				freqList.add(fr);
			}
			hiFreq = loFreq;
		}
		// reverse the frequency list, it should go from low to high
		for (int left = 0, right = freqList.size() - 1; left < right; left++, right--) {
			// exchange the first and last
			FloatRange temp = freqList.get(left); 
			freqList.set(left, freqList.get(right)); 
			freqList.set(right, temp);
		}
		// figure out the bins
		int hiBin = 0;
		int loBin = 0;
		float freq0 = statFFT.indexToFreq(0);
		float freq = statFFT.indexToFreq(hiBin);
		IntRange ir = null;
		for (FloatRange r : freqList) {
			if (freq < freq0) continue;
			while (freq < r.upper) {
				freq = statFFT.indexToFreq(hiBin++);
			}
			IntRange temp = new IntRange(loBin, hiBin);
			if (!temp.equals(ir)) {
				bandList.add(temp);
				ir = temp;
			}
			loBin = hiBin;
		}
		// TODO maybe there's a less kludgey way to initilize, without the following correction
		// fix off by two error....
		bandList.get(bandList.size() - 1).upper = statFFT.specSize() - 1;
		// omit printing of lists
		calculatedBands = bandList.size();
		println("----- number of frequency bands = "+ calculatedBands);
		// don't need to do averaging, without it FFT should be faster
		// (minBandWidth, bandsPerOctave);
		return bandList;
	}
    
    
    /**
     * not used
     */
    public void printEqInfo() {
    	int ct = 0;
    	println("-------- frequencies --------");
    	Iterator<FloatRange> iter = this.freqList.iterator();
    	while (iter.hasNext()) {
    		FloatRange fr = iter.next();
    		println("  "+ ct++ +": "+ twoPlaces.format(fr.lower) +", "+ twoPlaces.format(fr.upper));
    	}
    	println();
    }
  
    /**
     * Calculates and outputs statistics for display buffer, determined by current FFT and equalizer bands. 
     */
    public void testEq() {
    	int slots = minBandWidth * (bandsPerOctave);
    	ArrayList<FloatRange> freqList = new ArrayList<FloatRange>(slots);
    	// we can obtain frequencies up to the Nyquist limit, which is half the sample rate
    	float hiFreq = sampleRate / 2.0f, loFreq = 0;
    	// bandsPerOctave = 3
    	FloatRange fr;
    	int pos = slots - 1;
    	for (int i = 0; i < minBandWidth; i++) {
    		loFreq = hiFreq * 0.5f;
    		float incFreq = (hiFreq - loFreq)/bandsPerOctave;
    		// inner loop could be more efficient
    		for (int j = bandsPerOctave; j > 0; j--) {
    	   		fr = new FloatRange(loFreq + (j - 1) * incFreq, loFreq + j * incFreq);
         		freqList.add(fr);
    		}
     		hiFreq = loFreq;
    	}
    	// reverse the list
    	for (int left = 0, right = freqList.size() - 1; left < right; left++, right--) {
    	    // exchange the first and last
    	    FloatRange temp = freqList.get(left); 
    	    freqList.set(left, freqList.get(right)); 
    	    freqList.set(right, temp);
    	}
    	// figure out the bins
    	ArrayList<IntRange> theBandList = new ArrayList<IntRange>();
    	int hiBin = 0;
    	int loBin = 0;
    	float freq0 = statFFT.indexToFreq(0);
    	float freq = statFFT.indexToFreq(hiBin);
    	IntRange ir = null;
    	for (FloatRange r : freqList) {
    		if (freq < freq0) continue;
    		while (freq < r.upper) {
    			freq = statFFT.indexToFreq(hiBin++);
    		}
    		IntRange temp = new IntRange(loBin, hiBin);
    		if (!temp.equals(ir)) {
    			theBandList.add(temp);
    			ir = temp;
    		}
    		loBin = hiBin;
    	}
     	// print out the list
    	int ct = 0;
    	println("\n---- Frequency List ----");
    	for (FloatRange r : freqList) {
    		println("  "+ ct +": "+ r.toString());
    		ct++;
    	}
    	ct = 0;
    	println("\n---- Band List ----");
    	for (IntRange r : theBandList) {
    		println("  "+ ct +": "+ r.toString());
    		ct++;
    	}
    	println("  freq 0 = "+ statFFT.indexToFreq(0) +", freq "+ statFFT.specSize() +" = "+ statFFT.indexToFreq(statFFT.specSize()));
    	println("\n");
    }
    
    // end FFT object control section
    
    /**
     * sets up audification
     */
    public void audify() {
    	println("----audify ON");
    	if (null == glitchSignal) {
    		glitchSignal = new GlitchSignal();
    		int edgeSize = glitchSignal.getBlockEdgeSize();
    		int powTwo = (int) (Math.log(edgeSize)/Math.log(2));
    		println("audify ---- powTwo", powTwo);
    		setFFTBlockWidth(powTwo);
    		println("audify ------");
    		out = minim2().getLineOut(Minim.STEREO, edgeSize * edgeSize);
    		out.addSignal(glitchSignal);
    	}
    	else {
    		glitchSignal.setIsUpdate(true);
    	}
    	audioIsRunning = true;
    }
    
    /**
     * turns off audification
     */
    public void audifyOff() {
    	println("----audify off");
   	if (null != glitchSignal) {
    		out.removeSignal(glitchSignal);
    		glitchSignal = null;
    		audioIsRunning = false;
    	}
    }
    
    Minim minim2() {
    	if (null == minim2) {
    		minim2 = new Minim(this);
    	}
    	return minim2;
    }
 
  	public boolean isTrackMouse() {
  		return isTrackMouse;
  	}
  	public void setTrackMouse(boolean isTrackMouse, boolean isFromControlPanel) {
  		if (!isFromControlPanel) {
  			if (isTrackMouse) ((CheckBox) cpm.getControl().getGroup("TrackMouse")).activate(0);
  			else ((CheckBox) cpm.getControl().getGroup("TrackMouse")).deactivate(0);
  		}
  		else {
  			this.isTrackMouse = isTrackMouse;
  			if (null != this.glitchSignal) glitchSignal.checkAutoPilot();
  		}
  	}

  	public boolean isFollowArrows() {
  		return isFollowArrows;
  	}
  	public void setFollowArrows(boolean isFollowArrows, boolean isFromControlPanel) {
  		this.isFollowArrows = isFollowArrows;
  	}

  	public boolean isAutoPilot() {
  		return isAutoPilot;
  	}
  	public void setAutoPilot(boolean isAutoPilot, boolean isFromControlPanel) {
  		this.isAutoPilot = isAutoPilot;
  	}

  	public boolean isMuted() {
  		return isMuted;
  	}
  	public void setMuted(boolean isMuted, boolean isFromControlPanel) {
  		println("---- setMuted "+ isMuted);
  		this.isMuted = isMuted;
  		if ((isMuted) && (null != glitchSignal)) {
  			out.mute();
  		}
			else {
				if (null != glitchSignal) out.unmute();
			}
  	}
  	public void setMuted(boolean val) {
  		println("--- setMuted "+ val);
  	}

  	public boolean isFrozen() {
  		return isFrozen;
  	}
  	public void setFrozen(boolean isFrozen, boolean isFromControlPanel) {
  		println("---- setFrozen "+ isFrozen);
  		this.isFrozen = isFrozen;
  	}
  	
  	public boolean isHidden() {
  		return isHidden;
  	}
  	public void setHidden(boolean isHidden, boolean isFromControlPanel) {
  		println("---- setHidden "+ isHidden);
  		this.isHidden = isHidden;
  	}
  	
  	public boolean isPipeToOSC() {
  		return isPipeToOSC;
  	}
  	public void setPipeToOSC(boolean isPipeToOSC, boolean isFromControlPanel) {
  		println("---- setPipeToOSC "+ isPipeToOSC);
  		this.isPipeToOSC = isPipeToOSC;
  	}

    /**
     * @author paulhz
     * a class that implements an AudioSignal interface, used by Minim library to produce sound.
     */
    public class GlitchSignal implements AudioSignal {
    	// either 32 or 64 work well for close to real time synthesis
    	int blockEdgeSize = 64;
    	PixelScannerINF zz;	// a pixel scanner, Zigzagger or HilbertScanner
    	int dw;			// number of horizontal tiles
    	int dh;			// number of vertical tiles
    	int w;			// total pixel width of complete horizontal tiles
    	int h;			// total pixel height of complete vertical tiles
    	int ow;			// edge offset to center complete horizontal tiles
    	int oh;			// edge offset to center complete vertical tiles
    	int mapX;		// x-coordinate of current block
    	int mapY;		// y-coordinate of current block
  		float fac = 1.0f/255 * 2.0f;    // scaling factor
    	float[] buf;	// copy of audio buffer
    	boolean isUpdate;	// buffer needs to be updated
    	int xinc = 0;
    	int yinc = 0;
    	char cmd = '0';
    	HammingWindow hamming;
    	boolean isUseHamming = true;
    	float[] hammingValues;

    	public GlitchSignal() {
    		//        		zz = new Zigzagger(blockEdgeSize);
    		/* */
    		if (isHilbertScan) {
    			int depth = (int) (Math.log(blockEdgeSize)/Math.log(2));
    			zz = new HilbertScanner(depth);
    			println("audio Hilbert depth = "+ depth);
    		}
    		else {
    			zz = new Zigzagger(blockEdgeSize);
    			println("audio Zigzag order = "+ blockEdgeSize);
    		}
    		/* */
       		hamming = new HammingWindow();
       		int len = blockEdgeSize * blockEdgeSize;
       		hammingValues = new float[len];
       		for (int i = 0; i < len; i++) {
    				hammingValues[i] = hammingValue(len, i);
    			}
    	}
    	
    	public PixelScannerINF getZz() {
    		if (null == zz) {
      		//        		zz = new Zigzagger(blockEdgeSize);
      		/* */
      		if (isHilbertScan) {
      			int depth = (int) (Math.log(blockEdgeSize)/Math.log(2));
      			zz = new HilbertScanner(depth);
      			//println("audio Hilbert depth = "+ depth);
      		}
      		else {
      			zz = new Zigzagger(blockEdgeSize);
      			//println("audio Zigzag order = "+ blockEdgeSize);
      		}
      		/* */
    		}
    		return zz;
    	}
    	
    	public int getBlockEdgeSize() {
    		return this.blockEdgeSize;
    	}

    	public void generate(float[] samp) {
    		// update dimensions to catch rotations, new images, etc.;
    		// dw is number of tiles horizontally, dh is number of tiles verticallly
    		dw = (img.width / blockEdgeSize);
    		dh = (img.height / blockEdgeSize);
    		// w and h are the width and height of the image tiled by squares, 
    		// each blockEdgeSize pixels on a side
    		w = dw * blockEdgeSize;
    		h = dh * blockEdgeSize;
    		// calculate offsets from edges, non-zero if image dimensions are bigger than tiled dimensions
    		ow = (img.width - w) / 2;
    		oh = (img.height - h) / 2;
    		// initialize values we want to calculate
    		int inX = 0; int inY = 0;
    		int mx = 0; int my = 0;
    		if (isTrackMouse) {
    			// track the mouse position to determine the image block to sample
    			if (isFitToScreen) {
    				inX = (int) map(mouseX, 0, fitImg.width, 0, img.width);
    				inY = (int) map(mouseY, 0, fitImg.height, 0, img.height);
    			}
    			else {
    				inX = mouseX;
    				inY = mouseY;
    			}
    			mx = (inX/blockEdgeSize) * blockEdgeSize + ow;
    			my = (inY/blockEdgeSize) * blockEdgeSize + oh;
    		}
    		else if (isFollowArrows) {
    			// use the arrow keys to determine image block to sample
    			int temp = mapX + xinc;
    			if (temp < ow) {
    				temp = w - blockEdgeSize;
    			}
    			else if (temp > w - blockEdgeSize) {
    				temp = ow;
    			}
    			mx = temp;
    			xinc = 0;
    			temp = mapY + yinc;
    			if (temp < oh) {
    				temp = h - blockEdgeSize;
    			}
    			else if (temp > h - blockEdgeSize) {
    				temp = oh;
    			}
    			my = temp;
    			yinc = 0;
    		}
    		else if (isAutoPilot) {
    			// running on autopilot
    			if (xinc == 0) xinc = blockEdgeSize;
    			int temp = mapX + xinc;
    			if ((temp < ow) || (temp > w - blockEdgeSize)) {
    				xinc = -xinc;
    				temp = mapX;
    				yinc = blockEdgeSize;
    			}
    			mx = temp;
    			temp = mapY + yinc;
    			if (temp < oh) {
    				temp = h - blockEdgeSize;
    			}
    			else if (temp > h - blockEdgeSize) {
    				temp = oh;
    			}
    			my = temp;
    			// audio scanning seems to happen faster than screen refresh
    			// println("mx", mx, "my", my);
    			yinc = 0; 
    		}
    		else {

    		}
    		// check if we're outside the central area of complete tiles
    		// skip out if we are
    		if (mx > w - blockEdgeSize + ow || my > h - blockEdgeSize + oh) return;
    		if (mx == this.mapX && my == this.mapY && !isUpdate) {
    			// copy the buffer
    			// copyBuffer(samp);
    			// zeroBuffer();
    			copyBuffer(samp);
    		} 
    		else {
    			// calculate a new buffer
    			refreshBuffer(mx, my, fac, samp);
    			isUpdate = false;
    			if (cmd != '0') {decode(cmd);}   				
    		}
    	}
    	
    	// this is a bizarro mono signal
    	public void generate(float[] left, float[] right)
    	{
    		generate(left);
    		generate(right);
    	}
    	
    	public void refreshBuffer(int mx, int my, float fac, float[] samp) {
    		this.mapX = mx;
    		this.mapY = my;
    		int[] pix = getZz().pluck(img.pixels, img.width, img.height, mapX, mapY);
    		buf = pullChannel(pix, ChannelNames.L);
    		if (isUseHamming) {
    			for (int i = 0; i < buf.length; i++) {
    				buf[i] = buf[i] * fac - 1.0f;
    				// smooth buffer with a hamming function
    				// samp[i] = buf[i] * hammingValue(buf.length, i);
    				samp[i] = buf[i] * hammingValues[i];
    			}
    		}
    		else {
    			for (int i = 0; i < buf.length; i++) {
    				buf[i] = buf[i] * fac - 1.0f;
    				// box filter
    				samp[i] = buf[i];
    			}
    		}
    	}
    	
    	public void copyBuffer(float[] samp) {
    		if (null == buf) return;
     		for (int i = 0; i < buf.length; i++) {
    			samp[i] = buf[i];
    		}
    	}
 
    	public void zeroBuffer() {
    		if (null == buf) return;
     		for (int i = 0; i < buf.length; i++) {
    			buf[i] = 0;
    		}
    	}
    	
    	public float hammingValue(int length, int index) {
    		return 0.54f - 0.46f * (float) Math.cos(TWO_PI * index / (length - 1));
    	}

    	public void moveBlock(int dx, int dy) {
    		xinc += (dx * this.blockEdgeSize);
    		yinc += (dy * this.blockEdgeSize);
     	}
    	
   	public int getMapX() {
    		return this.mapX;
    	}
    	public int getMapY() {
    		return this.mapY;
    	}
    	
    	public void checkAutoPilot() {
    		if (isAutoPilot) {
    			xinc = this.blockEdgeSize;
    			yinc = 0;
    		}
    	}
   	
    	public boolean getIsUpdate() {
    		return this.isUpdate;
    	}
    	public void setIsUpdate(boolean newIsUpdate) {
    		isUpdate = newIsUpdate;
    	}
    	
    	public void decode(char c) {
    		cmd = '0';
    		if (mapX > w - blockEdgeSize + ow || mapY > h - blockEdgeSize + oh) return;
    		//        		zz = new Zigzagger(blockEdgeSize);
    		/* */
    		if (isHilbertScan) {
    			int depth = (int) (Math.log(blockEdgeSize)/Math.log(2));
    			zz = new HilbertScanner(depth);
    			//println("audio Hilbert depth = "+ depth);
    		}
    		else {
    			zz = new Zigzagger(blockEdgeSize);
    			//println("audio Zigzag order = "+ blockEdgeSize);
    		}
    		/* */
    		img.loadPixels();
    		int[] pix = zz.pluck(img.pixels, img.width, img.height, mapX, mapY);
    		// do something to a single block
    		if ('G' == c) { sortTool.setControlState(); sortTool.sort(pix); cmd = c;}
    		else if ('K' == c) { 
    			if (statFFTBlockWidth != blockEdgeSize) {
    				resetFFT(blockEdgeSize);
    				println("---- reset FFT for audio");
    			}
    			fftStatGlitch(pix, ChannelNames.L);
    			if (statBufferSize != this.blockEdgeSize) {
    				//setFFTBlockWidth((int) Math.sqrt(this.blockEdgeSize));
    			}
    			cmd = c; 
    		}
    		else if ('J' == c) { 
    			if (statFFTBlockWidth != blockEdgeSize) {
    				resetFFT(blockEdgeSize);
    				println("---- reset FFT for audio");
    			}
    			fftEqGlitch(pix, ChannelNames.L); 
    			if (statBufferSize != this.blockEdgeSize) {
    				//setFFTBlockWidth((int) Math.sqrt(this.blockEdgeSize));
    			}
    			cmd = c; }
    		else if ('L' == c) { 
    			sortTool.setControlState(); 
    			sortTool.sort(pix); 
    			cmd = c; 
    		}
    		else if ('6' == c && isCapsLockDown()) { 
    			if (statFFTBlockWidth != blockEdgeSize) {
    				resetFFT(blockEdgeSize);
    				println("---- reset FFT for audio");
    			}
    			// Formant formant = formantList[formantIndex];
    			Formant formant = theFormant;
  				if (isEqGlitchBrightness) fftFormantGlitch(pix, ChannelNames.L, formant);
  				if (isEqGlitchHue) fftFormantGlitch(pix, ChannelNames.H, formant);
  				if (isEqGlitchSaturation) fftFormantGlitch(pix, ChannelNames.S, formant);
  				if (isEqGlitchRed) fftFormantGlitch(pix, ChannelNames.R, formant);
  				if (isEqGlitchGreen) fftFormantGlitch(pix, ChannelNames.G, formant);
  				if (isEqGlitchBlue) fftFormantGlitch(pix, ChannelNames.B, formant);
    			cmd = c; 
    		}
    		else if ('W' == c || 'w' == c) {
    			isTrackMouse = !isTrackMouse;
    			println("isTrackMouse = "+ isTrackMouse);
    		}
    		else if ('Q' == c || 'q' == c) {
    			isAutoPilot = !isAutoPilot;
    			println("isAutoPilot = "+ isAutoPilot);
    		}
    		else if ('H' == c) {
    			isUseHamming = !isUseHamming;
    		}
    		else {
    			// do nothing
    			/* sortTool.setControlState(); 
    			sortTool.sort(pix); */
    		}
    		zz.plant(img.pixels, pix, img.width, img.height, mapX, mapY);
    		img.updatePixels();
    		// necessary to call fitPixels to show updated image, but there may be threading problems...
    		// fitPixels(isFitToScreen, false);
    		screenNeedsUpdate = true;
    		isUpdate = true;
    	}

    }
    

	
    /********************************************/
    /*                                          */
    /*              >>> ZIGZAG <<<              */
    /*    control panel commands - migrate      */
    /*                                          */
    /********************************************/

    public ZigzagCorner[] zzCorners;
    public int zzLen = 0;
    
    /**
     * Sets zigzagFloor and zigzagCeiling in response to control panel.
     * @param val   a value forwarded by ControlP5 that we will ignore (just in this case)
     */
    public void setZigzagRange(float val) {
    	// here's one way to retrieve the values of the range controller
		Range r1 = (Range) cpm.getControl().getController("setZigzagRange");
		if (!r1.isInside()) {
			return;
		}
		zigzagFloor = (int) r1.getArrayValue()[0];
		zigzagCeiling = (int) r1.getArrayValue()[1];
    }
 
	/**
	 * Sets the value that determines on average how often a zigzag sort will be executed.
	 * The effect of sorting is to scatter zigzag-sorted triangles across the image. 
	 * @param newZigzagPercent   the zigZagPercent to set
	 */
	public void setZigzagPercent(float newZigzagPercent) {
		if (newZigzagPercent == zigzagPercent) return;
		zigzagPercent = newZigzagPercent;
	}

    
	/**
	 * Performs a zigzag sort, centered in the image.
	 * @param order   the width/height of each pixel block to sort
	 */
	public void zigzag(int order) {
		// TODO better fix: ControlP5 button press calls here with 0 for order, apparently...
		if (0 == order) order = zigzagBlockWidth;
		Zigzagger zz = new Zigzagger(order);
		println("Zigzag order = "+ order);
		int dw = (img.width / order);
		int dh = (img.height / order);
		int w = dw * order;
		int h = dh * order;
		int ow = (img.width - w) / 2;
		int oh = (img.height - h) / 2;
		backup();
		img.loadPixels();
		this.sortTool.setControlState();
		println("--- "+ zigzagStyle.name() +" zigzag ----");
		if (ZigzagStyle.PERMUTE != zigzagStyle) {
			zzLen = dh * dw;
			zzCorners = new ZigzagCorner[zzLen];
			Arrays.fill(zzCorners, ZigzagCorner.TOPLEFT);
			for (int y = 0; y < dh; y++) {
				for (int x = 0; x < dw; x++) {
					// a quick way to sort only a determined percentage of cells
					if (random(100) > (int)(zigzagPercent)) continue;
					int mx = x * order + ow;
					int my = y * order + oh;
					int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
					int zzWhichCorner = 0;
					if (ZigzagStyle.RANDOM == zigzagStyle) {
						if (random(1) > 0.5f) {
							zz.flipX();
							zzWhichCorner += 1;
						}
						if (random(1) > 0.5f) {
							zz.flipY();
							zzWhichCorner += 2;
						}
						zzCorners[y * dw + x] = (ZigzagCorner.values()[zzWhichCorner]);
					}
				}
			}
		}
		else {
			// permute zigzag orientation in 2x2 blocks
			int[] perm = {0, 1, 2, 3};
			ZigzagCorner[] zzOrder 
			= {ZigzagCorner.TOPLEFT, ZigzagCorner.BOTTOMLEFT, ZigzagCorner.BOTTOMRIGHT, ZigzagCorner.TOPRIGHT};
			Zigzagger[] zzList = new Zigzagger[4];
			zzList[0] = zz;              // TOPLEFT
			zz = new Zigzagger(order);
			zz.flipX();
			zzList[1] = zz;              // BOTTOMLEFT
			zz = new Zigzagger(order);
			zz.flipX();
			zz.flipY();
			zzList[2] = zz;              // BOTTOMRIGHT
			zz = new Zigzagger(order);
			zz.flipY();
			zzList[3] = zz;              // TOPRIGHT
			int dw2 = dw/2;
			int dh2 = dh/2;
			zzLen = dh2 * 2 * dw2 * 2;
			zzCorners = new ZigzagCorner[zzLen];
			this.sortTool.setControlState();
			for (int y = 0; y < dh2; y++) {
				for (int x = 0; x < dw2; x++) {
					// a quick way to sort only a determined percentage of cells
					if (random(100) > (int)(zigzagPercent)) continue;
					int mx = 2 * x * order + ow;
					int my = 2 * y * order + oh;
					shuffle(perm);
					int elem = perm[0];
					zz = zzList[elem];
					// zzCorners[y * dw2 + x] = zzOrder[elem];
					int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
					elem = perm[1];
					zz = zzList[elem];
					my += order;
					//zzCorners[y *  dw2 + x * 2] = zzOrder[elem];
					pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
					elem = perm[2];
					zz = zzList[elem];
					mx += order;
					pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
					elem = perm[3];
					zz = zzList[elem];
					my -= order;
					pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
				}
			}
		}
		img.updatePixels();
		// necessary to call fitPixels to show updated image
		fitPixels(isFitToScreen, false);
	}
    
    /**
      * Performs a zigzag sort, centered in the image, sets the width of the square 
      * pixel blocks for zigzag sorting to a random number between zigzagFloor and zigzagCeiling + 1.
     */
    public void zigzag() {
    	int order = (int) random(zigzagFloor, zigzagCeiling + 1);
    	zigzagBlockWidth = order;
    	println("zigzagFloor = "+ zigzagFloor +", zigzagCeiling = "+ zigzagCeiling +", order = "+ order);
    	zigzag(order);
    }
    
    /********************************************/
    /*                                          */
    /*           >>> HILBERT SCAN <<<           */
    /*    control panel commands - migrate      */
    /*                                          */
    /********************************************/

    public ZigzagCorner[] hilbCorners;
    public int hilbLen = 0;
    
  	/**
  	 * Performs a sort along a Hilbert curve, centered in the image.
  	 * @param depth   the width/height of each pixel block to sort
  	 */
  	public void hilbertScan(int depth) {
  		// TODO better fix: ControlP5 button press calls here with 0 for order, apparently...
  		if (0 == depth) depth = (int) (Math.log(zigzagBlockWidth)/Math.log(2));
  		println("-------- hilbertScan depth = "+ depth);
  		HilbertScanner hilb = new HilbertScanner(depth);
  		println("hilbertScan depth = "+ depth);
  		int blockWidth = hilb.getBlockWidth();
  		int dw = (img.width / blockWidth);
  		int dh = (img.height / blockWidth);
  		int w = dw * blockWidth;
  		int h = dh * blockWidth;
  		int ow = (img.width - w) / 2;
  		int oh = (img.height - h) / 2;
  		backup();
  		img.loadPixels();
  		this.sortTool.setControlState();
  		println("--- "+ zigzagStyle.name() +" zigzag ----");
  		if (ZigzagStyle.PERMUTE != zigzagStyle) {
  			hilbLen = dh * dw;
  			hilbCorners = new ZigzagCorner[hilbLen];
  			Arrays.fill(hilbCorners, ZigzagCorner.TOPLEFT);
  			for (int y = 0; y < dh; y++) {
  				for (int x = 0; x < dw; x++) {
  					// a quick way to sort only a determined percentage of cells
  					if (random(100) > (int)(zigzagPercent)) continue;
  					int mx = x * blockWidth + ow;
  					int my = y * blockWidth + oh;
  					int[] pix = hilb.pluck(img.pixels, img.width, img.height, mx, my);
  					this.sortTool.sort(pix);
  					hilb.plant(img.pixels, pix, img.width, img.height, mx, my);
  					int hilbWhichCorner = 0;
  					if (ZigzagStyle.RANDOM == zigzagStyle) {
  						if (random(1) > 0.5f) {
  							hilb.flipX();
  							hilbWhichCorner += 1;
  						}
  						if (random(1) > 0.5f) {
  							hilb.flipY();
  							hilbWhichCorner += 2;
  						}
  						hilbCorners[y * dw + x] = (ZigzagCorner.values()[hilbWhichCorner]);
  					}
  				}
  			}
  		}
  		else {
  			// permute zigzag orientation in 2x2 blocks
  			int[] perm = {0, 1, 2, 3};
  			ZigzagCorner[] hilbOrder = {ZigzagCorner.TOPLEFT, ZigzagCorner.BOTTOMLEFT, ZigzagCorner.BOTTOMRIGHT, ZigzagCorner.TOPRIGHT};
  			HilbertScanner[] hilbList = new HilbertScanner[4];
  			hilbList[0] = hilb;              // TOPLEFT
  			hilb = new HilbertScanner(depth);
  			hilb.flipX();
  			hilbList[1] = hilb;              // BOTTOMLEFT
  			hilb = new HilbertScanner(depth);
  			hilb.flipX();
  			hilb.flipY();
  			hilbList[2] = hilb;              // BOTTOMRIGHT
  			hilb = new HilbertScanner(depth);
  			hilb.flipY();
  			hilbList[3] = hilb;              // TOPRIGHT
  			int dw2 = dw/2;
  			int dh2 = dh/2;
  			hilbLen = dh2 * 2 * dw2 * 2;
  			hilbCorners = new ZigzagCorner[hilbLen];
  			this.sortTool.setControlState();
  			for (int y = 0; y < dh2; y++) {
  				for (int x = 0; x < dw2; x++) {
  					// a quick way to sort only a determined percentage of cells
  					if (random(100) > (int)(zigzagPercent)) continue;
  					int mx = 2 * x * blockWidth + ow;
  					int my = 2 * y * blockWidth + oh;
  					shuffle(perm);
  					int elem = perm[0];
  					hilb = hilbList[elem];
  					// hilbCorners[y * dw2 + x] = hilbOrder[elem];
  					int[] pix = hilb.pluck(img.pixels, img.width, img.height, mx, my);
  					this.sortTool.sort(pix);
  					hilb.plant(img.pixels, pix, img.width, img.height, mx, my);
  					elem = perm[1];
  					hilb = hilbList[elem];
  					my += blockWidth;
  					//hilbCorners[y *  dw2 + x * 2] = hilbOrder[elem];
  					pix = hilb.pluck(img.pixels, img.width, img.height, mx, my);
  					this.sortTool.sort(pix);
  					hilb.plant(img.pixels, pix, img.width, img.height, mx, my);
  					elem = perm[2];
  					hilb = hilbList[elem];
  					mx += blockWidth;
  					pix = hilb.pluck(img.pixels, img.width, img.height, mx, my);
  					this.sortTool.sort(pix);
  					hilb.plant(img.pixels, pix, img.width, img.height, mx, my);
  					elem = perm[3];
  					hilb = hilbList[elem];
  					my -= blockWidth;
  					pix = hilb.pluck(img.pixels, img.width, img.height, mx, my);
  					this.sortTool.sort(pix);
  					hilb.plant(img.pixels, pix, img.width, img.height, mx, my);
  				}
  			}
  		}
  		img.updatePixels();
  		// necessary to call fitPixels to show updated image
  		fitPixels(isFitToScreen, false);
  	}

    /**
     * Performs a zigzag sort, centered in the image, sets the width of the square 
     * pixel blocks for zigzag sorting to a random number between zigzagFloor and zigzagCeiling + 1.
    */
   public void hilbertScan() {
   	int order = (int) random(zigzagFloor, zigzagCeiling + 1);
   	zigzagBlockWidth = order;
   	println("Hilbert Scan: zigzagFloor = "+ zigzagFloor +", zigzagCeiling = "+ zigzagCeiling +", order = "+ order);
   	hilbertScan(0);
   }

        
}
