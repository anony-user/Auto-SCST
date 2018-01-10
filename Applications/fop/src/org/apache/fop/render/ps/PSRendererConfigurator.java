/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: PSRendererConfigurator.java 627367 2008-02-13 12:03:30Z maxberger $ */

package org.apache.fop.render.ps;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.render.PrintRendererConfigurator;
import org.apache.fop.render.Renderer;
import org.apache.xmlgraphics.ps.PSGenerator;

/**
 * Postscript renderer config 
 */
public class PSRendererConfigurator extends PrintRendererConfigurator {

    /**
     * Default constructor
     * @param userAgent user agent
     */
    public PSRendererConfigurator(FOUserAgent userAgent) {
        super(userAgent);
    }

    /**
     * Configure the PS renderer.
     * @param renderer postscript renderer
     * @throws FOPException fop exception
     */
    public void configure(Renderer renderer) throws FOPException {
        Configuration cfg = super.getRendererConfig(renderer);
        if (cfg != null) {
            super.configure(renderer);

            PSRenderer psRenderer = (PSRenderer)renderer;
            
            psRenderer.setAutoRotateLandscape(
                cfg.getChild("auto-rotate-landscape").getValueAsBoolean(false));
            Configuration child;
            child = cfg.getChild("language-level");
            if (child != null) {
                psRenderer.setLanguageLevel(child.getValueAsInteger(
                        PSGenerator.DEFAULT_LANGUAGE_LEVEL));
            }
            child = cfg.getChild("optimize-resources");
            if (child != null) {
                psRenderer.setOptimizeResources(child.getValueAsBoolean(false));
            }
            psRenderer.setSafeSetPageDevice(
                cfg.getChild("safe-set-page-device").getValueAsBoolean(false));
            psRenderer.setDSCCompliant(
                cfg.getChild("dsc-compliant").getValueAsBoolean(true));
        }
    }
}
