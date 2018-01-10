/*******************************************************************************
 * Copyright (c) 2013, Bryan Matthews
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.gui.panel3d;

import de.jreality.geometry.BallAndStickFactory;
import de.jreality.geometry.GeometryMergeFactory;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.geometry.IndexedLineSetFactory;
import de.jreality.geometry.ParametricSurfaceFactory;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.ThickenedSurfaceFactory;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.Color;
import de.jreality.util.SceneGraphUtility;
import net.sourceforge.jocular.math.Vector3D;
import net.sourceforge.jocular.objects.SphericalSurface;
import net.sourceforge.jocular.splines.SplineCoefficients;
import net.sourceforge.jocular.splines.SplineMath;

/**
 *  A collection of static methods for generating the basic 3D geometry for various
 *  optics objects.
 * 
 * The majority of these methods should not be used directly but through the {@link OpticsObject3DFactory}
 * 
 * @author Bryan Matthews
 *
 */
public class OpticsObjects3D {
	
	// This is used for creating all underlying unit geometry
	//  It defines how many vertices to generate for circular or spherical geometry
	private final static int UNIT_DETAIL = 20;
	
	
	/**
	 * Create an origin at {0,0,0} with standard colours (X=red, Y=Green, Z=blue)
	 * 
	 * @param size - Used to scale the length of the origin lines
	 * @return a {@link SceneGraphComponent} containing the origin geometry
	 * 
	 * This was originally from jReality-TubeFactory.getXYZAxes()
	 */
	public static SceneGraphComponent createOrigin(double size){
		
		double[][] axes = { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
		int[][] axesIndices = { { 0, 1 }, { 0, 2 }, { 0, 3 } };
		Color[] axesColors = { Color.red, Color.green, Color.blue };

		IndexedLineSetFactory ilsf = new IndexedLineSetFactory();
		ilsf.setVertexCount(4);
		ilsf.setVertexCoordinates(axes);
		ilsf.setEdgeCount(3);
		ilsf.setEdgeIndices(axesIndices);
		ilsf.setEdgeColors(axesColors);
		ilsf.update();
		
		IndexedLineSet ils = ilsf.getIndexedLineSet();
		
		BallAndStickFactory basf = new BallAndStickFactory(ils);
		basf.setShowArrows(true);
		basf.setArrowPosition(1.0);
		basf.setStickRadius(.05);
		basf.setArrowScale(.15);
		basf.setArrowSlope(2.0);
		//basf.setArrowColor(arrowColor);
		basf.setShowBalls(false);
		basf.setShowSticks(true);
		basf.update();
		
		SceneGraphComponent original = basf.getSceneGraphComponent();
		
		MatrixBuilder.euclidean().scale(size, size, size)
				.assignTo(original);
		
		return original;
	}
	
//	private static void addLabel(SceneGraphComponent sgc, String label){
//		// each sample gets a numeric label at its geometric center point.
//		// Prepare the appearance.
//		Appearance labelAp = new Appearance();
//		DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(labelAp, false);
//		dgs.setShowPoints(true);
//		DefaultPointShader pointShader = (DefaultPointShader) dgs.getPointShader();
//		pointShader.setPointRadius(.00001);
//		DefaultTextShader pts = (DefaultTextShader) (pointShader).getTextShader();
//		pts.setScale(.0005);
//		pts.setOffset(new double[] { 0, 0, 0 });
//		pts.setAlignment(SwingConstants.CENTER);
//		
//		SceneGraphComponent labelSGC = SceneGraphUtility.createFullSceneGraphComponent("label");
//		PointSetFactory labelFac = new PointSetFactory();
//		labelFac.setVertexCount(1);
//		labelFac.setVertexCoordinates(new double[]{0,0,0});
//		labelFac.setVertexLabels(new String[]{label});
//		labelFac.update();
//		labelSGC.setGeometry(labelFac.getPointSet());
//		labelSGC.setAppearance(labelAp);
//		sgc.addChild(labelSGC);
//	}
	
	/**
	 * Creates a unit line (length = 1,1,1) and scales it by the parameters.
	 *  
	 * 
	 * @param xLength - Length of line in the x dimension
	 * @param yLength - Length of line in the y dimension
	 * @param zLength - Length of line in the z dimension
	 * @return a {@link SceneGraphComponent} containing the line geometry
	 */
	public static SceneGraphComponent createLine(double xLength, double yLength, double zLength){
		
		IndexedLineSet line = createUnitLine();
		
		SceneGraphComponent original = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, 0.0)
				.scale(xLength, yLength, zLength)
				.assignTo(original);
		
		original.setGeometry(line);
		
		//  Need to merge in the scale factor
		SceneGraphComponent merged = mergeGeometry(original);
		
		return merged;
	}
	
	public static SceneGraphComponent createArrow(double xLength, double yLength, double zLength){
				
		double[][] vertices = new double[][] { { 0, 0, 0 }, { xLength, yLength, zLength } };
		int[][] edges = new int[][] { {0,1} };
		
		IndexedLineSetFactory ilsf = new IndexedLineSetFactory();
		
		ilsf.setVertexCount(vertices.length);
		ilsf.setVertexCoordinates(vertices);
		ilsf.setEdgeCount(edges.length);
		ilsf.setEdgeIndices(edges);
		
		ilsf.update();
		
		IndexedLineSet line = ilsf.getIndexedLineSet();
		
		BallAndStickFactory basf = new BallAndStickFactory(line);
		basf.setShowArrows(true);
		basf.setArrowPosition(1.0);
		basf.setStickRadius(.00025);
		basf.setArrowScale(.0005);
		basf.setArrowSlope(2.0);
		basf.setShowBalls(false);
		basf.setShowSticks(true);
		basf.update();
		
		SceneGraphComponent original = basf.getSceneGraphComponent();
		
		//TODO: This is very dependant on the ball and stick factory implementation
		//  Find a different method of clearing the child appearances
		//  It is required because the child components override the top appearance
		//  and makes it impossible to set
		clearAppearances(original);
	
		return original;
	}
	
	private static void clearAppearances(SceneGraphComponent comp){
		
		for(SceneGraphComponent child : comp.getChildComponents()){
			child.setAppearance(new Appearance());
			clearAppearances(child);
		}
	}
	
	
	/**
	 * Creates a 2D rectangle and scales it by the parameters.
	 * Center point is the midpoint of the length and height parameters
	 * 
	 * @param length - x dimension size
	 * @param height - y dimension size
	 * @return a {@link SceneGraphComponent} containing the rectangle geometry
	 */
	public static SceneGraphComponent createRectangle(double length, double height){
		
		IndexedLineSet square = createUnitSquare();
		
		SceneGraphComponent original = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, 0.0)
				.scale(length, height, 1.0)
				.assignTo(original);
		
		original.setGeometry(square);
		
		//  Need to merge in the scale factor
		SceneGraphComponent merged = mergeGeometry(original);

		return merged;
	}
	
	
	/**
	 * Creates a 2D circle and scales it by the parameters.
	 *  
	 * @param radius - radius of the circle
	 * @return a {@link SceneGraphComponent} containing the circle geometry
	 */
	public static SceneGraphComponent createCircle(double radius){
		
		IndexedLineSet circle = createUnitCircle(UNIT_DETAIL);
		
		SceneGraphComponent original = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, 0.0)
				.scale(radius, radius, 1.0)
				.assignTo(original);
		
		original.setGeometry(circle);
		
		//  Need to merge in the scale factor
		SceneGraphComponent merged = mergeGeometry(original);

		return merged;
	}
	
	/**
	 * Creates a sphere and scales it by the diameter.
	 * Center point is the center of the sphere. 
	 * 
	 * @param diameter - diameter of the sphere
	 * @return a {@link SceneGraphComponent} containing the sphere geometry
	 */
	public static SceneGraphComponent createSphere(double diameter){
		
		IndexedFaceSet sphere = Primitives.sphere(UNIT_DETAIL);
		
		SceneGraphComponent original = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, 0.0)
				.scale(diameter, diameter, diameter)
				.assignTo(original);
		
		original.setGeometry(sphere);
		
		//  Need to merge in the scale factor
		SceneGraphComponent merged = mergeGeometry(original);

		return merged;
	}
	
	public static SceneGraphComponent createRotatedSpline(final SplineCoefficients[] scs){
		
		ParametricSurfaceFactory.Immersion immersion = new ParametricSurfaceFactory.Immersion() {

			public int getDimensionOfAmbientSpace() {
				return 3;
			}

			public void evaluate(double u, double v, double[] xyz, int index) {
				double r = SplineMath.calcSplineYValue(scs, v);
				double z = SplineMath.calcSplineXValue(scs, v);
				//X
				xyz[index  ] = Math.cos(u)*r;
				//Y
				xyz[index+1] = Math.sin(u)*r;
				//Z
				xyz[index+2] = z;
					
			}

			public boolean isImmutable() {
				return true;
			}

		};

		ParametricSurfaceFactory factory = new ParametricSurfaceFactory(immersion);

		double n = SplineMath.getSplineParameterMax(scs);
		factory.setULineCount(UNIT_DETAIL*6);
		factory.setVLineCount(UNIT_DETAIL*(scs.length)+1);

		factory.setClosedInUDirection(true);
		factory.setClosedInVDirection(true);

		factory.setUMin(0);
		factory.setUMax(2*Math.PI);
		factory.setVMin(0.0);
		factory.setVMax(n);

		factory.setGenerateFaceNormals(true);
		factory.setGenerateVertexNormals(true);
		factory.setGenerateTextureCoordinates(true);
		factory.update();

		SceneGraphComponent sgc = new SceneGraphComponent();
		sgc.setGeometry(factory.getIndexedFaceSet());
		//MatrixBuilder.euclidean().translate(0, 0, 0.0).scale(1.0, 1.0, 1.0).assignTo(sgc);
		sgc = mergeGeometry(sgc);
		return sgc;
	}
	
	public static SceneGraphComponent createExtrudedSpline(final SplineCoefficients[] scs, double thickness){
		SceneGraphComponent sgc = new SceneGraphComponent();
		
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		
		double[][] points = SplineMath.calcSplinePoints(scs, 10);
		int n = points[0].length;
		double[][] verteces = new double[n][3];
		double[][] vertexNormals = new double[n][3];
		int[][] faceIndices = new int[1][n];


		for(int i = 0; i < n; ++i){
			int i0 = i <= 0 ? n - 1 : i - 1;
			int i2 = i >= n - 1 ? 0 : i + 1;
			Vector3D p0 = new Vector3D(points[0][i0], points[1][i0],0);
			Vector3D p1 = new Vector3D(points[0][i], points[1][i],0);
			Vector3D p2 = new Vector3D(points[0][i2], points[1][i2],0);
			
			Vector3D norm = p1.subtract(p0).cross(p1.subtract(p2)).normalize();
			verteces[i][0] = -points[0][i];
			verteces[i][1] = points[1][i];
			verteces[i][2] = 0.0;
			
			vertexNormals[i][0] = 0;//norm.x;
			vertexNormals[i][0] = 0;//norm.y;
			vertexNormals[i][0] = 1;//norm.z;
			
			faceIndices[0][i] = i;

		}
		
//		ifsf.setFaceCount(1);
//		ifsf.setVertexCount(n);
//		ifsf.setVertexCoordinates(verteces);
//		ifsf.setFaceIndices(faceIndices);
//		ifsf.setVertexNormals(vertexNormals);
//		ifsf.setGenerateFaceNormals(true);
//		ifsf.setFaceNormals(new double[][] {{0,0,1}});
//		ifsf.setGenerateVertexNormals(true);
//		ifsf.setGenerateEdgesFromFaces(true);
//		ifsf.update();
//		IndexedFaceSet ifs = ifsf.getIndexedFaceSet();
		
		IndexedFaceSet ifs = IndexedFaceSetUtility.constructPolygon(verteces);
		
		

		//sgc.setGeometry(ifs);
		
	    ThickenedSurfaceFactory tsf = new ThickenedSurfaceFactory(ifs);
	    tsf.setThickness(thickness);
	    tsf.setCurvedEdges(true);      // force boundary curves to be tanget at vertices
	    tsf.update();
		sgc.setGeometry(tsf.getThickenedSurface());
		
		
	    
	
		sgc = mergeGeometry(sgc);
		return sgc;
	}
	/**
	 * Creates a spherical lens with the given parameters
	 * 
	 * @param frontRadius - The radius of the front side of the lens
	 * @param frontShape - The shape (Convex, Concave, Flat) of the front side of the lens
	 * @param backRadius - The radius of the back side of the lens
	 * @param backShape - The shape (Convex, Concave, Flat) of the back side of the lens
	 * @param diameter - The diameter of the lens
	 * @param thickness - The thickness of the lens
	 * @return a {@link SceneGraphComponent} containing the lens geometry
	 */
	public static SceneGraphComponent createSphericalLens(double frontRadius, SphericalSurface.SurfaceShape frontShape, double backRadius, SphericalSurface.SurfaceShape backShape, double diameter, double thickness){
			
		SceneGraphComponent original = new SceneGraphComponent();
		SceneGraphComponent childNode1 = null;
		SceneGraphComponent childNode2 = null;
		
		switch(frontShape){
		case CONCAVE:
			childNode1 = createFrontConcaveLens(frontRadius, diameter, thickness);
			break;
		case CONVEX:
			childNode1 = createFrontConvexLens(frontRadius, diameter, thickness);
			break;
			
		case FLAT:
			childNode1 = createFrontFlatLens(diameter, thickness);
			break;
		}
		
		switch(backShape){
		case CONCAVE:
			childNode2 = createBackConcaveLens(backRadius, diameter, thickness);
			break;
		case CONVEX:
			childNode2 = createBackConvexLens(backRadius, diameter, thickness);
			break;
			
		case FLAT:
			childNode2 = createBackFlatLens(diameter, thickness);
			break;
		}		
		
		SceneGraphComponent childNode3 = createLensCylinder(frontRadius, frontShape, backRadius, backShape, diameter, thickness);

		original.addChild(childNode1);
		original.addChild(childNode2);		
		original.addChild(childNode3);
		
		SceneGraphComponent merged = mergeGeometry(original);
		
		return merged;
	}
	
	
	private static SceneGraphComponent createFrontConvexLens(double radius, double diameter, double thickness){
				
		double yAngle = Math.asin(diameter/2.0/radius);		
		IndexedFaceSet lens = partialSphere(yAngle, UNIT_DETAIL);
		
		SceneGraphComponent node = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, -thickness/2 + radius)
				.scale((radius), (radius), (radius))				
				.rotateY(Math.PI)	
				.assignTo(node);	
		
		node.setGeometry(lens);
		
		return node;
	}
	
	private static SceneGraphComponent createFrontConcaveLens(double radius, double diameter, double thickness){
		
		double yAngle = Math.asin(diameter/2.0/radius);
		IndexedFaceSet lens = partialSphere(yAngle, UNIT_DETAIL);
		
		SceneGraphComponent node = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, (-thickness/2 - radius))	
				.scale((radius), (radius), (radius))						
				.assignTo(node);	
		
		node.setGeometry(lens);
		
		return node;
	}
	
	private static SceneGraphComponent createBackConvexLens(double radius, double diameter, double thickness){
		
		double yAngle = Math.asin(diameter/2.0/radius);
		IndexedFaceSet lens = partialSphere(yAngle, UNIT_DETAIL);
		
		SceneGraphComponent node = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, (thickness/2 - radius))
				.scale(radius, radius, radius)				
				.assignTo(node);	
		
		node.setGeometry(lens);
		
		return node;
	}
	
	private static SceneGraphComponent createBackConcaveLens(double radius, double diameter, double thickness){
		
		double yAngle = Math.asin(diameter/2.0/radius);
		IndexedFaceSet lens = partialSphere(yAngle, UNIT_DETAIL);
		
		SceneGraphComponent node = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, (thickness/2 + radius))
				.scale(radius, radius, radius)
				.rotateY(Math.PI)
				.assignTo(node);	
		
		node.setGeometry(lens);
		
		return node;
	}
	
	private static SceneGraphComponent createFrontFlatLens(double diameter, double thickness){
		
		IndexedFaceSet lens = createUnitCircle(UNIT_DETAIL);	
		
		SceneGraphComponent node = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, -thickness/2)
				.scale(diameter/2, diameter/2, 1.0)
				.assignTo(node);	
		
		node.setGeometry(lens);
		
		return node;
	}
	
	private static SceneGraphComponent createBackFlatLens(double diameter, double thickness){
		
		IndexedFaceSet lens = createUnitCircle(UNIT_DETAIL);	
		
		SceneGraphComponent node = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, thickness/2)
				.scale(diameter/2, diameter/2, 1.0)
				.assignTo(node);	
		
		node.setGeometry(lens);
		
		return node;
	}
	
	private static SceneGraphComponent createLensCylinder(double frontRadius, SphericalSurface.SurfaceShape frontShape, double backRadius, SphericalSurface.SurfaceShape backShape, double diameter, double thickness){
		
		IndexedFaceSet cyl = Primitives.cylinder(UNIT_DETAIL);
		
		double frontAngle = Math.asin(diameter/2.0/frontRadius);
		double frontTranslation = frontRadius - frontRadius*Math.cos(frontAngle);
		
		double backAngle = Math.asin(diameter/2.0/backRadius);
		double backTranslation = backRadius - backRadius*Math.cos(backAngle);
		
		double startZ = 0;
		double endZ = 0;		
		
		switch(frontShape){
		case CONCAVE:
			startZ = -thickness/2 - frontTranslation;
			break;
		case CONVEX:
			startZ = -thickness/2 + frontTranslation;			
			break;
			
		case FLAT:
			startZ = -thickness/2;
			break;
		}
		
		switch(backShape){
		case CONCAVE:
			endZ = thickness/2 + backTranslation;
			break;
		case CONVEX:
			endZ = thickness/2 - backTranslation;
			break;			
		case FLAT:
			endZ = thickness/2;
			break;
		}	
	
		double length = endZ-startZ;
		double center = startZ + length/2;
		
		SceneGraphComponent node = new SceneGraphComponent();
		MatrixBuilder.euclidean().translate(0, 0, center)
				.scale(diameter/2, diameter/2, length/2)
				.assignTo(node);	
		
		node.setGeometry(cyl);
		
		return node;
	}
		
	private static SceneGraphComponent mergeGeometry(SceneGraphComponent toMerge){
		
		GeometryMergeFactory mergeFactory = new GeometryMergeFactory();

		mergeFactory.setRespectFaces(true);
		mergeFactory.setRespectEdges(true);
		mergeFactory.setGenerateVertexNormals(true);
		
		SceneGraphComponent merged =  SceneGraphUtility.createFullSceneGraphComponent("merged");
		merged.setGeometry(mergeFactory.mergeGeometrySets(toMerge));		
		
		return merged;
	}
	
	
	private static IndexedFaceSet partialSphere(double maxAngle, final int detail ) {

		ParametricSurfaceFactory.Immersion immersion =
			new ParametricSurfaceFactory.Immersion() {

			public int getDimensionOfAmbientSpace() {
				return 3;
			}

			public void evaluate(double x, double y, double[] targetArray, int arrayLocation) {

				targetArray[arrayLocation  ] = Math.cos(x)*Math.sin(y);
				targetArray[arrayLocation+1] = Math.sin(x)*Math.sin(y);
				targetArray[arrayLocation+2] = Math.cos(y);
					
			}

			public boolean isImmutable() {
				return true;
			}

		};

		ParametricSurfaceFactory factory = new ParametricSurfaceFactory( immersion);

		factory.setULineCount(detail+1);
		factory.setVLineCount(detail+1);

		factory.setClosedInUDirection(true);
		factory.setClosedInVDirection(false);

		factory.setUMin(0);
		factory.setUMax(2*Math.PI);
		factory.setVMin(1e-5);
		factory.setVMax(maxAngle-1e-5);

		factory.setGenerateFaceNormals(true);
		factory.setGenerateVertexNormals(true);
		factory.setGenerateTextureCoordinates(true);
		factory.update();

		return factory.getIndexedFaceSet();
	}
	
	private static IndexedFaceSet createUnitCircle(int detail){
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();

		double[][] vertices = new double[detail][3];
		int[][] faceIndices = new int[1][detail];
		
		double angle = 0;
		for(int i=0; i < detail; i++){
			vertices[i][0] =  Math.cos(angle * Math.PI / 180);
			vertices[i][1] =  Math.sin(angle * Math.PI / 180);
			vertices[i][2] =  0;
			
			faceIndices[0][i] = i;
			
			angle += 360.0 / (double) detail;
		}
		

		ifsf.setVertexCount(vertices.length);
		ifsf.setVertexCoordinates(vertices);
		ifsf.setFaceCount(faceIndices.length);
		ifsf.setFaceIndices(faceIndices);

		ifsf.setGenerateEdgesFromFaces(true);
		ifsf.setGenerateFaceNormals(true);

		ifsf.update();

		return ifsf.getIndexedFaceSet();
	}
	
	private static IndexedLineSet createUnitSquare(){
		double[][] vertices = new double[][] { { -.5, -.5, 0 }, { .5, -.5, 0 },
				{ .5, .5, 0 }, { -.5, .5, 0 } };
		
		int[][] faceIndices = new int[][] { {0,1,2,3} };
		
		
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		
		ifsf.setVertexCount(vertices.length);
		ifsf.setVertexCoordinates(vertices);
		ifsf.setFaceCount(faceIndices.length);
		ifsf.setFaceIndices(faceIndices);
		
		// This was copied from jReality...Primitives.texturedQuadrilateralFactory()
		ifsf.setVertexTextureCoordinates(new double[] { 0,0,1,0,1,1,0,1});

		ifsf.setGenerateEdgesFromFaces(true);
		ifsf.setGenerateFaceNormals(true);

		ifsf.update();

		return ifsf.getIndexedFaceSet();
	}
	
	private static IndexedLineSet createUnitLine(){
		double[][] vertices = new double[][] { { 0, 0, 0 }, { 1.0, 1.0, 1.0 } };
		int[][] edges = new int[][] { {0,1} };
		
		IndexedLineSetFactory ilsf = new IndexedLineSetFactory();
		
		ilsf.setVertexCount(vertices.length);
		ilsf.setVertexCoordinates(vertices);
		ilsf.setEdgeCount(edges.length);
		ilsf.setEdgeIndices(edges);
		
		ilsf.update();

		return ilsf.getIndexedLineSet();
	}
	
	
	/**
	 * Creates geometry for a triangular prism.
	 * Center is in the center of the first triangular face.
	 * 
	 * @param width - size of the base of triangular faces
	 * @param length - overall length of the prism
	 * @param angle_1 - left angle of triangular face
	 * @param angle_2 - right angle of triangular face
	 * @return a {@link SceneGraphComponent} containing the prism geometry
	 */
	public static SceneGraphComponent createTriangularPrism(double width, double length, double angle_1, double angle_2){
			
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		
		double hyp = width / (Math.cos(angle_1) + Math.cos(angle_2));
		double center = hyp * Math.cos(angle_1);
		double height = hyp * Math.sin(angle_1);
		
		double[][] vertices = new double[][] { 
				{ width/2, length/2, 0 }, 
				{ center - width/2, length/2, height },
				{ -width/2, length/2, 0 }, 
				{ width/2, -length/2, 0 }, 
				{ center - width/2, -length/2, height },
				{ -width/2, -length/2, 0 }, 
				};
			

		int[][] faceIndices = new int[][] { 
				{ 0, 1, 2 },
				{ 3, 4, 5 },
				{ 2, 1, 4, 5 },
				{ 0, 1, 4, 3 },
				{ 0, 2, 5, 3 },
				};

		ifsf.setVertexCount(vertices.length);
		ifsf.setVertexCoordinates(vertices);
		ifsf.setFaceCount(faceIndices.length);
		ifsf.setFaceIndices(faceIndices);

		ifsf.setGenerateEdgesFromFaces(true);
		ifsf.setGenerateFaceNormals(true);

		ifsf.update();
		
		SceneGraphComponent output =  SceneGraphUtility.createFullSceneGraphComponent("merged");
		output.setGeometry(ifsf.getIndexedFaceSet());
		
		return output;
	}
			
	/**
	 * Creates geometry for a simple aperture object.  It is a 2D circle with a smaller
	 *  circular cut-out at the center
	 * 
	 * @param radius - Radius of outside circle
	 * @param apertureRadius - Radius of cut-out
	 * @return a {@link SceneGraphComponent} containing the aperture geometry
	 */
	public static SceneGraphComponent createSimpleAperture(double radius, double apertureRadius){
	
		int detail = UNIT_DETAIL;
		
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		
		double[][] vertices = new double[detail*2][3];
		int[][] faceIndices = new int[detail][4];
		
		double angle = 0;
		for(int i=0; i < detail*2; i+=2){
			vertices[i][0] =  apertureRadius * Math.cos(angle);
			vertices[i][1] =  apertureRadius * Math.sin(angle);
			vertices[i][2] =  0;
			
			vertices[i+1][0] =  radius * Math.cos(angle);
			vertices[i+1][1] =  radius * Math.sin(angle);
			vertices[i+1][2] =  0;
			
			faceIndices[i/2][0] = i%(detail*2);
			faceIndices[i/2][1] = (i+1)%(detail*2);
			faceIndices[i/2][2] = (i+3)%(detail*2);
			faceIndices[i/2][3] = (i+2)%(detail*2);
			
			angle += (2*Math.PI) / (double) detail;
		}
		
		
		ifsf.setVertexCount(vertices.length);
		ifsf.setVertexCoordinates(vertices);
		ifsf.setFaceCount(faceIndices.length);
		ifsf.setFaceIndices(faceIndices);
		
		ifsf.setGenerateEdgesFromFaces(true);
		ifsf.setGenerateFaceNormals(true);
		
		ifsf.update();
		
		SceneGraphComponent original = new SceneGraphComponent();
		
		original.setGeometry(ifsf.getIndexedFaceSet());
		
		SceneGraphComponent merged = mergeGeometry(original);
		
		return merged;
	}		
}
