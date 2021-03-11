package net.paulhertz.glitchsort;

import java.awt.Rectangle;
import java.util.List;
import net.paulhertz.glitchsort.GlitchConstants.ChannelNames;
import net.paulhertz.glitchsort.GlitchConstants.SorterType;
import net.paulhertz.glitchsort.GlitchConstants.SwapChannel;
import net.paulhertz.glitchsort.GlitchConstants.ZigzagStyle;
import net.paulhertz.glitchsort.GlitchSort.IntRange;
import processing.core.*;
import controlP5.*;

// audio operations with minim
import ddf.minim.*;

/**
 * @author paulhz
 * Manages controls and layout on control panel. Future: consider a separate class for each
 * functionality, with its own capacity to create controls (via a control format tool), 
 * to handle events, and to save and store state. 
 */
public class ControlPanelManager extends PApplet {
	private ControlP5 control;
	private GlitchSort app;
	int w;
	int h;
	int panelBackColor;
	int yPos = 6;
	int step = 18;
	int widgetH = 14;
	int labelW = 144;
	int panelHeight = 408;
	int panelWidth = 284;
	int panelX = 4;
	int panelY = 36;
	int controlPanelHeight = 480;
	int controlPanelWidth = 284;
	int controlPanelX = 4;
	int controlPanelY = 36;
	//
	Group glitchSettings;
	Group fftSettings;
	Group synthSettings;
	Group mungeSettings;
	Group jpegSettings;
	Group audifySettings;
	Tab glitchSettingsTab;
	Tab fftSettingsTab;
	Tab synthSettingsTab;
	Tab mungeSettingsTab;
	Tab jpegSettingsTab;
	Tab audifySettingsTab;
	Rectangle controlPanelRect;
	String sliderIdentifier = "_eq";
	//
	/*
	 * text label count 27
	 * slider count 5
	 * checkbox count 10
	 */

	// TODO create local controlP5, put controls in their own window
	public ControlPanelManager(GlitchSort parent, int width, int height) {
		this.app = parent;
		this.w = width;
		this.h = height;
	}
	
	// force instantiation to our public constructors
	private ControlPanelManager() {
		
	}
	
	public void setup() {
		size(w, h);
		// this.controlP5 = app.controlP5;
		this.control = new ControlP5(this);
		panelBackColor = app.color(123, 123, 144, 255);
		// load Glitch panel
		loadGlitchPanel();
		this.glitchSettingsTab.setActive(true);
		// load Munge panel
		loadMungePanel();
		// load FFT panel
		loadFFTPanel(app.eqH, app.eqMin, app.eqMax);
		// load Synth panel (placeholder code)
		// loadSynthPanel();
		// load Audify panel
		loadAudifyPanel();
		// initialize panel glitchSettings, now that panels are loaded
		app.initPanelSettings();
		// setup for formant glitching
		app.setupFormants();
	}
	
	public void draw() {
		trackMouseEq();		
	}
	
	/* (non-Javadoc)
	 * handles key presses intended as commands
	 * pass them to host app
	 * @see processing.core.PApplet#keyPressed()
	 */
	public void keyPressed() {
		app.parseKey(key, keyCode);
	}	
	
	public ControlP5 getControl() {
		return control;
	}
	public void setControl(ControlP5 controlP5) {
		this.control = controlP5;
	}
	

	/**
	 * @return the panelBackColor
	 */
	public int getPanelBackColor() {
		return panelBackColor;
	}
	/**
	 * @param panelBackColor the panelBackColor to set
	 */
	public void setPanelBackColor(int panelBackColor) {
		this.panelBackColor = panelBackColor;
	}


	/**
	 * returns current vertical position for widget creation
	 * @return the yPos
	 */
	public int getyPos() {
		return yPos;
	}
	/**
	 * Sets vertical position for widget creation.
	 * Call before setting up panels to set default initial vertical position
	 * @param yPos the yPos to set
	 */
	public void setyPos(int yPos) {
		this.yPos = yPos;
	}


	/**
	 * returns the default vertical distance between widgets
	 * @return the step
	 */
	public int getStep() {
		return step;
	}
	/**
	 * sets the default vertical distance between widgets
	 * @param step the step to set
	 */
	public void setStep(int step) {
		this.step = step;
	}

	
	/**
	 * returns the default height of new widgets
	 * @return the widgetH
	 */
	public int getWidgetH() {
		return widgetH;
	}
	/**
	 * sets the default height of new widgets
	 * @param widgetH the widgetH to set
	 */
	public void setWidgetH(int widgetH) {
		this.widgetH = widgetH;
	}


	/**
	 * returns the default width of new label text
	 * @return the labelW
	 */
	public int getLabelW() {
		return labelW;
	}
	/**
	 * sets the default width of new label text
	 * @param labelW the labelW to set
	 */
	public void setLabelW(int labelW) {
		this.labelW = labelW;
	}


	/**
	 * returns the default height of a new panel
	 * @return the panelHeight
	 */
	public int getPanelHeight() {
		return panelHeight;
	}
	/**
	 * sets the default height of a new panel
	 * @param panelHeight the panelHeight to set
	 */
	public void setPanelHeight(int panelHeight) {
		this.panelHeight = panelHeight;
	}


	/**
	 * returns the default width of a new panel
	 * @return the panelWidth
	 */
	public int getPanelWidth() {
		return panelWidth;
	}
	/**
	 * sets the default width of a new panel
	 * @param panelWidth the panelWidth to set
	 */
	public void setPanelWidth(int panelWidth) {
		this.panelWidth = panelWidth;
	}
	

	/**
	 * @return the panelX
	 */
	public int getPanelX() {
		return panelX;
	}
	/**
	 * @param panelX the panelX to set
	 */
	public void setPanelX(int panelX) {
		this.panelX = panelX;
	}
	

	/**
	 * @return the panelY
	 */
	public int getPanelY() {
		return panelY;
	}
	/**
	 * @param panelY the panelY to set
	 */
	public void setPanelY(int panelY) {
		this.panelY = panelY;
	}

	
    /***********************************************************/
    /*                                                         */
    /*          >>> CONTROL PANEL UTILITY METHODS <<<          */
    /*                                                         */
    /***********************************************************/
	
	/**
	 * formats radio button style
	 * @param rb      a ControlP5  RadioButton instance 
	 * @param width   width of radio button.
	 */
	void setRadioButtonStyle(RadioButton rb, int width) {
		for (Toggle t: rb.getItems()) {
			t.setColorForeground(app.color(233, 233, 0));
			Label l = t.getCaptionLabel();
			l.enableColorBackground();
			l.setColorBackground(app.color(80));
			l.getStyle().movePadding(2,0,-1,2);
			l.getStyle().moveMargin(-2,0,0,-3);
			l.getStyle().backgroundWidth = width;
		}
	}
	
	/**
	 * creates a button.
	 * @param command   name of the command to be called by the button
	 * @param x         x position
	 * @param y         y position
	 * @param w         width
	 * @param h         height
	 * @param g         ControlP5 Group instance the button belongs to
	 * @param name      the button's name (for display)
	 * @return          an initialized ControlP5 Button object
	 */
	Button createButton(String command, int x, int y, int w, int h, Group g, String name) {
		// olde code
		// Button b = controlP5.addButton(command, 0).setPosition(x, y).setSize(w, h);
		Button b = control.addButton(command).setPosition(x, y).setSize(w, h);
		b.plugTo(app, command).setDefaultValue(0);
		b.setGroup(g);
		b.getCaptionLabel().set(name);
		return b;
	}

	/**
	 * create a set of checkboxes
	 * @param command        the command to execute on activation of a checkbox
	 * @param x              x position
	 * @param y              y position
	 * @param count          number of individual checkboxes
	 * @param spacing        spacing between checkboxe, in pixels
	 * @param g              ControlP5 Group instance the boxes belong to
	 * @param foreColor      foreground color
	 * @param activeColor    color when activated
	 * @param labelColor     color of label text
	 * @return               an initialized ControlP5 CheckBox object
	 */
	CheckBox createCheckBox(String command, int x, int y, int count, int spacing, Group g, int foreColor, int activeColor, int labelColor) {
		CheckBox ch = control.addCheckBox(command).setPosition(x, y);
		// rb.plugTo(app, command);  // controlEvent(ControlEvent evt) traps events
		ch.setGroup(g);
		ch.setColorForeground(foreColor);
		ch.setColorActive(activeColor);
		ch.setColorLabel(labelColor);
		ch.setItemsPerRow(count);
		ch.setSpacingColumn(spacing);
		return ch;
	}

	/**
	 * Creates a set of ControP5 radio buttons.
	 * @param command        the command to execute when a radio button is activated
	 * @param x              x position
	 * @param y              y position
	 * @param count          number of radio buttons in the set
	 * @param spacing        space between buttons, in pixels
	 * @param g              ControlP5 Group the radio buttons belong to
	 * @param foreColor      foreground color
	 * @param activeColor    color wehn activated
	 * @param labelColor     color of label text
	 * @return               an initialized ControlP5 RadioButton instance
	 */
	RadioButton createRadioButton(String command, int x, int y, int count, int spacing, Group g, int foreColor, int activeColor, int labelColor) {
		RadioButton rb = control.addRadioButton(command).setPosition(x, y);
		// rb.plugTo(app, command);  // controlEvent(ControlEvent evt) traps events
		rb.setGroup(g);
		rb.setColorForeground(foreColor);
		rb.setColorActive(activeColor);
		rb.setColorLabel(labelColor);
		rb.setItemsPerRow(count);
		rb.setSpacingColumn(spacing);
		return rb;
	}
	
	/*
		Numberbox n1 = controlP5.addNumberbox("setBreakpoint", app.breakPoint, 8, yPos, 100, widgetH);
		n1.setGroup(glitchSettings);
		n1.setMultiplier(1f);
		n1.setDecimalPrecision(1);
		n1.setMin(1.0f);
		n1.setMax(999.0f);
		n1.getCaptionLabel().set("");
		// label for breakPoint number box
		Textlabel l2 = controlP5.addTextlabel("breakpointLabel", "Breakpoint: " + app.sortTool.sorter.getSorterType().toString(), 112, yPos + 4);
		l2.setGroup(glitchSettings);
	 */


    /******************************************/
    /*                                        */
    /*          >>> GLITCH PANEL <<<          */
    /*                                        */
    /******************************************/
	
	/**
	 * Initializes and arranges the "glitch" control panel widgets
	 * TODO control panel
	 */
	public void loadGlitchPanel() {
		int spacer = 4;
		int foreColor = app.color(120);
		int activeColor = app.color(255);
		int labelColor = app.color(255);
		glitchSettings = control.addGroup("Glitch", panelX, panelY, panelWidth);
		glitchSettings.setBackgroundColor(panelBackColor);
		glitchSettings.setBackgroundHeight(panelHeight);
		glitchSettings.setBarHeight(widgetH + 4);
		glitchSettings.setMoveable(false);     // option-drag on bar to move menu not permitted
		// add widgets
		// row of buttons: open, save, revert
		createButton("openFile", 8, yPos, 76, widgetH, glitchSettings, "Open (o)");
		createButton("saveFile", panelWidth/3 + 4, yPos, 76, widgetH, glitchSettings, "Save (s)");
		createButton("saveFileAs", 2 * panelWidth/3 + 4, yPos, 76, widgetH, glitchSettings, "Save as (^s)");
		// fit to screen/show all pixels toggle, rotate and undo buttons
		yPos += step;
		createButton("saveFileCopy", 8, yPos, 76, widgetH, glitchSettings, "Save copy");
		createButton("revert", panelWidth/3 + 4, yPos, 76, widgetH, glitchSettings, "Revert (r)");
		createButton("restore", 2 * panelWidth/3 + 4, yPos, 76, widgetH, glitchSettings, "Undo (z)");
		//
		yPos += step;
		CheckBox ch0 = createCheckBox("fitPixels", 8, yPos + 2, 3, (panelWidth - 8)/4, glitchSettings, foreColor, activeColor, labelColor);
		// add items to the checkbox
		ch0.addItem("Fit To Screen (f)", 1);
		ch0.setColorForeground(app.color(233, 233, 0));
//		CheckBox ch1 = createCheckBox("setSequence", panelWidth/3 + 4, yPos + 2, 3, (panelWidth - 8)/4, glitchSettings, foreColor, activeColor, labelColor);
//		// add items to the checkbox
//		ch1.addItem("Sequence", 1);
//		ch1.setColorForeground(app.color(233, 233, 0));
//		ch1.getItem(0).setState(true);
		createButton("resetCount", panelWidth/3 + 4, yPos, 76, widgetH, glitchSettings, "Reset count");	
		createButton("rotatePixels", 2 * panelWidth/3 + 4, yPos, 76, widgetH, glitchSettings, "Turn 90 (t)");
		// sorting section
		yPos += step + spacer;
		Textlabel l1 = control.addTextlabel("sorterLabel", "Sorting", 8, yPos);
		l1.setGroup(glitchSettings);
		Textlabel l1u = control.addTextlabel("sortingLabelUnder", "________________________________", 8, yPos + 3);
		l1u.setGroup(glitchSettings);
		// sort  button
		yPos += step;
		createButton("sortPixels", 8, yPos, 76, widgetH, glitchSettings, "Sort (g)");
		// sorter selection radio buttons
		yPos += step + 2;
		RadioButton r1 = createRadioButton("setSorter", 8, yPos, 5, 40, glitchSettings, foreColor, activeColor, labelColor);
		r1.setNoneSelectedAllowed(false);
		// enum SorterType {QUICK, SHELL, BUBBLE, INSERT;} 
		int n = 0;
		labelW = 32;
		r1.addItem("QUICK", n++);
		r1.addItem("SHELL", n++);
		r1.addItem("BUBBLE", n++);
		r1.addItem("INSERT", n++);
		setRadioButtonStyle(r1, labelW);
		/* r1.activate("QUICK"); */ // will throw a (non-fatal but annoying) error, see startup method
		// sorting checkboxes
		yPos += step - 4;
		CheckBox ch2 = createCheckBox("Sorting", 8, yPos, 3, (panelWidth - 8)/4, glitchSettings, foreColor, activeColor, labelColor);
		// add items to the checkbox
		ch2.addItem("Ascending", 1);
		ch2.addItem("Break", 2);
		ch2.addItem("Swap", 3);
		ch2.setColorForeground(app.color(233, 233, 0));
		ch2.activate(1);
		// breakPoint number box
		yPos += step;
		Numberbox n1 = control.addNumberbox("setBreakpoint").setPosition(8, yPos).setSize(100, widgetH);
		n1.plugTo(app, "setBreakpoint").setValue(app.breakPoint);
		n1.setGroup(glitchSettings);
		n1.setMultiplier(1f);
		n1.setDecimalPrecision(1);
		n1.setMin(1.0f);
		n1.setMax(999.0f);
		n1.getCaptionLabel().set("");
		// label for breakPoint number box
		Textlabel l2 = control.addTextlabel("breakpointLabel", "Breakpoint: " + app.sortTool.sorter.getSorterType().toString(), 112, yPos + 4);
		l2.setGroup(glitchSettings);
		// glitchSteps slider
		yPos += step;
		Slider s1;
		s1 = control.addSlider("setGlitchSteps").setPosition(8, yPos).setSize(101, widgetH);
		s1.plugTo(app, "setGlitchSteps").setValue(1).setMin(1).setMax(100.1f);
		s1.setGroup(glitchSettings);
		s1.setDecimalPrecision(0);
		s1.getCaptionLabel().set("");
		s1.setSliderMode(Slider.FLEXIBLE);
		// label for glitchSteps slider
		Textlabel l3 = control.addTextlabel("glitchStepsLabel", "Steps = "+ (int)app.glitchSteps, 112, yPos + 4);
		l3.setGroup(glitchSettings);
		// cycle checkbox
		CheckBox ch3 = createCheckBox("Glitchmode", 2 * panelWidth/3 + 8, yPos + 2, 3, (panelWidth - 8)/4, glitchSettings, foreColor, activeColor, labelColor);
		// add items to the checkbox
		ch3.addItem("Cycle", 1);
		ch3.setColorForeground(app.color(233, 233, 0));
		ch3.deactivate(0);
		// line count
		yPos += step;
		// s4tick = controlP5.addSlider("setLineTick", 0, 1024, app.lineTick, 8, yPos, 64, widgetH);
		Slider s4tick = control.addSlider("setLineTick").setPosition(8, yPos).setSize(64, widgetH);
		s4tick.plugTo(app, "setLineTick").setMin(0).setMax(1024).setValue(app.lineTick);
		s4tick.setGroup(glitchSettings);
		s4tick.setDecimalPrecision(0);
		s4tick.getCaptionLabel().set("");
		s4tick.setSliderMode(Slider.FLEXIBLE);
		s4tick.setNumberOfTickMarks(9);
		// Slider s4 = controlP5.addSlider("setLineAdd", 0, 127, app.lineAdd, 80, yPos, 127, widgetH);
		Slider s4 = control.addSlider("setLineAdd").setPosition(80, yPos).setSize(127, widgetH);
		s4.plugTo(app, "setLineAdd").setMin(0).setMax(127).setValue(app.lineAdd);
		s4.setGroup(glitchSettings);
		s4.setDecimalPrecision(0);
		s4.getCaptionLabel().set("");
		s4.setSliderMode(Slider.FLEXIBLE);
		// label for line count slider
		Textlabel l10 = control.addTextlabel("lineCountLabel", "Lines = "+ app.lineCount, 212, yPos + 4);
		l10.setGroup(glitchSettings);
		// sort order
		// label the radio button group
		yPos += step + 8;
		Textlabel l5 = control.addTextlabel("compOrderLabel", "Component Sorting Order:", 8, yPos + 4);
		l5.setGroup(glitchSettings);
		// move to next row
		yPos += step;
		RadioButton r2 = createRadioButton("setCompOrder", 8, yPos, 6, 32, glitchSettings, foreColor, activeColor, labelColor);
		r2.setSpacingRow(4);
		// enum CompOrder {RGB, RBG, GBR, GRB, BRG, BGR, HSB, HBS, SBH, SHB, BHS, BSH;}
		n = 0;
		labelW = 24;
		r2.addItem("RGB", n++);
		r2.addItem("RBG", n++);
		r2.addItem("GBR", n++);
		r2.addItem("GRB", n++);
		r2.addItem("BRG", n++);
		r2.addItem("BGR", n++);
		r2.addItem("HSB", n++);
		r2.addItem("HBS", n++);
		r2.addItem("SBH", n++);
		r2.addItem("SHB", n++);
		r2.addItem("BHS", n++);
		r2.addItem("BSH", n++);
		setRadioButtonStyle(r2, labelW);
		r2.setNoneSelectedAllowed(false);
		/* r2.activate("RGB"); */ // will throw a (non-fatal but annoying) error, see startup method
		// channel swap
//		yPos += step + step/2;
//		Textlabel l6 = controlP5.addTextlabel("swapLabel", "Swap Channels:", 8, yPos + 4);
//		l6.setGroup(glitchSettings);
		yPos += step + step - 4;
		int inset = 72;
		int spacing = 30;
		Textlabel l7 = control.addTextlabel("sourceLabel", "Swap Source:", 4, yPos);
		l7.setGroup(glitchSettings);
		RadioButton r3 = createRadioButton("setSourceChannel", inset, yPos, 3, spacing, glitchSettings, foreColor, activeColor, labelColor);
		n = 0;
		r3.addItem("R1", n++);
		r3.addItem("G1", n++);
		r3.addItem("B1", n++);
		setRadioButtonStyle(r3, labelW);
		r3.setNoneSelectedAllowed(false);
		// TODO
		// swapWeight number box
		// Numberbox n3 = controlP5.addNumberbox("setSwapWeight", app.swapWeight * 100, 218, yPos, 48, widgetH);
		Numberbox n3 = control.addNumberbox("setSwapWeight").setPosition(218, yPos).setSize(48, widgetH);
		n3.plugTo(app, "setSwapWeight").setValue(app.swapWeight * 100);
		n3.setGroup(glitchSettings);
		n3.setMultiplier(1f);
		n3.setDecimalPrecision(1);
		n3.setMin(0.0f);
		n3.setMax(100.0f);
		n3.getCaptionLabel().set("");
		// label for zigzagPercent number box
		Textlabel l10b = control.addTextlabel("swapWeightLabel", "%:", 198, yPos + 2);
		l10b.setGroup(glitchSettings);
		yPos += step;
		Textlabel l8 = control.addTextlabel("targetLabel", "Swap Target:", 4, yPos);
		l8.setGroup(glitchSettings);
		RadioButton r4 = createRadioButton("setTargetChannel", inset, yPos, 3, spacing, glitchSettings, foreColor, activeColor, labelColor);
		n = 0;
		r4.addItem("R2", n++);
		r4.addItem("G2", n++);
		r4.addItem("B2", n++);
		setRadioButtonStyle(r4, labelW);
		r4.setNoneSelectedAllowed(false);
		// zigzag intRange
		yPos += step;
		// use values that permit full stepwise range
		// addRange(name, min, max, defaultMin, defaultMax, x, y, w, h) 
		// Range r01 = controlP5.addRange("setZigzagRange", 4, 144, 8, 64, 8, yPos, 160, widgetH);
		Range r01 = control.addRange("setZigzagRange").plugTo(app, "setZigzagRange").setMin(4).setMax(144)
				.setLowValue(8).setHighValue(64).setPosition(8, yPos).setSize(160, widgetH);
		r01.setGroup(glitchSettings);
		r01.setDecimalPrecision(0);
		r01.setLowValue(8);
		r01.setHighValue(64);
		r01.getCaptionLabel().set("");
		// label for zigzag range slider
		Textlabel lineCountLabel = control.addTextlabel("zigzagRangeLabel", "Z Range", 170, yPos + 4);
		lineCountLabel.setGroup(glitchSettings);
		// zigzag button
		createButton("zigzag", 2 * panelWidth/3 + 28, yPos, 60, widgetH, glitchSettings, "Zigzag (l)");
		yPos += step;
		// zigzag sorting style
		RadioButton r6 = createRadioButton("setZigzagStyle", 8, yPos, 3, 48, glitchSettings, foreColor, activeColor, labelColor);
		n = 0;
		labelW = 40;
		r6.addItem("Random", n++);
		r6.addItem("Align", n++);
		r6.addItem("Permute", n++);
		setRadioButtonStyle(r6, labelW);
		r6.setNoneSelectedAllowed(false);
		// zigzagPercent number box
		// Numberbox n2 = controlP5.addNumberbox("setZigzagPercent", app.zigzagPercent, 218, yPos, 48, widgetH);
		Numberbox n2 = control.addNumberbox("setZigzagPercent").setPosition(218, yPos).setSize(48, widgetH);
		n2.plugTo(app, "setZigzagPercent").setValue(app.zigzagPercent);
		n2.setGroup(glitchSettings);
		n2.setMultiplier(1f);
		n2.setDecimalPrecision(1);
		n2.setMin(1.0f);
		n2.setMax(100.0f);
		n2.getCaptionLabel().set("");
		// label for zigzagPercent number box
		Textlabel l10a = control.addTextlabel("zigzagPercentLabel", "%:", 198, yPos + 2);
		l10a.setGroup(glitchSettings);		
		// create glitch glitchSettings tab
		Tab global = control.getTab("default");
		global.setLabel("");
		global.hide();
		glitchSettings.moveTo("glitch");
		glitchSettingsTab = control.getTab("glitch");
		glitchSettingsTab.activateEvent(true);
		glitchSettingsTab.disableCollapse();
		glitchSettingsTab.setLabel("  Glitch  ");
		glitchSettingsTab.setId(1);
	}

	
    /***************************************/
    /*                                     */
    /*          >>> FFT PANEL <<<          */
    /*                                     */
    /***************************************/
 
	
	/**
	 * Initializes and arranges the FFT control panel widgets
	 * TODO FFT panel
	 */
	public void loadFFTPanel(int h, float min, float max) {
		int foreColor = app.color(120);
		int activeColor = app.color(255);
		int labelColor = app.color(255);
		//
		yPos = 6;
		step = 18;
		int offset = -2;
		fftSettings = control.addGroup("FFT", panelX, panelY, panelWidth);
		fftSettings.setBackgroundColor(panelBackColor);
		fftSettings.setBackgroundHeight(panelHeight);
		fftSettings.setBarHeight(widgetH + 4);
		fftSettings.setMoveable(false);     // dragging throws absolute position off...
		// add widgets
		// legend
		Textlabel l12 = control.addTextlabel("equalizerLabel", "Equalizer FFT", 8, yPos);
		l12.setGroup(fftSettings);		
		Textlabel l12u = control.addTextlabel("equalizerLabelUnder", "________________________________", 8, yPos + 3);
		l12u.setGroup(fftSettings);		
		// row of buttons: 
		yPos += step + offset;
		createButton("eqZigzagFFT", 8, yPos, 40, widgetH, fftSettings, "Run (j)");
		createButton("resetEq", panelWidth/5, yPos, 48, widgetH, fftSettings, "Reset");
		//// TODO incorporate analysis into FFT ?
		createButton("analyzeEqBands", 2 * panelWidth/5, yPos, 48, widgetH, fftSettings, "Anal (;)");
		createButton("desaturate", 3 * panelWidth/5, yPos, 48, widgetH, fftSettings, "Desat");
		createButton("randomBands", 4 * panelWidth/5, yPos, 48, widgetH, fftSettings, "Random");
		yPos += step + offset;
		// label at bottom of eQ bands
		Textlabel l13 = control.addTextlabel("eqLabel", "----", 8, yPos + h + step/2);
		l13.setGroup(fftSettings);
		// equalizer
		setupEqualizer(yPos, h, max, min);
		showEqualizerBands();
		yPos += h + step + step/2 + offset;
		// HSB/RGB checkboxes for equalizer-controlled FFT
		CheckBox ch4 = createCheckBox("ChanEq", 8, yPos + 2, 3, (panelWidth - 8)/4, fftSettings, foreColor, activeColor, labelColor);
		// add items to the checkbox, note that we can't use names that start with sliderIdentifier "_eq" 
		ch4.addItem("eqBrightness", 1);
		ch4.getItem(0).setCaptionLabel("Brightness");
		//ch4.setColorForeground(app.color(233, 233, 0));
		ch4.addItem("eqHue", 2);
		ch4.getItem(1).setCaptionLabel("Hue");
		//ch4.setColorForeground(app.color(233, 233, 0));
		ch4.addItem("eqSaturation", 3);
		ch4.getItem(2).setCaptionLabel("Saturation");
		//ch4.setColorForeground(app.color(233, 233, 0));
		// add items to the checkbox
		ch4.addItem("eqRed", 4);
		ch4.getItem(3).setCaptionLabel("Red");
		//ch4.setColorForeground(app.color(233, 233, 0));
		ch4.addItem("eqGreen", 5);
		ch4.getItem(4).setCaptionLabel("Green");
		//ch4.setColorForeground(app.color(233, 233, 0));
		ch4.addItem("eqBlue", 6);
		ch4.getItem(5).setCaptionLabel("Blue");
		ch4.setColorForeground(app.color(233, 233, 0));
		/* get ready for separate eq and stat FFT controls 
		// slider for equalizer FFT block size 
		// watch out for a concurrent modification error when we regenerate the FFT panel
		yPos +=  step + step/2;
		Slider s5 = controlP5.addSlider("setEqFFTBlockWidth", 3, 10, 6, 8, yPos, 72, widgetH); 
		s5.setGroup(fftSettings);
		s5.setDecimalPrecision(0);
		s5.getCaptionLabel().set("");
		Textlabel l15 = controlP5.addTextlabel("eqBlockSizeLabel", "Equalizer FFT Block Size = "+ app.statFFTBlockWidth, 84, yPos + 2);
		l15.setGroup(fftSettings);
		*/
		// statistical FFT settings section
		// section label
		yPos += 2 * step + offset;
		Textlabel l14 = control.addTextlabel("statFFTLabel", "Statistical FFT", 8, yPos);
		l14.setGroup(fftSettings);
		Textlabel l14u = control.addTextlabel("statFFTLabelUnder", "________________________________", 8, yPos + 3);
		l14u.setGroup(fftSettings);		
		// buttons
		yPos += step + offset;
		createButton("statZigzagFFT", 8, yPos, 64, widgetH, fftSettings, "Run (k)");
		createButton("resetStat", panelWidth/3, yPos, 64, widgetH, fftSettings, "Reset");
		//------- begin slider
		// use a range slider for bounds
		yPos += step;
		// addRange(name, min, max, defaultMin, defaultMax, x, y, w, h) 
		// Range r02 = controlP5.addRange("setStatEqRange", -5.0f, 5.0f, app.leftBound, app.rightBound, 8, yPos, 180, widgetH);
		Range r02 = control.addRange("setStatEqRange").plugTo(app, "setStatEqRange").setMin(-5.0f).setMax(5.0f)
				.setLowValue(app.leftBound).setHighValue(app.rightBound).setPosition(8, yPos).setSize(180, widgetH);
		r02.setGroup(fftSettings);
		r02.setDecimalPrecision(2);
		r02.setLowValue(app.leftBound);
		r02.setHighValue(app.rightBound);
		r02.getCaptionLabel().set("");
		// label for statistical eQ range slider
		Textlabel l16 = control.addTextlabel("statEqRangeLabel", "Deviation", 190, yPos + 4);
		l16.setGroup(fftSettings);
		//------- end slider
		// number box for boost
		yPos += step;
		//Numberbox n4 = controlP5.addNumberbox("setBoost", app.boost, 8, yPos, 40, widgetH);
		Numberbox n4 = control.addNumberbox("setBoost").setPosition(8, yPos).setSize(40, widgetH);
		n4.plugTo(app, "setBoost").setValue(app.boost);
		n4.setGroup(fftSettings);
		n4.setMultiplier(0.01f);
		n4.setDecimalPrecision(2);
		n4.setMin(0.0f);
		n4.setMax(8.0f);
		n4.getCaptionLabel().set("");
		// label for boost number box
		Textlabel l17 = control.addTextlabel("boostLabel", "IN Scale", 48, yPos + 4);
		l17.setGroup(fftSettings);
		// number box for cut
		// Numberbox n5 = controlP5.addNumberbox("setCut", app.cut, (panelWidth - 8)/3, yPos, 40, widgetH);
		Numberbox n5 = control.addNumberbox("setCut").setPosition((panelWidth - 8)/3, yPos).setSize(40, widgetH);
		n5.plugTo(app, "setCut").setValue(app.cut);
		n5.setGroup(fftSettings);
		n5.setMultiplier(0.01f);
		n5.setDecimalPrecision(2);
		n5.setMin(0.0f);
		n5.setMax(8.0f);
		n5.getCaptionLabel().set("");
		// label for cut number box
		Textlabel l18 = control.addTextlabel("cutLabel", "OUT Scale", (panelWidth - 8)/3 + 40, yPos + 4);
		l18.setGroup(fftSettings);
		// link checkbox, link IN scale setting to OUT scale setting
		CheckBox ch6 = createCheckBox("Link", 2 * (panelWidth - 8)/3 + 8, yPos + 4, 3, (panelWidth - 8)/4, fftSettings, foreColor, activeColor, labelColor);		
		// add items to the checkbox
		ch6.addItem("setLink", 0);
		ch6.getItem(0).setCaptionLabel("Link");
		ch6.setColorForeground(app.color(233, 233, 0));
		// HSB/RGB checkboxes for statistically-controlled FFT
		yPos += step;
		CheckBox ch5 = createCheckBox("ChanStat", 8, yPos + 2, 3, (panelWidth - 8)/4, fftSettings, foreColor, activeColor, labelColor);		
		// add items to the checkbox
		ch5.addItem("statBrightness", 1);
		ch5.getItem(0).setCaptionLabel("Brightness");
		ch5.setColorForeground(app.color(233, 233, 0));
		ch5.addItem("statHue", 2);
		ch5.getItem(1).setCaptionLabel("Hue");
		ch5.setColorForeground(app.color(233, 233, 0));
		ch5.addItem("statSaturation", 3);
		ch5.getItem(2).setCaptionLabel("Saturation");
		ch5.setColorForeground(app.color(233, 233, 0));
		// add items to the checkbox
		ch5.addItem("statRed", 4);
		ch5.getItem(3).setCaptionLabel("Red");
		ch5.setColorForeground(app.color(233, 233, 0));
		ch5.addItem("statGreen", 5);
		ch5.setColorForeground(app.color(233, 233, 0));
		ch5.getItem(4).setCaptionLabel("Green");
		ch5.addItem("statBlue", 6);
		ch5.getItem(5).setCaptionLabel("Blue");
		ch5.setColorForeground(app.color(233, 233, 0));
		/* 
		// section label
		yPos += 2 * step;
		Textlabel l19 = controlP5.addTextlabel("blockSizeSectionLabel", "FFT Block Size", 8, yPos);
		l19.setGroup(fftSettings);
		Textlabel l19u = controlP5.addTextlabel("blockSizeSectionLabelUnder", "________________________________", 8, yPos + 3);
		l19u.setGroup(fftSettings);	
		*/	
		// slider for FFT block size 
		// watch out for a concurrent modification error when we regenerate the FFT panel
		yPos +=  step + step/2;
		Slider s4 = control.addSlider("setFFTBlockWidth").setPosition(8, yPos).setSize(72, widgetH); 
		s4.setMin(3).setMax(10).setValue(3);
		s4.plugTo(app, "setFFTBlockWidth");
		s4.setGroup(fftSettings);
		s4.setDecimalPrecision(0);
		s4.getCaptionLabel().set("");
		Textlabel l11 = control.addTextlabel("blockSizeLabel", "Statistical FFT Block Size = "+ app.statFFTBlockWidth, 84, yPos + 2);
		l11.setGroup(fftSettings);
		
		CheckBox ch7 = createCheckBox("hilbert", 84 + 72 + 16, yPos + 2, 1, (panelWidth - 8)/4, fftSettings, foreColor, activeColor, labelColor);		
		ch7.addItem("isHilbert", 1);
		ch7.getItem(0).setCaptionLabel("Hilbert");
		/**/
		// RGB check box, sets app.isRunFormantRGB
		// hzOffset += labelW + 8;
		CheckBox ch9 = createCheckBox("FormantRGB", 84 + 72 + 72, yPos + 2, 1, 0, fftSettings, foreColor, activeColor, labelColor);
		ch9.setColorForeground(app.color(233, 233, 0));
		ch9.addItem("setIsRunFormantRGB", 1);
		ch9.getItem(0).setValue(app.isRunFormantRGB);
		ch9.getItem(0).setCaptionLabel("RGB");
		
		
		// formant section
		// TODO formant popup menu
		yPos +=  step + step + offset;
		int numboxW = 48;
		int labelW = 24;
		int hzOffset = 8;
		int gap = 2;
		Numberbox n8 = control.addNumberbox("setFfreq1").setPosition(hzOffset, yPos).setSize(numboxW, widgetH);
		n8.plugTo(app, "setFfreq1").setValue(app.ffreq1);
		n8.setGroup(fftSettings);
		n8.setMultiplier(0.1f);
		n8.setDecimalPrecision(1);
		n8.setMin(0.0f);
		n8.setMax(this.app.sampleRate/2);
		n8.getCaptionLabel().set("");
		// text label for number box
		hzOffset += numboxW;
		Textlabel l20 = control.addTextlabel("f1Label", "F1", hzOffset, yPos + 4);
		l20.setGroup(fftSettings);
		// next number box
		hzOffset += labelW + gap;
		Numberbox n9 = control.addNumberbox("setFfreq2").setPosition(hzOffset, yPos).setSize(numboxW, widgetH);
		n9.plugTo(app, "setFfreq2").setValue(app.ffreq2);
		n9.setGroup(fftSettings);
		n9.setMultiplier(0.1f);
		n9.setDecimalPrecision(1);
		n9.setMin(0.0f);
		n9.setMax(this.app.sampleRate/2);
		n9.getCaptionLabel().set("");
		hzOffset += numboxW;
		Textlabel l21 = control.addTextlabel("f2Label", "F2", hzOffset, yPos + 4);
		l21.setGroup(fftSettings);
		// next number box
		hzOffset += labelW + gap;
		Numberbox n10 = control.addNumberbox("setFfreq3").setPosition(hzOffset, yPos).setSize(numboxW, widgetH);
		n10.plugTo(app, "setFfreq3").setValue(app.ffreq3);
		n10.setGroup(fftSettings);
		n10.setMultiplier(0.1f);
		n10.setDecimalPrecision(1);
		n10.setMin(0.0f);
		n10.setMax(this.app.sampleRate/2);
		n10.getCaptionLabel().set("");
		hzOffset += numboxW;
		Textlabel l22 = control.addTextlabel("f3Label", "F3", hzOffset, yPos + 4);
		l22.setGroup(fftSettings);
		/*  */
		// next number box, DC bias
		hzOffset += labelW + gap;
		Numberbox n14 = control.addNumberbox("setFDCBias").setPosition(hzOffset, yPos).setSize(32, widgetH);
		n14.plugTo(app, "setFDCBias").setValue(app.fDCBias);
		n14.setGroup(fftSettings);
		n14.setMultiplier(1.0f);
		n14.setDecimalPrecision(0);
		n14.setMin(-50.0f);
		n14.setMax(50.0f);
		n14.getCaptionLabel().set("");
		hzOffset += 32;
		Textlabel l26 = control.addTextlabel("fDCBiasLabel", "DC", hzOffset, yPos + 4);
		l26.setGroup(fftSettings);		
		//
		// amplitudes, new row //
		yPos +=  step;
		hzOffset = 8;
		Numberbox n11 = control.addNumberbox("setFamp1").setPosition(hzOffset, yPos).setSize(numboxW, widgetH);
		n11.plugTo(app, "setFamp1").setValue(app.famp1);
		n11.setGroup(fftSettings);
		n11.setMultiplier(0.01f);
		n11.setDecimalPrecision(2);
		n11.setMin(0.0f);
		n11.setMax(4.0f);
		n11.getCaptionLabel().set("");
		hzOffset += numboxW;
		Textlabel l23 = control.addTextlabel("f1AmpLabel", "A1", hzOffset, yPos + 4);
		l23.setGroup(fftSettings);
		// next number box
		hzOffset += labelW + gap;
		Numberbox n12 = control.addNumberbox("setFamp2").setPosition(hzOffset, yPos).setSize(numboxW, widgetH);
		n12.plugTo(app, "setFamp2").setValue(app.famp2);
		n12.setGroup(fftSettings);
		n12.setMultiplier(0.01f);
		n12.setDecimalPrecision(2);
		n12.setMin(0.0f);
		n12.setMax(4.0f);
		n12.getCaptionLabel().set("");
		hzOffset += numboxW;
		Textlabel l24 = control.addTextlabel("f2AmpLabel", "A2", hzOffset, yPos + 4);
		l24.setGroup(fftSettings);
		// next number box
		hzOffset += labelW + gap;
		Numberbox n13 = control.addNumberbox("setFamp3").setPosition(hzOffset, yPos).setSize(numboxW, widgetH);
		n13.plugTo(app, "setFamp3").setValue(app.famp3);
		n13.setGroup(fftSettings);
		n13.setMultiplier(0.01f);
		n13.setDecimalPrecision(2);
		n13.setMin(0.0f);
		n13.setMax(4.0f);
		n13.getCaptionLabel().set("");
		hzOffset += numboxW;
		Textlabel l25 = control.addTextlabel("f3AmpLabel", "A3", hzOffset, yPos + 4);
		l25.setGroup(fftSettings);
		/* */
		// next number box, amplitude scale
		hzOffset += labelW + gap;
		Numberbox n15 = control.addNumberbox("setFormantScale").setPosition(hzOffset, yPos).setSize(32, widgetH);
		n15.plugTo(app, "setFormantScale").setValue(app.formantScale);
		n15.setGroup(fftSettings);
		n15.setMultiplier(0.01f);
		n15.setDecimalPrecision(2);
		n15.setMin(0.01f);
		n15.setMax(4.0f);
		n15.getCaptionLabel().set("");
		hzOffset += 32;
		Textlabel l27 = control.addTextlabel("formantScaleLabel", "S", hzOffset, yPos + 4);
		l27.setGroup(fftSettings);
		// mute checkboxes 
		/**/
		yPos += step;
		hzOffset = 8;
		CheckBox ch10 = createCheckBox("FormantMute", hzOffset, yPos + 2, 3, 64, fftSettings, foreColor, activeColor, labelColor);
		ch10.setColorForeground(app.color(233, 233, 0));
		ch10.addItem("setMuteF1", 1);
		ch10.getItem(0).setValue(app.isMuteF1);
		ch10.getItem(0).setCaptionLabel("mute");
		ch10.addItem("setMuteF2", 1);
		ch10.getItem(1).setValue(app.isMuteF2);
		ch10.getItem(1).setCaptionLabel("mute");
		ch10.addItem("setMuteF3", 1);
		ch10.getItem(2).setValue(app.isMuteF3); 
		ch10.getItem(2).setCaptionLabel("mute"); 
		int btnStep = 75;
		// TODO adjust spacing
		createButton("stepFfreq1", hzOffset + 40, yPos, 20, widgetH, fftSettings, "+");
		createButton("stepFfreq2", hzOffset + 40 + btnStep, yPos, 20, widgetH, fftSettings, "+");
		createButton("stepFfreq3", hzOffset + 40 + btnStep + btnStep, yPos, 20, widgetH, fftSettings, "+");
		createButton("stepAll", hzOffset + 40 + btnStep + btnStep + 40, yPos, 28, widgetH, fftSettings, "+");
		// another row
		yPos += step;
		hzOffset = 8;
		createButton("loadFormantOctave", hzOffset, yPos, 48, widgetH, fftSettings, "Scale");
		hzOffset += numboxW + labelW + gap;
		createButton("permuteFormantValues", hzOffset, yPos, 48, widgetH, fftSettings, "Perm");
		hzOffset += numboxW + labelW + gap;
		createButton("halfFormantValues", hzOffset, yPos, 48, widgetH, fftSettings, "half-dbl");
		/**/
		
		// move fftSettings into a tab
		fftSettings.moveTo("FFT");
		fftSettingsTab = control.getTab("FFT");
		fftSettingsTab.activateEvent(true);
		fftSettingsTab.disableCollapse();
		fftSettingsTab.setLabel("  FFT  ");
		fftSettingsTab.setId(2);
	}
	
    /**
	 * Sets up and draws the multi-band equalizer control.
	 * @param yPos   y-offset of control position
	 * @param h      height of a slider
	 * @param max    maximum value represented by slider
	 * @param min    minimum value represented by slider
	 */
	public void setupEqualizer(int yPos, int h, float max, float min) {
		int eqW = 8;
		int left = 8;
		// we use a fixed maximum number of bands and show or hide bands as FFT buffer size varies
		int lim = app.eqBands;
		for (int i = 0; i < lim; i++) {
			String token = sliderIdentifier + app.noPlaces.format(i);
			// Slider slider = controlP5.addSlider(token).setPosition(left, yPos).setSize(eqW, h).setId(i);
			Slider slider = control.addSlider(token).plugTo(app, token).setPosition(left, yPos).setSize(eqW, h).setId(i);
			slider.setMax(max);
			slider.setMin(min);
			slider.setValue(0);
			slider.setMoveable(false).setLabelVisible(false);
			int fc = app.color(199, 47, 21, 255);
			slider.setColorForeground(fc);
			int bc = app.color(233, 233, 254, 255);
			slider.setColorBackground(bc);
			slider.setGroup(fftSettings);
			left += eqW;
		}
		if (0 == app.eqPos) app.eqPos = yPos;
	}
	
	/**
	 * removes the equalizer from the control panel, currently not used
	 */
	public void removeEqualizer() {
		int lim = app.eq.length;
		for (int i = 0; i < lim; i++) {
			String token = sliderIdentifier + app.noPlaces.format(i);
			Slider slider = (Slider) control.getController(token);
			slider.remove();
		}
	}

	/**
	 * shows equalizer bands used for current FFT block size, hides others
	 */
	public void showEqualizerBands() {
		// precautionary coding. The number of eq bins (eq.length) should not exceed the max number of bands.
		int lim = app.eq.length > app.eqBands ? app.eqBands : app.eq.length;
		float[] bins = getEqualizerValues();
		for (int i = 0; i < lim; i++) {
			String token = sliderIdentifier + app.noPlaces.format(i);
			Slider slider = (Slider) control.getController(token);
			slider.setVisible(true);
			// set value to 0
			slider.setValue(bins[i]);
		}
		// hide bands we don't use
		for (int i = lim; i < app.eqBands; i++) {
			String token = sliderIdentifier + app.noPlaces.format(i);
			Slider slider = (Slider) control.getController(token);
			slider.setVisible(false);
			// don't change the value, bins are out of range for ControlP5 propagated event
		}
	}
	
	public float[] getEqualizerValues() {
		int lim = app.eqBands;
		float[] bins = new float[lim];
		for (int i = 0; i < lim; i++) {
			String token = sliderIdentifier + app.noPlaces.format(i);
			// Slider slider = controlP5.addSlider(token).setPosition(left, yPos).setSize(eqW, h).setId(i);
			Slider slider = (Slider) control.getController(token);
			bins[i] = slider.getValue();
		}
		return bins;
	}
	
	/**
	 * Resets equalizer FFT controls, duplicate of call in GlitchSort
	 */
	public void resetEqualizerValues(float[] newValues) {
		for (int i = 0; i < app.eq.length; i++) {
			String token = sliderIdentifier + app.noPlaces.format(i);
			Slider slider = (Slider) getControl().getController(token);
			slider.setValue(newValues[i]);
		}
		app.analyzeEq(false);
	}

	/**
	 * Resets equalizer FFT controls, duplicate of call in GlitchSort
	 */
	public void setEq() {
		for (int i = 0; i < app.eq.length; i++) {
			String token = sliderIdentifier + app.noPlaces.format(i);
			Slider slider = (Slider) getControl().getController(token);
			slider.setValue(0);
		}
		app.analyzeEq(false);
	}


	
	public float getEqBinValue(int bin) {
		// precautionary coding. The number of eq bins (eq.length) should not exceed the max number of bands.
		int lim = app.eq.length > app.eqBands ? app.eqBands : app.eq.length;
		if (bin >= lim) return 0;
		String token = sliderIdentifier + app.noPlaces.format(bin);
		Slider slider = (Slider) control.getController(token);
		float val = slider.getValue();
		return val;
	}


	/*****************************************/
    /*                                       */
    /*          >>> SYNTH PANEL <<<          */
    /*                                       */
    /*****************************************/

	
		public void loadSynthPanel() {
				int foreColor = app.color(120);
				int activeColor = app.color(255);
				int labelColor = app.color(255);
				//
				yPos = 6;
				step = 18;
				int offset = -2;
				synthSettings = control.addGroup("Synth", panelX, panelY, panelWidth);
				synthSettings.setBackgroundColor(panelBackColor);
				synthSettings.setBackgroundHeight(panelHeight);
				synthSettings.setBarHeight(widgetH + 4);
				synthSettings.setMoveable(false);     // dragging throws absolute position off...
				// widgets
				// audio operators: Frequency, Amplitude, Phase. 
				// image operators: RGBA gradient
				// storage class: ColorOscillator
				// move fftSettings into a tab
				synthSettings.moveTo("Synth");
				synthSettingsTab = control.getTab("Synth");
				synthSettingsTab.activateEvent(true);
				synthSettingsTab.disableCollapse();
				synthSettingsTab.setLabel("  Synth  ");
				synthSettingsTab.setId(2);
		}
		
		
	
	
	
    /*****************************************/
    /*                                       */
    /*          >>> MUNGE PANEL <<<          */
    /*                                       */
    /*****************************************/

	
	/**
	 * Initializes and arranges the "munge" control panel widgets
	 */
	public void loadMungePanel() {
		int spacer = 4;
		int foreColor = app.color(120);
		int activeColor = app.color(255);
		int labelColor = app.color(255);
		yPos = 6;
		step = 18;
		mungeSettings = control.addGroup("Munge", panelX, panelY, panelWidth);
		mungeSettings.setBackgroundColor(panelBackColor);
		mungeSettings.setBackgroundHeight(panelHeight);
		mungeSettings.setBarHeight(widgetH + 4);
		mungeSettings.setMoveable(false);     // option-drag on bar to move menu not permitted
		// add widgets
		// degrading, compositing section
		Textlabel l20 = control.addTextlabel("degradeLabel", "Degrade + Quantize + Munge", 8, yPos);
		l20.setGroup(mungeSettings);
		Textlabel l20u = control.addTextlabel("degradeLabelUnder", "________________________________", 8, yPos + 3);
		l20u.setGroup(mungeSettings);		
		// degrade controls
		yPos += step;
		//Slider s2 = controlP5.addSlider("setQuality", 100, 0, 13.0f, 8, yPos, 128, widgetH);
		Slider s2 = control.addSlider("setQuality").setPosition(8, yPos).setSize(128, widgetH);
		s2.setMin(100).setMax(0).setValue(13.0f);
		s2.plugTo(app, "setQuality");
		s2.setGroup(mungeSettings);
		s2.setDecimalPrecision(1);
		s2.getCaptionLabel().set("");
		s2.setSliderMode(Slider.FLEXIBLE);
		// label for degrade quality slider
		Textlabel l4 = control.addTextlabel("QualityLabel", "Quality", 137, yPos + 4);
		l4.setGroup(mungeSettings);
		// degrade button
		createButton("degrade", 2 * panelWidth/3 + 28, yPos, 60, widgetH, mungeSettings, "Degrade (d)");
		// reduce colors slider
		yPos += step;
		//Slider s3 = controlP5.addSlider("setColorQuantize", 2, 128, app.colorQuantize, 8, yPos, 127, widgetH);
		Slider s3 = control.addSlider("setColorQuantize").setPosition(8, yPos).setSize(127, widgetH);
		s3.setMin(2).setMax(128).setValue(app.colorQuantize);
		s3.plugTo(app, "setColorQuantize");
		s3.setGroup(mungeSettings);
		s3.setDecimalPrecision(0);
		s3.getCaptionLabel().set("");
		s3.setSliderMode(Slider.FLEXIBLE);
		// label for color quantize slider
		Textlabel l9 = control.addTextlabel("colorQuantizeLabel", "Colors = "+ app.colorQuantize, 137, yPos + 4);
		l9.setGroup(mungeSettings);
		// reduce colors button
		createButton("reduceColors", 2 * panelWidth/3 + 28, yPos, 60, widgetH, mungeSettings, "Reduce (p)");
		// denoise
		yPos += step + spacer;
		createButton("mean", 1 * panelWidth/3 + 28, yPos, 60, widgetH, mungeSettings, "Mean (CL-9)");
		createButton("denoise", 2 * panelWidth/3 + 28, yPos, 60, widgetH, mungeSettings, "Denoise (9)");
		// shift color channels
		yPos += step;
		createButton("shiftLeft", 8, yPos, 32, widgetH, mungeSettings, " << ");
		RadioButton r5 = createRadioButton("Shift", 48, yPos + 2, 3, (panelWidth - 8)/12, mungeSettings, foreColor, activeColor, labelColor);
		// add items to the checkbox
		r5.addItem("R", 1);
		r5.addItem("G", 2);
		r5.addItem("B", 3);
		r5.setColorForeground(app.color(233, 233, 0));
		r5.activate(0);		
		createButton("shiftRight", 48 + 4 * ((panelWidth - 8)/12), yPos, 32, widgetH, mungeSettings, " >> ");
		//Numberbox n6 = controlP5.addNumberbox("setShift", app.shift, 2 * panelWidth/3 + 28, yPos, 60, widgetH);
		Numberbox n6 = control.addNumberbox("setShift").setPosition(2 * panelWidth/3 + 28, yPos).setSize(60, widgetH);
		n6.plugTo(app, "setShift").setValue(app.shift);
		n6.setGroup(mungeSettings);
		n6.setMultiplier(1f);
		n6.setDecimalPrecision(1);
		n6.setMin(1.0f);
		n6.setMax(1024.0f);
		n6.getCaptionLabel().set("");
		Textlabel l10 = control.addTextlabel("shiftLabel", "Shift", 2 * panelWidth/3 + 28 - 36, yPos + 4);
		l10.setGroup(mungeSettings);
		// shift pixels (rotateLeft)
		yPos += step + spacer;
		createButton("doShift", 8, yPos, 76, widgetH, mungeSettings, "Rotate Left (')");
		// snap, unsnap
		yPos += step + spacer;
		createButton("snap", 8, yPos, 76, widgetH, mungeSettings, "Snap (n)");
		createButton("unsnap", panelWidth/3 + 4, yPos, 76, widgetH, mungeSettings, "Unsnap (u)");
		// invert munge checkbox
		CheckBox ch6 = createCheckBox("invertMunge", 2 * panelWidth/3 + 4, yPos + 2, 3, (panelWidth - 8)/4, mungeSettings, foreColor, activeColor, labelColor);
		// add items to the checkbox
		ch6.addItem("Invert Munge (i)", 0);
		ch6.setColorForeground(app.color(233, 233, 0));		
		// mungeThreshold setting
		yPos += step;
		//Slider s5 = controlP5.addSlider("setMungeThreshold", 100, 1, app.mungeThreshold, 8, yPos, 101, widgetH);
		Slider s5 = control.addSlider("setMungeThreshold").setPosition(8, yPos).setSize(101, widgetH);
		s5.setMin(100).setMax(1).setValue(app.mungeThreshold);
		s5.plugTo(app, "setMungeThreshold");
		s5.setGroup(mungeSettings);
		s5.setDecimalPrecision(0);
		s5.getCaptionLabel().set("");
		s5.setSliderMode(Slider.FLEXIBLE);
		// label for degrade quality slider
		Textlabel l19 = control.addTextlabel("mungeThresholdLabel", "Munge Threshold", 112, yPos + 4);
		l19.setGroup(mungeSettings);
		// munge button
		createButton("munge", 2 * panelWidth/3 + 28, yPos, 60, widgetH, mungeSettings, "Munge (m)");
		// nexxxxxt....
		yPos += step;
		
		// create munge settings tab
		Tab global = control.getTab("default");
		global.setLabel("");
		global.hide();
		mungeSettings.moveTo("munge");
		mungeSettingsTab = control.getTab("munge");
		mungeSettingsTab.activateEvent(true);
		mungeSettingsTab.disableCollapse();
		mungeSettingsTab.setLabel("  Munge  ");
		mungeSettingsTab.setId(3);
	}

	
  /*****************************************/
  /*                                       */
  /*         >>> AUDIFY PANEL <<<          */
  /*                                       */
  /*****************************************/


/**
 * Initializes and arranges the "audify" control panel widgets
 * TODO audify code
 */
public void loadAudifyPanel() {
	int spacer = 4;
	int foreColor = app.color(120);
	int activeColor = app.color(255);
	int labelColor = app.color(255);
	yPos = 6;
	step = 18;
	audifySettings = control.addGroup("Audify", panelX, panelY, panelWidth);
	audifySettings.setBackgroundColor(panelBackColor);
	audifySettings.setBackgroundHeight(panelHeight);
	audifySettings.setBarHeight(widgetH + 4);
	audifySettings.setMoveable(false);     // option-drag on bar to move menu not permitted
	// add widgets
	// legend
	Textlabel la1 = control.addTextlabel("audifyLabel", "Audify Settings", 8, yPos);
	la1.setGroup(audifySettings);		
	Textlabel la1u = control.addTextlabel("audifyLabelUnder", "________________________________", 8, yPos + 3);
	la1u.setGroup(audifySettings);		
	yPos += step;
	createButton("audify", 8, yPos, 64, widgetH, audifySettings, "Audify (/)");
	createButton("audifyOff", panelWidth/3, yPos, 64, widgetH, audifySettings, "Audify Off (\\)");
	
	// nexxxxxt....
	yPos += step;
	RadioButton r6 = createRadioButton("Tracking", 8, yPos + 2, 3, (panelWidth - 8)/4, audifySettings, foreColor, activeColor, labelColor);
	r6.setColorForeground(app.color(233, 233, 0));
	// add items to the radio button, note that we can't use names that start with sliderIdentifier "_eq" 
	r6.addItem("setTrackMouse", 1);
	r6.getItem(0).setCaptionLabel("Mouse");
	r6.addItem("setFollowArrows", 2);
	r6.getItem(1).setCaptionLabel("Arrows");
	r6.addItem("setAutoPilot", 3);
	r6.getItem(2).setCaptionLabel("Autopilot");	
	if (app.isAutoPilot) r6.activate("setAutoPilot");
	yPos += step;
	CheckBox ch7 = createCheckBox("Muting", 8, yPos + 2, 3, (panelWidth - 8)/4, audifySettings, foreColor, activeColor, labelColor);
	ch7.setColorForeground(app.color(233, 233, 0));
	// add items to the checkbox
	ch7.addItem("setMuted", 1);
	ch7.getItem(0).setCaptionLabel("Mute");
	// ch7.getItem(0).plugTo(app, "setMuted"); // apparently doesn't succeed at runtime
	ch7.addItem("setFrozen", 2);
	ch7.getItem(1).setCaptionLabel("Freeze");
	ch7.addItem("setHidden", 3);
	ch7.getItem(2).setCaptionLabel("Hide");
	yPos += step;
	CheckBox ch8 = createCheckBox("Pipe", 8, yPos + 2, 3, (panelWidth - 8)/4, audifySettings, foreColor, activeColor, labelColor);
	ch8.setColorForeground(app.color(233, 233, 0));
	ch8.addItem("setPipeToOSC", 4);
	ch8.getItem(0).setCaptionLabel("Pipe to OSC");
	yPos += step;
	createButton("whoCalled", 8, yPos, 64, widgetH, audifySettings, "Doobie");
	createButton("moose", panelWidth/3, yPos, 64, widgetH, audifySettings, "Moose");
	
	// create audify settings tab
	Tab global = control.getTab("default");
	global.setLabel("");
	global.hide();
	audifySettings.moveTo("audify");
	audifySettingsTab = control.getTab("audify");
	audifySettingsTab.activateEvent(true);
	audifySettingsTab.disableCollapse();
	audifySettingsTab.setLabel("  Audify  ");
	audifySettingsTab.setId(3);
}

	
	// UTILITY //
	
	/**
	 * @return true if control panel is hidden or inactive, false otherwise. 
	 */
	public boolean panelIsInactive() {
		return (!control.isVisible() || !( glitchSettingsTab.isActive() || fftSettingsTab.isActive() || mungeSettingsTab.isActive() ));
	}
	
	public Rectangle controlPanelRect() {
		if (null == controlPanelRect) {
			controlPanelRect = new Rectangle(controlPanelX, controlPanelY, controlPanelWidth, controlPanelHeight);
		}
		return controlPanelRect;
	}
	
	float tolerance = 0.0001f;
	boolean nearlyEqual(float v1, float v2) {
		return (abs(v1 - v2) > tolerance);
	}
	
	/************************************************
	 *                 COMMANDS                     *
	 ************************************************/
	
	float trackY = 0;
	/**
	 * tracks mouse movement over the equalizer in the FFT control panel
	 */
	public void trackMouseEq() {
		if (null == getControl()) return;
		if (getControl().isVisible() && null != fftSettings) {
			if (fftSettings.isVisible()) {
				List<ControllerInterface<?>> mouseControls = getControl().getMouseOverList();
				for (ControllerInterface<?> con : mouseControls) {
					if (con.getName().length() > 3 && con.getName().substring(0, 3).equals(sliderIdentifier)) {
						if (mousePressed) {
							// change the value of the eq bin based on mouse position
							float[] pos = con.getAbsolutePosition();
							float yPos = pos[1];
							/*
							PVector vec = new PVector(pos[0], pos[1]);
							float v = map(mouseY, vec.y, vec.y + app.eqH, app.eqMax, app.eqMin);
							if (v != con.getValue()) {
									println(con.getName() +": "+ vec.y +"; mouseY: "+ mouseY +"; v = "+ v +"; control = "+ con.getValue());
								con.setValue(v);
							}
							*/
							// if (yPos != trackY) {
								float v = map(mouseY, yPos, yPos + app.eqH, app.eqMax, app.eqMin);
								con.setValue(v);
								trackY = yPos;
							// }
						}
						else {
							if (con.getId() >= 0) {
								int bin = con.getId();
								// write out the current amplitude setting from the eq tool
								if (bin < app.eq.length) {
									String legend = "band "+ bin +" = "+ app.twoPlaces.format(app.eq[bin]);
									if (null != app.binTotals && bin < app.binTotals.length) {
										legend += ", bin avg = "+ app.twoPlaces.format(app.binTotals[bin]);
										// legend += ", bins "+ bandList.get(eq.length - bin - 1).toString();
										// get indices of the range of bands covered by each slider and calculate their center frequency
										IntRange ir = app.bandList.get(bin);
										legend += ", cf = "+ app.twoPlaces.format((app.statFFT.indexToFreq(ir.upper) + app.statFFT.indexToFreq(ir.lower)) * 0.5f);
										// get the scaling value set by the user
									}
									((Textlabel)getControl().getController("eqLabel")).setValue(legend);
								}
							}
						}
					}
				}
			}
			else {
				// println("conditions not met");
			}
		}
	}


	/**
	 * Bottleneck that catches events propagated by control panel, used particularly for radio buttons and checkboxes.
	 * @param evt   the event from the control panel
	 */
	public void controlEvent(ControlEvent evt) {
		if (evt.isGroup()) {
			if ("setCompOrder".equals(evt.getName())) {
				app.setCompOrder((int) evt.getGroup().getValue(), true);
			}
			else if ("setSorter".equals((evt.getName()))) {
				SorterType type = SorterType.values()[(int) evt.getGroup().getValue()];
				app.setSorter(type, true);
			}
			else if ("Sorting".equals(evt.getName())) {
				int n = (int)(evt.getGroup().getArrayValue()[0]);
				app.setAscending(n == 1, true);
				n = (int)(evt.getGroup().getArrayValue()[1]);
				app.setRandomBreak(n == 1, true);
				n = (int)(evt.getGroup().getArrayValue()[2]);
				app.setIsSwapChannels(n == 1, true);
			}
			else if ("setSourceChannel".equals(evt.getName())) {
				println("---- setSourcechannel");
				int n = (int)(evt.getGroup().getValue());
				RadioButton rb = (RadioButton)getControl().getGroup("setTargetChannel");
				int m = (int) rb.getValue();
				String str = ChannelNames.values()[n].toString() + ChannelNames.values()[m].toString();
				SwapChannel sc = SwapChannel.valueOf(str);
				app.setSwap(sc, true);
			}
			else if ("setTargetChannel".equals(evt.getName())) {
				println("---- setTargetChannel");
				RadioButton rb = (RadioButton)getControl().getGroup("setSourceChannel");
				int n = (int) rb.getValue();
				int m = (int)(evt.getGroup().getValue());
				String str = ChannelNames.values()[n].toString() + ChannelNames.values()[m].toString();
				SwapChannel sc = SwapChannel.valueOf(str);
				app.setSwap(sc, true);
			}
			else if ("fitPixels".equals(evt.getName())) {
				int n = (int)(evt.getGroup().getArrayValue()[0]);
				app.fitPixels(n == 1, true);
			}
			else if (("setZigzagStyle").equals(evt.getName())) {
				ZigzagStyle z = ZigzagStyle.values()[(int) evt.getGroup().getValue()];
				app.setZigzagStyle(z, true);				
			}
			else if ("invertMunge".equals(evt.getName())) {
				int n = (int)(evt.getGroup().getArrayValue()[0]);
				app.invertMunge(n == 1, true);
			}
			else if ("Glitchmode".equals(evt.getName())) {
				int n = (int)(evt.getGroup().getArrayValue()[0]);
				app.setCycle(n == 1, true);
			}
			else if ("ChanEq".equals(evt.getName())) {
				if (app.verbose) println("ChanEq event");
				int b = (int)(evt.getGroup().getArrayValue()[0]);
				int h = (int)(evt.getGroup().getArrayValue()[1]);
				int s = (int)(evt.getGroup().getArrayValue()[2]);
				int r = (int)(evt.getGroup().getArrayValue()[3]);
				int g = (int)(evt.getGroup().getArrayValue()[4]);
				int bl = (int)(evt.getGroup().getArrayValue()[5]);
				app.setEqChan(b == 1, h == 1, s == 1, r == 1, g == 1, bl == 1, true);
			}
			else if ("ChanStat".equals(evt.getName())) {
				if (app.verbose) println("ChanStat event");
				int b = (int)(evt.getGroup().getArrayValue()[0]);
				int h = (int)(evt.getGroup().getArrayValue()[1]);
				int s = (int)(evt.getGroup().getArrayValue()[2]);
				int r = (int)(evt.getGroup().getArrayValue()[3]);
				int g = (int)(evt.getGroup().getArrayValue()[4]);
				int bl = (int)(evt.getGroup().getArrayValue()[5]);
				app.setStatChan(b == 1, h == 1, s == 1, r == 1, g == 1, bl == 1, true);
			}
			else if ("hilbert".equals(evt.getName())) {
				app.isHilbertScan = ((int)(evt.getGroup().getArrayValue()[0])) == 1;
				println("-------- isHilbertScan = "+ app.isHilbertScan);
			}
			else if ("FormantRGB".equals(evt.getName())) {
				app.isRunFormantRGB = ((int)(evt.getGroup().getArrayValue()[0])) == 1;
				println("-------- isRunFormantRGB = "+ app.isRunFormantRGB);
			}
			else if ("Shift".equals(evt.getName())) { 
				app.isShiftR = ((int)(evt.getGroup().getArrayValue()[0])) == 1;
				app.isShiftG = ((int)(evt.getGroup().getArrayValue()[1])) == 1;
				app.isShiftB = ((int)(evt.getGroup().getArrayValue()[2])) == 1;
			}
			else if ("FormantMute".equals(evt.getName())) {
				app.isMuteF1 = ((int)(evt.getGroup().getArrayValue()[0])) == 1;
				app.isMuteF2 = ((int)(evt.getGroup().getArrayValue()[1])) == 1;
				app.isMuteF3 = ((int)(evt.getGroup().getArrayValue()[2])) == 1;
				println("-------- formant muting set");
			}
			else if ("Link".equals(evt.getName())) {
				if (app.verbose) println("Link event");
				int cut = (int)(evt.getGroup().getArrayValue()[0]);
				app.setLink(cut == 1, true);
			}
			else if ("Tracking".equals(evt.getName())) {
				if (app.verbose) println("Tracking event");
				app.setTrackMouse(((int)(evt.getGroup().getArrayValue()[0])) == 1, true);
				app.setFollowArrows(((int)(evt.getGroup().getArrayValue()[1])) == 1, true);
				app.setAutoPilot(((int)(evt.getGroup().getArrayValue()[2])) == 1, true);
			}
			else if ("Muting".equals(evt.getName())) {
				if (app.verbose) println("Muting event");
				app.setMuted(((int)(evt.getGroup().getArrayValue()[0])) == 1, true);
				app.setFrozen(((int)(evt.getGroup().getArrayValue()[1])) == 1, true);
				app.setHidden(((int)(evt.getGroup().getArrayValue()[2])) == 1, true);
			}
			else if ("Pipe".equals(evt.getName())) {
				if (app.verbose) println("Muting event");
				app.setPipeToOSC(((int)(evt.getGroup().getArrayValue()[0])) == 1, true);
			}
			if (app.verbose) {
				print("got an event from "+ evt.getGroup().getName() +"\t");
				for(int i=0; i < evt.getGroup().getArrayValue().length; i++) {
					print((int)(evt.getGroup().getArrayValue()[i]));
				}
				println("\t "+ evt.getGroup().getValue());
			}
		}
		else if (evt.isController()) {
			String name = evt.getController().getName();
			if (name.substring(0, 3).equals(sliderIdentifier)) {
				Slider con = (Slider) evt.getController();
				int bin = con.getId();
				float val = con.getValue();
				if (bin >= 0 && bin < app.eq.length) {
					if (val < 0) app.eq[bin] = val + 1;
					else app.eq[bin] = lerp(0, app.eqScale, val) + 1;
					String legend = "band "+ bin +" = "+ app.twoPlaces.format(app.eq[bin]);
					if (null != app.binTotals && bin < app.binTotals.length) {
						// TODO : duplicated code here, put it in a function
						legend += ", bin avg = "+ app.twoPlaces.format(app.binTotals[bin]);
						IntRange ir = app.bandList.get(bin);
						legend += ", cf = "+ app.twoPlaces.format((app.statFFT.indexToFreq(ir.upper) 
								      + app.statFFT.indexToFreq(ir.lower)) * 0.5f);
					}
					((Textlabel)getControl().getController("eqLabel")).setValue(legend);
				}
			}
			
		}
	}
	
	class FormantState {
		float freq;
		float amp;
		boolean isOn;
		
		public FormantState() {
			this(220.0f, 1.0f, true);
		}
		
		public FormantState(float f, float a, boolean on) {
			this.freq = f;
			this.amp = a;
			this.isOn = on;
		}
	}


}
