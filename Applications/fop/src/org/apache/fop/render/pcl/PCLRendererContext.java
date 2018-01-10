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

/* $Id: PCLRendererContext.java 627367 2008-02-13 12:03:30Z maxberger $ */

package org.apache.fop.render.pcl;

import org.apache.fop.fo.extensions.ExtensionElementMapping;
import org.apache.fop.render.RendererContext;
import org.apache.fop.util.QName;

/**
 * Wrapper on the RendererContext to access the information structure for drawing 
 * the XML document.
 */
public class PCLRendererContext extends RendererContext.RendererContextWrapper {

    /**
     * Wrap the render context to allow easier access to its values.
     *
     * @param context the renderer context
     * @return the PCL-specific renderer context wrapper
     */
    public static PCLRendererContext wrapRendererContext(RendererContext context) {
        PCLRendererContext pcli = new PCLRendererContext(context);
        return pcli;
    }

    /**
     * Main constructor
     * @param context the RendererContent instance
     */
    public PCLRendererContext(RendererContext context) {
        super(context);
    }
    
    /** @return true if the SVG image should be rendered as a bitmap */
    public boolean paintAsBitmap() {
        QName qName = new QName(ExtensionElementMapping.URI, null, "conversion-mode");
        return getForeignAttributes() != null 
             && "bitmap".equalsIgnoreCase((String)getForeignAttributes().get(qName));
    }
    
    /** @return true if clipping is disabled inside the PCLGraphics2D. */
    public boolean isClippingDisabled() {
        QName qName = new QName(ExtensionElementMapping.URI, null, "disable-clipping");
        return getForeignAttributes() != null 
             && "true".equalsIgnoreCase((String)getForeignAttributes().get(qName));
    }

    /**
     * Indicates whether the background should not be erased prior to painting.
     * @return true if the background shouldn't be erased
     */
    public boolean isSourceTransparency() {
        QName qName = new QName(ExtensionElementMapping.URI, null, "source-transparency");
        return getForeignAttributes() != null 
             && "true".equalsIgnoreCase((String)getForeignAttributes().get(qName));
    }
    
    /**
     * Indicates whether an RGB canvas should be used rather than one with grayscales.
     * This can be used to work around limitations of Apache Batik if you get error while
     * processing SVG graphics. Note, however, that RGB mode will use more memory.
     * @return true if an EGB canvas should be used
     */
    public boolean isColorCanvas() {
        QName qName = new QName(ExtensionElementMapping.URI, null, "color-canvas");
        Boolean prop = (Boolean)context.getProperty(PCLRendererContextConstants.PCL_COLOR_CANVAS);
        return Boolean.TRUE.equals(prop) 
            || (getForeignAttributes() != null 
                    && "true".equalsIgnoreCase((String)getForeignAttributes().get(qName)));
    }

}