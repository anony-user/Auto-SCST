package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;
import net.sourceforge.jocular.math.SellmeierEquation;
import net.sourceforge.jocular.math.TransmittanceLookupTable;


public class lBal35 extends SimpleOpticalMaterial {
	
	private static final double[] iWavelength = {2.32542, 1.97009, 1.52958, 1.12864, 1.01398, 0.85211, 0.76819, 0.70652, 0.65627, 0.64385, 0.6328, 0.58929, 0.58756, 0.54607, 0.48613, 0.47999, 0.44157, 0.435835, 0.404656, 0.365015};
	private static final double[] index = {1.55775, 1.56407, 1.57069, 1.57622, 1.57795, 1.58085, 1.58276, 1.58448, 1.58618, 1.58665, 1.58709, 1.58904, 1.58913, 1.59143, 1.59581, 1.59636, 1.60031, 1.601, 1.60528, 1.61256};
	private static final double[] tWavelength = {280, 290, 300, 310, 320, 330, 340, 350, 360, 370, 380, 390, 400, 420, 440, 460, 480, 500, 550, 600, 650, 700, 800, 900, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400};
	private static final double[] transmissivity = {0, 0, 0.06, 0.27, 0.53, 0.73, 0.85, 0.922, 0.956, 0.975, 0.984, 0.989, 0.992, 0.993, 0.993, 0.995, 0.996, 0.998, 0.999, 0.998, 0.998, 0.998, 0.999, 0.998, 0.997, 0.997, 0.991, 0.994, 0.989, 0.978, 0.934, 0.81};
	//private static final FunctionOfX i = new SellmeierEquation(iWavelength, index, 1e-6);
	//private static final FunctionOfX i = new SellmeierEquation(1.09972335, 3.8787253e-1, 1.11247378, 5.8230345e-3, 1.8874514e-2, 1.08214962e2);
	private static final FunctionOfX i = new SellmeierEquation(1.16262630E+00, 3.25661051E-01, 1.35132486E+00, 1.25957437E-02, -3.26911050E-03, 1.19214596E+02);
	private static final FunctionOfX t = new TransmittanceLookupTable(tWavelength, 1e-9, 10e-3, transmissivity, 1.0, false, i);
	
	public lBal35(){
		super(i, t, null);
	}
	
	@Override
	public Color getColour(){
		// Light Sky Blue
		return new Color(150,130,255);
	}
}

