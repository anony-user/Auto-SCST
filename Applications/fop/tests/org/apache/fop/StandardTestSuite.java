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

/* $Id: StandardTestSuite.java 647403 2008-04-12 09:02:01Z jeremias $ */
 
package org.apache.fop;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.fop.fonts.TrueTypeAnsiTestCase;
import org.apache.fop.render.pdf.PDFAConformanceTestCase;
import org.apache.fop.render.pdf.PDFCMapTestCase;
import org.apache.fop.render.pdf.PDFEncodingTestCase;
import org.apache.fop.render.pdf.PDFsRGBSettingsTestCase;
import org.apache.fop.render.rtf.RichTextFormatTestSuite;

/**
 * Test suite for basic functionality of FOP.
 */
public class StandardTestSuite {

    /**
     * Builds the test suite
     * @return the test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(
            "Basic functionality test suite for FOP");
        //$JUnit-BEGIN$
        suite.addTest(BasicDriverTestSuite.suite());
        suite.addTest(UtilityCodeTestSuite.suite());
        suite.addTest(new TestSuite(PDFAConformanceTestCase.class));
        suite.addTest(new TestSuite(PDFEncodingTestCase.class));
        suite.addTest(new TestSuite(PDFCMapTestCase.class));
        suite.addTest(new TestSuite(PDFsRGBSettingsTestCase.class));
        suite.addTest(new TestSuite(TrueTypeAnsiTestCase.class));
        suite.addTest(RichTextFormatTestSuite.suite());
        //$JUnit-END$
        return suite;
    }
}
