package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;

public class Vacuum extends SimpleOpticalMaterial {
	public Vacuum(){
		super(new FunctionOfX() {
			public double getValue(double x){
				return 1.0;
			}
		}, new FunctionOfX() {
			public double getValue(double x){
				return 1.0;
			}
		}, null);
	}
	
	@Override
	public Color getColour(){
		 
		return Color.WHITE;
	}
}
