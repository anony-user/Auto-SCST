/*******************************************************************************
 * Copyright (c) 2016,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.sources;

import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.positioners.ObjectPositioner;

public class SourceCalcs {
	public static Vector3D calcPhotonDir(ObjectPositioner pos, double maxOrtho, double maxTrans){
		Vector3D dir = pos.getDirection();
		Vector3D trans = pos.getTransDirection();
		Vector3D ortho = pos.getOrthoDirection();
		//Calculate direction of photon
		double a = Math.random()*2*Math.PI;
		//double r = Math.sqrt(random.nextDouble());//square root this to account for increased power density to smaller radii
		double b = Math.random();
		double pTransAngle = b*Math.sin(a)*maxTrans;
		double pOrthoAngle = b*Math.cos(a)*maxOrtho;
		
		
		double c = Math.sqrt(pTransAngle*pTransAngle + pOrthoAngle*pOrthoAngle);
		
		dir = dir.scale(Math.cos(c));
		trans = trans.scale(pTransAngle*Math.sin(c));		
		//ortho = ortho.scale(Math.sin(pAngle));
		
		//trans = trans.scale(Math.cos(pOrthoAngle));
		//dir = dir.scale(Math.cos(pOrthoAngle));
		ortho = ortho.scale(pOrthoAngle*Math.sin(c));

		
		dir = dir.add(trans).add(ortho);
		return dir;
	}
}
