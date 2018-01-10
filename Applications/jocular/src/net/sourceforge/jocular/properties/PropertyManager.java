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
package net.sourceforge.jocular.properties;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sourceforge.jocular.math.equations.EquationParser;
import net.sourceforge.jocular.math.equations.EquationParser.CalcState;
import net.sourceforge.jocular.math.equations.PropertyNotFoundException;
import net.sourceforge.jocular.math.equations.UnitedValue;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectCombo;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsPart;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.PropertyOwnerInterestList.PropertyOwnerLink;


public class PropertyManager implements PropertyUpdatedListener {
	//private static OpticsProject m_project = new OpticsProject();
	private static final PropertyManager PM_SINGLETON = new PropertyManager();
	private CopyOnWriteArrayList<OpticsProject> m_projects = new CopyOnWriteArrayList<OpticsProject>();
	private static EquationParser m_equationParser = new EquationParser();
	private PropertyOwnerInterestList m_interestList = new PropertyOwnerInterestList();
	private String m_updateReference = null;
	//private boolean m_deferParsing = false;
	private long m_startTimeMillis = System.currentTimeMillis();
	private CopyOnWriteArrayList<OpticsProject> m_notDeferredProjects = new CopyOnWriteArrayList<OpticsProject>();
	private PropertyManager(){
		super();
	}
	public static PropertyManager getInstance(){
		return PM_SINGLETON;
	}
	
	public List<UnitedValue> parseEquation(String s, PropertyOwner po, PropertyKey key){
		if(isDeferred(getProject(po))){
			return Collections.singletonList(UnitedValue.DEFERRED);
		}
		removeInterest(po, key);
		boolean namedThisTime;
//		//TODO:this is just for debugging
//		if(m_startTimeMillis + 30000 < System.currentTimeMillis()){
//			throw new RuntimeException("10s elapsed.");
//		}
		if(m_updateReference == null){
			namedThisTime = true;
			Property p = po.getProperty(PropertyKey.NAME);
			m_updateReference = DateFormat.getDateTimeInstance().format(new Date());
			m_updateReference += " "+po.getClass().getSimpleName()+" ";
			if(p != null){
				m_updateReference += "\""+p.getDefiningString()+"\"";
			} else {
				m_updateReference += "null";
			}
			m_updateReference += ">"+key;
		} else {
			namedThisTime = false;
		}
		//System.out.println("PropertyManager.parseEquation ("+m_updateReference+") "+po+"\""+s+"\" interest list:  "+m_interestList);
		
		List<UnitedValue> result = m_equationParser.parse(s, po, this, key);
		
		if(namedThisTime){
			m_updateReference = null;
		}
		
		return result;
	}
//	public OpticsObject lookupAndRegisterInterestInObject(OpticsObject interestedObject, String interestingObjectName){
//		return null;
//	}
	/**
	 * this will look up the property of the specified object.
	 * 
	 * @param objectName
	 * @param propertyName
	 * @return
	 */
	public UnitedValue lookupObjectProperty(String objectName, String propertyName, PropertyOwner po, PropertyKey key){
		UnitedValue result = null;
		

		List<OpticsObject> os = getObjectByName(objectName, po);
		if(os.size() > 1){
			return UnitedValue.makeError(CalcState.UNDERSPECIFIED_OBJECT);
			//throw new ObjectUnderspecifiedException(objectName);
		}
		

		
			

		if(os.size() == 0){
			
			
			return UnitedValue.makeError(CalcState.OBJECT_NOT_FOUND);	
			
		}
		OpticsObject o = os.get(0); 
		PropertyKey pk = getPropertyKey(o, propertyName);
		Property p = o.getProperty(pk);
		//first see if this link exists
		//TODO:
		if(m_interestList.doesLinkExist(po,  key,  o,  pk)){
			//link exists, don't bother adding
		} else {
			//check for circular reference
			PropertyOwnerLink pol = m_interestList.checkForCircularReference(po, key, o, pk);
			if(pol != null){
				String s = "";
				s += o.getProperty(PropertyKey.NAME).getDefiningString();
				s += ">"+pk.name() + " depends on ";
				s += po.getProperty(PropertyKey.NAME).getDefiningString();
				s += ">"+key.name() + ".";
				return UnitedValue.makeError(CalcState.CIRCULAR_REFERENCE);
			} else {
				addInterest(po, key, o, pk);
			}
		}
		//System.out.println("PropertyManager.lookupObjectProperty "+p.getDefiningString());
		if(p instanceof EquationProperty){
			
			//UnitedValue uv = m_equationParser.parse(p.getDefiningString(), po, this, key);
			UnitedValue uv = ((EquationProperty)p).getValue();
			if(uv.isDeferred()){
				updateEquation(o, pk, true);
				uv = ((EquationProperty)p).getValue();
				
			}
			//EquationProperty ep = (EquationProperty)p;
			//result = UnitedValue.makeFunctionResult(ep.getUnitedValue().getUnit(), ep.getUnitedValue().getValue());
			result = UnitedValue.makeFunctionResult(uv.getUnit(), uv.getValue());
		} else {
			result = UnitedValue.makeError(CalcState.PROPERTY_NOT_NUMBER);
		}
		return result;
	}
	/**
	 * this will look up the property of the positioner of the specified object.
	 * 
	 * @param objectName
	 * @param propertyName
	 * @return
	 */
	public UnitedValue lookupObjectPositionerProperty(String objectName, String propertyName, PropertyOwner po, PropertyKey key){
		UnitedValue result;
	
		List<OpticsObject> os = getObjectByName(objectName, po);
		if(os.size() > 1){
			return UnitedValue.makeError(CalcState.UNDERSPECIFIED_OBJECT);
			//throw new ObjectUnderspecifiedException(objectName);
		}
		
		if(os.size() == 0){
			return UnitedValue.makeError(CalcState.OBJECT_NOT_FOUND);
			//throw new ObjectNotFoundException(objectName);
		}
		OpticsObject o = os.get(0); 
		ObjectPositioner pos = o.getPositioner();
		PropertyKey pk = getPropertyKey(pos, propertyName);
		Property p = pos.getProperty(pk);
		//check for circular reference
		PropertyOwnerLink pol = m_interestList.checkForCircularReference(po, key, pos, pk);
		if(pol != null){
			String s = "";
			s += pos.getProperty(PropertyKey.NAME).getDefiningString();
			s += ">"+pk.name() + " depends on ";
			s += po.getProperty(PropertyKey.NAME).getDefiningString();
			s += ">"+key.name() + ".";
			return UnitedValue.makeError(CalcState.CIRCULAR_REFERENCE);
		} else {
			addInterest(po, key, pos, pk);
		}
		if(p instanceof EquationProperty){
			//UnitedValue uv = m_equationParser.parse(p.getDefiningString(), po, this, key);
			UnitedValue uv = ((EquationProperty)p).getValue();
			if(uv.isDeferred()){
				updateEquation(pos, pk, true);
				uv = ((EquationProperty)p).getValue();
				
			}
			//EquationProperty ep = (EquationProperty)p;
			//result = UnitedValue.makeFunctionResult(ep.getUnitedValue().getUnit(), ep.getUnitedValue().getValue());
			result = UnitedValue.makeFunctionResult(uv.getUnit(), uv.getValue());
		} else {
			result = UnitedValue.makeError(CalcState.PROPERTY_NOT_NUMBER);	
		}
		
		return result;
	}
	protected void removeInterest(PropertyOwner interestedOwner, PropertyKey interestedKey){
		//List<PropertyOwnerLink> links = m_interestList.getLinksOfInterest(interestedOwner, interestedKey);
		m_interestList.removeOwnerInterest(interestedOwner, interestedKey);
	}
	protected void addInterest(PropertyOwner interestedOwner, PropertyKey interestedKey, PropertyOwner interestingOwner, PropertyKey interestingKey){
		
		//first check to see if this already exists
		if(!m_interestList.doesLinkExist(interestedOwner, interestedKey, interestingOwner, interestingKey)){
			m_interestList.addOwnerInterest(interestedOwner, interestedKey, interestingOwner, interestingKey);
			interestingOwner.addPropertyUpdatedListener(this);
		}
		
	}
	public void addProject(OpticsProject p){
		if(!m_projects.contains(p)){
			m_projects.add(p);
			OpticsObject o = p.getOpticsObject();
			updateEquations(o, false);
			
		}
	}
	public void removeProject(OpticsProject p){
		Collection<OpticsObject> parts = p.getOpticsObject().getFlattenedObjectsOfType(OpticsPart.class, true);
		m_projects.remove(p);
		m_projects.removeAll(parts);
	}
	/**
	 * looks up an object in the project containing the property owner. It first looks for a name but it will then check for a matching ID.
	 * @param n
	 * @param po
	 * @return
	 */
	public List<OpticsObject> getObjectByName(String n, PropertyOwner po){
		OpticsProject p = getProject(po);
		if(p == null){
			return new ArrayList<OpticsObject>();
		}
		return getObjectByName(p.getOpticsObject(), n);
	}
	/**
	 * compares object name and ID to the specified string
	 * @param o
	 * @param n
	 * @return
	 */
	protected boolean isNamedObject(OpticsObject o, String n){
		boolean result = false;
		String s = o.getProperty(PropertyKey.NAME).getDefiningString();
		
		if(s.toLowerCase().trim().equals(n.toLowerCase().trim())){
			result = true;
		} else if(n.equals(o.getID().toHashString().toLowerCase())){
			result = true;
		}
		return result;
	}
	/**
	 * Looks up the property whose description - the displayed text - starts with the specified string.
	 * This is not case-sensitive
	 * @param o
	 * @param n
	 * @return
	 */
	public PropertyKey getPropertyKey(PropertyOwner o, String n){
		PropertyKey key = null;
		for(PropertyKey pk : o.getPropertyKeys()){
			//if(pk.name().toLowerCase().startsWith(n.toLowerCase()) || pk.getDescription().toLowerCase().startsWith(n.toLowerCase())){
			if(pk.getDescription().toLowerCase().startsWith(n.toLowerCase())){
				key = pk;
				break;
			}
		}
		if(key == null){
			System.out.println("PropertyManager.getProperty can't find property with name \"" + n + "\" in "+o.getClass().getSimpleName());
			throw new PropertyNotFoundException(n);
		}
		return key;
		
	}
	public List<OpticsObject> getObjectByName(OpticsObject o, String n){
		ArrayList<OpticsObject> result = new ArrayList<OpticsObject>();
		//if this object matches the name then make sure we return it
		if(isNamedObject(o, n)){
			result.add(o);
		} 
		//check all children of groups too
		if(o instanceof OpticsObjectGroup){
			OpticsObjectGroup g = (OpticsObjectGroup)o;
			for(OpticsObject ot : g.getObjects()){
				result.addAll(getObjectByName(ot,n));
			}
		
		}
		
		return result;
	}
	/**
	 * Finds the project that directly owns the specified property owner
	 * @param po
	 * @return
	 */
	public OpticsProject getProject(PropertyOwner po){
		OpticsProject result = null;
		int bestProjLevel = Integer.MAX_VALUE;
//		if(m_projects.size() > 2){
//			System.out.println("PropertyManager.getProject more than one project: "+m_projects.size());
//		}
		for(OpticsProject p : m_projects){
			int i = isInObject(po, p.getOpticsObject(),0);
			if(i >= 0){
				if(i < bestProjLevel){
					result = p;
					bestProjLevel = i;
				}
			}
		}
//		if(result == null){
//			throw new RuntimeException("Can't find project containing "+po+" out of "+m_projects.size()+" projects.");
//		}
		return result;
	}
	/**
	 * used to find which project an object or positioner is in.
	 * @param theObject - the object to find. 
	 * @param theObjectToLookIn - the object to look in
	 * @param level - used to count levels of recursion. First call should set this to zero. Will return -1 if not found.
	 * @return
	 */
	protected int isInObject(PropertyOwner theObject, OpticsObject theObjectToLookIn, int level){
		int result = -1;
		if(theObject.isSame(theObjectToLookIn)){
			result = level;
		} else if(theObjectToLookIn instanceof OpticsObjectCombo){
			OpticsObjectCombo ooc = (OpticsObjectCombo)theObjectToLookIn;
			for(OpticsObject ot : ooc.getObjects()){
				result = isInObject(theObject, ot, level + 1);
				if(result >= 0){
					break;
				}
			}
		}
			
		
		
		return result;
	}
	
	@Override
	public void propertyUpdated(PropertyUpdatedEvent e) {
		//find interested objects
		List<PropertyOwnerLink> ls = m_interestList.getLinksToOwnersInterestedIn(e.getPropertyOwnerSource(), e.getPropertyKey());
		
		for(PropertyOwnerLink pol : ls){
			//pol.getInterestedOwner().setProperty(pol.getInterestedPropertyKey(), pol.getInterestedOwner().getProperty(pol.getInterestedPropertyKey()).getDefiningString());
			updateEquation(pol.getInterestedOwner(), pol.getInterestedPropertyKey(), false);
			
		}
	}
	public void updateAllInterests(){
		m_interestList.updateAllInterests();
	}
	/**
	 * returns a relative path string of this project to the location of the specified project
	 * @param p
	 * @return
	 */
	public static String getRelativePath(PropertyOwner owner, File file) {
		if(file == null){
			return "";
		}
		OpticsProject p = PropertyManager.getInstance().getProject(owner);
		URI fp = file.toURI();
		URI rp;
		URI pp;
		if(p != null && !p.getFileName().equals("")){
			pp = p.getFile().getParentFile().toURI();
			rp = pp.relativize(fp);
		} else {
			rp = fp;
		
		}
		
		
		return rp.toString();
	}
	/**
	 * loads the project specified by the filename, and assumes a relative location with respect to relativeTo parameter
	 * @param filename
	 * @param relativeTo
	 * @return
	 */
	public static File loadRelative(File relativeTo, String filename){
		File file = null;
		if(relativeTo != null){
			//Path pp = (new File(relativeTo.getFileName())).getParentFile().toPath();
			URI uri = null;
			try {
				uri = new URI(filename);
				uri = relativeTo.toURI().resolve(uri);
				file = new File(uri);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

			
		}
		
		
		return file;
	}
	public void deferParsing(OpticsProject p){
		m_notDeferredProjects.add(p);
	}
	
	public void updateDeferred(OpticsProject p){
		undeferParsing(p);
		updateEquations(p, true);
		
	}
	public void undeferParsing(OpticsProject p) {
		
		if(p != null){
			m_notDeferredProjects.addIfAbsent(p);
		}
		
	}
	private boolean isDeferred(OpticsProject p){
		
		return !m_notDeferredProjects.contains(p);
	}
	public boolean updateEquations(OpticsProject proj, boolean onlyDeferred){
		return updateEquations(proj.getOpticsObject(), onlyDeferred);
	}
	/**
	 * cause this owner to recalc a specific equation. Should just quietly ignore if key does not refer to an equation property
	 * @param key
	 * @returns true if EquationProperties was able to be calculated and did not have a Value of NaN.
	 */
	public boolean updateEquations(PropertyOwner owner, boolean onlyDeferred) {
		boolean result = true;
		for(PropertyKey key : owner.getPropertyKeys()){
			result &= updateEquation(owner, key, onlyDeferred);
		}
		if(owner instanceof OpticsObject){
			result &= updateEquations(((OpticsObject) owner).getPositioner(), onlyDeferred);
		}
		if(owner instanceof OpticsObjectCombo){
			for(OpticsObject o : ((OpticsObjectCombo) owner).getObjects()){
				result &= updateEquations(o, onlyDeferred);
			}
		}
		owner.doInternalCalcs();
		return result;
		
	}
	public boolean updateEquation(PropertyOwner owner, PropertyKey key, boolean onlyDeferred) {
		boolean result = true;
		Property p = owner.getProperty(key);
		if(p instanceof EquationProperty){
			EquationProperty ep = (EquationProperty)p;
			if(!onlyDeferred || ep.getValue().isDeferred()){
				ep.calcValue(owner, key);
				if(ep.getValue().isError()){
					result = false;
				}
			}
		} else if(p instanceof EquationArrayProperty){
			EquationArrayProperty eap = (EquationArrayProperty)p;
			if(!onlyDeferred || eap.isDeferred()){
				eap.calcValue(owner, key);
				if(eap.isError()){
					result = false;
				}
			}
		}
		return result;
		
	}
	public void updateEverything() {
		for(OpticsProject p : m_projects){
			updateEquations(p, true);
		}
		
	}
	
}
