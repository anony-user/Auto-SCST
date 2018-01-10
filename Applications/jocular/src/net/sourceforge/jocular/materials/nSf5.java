package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;
import net.sourceforge.jocular.math.SellmeierEquation;
import net.sourceforge.jocular.math.TransmittanceLookupTable;


public class nSf5 extends SimpleOpticalMaterial {
	
	
	private static final double[] tWavelength = {365, 370, 380, 390, 400, 405, 420, 436, 460, 500, 546, 580, 620, 660, 700, 1060, 1530, 1970, 2325, 2500};
	private static final double[] transmissivity = {0.116, 0.276, 0.642, 0.826, 0.905, 0.928, 0.963, 0.973, 0.982, 0.99, 0.995, 0.996, 0.995, 0.995, 0.996, 0.998, 0.99, 0.95, 0.831, 0.758};
	
	private static final FunctionOfX i = new SellmeierEquation(1.52481889, 0.187085527, 1.42729015, 0.011254756, 0.0588995392, 129.141675);
	private static final FunctionOfX t = new TransmittanceLookupTable(tWavelength, 1e-9, 10e-3, transmissivity, 1.0, false, i);
	
	public nSf5(){
		super(i, t, null);
	}
	
	@Override
	public Color getColour(){
		// Light Sky Blue
		return new Color(150,100,50);
	}
}

