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

/* $Id: BitmapProducerPS.java 426576 2006-07-28 15:44:37Z jeremias $ */

package org.apache.fop.visual;

import org.apache.fop.apps.MimeConstants;

/**
 * BitmapProducer implementation that uses the PSRenderer and an external converter 
 * to create bitmaps.
 * <p>
 * See the superclass' javadoc for info on the configuration format.
 */
public class BitmapProducerPS extends AbstractPSPDFBitmapProducer {

    /** @see org.apache.fop.visual.AbstractPSPDFBitmapProducer#getTargetExtension() */
    protected String getTargetExtension() {
        return "ps";
    }
    
    /** @see org.apache.fop.visual.AbstractPSPDFBitmapProducer#getTargetFormat() */
    protected String getTargetFormat() {
        return MimeConstants.MIME_POSTSCRIPT;
    }
    

}
