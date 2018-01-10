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
package net.sourceforge.jocular.clipboard;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.properties.ImageProperty;
import net.sourceforge.jocular.properties.PropertyKey;

public class OpticsObjectTransferable implements Transferable, ClipboardOwner {
	private final DataFlavor[] m_flavors;
	private final OpticsObject m_object;
	public OpticsObjectTransferable(OpticsObject o){
		m_object = o;
		
		if(o instanceof Imager){
			DataFlavor[] dfs = {new DataFlavor(OpticsObject.class, "Optics Object"), DataFlavor.imageFlavor};
			m_flavors = dfs;
		} else {
			DataFlavor[] dfs = {new DataFlavor(OpticsObject.class, "Optics Object")};
			m_flavors = dfs;
		}
		
		
		
	}
	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
		Object result = null;
		if(!isDataFlavorSupported(df)){
			throw new UnsupportedFlavorException(df);
		}
		if(df.equals(DataFlavor.imageFlavor)){
			Imager i = (Imager)m_object;
			result = ((ImageProperty)i.getProperty(PropertyKey.IMAGE)).getValue();
		} else {
			result = m_object.makeCopy();
		}
		return result;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return m_flavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor df) {
		boolean result = false;
		for(DataFlavor dft : getTransferDataFlavors()){
			if(dft.equals(df)){
				result = true;
				break;
			}
		}
		return result;
	}

}
