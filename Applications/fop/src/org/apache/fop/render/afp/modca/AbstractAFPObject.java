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

/* $Id: AbstractAFPObject.java 631178 2008-02-26 11:07:20Z jeremias $ */

package org.apache.fop.render.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the base class for all data stream objects. Page objects are
 * responsible for building and generating the binary datastream in an
 * AFP format.
 *
 */
public abstract class AbstractAFPObject {

    /**
     * Static logging instance
     */
    protected static final Log log = LogFactory.getLog("org.apache.fop.render.afp.modca");

    /**
     * DataStream objects must implement the writeDataStream()
     * method to write its data to the given OutputStream
     * @param os The outputsteam stream
     * @throws java.io.IOException
     */
    public abstract void writeDataStream(OutputStream os) throws IOException;

    /**
     * Help method to write a set of AFPObjects to the AFP datastream.
     * @param afpObjects a list of AFPObjects
     * @param os The stream to write to
     * @throws java.io.IOException
     */
    protected void writeObjectList(List afpObjects, OutputStream os)
        throws IOException {

        for (Iterator it = afpObjects.iterator(); it.hasNext(); ) {
            ((AbstractAFPObject)it.next()).writeDataStream(os);
        }

    }

}
