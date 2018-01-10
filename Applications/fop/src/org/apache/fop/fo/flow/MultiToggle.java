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

/* $Id: MultiToggle.java 557337 2007-07-18 17:37:14Z adelmelle $ */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;


/**
 * Class modelling the fo:multi-toggle property.
 */
public class MultiToggle extends FObj {
    // The value of properties relevant for fo:multi-toggle (commented out for performance).
    //     private CommonAccessibility commonAccessibility;
    // public ToBeImplementedProperty prSwitchTo;
    // End of property values
    
    static boolean notImplementedWarningGiven = false;

    /**
     * @param parent FONode that is the parent of this object
     */
    public MultiToggle(FONode parent) {
        super(parent);

        if (!notImplementedWarningGiven) {
            log.warn("fo:multi-toggle is not yet implemented.");
            notImplementedWarningGiven = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void bind(PropertyList pList) throws FOPException {
        // prSwitchTo = pList.get(PR_SWITCH_TO);

    }

    /**
     * {@inheritDoc}
     * XSL Content Model: (#PCDATA|%inline;|%block;)*
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
        if (!isBlockOrInlineItem(nsURI, localName)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "multi-toggle";
    }
    
    /**
     * {@inheritDoc}
     */
    public int getNameId() {
        return FO_MULTI_TOGGLE;
    }
}
