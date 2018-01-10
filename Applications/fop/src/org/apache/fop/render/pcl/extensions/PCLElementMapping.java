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

/* $Id: PCLElementMapping.java 627367 2008-02-13 12:03:30Z maxberger $ */

package org.apache.fop.render.pcl.extensions;

import java.util.HashMap;

import org.apache.fop.fo.ElementMapping;

/**
 * PCL-specific extensions for Apache FOP.
 */
public class PCLElementMapping extends ElementMapping {

    /** The namespace used for PCL extensions */
    public static final String NAMESPACE = "http://xmlgraphics.apache.org/fop/extensions/pcl";

    /** The usual namespace prefix used for PCL extensions */
    public static final String NAMESPACE_PREFIX = "pcl";

    /** Main constructor */
    public PCLElementMapping() {
        this.namespaceURI = NAMESPACE;
    }

    /** {@inheritDoc} */
    protected void initialize() {

        if (foObjs == null) {
            foObjs = new HashMap();
            //No extension elements, yet, only attributes
        }

    }

}
