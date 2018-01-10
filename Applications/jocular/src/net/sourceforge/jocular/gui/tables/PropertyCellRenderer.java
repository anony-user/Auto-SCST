/*******************************************************************************
 * Copyright (c) 2013, Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.gui.tables;

import java.awt.Color;

import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.jocular.math.equations.UnitedValue;
import net.sourceforge.jocular.properties.AngleProperty;
import net.sourceforge.jocular.properties.BooleanProperty;
import net.sourceforge.jocular.properties.EnumArrayProperty;
import net.sourceforge.jocular.properties.EnumProperty;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.FileProperty;
import net.sourceforge.jocular.properties.ImageProperty;
import net.sourceforge.jocular.properties.IntegerProperty;
import net.sourceforge.jocular.properties.LengthProperty;
import net.sourceforge.jocular.properties.MaterialProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyVisitor;
import net.sourceforge.jocular.properties.StringArrayProperty;
import net.sourceforge.jocular.properties.StringProperty;
import net.sourceforge.jocular.settings.Settings;

//protected class PropertyCellRenderer extends DefaultTableCellRenderer {
//	@Override
//	protected void setValue(Object value) {
//		
//		PropertyCellRendererFactory factory = new PropertyCellRendererFactory();
//		super.setValue(factory.getRendererString((Property<?>) value));			
//		
//	}
//	
//}
public class PropertyCellRenderer extends DefaultTableCellRenderer implements PropertyVisitor {

	String m_result;
	
	
	@Override
	protected void setValue(Object value) {
		if(value == null){
			return;
		}
		Property p = (Property<?>)value;
		p.accept(this);
		super.setValue(m_result);			
		
	}
	
	@Override
	public void visit(AngleProperty v) {
		m_result = v.getDefiningString();
		setBackground(Color.white);

	}

	@Override
	public void visit(BooleanProperty v) {
		m_result = v.getDefiningString();
		setBackground(Color.white);

	}

	@Override
	public void visit(EnumProperty v) {
		m_result = v.getValue().toString();
		setBackground(Color.white);

	}

	@Override
	public void visit(ImageProperty v) {
		m_result = "...";
		setBackground(Color.white);

	}

	@Override
	public void visit(IntegerProperty v) {
		m_result = v.getDefiningString();
		setBackground(Color.white);

	}
	@Override
	public void visit(EquationProperty v) {
		switch(Settings.SETTINGS.getNumberDisplay()){
		case EQUATION:
			m_result = v.getDefiningString();
			break;
		case METRIC:
			m_result = v.getValue().getBaseUnitString();
			break;
		case IMPERIAL:
			m_result = "Imperial view not implemented yet.";
			break;
		}
		if(v.getValue().isError()){
			setBackground(new Color(1.0f, 0.5f, 0.5f));
			setToolTipText(v.getValue().getErrorText());
		} else if(v.getValue().isDeferred()){
			this.setBackground(new Color(1.0f, 0.75f, 0.5f));
		} else if(v.getValue().isSimpleValue()){
			this.setBackground(new Color(0.5f, 1.0f, 0.5f));
		} else {
			this.setBackground(new Color(0.5f, 0.5f, 1.0f));
		}
			

	}
	@Override
	public void visit(LengthProperty v) {
		m_result = v.getDefiningString();
		setBackground(Color.white);

	}

	@Override
	public void visit(MaterialProperty v) {
		m_result = v.getKey().toString();
		setBackground(Color.white);

	}

	@Override
	public void visit(StringProperty v) {
		m_result = v.getDefiningString();
		setBackground(Color.white);

	}
	@Override
	public void visit(FileProperty v) {
		//m_result = v.getDefiningString();
		m_result = v.getValue().getName();
		setBackground(Color.white);

	}
	@Override
	public void visit(StringArrayProperty v) {
		m_result = v.getDefiningString();
		setBackground(Color.white);

	}
	@Override
	public void visit(EnumArrayProperty v) {
		m_result = v.getDefiningString();
		setBackground(Color.white);

	}
	@Override
	public void visit(EquationArrayProperty v) {
		m_result = v.getDefiningString();
		
		String et = "";
		boolean simple = false;
		boolean error = false;
		boolean deferred = false;
		
		for(UnitedValue uv : v.getValue()){
			if(uv.isDeferred()){
				deferred = true;
			}
			if(uv.isSimpleValue()){
				simple = true;
			}
			if(uv.isError()){
				error = true;
				et = uv.getErrorText();
			}
		}

		
		if(error){
			setBackground(new Color(1.0f, 0.5f, 0.5f));
			setToolTipText(et);
		} else if(deferred){
			this.setBackground(new Color(1.0f, 0.75f, 0.5f));
		} else if(simple){
			this.setBackground(new Color(0.5f, 1.0f, 0.5f));
		} else {
			this.setBackground(new Color(0.5f, 0.5f, 1.0f));
		}

	}

}
