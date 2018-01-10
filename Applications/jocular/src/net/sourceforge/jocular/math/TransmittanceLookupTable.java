package net.sourceforge.jocular.math;

public class TransmittanceLookupTable implements FunctionOfX {
	private final FunctionOfX m_lookup;
	private final double m_minW;
	private final double m_maxW;
	/**
	 * Assumes that wavelength data is in increasing value.
	 * @param ws
	 * @param wavelengthMultiplier
	 * @param thickness
	 * @param ts
	 * @param transmittanceMultiplier
	 * @param totalNotInternal
	 * @param refractiveIndex
	 */
	public TransmittanceLookupTable(double[] ws, double wavelengthMultiplier, double thickness, double[] ts, double transmittanceMultiplier, boolean totalNotInternal, FunctionOfX refractiveIndex){
		if(ts.length != ws.length){
			throw new RuntimeException("Arrays are not the same length.");
		}
		int n = ws.length;
		double[] ws2 = new double[n + 2];
		double[] ts2 = new double[n + 2];
		
		for(int i = 0; i < n; ++i){
			double w = ws[i]*wavelengthMultiplier;
			double t = ts[i]*transmittanceMultiplier;
			if(t > 1.0){
				t = 1.0;
			} else if(t < 0){
				t = 0;
			}
			if(totalNotInternal){
				double index = refractiveIndex.getValue(w);
				t /= 2*index/(index*index + 1);
			}
			t = Math.pow(t, 1e-3/thickness);
			ws2[i + 1] = w;
			ts2[i + 1] = t;
			
			
		}
		m_maxW = ws2[n];
		m_minW = ws2[1];
		ws2[0] = ws2[1] - (m_maxW - m_minW)/((double)n);
		ws2[n + 1] = ws2[n] + (m_maxW - m_minW)/((double)n);
		ts2[0] = 0;
		ts2[n + 1] = 0;
		
		m_lookup = new LookupTable(ws2, ts2);
		
	}

	@Override
	public double getValue(double x) {
		double result;
		if(x < m_minW){
			result = 0.0;
		} else if(x > m_maxW){
			result = 0.0;
		} else {
			result = m_lookup.getValue(x);
		}
		return result;
	}
	
}
