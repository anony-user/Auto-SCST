/*******************************************************************************
 * Copyright (c) 2013,Bryan Matthews
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

import java.util.Random;

import org.xml.sax.Attributes;

public class OpticsID {
	
	private static String ID_STRING = "id";
	private static Random rand = new Random();

	public final int id;
	
	public OpticsID(String s){
		id = Integer.parseInt(s);
	}
	
	public OpticsID(Attributes attr){
		
		id = parseID(attr);
	}
	
	public OpticsID(){		
		
		id = rand.nextInt();
	}
	
	private int parseID(Attributes attr){
		
		int value = -1;
		
		int attrLength = attr.getLength();
		
		for(int i=0; i < attrLength; i++)
		{
			if(attr.getQName(i) == ID_STRING){
				value = Integer.parseInt(attr.getValue(i));
			}
		}
		
		return value;
	}

	//TODO: Replace with an actual comparison of the integers
	public boolean isMatch(OpticsID matchId){
				
		if(id == matchId.id)
			return true;
		
		return false;
	}
	
	public String toString(){
		return String.valueOf(id);
	}

	public String toHashString() {
		return "H"+Integer.toHexString(id).toUpperCase();
	}

	
}
