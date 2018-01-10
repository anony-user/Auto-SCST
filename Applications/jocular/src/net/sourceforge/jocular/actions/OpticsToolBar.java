/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum, Bryan Matthews
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

import javax.swing.Box;
import javax.swing.JToolBar;

import net.sourceforge.jocular.Jocular;

@SuppressWarnings("serial")
public class OpticsToolBar extends JToolBar {
 
	public OpticsToolBar(Jocular app){
		add(OpticsAction.NEW);
		add(OpticsAction.LOAD);
		add(OpticsAction.SAVE);
		add(OpticsAction.UNDO);
		add(OpticsAction.REDO);
		add(OpticsAction.ZOOM_IN);
		add(OpticsAction.ZOOM_OUT);
		add(OpticsAction.OPEN_IMAGER_WINDOW);
		add(OpticsAction.CALC_DISPLAY_PHOTONS);
		add(OpticsAction.CALC_PHOTONS);
		add(OpticsAction.SET_VIEW_PLANE_TO_ZX);
		add(OpticsAction.SET_VIEW_PLANE_TO_ZY);
		add(OpticsAction.SET_VIEW_PLANE_TO_XY);
		add(OpticsAction.SET_CLIP_PLANE_TO_ZX);
		add(OpticsAction.SET_CLIP_PLANE_TO_ZY);
		add(OpticsAction.SET_CLIP_PLANE_TO_XY);
		add(OpticsAction.SET_CLIP_PLANE_TO_NONE);
		//add(OpticsAction.TEST_ACTION);
		add(Box.createHorizontalGlue());
		add(OpticsAction.HELP);
		
	}
}
