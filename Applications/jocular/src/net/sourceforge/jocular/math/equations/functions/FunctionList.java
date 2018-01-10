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
package net.sourceforge.jocular.math.equations.functions;

import java.util.HashMap;

public class FunctionList {
	private HashMap<String, Function> m_functionMap = new HashMap<String, Function>();
	private static final FunctionList FUNCION_LIST = new FunctionList();
	public static FunctionList getFunctionList(){
		return FUNCION_LIST;
	}
	private FunctionList(){
		add("addition",new AdditionFunction());
		add("negation", new NegationFunction());
		add("-", new NegationFunction());
		add("subtraction", new SubtractionFunction());
		add("multiplication", new MultiplicationFunction());
		add("sqrt", new SquareRootFunction());
		add("sin", new SineFunction());
		add("cos", new CosineFunction());
		add("tan", new TangentFunction());
		add("invert", new InvertFunction());
		add("division", new DivisionFunction());
		add("ln", new NaturalLogFunction());
		add("k", new PlanckConstantFunction());
		add("c", new CFunction());
		add("e", new EFunction());
		add("pi", new PiFunction());
		add("asin", new ArcSineFunction());
		add("acos", new ArcCosineFunction());
		add("atan", new ArcTangentFunction());
		add("atan2", new ArcTangent2Function());
		
	}
	protected void add(String name, Function f){
		m_functionMap.put(name, f);
	}
	public Function get(String name){
		String n = name.trim().toLowerCase();
		Function result = m_functionMap.get(n);
		if(result == null){
			throw new RuntimeException("Function \"" + n +"\" cannot be found in list.");
		}
		return result;
	}
}
