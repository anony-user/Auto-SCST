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

/* $Id: FOProcessorImpl.java 627367 2008-02-13 12:03:30Z maxberger $ */

package org.apache.fop.threading;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

/**
 * Default implementation of the FOProcessor interface using FOP.
 */
public class FOProcessorImpl extends AbstractLogEnabled
            implements FOProcessor, Configurable, Initializable {

    private FopFactory fopFactory = FopFactory.newInstance();
    private TransformerFactory factory = TransformerFactory.newInstance();
    private String userconfig;
    private String mime;
    private String fileExtension;

    /** {@inheritDoc} */
    public void configure(Configuration configuration) throws ConfigurationException {
        this.userconfig = configuration.getChild("userconfig").getValue(null);
        this.mime = configuration.getChild("mime").getValue(MimeConstants.MIME_PDF);
        this.fileExtension = configuration.getChild("extension").getValue(".pdf");
    }

    /** {@inheritDoc} */
    public void initialize() throws Exception {
        if (this.userconfig != null) {
            getLogger().debug("Setting user config: " + userconfig);
            fopFactory.setUserConfig(this.userconfig);
        }
    }

    /** {@inheritDoc} */
    public void process(InputStream in, Templates templates, OutputStream out) 
                throws org.apache.fop.apps.FOPException, java.io.IOException {
        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        Fop fop = fopFactory.newFop(this.mime, foUserAgent, out);

        try {
            Transformer transformer;
            if (templates == null) {
                transformer = factory.newTransformer();
            } else {
                transformer = templates.newTransformer();
            }
            Source src = new StreamSource(in);
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, res);
        } catch (TransformerException e) {
            throw new FOPException(e);
        }
    }

    /** {@inheritDoc} */
    public String getTargetFileExtension() {
        return this.fileExtension;
    }

}