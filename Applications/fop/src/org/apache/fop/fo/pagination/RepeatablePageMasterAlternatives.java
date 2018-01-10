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

/* $Id: RepeatablePageMasterAlternatives.java 607032 2007-12-27 10:34:15Z jeremias $ */

package org.apache.fop.fo.pagination;

// Java
import java.util.List;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.Property;
import org.xml.sax.Locator;

/**
 * A repeatable-page-master-alternatives formatting object.
 * This contains a list of conditional-page-master-reference
 * and the page master is found from the reference that
 * matches the page number and emptyness.
 */
public class RepeatablePageMasterAlternatives extends FObj
    implements SubSequenceSpecifier {
    // The value of properties relevant for fo:repeatable-page-master-alternatives.
    private Property maximumRepeats;
    // End of property values
    
    private static final int INFINITE = -1;

    private int numberConsumed = 0;

    private List conditionalPageMasterRefs;
    private boolean hasPagePositionLast = false;
    private boolean hasPagePositionOnly = false;

    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public RepeatablePageMasterAlternatives(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    public void bind(PropertyList pList) throws FOPException {
        maximumRepeats = pList.get(PR_MAXIMUM_REPEATS);
    }

    /**
     * {@inheritDoc}
     */
    protected void startOfNode() throws FOPException {
        conditionalPageMasterRefs = new java.util.ArrayList();

        if (parent.getName().equals("fo:page-sequence-master")) {
            PageSequenceMaster pageSequenceMaster = (PageSequenceMaster)parent;
            pageSequenceMaster.addSubsequenceSpecifier(this);
        } else {
            throw new ValidationException("fo:repeatable-page-master-alternatives "
                                   + "must be child of fo:page-sequence-master, not "
                                   + parent.getName(), locator);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void endOfNode() throws FOPException {
        if (firstChild == null) {
           missingChildElementError("(conditional-page-master-reference+)");
        }
    }

    /**
     * {@inheritDoc}
        XSL/FOP: (conditional-page-master-reference+)
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
        if (!(FO_URI.equals(nsURI)
                && localName.equals("conditional-page-master-reference"))) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** @return the "maximum-repeats" property. */
    public int getMaximumRepeats() {
        if (maximumRepeats.getEnum() == EN_NO_LIMIT) {
            return INFINITE;
        } else {
            int mr = maximumRepeats.getNumeric().getValue();
            if (mr < 0) {
                log.debug("negative maximum-repeats: "
                        + this.maximumRepeats);
                mr = 0;
            }
            return mr;
        }
    }

    /**
     * Get the next matching page master from the conditional
     * page master references.
     * @see org.apache.fop.fo.pagination.SubSequenceSpecifier
     */
    public String getNextPageMasterName(boolean isOddPage,
                                        boolean isFirstPage,
                                        boolean isLastPage,
                                        boolean isOnlyPage,
                                        boolean isBlankPage) {
        if (getMaximumRepeats() != INFINITE) {
            if (numberConsumed < getMaximumRepeats()) {
                numberConsumed++;
            } else {
                return null;
            }
        } else {
            numberConsumed++;
        }

        for (int i = 0; i < conditionalPageMasterRefs.size(); i++) {
            ConditionalPageMasterReference cpmr
                = (ConditionalPageMasterReference)conditionalPageMasterRefs.get(i);
            if (cpmr.isValid(isOddPage, isFirstPage, isLastPage, isOnlyPage, isBlankPage)) {
                return cpmr.getMasterReference();
            }
        }
        return null;
    }


    /**
     * Adds a new conditional page master reference.
     * @param cpmr the new conditional reference
     */
    public void addConditionalPageMasterReference(ConditionalPageMasterReference cpmr) {
        this.conditionalPageMasterRefs.add(cpmr);
        if (cpmr.getPagePosition() == EN_LAST) {
            this.hasPagePositionLast = true;
        }
        if (cpmr.getPagePosition() == EN_ONLY) {
            this.hasPagePositionOnly = true;
        }
    }

    /** {@inheritDoc} */
    public void reset() {
        this.numberConsumed = 0;
    }

    /** {@inheritDoc} */
    public boolean goToPrevious() {
        if (numberConsumed == 0) {
            return false;
        } else {
            numberConsumed--;
            return true;
        }
    }
    
    /** {@inheritDoc} */
    public boolean hasPagePositionLast() {
        return this.hasPagePositionLast;
    }
    
    /** {@inheritDoc} */
    /** @see org.apache.fop.fo.pagination.SubSequenceSpecifier#hasPagePositionOnly() */
    public boolean hasPagePositionOnly() {
        return this.hasPagePositionOnly;
    }
    
    /** @see org.apache.fop.fo.FONode#getLocalName() */
    public String getLocalName() {
        return "repeatable-page-master-alternatives";
    }

    /** {@inheritDoc} */
    public int getNameId() {
        return FO_REPEATABLE_PAGE_MASTER_ALTERNATIVES;
    }

}
