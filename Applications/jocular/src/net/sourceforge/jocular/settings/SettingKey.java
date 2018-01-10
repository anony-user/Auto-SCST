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
package net.sourceforge.jocular.settings;

public enum SettingKey {						
				//these ones are for the application
				DEFAULT_FILE_PATH("Default File Path"),
				IMG_WIN_X_LOC("Default Imager Window X Loc"),
				IMG_WIN_Y_LOC("Default Imager Window Y Loc"),
				IMG_WIN_WIDTH("Default Imager Window Width"),
				IMG_WIN_HEIGHT("Default Imager Window Height"),
				MAIN_WIN_X_LOC("Default Main Window X Loc"),
				MAIN_WIN_Y_LOC("Default Main Window Y Loc"),
				MAIN_WIN_WIDTH("Default Main Window Width"),
				MAIN_WIN_HEIGHT("Default Main Window Height"),
				MAIN_WIN_SPLIT_HORIZ("Default Main Window Horizontal Split Location"),
				MAIN_WIN_SPLIT_TREE("Default Main Window Tree Split Location"),
				MAIN_WIN_SPLIT_POS("Default Main Window Positioner Split Location"),
				MAIN_WIN_SPLIT_GEO("Default Main Window Geometry Split Location");
				
	private final String m_description;
	private SettingKey(String description){
		m_description = description;
	}
	private SettingKey(){
		m_description = name().toLowerCase();
	}
	public String toString(){
		return m_description;
	}
	public String getDescription(){
		return m_description;
	}
	
	public static SettingKey getKey(String keyToMatch){
		
		for(SettingKey key : SettingKey.values()){
		
			if(keyToMatch.equals(key.name())){
				return key;
			}
		}
		
		return null;
	}
}