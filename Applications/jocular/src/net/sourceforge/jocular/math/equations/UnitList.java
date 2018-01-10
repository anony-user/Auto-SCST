/*******************************************************************************
 * Copyright (c) 2014,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.math.equations;

import java.util.Collection;
import java.util.HashMap;

public class UnitList {
	HashMap<String, Unit> m_unitMap = new HashMap<String, Unit>();
	private static final UnitList UNIT_LIST = new UnitList();
	private UnitList(){
		m_unitMap.put("m", BaseUnit.METRE);
		m_unitMap.put("mm", new AbstractUnit("millimetre","mm", BaseUnit.METRE, 1e-3){});
		m_unitMap.put("um", new AbstractUnit("micrometre","um", BaseUnit.METRE, 1e-6){});
		m_unitMap.put("nm", new AbstractUnit("nanometre","nm", BaseUnit.METRE, 1e-9){});
		m_unitMap.put("pm", new AbstractUnit("picometre","pm", BaseUnit.METRE, 1e-12){});
		m_unitMap.put("fm", new AbstractUnit("femtometre","fm", BaseUnit.METRE, 1e-15){});
		m_unitMap.put("cm", new AbstractUnit("centimetre","cm", BaseUnit.METRE, 1e-2){});
		m_unitMap.put("km", new AbstractUnit("kilometre","km", BaseUnit.METRE, 1e3){});
		m_unitMap.put("dm", new AbstractUnit("decimetre","dm", BaseUnit.METRE, 1e-1){});
		m_unitMap.put("ft", new AbstractUnit("foot","ft", BaseUnit.METRE, 12.0*25.4e-3){});
		m_unitMap.put("'", m_unitMap.get("ft"));
		m_unitMap.put("in", new AbstractUnit("inch","in", BaseUnit.METRE, 25.4e-3){});
		m_unitMap.put("mil", new AbstractUnit("thousandths of an inch","mil", BaseUnit.METRE, 25.4e-6){});
		m_unitMap.put("thou", m_unitMap.get("mil"));
		m_unitMap.put("\"", m_unitMap.get("in"));
		m_unitMap.put("yd", new AbstractUnit("yards", "yd", BaseUnit.METRE, 25.4e-3*12*3){});
		m_unitMap.put("rad", BaseUnit.RADIAN);
		m_unitMap.put("mrad", new AbstractUnit("milliradian","mrad", BaseUnit.RADIAN, 1e-3){});
		m_unitMap.put("urad", new AbstractUnit("microradian","urad", BaseUnit.RADIAN, 1e-6){});
		m_unitMap.put("deg", new AbstractUnit("degree","°", BaseUnit.RADIAN, 2.0*Math.PI/360){});
		m_unitMap.put("mdeg", new AbstractUnit("millidegree","m°", BaseUnit.RADIAN, 2.0e-3*Math.PI/360){});
		m_unitMap.put("°", m_unitMap.get("deg"));
		m_unitMap.put("grad", new AbstractUnit("gradian","grad", BaseUnit.RADIAN, 2.0*Math.PI/100){});
		m_unitMap.put("kg", BaseUnit.KILOGRAM);
		m_unitMap.put("lb", new AbstractUnit("pound", "lb", BaseUnit.KILOGRAM, 1.0/2.2046){});
		m_unitMap.put("s", BaseUnit.SECOND);
		m_unitMap.put("ms", new AbstractUnit("millisecond", "ms", BaseUnit.SECOND, 1e-3){});
		m_unitMap.put("us", new AbstractUnit("microsecond", "us", BaseUnit.SECOND, 1e-6){});
		m_unitMap.put("ns", new AbstractUnit("nanosecond", "ns", BaseUnit.SECOND, 1e-9){});
		m_unitMap.put("ps", new AbstractUnit("picosecond", "ps", BaseUnit.SECOND, 1e-12){});
		m_unitMap.put("fs", new AbstractUnit("femtosecond", "fs", BaseUnit.SECOND, 1e-15){});
		m_unitMap.put("K", BaseUnit.KELVIN);
		
	}
	public Unit get(String name){
		Unit result = m_unitMap.get(name);
		if(result == null){
			throw new RuntimeException("specified unit \""+name+"\" cannot be found.");
		}
		return result;
	}
	public Collection<String> getUnitNames(){
		return m_unitMap.keySet();
	}
	public static UnitList getUnitList(){
		return UNIT_LIST;
	}
}
