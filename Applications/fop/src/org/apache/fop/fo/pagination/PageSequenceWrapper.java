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

/* $Id: PageSequenceWrapper.java 627367 2008-02-13 12:03:30Z maxberger $ */

package org.apache.fop.fo.pagination;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;

/**
 * The fo:page-sequence-wrapper formatting object, first introduced
 * in the XSL 1.1 WD.  Prototype version only, subject to change as
 * XSL 1.1 WD evolves.
 */
public class PageSequenceWrapper extends FObj {
    // The value of properties relevant for this FO
    private String indexClass;
    private String indexKey;
    // End of property values
    
    /**
     * @param parent FONode that is the parent of this object
     */
    public PageSequenceWrapper(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
        indexClass = pList.get(PR_INDEX_CLASS).getString();
        indexKey = pList.get(PR_INDEX_KEY).getString();
    }

    /**
     * {@inheritDoc}
        XSL/FOP: (bookmark+)
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
        if (!(FO_URI.equals(nsURI) && (localName.equals("page-sequence") || 
            localName.equals("page-sequence-wrapper")))) {
                invalidChildError(loc, nsURI, localName);
        }
    }

    /** @return the "index-class" property. */
    public String getIndexClass() {
        return indexClass;
    }

    /** @return the "index-key" property. */
    public String getIndexKey() {
        return indexKey;
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "page-sequence-wrapper";
    }

    /**
     * {@inheritDoc}
     */
    public int getNameId() {
        return FO_PAGE_SEQUENCE_WRAPPER;
    }
}

