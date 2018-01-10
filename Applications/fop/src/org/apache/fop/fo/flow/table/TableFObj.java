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

/* $Id: TableFObj.java 603962 2007-12-13 17:32:43Z vhennebert $ */

package org.apache.fop.fo.flow.table;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.ValidationPercentBaseContext;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.NumberProperty;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.layoutmgr.table.CollapsingBorderModel;

/**
 * Common base class for table-related FOs
 */
public abstract class TableFObj extends FObj {

    private Numeric borderAfterPrecedence;
    private Numeric borderBeforePrecedence;
    private Numeric borderEndPrecedence;
    private Numeric borderStartPrecedence;

    ConditionalBorder borderBefore;
    ConditionalBorder borderAfter;
    BorderSpecification borderStart;
    BorderSpecification borderEnd;

    CollapsingBorderModel collapsingBorderModel;

    /**
     * Main constructor
     * 
     * @param parent    the parent node
     */
    public TableFObj(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
        borderAfterPrecedence = pList.get(PR_BORDER_AFTER_PRECEDENCE).getNumeric();
        borderBeforePrecedence = pList.get(PR_BORDER_BEFORE_PRECEDENCE).getNumeric();
        borderEndPrecedence = pList.get(PR_BORDER_END_PRECEDENCE).getNumeric();
        borderStartPrecedence = pList.get(PR_BORDER_START_PRECEDENCE).getNumeric();
        if (getNameId() != FO_TABLE //Separate check for fo:table in Table.java
                && getNameId() != FO_TABLE_CELL
                && getCommonBorderPaddingBackground().hasPadding(
                        ValidationPercentBaseContext.getPseudoContext())) {
            attributeWarning(
                    "padding-* properties are not applicable to " + getName()
                    + ", but a non-zero value for padding was found.");
        }
    }

    /**
     * 
     * @param side  the side for which to return the border precedence
     * @return the "border-precedence" value for the given side
     */
    public Numeric getBorderPrecedence(int side) {
        switch (side) {
        case CommonBorderPaddingBackground.BEFORE:
            return borderBeforePrecedence;
        case CommonBorderPaddingBackground.AFTER:
            return borderAfterPrecedence;
        case CommonBorderPaddingBackground.START:
            return borderStartPrecedence;
        case CommonBorderPaddingBackground.END:
            return borderEndPrecedence;
        default:
            return null;
        }
    }

    /**
     * Convenience method to returns a reference
     * to the base Table instance
     * 
     * @return  the base table instance
     * 
     */
    public Table getTable() {
        // Will be overridden in Table; for any other Table-node, recursive call to
        // parent.getTable()
        return ((TableFObj) parent).getTable();
    }

    /**
     * @return the Common Border, Padding, and Background Properties.
     */
    public abstract CommonBorderPaddingBackground getCommonBorderPaddingBackground();

    /**
     * PropertyMaker subclass for the column-number property
     *
     */
    public static class ColumnNumberPropertyMaker extends NumberProperty.Maker {

        /**
         * Constructor
         * @param propId    the id of the property for which the maker should
         *                  be created
         */
        public ColumnNumberPropertyMaker(int propId) {
            super(propId);
        }

        /** {@inheritDoc} */
        public Property make(PropertyList propertyList)
                throws PropertyException {
            FObj fo = propertyList.getFObj();

            return NumberProperty.getInstance(((ColumnNumberManagerHolder) fo.getParent())
                    .getColumnNumberManager().getCurrentColumnNumber());
        }


        /**
         * Check the value of the column-number property.
         * Return the parent's column index (initial value) in case
         * of a negative or zero value
         * 
         * @see org.apache.fop.fo.properties.PropertyMaker#make(PropertyList, String, FObj)
         */
        public Property make(PropertyList propertyList, String value, FObj fo)
                    throws PropertyException {
            Property p = super.make(propertyList, value, fo);

            ColumnNumberManagerHolder parent
                    = (ColumnNumberManagerHolder) propertyList.getParentFObj();
            ColumnNumberManager columnIndexManager =  parent.getColumnNumberManager();
            int columnIndex = p.getNumeric().getValue();
            if (columnIndex <= 0) {
                log.warn("Specified negative or zero value for "
                        + "column-number on " + fo.getName() + ": "
                        + columnIndex + " forced to "
                        + columnIndexManager.getCurrentColumnNumber());
                return NumberProperty.getInstance(columnIndexManager.getCurrentColumnNumber());
            } else {
                double tmpIndex = p.getNumeric().getNumericValue();
                if (tmpIndex - columnIndex > 0.0) {
                    columnIndex = (int) Math.round(tmpIndex);
                    log.warn("Rounding specified column-number of "
                            + tmpIndex + " to " + columnIndex);
                    p = NumberProperty.getInstance(columnIndex);
                }
            }

            int colSpan = propertyList.get(Constants.PR_NUMBER_COLUMNS_SPANNED)
                                .getNumeric().getValue();
            int i = -1;
            while (++i < colSpan) {
                if (columnIndexManager.isColumnNumberUsed(columnIndex + i)) {
                    /* if column-number is already in use by another
                     * cell/column => error!
                     */
                    StringBuffer errorMessage = new StringBuffer();
                    errorMessage.append(fo.getName() + " overlaps in column ")
                           .append(columnIndex + i);
                    org.xml.sax.Locator loc = fo.getLocator();
                    if (loc != null && loc.getLineNumber() != -1) {
                        errorMessage.append(" (line #")
                            .append(loc.getLineNumber()).append(", column #")
                            .append(loc.getColumnNumber()).append(")");
                    }
                    throw new PropertyException(errorMessage.toString());
                }
            }

            return p;
        }
    }

    /** {@inheritDoc} */
    public void startOfNode() throws FOPException {
        super.startOfNode();
        Table table = getTable();
        if (!inMarker() && !table.isSeparateBorderModel()) {
            collapsingBorderModel = CollapsingBorderModel.getBorderModelFor(table
                    .getBorderCollapse());
            setCollapsedBorders();
        }
    }

    /*
     * TODO made public so that RetrieveMarker can access it.
     */
    /** {@inheritDoc} */
    public void endOfNode() throws FOPException {
        super.endOfNode();
    }

    /**
     * Prepares the borders of this element if the collapsing-border model is in use.
     * Conflict resolution with parent elements is done where applicable.
     */
    protected abstract void setCollapsedBorders();

    /**
     * Creates a BorderSpecification from the border set on the given side. If no border
     * is set, a BorderSpecification with border-style none is created.
     * 
     * @param side one of CommonBorderPaddingBackground.BEFORE|AFTER|START|END
     */
    protected void createBorder(int side) {
        BorderSpecification borderSpec = new BorderSpecification(
                getCommonBorderPaddingBackground().getBorderInfo(side), getNameId());
        switch (side) {
        case CommonBorderPaddingBackground.BEFORE:
            borderBefore = new ConditionalBorder(borderSpec, collapsingBorderModel);
            break;
        case CommonBorderPaddingBackground.AFTER:
            borderAfter = new ConditionalBorder(borderSpec, collapsingBorderModel);
            break;
        case CommonBorderPaddingBackground.START:
            borderStart = borderSpec;
            break;
        case CommonBorderPaddingBackground.END:
            borderEnd = borderSpec;
            break;
        default: assert false;
        }
    }

    /**
     * Creates a BorderSpecification from the border set on the given side, performing
     * conflict resolution with the same border on the given object.
     * 
     * @param side one of CommonBorderPaddingBackground.BEFORE|AFTER|START|END
     * @param competitor a parent table element whose side coincides with the given side
     * on this element
     */
    protected void createBorder(int side, TableFObj competitor) {
        createBorder(side);
        switch (side) {
        case CommonBorderPaddingBackground.BEFORE:
            borderBefore.integrateSegment(competitor.borderBefore, true, true, true);
            break;
        case CommonBorderPaddingBackground.AFTER:
            borderAfter.integrateSegment(competitor.borderAfter, true, true, true);
            break;
        case CommonBorderPaddingBackground.START:
            borderStart = collapsingBorderModel.determineWinner(borderStart,
                    competitor.borderStart);
            break;
        case CommonBorderPaddingBackground.END:
            borderEnd = collapsingBorderModel.determineWinner(borderEnd,
                    competitor.borderEnd);
            break;
        default: assert false;
        }
    }
}
