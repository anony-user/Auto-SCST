package net.sourceforge.jocular.materials;

import java.awt.Color;

import net.sourceforge.jocular.math.FunctionOfX;

public class ShinyMetal extends SimpleOpticalMaterial {
	public ShinyMetal(){
		super(new FunctionOfX() {
			public double getValue(double x){
				return 0.0;
			}
		}, new FunctionOfX() {
			public double getValue(double x){
				return 0.0;
			}
		}, null);
	}
	
	@Override
	public Color getColour(){
		 
		return Color.GRAY;
	}
}
