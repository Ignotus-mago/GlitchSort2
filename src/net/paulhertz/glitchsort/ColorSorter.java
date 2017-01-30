package net.paulhertz.glitchsort;

import static net.paulhertz.glitchsort.GlitchConstants.*;

/**
 * @author paulhz
 * Extends Sorter with methods required for sorting color arrays in GlitchSort.
 */
public interface ColorSorter extends Sorter {
	/**
	 * @return the isRandomBreak
	 */
	public boolean isRandomBreak();

	/**
	 * @param isRandomBreak the isRandomBreak to set
	 */
	public void setRandomBreak(boolean isRandomBreak);

	public float getBreakPoint();

	public void setBreakPoint(float breakPoint);
	
	/**
	 * @return the isSwapChannels
	 */
	public boolean isSwapChanels();

	/**
	 * @param isSwapChannels the isSwapChannels to set
	 */
	public void setSwapChannels(boolean isSwapChannels);

	/**
	 * @return the swapWeight
	 */
	public float getSwapWeight();

	/**
	 * @param swapWeight the swapWeight to set
	 */
	public void setSwapWeight(float swapWeight);

	/**
	 * @return the isAscendingSort
	 */
	public boolean isAscendingSort();

	/**
	 * @param isAscendingSort the isAscendingSort to set
	 */
	public void setAscendingSort(boolean isAscendingSort);

	public SorterType getSorterType();
	
	/**
	 * @return the compOrder
	 */
	public CompOrder getCompOrder();

	/**
	 * @param compOrder the compOrder to set
	 */
	public void setCompOrder(CompOrder compOrder);

	/**
	 * @return the swap
	 */
	public SwapChannel getSwap();
	/**
	 * @param swap the swap to set
	 */
	public void setSwap(SwapChannel swap);

}
