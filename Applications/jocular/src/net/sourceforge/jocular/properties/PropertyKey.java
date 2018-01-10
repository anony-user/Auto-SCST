/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.properties;

public enum PropertyKey {ABSORBS_PHOTONS("Absorb Photons","Indicates that this object should either absorb photons or have them pass right through."),
	ANGLE("Angle", "The angle to rotate."),
	ANGLE_1("Angle 1", "The first angle specifying this object."),
	ANGLE_2("Angle 2", "The second angle specifying this object."),
	APERTURE_DIAMETER("Aperture Diameter", "Force the emission to be constrained to within this aperture."),
	APERTURE_DISTANCE("Aperture Distance", "The distance to the aperture."),
	AXIS("Axis","The axis of the main positioner."),
	BACK_RADIUS("Back Radius","The back radius of this object."),
	BACK_SHAPE("Back Shape","The shape of the back of this object."),
	COEFF_UNITS("Coefficient Units", "Specify the units that the coefficients are intended to be used with."),
	CONIC_COEFF("Conic Coefficient", "The conic coefficient of an aspheric lens."),
	DIAMETER("Diameter", "The diameter of the object"), 
	DIR_ANGLE("Dir Angle","The amount to rotate about the dir axis."),
	DIR_OFFSET("Dir Offset","The offset in the direction of the positioner."), 
	EMISSION_PATTERN("Emission Pattern", "The pattern of this image sources emission."),
	FILE_NAME("File Name", "The file that this part is derived from."),
	FRONT_RADIUS("Front Radius","The front radius of this object."), 
	FRONT_SHAPE("Front Shape","The shape of the front of this object."), 
	GREY_SCALE("Grey scale", "The image source will treat image as grey scale not colour."),
	IMAGE("Image", "The image of this object, use defineImage to set this property."),
	INSIDE_MATERIAL("Material", "The material inside of this object."), 
	LENGTH("Length", "Indicates the length of this object."),
	NAME("Name", "A description name for this object."), 
	NUM_BINS("# of Bins","The number of bins of the histogram."),
	ORTHO_ANGLE("Ortho Angle","The angle in the orthogonal direction."), 
	ORTHO_DIM("Ortho Dim.", "The dimension (i.e. number of pixels, etc) in the orthogonal direction."),
	ORTHO_OFFSET("Ortho Offset", "The offset in the orthogonal direction of the positioner"), 
	ORTHO_SIZE("Ortho Size","The size of the object in the orthogonal direction."), 
	OUTER_DIAMETER("Outer diameter", "The outer diameter of this object"),
//	OUTSIDE_MATERIAL("Outside Material","The material outside of this object."), 
	PHASE_FWHM("Phase FWHM", "The full-width half-maximum of the phase."),
	PIXEL_TYPE("Pixel Type", "The type of pixel, whether colour or black and white."),
	POLY_COEFFS("Polynomial coefficients", "Coefficients of an aspherical lens."),
	POSITIONER("Positioner", "The positioner of this object. This is a virtual property."),
	RADIUS("Radius", "The radius of the object"), 
	ROT_AXIS("Rotation Axis", "The axis about which the rotation will occur."),
	SELECTED("Selected", "This object has been selected by the user. this is a virtual property."),
	SIMPLIFY_THRESHOLD("Simplify Threshold", "The error threshold for spline simplification. 0.0 means no simplification"),
	SPHERICAL_SHAPE("Spherical Shape", "The shape of the object."), 
	SUPPRESSED("Suppress", "Prevent this object from participating in a simulation."),
	THICKNESS("Thickness","The thickness of this object."), 
	TRANS_ANGLE("Trans Angle","The angle in the transverse direction."), 
	TRANS_DIM("Trans Dim.", "The dimension (i.e. number of pixels, etc) in the transverse direction."), 
	TRANS_OFFSET("Trans Offset", "The offset in the transverse direction of the positioner."), 
	TRANS_SIZE("Trans Size","The size of the object in the transverse direction."), 
	WAVELENGTH_FWHM("Wavelength FWHM","The full-width half-maximum of the wavelength."),
	WAVELENGTH_MAX("Max. Wavelength","The maximum wavelength of a distribution."),
	WAVELENGTH_MEAN("Mean Wavelength","The centre wavelength of a distribution."), 
	WAVELENGTH_MIN("Min. Wavelength","The minimum wavelength of a distribution."),
	WAVELENGTH_STDEV("St. Dev. Wavelength","The standard deviation of a distribution."),
	WIDTH("Width", "The width of this object."),
	MINIMUMS("Minumum Values", "An array of minimum values."),
	MAXIMUMS("Maximum Values", "An array of maximum values."),
	OBJECT_NAMES("Object names", "An array of object names."),
	OBJECT_PROPERTIES("Object properties", "An array of object properties"),
	POINTS_X("Point Independant value", "An array of point's independant values (X or Z)"),
	POINTS_Y("Point Dependant value", "An array of point's dependant values (Y or Rad)"),
	POINTS_TYPES("Point Type", "An array of point's type: cusp or smooth."),
						
	//these ones are for Settings
	PHOTON_COLOUR_SCHEME("Photon colour scheme", "The colourizing scheme for showing photon trajectories."),
	PICK_MOST_PROBABLE("Only most probable", "Only choose most probably path for photon trajectories."),
	TRAJECTORY_COUNT("Trajectory Count","The number of photon trajectories to calculate."),
	TRAJECTORY_DISPLAY_COUNT("Trajectory Display Count","The maximum number of photon trajectories to display."),
	USE_POLARIZATION("Use polarization", "Use polarization of photon when interacting with imager."),			
	WRANGLER_THREAD_COUNT("Wrangler thread number", "The number of threads to run while generating photon trajectories."),
	EQUATION_DISPLAY("Number Display", "How to display numbers.");
	private final String m_description;
	private final String m_tooltip;
	private PropertyKey(String description, String tooltip){
		m_description = description;
		m_tooltip = tooltip;
	}
	private PropertyKey(){
		m_description = name().toLowerCase();
		m_tooltip = "No tooltip defined.";
	}
	public String toString(){
		return m_description;
	}
	public String getDescription(){
		return m_description;
	}
	public String getTooltip(){
		return m_tooltip;
	}
	
	public static PropertyKey getKey(String keyToMatch){
		
		for(PropertyKey key : PropertyKey.values()){
		
			if(keyToMatch.equals(key.name())){
				return key;
			}
		}
		
		return null;
	}
}