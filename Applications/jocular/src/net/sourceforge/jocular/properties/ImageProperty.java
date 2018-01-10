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
package net.sourceforge.jocular.properties;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

public class ImageProperty implements Property<BufferedImage> {
	private final BufferedImage m_image;

	public ImageProperty(){
		m_image = new BufferedImage(10,10, BufferedImage.TYPE_BYTE_BINARY);
		
	}
	public ImageProperty(BufferedImage i){
		m_image = i;
		
	}
	public ImageProperty(String s){
		byte[] bytes = new byte[0];
		try {
			bytes = DatatypeConverter.parseBase64Binary(s);
		} catch(Exception e){
			//System.out.println("ImageProperty constructor failed to create image. "+s.length());
			throw new RuntimeException("ImageProperty constructor failed to create image. "+s.length());
		}

		
		
		InputStream is = new ByteArrayInputStream(bytes);
		BufferedImage i = null;
		try {
			i = ImageIO.read(is);
		} catch(IOException e){
			System.out.println("ImageProperty constructor failed to create image.");

		}
		m_image = i;
		
		
	}

	protected boolean isBlackAndWhite(BufferedImage image){
		
		boolean result;
		if(image == null){
			return false;
		}
		switch(image.getType()){
		case BufferedImage.TYPE_3BYTE_BGR:
		case BufferedImage.TYPE_4BYTE_ABGR:
		case BufferedImage.TYPE_4BYTE_ABGR_PRE:
		case BufferedImage.TYPE_BYTE_INDEXED:
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_INT_ARGB_PRE:
		case BufferedImage.TYPE_INT_BGR:
		case BufferedImage.TYPE_INT_RGB:
		case BufferedImage.TYPE_USHORT_555_RGB:
		case BufferedImage.TYPE_USHORT_565_RGB:
			result = false;
			break;

		default:
		case BufferedImage.TYPE_CUSTOM://not sure what this is
		case BufferedImage.TYPE_BYTE_BINARY:
		case BufferedImage.TYPE_BYTE_GRAY:
		case BufferedImage.TYPE_USHORT_GRAY:
			result = true;
			 break;
		}
		return result;
	}

	@Override
	public String getDefiningString() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(m_image, "png", baos);
		} catch(IOException e){
			System.out.println("ImageProperty.getDefiningString error creating string.");
		}
		byte[] bs = baos.toByteArray();
		String result = DatatypeConverter.printBase64Binary(bs);
		return result;
		
	}

	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);

	}

	@Override
	public BufferedImage getValue() {
		return m_image;
	}
	@Override
	public String toString(){
		return getClass().getSimpleName();
	}

}
