package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;
import net.sourceforge.jocular.math.SellmeierEquation;
import net.sourceforge.jocular.math.TransmittanceLookupTable;


public class nSf57 extends SimpleOpticalMaterial {
	
	
	private static final double[] tWavelength = {365, 370, 380, 390, 400, 405, 420, 436, 460, 500, 546, 580, 620, 660, 700, 1060, 1530, 1970, 2325, 2500};
	private static final double[] transmissivity = {0.003, 0.063, 0.302, 0.574, 0.733, 0.782, 0.872, 0.919, 0.949, 0.971, 0.986, 0.99, 0.988, 0.987, 0.991, 0.999, 0.992, 0.956, 0.838, 0.806};
	
	private static final FunctionOfX i = new SellmeierEquation(1.87543831, 0.37375749, 2.30001797, 0.0141749518, 0.0640509927, 177.389795);
	private static final FunctionOfX t = new TransmittanceLookupTable(tWavelength, 1e-9, 10e-3, transmissivity, 1.0, false, i);
	
	public nSf57(){
		super(i, t, null);
	}
	
	@Override
	public Color getColour(){
		// Light Sky Blue
		return new Color(50,100,150);
	}
}

