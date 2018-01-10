package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;
import net.sourceforge.jocular.math.SellmeierEquation;
import net.sourceforge.jocular.math.TransmittanceLookupTable;


public class nSf2 extends SimpleOpticalMaterial {
	
	
	private static final double[] tWavelength = {334, 350, 365, 370, 380, 390, 400, 405, 420, 436, 460, 500, 546, 580, 620, 660, 700, 1060, 1530, 1970, 2325, 2500};
	private static final double[] transmissivity = {0.11, 0.672, 0.877, 0.91, 0.946, 0.967, 0.981, 0.985, 0.99, 0.993, 0.995, 0.997, 0.998, 0.998, 0.998, 0.998, 0.998, 0.998, 0.994, 0.95, 0.872, 0.826};
	
	private static final FunctionOfX i = new SellmeierEquation(1.40301821, 0.231767504, 0.939056586, 0.0105795466, 0.0493226978, 112.405955);
	private static final FunctionOfX t = new TransmittanceLookupTable(tWavelength, 1e-9, 10e-3, transmissivity, 1.0, false, i);
	
	public nSf2(){
		super(i, t, null);
	}
	
	@Override
	public Color getColour(){
		// Light Sky Blue
		return new Color(50,100,150);
	}
}

