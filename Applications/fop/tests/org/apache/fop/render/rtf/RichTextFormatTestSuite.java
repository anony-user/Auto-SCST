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

/* $Id: RichTextFormatTestSuite.java 627367 2008-02-13 12:03:30Z maxberger $ */
 
package org.apache.fop.render.rtf;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for FOP's RTF library.
 */
public class RichTextFormatTestSuite {

    /**
     * Builds the test suite
     * @return the test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(
            "Test suite for RTF library");
        //$JUnit-BEGIN$
        suite.addTest(new TestSuite(Bug39607TestCase.class));
        //$JUnit-END$
        return suite;
    }
}
