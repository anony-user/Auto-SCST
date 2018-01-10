package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.SellmeierEquation;
import net.sourceforge.jocular.math.TransmittanceLookupTable;


public class MagnesiumFluoride extends SimpleOpticalMaterial {
	private static final double[] w = {0.0000002, 0.0000005, 0.000001, 0.000003, 0.000005, 0.000006, 0.000007, 0.000008};
	private static final double[] t = {0.95, 0.97, 0.97, 0.97, 0.97, 0.91, 0.54, 0.12}; 
	public MagnesiumFluoride(){

		super(new SellmeierEquation(0.4134, 0.5050, 2.4905,  0.0013574, 0.0082377, 565.11), new SellmeierEquation(0.4876, 0.3988, 2.312, 0.0054804, 0.0089519, 566.14), new TransmittanceLookupTable(w, 1, 1e-3, t, 1, false, null), null);
		
	}
	
	@Override
	public Color getColour(){
		// royal blue?
		return new Color(100,100,255);
	}
}

