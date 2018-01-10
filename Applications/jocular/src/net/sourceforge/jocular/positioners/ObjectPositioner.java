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
package net.sourceforge.jocular.positioners;

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.OpticsID;
import net.sourceforge.jocular.project.OpticsPositionerElement;
import net.sourceforge.jocular.project.OpticsPositionerVisitor;
import net.sourceforge.jocular.properties.PropertyOwner;

/**
 * Used to provide position and orientation to OpticsObjects and OpticsSurfaces.
 * For instance, one could be developed to position on the Z axis and then one could be developed to offset along that axis
 * @author kmaccallum
 *
 */
public interface ObjectPositioner extends PropertyOwner, OpticsPositionerElement {
	public Vector3D getOrigin();
	public Vector3D getDirection();
	public Vector3D getTransDirection();
	public Vector3D getOrthoDirection();
	/**
	 * Sets the reference positioner if there is one
	 * @param op
	 */
	//public void setPositioner(ObjectPositioner op);
	//public ObjectPositioner getPositioner();
	public ObjectPositioner makeCopy();
	public void copyProperties(PropertyOwner o);
	
	public OpticsID getID();
	public void setID(OpticsID id);
	//public OpticsID getLinkID();
	
	public Vector3D transform(Vector3D p);
	public void update(ObjectPositioner parentPositioner);
	
	void accept(OpticsPositionerVisitor v);	
}
