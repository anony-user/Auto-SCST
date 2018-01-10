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

/* $Id: PDFTranscoder.java 611278 2008-01-11 19:50:53Z jeremias $ */

package org.apache.fop.svg;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.UnitProcessor;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.keys.BooleanKey;
import org.apache.batik.transcoder.keys.FloatKey;
import org.apache.batik.util.ParsedURL;
import org.apache.fop.Version;
import org.apache.fop.fonts.FontInfo;
import org.apache.xmlgraphics.image.loader.ImageContext;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.impl.AbstractImageSessionContext;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGLength;

/**
 * This class enables to transcode an input to a pdf document.
 *
 * <p>Two transcoding hints (<tt>KEY_WIDTH</tt> and
 * <tt>KEY_HEIGHT</tt>) can be used to respectively specify the image
 * width and the image height. If only one of these keys is specified,
 * the transcoder preserves the aspect ratio of the original image.
 *
 * <p>The <tt>KEY_BACKGROUND_COLOR</tt> defines the background color
 * to use for opaque image formats, or the background color that may
 * be used for image formats that support alpha channel.
 *
 * <p>The <tt>KEY_AOI</tt> represents the area of interest to paint
 * in device space.
 *
 * <p>Three additional transcoding hints that act on the SVG
 * processor can be specified:
 *
 * <p><tt>KEY_LANGUAGE</tt> to set the default language to use (may be
 * used by a &lt;switch> SVG element for example),
 * <tt>KEY_USER_STYLESHEET_URI</tt> to fix the URI of a user
 * stylesheet, and <tt>KEY_PIXEL_TO_MM</tt> to specify the pixel to
 * millimeter conversion factor.
 * 
 * <p><tt>KEY_AUTO_FONTS</tt> to disable the auto-detection of fonts installed in the system.
 * The PDF Transcoder cannot use AWT's font subsystem and that's why the fonts have to be
 * configured differently. By default, font auto-detection is enabled to match the behaviour
 * of the other transcoders, but this may be associated with a price in the form of a small
 * performance penalty. If font auto-detection is not desired, it can be disable using this key.
 *
 * @author <a href="mailto:keiron@aftexsw.com">Keiron Liddle</a>
 * @version $Id: PDFTranscoder.java 611278 2008-01-11 19:50:53Z jeremias $
 */
public class PDFTranscoder extends AbstractFOPTranscoder
        implements Configurable {

    /**
     * The key is used to specify the resolution for on-the-fly images generated
     * due to complex effects like gradients and filters. 
     */
    public static final TranscodingHints.Key KEY_DEVICE_RESOLUTION = new FloatKey();

    /**
     * The key is used to specify whether the available fonts should be automatically
     * detected. The alternative is to configure the transcoder manually using a configuration
     * file. 
     */
    public static final TranscodingHints.Key KEY_AUTO_FONTS = new BooleanKey();

    private Configuration cfg = null;
    
    /** Graphics2D instance that is used to paint to */
    protected PDFDocumentGraphics2D graphics = null;

    private ImageManager imageManager;
    private ImageSessionContext imageSessionContext;
    
    /**
     * Constructs a new <tt>PDFTranscoder</tt>.
     */
    public PDFTranscoder() {
        super();
        this.handler = new FOPErrorHandler();
    }

    /**
     * {@inheritDoc}
     */
    protected UserAgent createUserAgent() {
        return new AbstractFOPTranscoder.FOPTranscoderUserAgent() {
            // The PDF stuff wants everything at 72dpi
            public float getPixelUnitToMillimeter() {
                return super.getPixelUnitToMillimeter();
                //return 25.4f / 72; //72dpi = 0.352778f;
            }
        };
    }
    
    /** {@inheritDoc} */
    public void configure(Configuration cfg) throws ConfigurationException {
        this.cfg = cfg;
    }

    /**
     * Transcodes the specified Document as an image in the specified output.
     *
     * @param document the document to transcode
     * @param uri the uri of the document or null if any
     * @param output the ouput where to transcode
     * @exception TranscoderException if an error occured while transcoding
     */
    protected void transcode(Document document, String uri,
                             TranscoderOutput output) 
        throws TranscoderException {

        graphics = new PDFDocumentGraphics2D(isTextStroked());
        graphics.getPDFDocument().getInfo().setProducer("Apache FOP Version " 
                + Version.getVersion() 
                + ": PDF Transcoder for Batik");
        if (hints.containsKey(KEY_DEVICE_RESOLUTION)) {
            graphics.setDeviceDPI(((Float)hints.get(KEY_DEVICE_RESOLUTION)).floatValue());
        }
        
        setupImageInfrastructure(uri);
        
        try {
            Configuration effCfg = this.cfg; 
            if (effCfg == null) {
                //By default, enable font auto-detection if no cfg is given
                boolean autoFonts = true;
                if (hints.containsKey(KEY_AUTO_FONTS)) {
                    autoFonts = ((Boolean)hints.get(KEY_AUTO_FONTS)).booleanValue();
                }
                if (autoFonts) {
                    DefaultConfiguration c = new DefaultConfiguration("pdf-transcoder");
                    DefaultConfiguration fonts = new DefaultConfiguration("fonts");
                    c.addChild(fonts);
                    DefaultConfiguration autodetect = new DefaultConfiguration("auto-detect");
                    fonts.addChild(autodetect);
                    effCfg = c;
                }
            }
            
            if (effCfg != null) {
                PDFDocumentGraphics2DConfigurator configurator
                        = new PDFDocumentGraphics2DConfigurator();
                configurator.configure(graphics, effCfg);
            } else {
                graphics.setupDefaultFontInfo();
            }
        } catch (Exception e) {
            throw new TranscoderException(
                "Error while setting up PDFDocumentGraphics2D", e);
        }

        super.transcode(document, uri, output);

        if (getLogger().isTraceEnabled()) {
            getLogger().trace("document size: " + width + " x " + height);
        }
        
        // prepare the image to be painted
        UnitProcessor.Context uctx = UnitProcessor.createContext(ctx, 
                    document.getDocumentElement());
        float widthInPt = UnitProcessor.userSpaceToSVG(width, SVGLength.SVG_LENGTHTYPE_PT, 
                    UnitProcessor.HORIZONTAL_LENGTH, uctx);
        int w = (int)(widthInPt + 0.5);
        float heightInPt = UnitProcessor.userSpaceToSVG(height, SVGLength.SVG_LENGTHTYPE_PT, 
                UnitProcessor.HORIZONTAL_LENGTH, uctx);
        int h = (int)(heightInPt + 0.5);
        if (getLogger().isTraceEnabled()) {
            getLogger().trace("document size: " + w + "pt x " + h + "pt");
        }

        // prepare the image to be painted
        //int w = (int)(width + 0.5);
        //int h = (int)(height + 0.5);

        try {
            OutputStream out = output.getOutputStream();
            if (!(out instanceof BufferedOutputStream)) {
                out = new BufferedOutputStream(out);
            }
            graphics.setupDocument(out, w, h);
            graphics.setSVGDimension(width, height);

            if (hints.containsKey(ImageTranscoder.KEY_BACKGROUND_COLOR)) {
                graphics.setBackgroundColor
                    ((Color)hints.get(ImageTranscoder.KEY_BACKGROUND_COLOR));
            }
            graphics.setGraphicContext
                (new org.apache.xmlgraphics.java2d.GraphicContext());
            graphics.preparePainting();

            graphics.transform(curTxf);
            graphics.setRenderingHint
                (RenderingHintsKeyExt.KEY_TRANSCODING,
                 RenderingHintsKeyExt.VALUE_TRANSCODING_VECTOR);

            this.root.paint(graphics);

            graphics.finish();
        } catch (IOException ex) {
            throw new TranscoderException(ex);
        }
    }

    private void setupImageInfrastructure(final String baseURI) {
        final ImageContext imageContext = new ImageContext() {
            public float getSourceResolution() {
                return 25.4f / userAgent.getPixelUnitToMillimeter();
            }
        };
        this.imageManager = new ImageManager(imageContext);
        this.imageSessionContext = new AbstractImageSessionContext() {

            public ImageContext getParentContext() {
                return imageContext;
            }

            public float getTargetResolution() {
                return graphics.getDeviceDPI();
            }

            public Source resolveURI(String uri) {
                System.out.println("resolve " + uri);
                try {
                    ParsedURL url = new ParsedURL(baseURI, uri);
                    InputStream in = url.openStream();
                    StreamSource source = new StreamSource(in, url.toString());
                    return source;
                } catch (IOException ioe) {
                    userAgent.displayError(ioe);
                    return null;
                }
            }
            
        };
    }

    /** {@inheritDoc} */
    protected BridgeContext createBridgeContext() {
        //For compatibility with Batik 1.6
        return createBridgeContext("1.x");
    }

    /** {@inheritDoc} */
    public BridgeContext createBridgeContext(String version) {
        FontInfo fontInfo = graphics.getFontInfo();
        if (isTextStroked()) {
            fontInfo = null;
        }
        BridgeContext ctx = new PDFBridgeContext(userAgent, fontInfo,
                this.imageManager, this.imageSessionContext);
        return ctx;
    }
    
}
