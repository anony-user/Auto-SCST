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


public interface Unit {
	public enum UnitType {NONE, LENGTH, ANGLE, MASS, TIME, MULTIPLE, TEMPERATURE};
	public BaseUnit getBaseUnit(int i);
	public double getBaseUnitPower(int i);
	public int getBaseUnitCount();
	/**
	 * @return the factor to multiply this unit by to convert to the base units 
	 */
	//public double getFactor();
	public double convertToBaseUnitValue(double v);
	public double convertFromBaseUnitValue(double v);
	public UnitType getType();
	public String getName();
	/**
	 * 
	 * @return the abreviation that is most often used to represent this unit
	 */
	public String getPreferredShortName();
	/**
	 * 
	 * @param u
	 * @return - true if base units match or if either object are UNITLESS, else false
	 */
	public boolean isMatchOrUnitless(Unit u);
	public BaseUnit[] getBaseUnits();
	public double[] getBaseUnitPowers();
	public boolean isUnitless();
}
