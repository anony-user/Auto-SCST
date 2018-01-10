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

/* $Id: PDFImageHandlerRawCCITTFax.java 611278 2008-01-11 19:50:53Z jeremias $ */

package org.apache.fop.render.pdf;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;

import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFImage;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.render.RendererContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageRawCCITTFax;

/**
 * PDFImageHandler implementation which handles CCITT encoded images (CCITT fax group 3/4).
 */
public class PDFImageHandlerRawCCITTFax implements PDFImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] {
        ImageFlavor.RAW_CCITTFAX,
    };
    
    /** {@inheritDoc} */
    public PDFXObject generateImage(RendererContext context, Image image, 
            Point origin, Rectangle pos)
            throws IOException {
        PDFRenderer renderer = (PDFRenderer)context.getRenderer();
        ImageRawCCITTFax ccitt = (ImageRawCCITTFax)image;
        PDFDocument pdfDoc = (PDFDocument)context.getProperty(
                PDFRendererContextConstants.PDF_DOCUMENT);
        PDFResourceContext resContext = (PDFResourceContext)context.getProperty(
                PDFRendererContextConstants.PDF_CONTEXT);
        
        PDFImage pdfimage = new ImageRawCCITTFaxAdapter(ccitt, image.getInfo().getOriginalURI());
        PDFXObject xobj = pdfDoc.addImage(resContext, pdfimage);

        float x = (float)pos.getX() / 1000f;
        float y = (float)pos.getY() / 1000f;
        float w = (float)pos.getWidth() / 1000f;
        float h = (float)pos.getHeight() / 1000f;
        renderer.placeImage(x, y, w, h, xobj);
        
        return xobj;
    }

    /** {@inheritDoc} */
    public int getPriority() {
        return 100;
    }

    /** {@inheritDoc} */
    public Class getSupportedImageClass() {
        return ImageRawCCITTFax.class;
    }

    /** {@inheritDoc} */
    public ImageFlavor[] getSupportedImageFlavors() {
        return FLAVORS;
    }

}
