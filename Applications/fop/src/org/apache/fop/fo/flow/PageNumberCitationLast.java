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

/* $Id: PageNumberCitationLast.java 596097 2007-11-18 16:56:09Z jeremias $ */

package org.apache.fop.fo.flow;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;

/**
 * Class modelling the fo:page-number-citation-last object from XSL 1.1.
 * This inline fo is replaced with the text for a page number.
 * The page number used is the page that contains the end of the
 * block referenced with the ref-id attribute.
 * @since XSL 1.1
 */
public class PageNumberCitationLast extends AbstractPageNumberCitation {

    /**
     * Main constructor
     * @param parent the parent FO node
     */
    public PageNumberCitationLast(FONode parent) {
        super(parent);
    }
    
    /** {@inheritDoc} */
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startPageNumberCitationLast(this);
    }

    /** {@inheritDoc} */
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        getFOEventHandler().endPageNumberCitationLast(this);
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "page-number-citation-last";
    }
    
    /** {@inheritDoc} */
    public int getNameId() {
        return FO_PAGE_NUMBER_CITATION_LAST;
    }

}
