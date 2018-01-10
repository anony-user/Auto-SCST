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

/* $Id: Float.java 557337 2007-07-18 17:37:14Z adelmelle $ */

package org.apache.fop.fo.flow;

// XML
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;

/**
 * Class modelling the fo:float object.
 */
public class Float extends FObj {
    // The value of properties relevant for fo:float (commented out for performance.
    //     private int float_;
    //     private int clear;
    // End of property values

    static boolean notImplementedWarningGiven = false;
    
    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public Float(FONode parent) {
        super(parent);
        
        if (!notImplementedWarningGiven) {
            log.warn("fo:float is not yet implemented.");
            notImplementedWarningGiven = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void bind(PropertyList pList) throws FOPException {
        // No active properties -> Nothing to do.
    }

    /**
     * {@inheritDoc}
     * XSL Content Model: (%block;)+
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
            if (!isBlockItem(nsURI, localName)) {
                invalidChildError(loc, nsURI, localName);
            }
    }

    /**
     * {@inheritDoc}
     */
    protected void endOfNode() throws FOPException {
        if (firstChild == null) {
            missingChildElementError("(%block;)+");
        }
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "float";
    }
    
    /**
     * {@inheritDoc}
     */
    public int getNameId() {
        return FO_FLOAT;
    }
}