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

/* $Id: AbstractPageSequence.java 607032 2007-12-27 10:34:15Z jeremias $ */

package org.apache.fop.fo.pagination;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;

/**
 * Abstract base class for the fo:page-sequence formatting object and the fox:external-document
 * extension object.
 */
public abstract class AbstractPageSequence extends FObj {
    
    // The value of properties relevant for fo:page-sequence.
    protected Numeric initialPageNumber;
    protected int forcePageCount;
    private String format;
    private int letterValue;
    private char groupingSeparator;
    private int groupingSize;
    private Numeric referenceOrientation; //XSL 1.1
    // End of property values

    private PageNumberGenerator pageNumberGenerator;

    protected int startingPageNumber = 0;

    /**
     * Create a page sequence FO node.
     *
     * @param parent the parent FO node
     */
    public AbstractPageSequence(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FObj#bind(PropertyList)
     */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
        initialPageNumber = pList.get(PR_INITIAL_PAGE_NUMBER).getNumeric();
        forcePageCount = pList.get(PR_FORCE_PAGE_COUNT).getEnum();
        format = pList.get(PR_FORMAT).getString();
        letterValue = pList.get(PR_LETTER_VALUE).getEnum();
        groupingSeparator = pList.get(PR_GROUPING_SEPARATOR).getCharacter();
        groupingSize = pList.get(PR_GROUPING_SIZE).getNumber().intValue();
        referenceOrientation = pList.get(PR_REFERENCE_ORIENTATION).getNumeric();
    }

    /**
     * @see org.apache.fop.fo.FONode#startOfNode()
     */
    protected void startOfNode() throws FOPException {
        this.pageNumberGenerator = new PageNumberGenerator(
                format, groupingSeparator, groupingSize, letterValue);

    }

    /** @see org.apache.fop.fo.FONode#endOfNode() */
    protected void endOfNode() throws FOPException {
    }

    /**
     * Initialize the current page number for the start of the page sequence.
     */
    public void initPageNumber() {
        int pageNumberType = 0;
        
        if (initialPageNumber.getEnum() != 0) {
            // auto | auto-odd | auto-even.
            startingPageNumber = getRoot().getEndingPageNumberOfPreviousSequence() + 1;
            pageNumberType = initialPageNumber.getEnum();
            if (pageNumberType == EN_AUTO_ODD) {
                if (startingPageNumber % 2 == 0) {
                    startingPageNumber++;
                }
            } else if (pageNumberType == EN_AUTO_EVEN) {
                if (startingPageNumber % 2 == 1) {
                    startingPageNumber++;
                }
            }
        } else { // <integer> for explicit page number
            int pageStart = initialPageNumber.getValue();
            startingPageNumber = (pageStart > 0) ? pageStart : 1; // spec rule
        }
    }

    /**
     * Get the starting page number for this page sequence.
     *
     * @return the starting page number
     */
    public int getStartingPageNumber() {
        return startingPageNumber;
    }

    /**
     * Retrieves the string representation of a page number applicable
     * for this page sequence
     * @param pageNumber the page number
     * @return string representation of the page number
     */
    public String makeFormattedPageNumber(int pageNumber) {
        return pageNumberGenerator.makeFormattedPageNumber(pageNumber);
    }

    /**
     * Public accessor for the ancestor Root.
     * @return the ancestor Root
     */
    public Root getRoot() {
        return (Root)this.getParent();
    }

    /** @return the force-page-count value */
    public int getForcePageCount() {
        return forcePageCount;
    }

    /** @return the initial-page-number property value */
    public Numeric getInitialPageNumber() {
        return initialPageNumber;
    }
    
    /**
     * @return the "reference-orientation" property.
     * @since XSL 1.1
     */
    public int getReferenceOrientation() {
        return referenceOrientation.getValue();
    }

}
