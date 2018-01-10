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

import net.sourceforge.jocular.materials.OpticalMaterial;
import net.sourceforge.jocular.photons.InteractionSorter;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.positioners.ObjectPositioner;
import net.sourceforge.jocular.project.OpticsObjectElement;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.properties.PropertyOwner;
import net.sourceforge.jocular.properties.PropertyUpdatedListener;

public interface OpticsObject extends PropertyOwner, OpticsObjectElement{
	/**
	 * Computes all possible interactions with the current photon path of the specified trajectory
	 * The decision of which interaction to choose will be done by the interaction sorter and the photon wrangler
	 * @param pt
	 * @param is
	 */
	public void getPossibleInteraction(PhotonTrajectory pt, InteractionSorter is);
	/**
	 * Interact the present photon of the photon trajectory with this object
	 * @param pi
	 * @param pt
	 */
	public void interact(PhotonInteraction pi, PhotonTrajectory pt);
	public void setPositioner(ObjectPositioner op);
	public void updatePositioner(ObjectPositioner parentPositioner);
	public ObjectPositioner getPositioner();
	/**
	 * this allows the optical medium outside this object to be changed
	 * @param om
	 */
	//public void setOutsideMaterial(OpticalMaterial om);
	/**
	 * makes a copy of this object. This is expected to be quite deep. this will be used for copying in the UI among other things
	 * @return
	 */
	public OpticsObject makeCopy();

	public boolean isSelected();
	public void setSelected(boolean s);
	public boolean isSuppressed();
	public OpticsID getID();
	public void setID(OpticsID id);
	public OpticsID getLinkID();
	
	void accept(OpticsObjectVisitor v);
	/**
	 * @return the material this object is made of
	 */
	public OpticalMaterial getMaterial();
//	/**
//	 * cause object to recalculate the values of it's properties. So far
//	 * the only properties that could possibly change their value are equation properties.
//	 */
//	public void recalcProperties();
	public void addPropertyUpdatedListener(PropertyUpdatedListener listener);
	public String getName();
//	/**
//	 * Adds a row to a property if it happens to be an array property. Otherwise does nothing
//	 * @param key
//	 * @param i
//	 */
//	public void addRowToArrayProperty(PropertyKey key, int i);
//	/**
//	 * Removes a row to a property if it happens to be an array property. Otherwise does nothing
//	 * @param key
//	 * @param i
//	 */
//	public void removeRowFromArrayProperty(PropertyKey key, int i);
}
