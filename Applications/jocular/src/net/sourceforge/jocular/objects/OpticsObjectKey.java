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
package net.sourceforge.jocular.objects;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import net.sourceforge.jocular.autofocus.AutofocusSensor;
import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.sources.HemiPointSource;
import net.sourceforge.jocular.sources.ImageSource;
import net.sourceforge.jocular.splines.ExtrudedSpline;
import net.sourceforge.jocular.splines.RotatedSpline;
/**
 * A handy list of objects that can be used by the user. Contains a name, an icon, a tooltip and can be used to make a new instance of that type of object.
 * @author tandk
 *
 */
public enum OpticsObjectKey {SPHERICAL_LENS("Spherical Lens",new SphericalLens(), "SphericalLens.png", "A simple lens with front and back spherical surface and an outer diameter"),
							OPTICS_GROUP("Group", new OpticsObjectGroup(), "OpticsObjectGroup.png", "A group of objects"),
							IMAGER("Imager", new Imager(), "Imager.png", "A RGB sensor for generating simulated images."),
							POINT_SOURCE("Point Source", new HemiPointSource(), "PointSource.png", "A point source of light."),
							ROOT_GROUP("Root Group", new ProjectRootGroup(), "RootGroup.png", "The root of a project tree."),
							TRIANGULAR_PRISM("Triangular Prism", new TriangularPrism(), "TriangularPrism.png", "A triangular prism."),
							IMAGE_SOURCE("Image Source", new ImageSource(), "ImageSource.png", "An image source."),
							SIMPLE_APERTURE("Simple Aperture", new SimpleAperture(), "SimpleAperture.png", "A simple aperture."),
							SPECTROPHOTOMETER("Spectrophotometer", new SpectroPhotometer(), "SpectroPhotometer.png", "A spectrophotometer."),
							AUTOFOCUS_SENSOR("Autofocus Sensor", new AutofocusSensor(), "AutofocusSensor.png", "An autofocus sensor."),
							ROTATED_SPLINE("Rotated Spline", new RotatedSpline(), "RotatedSpline.png", "An object defined by rotating a spline curve."),
							EXTRUDED_SPLINE("Extruded Spline", new ExtrudedSpline(), "ExtrudedSpline.png", "An object defined by extruded a spline curve."),
							PLANO_ASPHERIC_LENS("Plano-Aspheric Lens", new PlanoAsphericLens(), "PlanoAsphericLens.png", "A lens with one flat side and one aspheric side difined by a base radius, a conic coefficient and a series of polynomial coefficients."),
							PART("Part", new OpticsPart(),"Part.png", "A part based on a specified project.");
	private String name;
	private OpticsObject prototype;
	private Icon icon;
	private String tooltip;
	private OpticsObjectKey(String n, OpticsObject o, String i, String t){
		name = n;
		prototype = o;
		tooltip = t;
			
		if(i == null){
			icon = null;
		} else {
			Class<? extends String> c = null;
			URL url = null;
							
			c = i.getClass();
			
			if(c != null){
				url = c.getResource("/net/sourceforge/jocular/icons/"+n);
			}
			if(url != null){
				icon = new ImageIcon(url);
			}
		}
	}
	public String getName(){
		return name;
	}
	public OpticsObject getNewObject(){
		return prototype.makeCopy();
	}
	public Icon getIcon(){
		return icon;
	}
	public String getToolTipText(){
		return tooltip;
	}
	
	public static OpticsObjectKey getKey(String objectToMatch){
		
		for(OpticsObjectKey key : OpticsObjectKey.values()){
		
			if(objectToMatch.equals(key.name())){
				return key;
			}
		}
		
		return null;
	}
	
	public static String getKeyName(OpticsObject p){
		String result = null;
		for(OpticsObjectKey k : OpticsObjectKey.values()){
			if(k.prototype.getClass() == p.getClass()){
				result = k.name();
				break;
			}
		}
		return result;
		
	}

}
