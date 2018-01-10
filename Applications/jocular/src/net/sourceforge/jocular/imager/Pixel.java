package net.sourceforge.jocular.imager;

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.Photon;

public interface Pixel {
	public void clear();
	public void addPhoton(Photon photon, Vector3D trans);
	public double getMaxMagnitude();
	public double getMaxValue();
	public OpticsColour getNormalColour();
	public int getRGBMagnitude(double scale);
	public int getRGBValue(double scale);
	public double getRed(boolean valueNotMagnitude);
	public double getBlue(boolean valueNotMagnitude);
	public void addColourPoints(ColourPoint[] cps);
	public ColourPoint[] getColourPoints();
	public double getGreen(boolean valueNotMagnitude);

	
}
