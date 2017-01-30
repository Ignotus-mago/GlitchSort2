package net.paulhertz.glitchsort;

/**
 * @author paulhz
 * Constant utility class. Preferred over placing constants in interfaces. 
 * May make export to Processing a little easier.
 */
public class GlitchConstants {
	// Prevent instantiation by declaring private constructor.
	private GlitchConstants() { }
	

	/** List of available color channels, "L" for lightness, since "B" for brightness is taken */
	public enum ChannelNames {
		R, G, B, H, S, L;
	}

	/** List of different component orders for sorting pixels */
	public enum CompOrder {
		RGB, RBG, GBR, GRB, BRG, BGR, HSB, HBS, SBH, SHB, BHS, BSH;
	}

	/** List of available sorting methods */
	public enum SorterType {
		QUICK, SHELL, BUBBLE, INSERT;
	}

	/** List of possible channel swaps between source and target */
	public enum SwapChannel {
		RR, RG, RB, GR, GG, GB, BR, BG, BB;
	}
	
	/** List of possible zigzag sorting styles: aligned, random, or four different orientations permuted in blocks of four */
	public enum ZigzagStyle {
		RANDOM, ALIGN, PERMUTE;
	}
	
	/**	List of zizzag orientations, in an order easily referenced by bitwise values 00, 01, 10, 11 */
	public enum ZigzagCorner {
		TOPLEFT, BOTTOMLEFT, TOPRIGHT, BOTTOMRIGHT;
	}
	
}
