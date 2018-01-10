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
package net.sourceforge.jocular.project;

import net.sourceforge.jocular.autofocus.AutofocusSensor;
import net.sourceforge.jocular.imager.Imager;
import net.sourceforge.jocular.objects.CylindricalSurface;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsPart;
import net.sourceforge.jocular.objects.PlanoAsphericLens;
import net.sourceforge.jocular.objects.ProjectRootGroup;
import net.sourceforge.jocular.objects.SimpleAperture;
import net.sourceforge.jocular.objects.SpectroPhotometer;
import net.sourceforge.jocular.objects.SphericalLens;
import net.sourceforge.jocular.objects.SphericalSurface;
import net.sourceforge.jocular.objects.TriangularPrism;
import net.sourceforge.jocular.sources.HemiPointSource;
import net.sourceforge.jocular.sources.ImageSource;
import net.sourceforge.jocular.splines.ExtrudedSpline;
import net.sourceforge.jocular.splines.RotatedSpline;

public interface OpticsObjectVisitor {
	public void visit(Imager v);
	public void visit(SphericalSurface v);
	public void visit(SphericalLens v);
	public void visit(HemiPointSource v);
	public void visit(CylindricalSurface v);
	public void visit(OpticsObjectGroup v);
	public void visit(ProjectRootGroup v);
	public void visit(TriangularPrism v);
	public void visit(ImageSource v);
	public void visit(SimpleAperture v);
	public void visit(SpectroPhotometer v);
	public void visit(AutofocusSensor v);
	public void visit(RotatedSpline v);
	public void visit(PlanoAsphericLens v);
	public void visit(ExtrudedSpline v);
	public void visit(OpticsPart v);
}

