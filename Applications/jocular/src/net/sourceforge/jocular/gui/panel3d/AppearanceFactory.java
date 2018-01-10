/*******************************************************************************
 * Copyright (c) 2013, Bryan Matthews
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.gui.panel3d;

import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.TEXTURE_2D;

import java.awt.image.BufferedImage;

import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.shader.Color;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultLineShader;
import de.jreality.shader.DefaultPolygonShader;
import de.jreality.shader.ImageData;
import de.jreality.shader.RenderingHintsShader;
import de.jreality.shader.ShaderUtility;
import de.jreality.shader.Texture2D;
import net.sourceforge.jocular.autofocus.AutofocusSensor;
import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.imager.OpticsColour;
import net.sourceforge.jocular.materials.OpticalMaterial;
import net.sourceforge.jocular.materials.SimpleOpticalMaterial;
import net.sourceforge.jocular.objects.CylindricalSurface;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsPart;
import net.sourceforge.jocular.objects.PlanoAsphericLens;
import net.sourceforge.jocular.objects.ProjectRootGroup;
import net.sourceforge.jocular.objects.SimpleAperture;
import net.sourceforge.jocular.objects.SpectroPhotometer;
import net.sourceforge.jocular.objects.SphericalLens;
import net.sourceforge.jocular.objects.SphericalSurface;
import net.sourceforge.jocular.objects.TriangularPrism;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.BooleanProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.ImageProperty;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.sources.HemiPointSource;
import net.sourceforge.jocular.sources.ImageSource;
import net.sourceforge.jocular.splines.ExtrudedSpline;
import net.sourceforge.jocular.splines.RotatedSpline;

/**
 * A factory for applying a default appearance to 3D geometry based on the type of object.
 * 
 * @author Bryan Matthews
 *
 */
public class AppearanceFactory implements OpticsObjectVisitor {
	
	private final double LENS_TRANSPARENCY = 0.3;
	private final double IMAGER_TRANSPARENCY = 0.0;
	private final Color IMAGER_TRANSPARENT_COLOUR = new Color(176,226,255); // Light Sky Blue
	
	private final Appearance m_ap;
	private final DefaultGeometryShader m_geometryShader;
	private final RenderingHintsShader m_rhs;
	private final DefaultPolygonShader m_polygonShader;
	private final DefaultLineShader m_lineShader;
	//private final DefaultPointShader m_pointShader;
	
	
	private boolean m_isTransparent;
	
	public AppearanceFactory(SceneGraphComponent sgc){

		m_ap = sgc.getAppearance();
		m_geometryShader = ShaderUtility.createDefaultGeometryShader(m_ap, true);
		m_geometryShader.setShowLines(true);
		m_polygonShader = (DefaultPolygonShader) m_geometryShader.createPolygonShader("default");
		m_rhs = ShaderUtility.createDefaultRenderingHintsShader(m_ap, true);
		m_rhs.setAdditiveBlendingEnabled(true);
		m_lineShader = (DefaultLineShader) m_geometryShader.createLineShader("default");
//		m_lineShader.setTubeDraw(false);
//		m_lineShader.setLineWidth(5.0);
//		m_lineShader.setDiffuseColor(Color.BLACK);
		//m_pointShader = (DefaultPointShader) m_geometryShader.createPointShader("default");
		
		m_isTransparent = false;
	}
	
	public boolean setAppearance(OpticsObject obj){
		
		obj.accept(this);
		
		if(obj.isSelected()){
			setIsSelectedAppearance();
		}
		
		return m_isTransparent;
	}

	@Override
	public void visit(Imager v) {
		
		BooleanProperty absorbs = (BooleanProperty)v.getProperty(PropertyKey.ABSORBS_PHOTONS);
				
		if(absorbs.getValue()){
			setDefaultAppearance(Color.WHITE);
			BufferedImage image = ((ImageProperty)v.getProperty(PropertyKey.IMAGE)).getValue();			
			ImageData data = new ImageData(image);			
			setTexture(data);
		} else{

			setDefaultAppearance(IMAGER_TRANSPARENT_COLOUR);
			setTransparency(IMAGER_TRANSPARENCY);
			m_isTransparent = true;
		}
		
	}

	@Override
	public void visit(SphericalSurface v) {

		//A SphericalSurface is only used as part of a SphericalLens 
	}

	@Override
	public void visit(SphericalLens v) {
		
		setPropertiesFromMaterial(v.getMaterial());
		
				
	}
	@Override
	public void visit(PlanoAsphericLens v) {
		
		setPropertiesFromMaterial(v.getMaterial());	
	}
	@Override
	public void visit(RotatedSpline v) {
		
		setPropertiesFromMaterial(v.getMaterial());	
	}
	@Override
	public void visit(ExtrudedSpline v) {
		
		setPropertiesFromMaterial(v.getMaterial());	
	}
	@Override
	public void visit(HemiPointSource v) {
		
		double wavelength = ((EquationProperty)(v.getProperty(PropertyKey.WAVELENGTH_MEAN))).getValue().getBaseUnitValue();
		OpticsColour colour = OpticsColour.getColorFromWavelength(wavelength);
		
		setDefaultAppearance(colour.getShaderColour());
	}

	@Override
	public void visit(CylindricalSurface v) {
		//A CylindricalSurface is only used as part of a SphericalLens
	}

	@Override
	public void visit(OpticsObjectGroup v) {
		// Object groups are not drawn
		
	}
	@Override
	public void visit(OpticsPart v) {
		// Object groups are not drawn
		
	}
	@Override
	public void visit(ProjectRootGroup v) {
		// Object groups are not drawn
		
	}

	@Override
	public void visit(TriangularPrism v) {
		
		setPropertiesFromMaterial(v.getMaterial());
		
	}

	@Override
	public void visit(ImageSource v) {
	
		setDefaultAppearance(Color.WHITE);
		
		BufferedImage image = ((ImageProperty) v.getProperty(PropertyKey.IMAGE)).getValue();		
		ImageData data = new ImageData(image);		
		setTexture(data);
	}

	@Override
	public void visit(SimpleAperture v) {
		setDefaultAppearance(Color.WHITE);
	}

	@Override
	public void visit(SpectroPhotometer v) {
		setDefaultAppearance(IMAGER_TRANSPARENT_COLOUR);
		setTransparency(IMAGER_TRANSPARENCY);
		m_isTransparent = true;
	}
	@Override
	public void visit(AutofocusSensor v) {
		setDefaultAppearance(IMAGER_TRANSPARENT_COLOUR);
		setTransparency(IMAGER_TRANSPARENCY);
		m_isTransparent = true;
	}
	
	private void setTexture(ImageData image){
						
		Texture2D tex2d = (Texture2D) AttributeEntityUtility.createAttributeEntity(Texture2D.class, 
				POLYGON_SHADER+"."+TEXTURE_2D,m_ap, true);
		
		tex2d.setImage(image);
		
		Matrix foo = new Matrix();
		MatrixBuilder.euclidean().rotateY(Math.PI).scale(1).assignTo(foo);
		tex2d.setTextureMatrix(foo);
	}
	
	private void setPropertiesFromMaterial(OpticalMaterial m){
		setDefaultAppearance(m.getShaderColour());
		
		if(m != SimpleOpticalMaterial.SHINYMETAL){

			setTransparency(LENS_TRANSPARENCY);
			
		} else {

			setTransparency(0.0);

		}
	}
	private void setTransparency(Double transparency){
		if(transparency == 0.0){
			m_isTransparent = false;
			m_rhs.setTransparencyEnabled(false);	
		} else {
			m_isTransparent = true;
			m_rhs.setTransparencyEnabled(true);	
		}
		m_polygonShader.setTransparency(transparency);		
		
	}
	
	private void setIsSelectedAppearance(){
		
		m_geometryShader.setShowLines(true);
				
		m_lineShader.setLineLighting(false);
		m_lineShader.setLineWidth(2.0);
		m_lineShader.setTubeDraw(false);
		m_lineShader.setDiffuseColor(Color.MAGENTA);
		
		m_rhs.setOpaqueTubesAndSpheres(true);
		
	}
	
	private void setDefaultAppearance(Color diffuseColour){

		m_geometryShader.setShowFaces(true);
		m_geometryShader.setShowLines(true);
		m_geometryShader.setShowPoints(false);
				
		m_polygonShader.setSmoothShading(true);
		m_polygonShader.setAmbientCoefficient(0.0);
		m_polygonShader.setDiffuseColor(diffuseColour);
		m_polygonShader.setSpecularCoefficient(0.0);
		
		m_lineShader.setTubeDraw(false);
		m_lineShader.setLineWidth(2.0);
		m_lineShader.setDiffuseColor(Color.BLACK);
				
		m_rhs.setOpaqueTubesAndSpheres(false);	
	}	
}
