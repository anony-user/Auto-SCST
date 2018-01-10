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

/* $Id: TableFooter.java 594571 2007-11-13 16:24:32Z vhennebert $ */

package org.apache.fop.fo.flow.table;

// FOP
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;


/**
 * Class modelling the fo:table-footer object.
 */
public class TableFooter extends TableBody {

    /**
     * @param parent FONode that is the parent of this object
     */
    public TableFooter(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    public void startOfNode() throws FOPException {
        super.startOfNode();
        //getFOEventHandler().startBody(this);
    }

    /**
     * {@inheritDoc}
     */
    public void endOfNode() throws FOPException {
//      getFOEventHandler().endFooter(this);
        if (!(tableRowsFound || tableCellsFound)) {
            missingChildElementError("marker* (table-row+|table-cell+)");
        } else {
            finishLastRowGroup();
        }
//      convertCellsToRows();
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "table-footer";
    }

    /** {@inheritDoc} */
    public int getNameId() {
        return FO_TABLE_FOOTER;
    }

    /** {@inheritDoc} */
    protected boolean isTableFooter() {
        return true;
    }
}
