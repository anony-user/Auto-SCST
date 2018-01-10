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

/* $Id: TableColumn.java 594571 2007-11-13 16:24:32Z vhennebert $ */

package org.apache.fop.fo.flow.table;

// XML
import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.TableColLength;
import org.apache.fop.layoutmgr.table.CollapsingBorderModel;
import org.xml.sax.Locator;

/**
 * Class modelling the fo:table-column object.
 */
public class TableColumn extends TableFObj {
    // The value of properties relevant for fo:table-column.
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private int columnNumber;
    private Length columnWidth;
    private int numberColumnsRepeated;
    private int numberColumnsSpanned;
    // Unused but valid items, commented out for performance:
    //     private int visibility;
    // End of property values

    private boolean implicitColumn;
    private PropertyList pList = null;

    /**
     * @param parent FONode that is the parent of this object
     */
    public TableColumn(FONode parent) {
        this(parent, false);
    }

    /**
     * @param parent FONode that is the parent of this object
     * @param implicit true if this table-column has automatically been created (does not
     * correspond to an explicit fo:table-column in the input document)
     */
    public TableColumn(FONode parent, boolean implicit) {
        super(parent);
        this.implicitColumn = implicit;
    }


    /**
     * {@inheritDoc}
     */
    public void bind(PropertyList pList) throws FOPException {
        commonBorderPaddingBackground = pList.getBorderPaddingBackgroundProps();
        columnNumber = pList.get(PR_COLUMN_NUMBER).getNumeric().getValue();
        columnWidth = pList.get(PR_COLUMN_WIDTH).getLength();
        numberColumnsRepeated = pList.get(PR_NUMBER_COLUMNS_REPEATED)
                                    .getNumeric().getValue();
        numberColumnsSpanned = pList.get(PR_NUMBER_COLUMNS_SPANNED)
                                    .getNumeric().getValue();
        super.bind(pList);

        if (numberColumnsRepeated <= 0) {
            throw new PropertyException("number-columns-repeated must be 1 or bigger, "
                    + "but got " + numberColumnsRepeated);
        }
        if (numberColumnsSpanned <= 0) {
            throw new PropertyException("number-columns-spanned must be 1 or bigger, "
                    + "but got " + numberColumnsSpanned);
        }

        /* check for unspecified width and replace with default of
         * proportional-column-width(1), in case of fixed table-layout
         * warn only for explicit columns
         */
        if (columnWidth.getEnum() == EN_AUTO) {
            if (!this.implicitColumn && !getTable().isAutoLayout()) {
                log.warn("table-layout=\"fixed\" and column-width unspecified "
                        + "=> falling back to proportional-column-width(1)");
            }
            columnWidth = new TableColLength(1.0, this);
        }

        /* in case of explicit columns, from-table-column()
         * can be used on descendants of the table-cells, so
         * we need a reference to the column's property list
         * (cleared in Table.endOfNode())
         */
        if (!this.implicitColumn) {
            this.pList = pList;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startColumn(this);
    }

    void setCollapsedBorders(CollapsingBorderModel collapsingBorderModel) {
        this.collapsingBorderModel = collapsingBorderModel;
        setCollapsedBorders();
    }

    /** {@inheritDoc} */
    protected void setCollapsedBorders() {
        Table table = (Table) parent;
        createBorder(CommonBorderPaddingBackground.BEFORE, table);
        createBorder(CommonBorderPaddingBackground.AFTER, table);
        createBorder(CommonBorderPaddingBackground.START);
        createBorder(CommonBorderPaddingBackground.END);
    }

    /** {@inheritDoc} */
    public void endOfNode() throws FOPException {
        getFOEventHandler().endColumn(this);
    }

    /**
     * {@inheritDoc}
     * XSL Content Model: empty
     */
    protected void validateChildNode(Locator loc,
                        String nsURI, String localName)
        throws ValidationException {
            invalidChildError(loc, nsURI, localName);
    }

    /**
     * @return the Common Border, Padding, and Background Properties.
     */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return commonBorderPaddingBackground;
    }

    /**
     * @return the "column-width" property.
     */
    public Length getColumnWidth() {
        return columnWidth;
    }

    /**
     * Sets the column width.
     * @param columnWidth the column width
     */
    public void setColumnWidth(Length columnWidth) {
        this.columnWidth = columnWidth;
    }

    /**
     * @return the "column-number" property.
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Used for setting the column-number for an implicit column
     * @param columnNumber
     */
    protected void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    /** @return value for number-columns-repeated. */
    public int getNumberColumnsRepeated() {
        return numberColumnsRepeated;
    }

    /** @return value for number-columns-spanned. */
    public int getNumberColumnsSpanned() {
        return numberColumnsSpanned;
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "table-column";
    }

    /** {@inheritDoc} */
    public int getNameId() {
        return FO_TABLE_COLUMN;
    }

    /**
     * Indicates whether this table-column has been created as
     * default column for this table in case no table-columns
     * have been defined.
     * Note that this only used to provide better
     * user feedback (see ColumnSetup).
     * @return true if this table-column has been created as default column
     */
    public boolean isImplicitColumn() {
        return implicitColumn;
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuffer sb = new StringBuffer("fo:table-column");
        sb.append(" column-number=").append(getColumnNumber());
        if (getNumberColumnsRepeated() > 1) {
            sb.append(" number-columns-repeated=")
                .append(getNumberColumnsRepeated());
        }
        if (getNumberColumnsSpanned() > 1) {
            sb.append(" number-columns-spanned=")
                .append(getNumberColumnsSpanned());
        }
        sb.append(" column-width=").append(getColumnWidth());
        return sb.toString();
    }

    /**
     * Retrieve a property value through its Id; used by
     * from-table-column() function
     * 
     * @param propId    the id for the property to retrieve
     * @return the requested Property
     * @throws PropertyException
     */
    public Property getProperty(int propId) throws PropertyException {
        return this.pList.get(propId);
    }

    /**
     * Clear the reference to the PropertyList (retained for
     * from-table-column())
     */
    protected void releasePropertyList() {
        this.pList = null;
    }

}
