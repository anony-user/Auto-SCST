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

/* $Id: PageProvider.java 607032 2007-12-27 10:34:15Z jeremias $ */

package org.apache.fop.layoutmgr;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOPException;
import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.Region;
import org.apache.fop.fo.pagination.SimplePageMaster;

/**
 * <p>This class delivers Page instances. It also caches them as necessary.
 * </p>
 * <p>Additional functionality makes sure that surplus instances that are requested by the
 * page breaker are properly discarded, especially in situations where hard breaks cause
 * blank pages. The reason for that: The page breaker sometimes needs to preallocate 
 * additional pages since it doesn't know exactly until the end how many pages it really needs.
 * </p>
 */
public class PageProvider implements Constants {
    
    private Log log = LogFactory.getLog(PageProvider.class);

    /** Indices are evaluated relative to the first page in the page-sequence. */
    public static final int RELTO_PAGE_SEQUENCE = 0;
    /** Indices are evaluated relative to the first page in the current element list. */
    public static final int RELTO_CURRENT_ELEMENT_LIST = 1;
    
    private int startPageOfPageSequence;
    private int startPageOfCurrentElementList;
    private int startColumnOfCurrentElementList;
    private List cachedPages = new java.util.ArrayList();
    
    private int lastPageIndex = -1;
    private int indexOfCachedLastPage = -1;
    
    //Cache to optimize getAvailableBPD() calls
    private int lastRequestedIndex = -1;
    private int lastReportedBPD = -1;

    /** 
     * AreaTreeHandler which activates the PSLM and controls
     * the rendering of its pages.
     */
    private AreaTreeHandler areaTreeHandler;

    /** 
     * fo:page-sequence formatting object being
     * processed by this class
     */
    private PageSequence pageSeq;

    /**
     * Main constructor.
     * @param ps The page-sequence the provider operates on
     */
    public PageProvider(AreaTreeHandler ath, PageSequence ps) {
        this.areaTreeHandler = ath;
        this.pageSeq = ps;
        this.startPageOfPageSequence = ps.getStartingPageNumber();
    }
    
    /**
     * The page breaker notifies the provider about the page number an element list starts
     * on so it can later retrieve PageViewports relative to this first page.
     * @param startPage the number of the first page for the element list.
     * @param startColumn the starting column number for the element list. 
     */
    public void setStartOfNextElementList(int startPage, int startColumn) {
        log.debug("start of the next element list is:"
                + " page=" + startPage + " col=" + startColumn);
        this.startPageOfCurrentElementList = startPage - startPageOfPageSequence + 1;
        this.startColumnOfCurrentElementList = startColumn;
        //Reset Cache
        this.lastRequestedIndex = -1;
        this.lastReportedBPD = -1;
    }
    
    /**
     * Sets the index of the last page. This is done as soon as the position of the last page
     * is known or assumed.
     * @param index the index relative to the first page in the page-sequence
     */
    public void setLastPageIndex(int index) {
        this.lastPageIndex = index;
    }
    
    /**
     * Returns the available BPD for the part/page indicated by the index parameter.
     * The index is the part/page relative to the start of the current element list.
     * This method takes multiple columns into account.
     * @param index zero-based index of the requested part/page
     * @return the available BPD
     */
    public int getAvailableBPD(int index) {
        //Special optimization: There may be many equal calls by the BreakingAlgorithm
        if (this.lastRequestedIndex == index) {
            if (log.isTraceEnabled()) {
                log.trace("getAvailableBPD(" + index + ") -> (cached) " + lastReportedBPD);
            }
            return this.lastReportedBPD;
        }
        int c = index;
        int pageIndex = 0;
        int colIndex = startColumnOfCurrentElementList;
        Page page = getPage(
                false, pageIndex, RELTO_CURRENT_ELEMENT_LIST);
        while (c > 0) {
            colIndex++;
            if (colIndex >= page.getPageViewport().getCurrentSpan().getColumnCount()) {
                colIndex = 0;
                pageIndex++;
                page = getPage(
                        false, pageIndex, RELTO_CURRENT_ELEMENT_LIST);
            }
            c--;
        }
        this.lastRequestedIndex = index;
        this.lastReportedBPD = page.getPageViewport().getBodyRegion().getRemainingBPD();
        if (log.isTraceEnabled()) {
            log.trace("getAvailableBPD(" + index + ") -> " + lastReportedBPD);
        }
        return this.lastReportedBPD;
    }
    
    /**
     * Returns the part index (0<x<partCount) which denotes the first part on the last page 
     * generated by the current element list.
     * @param partCount Number of parts determined by the breaking algorithm
     * @return the requested part index
     */
    public int getStartingPartIndexForLastPage(int partCount) {
        int result = 0;
        int idx = 0;
        int pageIndex = 0;
        int colIndex = startColumnOfCurrentElementList;
        Page page = getPage(
                false, pageIndex, RELTO_CURRENT_ELEMENT_LIST);
        while (idx < partCount) {
            if ((colIndex >= page.getPageViewport().getCurrentSpan().getColumnCount())) {
                colIndex = 0;
                pageIndex++;
                page = getPage(
                        false, pageIndex, RELTO_CURRENT_ELEMENT_LIST);
                result = idx;
            }
            colIndex++;
            idx++;
        }
        return result;
    }

    /**
     * Returns a Page.
     * @param isBlank true if this page is supposed to be blank.
     * @param index Index of the page (see relativeTo)
     * @param relativeTo Defines which value the index parameter should be evaluated relative 
     * to. (One of PageProvider.RELTO_*)
     * @return the requested Page
     */
    public Page getPage(boolean isBlank, int index, int relativeTo) {
        if (relativeTo == RELTO_PAGE_SEQUENCE) {
            return getPage(isBlank, index);
        } else if (relativeTo == RELTO_CURRENT_ELEMENT_LIST) {
            int effIndex = startPageOfCurrentElementList + index;
            effIndex += startPageOfPageSequence - 1;
            return getPage(isBlank, effIndex);
        } else {
            throw new IllegalArgumentException(
                    "Illegal value for relativeTo: " + relativeTo);
        }
    }
    
    /**
     * 
     * @param isBlank   true if the Page should be a blank one
     * @param index the Page's index
     * @return  a Page instance
     */
    protected Page getPage(boolean isBlank, int index) {
        boolean isLastPage = (lastPageIndex >= 0) && (index == lastPageIndex);
        if (log.isTraceEnabled()) {
            log.trace("getPage(" + index + " " + (isBlank ? "blank" : "non-blank") 
                    + (isLastPage ? " <LAST>" : "") + ")");
        }
        int intIndex = index - startPageOfPageSequence;
        if (log.isTraceEnabled()) {
            if (isBlank) {
                log.trace("blank page requested: " + index);
            }
            if (isLastPage) {
                log.trace("last page requested: " + index);
            }
        }
        while (intIndex >= cachedPages.size()) {
            if (log.isTraceEnabled()) {
                log.trace("Caching " + index);
            }
            cacheNextPage(index, isBlank, isLastPage);
        }
        Page page = (Page)cachedPages.get(intIndex);
        boolean replace = false;
        if (page.getPageViewport().isBlank() != isBlank) {
            log.debug("blank condition doesn't match. Replacing PageViewport.");
            replace = true;
        }
        if ((isLastPage && indexOfCachedLastPage != intIndex)
                || (!isLastPage && indexOfCachedLastPage >= 0)) {
            log.debug("last page condition doesn't match. Replacing PageViewport.");
            replace = true;
            indexOfCachedLastPage = (isLastPage ? intIndex : -1);
        }
        if (replace) {
            disardCacheStartingWith(intIndex);
            page = cacheNextPage(index, isBlank, isLastPage);
        }
        return page;
    }

    private void disardCacheStartingWith(int index) {
        while (index < cachedPages.size()) {
            this.cachedPages.remove(cachedPages.size() - 1);
            if (!pageSeq.goToPreviousSimplePageMaster()) {
                log.warn("goToPreviousSimplePageMaster() on the first page called!");
            }
        }
    }
    
    private Page cacheNextPage(int index, boolean isBlank, boolean isLastPage) {
        try {
            String pageNumberString = pageSeq.makeFormattedPageNumber(index);
            SimplePageMaster spm = pageSeq.getNextSimplePageMaster(
                    index, (startPageOfPageSequence == index), isLastPage, false, isBlank);
                
            Region body = spm.getRegion(FO_REGION_BODY);
            if (!pageSeq.getMainFlow().getFlowName().equals(body.getRegionName())) {
                // this is fine by the XSL Rec (fo:flow's flow-name can be mapped to
                // any region), but we don't support it yet.
                throw new FOPException("Flow '" + pageSeq.getMainFlow().getFlowName()
                    + "' does not map to the region-body in page-master '"
                    + spm.getMasterName() + "'.  FOP presently "
                    + "does not support this.");
            }
            Page page = new Page(spm, index, pageNumberString, isBlank);
            //Set unique key obtained from the AreaTreeHandler
            page.getPageViewport().setKey(areaTreeHandler.generatePageViewportKey());
            page.getPageViewport().setForeignAttributes(spm.getForeignAttributes());
            cachedPages.add(page);
            return page;
        } catch (FOPException e) {
            //TODO Maybe improve. It'll mean to propagate this exception up several
            //methods calls.
            throw new IllegalStateException(e.getMessage());
        }
    }
    
}