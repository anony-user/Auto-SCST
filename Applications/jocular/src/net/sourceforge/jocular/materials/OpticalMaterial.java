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
package net.sourceforge.jocular.materials;


import java.awt.Color;

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.photons.PhotonInteraction;
import net.sourceforge.jocular.photons.PhotonTrajectory;
import net.sourceforge.jocular.positioners.ObjectPositioner;

/**
 * Interface to capture optical characteristics of matierals.
 * @author kmaccallum
 *
 */
public interface OpticalMaterial {
	

	/**
	 * returns the refractive index of this material at the specified wavelength for a photon polarized in the ordinary direction
	 * @param wavelength
	 * @return
	 */
	double getOrdinaryRefractiveIndex(double wavelength);
	/**
	 * returns the refractive index of this material at the specified wavelength for a photon polarized in the extraordinary direction
	 * @param wavelength
	 * @return
	 */
	double getExtraordinaryRefractiveIndex(double wavelength);
	/**
	 * returns the transmissivity of this material. That is the probability that a photon that travels 1mm is absorbed.
	 * @param wavelength
	 * @param polarizationAngle
	 * @return
	 */
	double getTransmissivity(double wavelength, Vector3D  polarizationAngle, ObjectPositioner materialPositioner);
	
	/**
	 * computes the output photon after traveling through this material.
	 * @param pi TODO
	 * @param pt TODO
	 */
	void interact(PhotonInteraction pi, PhotonTrajectory pt);	
	
	de.jreality.shader.Color getShaderColour();
	Color getColour();
	/**
	 * indicates if this material is optically isotropic. If not then any photon interactions should be resolved onto
	 * the various optical axes as required.
	 * @return
	 */
	public boolean isIsotropic();
}
