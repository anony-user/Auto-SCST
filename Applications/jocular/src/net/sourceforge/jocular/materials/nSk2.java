package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;
import net.sourceforge.jocular.math.SellmeierEquation;
import net.sourceforge.jocular.math.TransmittanceLookupTable;


public class nSk2 extends SimpleOpticalMaterial {
	
	
	private static final double[] tWavelength = {290, 300, 310, 320, 334, 350, 365, 370, 380, 390, 400, 405, 420, 436, 460, 500, 546, 580, 620, 660, 700, 1060, 1530, 1970, 2325, 2500};
	private static final double[] transmissivity = {0.02, 0.102, 0.276, 0.504, 0.752, 0.905, 0.967, 0.976, 0.988, 0.992, 0.994, 0.994, 0.994, 0.993, 0.993, 0.996, 0.998, 0.998, 0.998, 0.998, 0.998, 0.998, 0.995, 0.971, 0.896, 0.815};
	
	private static final FunctionOfX i = new SellmeierEquation(1.28189012, 0.257738258, 0.96818604, 0.0072719164, 0.0242823527, 110.377773);
	private static final FunctionOfX t = new TransmittanceLookupTable(tWavelength, 1e-9, 10e-3, transmissivity, 1.0, false, i);
	
	public nSk2(){
		super(i, t, null);
	}
	
	@Override
	public Color getColour(){
		// Light Sky Blue
		return new Color(150,100,50);
	}
}

