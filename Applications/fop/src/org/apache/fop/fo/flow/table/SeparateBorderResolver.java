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

/* $Id: SeparateBorderResolver.java 603975 2007-12-13 18:52:48Z vhennebert $ */

package org.apache.fop.fo.flow.table;

import java.util.List;

/**
 * A resolver for the separate-border model. Basically this class does nothing.
 */
class SeparateBorderResolver implements BorderResolver {

    /** {@inheritDoc} */
    public void endRow(List row, TableCellContainer container) {
    }

    /** {@inheritDoc} */
    public void startPart(TableBody part) {
    }

    /** {@inheritDoc} */
    public void endPart() {
    }

    /** {@inheritDoc} */
    public void endTable() {
    }
}
