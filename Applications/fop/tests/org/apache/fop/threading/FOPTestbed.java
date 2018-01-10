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

/* $Id: FOPTestbed.java 627367 2008-02-13 12:03:30Z maxberger $ */

package org.apache.fop.threading;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.framework.activity.Executable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.io.IOUtils;

/**
 * Testbed for multi-threading tests. The class can run a configurable set of task a number of
 * times in a configurable number of threads to easily reproduce multi-threading issues.
 */
public class FOPTestbed extends AbstractLogEnabled 
            implements Configurable, Initializable {

    private int repeat;
    private List taskList = new java.util.ArrayList();
    private int threads;
    private File outputDir;
    private Configuration fopCfg;
    private FOProcessor foprocessor;

    private int counter = 0;
    
    /** {@inheritDoc} */
    public void configure(Configuration configuration) throws ConfigurationException {
        this.threads = configuration.getChild("threads").getValueAsInteger(10);
        this.outputDir = new File(configuration.getChild("output-dir").getValue());
        Configuration tasks = configuration.getChild("tasks");
        this.repeat = tasks.getAttributeAsInteger("repeat", 1);
        Configuration[] entries = tasks.getChildren("task");
        for (int i = 0; i < entries.length; i++) {
            this.taskList.add(new TaskDef(entries[i]));
        }
        this.fopCfg = configuration.getChild("foprocessor");
    }

    /** {@inheritDoc} */
    public void initialize() throws Exception {
        this.foprocessor = createFOProcessor();
    }

    /**
     * Starts the stress test.
     */
    public void doStressTest() {
        getLogger().info("Starting stress test...");
        long start = System.currentTimeMillis();
        this.counter = 0;
        
        //Initialize threads
        List threadList = new java.util.LinkedList();
        for (int ti = 0; ti < this.threads; ti++) {
            TaskRunner runner = new TaskRunner();
            ContainerUtil.enableLogging(runner, getLogger());
            Thread thread = new Thread(runner);
            threadList.add(thread);
        }
        
        //Start threads
        Iterator i = threadList.iterator();
        while (i.hasNext()) {
            ((Thread)i.next()).start();
        }
        
        //Wait for threads to end
        while (threadList.size() > 0) {
            Thread t = (Thread)threadList.get(0);
            if (!t.isAlive()) {
                threadList.remove(0);
                continue;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                //ignore
            }
        }
        getLogger().info("Stress test duration: " + (System.currentTimeMillis() - start) + "ms");
    }

    private class TaskRunner extends AbstractLogEnabled implements Runnable {
        
        public void run() {
            try {
                for (int r = 0; r < repeat; r++) {
                    Iterator i = taskList.iterator();
                    while (i.hasNext()) {
                        TaskDef def = (TaskDef)i.next();
                        final Task task = new Task(def, counter++, foprocessor);
                        ContainerUtil.enableLogging(task, getLogger());
                        task.execute();
                    }
                }
            } catch (Exception e) {
                getLogger().error("Thread ended with an exception", e);
            }
        }

    }
    
    /**
     * Creates a new FOProcessor.
     * @return the newly created instance
     */
    public FOProcessor createFOProcessor() {
        try {
            Class clazz = Class.forName(this.fopCfg.getAttribute("class", 
                    "org.apache.fop.threading.FOProcessorImpl"));
            FOProcessor fop = (FOProcessor)clazz.newInstance();
            ContainerUtil.enableLogging(fop, getLogger());
            ContainerUtil.configure(fop, this.fopCfg);
            ContainerUtil.initialize(fop);
            return fop;
        } catch (Exception e) {
            throw new CascadingRuntimeException("Error creating FO Processor", e);
        }
    }
    

    private class TaskDef {
        private String fo;
        private String xml;
        private String xslt;
        private Templates templates;

        public TaskDef(String fo) {
            this.fo = fo;
        }

        public TaskDef(Configuration cfg) throws ConfigurationException {
            this.fo = cfg.getAttribute("fo", null);
            if (this.fo == null) {
                this.xml = cfg.getAttribute("xml");
                this.xslt = cfg.getAttribute("xslt");
                TransformerFactory factory = TransformerFactory.newInstance();
                Source xsltSource = new StreamSource(new File(xslt));
                try {
                    this.templates = factory.newTemplates(xsltSource);
                } catch (TransformerConfigurationException tce) {
                    throw new ConfigurationException("Invalid XSLT", tce);
                }
            }
        }

        public String getFO() {
            return this.fo;
        }

        public String getXML() {
            return this.xml;
        }

        public Templates getTemplates() {
            return this.templates;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.fo != null) {
                sb.append("fo=");
                sb.append(this.fo);
            } else {
                sb.append("xml=");
                sb.append(this.xml);
                sb.append(" xslt=");
                sb.append(this.xslt);
            }
            return sb.toString();
        }
    }


    private class Task extends AbstractLogEnabled implements Executable {

        private TaskDef def;
        private int num;
        private FOProcessor fop;

        public Task(TaskDef def, int num, FOProcessor fop) {
            this.def = def;
            this.num = num;
            this.fop = fop;
        }


        public void execute() throws Exception {
            getLogger().info("Processing: " + def);
            DecimalFormat df = new DecimalFormat("00000");
            File outfile = new File(outputDir, df.format(num) + fop.getTargetFileExtension());
            OutputStream out = new java.io.FileOutputStream(outfile);
            try {
                InputStream in;
                Templates templates;
                
                if (def.getFO() != null) {
                    in = new java.io.FileInputStream(new File(def.getFO()));
                    templates = null;
                } else {
                    in = new java.io.FileInputStream(new File(def.getXML()));
                    templates = def.getTemplates();
                }
                try {
                    fop.process(in, templates, out);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }

}