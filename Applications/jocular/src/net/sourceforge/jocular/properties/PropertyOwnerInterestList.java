package net.sourceforge.jocular.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PropertyOwnerInterestList {
	public class PropertyOwnerLink {
		PropertyOwner m_interestedOwner;
		PropertyOwner m_interestingOwner;
		PropertyKey m_interestedKey;
		PropertyKey m_interestingKey;
		public PropertyOwnerLink(PropertyOwner interestedOwner, PropertyKey interestedKey, PropertyOwner interestingOwner, PropertyKey interestingKey){
			m_interestedOwner = interestedOwner;
			m_interestedKey = interestedKey;
			m_interestingOwner = interestingOwner;
			m_interestingKey = interestingKey;
		}
		public PropertyOwner getInterestedOwner(){
			return m_interestedOwner;
		}
		public PropertyOwner getInterestingOwner(){
			return m_interestingOwner;
		}
		public PropertyKey getInterestedPropertyKey(){
			return m_interestedKey;
		}
		public PropertyKey getInterestingPropertyKey(){
			return m_interestingKey;
		}
		public String toString(){
			Property p1 = m_interestedOwner.getProperty(PropertyKey.NAME);
			String sp1 = "null";
			if(p1 != null){
				sp1 = p1.getDefiningString();
			}
			Property p2 = m_interestingOwner.getProperty(PropertyKey.NAME);
			String sp2 = "null";
			if(p2 != null){
				sp2 = p2.getDefiningString();
			}
			return "PropertyOwnerLink: \""+sp1+"\" ("+m_interestedOwner+") > "+m_interestedKey+" is interested in \""+sp2+"\" ("+m_interestingOwner+") > "+m_interestingKey;
		}
		public boolean equals(PropertyOwnerLink link){
			boolean result = true;
			if(link.getInterestedOwner() != getInterestedOwner()){
				result = false;
			}
			if(link.getInterestedPropertyKey() != getInterestedPropertyKey()){
				result = false;
			}
			if(link.getInterestingOwner() != getInterestingOwner()){
				result = false;
			}
			if(link.getInterestingPropertyKey() != getInterestingPropertyKey()){
				result = false;
			}
			return result;
		}
	}
	private List<PropertyOwnerLink> m_linkList = new CopyOnWriteArrayList<PropertyOwnerLink>();
	

	public List<PropertyOwnerLink> getLinksToOwnersInterestedIn(PropertyOwner o, PropertyKey k){
		List<PropertyOwnerLink> result = new ArrayList<PropertyOwnerLink>();
		//System.out.println("PropertyOwnerInterestList.getLinksToOwnersInterestedIn "+o+">"+k);
		for(PropertyOwnerLink ool : m_linkList){
			if(ool.getInterestingPropertyKey() == PropertyKey.FRONT_RADIUS){
				//System.out.println("      "+ool.getInterestingOwner());
			}
			if(ool.getInterestingOwner() == o && ool.getInterestingPropertyKey() == k){
				result.add(ool);
			}
		}	
		return result;
	}
	public List<PropertyOwner> getOwnersInterestingTo(PropertyOwner o){
		List<PropertyOwner> result = new ArrayList<PropertyOwner>();
		for(PropertyOwnerLink ool : m_linkList){
			if(ool.getInterestedOwner() == o){
				result.add(ool.getInterestingOwner());
				break;
			}
		}
		return result;
	}
	public void addOwnerInterest(PropertyOwner interestedOwner, PropertyKey interestedKey, PropertyOwner interestingOwner, PropertyKey interestingKey){
		m_linkList.add(new PropertyOwnerLink(interestedOwner, interestedKey, interestingOwner, interestingKey));
	}
	public PropertyOwnerLink findFirstLinkOfInterest(PropertyOwner interestedOwner){
		PropertyOwnerLink result = null;
		for(PropertyOwnerLink ool : m_linkList){
			if(ool.getInterestedOwner() == interestedOwner){
				result = ool;
				break;
			}
		}
		return result;
	}
	public List<PropertyOwnerLink> getInterestingLinks(PropertyOwner interestingOwner, PropertyKey interestingKey){
		List<PropertyOwnerLink> result = new ArrayList<PropertyOwnerLink>();
		for(PropertyOwnerLink link : m_linkList){
			if(link.getInterestingOwner() == interestingOwner && link.getInterestingPropertyKey() == interestingKey){
				result.add(link);
			}
		}
		return result;
	}
	public List<PropertyOwnerLink> getLinksOfInterest(PropertyOwner interestedOwner, PropertyKey interestedKey){
		List<PropertyOwnerLink> result = new ArrayList<PropertyOwnerLink>();
		for(PropertyOwnerLink link : m_linkList){
			if(link.getInterestedOwner() == interestedOwner && link.getInterestedPropertyKey() == interestedKey){
				result.add(link);
			}
		}
		return result;
	}
	public void removeOwnerInterest(PropertyOwner interestedOwner, PropertyKey interestedKey){
		List<PropertyOwnerLink> links = getLinksOfInterest(interestedOwner, interestedKey);
		for(PropertyOwnerLink link : links){
			m_linkList.remove(link);
		}
	}
	public boolean doesLinkExist(PropertyOwner interestedOwner, PropertyKey interestedKey, PropertyOwner interestingOwner, PropertyKey interestingKey){
		boolean result = false;
		PropertyOwnerLink testLink = new PropertyOwnerLink(interestedOwner, interestedKey, interestingOwner, interestingKey);
		for(PropertyOwnerLink l : m_linkList){
			if(l.equals(testLink)){
				result = true;
				break;
			}
		}
		return result;
	}
	public PropertyOwnerLink checkForCircularReference(PropertyOwner interestedOwner, PropertyKey interestedKey, PropertyOwner interestingOwner, PropertyKey interestingKey){
		PropertyOwnerLink result = null;
		
		//
		////first be sure there's not a direct reference.
		if(interestingOwner == interestedOwner && interestingKey == interestedKey){
			//if so then make a new link to return
			result = new PropertyOwnerLink(interestedOwner, interestedKey, interestingOwner, interestingKey);
		} else {
			//now find links where interesting owner is interested in other stuff
			List<PropertyOwnerLink> pols = getLinksOfInterest(interestingOwner, interestingKey);
			for(PropertyOwnerLink pol : pols){
				if(pol.getInterestingOwner() == interestedOwner && pol.getInterestingPropertyKey() == interestedKey){
					result = pol;
					break;
				} else {
					result = checkForCircularReference(interestedOwner, interestedKey, pol.getInterestingOwner(), pol.getInterestingPropertyKey());
					if(result != null){
						break;
					}
				}
			}
		}
		//System.out.println("PropertyOwnerInterestList.checkForCircularReference found?"+result);
		return result;
	}
	public String toString(){
		String result = "PropertyOwnerInterestList: \n";
		for(PropertyOwnerLink link : m_linkList){
			result += "     "+link + "\n";
		}
		return result;
	}
	public void updateAllInterests() {
		for(PropertyOwnerLink link : m_linkList){
			PropertyManager.getInstance().updateEquations(link.getInterestingOwner(), false);
		}
		
	}
	
}
