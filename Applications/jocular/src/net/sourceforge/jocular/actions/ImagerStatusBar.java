/*******************************************************************************
 * Copyright (c) 2013, Bryan Matthews
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.actions;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


@SuppressWarnings("serial")
public class ImagerStatusBar extends JPanel{
	
	private final static String DEFAULT_DISPLAY = "Wrangler: ";
	private final static String DEFAULT_COUNT = "Cycles: ";
	
	JLabel m_statusLabel;
	JLabel m_countLabel;
	
	int currentCount, totalCount;
	int m_lastCount = 0;
	long m_currentTimeMs;
	long m_startTimeMs;
	double m_rate;
	
	JProgressBar m_progressBar;
	
	public ImagerStatusBar() {  
		super();  
		
		totalCount = 0;
		currentCount = 0;

		setLayout(new BorderLayout(2, 2));
 
		m_statusLabel = new JLabel(DEFAULT_DISPLAY + "Ready"); 
		m_statusLabel.setBorder(BorderFactory.createLoweredBevelBorder()); 
		m_statusLabel.setForeground(Color.black);
		add(BorderLayout.WEST, m_statusLabel); 
		
		m_countLabel = new JLabel(DEFAULT_COUNT + convertCountsToString()); 
		m_countLabel.setBorder(BorderFactory.createLoweredBevelBorder()); 
		m_countLabel.setForeground(Color.black);
		add(BorderLayout.CENTER, m_countLabel);
		
		m_progressBar = new JProgressBar(0, totalCount);
		m_progressBar.setValue(0);
		m_progressBar.setStringPainted(true);
		add(BorderLayout.EAST, m_progressBar);
		
	}
	
	private String convertCountsToString(){
		
		String temp;
		
		temp = Integer.toString(currentCount);
		temp += "/";
		temp += Integer.toString(totalCount);
		temp += ", rate: "+m_rate;
		
		return temp;
	}
	
	public void setStatusToReady(){
		m_statusLabel.setText(DEFAULT_DISPLAY + "Ready");
	}
	
	public void setStatusToRunning(){
		m_statusLabel.setText(DEFAULT_DISPLAY + "Running");
		
	}
	
	public void setTotalCounts(int counts){
		currentCount = 0;
		m_startTimeMs = System.currentTimeMillis();
		totalCount = counts;
		m_progressBar.setMaximum(counts);
	}
	
	public void setCurrentCount(int count){
		m_currentTimeMs = System.currentTimeMillis();
		
		currentCount = count;
		m_rate = Math.floor(1000.0d*((double)(currentCount))/((double)(m_currentTimeMs - m_startTimeMs)));
		
		m_countLabel.setText(DEFAULT_COUNT + convertCountsToString());
		m_progressBar.setValue(count);
	}
}
