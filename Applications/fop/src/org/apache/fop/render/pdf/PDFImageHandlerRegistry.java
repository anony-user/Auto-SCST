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

/* $Id: PDFImageHandlerRegistry.java 611278 2008-01-11 19:50:53Z jeremias $ */

package org.apache.fop.render.pdf;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.util.Service;

/**
 * This class holds references to various image handlers used by the PDF renderer. It also
 * supports automatic discovery of additional handlers available through
 * the class path.
 */
public class PDFImageHandlerRegistry {

    /** the logger */
    private static Log log = LogFactory.getLog(PDFImageHandlerRegistry.class);
    
    private static final Comparator HANDLER_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            PDFImageHandler h1 = (PDFImageHandler)o1;
            PDFImageHandler h2 = (PDFImageHandler)o2;
            return h1.getPriority() - h2.getPriority();
        }
    };

    /** Map containing PDF image handlers for various MIME types */
    private Map handlers = new java.util.HashMap();
    /** List containing the same handlers as above but ordered by priority */
    private List handlerList = new java.util.LinkedList();
    
    /** Sorted Set of registered handlers */
    private ImageFlavor[] supportedFlavors = new ImageFlavor[0];
    private int handlerRegistrations;
    private int lastSync;
    
    /**
     * Default constructor.
     */
    public PDFImageHandlerRegistry() {
        discoverHandlers();
    }
    
    /**
     * Add an PDFImageHandler. The handler itself is inspected to find out what it supports.
     * @param classname the fully qualified class name
     */
    public void addHandler(String classname) {
        try {
            PDFImageHandler handlerInstance
                = (PDFImageHandler)Class.forName(classname).newInstance();
            addHandler(handlerInstance);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find "
                                               + classname);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                                               + classname);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access "
                                               + classname);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(classname
                                               + " is not an " 
                                               + PDFImageHandler.class.getName());
        }
    }
    
    /**
     * Add an image handler. The handler itself is inspected to find out what it supports.
     * @param handler the PDFImageHandler instance
     */
    public synchronized void addHandler(PDFImageHandler handler) {
        Class imageClass = handler.getSupportedImageClass();
        this.handlers.put(imageClass, handler);
        
        //Sorted insert
        ListIterator iter = this.handlerList.listIterator();
        while (iter.hasNext()) {
            PDFImageHandler h = (PDFImageHandler)iter.next();
            if (HANDLER_COMPARATOR.compare(handler, h) < 0) {
                iter.previous();
                break;
            }
        }
        iter.add(handler);
        this.handlerRegistrations++;
    }
    
    /**
     * Returns an PDFImageHandler which handles an specific image type given the MIME type
     * of the image.
     * @param img the Image to be handled
     * @return the PDFImageHandler responsible for handling the image or null if none is available
     */
    public PDFImageHandler getHandler(Image img) {
        return getHandler(img.getClass());
    }

    /**
     * Returns an PDFImageHandler which handles an specific image type given the MIME type
     * of the image.
     * @param imageClass the Image subclass for which to get a handler
     * @return the PDFImageHandler responsible for handling the image or null if none is available
     */
    protected synchronized PDFImageHandler getHandler(Class imageClass) {
        PDFImageHandler handler = null;
        Class cl = imageClass;
        while (cl != null) {
            handler = (PDFImageHandler)handlers.get(cl);
            if (handler != null) {
                break;
            }
            cl = cl.getSuperclass();
        }
        return handler;
    }

    /**
     * Returns the ordered array of supported image flavors. 
     * @return the array of image flavors
     */
    public synchronized ImageFlavor[] getSupportedFlavors() {
        if (this.lastSync != this.handlerRegistrations) {
            //Extract all ImageFlavors into a single array
            List flavors = new java.util.ArrayList();
            Iterator iter = this.handlerList.iterator();
            while (iter.hasNext()) {
                ImageFlavor[] f = ((PDFImageHandler)iter.next()).getSupportedImageFlavors();
                for (int i = 0; i < f.length; i++) {
                    flavors.add(f[i]);
                }
            }
            this.supportedFlavors = (ImageFlavor[])flavors.toArray(new ImageFlavor[flavors.size()]);
            this.lastSync = this.handlerRegistrations;
        }
        return this.supportedFlavors;
    }
    
    /**
     * Discovers PDFImageHandler implementations through the classpath and dynamically
     * registers them.
     */
    private void discoverHandlers() {
        // add mappings from available services
        Iterator providers = Service.providers(PDFImageHandler.class);
        if (providers != null) {
            while (providers.hasNext()) {
                PDFImageHandler handler = (PDFImageHandler)providers.next();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Dynamically adding PDFImageHandler: " 
                                + handler.getClass().getName());
                    }
                    addHandler(handler);
                } catch (IllegalArgumentException e) {
                    log.error("Error while adding PDFImageHandler", e);
                }

            }
        }
    }
}
