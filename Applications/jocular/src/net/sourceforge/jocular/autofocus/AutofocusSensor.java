/*******************************************************************************
 * Copyright (c) 2014,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.autofocus;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.AbstractOpticsObject;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OutputObject;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.Photon;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.StringArrayProperty;
import net.sourceforge.jocular.sources.HemiPointSource;
import net.sourceforge.jocular.sources.LightSource;

/**
 * @author kmaccallum
 *
 */
public class AutofocusSensor extends AbstractOpticsObject implements OutputObject{

	private EquationProperty m_diameter = new EquationProperty("10cm", this, PropertyKey.DIAMETER);
	private EquationArrayProperty m_minimums = new EquationArrayProperty("", this, PropertyKey.MINIMUMS);
	private EquationArrayProperty m_maximums = new EquationArrayProperty("", this, PropertyKey.MAXIMUMS);
	private StringArrayProperty m_objectNames = new StringArrayProperty("");
	private StringArrayProperty m_objectProperties = new StringArrayProperty("");
	public enum ErrorType {VARIANCE, MAX_EXTENT, MAX_CIRCLE, AV_DIST_FROM_AV};
	private class StatAccumulator {
//		private LightSource m_source;
		ArrayList<Vector3D> m_points = new ArrayList<Vector3D>();
		//ArrayList<Vector3D> m_dirs = new ArrayList<Vector3D>();
		private double m_avgAccX = 0;
		private double m_avgAccY = 0;
		private double m_avgAccZ = 0;
	
//		private double m_sqrAccX = 0;
//		private double m_sqrAccY = 0;
//		private double m_sqrAccZ = 0;
//		
//		private double m_count = 0;
//		private double m_minX = Double.POSITIVE_INFINITY;
//		private double m_maxX = Double.NEGATIVE_INFINITY;
//		private double m_minY = Double.POSITIVE_INFINITY;
//		private double m_maxY = Double.NEGATIVE_INFINITY;
//		private double m_circleX = 0;
//		private double m_circleY = 0;
//		private double m_circleR = 0;
//		public StatAccumulator(LightSource s){
//			m_source = s;
//		}
		public void acc(Vector3D p, Vector3D d){
			m_points.add(p);
			//m_dirs.add(d);
			m_avgAccX += p.x;
			m_avgAccY += p.y;
			m_avgAccZ += p.z;
			
//			m_sqrAccX += p.x*p.x;
//			m_sqrAccY += p.y*p.y;
//			m_sqrAccZ += p.z*p.z;
			
//			++m_count;
		}
//		public String pointsToString(){
//			String result = "";
//			for(Vector3D v : m_points){
//				result+= v+"\n";
//			}
//			return result;
//		}
		
		public double getError(){
			double result;
			
			Vector3D avg = new Vector3D(m_avgAccX,m_avgAccY,m_avgAccZ);
			
			avg = avg.scale(1.0/((double)m_points.size()));
			double acc = 0.0;
			for(int i = 0; i < m_points.size(); ++i){
				
			//for(Vector3D v : m_points){
				//Vector3D dv = v.subtract(avg);
				Vector3D dv = m_points.get(i).subtract(avg);
				double r = dv.abs();
//					if(dv.dot(m_dirs.get(i)) >= 0){
//						r = -r;
//					}
				
				acc += r;
			}
			acc = acc/m_points.size();
			result = acc;
				
			
			return result;
			
		}
//		public LightSource getSource() {
//			return m_source;
//		}
	}
	private HashMap<LightSource, StatAccumulator> m_sourceAccumulators = new HashMap<LightSource, StatAccumulator>();


	
	public AutofocusSensor() {
		super();
		clear();
	}
	public double getError(){
		double acc = 0;
		for(StatAccumulator sa : m_sourceAccumulators.values()){
			acc += Math.pow(sa.getError(),2);
		}
		return acc;
	}


	@Override
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is) {

		Photon p = pt.getPhoton();
		//if this photon did not come from a hemi point source then don't bother computing
		if(!(pt.getSourceObject(0) instanceof HemiPointSource)){
			return;
		}
		Vector3D intersection = Vector3D.INF;
//		Vector3D yDir = getPositioner().getTransDirection();
//		Vector3D xDir = Vector3D.getTrans(getPositioner().getDirection(),yDir);
		Vector3D distToImagerOrigin = getPositioner().getOrigin().subtract(p.getOrigin());
		Vector3D alongImagerNormal = distToImagerOrigin.getParallelComponent(getPositioner().getDirection());
		double d = alongImagerNormal.magSquared()/alongImagerNormal.dot(p.getDirection());
		if(d > 0){
			Vector3D vectToImagerPlane = p.getDirection().scale(d);
			Vector3D pointOnImagerPlane = vectToImagerPlane.add(p.getOrigin());
			Vector3D distAlongPlane = pointOnImagerPlane.subtract(getPositioner().getOrigin());
//			double yDist = yDir.dot(distAlongPlane);
//			double xDist = xDir.dot(distAlongPlane);
			//make sure the photon crosses plane within the imager size limits
			if(distAlongPlane.abs()*2 < m_diameter.getValue().getBaseUnitValue()){
				intersection = pointOnImagerPlane;
			}
		}
		is.add(new PhotonInteraction(p, this, null, intersection, null, false, ""));
	}

	
	
	@Override
	public void interact(PhotonInteraction pi, PhotonTrajectory pt) {
		//TODO: accumulate new photon
		addPhoton(pi, pt);
		//make a new photon just like the incoming one
		synchronized(this){
			pt.addPhoton(new Photon(pt.getPhoton()), pi);
		}
	}
	
	private void addPhoton(PhotonInteraction pi, PhotonTrajectory pt){
		if(!(pt.getSourceObject(0) instanceof HemiPointSource)){
			//throw new RuntimeException("Trajectory photon source is not a LightSource.");
			return;
		}
		//don't include photons that have come straight from the source
		if(pt.getNumberOfPhotons() <= 1){
			return;
		}
//		Vector3D delta = pi.getLocation().subtract(getPositioner().getOrigin());
//		double x = getPositioner().getOrthoDirection().dot(delta);
//		double y = getPositioner().getTransDirection().dot(delta);
		
		LightSource source = (LightSource)pt.getSourceObject(0);
		if(m_sourceAccumulators.containsKey(source)){
			m_sourceAccumulators.get(source).acc(pi.getLocation(),pt.getPhoton().getDirection());
		} else {
			StatAccumulator sa = new StatAccumulator();
			sa.acc(pi.getLocation(),pt.getPhoton().getDirection());
			m_sourceAccumulators.put(source, sa);
		}
	}


	public void clear(){
		m_sourceAccumulators = new HashMap<LightSource, StatAccumulator>();
	}




	@Override
	public void accept(OpticsObjectVisitor v) {
		v.visit(this);

	}
	
	
	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case DIAMETER:
			m_diameter = new EquationProperty(s, this, key);
			break;
		case MINIMUMS:
			m_minimums = new EquationArrayProperty(s, this, key);
			break;
		case MAXIMUMS:
			m_maximums = new EquationArrayProperty(s, this, key);
			break;
		case OBJECT_NAMES:
			m_objectNames = new StringArrayProperty(s);
			break;
		case OBJECT_PROPERTIES:
			m_objectProperties = new StringArrayProperty(s);
			break;
		default:
			super.setProperty(key, s);
			break;
		}
		if(getProperty(key) != null){
			firePropertyUpdated(key);
		}
	}

	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result = null;
		switch(key){
		case DIAMETER:
			result = m_diameter;
			break;
		case MINIMUMS:
			result = m_minimums;
			break;
		case MAXIMUMS:
			result = m_maximums;
			break;
		case OBJECT_NAMES:
			result = m_objectNames;
			break;
		case OBJECT_PROPERTIES:
			result = m_objectProperties;
			break;
		default:
			result = super.getProperty(key);
			break;
			
		}
		return result;
	}
	
	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.DIAMETER, PropertyKey.MINIMUMS, PropertyKey.MAXIMUMS, PropertyKey.OBJECT_NAMES, PropertyKey.OBJECT_PROPERTIES, PropertyKey.MINIMUMS, PropertyKey.MAXIMUMS));
		return result;
	}
	public int getParameterCount(){
		int no = m_objectNames.size();
		int np = m_objectProperties.size();
		int nm = m_minimums.size();
		int nn = m_maximums.size();
		int result = no;
		if(result > np){
			result = np;
		}
		if(result > nm){
			result = nm;
		}
		if(result > nn){
			result = nn;
		}
		return result;
	}
//	public void addParameter(int i){
//		
//		m_objectProperties.add(i);
//		m_minimums.add(i);
//		m_maximums.add(i);
//		m_objectNames.add(i);
//	}
	@Override
	public OpticsObject makeCopy() {
		AutofocusSensor result = new AutofocusSensor();
		result.copyProperties(this);
		result.setPositioner(getPositioner().makeCopy());
		return result;
	}
}
