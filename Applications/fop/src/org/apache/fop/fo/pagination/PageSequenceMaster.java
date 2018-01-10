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

/* $Id: PageSequenceMaster.java 607032 2007-12-27 10:34:15Z jeremias $ */

package org.apache.fop.fo.pagination;

// Java
import java.util.List;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;

/**
 * The page-sequence-master formatting object.
 * This class handles a list of subsequence specifiers
 * which are simple or complex references to page-masters.
 */
public class PageSequenceMaster extends FObj {
    // The value of properties relevant for fo:page-sequence-master.
    private String masterName;
    // End of property values
    
    private LayoutMasterSet layoutMasterSet;
    private List subSequenceSpecifiers;
    private SubSequenceSpecifier currentSubSequence;
    private int currentSubSequenceNumber = -1;

    // The terminology may be confusing. A 'page-sequence-master' consists
    // of a sequence of what the XSL spec refers to as
    // 'sub-sequence-specifiers'. These are, in fact, simple or complex
    // references to page-masters. So the methods use the former
    // terminology ('sub-sequence-specifiers', or SSS),
    // but the actual FO's are MasterReferences.

    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public PageSequenceMaster(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    public void bind(PropertyList pList) throws FOPException {
        masterName = pList.get(PR_MASTER_NAME).getString();
        
        if (masterName == null || masterName.equals("")) {
            missingPropertyError("master-name");
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void startOfNode() throws FOPException {
        subSequenceSpecifiers = new java.util.ArrayList();
        layoutMasterSet = parent.getRoot().getLayoutMasterSet();
        layoutMasterSet.addPageSequenceMaster(masterName, this);
    }
    
    /**
     * {@inheritDoc}
     */
    protected void endOfNode() throws FOPException {
        if (firstChild == null) {
            missingChildElementError("(single-page-master-reference|"
                    + "repeatable-page-master-reference|repeatable-page-master-alternatives)+");
        }
    }

    /**
     * {@inheritDoc}
     * XSL/FOP: (single-page-master-reference|repeatable-page-master-reference|
     *     repeatable-page-master-alternatives)+
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!localName.equals("single-page-master-reference") 
                && !localName.equals("repeatable-page-master-reference")
                && !localName.equals("repeatable-page-master-alternatives")) {   
                    invalidChildError(loc, nsURI, localName);
            }
        } else {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /**
     * Adds a new suqsequence specifier to the page sequence master.
     * @param pageMasterReference the subsequence to add
     */
    protected void addSubsequenceSpecifier(SubSequenceSpecifier pageMasterReference) {
        subSequenceSpecifiers.add(pageMasterReference);
    }

    /**
     * Returns the next subsequence specifier
     * @return a subsequence specifier
     */
    private SubSequenceSpecifier getNextSubSequence() {
        currentSubSequenceNumber++;
        if (currentSubSequenceNumber >= 0
                && currentSubSequenceNumber < subSequenceSpecifiers.size()) {
            return (SubSequenceSpecifier)subSequenceSpecifiers
              .get(currentSubSequenceNumber);
        }
        return null;
    }

    /**
     * Resets the subsequence specifiers subsystem.
     */
    public void reset() {
        currentSubSequenceNumber = -1;
        currentSubSequence = null;
        if (subSequenceSpecifiers != null) {
            for (int i = 0; i < subSequenceSpecifiers.size(); i++) {
                ((SubSequenceSpecifier)subSequenceSpecifiers.get(i)).reset();
            }
        }
    }

    /**
     * Used to set the "cursor position" for the page masters to the previous item.
     * @return true if there is a previous item, false if the current one was the first one.
     */
    public boolean goToPreviousSimplePageMaster() {
        if (currentSubSequence != null) {
            boolean success = currentSubSequence.goToPrevious();
            if (!success) {
                if (currentSubSequenceNumber > 0) {
                    currentSubSequenceNumber--;
                    currentSubSequence = (SubSequenceSpecifier)subSequenceSpecifiers
                        .get(currentSubSequenceNumber);
                } else {
                    currentSubSequence = null;
                }
            }
        }
        return (currentSubSequence != null);
    }
    
    /** @return true if the page-sequence-master has a page-master with page-position="last" */
    public boolean hasPagePositionLast() {
        if (currentSubSequence != null) {
            return currentSubSequence.hasPagePositionLast();
        } else {
            return false;
        }
    }
    
    /** @return true if the page-sequence-master has a page-master with page-position="only" */
    public boolean hasPagePositionOnly() {
        if (currentSubSequence != null) {
            return currentSubSequence.hasPagePositionOnly();
        } else {
            return false;
        }
    }
    
    /**
     * Returns the next simple-page-master.
     * @param isOddPage True if the next page number is odd
     * @param isFirstPage True if the next page is the first
     * @param isLastPage True if the next page is the last
     * @param isOnlyPage True if the next page is the only page
     * @param isBlankPage True if the next page is blank
     * @return the requested page master
     * @throws FOPException if there's a problem determining the next page master
     */
    public SimplePageMaster getNextSimplePageMaster(boolean isOddPage,
                                                    boolean isFirstPage,
                                                    boolean isLastPage,
                                                    boolean isOnlyPage,
                                                    boolean isBlankPage)
                                                      throws FOPException {
        if (currentSubSequence == null) {
            currentSubSequence = getNextSubSequence();
            if (currentSubSequence == null) {
                throw new FOPException("no subsequences in page-sequence-master '"
                                       + masterName + "'");
            }
        }
        String pageMasterName = currentSubSequence
            .getNextPageMasterName(isOddPage, isFirstPage, isLastPage, isOnlyPage, isBlankPage);
        boolean canRecover = true;
        while (pageMasterName == null) {
            SubSequenceSpecifier nextSubSequence = getNextSubSequence();
            if (nextSubSequence == null) {
                if (!canRecover) {
                    throw new FOPException("subsequences exhausted in page-sequence-master '"
                                           + masterName
                                           + "', cannot recover");
                }
                log.warn("subsequences exhausted in page-sequence-master '"
                                 + masterName
                                 + "', using previous subsequence");
                currentSubSequence.reset();
                canRecover = false;
            } else {
                currentSubSequence = nextSubSequence;
            }
            pageMasterName = currentSubSequence
                .getNextPageMasterName(isOddPage, isFirstPage, isLastPage, isOnlyPage, isBlankPage);
        }
        SimplePageMaster pageMaster = this.layoutMasterSet
            .getSimplePageMaster(pageMasterName);
        if (pageMaster == null) {
            throw new FOPException("No simple-page-master matching '"
                                   + pageMasterName + "' in page-sequence-master '"
                                   + masterName + "'");
        }
        return pageMaster;
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "page-sequence-master";
    }
    
    /**
     * {@inheritDoc}
     */
    public int getNameId() {
        return FO_PAGE_SEQUENCE_MASTER;
    }
}

