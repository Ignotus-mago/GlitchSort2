/**
 * 
 */
package net.paulhertz.glitchsort;

import processing.core.*;
import ddf.minim.analysis.*;
import ddf.minim.*;
//static import statement (Java 1.5) allows us to use unqualified constant names
import static net.paulhertz.glitchsort.GlitchConstants.*;

/**
 * @author paulhz
 * Plan: shift FFT calls from GlitchSort to FFTHandler. Perhaps as a singleton?
 */
public class FFTHandler {
	private GlitchSort app;
	private Minim minim;
	private FFT fft;

	/**
	 * @param app
	 * @param minim
	 */
	public FFTHandler(GlitchSort app, Minim minim) {
		super();
		this.app = app;
		this.minim = minim;
		this.fft = new FFT(app.statBufferSize, app.sampleRate);
	}
	
  /**
   * Scales a frequency by a factor.
   * 
   * @param freq
   * @param fac
   */
  public void fftScaleFreq(float freq, float fac) {
  	fft.scaleFreq(freq, fac);
  }

  /**
   * Scales an array of frequencies by an array of factors.
   * @param freqs
   * @param facs
   */
  public void fftScaleFreq(float[] freqs, float[] facs) {
  	for (int i = 0; i < freqs.length; i++) {
  		fft.scaleFreq(freqs[i], facs[i]);
  	}
  }

  /**
   * Scales a single frequency bin (index number) by a factor.
   * 
   * @param bin
   * @param fac
   */
  public void fftScaleBin(int bin, float fac) {
  	fft.scaleBand(bin, fac);
  }
	
  /**
   * Scales an array of frequency bins (index numbers) by an array of factors.
   * @param bins
   * @param facs
   */
  public void fftScaleBin(int[] bins, float[] facs) {
  	for (int i = 0; i < bins.length; i++) {
  		fft.scaleBand(bins[i], facs[i]);
  	}
  }
  
  /**
   * Calculates statistical variables from frequencies in the current FFT and returns then in an array.
   * 
   * @param l         left bound of bin index numbers
   * @param r         right bound of bin index numbers
   * @param verbose   true if output to consoles is desired, false otherwise
   * @param msg       a message to include with output
   * @return          an array of derived values: minimum, maximum, sum, mean, median, standard deviation, skew.
   */
  public float[] fftStat(int l, int r, boolean verbose, String msg) {
  	double sum = 0;
  	double squareSum = 0;
  	float[] values = new float[r - l];
  	int index = 0;
  	for (int i = l; i < r; i++) {
  		float val = fft.getBand(i);
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
  		PApplet.println(msg);
  		PApplet.print("  min = "+ min);
  		PApplet.print("  max = "+ max);
  		PApplet.print("  sum = "+ (float) sum);
  		PApplet.print("  mean = "+ mean);
  		PApplet.print("  median = "+ median);
  		PApplet.println("  sd = "+ standardDeviation);
  		PApplet.println("  skew = "+ skew);
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
   * requires app.brightness(), app.hue(), app.saturation()
   */
  public float[] pullChannel(int[] samples, ChannelNames chan) {
  	// convert sample channel to float array buf
  	float[] buf = new float[samples.length];
  	int i = 0;
  	switch (chan) {
  	case L: {
  		for (int argb : samples) buf[i++] = app.brightness(argb);
  		break;
  	}
  	case H: {
  		for (int argb : samples) buf[i++] = app.hue(argb);
  		break;
  	}
  	case S: {
  		for (int argb : samples) buf[i++] = app.saturation(argb);
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
   * requires: app.colorMode(), app.color(), app.hue(), app.brightness(), app.saturation()
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
  		app.colorMode(PApplet.HSB, 255);
  		for (float component : buf) {
  			int comp = Math.round((int) component); 
  			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
  			int argb = samples[i];
  			samples[i++] = app.color(Math.round(app.hue(argb)), Math.round(app.saturation(argb)), comp, 255);
  		}
  		break;
  	}
  	case H: {
  		app.colorMode(PApplet.HSB, 255);
  		for (float component : buf) {
  			int comp = Math.round((int) component); 
  			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
  			int argb = samples[i];
  			samples[i++] = app.color(comp, Math.round(app.saturation(argb)), Math.round(app.brightness(argb)), 255);
  		}
  		break;
  	}
  	case S: {
  		app.colorMode(PApplet.HSB, 255);
  		for (float component : buf) {
  			int comp = Math.round((int) component); 
  			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
  			int argb = samples[i];
  			samples[i++] = app.color(Math.round(app.hue(argb)), comp, Math.round(app.brightness(argb)), 255);
  		}
  		break;
  	}
  	case R: {
  		app.colorMode(PApplet.RGB, 255);
  		for (float component : buf)  {
  			int comp = Math.round((int) component); 
  			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
  			int argb = samples[i];
  			samples[i++] = 255 << 24 | comp << 16 | ((argb >> 8) & 0xFF) << 8 | argb & 0xFF;
  		}
  		break;
  	}
  	case G: {
  		app.colorMode(PApplet.RGB, 255);
  		for (float component : buf) {
  			int comp = Math.round((int) component); 
  			comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
  			int argb = samples[i];
  			samples[i++] = 255 << 24 | ((argb >> 16) & 0xFF) << 16 | comp << 8 | argb & 0xFF;
  		}
  		break;
  	}
  	case B: {
  		app.colorMode(PApplet.RGB, 255);
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
	 * requires: app.calculatedBands, app.bandList, app.eq
	 * 
	 * @param samples   an array of RGB values
	 * @param chan      the channel to pass through the FFT
	 * @return          the modified samples
	 */
	public int[] fftEqGlitch(int[] samples, ChannelNames chan) {
		// convert the selected channel to an array of floats
		float[] buf = pullChannel(samples, chan);
		// do a forward transform on the array of floats
		fft.forward(buf);
		// scale the frequencies in the fft by user-selected values from the equalizer interface
		for (int i = 0; i < app.calculatedBands; i++) {
			// get indices of the range of bands covered by each slider
			int pos = app.eq.length - i - 1;
			GlitchSort.IntRange ir = app.bandList.get(pos);
			// get the scaling value set by the user
			float scale = app.eq[pos];
			// scale all bands between lower and upper index
			for (int j = ir.lower; j <= ir.upper; j++) {
				fft.scaleBand(j, scale);
			}
		}
		// inverse the transform
		fft.inverse(buf);
		pushChannel(samples, buf, chan);
		return samples;
	}

	/**
	 * Performs a zigzag scan, centered in the image, and passes blocks 
	 * to an FFT transform that uses a user-supplied equalization curve.
	 * requires: app.img, app.statBufferSize, app.statFFTBlockWidth,
	 * app.isEqGlitchBrightness, app.isEqGlitchHue, app.isEqGlitchSaturation, 
	 * app.isEqGlitchRed, app.isEqGlitchGreen, app.isEqGlitchBlue,
	 * app.backup(), app.fitPixels()
	 * 
	 */
	public void eqZigzagFFT() {
		PImage img = app.img;
		int order = (int) Math.sqrt(app.statBufferSize);
		app.statFFTBlockWidth = order;
		HilbertScanner zz = new HilbertScanner(order);
		PApplet.println("Zigzag order = "+ order);
		int dw = (img.width / order);
		int dh = (img.height / order);
		int w = dw * order;
		int h = dh * order;
		int ow = (img.width - w) / 2;
		int oh = (img.height - h) / 2;
		app.backup();
		img.loadPixels();
		for (int y = 0; y < dh; y++) {
			for (int x = 0; x < dw; x++) {
				int mx = x * order + ow;
				int my = y * order + oh;
				// handle orientation of zigzag scan here
				//     			if (random(1) > 0.5f) {
				//    				zz.flipX();
				//    			}
				//     			if (random(1) > 0.5f) {
				//    				zz.flipY();
				//    			}
				int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
				// the samples are returned by fftEqGlitch, but they are modified already
				if (app.isEqGlitchBrightness) fftEqGlitch(pix, ChannelNames.L);
				if (app.isEqGlitchHue) fftEqGlitch(pix, ChannelNames.H);
				if (app.isEqGlitchSaturation) fftEqGlitch(pix, ChannelNames.S);
				if (app.isEqGlitchRed) fftEqGlitch(pix, ChannelNames.R);
				if (app.isEqGlitchGreen) fftEqGlitch(pix, ChannelNames.G);
				if (app.isEqGlitchBlue) fftEqGlitch(pix, ChannelNames.B);
				zz.plant(img.pixels, pix, img.width, img.height, mx, my);
			}
		}
		img.updatePixels();
		// necessary to call fitPixels to show updated image
		app.fitPixels(app.isFitToScreen, false);
	}
  
	/**
	 * Performs an FFT on a supplied array of samples, scales frequencies using settings in the 
	 * statistical interface, modifies the samples and also returns the modified samples. 
	 * requires: app.leftBound, app.rightBound, app.cut, app.boost
	 * 
	 * @param samples   an array of RGB values
	 * @param chan      the channel to pass through the FFT
	 * @return          the modified samples
	 */
	public float[] fftStatGlitch(int[] samples, ChannelNames chan) {
		// convert the selected channel to an array of floats
		float[] buf = pullChannel(samples, chan);
		// do a forward transform on the array of floats
		fft.forward(buf);
		// ignore first bin, the "DC component" if low frequency is cut
		// function removed, didn't seem particularly useful
		float[] stats = fftStat(0, buf.length, false, "fft "+ chan.name());
		float min = stats[0];
		float max = stats[1];
		float mean = stats[2];
		float median = stats[3];
		float sd = stats[4];
		float skew = stats[5];
		int t = samples.length / 2;
		// typical values: left = 0.5f, right = 2.0f
		//		float leftEdge = mean - sd * app.leftBound;
		//		float rightEdge = mean + sd * app.rightBound;
		float leftEdge = app.leftBound < 0 ? mean - sd * -app.leftBound : mean + sd * app.leftBound;
		float rightEdge = app.rightBound < 0 ? mean - sd * -app.rightBound : mean + sd * app.rightBound;
		//		println("min = "+ min +", max = "+ max +", mean = "+ mean +", median = "+ median +", sd = " + sd  +", skew = "+ skew +", app.leftBound = "+ app.leftBound +", rightBound = "+ rightBound);		
		//		println("-- leftEdge = "+ leftEdge +", rightEdge = "+ rightEdge	);
		// scale the frequencies in the fft, skipping band 0
		for (int i = 1; i < t; i++) {
			float val = fft.getBand(i);
			// frequencies whose amplitudes lie outside the bounds are scaled by the cut value
			if (val < leftEdge || val > rightEdge) fft.scaleBand(i, app.cut);
			// frequencies whose amplitudes lie inside the bounds are scaled by the boost value
			else {
				fft.scaleBand(i, app.boost);
			}
		}
		// inverse the transform
		fft.inverse(buf);
		pushChannel(samples, buf, chan);
		return stats;
	}

	/**
	 * Performs a zigzag scan, centered in the image, and passes blocks 
	 * to an FFT transform that uses statistical analysis to determine frequency scaling.
	 * requires: app.img, app.statBufferSize, app.statFFTBlockWidth, app.backup(),
	 * app.isStatGlitchBrightness, app.isStatGlitchHue, app.isStatGlitchSaturation, 
	 * app.isStatGlitchRed, app.isStatGlitchGreen, app.isStatGlitchBlue 
	 * app.leftBound, app.rightBound, app.twoPlaces.format(), app.fitPixels()
	 * 
	 * @param order   the width/height of each pixel block to sort
	 */
	public void statZigzagFFT() {
		PImage img = app.img;
		int order = (int) Math.sqrt(app.statBufferSize);
		app.statFFTBlockWidth = order;
		// eliminate fft averaging, don't need it
		// fft.logAverages(minBandWidth, bandsPerOctave);
		HilbertScanner zz = new HilbertScanner(order);
		PApplet.println("Zigzag order = "+ order);
		int dw = (img.width / order);
		int dh = (img.height / order);
		int totalBlocks = dw * dh;
		int w = dw * order;
		int h = dh * order;
		int ow = (img.width - w) / 2;
		int oh = (img.height - h) / 2;
		float min = 0, max = 0, mean = 0, median = 0, sd = 0, skew = 0;
		float[] stats = new float[6];
		app.backup();
		img.loadPixels();
		for (int y = 0; y < dh; y++) {
			for (int x = 0; x < dw; x++) {
				int mx = x * order + ow;
				int my = y * order + oh;
				//     			if (random(1) > 0.5f) {
				//    				zz.flipX();
				//    			}
				//     			if (random(1) > 0.5f) {
				//    				zz.flipY();
				//    			}
				int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
				if (app.isStatGlitchBrightness) stats = fftStatGlitch(pix, ChannelNames.L);
				if (app.isStatGlitchHue) stats = fftStatGlitch(pix, ChannelNames.H);
				if (app.isStatGlitchSaturation) stats = fftStatGlitch(pix, ChannelNames.S);
				if (app.isStatGlitchRed) stats = fftStatGlitch(pix, ChannelNames.R);
				if (app.isStatGlitchGreen) stats = fftStatGlitch(pix, ChannelNames.G);
				if (app.isStatGlitchBlue) stats = fftStatGlitch(pix, ChannelNames.B);
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
		float leftEdge = app.leftBound < 0 ? mean - sd * -app.leftBound : mean + sd * app.leftBound;
		float rightEdge = app.rightBound < 0 ? mean - sd * -app.rightBound : mean + sd * app.rightBound;
		PApplet.println("---- Average statistical values for image before FFT ----");
		PApplet.println("  min = "+ app.twoPlaces.format(min) +", max = "+ app.twoPlaces.format(max) +", mean = "+ app.twoPlaces.format(mean) 
				+", median = "+ app.twoPlaces.format(median) +", sd = " + app.twoPlaces.format(sd)  +", skew = "+ app.twoPlaces.format(skew));		
		PApplet.println("  leftEdge = "+ app.twoPlaces.format(leftEdge) +", rightEdge = "+ app.twoPlaces.format(rightEdge) 
				+", leftBound = "+ app.leftBound +", rightBound = "+ app.rightBound);
		img.updatePixels();
		// necessary to call fitPixels to show updated image
		app.fitPixels(app.isFitToScreen, false);
		//		analyzeEq(false);
	}

	
}
