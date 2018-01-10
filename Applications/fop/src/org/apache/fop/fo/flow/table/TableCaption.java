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

/* $Id: TableCaption.java 592103 2007-11-05 17:42:37Z vhennebert $ */

package org.apache.fop.fo.flow.table;

// XML
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;


/**
 * Class modelling the fo:table-caption object.
 */
public class TableCaption extends FObj {
    // The value of properties relevant for fo:table-caption.
    // Unused but valid items, commented out for performance:
    //     private CommonAural commonAural;
    //     private CommonRelativePosition commonRelativePosition;
    //     private LengthRangeProperty blockProgressionDimension;
    //     private Length height;
    //     private LengthRangeProperty inlineProgressionDimension;
    //     private int intrusionDisplace;
    //     private KeepProperty keepTogether;
    //     private Length width;
    // End of property values

    /** used for FO validation */
    private boolean blockItemFound = false;

    static boolean notImplementedWarningGiven = false;

    /**
     * @param parent FONode that is the parent of this object
     */
    public TableCaption(FONode parent) {
        super(parent);

        if (!notImplementedWarningGiven) {
            log.warn("fo:table-caption is not yet implemented.");
            notImplementedWarningGiven = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
    }

    /**
     * {@inheritDoc}
     */
    protected void endOfNode() throws FOPException {
        if (firstChild == null) {
            missingChildElementError("marker* (%block;)");
        }
    }

    /**
     * {@inheritDoc}
     * XSL Content Model: marker* (%block;)
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
        if (FO_URI.equals(nsURI) && localName.equals("marker")) {
            if (blockItemFound) {
               nodesOutOfOrderError(loc, "fo:marker", "(%block;)");
            }
        } else if (!isBlockItem(nsURI, localName)) {
            invalidChildError(loc, nsURI, localName);
        } else {
            blockItemFound = true;
        }
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "table-caption";
    }

    /**
     * {@inheritDoc}
     */
    public int getNameId() {
        return FO_TABLE_CAPTION;
    }
}

