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

/* $Id: AbstractPageObject.java 631178 2008-02-26 11:07:20Z jeremias $ */

package org.apache.fop.render.afp.modca;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.apache.fop.render.afp.fonts.AFPFont;
import org.apache.fop.render.afp.tools.StringUtils;

/**
 * Pages contain the data objects that comprise a presentation document. Each
 * page has a set of data objects associated with it. Each page within a
 * document is independent from any other page, and each must establish its own
 * environment parameters.
 *
 * The page is the level in the document component hierarchy that is used for
 * printing or displaying a document's content. The data objects contained in
 * the page envelope in the data stream are presented when the page is
 * presented. Each data object has layout information associated with it that
 * directs the placement and orientation of the data on the page. In addition,
 * each page contains layout information that specifies the measurement units,
 * page width, and page depth.
 *
 * A page is initiated by a begin page structured field and terminated by an end
 * page structured field. Structured fields that define objects and active
 * environment groups or that specify attributes of the page may be encountered
 * in page state.
 *
 */
public abstract class AbstractPageObject extends AbstractNamedAFPObject {

    /**
     * The active environment group for the page
     */
    protected ActiveEnvironmentGroup activeEnvironmentGroup = null;

    /**
     * The presentation text object, we only have one per page
     */
    private PresentationTextObject presentationTextObject = null;

    /**
     * The list of objects within the page
     */
    protected List objects = new ArrayList();

    /**
     * The list of tag logical elements
     */
    protected ArrayList tagLogicalElements = new ArrayList();

    /**
     * The list of the include page segments
     */
    protected ArrayList segments = new ArrayList();

    /**
     * The page width
     */
    private int width;

    /**
     * The page height
     */
    private int height;

    /**
     * The page rotation
     */
    private int rotation = 0;

    /**
     * The page state
     */
    private boolean complete = false;

    /**
     * Construct a new page object for the specified name argument, the page
     * name should be an 8 character identifier.
     *
     * @param name
     *            the name of the page.
     * @param width
     *            the width of the page.
     * @param height
     *            the height of the page.
     * @param rotation
     *            the rotation of the page.
     * @param widthResolution
     *            the width resolution of the page.
     * @param heightResolution
     *            the height resolution of the page.
     */
    public AbstractPageObject(String name, int width, int height, int rotation,
            int widthResolution, int heightResolution) {

        super(name);
        this.width = width;
        this.height = height;
        this.rotation = rotation;

        /**
         * Every page object must have an ActiveEnvironmentGroup
         */
        activeEnvironmentGroup = new ActiveEnvironmentGroup(width, height,
                widthResolution, heightResolution);

        if (rotation != 0) {
            switch (rotation) {
                case 90:
                    activeEnvironmentGroup.setPosition(width, 0, rotation);
                    break;
                case 180:
                    activeEnvironmentGroup.setPosition(width, height, rotation);
                    break;
                case 270:
                    activeEnvironmentGroup.setPosition(0, height, rotation);
                    break;
                default:
            }
        }

        /**
         * We have a presentation text object per page
         */
        presentationTextObject = new PresentationTextObject();
        objects.add(presentationTextObject);

    }

    /**
     * Helper method to create a map coded font object on the current page, this
     * method delegates the construction of the map coded font object to the
     * active environment group on the page.
     *
     * @param fontReference
     *            the font number used as the resource identifier
     * @param font
     *            the font
     * @param size
     *            the point size of the font
     */
    public void createFont(byte fontReference, AFPFont font, int size) {

        activeEnvironmentGroup.createFont(fontReference, font, size, 0);

    }

    /**
     * Helper method to create a line on the current page, this method delegates
     * to the presentation text object in order to construct the line.
     *
     * @param x1
     *            the first x coordinate of the line
     * @param y1
     *            the first y coordinate of the line
     * @param x2
     *            the second x coordinate of the line
     * @param y2
     *            the second y coordinate of the line
     * @param thickness
     *            the thickness of the line
     * @param lineRotation
     *            the rotation of the line
     * @param col
     *            The text color.
     */
    public void createLine(int x1, int y1, int x2, int y2, int thickness,
            int lineRotation, Color col) {

        if (presentationTextObject == null) {
            presentationTextObject = new PresentationTextObject();
            objects.add(presentationTextObject);
        }
        presentationTextObject.createLineData(x1, y1, x2, y2, thickness, lineRotation, col);

    }

    /**
     * Helper method to create text on the current page, this method delegates
     * to the presentation text object in order to construct the text.
     *
     * @param fontNumber
     *            the font number used as the resource identifier
     * @param x
     *            the x coordinate of the text data
     * @param y
     *            the y coordinate of the text data
     * @param textRotation
     *            the rotation of the text data
     * @param col
     *            the text color
     * @param vsci
     *            The variable space character increment.
     * @param ica
     *            The inter character adjustment.
     * @param data
     *            the text data to create
     */
    public void createText(int fontNumber, int x, int y, int textRotation, Color col,
            int vsci, int ica, byte[] data) {

        if (presentationTextObject == null) {
            presentationTextObject = new PresentationTextObject();
            objects.add(presentationTextObject);
        }
        presentationTextObject.createTextData(fontNumber, x, y, textRotation, col, vsci, ica, data);

    }

    /**
     * Helper method to mark the end of the page. This should end the control
     * sequence on the current presenation text object.
     */
    public void endPage() {

        if (presentationTextObject != null) {
            presentationTextObject.endControlSequence();
        }

        complete = true;

    }

    /**
     * This method will create shading on the page using the specified
     * coordinates (the shading contrast is controlled via the red, green blue
     * parameters, by converting this to grey scale).
     *
     * @param x
     *            the x coordinate of the shading
     * @param y
     *            the y coordinate of the shading
     * @param w
     *            the width of the shaded area
     * @param h
     *            the height of the shaded area
     * @param red
     *            the red value
     * @param green
     *            the green value
     * @param blue
     *            the blue value
     */
    public void createShading(int x, int y, int w, int h, int red, int green,
        int blue) {

        int xCoord = 0;
        int yCoord = 0;
        int areaWidth = 0;
        int areaHeight = 0;

        switch (rotation) {
            case 90:
                xCoord = areaWidth - y - h;
                yCoord = x;
                areaWidth = h;
                areaHeight = w;
                break;
            case 180:
                xCoord = areaWidth - x - w;
                yCoord = areaHeight - y - h;
                areaWidth = w;
                areaHeight = h;
                break;
            case 270:
                xCoord = y;
                yCoord = areaHeight - x - w;
                areaWidth = h;
                areaHeight = w;
                break;
            default:
                xCoord = x;
                yCoord = y;
                areaWidth = w;
                areaHeight = h;
                break;
        }

        // Convert the color to grey scale
        float shade = (float) ((red * 0.3) + (green * 0.59) + (blue * 0.11));

        int greyscale = Math.round((shade / 255) * 16);

        String imageName = "IMG"
            + StringUtils.lpad(String.valueOf(objects.size() + 1),
            '0', 5);

        IMImageObject io = new IMImageObject(imageName);
        ImageOutputControl ioc = new ImageOutputControl(0, 0);
        ImageInputDescriptor iid = new ImageInputDescriptor();
        ImageCellPosition icp = new ImageCellPosition(xCoord, yCoord);
        icp.setXFillSize(areaWidth);
        icp.setYFillSize(areaHeight);
        icp.setXSize(64);
        icp.setYSize(8);

        //defining this as a resource
        ImageRasterData ird = new ImageRasterData(ImageRasterPattern
            .getRasterData(greyscale));

        io.setImageOutputControl(ioc);
        io.setImageInputDescriptor(iid);
        io.setImageCellPosition(icp);
        io.setImageRasterData(ird);
        objects.add(io);

    }

    /**
     * Helper method to create an image on the current page and to return
     * the object.
     * @return the image object
     */
    public ImageObject getImageObject() {

        if (presentationTextObject != null) {
            presentationTextObject.endControlSequence();
        }
        presentationTextObject = null;

        String imageName = "IMG"
            + StringUtils.lpad(String.valueOf(objects.size() + 1),
            '0', 5);

        ImageObject io = new ImageObject(imageName);
        objects.add(io);
        return io;
    }

    /**
     * Creates a TagLogicalElement on the page.
     *
     * @param name
     *            the name of the tag
     * @param value
     *            the value of the tag
     */
    public void createTagLogicalElement(String name, String value) {

        TagLogicalElement tle = new TagLogicalElement(name, value);
        tagLogicalElements.add(tle);

    }

    /**
     * Creates a NoOperation on the page.
     *
     * @param content the byte data
     */
    public void createNoOperation(String content) {

        NoOperation noOp = new NoOperation(content);
        objects.add(noOp);

    }

    /**
     * Creates an IncludePageSegment on the current page.
     *
     * @param name
     *            the name of the page segment
     * @param xCoor
     *            the x cooridinate of the page segment.
     * @param yCoor
     *            the y cooridinate of the page segment.
     */
    public void createIncludePageSegment(String name, int xCoor, int yCoor) {

        IncludePageSegment ips = new IncludePageSegment(name, xCoor, yCoor);
        segments.add(ips);

    }

    /**
     * Returns the ActiveEnvironmentGroup associated with this page.
     *
     * @return the ActiveEnvironmentGroup object
     */
    public ActiveEnvironmentGroup getActiveEnvironmentGroup() {
        return activeEnvironmentGroup;
    }

    /**
     * Returns an indication if the page is complete
     * @return whether this page is complete
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Returns the height of the page
     * @return the height of the page
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the width of the page
     * @return the width of the page
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the rotation of the page
     * @return the rotation of the page
     */
    public int getRotation() {
        return rotation;
    }

}
