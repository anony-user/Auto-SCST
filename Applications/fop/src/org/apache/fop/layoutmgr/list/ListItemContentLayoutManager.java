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

/* $Id: ListItemContentLayoutManager.java 635508 2008-03-10 10:06:37Z jeremias $ */
 
package org.apache.fop.layoutmgr.list;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.fo.flow.AbstractListItemPart;
import org.apache.fop.fo.flow.ListItemBody;
import org.apache.fop.fo.flow.ListItemLabel;
import org.apache.fop.layoutmgr.BlockLevelLayoutManager;
import org.apache.fop.layoutmgr.BlockStackingLayoutManager;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.NonLeafPosition;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.SpaceResolver.SpaceHandlingBreakPosition;
import org.apache.fop.layoutmgr.TraitSetter;

/**
 * LayoutManager for a list-item-label or list-item-body FO.
 */
public class ListItemContentLayoutManager extends BlockStackingLayoutManager {

    private Block curBlockArea;

    private int xoffset;
    private int itemIPD;

    private static class StackingIter extends PositionIterator {
        StackingIter(Iterator parentIter) {
            super(parentIter);
        }

        protected LayoutManager getLM(Object nextObj) {
            return ((Position) nextObj).getLM();
        }

        protected Position getPos(Object nextObj) {
            return ((Position) nextObj);
        }
    }

    /**
     * Create a new Cell layout manager.
     * @param node list-item-label node
     */
    public ListItemContentLayoutManager(ListItemLabel node) {
        super(node);
    }

    /**
     * Create a new Cell layout manager.
     * @param node list-item-body node
     */
    public ListItemContentLayoutManager(ListItemBody node) {
        super(node);
    }

    /**
     * Convenience method.
     * @return the ListBlock node
     */
    protected AbstractListItemPart getPartFO() {
        return (AbstractListItemPart)fobj;
    }
    
    /**
     * Set the x offset of this list item.
     * This offset is used to set the absolute position
     * of the list item within the parent block area.
     *
     * @param off the x offset
     */
    public void setXOffset(int off) {
        xoffset = off;
    }

    /** {@inheritDoc} */
    public LinkedList getChangedKnuthElements(List oldList, int alignment) {
        //log.debug("  ListItemContentLayoutManager.getChanged>");
        return super.getChangedKnuthElements(oldList, alignment);
    }

    /**
     * Add the areas for the break points.
     * The list item contains block stacking layout managers
     * that add block areas.
     *
     * @param parentIter the iterator of the break positions
     * @param layoutContext the layout context for adding the areas
     */
    public void addAreas(PositionIterator parentIter,
                         LayoutContext layoutContext) {
        getParentArea(null);
        
        getPSLM().addIDToPage(getPartFO().getId());

        LayoutManager childLM = null;
        LayoutContext lc = new LayoutContext(0);
        LayoutManager firstLM = null;
        LayoutManager lastLM = null;
        Position firstPos = null;
        Position lastPos = null;

        // "unwrap" the NonLeafPositions stored in parentIter
        // and put them in a new list; 
        LinkedList positionList = new LinkedList();
        Position pos;
        while (parentIter.hasNext()) {
            pos = (Position)parentIter.next();
            if (pos == null) {
                continue;
            }
            if (pos.getIndex() >= 0) {
                if (firstPos == null) {
                    firstPos = pos;
                }
                lastPos = pos;
            }
            if (pos instanceof NonLeafPosition) {
                // pos was created by a child of this ListBlockLM
                positionList.add(((NonLeafPosition) pos).getPosition());
                lastLM = ((NonLeafPosition) pos).getPosition().getLM();
                if (firstLM == null) {
                    firstLM = lastLM;
                }
            } else if (pos instanceof SpaceHandlingBreakPosition) {
                positionList.add(pos);
            } else {
                // pos was created by this ListBlockLM, so it must be ignored
            }
        }

        addMarkersToPage(true, isFirst(firstPos), isLast(lastPos));
        
        StackingIter childPosIter = new StackingIter(positionList.listIterator());
        while ((childLM = childPosIter.getNextChildLM()) != null) {
            // Add the block areas to Area
            lc.setFlags(LayoutContext.FIRST_AREA, childLM == firstLM);
            lc.setFlags(LayoutContext.LAST_AREA, childLM == lastLM);
            // set the space adjustment ratio
            lc.setSpaceAdjust(layoutContext.getSpaceAdjust());
            lc.setStackLimitBP(layoutContext.getStackLimitBP());
            childLM.addAreas(childPosIter, lc);
        }

        addMarkersToPage(false, isFirst(firstPos), isLast(lastPos));
        
        flush();

        curBlockArea = null;
        
        getPSLM().notifyEndOfLayout(((AbstractListItemPart)getFObj()).getId());
    }

    /**
     * Return an Area which can contain the passed childArea. The childArea
     * may not yet have any content, but it has essential traits set.
     * In general, if the LayoutManager already has an Area it simply returns
     * it. Otherwise, it makes a new Area of the appropriate class.
     * It gets a parent area for its area by calling its parent LM.
     * Finally, based on the dimensions of the parent area, it initializes
     * its own area. This includes setting the content IPD and the maximum
     * BPD.
     *
     * @param childArea the child area to get the parent for
     * @return the parent area
     */
    public Area getParentArea(Area childArea) {
        if (curBlockArea == null) {
            curBlockArea = new Block();
            curBlockArea.setPositioning(Block.ABSOLUTE);
            // set position
            curBlockArea.setXOffset(xoffset);
            curBlockArea.setIPD(itemIPD);
            //curBlockArea.setHeight();

            TraitSetter.setProducerID(curBlockArea, getPartFO().getId());
            
            // Set up dimensions
            Area parentArea = parentLM.getParentArea(curBlockArea);
            int referenceIPD = parentArea.getIPD();
            curBlockArea.setIPD(referenceIPD);
            // Get reference IPD from parentArea
            setCurrentArea(curBlockArea); // ??? for generic operations
        }
        return curBlockArea;
    }

    /**
     * Add the child to the list item area.
     *
     * @param childArea the child to add to the cell
     */
    public void addChildArea(Area childArea) {
        if (curBlockArea != null) {
            curBlockArea.addBlock((Block) childArea);
        }
    }

    /** {@inheritDoc} */
    public boolean mustKeepTogether() {
        //TODO Keeps will have to be more sophisticated sooner or later
        return ((BlockLevelLayoutManager)getParent()).mustKeepTogether() 
                || !getPartFO().getKeepTogether().getWithinPage().isAuto()
                || !getPartFO().getKeepTogether().getWithinColumn().isAuto();
    }

}

