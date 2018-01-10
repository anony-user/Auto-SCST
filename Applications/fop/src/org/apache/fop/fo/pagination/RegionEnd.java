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

/* $Id: RegionEnd.java 557337 2007-07-18 17:37:14Z adelmelle $ */

package org.apache.fop.fo.pagination;

// Java
import java.awt.Rectangle;

import org.apache.fop.datatypes.FODimension;
import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.datatypes.SimplePercentBaseContext;
import org.apache.fop.fo.FONode;
// FOP

/**
 * The fo:region-end element.
 */
public class RegionEnd extends RegionSE {
    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public RegionEnd(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc} 
     */
    public Rectangle getViewportRectangle (FODimension reldims, SimplePageMaster spm) {
        /* Special rules apply to resolving extent as values are resolved relative 
         * to the page size and reference orientation.
         */
        SimplePercentBaseContext pageWidthContext;
        SimplePercentBaseContext pageHeightContext;
        if (spm.getReferenceOrientation() % 180 == 0) {
            pageWidthContext = new SimplePercentBaseContext(null, 
                                                            LengthBase.CUSTOM_BASE,
                                                            spm.getPageWidth().getValue());
            pageHeightContext = new SimplePercentBaseContext(null,
                                                             LengthBase.CUSTOM_BASE,
                                                             spm.getPageHeight().getValue());
        } else {
            // invert width and height since top left are rotated by 90 (cl or ccl)
            pageWidthContext = new SimplePercentBaseContext(null, 
                                                            LengthBase.CUSTOM_BASE,
                                                            spm.getPageHeight().getValue());
            pageHeightContext = new SimplePercentBaseContext(null,
                                                             LengthBase.CUSTOM_BASE,
                                                             spm.getPageWidth().getValue());
        }
        SimplePercentBaseContext neighbourContext;
        Rectangle vpRect;
        if (spm.getWritingMode() == EN_LR_TB || spm.getWritingMode() == EN_RL_TB) {
            neighbourContext = pageHeightContext;
            vpRect = new Rectangle(reldims.ipd - getExtent().getValue(pageWidthContext), 0,
                    getExtent().getValue(pageWidthContext), reldims.bpd);
        } else {
            // Rectangle:  x , y (of top left point), width, height
            neighbourContext = pageWidthContext;
            vpRect = new Rectangle(reldims.ipd - getExtent().getValue(pageHeightContext), 0,
                    reldims.bpd, getExtent().getValue(pageHeightContext));
        }
        adjustIPD(vpRect, spm.getWritingMode(), neighbourContext);
        return vpRect;
    }

    /**
     * {@inheritDoc}
     */
    protected String getDefaultRegionName() {
        return "xsl-region-end";
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "region-end";
    }
    
    /**
     * {@inheritDoc}
     */
    public int getNameId() {
        return FO_REGION_END;
    }
}

