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
package net.sourceforge.jocular.photons;

import java.util.ArrayList;

/**
 * A class for accumulating the shortest and next shortest photon interactions
 * @author tandk
 *
 */
public class InteractionSorter{
	private int m_shortestIndex = 0;
	private int m_nextShortestIndex = -1;
	//private HashMap<OpticsObject, ArrayList<PhotonInteraction>> m_objectInteractions = new HashMap<OpticsObject, ArrayList<PhotonInteraction>>();
	private ArrayList<PhotonInteraction> m_interactions = new ArrayList<PhotonInteraction>();
	public void clear(){
//		m_shortest = null;
//		m_nextShortest = null;
		m_shortestIndex = 0;
		m_nextShortestIndex = -1;
		m_interactions.clear();
	}
	public void add(PhotonInteraction pi){
		//don't add this interaction if it's from the same object we just interacted with and it's not traveled any significant distance
//		if(lastObject == pi.getOpticsObject() && pi.getDistance() < Settings.MIN_DISTANCE){
//		} else {
			
			add(0,pi);
			
		//}
//		if(lastObject == pi.getOpticsObject() && pi.getDistance() < Settings.MIN_DISTANCE){
//			//don't add this because it's most likely where we're from
//		}else if(m_shortest == null){
//			m_shortest = pi;
//		} else if(pi.getDistance() < m_shortest.getDistance()){
//			m_nextShortest = m_shortest;
//			m_shortest = pi;
//		} else if(m_nextShortest == null){
//			m_nextShortest = pi;
//		} else if(pi.getDistance() < m_nextShortest.getDistance()){
//			m_nextShortest = pi;
//		}
	}
	/**
	 * Add a new interaction
	 * @param i
	 * @param pi
	 */
	private void add(int i, PhotonInteraction pi){
//		OpticsObject o = pi.getOpticsObject();
//		ArrayList<PhotonInteraction> list = m_objectInteractions.get(o);
//		if(list == null){
//			list = new ArrayList<PhotonInteraction>();
//			m_objectInteractions.put(o, list);
//		}
		ArrayList<PhotonInteraction> list = m_interactions;
		if(list.size() == 0){
			list.add(pi);
		} else {
			for(int j = 0; j < list.size(); ++j){
				if(list.get(j).getDistance() > pi.getDistance()){
					list.add(j,pi);
					break;
				} else if(j == list.size() - 1){
					list.add(pi);
					break;
				}
			
			}
		}
	}
	/**
	 * computes the shortest and next shortest interactions. This should be done prior to getting the shortest or next shortest.
	 * @param lastObject
	 */
	public void sort(PhotonInteraction lastInteraction){
		m_shortestIndex = 0;
		for(int i = 0; i < m_interactions.size(); ++i){
			
			if(m_interactions.get(i).isInteractingObject(lastInteraction)){
				//only stop looking if the distance to the new interaction with the last object is beyond the min distance
				if(m_interactions.get(i).getDistance() >= 1e-9){
			
					break;
				} else {
					m_shortestIndex = i+1;
				}
				
			} else {
				break;
			}
		}
		
		//find next shortest that is not interacting with the same object
		m_nextShortestIndex = m_shortestIndex;
		if(m_interactions.size() <= m_shortestIndex){
			System.out.println("InteractionSorter.sort: There were no interactions.");
			return;
		}
		while(m_interactions.get(m_shortestIndex).getToObject() == m_interactions.get(m_nextShortestIndex).getToObject()){
			++m_nextShortestIndex;
			if(m_nextShortestIndex >= m_interactions.size()){
				m_nextShortestIndex = m_shortestIndex;
				break;
			}
		}
		
			
		
		
	}
	/**
	 * finds the closest interaction
	 * @param lastObject the last object to interact
	 * @return
	 */
	public PhotonInteraction getShortest(){
//		if(m_interactions.size() > 0){
//			return m_interactions.get(0);
//		} else {
//			return null;
//		}
//		for(OpticsObject o : m_objectInteractions.keySet()){
//			
//		}
		//first find closest interaction with the object we're in
		if(m_interactions.size() <= m_shortestIndex){
			return null;
		}
		return m_interactions.get(m_shortestIndex);
	}
	/**
	 * Gets not the closest interaction but the next closest interactin to the origin of the photon used for computation
	 * @return
	 */
	public PhotonInteraction getNextShortest(){
//		if(m_interactions.size() > 1){
//			return m_interactions.get(1);
//		} else {
//			return null;
//		}
		
		//return m_nextShortest;
		if(m_interactions.size() <= m_nextShortestIndex){
			return null;
		}
		return m_interactions.get(m_nextShortestIndex);
	}
	/**
	 * returns the difference in distance between the first two interactions.
	 * This is helpful to determine if we're leaving one object and entering another right afterwards.
	 * @return
	 */
	public double getDistanceBetweenShortests(){
		if(m_interactions.size() > m_nextShortestIndex){
			return m_interactions.get(m_nextShortestIndex).getDistance() -m_interactions.get(m_shortestIndex).getDistance() ;
//		if(m_shortest != null && m_nextShortest != null){
//			return m_nextShortest.getDistance() - m_shortest.getDistance();
		} else {
			return Double.POSITIVE_INFINITY;
		}
	}
	public PhotonInteraction getFirstInteractionWithObject(PhotonInteraction last){
		
		PhotonInteraction result = null;
		for(PhotonInteraction pi : m_interactions){
			if(pi.isInteractingObject(last)){
				result = pi;
				break;
			}
		}
		return result;
	}
}