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
/**
 * A handy list of positioners that can be used by the user. Contains a name, a tooltip and can be used to make a new instance of that type of object.
 * @author Kenneth MacCallum
 *
 */
public enum ObjectPositionerKey {         AXIS("Axis",new AxisPositioner(), "Positions on an axis(X, Y, Z) at the origin."),
							//Z_AXIS("Z Axis",new AxisPositioner("Z"), "Positions on the ZAxis at the origin"),
							OFFSET("Offset", new OffsetPositioner(), "Positions this object a distance along the direction of the parent positioner."),
							REVERSE_OFFSET("Offset and reverse", new ReverseOffsetPositioner(), "Positions this object a distance along the direction of the parent positioner and reverses the direction."),
							TRANSVERSE_OFFSET("Transverse Offset", new TransverseOffsetPositioner(), "Offsets the object transversely a specified distance off the direction of the parent positioner."),
							ANGLE("Angle", new AnglePositioner(), "Rotates the object about one of its axes"),
							TRANSLATE_ROTATE("Translate & Rotate", new TranslateRotatePositioner(), "Translates the object in 3-space then rotates about 3 axes");
	private String name;
	private ObjectPositioner prototype;
	private String tooltip;
	private ObjectPositionerKey(String n, ObjectPositioner o, String t){
		name = n;
		prototype = o;
		tooltip = t;
	}
	public String getName(){
		return name;
	}
	public String toString(){
		return name;
	}
	public ObjectPositioner getNewPositioner(){
		return prototype.makeCopy();
	}
	public String getToolTipText(){
		return tooltip;
	}
	public static ObjectPositionerKey getKey(ObjectPositioner p){
		ObjectPositionerKey result = null;
		for(ObjectPositionerKey k : ObjectPositionerKey.values()){
			if(k.prototype.getClass() == p.getClass()){
				result = k;
				break;
			}
		}
		return result;
		
	}
	
	public static String getKeyName(ObjectPositioner p){
		String result = null;
		for(ObjectPositionerKey k : ObjectPositionerKey.values()){
			if(k.prototype.getClass() == p.getClass()){
				result = k.name();
				break;
			}
		}
		return result;
		
	}
	
	public static ObjectPositionerKey getKey(String positionerToMatch){
		
		for(ObjectPositionerKey key : ObjectPositionerKey.values()){
		
			if(positionerToMatch.equals(key.name())){
				return key;
			}
		}
		
		return null;
	}

}