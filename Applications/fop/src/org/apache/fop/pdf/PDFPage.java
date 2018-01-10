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

/* $Id: PDFPage.java 618626 2008-02-05 12:34:56Z jeremias $ */
 
package org.apache.fop.pdf;

import java.awt.geom.Rectangle2D;

/**
 * Class representing a /Page object.
 * <p>
 * There is one of these for every page in a PDF document. The object
 * specifies the dimensions of the page and references a /Resources
 * object, a contents stream and the page's parent in the page
 * hierarchy.
 */
public class PDFPage extends PDFResourceContext {

    /** the page index (zero-based) */
    protected int pageIndex;
    
    /**
     * Create a /Page object
     *
     * @param resources the /Resources object
     * @param contents the content stream
     * @param pageWidth the page's width in points
     * @param pageHeight the page's height in points
     * @param pageIndex the page's zero-based index (or -1 if the page number is auto-determined)
     */
    public PDFPage(PDFResources resources, PDFStream contents,
                   int pageWidth, int pageHeight, int pageIndex) {

        /* generic creation of object */
        super(resources);

        put("Type", new PDFName("Page"));
        /* set fields using parameters */
        setContents(contents);
        setSimplePageSize(pageWidth, pageHeight);
        this.pageIndex = pageIndex;
    }

    /**
     * Create a /Page object
     *
     * @param resources the /Resources object
     * @param pageWidth the page's width in points
     * @param pageHeight the page's height in points
     * @param pageIndex the page's zero-based index (or -1 if the page number is auto-determined)
     */
    public PDFPage(PDFResources resources,
                   int pageWidth, int pageHeight, int pageIndex) {
        this(resources, null, pageWidth, pageHeight, pageIndex);
    }

    private void setSimplePageSize(int width, int height) {
        Rectangle2D box = new Rectangle2D.Double(0, 0, width, height);
        setMediaBox(box);
        setBleedBox(box); //Recommended by PDF/X
        setTrimBox(box); //Needed for PDF/X
    }
    
    private PDFArray toPDFArray(Rectangle2D box) {
        return new PDFArray(this, new double[] {
                box.getX(), box.getY(), box.getMaxX(), box.getMaxY()});
    }
    
    /**
     * Sets the "MediaBox" entry
     * @param box the media rectangle
     */
    public void setMediaBox(Rectangle2D box) {
        put("MediaBox", toPDFArray(box));
    }
    
    /**
     * Sets the "TrimBox" entry
     * @param box the trim rectangle
     */
    public void setTrimBox(Rectangle2D box) {
        put("TrimBox", toPDFArray(box));
    }
    
    /**
     * Sets the "BleedBox" entry
     * @param box the bleed rectangle
     */
    public void setBleedBox(Rectangle2D box) {
        put("BleedBox", toPDFArray(box));
    }
    
    /**
     * set this page contents
     *
     * @param contents the contents of the page
     */
    public void setContents(PDFStream contents) {
        if (contents != null) {
            put("Contents", new PDFReference(contents));
        }
    }

    /**
     * set this page's parent
     *
     * @param parent the /Pages object that is this page's parent
     */
    public void setParent(PDFPages parent) {
        put("Parent", new PDFReference(parent));
    }

    /**
     * Set the transition dictionary and duration.
     * This sets the duration of the page and the transition
     * dictionary used when going to the next page.
     *
     * @param dur the duration in seconds
     * @param tr the transition dictionary
     */
    public void setTransition(int dur, TransitionDictionary tr) {
        put("Dur", new Integer(dur));
        put("Trans", tr);
    }

    /**
     * @return the page Index of this page (zero-based), -1 if it the page index should
     *         automatically be determined.
     */
    public int getPageIndex() {
        return this.pageIndex;
    }

}
