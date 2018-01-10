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

/* $Id: RendererFactory.java 627367 2008-02-13 12:03:30Z maxberger $ */
 
package org.apache.fop.render;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.fo.FOEventHandler;
import org.apache.xmlgraphics.util.Service;

/**
 * Factory for FOEventHandlers and Renderers.
 */
public class RendererFactory {
    
    /** the logger */
    private static Log log = LogFactory.getLog(RendererFactory.class);

    private Map rendererMakerMapping = new java.util.HashMap();
    private Map eventHandlerMakerMapping = new java.util.HashMap();
    
    
    /**
     * Main constructor.
     */
    public RendererFactory() {
        discoverRenderers();
        discoverFOEventHandlers();
    }
    
    /**
     * Add a new RendererMaker. If another maker has already been registered for a
     * particular MIME type, this call overwrites the existing one.
     * @param maker the RendererMaker
     */
    public void addRendererMaker(AbstractRendererMaker maker) {
        String[] mimes = maker.getSupportedMimeTypes();
        for (int i = 0; i < mimes.length; i++) {
            //This overrides any renderer previously set for a MIME type
            if (rendererMakerMapping.get(mimes[i]) != null) {
                log.trace("Overriding renderer for " + mimes[i] 
                        + " with " + maker.getClass().getName());
            }
            rendererMakerMapping.put(mimes[i], maker);
        }
    }
    
    /**
     * Add a new FOEventHandlerMaker. If another maker has already been registered for a
     * particular MIME type, this call overwrites the existing one.
     * @param maker the FOEventHandlerMaker
     */
    public void addFOEventHandlerMaker(AbstractFOEventHandlerMaker maker) {
        String[] mimes = maker.getSupportedMimeTypes();
        for (int i = 0; i < mimes.length; i++) {
            //This overrides any event handler previously set for a MIME type
            if (eventHandlerMakerMapping.get(mimes[i]) != null) {
                log.trace("Overriding FOEventHandler for " + mimes[i] 
                        + " with " + maker.getClass().getName());
            }
            eventHandlerMakerMapping.put(mimes[i], maker);
        }
    }
    
    /**
     * Add a new RendererMaker. If another maker has already been registered for a
     * particular MIME type, this call overwrites the existing one.
     * @param className the fully qualified class name of the RendererMaker
     */
    public void addRendererMaker(String className) {
        try {
            AbstractRendererMaker makerInstance
                = (AbstractRendererMaker)Class.forName(className).newInstance();
            addRendererMaker(makerInstance);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find "
                                               + className);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                                               + className);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access "
                                               + className);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(className
                                               + " is not an "
                                               + AbstractRendererMaker.class.getName());
        }
    }
    
    /**
     * Add a new FOEventHandlerMaker. If another maker has already been registered for a
     * particular MIME type, this call overwrites the existing one.
     * @param className the fully qualified class name of the FOEventHandlerMaker
     */
    public void addFOEventHandlerMaker(String className) {
        try {
            AbstractFOEventHandlerMaker makerInstance
                = (AbstractFOEventHandlerMaker)Class.forName(className).newInstance();
            addFOEventHandlerMaker(makerInstance);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find "
                                               + className);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                                               + className);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access "
                                               + className);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(className
                                               + " is not an "
                                               + AbstractFOEventHandlerMaker.class.getName());
        }
    }
    
    /**
     * Returns a RendererMaker which handles the given MIME type.
     * @param mime the requested output format
     * @return the requested RendererMaker or null if none is available
     */
    public AbstractRendererMaker getRendererMaker(String mime) {
        AbstractRendererMaker maker
            = (AbstractRendererMaker)rendererMakerMapping.get(mime);
        return maker;
    }
    
    /**
     * Returns a FOEventHandlerMaker which handles the given MIME type.
     * @param mime the requested output format
     * @return the requested FOEventHandlerMaker or null if none is available
     */
    public AbstractFOEventHandlerMaker getFOEventHandlerMaker(String mime) {
        AbstractFOEventHandlerMaker maker
            = (AbstractFOEventHandlerMaker)eventHandlerMakerMapping.get(mime);
        return maker;
    }
    
    /**
     * Creates a Renderer object based on render-type desired
     * @param userAgent the user agent for access to configuration
     * @param outputFormat the MIME type of the output format to use (ex. "application/pdf").
     * @return the new Renderer instance
     * @throws FOPException if the renderer cannot be properly constructed
     */
    public Renderer createRenderer(FOUserAgent userAgent, String outputFormat) 
                    throws FOPException {
        if (userAgent.getRendererOverride() != null) {
            return userAgent.getRendererOverride();
        } else {
            AbstractRendererMaker maker = getRendererMaker(outputFormat);
            if (maker == null) {
                throw new UnsupportedOperationException(
                        "No renderer for the requested format available: " + outputFormat);
            }
            Renderer rend = maker.makeRenderer(userAgent);
            rend.setUserAgent(userAgent);
            RendererConfigurator configurator = maker.getConfigurator(userAgent);
            if (configurator != null) {
                configurator.configure(rend);
            }
            return rend;
        }
    }
    
    
    /**
     * Creates FOEventHandler instances based on the desired output.
     * @param userAgent the user agent for access to configuration
     * @param outputFormat the MIME type of the output format to use (ex. "application/pdf").
     * @param out the OutputStream where the output is written to (if applicable)
     * @return the newly constructed FOEventHandler
     * @throws FOPException if the FOEventHandler cannot be properly constructed
     */
    public FOEventHandler createFOEventHandler(FOUserAgent userAgent, 
                String outputFormat, OutputStream out) throws FOPException {

        if (userAgent.getFOEventHandlerOverride() != null) {
            return userAgent.getFOEventHandlerOverride();
        } else {
            AbstractFOEventHandlerMaker maker = getFOEventHandlerMaker(outputFormat);
            if (maker == null) {
                AbstractRendererMaker rendMaker = getRendererMaker(outputFormat);
                if (rendMaker == null && userAgent.getRendererOverride() == null) {
                    throw new UnsupportedOperationException(
                            "Don't know how to handle \"" + outputFormat + "\" as an output format."
                            + " Neither an FOEventHandler, nor a Renderer could be found"
                            + " for this output format.");
                } else {
                    if (out == null 
                            && userAgent.getRendererOverride() == null 
                            && rendMaker.needsOutputStream()) {
                        throw new FOPException(
                            "OutputStream has not been set");
                    }
                    //Found a Renderer so we need to construct an AreaTreeHandler.
                    return new AreaTreeHandler(userAgent, outputFormat, out);
                }
            } else {
                return maker.makeFOEventHandler(userAgent, out);
            }
        }
    }
    
    /**
     * @return an array of all supported MIME types
     */
    public String[] listSupportedMimeTypes() {
        List lst = new java.util.ArrayList();
        Iterator iter = this.rendererMakerMapping.keySet().iterator();
        while (iter.hasNext()) {
            lst.add(((String)iter.next()));
        }
        iter = this.eventHandlerMakerMapping.keySet().iterator();
        while (iter.hasNext()) {
            lst.add(((String)iter.next()));
        }
        Collections.sort(lst);
        return (String[])lst.toArray(new String[lst.size()]);
    }
    
    /**
     * Discovers Renderer implementations through the classpath and dynamically
     * registers them.
     */
    private void discoverRenderers() {
        // add mappings from available services
        Iterator providers
            = Service.providers(Renderer.class);
        if (providers != null) {
            while (providers.hasNext()) {
                AbstractRendererMaker maker = (AbstractRendererMaker)providers.next();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Dynamically adding maker for Renderer: " 
                                + maker.getClass().getName());
                    }
                    addRendererMaker(maker);
                } catch (IllegalArgumentException e) {
                    log.error("Error while adding maker for Renderer", e);
                }

            }
        }
    }
    
    /**
     * Discovers FOEventHandler implementations through the classpath and dynamically
     * registers them.
     */
    private void discoverFOEventHandlers() {
        // add mappings from available services
        Iterator providers
            = Service.providers(FOEventHandler.class);
        if (providers != null) {
            while (providers.hasNext()) {
                AbstractFOEventHandlerMaker maker = (AbstractFOEventHandlerMaker)providers.next();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Dynamically adding maker for FOEventHandler: " 
                                + maker.getClass().getName());
                    }
                    addFOEventHandlerMaker(maker);
                } catch (IllegalArgumentException e) {
                    log.error("Error while adding maker for FOEventHandler", e);
                }

            }
        }
    }
    
}
