package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;
import net.sourceforge.jocular.math.SellmeierEquation;
import net.sourceforge.jocular.math.TransmittanceLookupTable;


public class nSk14 extends SimpleOpticalMaterial {
	
	
	private static final double[] tWavelength = {290, 300, 310, 320, 334, 350, 365, 370, 380, 390, 400, 405, 420, 436, 460, 500, 546, 580, 620, 660, 700, 1060, 1530, 1970, 2325, 2500};
	private static final double[] transmissivity = {0.04, 0.16, 0.345, 0.546, 0.77, 0.91, 0.963, 0.971, 0.981, 0.988, 0.99, 0.991, 0.993, 0.994, 0.995, 0.997, 0.998, 0.998, 0.998, 0.998, 0.998, 0.998, 0.992, 0.959, 0.831, 0.679};
	
	private static final FunctionOfX i = new SellmeierEquation(0.936155374, 0.594052018, 1.04374583, 0.00461716525, 0.016885927, 103.736265);
	private static final FunctionOfX t = new TransmittanceLookupTable(tWavelength, 1e-9, 10e-3, transmissivity, 1.0, false, i);
	
	public nSk14(){
		super(i, t, null);
	}
	
	@Override
	public Color getColour(){
		// Light Sky Blue
		return new Color(50,150,100);
	}
}

