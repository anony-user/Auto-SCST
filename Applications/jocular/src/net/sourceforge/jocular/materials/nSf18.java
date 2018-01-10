package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;
import net.sourceforge.jocular.math.SumitaDispersion;
import net.sourceforge.jocular.math.TransmittanceLookupTable;


public class nSf18 extends SimpleOpticalMaterial {
	
	
	private static final double[] tWavelength = {340, 350, 360, 370, 380, 390, 400, 420, 440, 460, 480, 500, 550, 600, 650, 700, 800, 1060, 1500, 2000};
	private static final double[] transmissivity = {0.05, 0.187, 0.482, 0.701, 0.815, 0.883, 0.929, 0.961, 0.974, 0.98, 0.982, 0.985, 0.988, 0.99, 0.991, 0.995, 0.998, 0.998, 0.997, 0.968};
	
	private static final FunctionOfX i = new SumitaDispersion(2.8594457, -1.0229223E-02, 3.1936097E-02, 2.0257382E-03, -1.1649195E-04, 1.4181908E-05);
	private static final FunctionOfX t = new TransmittanceLookupTable(tWavelength, 1e-9, 10e-3, transmissivity, 1.0, false, i);
	
	public nSf18(){
		super(i, t, null);
	}
	
	@Override
	public Color getColour(){
		// Light Sky Blue
		return new Color(150,100,50);
	}
}

