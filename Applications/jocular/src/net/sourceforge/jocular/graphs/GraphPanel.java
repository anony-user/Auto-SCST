/*******************************************************************************
 * Copyright (c) 2014,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.graphs;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.JPanel;
import javax.swing.JToolTip;


@SuppressWarnings("serial")
public class GraphPanel extends JPanel {
	private Point2D.Double startPoint = null; 
	private Point2D.Double startPointOrigin = null;
	private Point2D.Double mousePoint = new Point2D.Double();
	private Point2D.Double mousePointOrigin = new Point2D.Double();
	private int m_timesPaintedCount = 0;
	private boolean m_painting = false;
	private boolean m_showCrossHairs = true;
	private HashMap<Object, GraphSeries> series = new LinkedHashMap<Object, GraphSeries>();
	public enum GraphType {XY_DOT("XY Dot"), 
							XY_DOT_AND_LINE("Dot and Line"),
							XY_LINE("XY Line"),
							XY_CONTINUOUS("XY Continuous Line"),
							XY_CONTINUOUS_AND_DOTS("XY Continuous Line with dots"),
							BAR("Bar"), BAR_DOT("Bar Dotted"),
							FROM_GRAPH("from Graph");
	
		private String name;
		private GraphType(String n){
			name = n;
		}
		public String toString(){
			return name;
		}
	}
	
	private GraphType defaultGraphType = GraphType.XY_DOT;
	public enum DotType {CIRCLE("Circle"), SQUARE("Square"), TRIANGLE("Triangle"), CROSS("Cross");
		private String name;
		private DotType(String n){
			name = n;
		}
		public String toString(){
			return name;
		}
	}
	private DotType defaultDotType = DotType.CIRCLE;
	private GraphAxis xAxis, yAxis;
	private static final Color[] COLORS = {Color.red, Color.green, Color.blue, Color.magenta, Color.cyan, Color.orange, Color.gray};
	NumberFormat format = new DecimalFormat("##0E0");
	
	private Color color = Color.red;
	private JToolTip jtp = new JToolTip();
	private Color m_gridColor = new Color(220,220,220);
	private Color m_textColor = new Color(220,220,220);
	private Color m_cursorColor = new Color(119,0,255);
	/**
	 * Makes a new graph Panel
	 * @param fitted 
	 */
	public GraphPanel() {
		super();
		setPreferredSize(new Dimension(200, 200));
		addMouseMotionListener(new MouseMotionListener(){

			@Override
			public void mouseDragged(MouseEvent me) {
				
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				Point2D.Double p = getMousePos();
				mousePoint = new Point2D.Double(p.x,p.y);
				mousePointOrigin = GraphPanel.this.getScaledOrigin();
				repaint();
				if(startPoint != null){
					Point2D.Double newOrigin = GraphPanel.this.getScaledOrigin();
					p.setLocation(p.x-(startPoint.x-startPointOrigin.x+newOrigin.x),p.y-(startPoint.y-startPointOrigin.y+newOrigin.y));
					setToolTipText("delta = "+format.format(p.x)+","+format.format(p.y));
				} else {
					setToolTipText(format.format(p.x)+","+format.format(p.y));
				}
				
			}
			
		});
		addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent me) {
				startPoint = getMousePos();
				startPointOrigin = GraphPanel.this.getScaledOrigin();
			}

			@Override
			public void mouseEntered(MouseEvent me) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent me) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent me) {
			
			}

			@Override
			public void mouseReleased(MouseEvent me) {
				// TODO Auto-generated method stub
				
			}
			
		});

		

	}
	
	
	public void zeroOffset(){
		startPoint = null;
	}
	public Point2D.Double getMousePos(){
		Point2D.Double result = new Point2D.Double();
		Point p = getMousePosition();
		if(p != null){
			Point2D.Double p1 = new Point2D.Double(p.getX(), p.getY());
			
			try {
			
				AffineTransform t = getTransform().createInverse();
				t.transform(p1, result);
				//System.out.println("pos "+result.x+","+result.y);
				result = unScale(result);
				
			} catch(NoninvertibleTransformException e){
				
			}
		}
		return result;
		
	}
	private Point2D.Double unScale(Point2D.Double p){
		Point2D.Double r = (Point2D.Double)p.clone();
		r.x = xAxis.unScale(r.x);
		r.y = yAxis.unScale(r.y);
		return r;
		
	}
	public void setColor(Color c){
		color = c;
	}

	public void updateGraph(Object seriesKey, double xNew[], double yNew[]){//assumed that x and y span 0 to 1
		Color c = getNextSeriesColor();
		if(xNew.length != yNew.length){
			throw new RuntimeException("X and Y series are not the same length: "+xNew.length+", "+yNew.length);
		}
		if(series.containsKey(seriesKey)){
			c = series.get(seriesKey).getColor();
		} else {
			System.out.println("Adding "+ seriesKey + ": "+c);
		}
		if(!m_painting){
			series.put(seriesKey, new GraphSeries(xNew, yNew,c));
		}
		//System.out.println("min x,y:"+minX+","+minY+" max x,y:"+maxX+","+maxY);
		//repaint();
	}
	public void setSeriesColor(Object key, Color color){
		if(series.containsKey(key)){
			series.get(key).color = color;
		}
	}
	public void setSeriesSize(Object key, double dotSize, double lineWidth){
		if(series.containsKey(key)){
			series.get(key).setDotSize(dotSize);
			series.get(key).setLineWidth(lineWidth);
		}
	}
	public void removeSeries(Object seriesKey){
		if(series.containsKey(seriesKey)){
			series.remove(seriesKey);
		}
		repaint();
	}
	public void removeAllSeries(){
		series.clear();
		repaint();
	}
	public GraphType getSeriesType(Object seriesKey) {
		GraphSeries s = series.get(seriesKey);
		return s.getType();
	}
	public void setSeriesType(Object seriesKey, GraphType gt) {
		GraphSeries s = series.get(seriesKey);
		s.setType(gt);
	}
	public void setSeriesDotType(Object seriesKey, DotType dt){
		GraphSeries s = series.get(seriesKey);
		s.setDotType(dt);
	}
	public boolean isSeries(Object seriesKey) {
		return series.containsKey(seriesKey);
	}
	public void setGraphType(GraphType gt){
		defaultGraphType = gt;
	}
	public void setScale(double xMin, double xMax, double yMin, double yMax, boolean xLog, GraphType gt){
		setScale(xMin, xMax, yMin, yMax, xLog);
		this.defaultGraphType = gt;
	}
	public void setScale(double xMin, double xMax, double yMin, double yMax, boolean xLog){
		this.xAxis = new GraphAxis(xMin, xMax, xLog, 1);
		this.yAxis = new GraphAxis(yMin, yMax, false, 0);
		//System.out.println("GraphPanel.setScale: "+xMin+" "+yMin+" "+xMax+" "+yMax);
	}
	public void autoScale(boolean scaleX, boolean logX, boolean scaleY, boolean logY){
		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		
		for(GraphSeries gs : series.values()){
			double xs[] = gs.x;
			double ys[] = gs.y;
			
			for(int i = 0 ; i < xs.length; ++i){
				double x = xs[i];
				double y = ys[i];
				if(x < xMin){
					xMin = x;
				}
				if(x > xMax){
					xMax = x;
				}
				if(y < yMin){
					yMin = y;
				}
				if(y > yMax){
					yMax = y;
				}
			}
			
		}
		if(scaleX){
			this.xAxis = new GraphAxis(xMin, xMax, logX, getBestGridlineSpacing(xMin, xMax));
		}
		if(scaleY){
			this.yAxis = new GraphAxis(yMin, yMax, logY, getBestGridlineSpacing(yMin, yMax));
		}
	}
	private static final int OPTIMAL_GRID_COUNT = 10;//assume that around 10 gridlines is optimal
	public double getBestGridlineSpacing(double min, double max){
		double t = Math.abs((max - min)/OPTIMAL_GRID_COUNT);
		double l = Math.floor(Math.log10(t));
		double p = Math.pow(10,l); 
		t /= p;
		//t should be between 0 and 10 now
		double[] spacingOptions = {1, 2, 5}; 
		double sBest = 0;
		double distBest = Double.POSITIVE_INFINITY;
		for(double sTest : spacingOptions){
			double dist = Math.abs(sTest - t);
			if(dist < distBest){
				distBest = dist;
				sBest = sTest;
			}
		}
		return sBest * p;
	}
	public void fitAllSeries(){
		double maxValue = Double.MIN_VALUE;
		double minValue = Double.MAX_VALUE;
		Object maxKey = null;
		for(Object key : series.keySet()){
		//for(Series s : series.values()){
			for(int i = 0; i < series.get(key).size(); i++){
				if(series.get(key).getY(i) > maxValue){
					maxValue = series.get(key).getY(i);
					maxKey = key;
				}
				if(series.get(key).getY(i) < minValue){
					minValue = series.get(key).getY(i); 
				}
			}
			
		}
		//System.out.println("GraphPanel.fitAllSeries : "+minValue+" "+maxValue+" series "+maxKey);
		this.setScale(xAxis.min, xAxis.max, minValue, maxValue, xAxis.log);
		repaint();
		//this.yAxis = new Axis(minValue, maxValue,false,0);
		
	}
	public double[] getXData(Object key) {
		GraphSeries ser = series.get(key);
		if(ser == null){
			return null;
		}
		int len = ser.size();
		double[] x = new double[len];
		for(int i=0; i< len; i++) {
			x[i] = ser.getX(i);
		}		
		return x;
	}
	public double[] getYData(Object key) {
		GraphSeries ser = series.get(key);
		if(ser == null){
			return null;
		}
		int len = ser.size();
		double[] y = new double[len];
		for(int i=0; i< len; i++) {
			y[i] = ser.getY(i);
		}		
		return y;		
	}
	public double[] getYDataNormalized(Object key) {
		GraphSeries ser = series.get(key);
		int len = ser.size();
		double total=0.0;
		double[] y = new double[len];
		for(int i=0; i< len; i++) {
			y[i] = ser.getY(i);
			total += y[i];
		}	
		for(int i=0; i< len; i++) {
			y[i] /= total;
		}
		return y;				
	}
	private AffineTransform getTransform(){
		AffineTransform t;
		Dimension panelSize = getSize();
		t = AffineTransform.getScaleInstance(((double) (panelSize.width)),
				-((double) (panelSize.height)));
		t.translate(0, -1);
		return t;
	}
	
	@Override
	public void repaint() {
		
		super.repaint();
	}
	@Override
	protected void paintComponent(Graphics g) {
		
		
		
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		Rectangle r = g2.getClipBounds();

		g2.setColor(getBackground());

		g2.fillRect(r.x, r.y, r.width, r.height);
		
		AffineTransform oldTransform = g2.getTransform();
		
		Dimension panelSize = getSize();
		AffineTransform transform;
		transform = new AffineTransform(oldTransform);
		
		transform.concatenate(getTransform());
		g2.setTransform(transform);
		
		//do frame
		g2.setStroke(new BasicStroke(0.0020f));
		g2.setColor(m_gridColor);
		g2.draw(new Rectangle2D.Double(0.1,0.1,.8,.8));
		
		//do the x axis grid
		ArrayList<Double> xGridLines = xAxis.getGridLines();
		if(xGridLines != null){
			//g2.setColor(new Color(220,220,220));
			
			for(Double d : xGridLines){
				//drawBar(g2, xAxis.scale(d), 1, 0.001);
				drawVertLine(g2,xAxis.scale(d),panelSize.width);
			}
			
		}

		//do the y axis grid
		ArrayList<Double> yGridLines = yAxis.getGridLines();
		if(yGridLines != null){
			//g2.setColor(new Color(180,180,180));
			for(Double d : yGridLines){
				//drawBar(g2, xAxis.scale(d), 1, 0.001);
				drawHorizLine(g2,yAxis.scale(d),panelSize.height);
			}
			
		}

		
		
		// let's assume that we'll draw from .1 to .9 in x
		// .1 will be 0 and .9 will be 2 in y
		//System.out.println("CorrelatorComms.paintComponent");
		m_painting = true;

		for(GraphSeries s : series.values()){
			double pw = panelSize.width;
			double ph = panelSize.height;
			double xr = s.getDotSize()/pw;
			double yr = s.getDotSize()/ph;
			double w = s.getLineWidth()/Math.sqrt(pw*pw + ph*ph);
			g2.setColor(s.getColor());
			//drawDot(g2,.9,.9,.005);
			for(int i = 0; i < s.size(); i++){
				GraphType t;
				if(s.getType() == GraphType.FROM_GRAPH) {
					t = defaultGraphType;
				} else {
					t = s.getType();
				}
				if(!Double.isNaN(s.getX(i))){
					switch(t){
					case XY_DOT:
						drawDot(g2, xAxis.scale(s.getX(i)), yAxis.scale(s.getY(i)),xr,yr, s.getDotType());
						break;
					case BAR:
						drawBar(g2, xAxis.scale(s.getX(i)), yAxis.scale(s.getY(i)),xr,yr);
						break;
					case BAR_DOT:
						drawDottedBar(g2, xAxis.scale(s.getX(i)), yAxis.scale(s.getY(i)),xr,yr);
						break;
					case XY_DOT_AND_LINE:
						drawDot(g2, xAxis.scale(s.getX(i)), yAxis.scale(s.getY(i)),xr,yr, s.getDotType());
						//let it flow into next
					case XY_LINE:
						if(!Double.isNaN(s.getX(i - 1))){
							if(s.getX(i - 1) < s.getX(i)){
								drawLine(g2, xAxis.scale(s.getX(i-1)), yAxis.scale(s.getY(i-1)),xAxis.scale(s.getX(i)), yAxis.scale(s.getY(i)),w,w);
							}
						}
						break;
					case XY_CONTINUOUS_AND_DOTS:
						drawDot(g2, xAxis.scale(s.getX(i)), yAxis.scale(s.getY(i)),xr,yr, s.getDotType());
						//let it flow into next
					case XY_CONTINUOUS:
						if(!Double.isNaN(s.getX(i - 1))){
							drawLine(g2, xAxis.scale(s.getX(i-1)), yAxis.scale(s.getY(i-1)),xAxis.scale(s.getX(i)), yAxis.scale(s.getY(i)),w,w);
						}
						break;
					}
				}
			}
		}
		m_painting = false;
		if(m_showCrossHairs){
			g2.setStroke(new BasicStroke(0.0020f));
			//do the cursor offset pos
			g2.setColor(m_cursorColor);
			Point2D.Double newOrigin = getScaledOrigin();
			
			if(startPoint != null){
				
				drawVertLine(g2,xAxis.scale(startPoint.x-startPointOrigin.x+newOrigin.x),panelSize.width);
				drawHorizLine(g2,yAxis.scale(startPoint.y-startPointOrigin.y+newOrigin.y),panelSize.height);
			}
			
			Point2D.Double p = getMousePos();
			mousePoint = new Point2D.Double(p.x,p.y);
			mousePointOrigin = GraphPanel.this.getScaledOrigin();
			
			drawVertLine(g2,xAxis.scale(mousePoint.x-mousePointOrigin.x+newOrigin.x),panelSize.width);
			drawHorizLine(g2,yAxis.scale(mousePoint.y-mousePointOrigin.y+newOrigin.y),panelSize.height);
			
		}
		
		
		
		g2.setTransform(oldTransform);
	}



	private void drawDot(Graphics2D g, double x, double y, double xr, double yr, DotType dotType) {
		switch(dotType){
		default:
		case CIRCLE:
			g.fill(new Ellipse2D.Double(x - xr, y - yr, xr * 2, yr * 2));
			break;
		case CROSS:
			Shape cl1 = new Line2D.Double(x - xr, y - yr, x + xr, y + yr);
			Shape cl2 = new Line2D.Double(x + xr, y - yr, x - xr, y + yr);
			g.draw(cl1);
			g.draw(cl2);
			break;
		case SQUARE:
			Shape sl1 = new Line2D.Double(x - xr, y - yr, x + xr, y - yr);
			Shape sl2 = new Line2D.Double(x - xr, y + yr, x + xr, y + yr);
			Shape sl3 = new Line2D.Double(x - xr, y - yr, x - xr, y + yr);
			Shape sl4 = new Line2D.Double(x + xr, y - yr, x + xr, y + yr);
			g.draw(sl1);
			g.draw(sl2);
			g.draw(sl3);
			g.draw(sl4);
			break;
		case TRIANGLE:
			Point2D.Double p1 = new Point2D.Double(x, y + yr);
			Point2D.Double p2= new Point2D.Double(x - xr*0.86602540378, y - yr*0.5);
			Point2D.Double p3= new Point2D.Double(x + xr*0.86602540378, y - yr*0.5);
			Shape l1 = new Line2D.Double(p1,p2);
			Shape l2 = new Line2D.Double(p2,p3);
			Shape l3 = new Line2D.Double(p3,p1);
			g.draw(l1);
			g.draw(l2);
			g.draw(l3);
			break;
		}
		
	}
	private void drawBar(Graphics2D g, double x, double y, double xr, double yr) {
		g.fill(new Rectangle2D.Double(x - xr, .1-yr, 2 * xr, y-.1+yr));
		//g.fill(new Ellipse2D.Double(-2*r, -2*r, r * 4, r * 4));
	}
	private void drawDottedBar(Graphics2D g, double x, double y, double xr, double yr) {
		// eventually a dotted bar would be nice, for now just a half width line
		g.fill(new Rectangle2D.Double(x - xr/2.0, .1-yr, xr, y-.1+yr));		
	}
	private void drawLine(Graphics2D g, double x1, double y1, double x2, double y2, double xr, double yr) {
		 
		
		double dx = x2 - x1;
		double dy = y2 - y1;
		
		double r = (Math.abs(dy*xr)+Math.abs(dx*yr))/Math.sqrt(dx*dx+dy*dy);
		g.setStroke(new BasicStroke((float)(2*r),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(x1, y1, x2, y2));
		//g.fill(new Ellipse2D.Double(-2*r, -2*r, r * 4, r * 4));
	}
	private void drawVertLine(Graphics2D g, double x, int xDim){
		double r = 0.6/xDim;
		g.fill(new Rectangle2D.Double(x - r,0.1, 2 * r, 0.8));
	}
	private void drawHorizLine(Graphics2D g, double y, int yDim){
		double r = 0.6/yDim;
		g.fill(new Rectangle2D.Double(0.1, y - r, 0.8, 2 * r));
	}
	protected Color getNextSeriesColor(){
		
		for(Color c : COLORS){
			boolean matched = false;
			for(GraphSeries s : series.values()){
				if(s.getColor().equals(c)){
					matched = true;
				}
			}
			if(!matched){
				return c;
			}
			
		}
		return new Color((float)Math.random(), (float)Math.random(), (float)Math.random());
	}
	public void setXGridLineSpacing(double gs){
		xAxis.gridLineSpacing = gs;
	}
	public void setYGridLineSpacing(double gs){
		yAxis.gridLineSpacing = gs;
	}
	public Point2D.Double getScaledOrigin(){
		return new Point2D.Double(xAxis.min,yAxis.min);
	}
	public Rectangle2D.Double getScaledBounds(){
		double x0,y0, dx, dy;
		x0 = xAxis.min;
		dx = xAxis.max - x0;
		y0 = yAxis.min;
		dy = yAxis.max - y0;
		return new Rectangle2D.Double(x0,y0,dx,dy);
	}
	/**
	 * Moves the series with the specified key so it draws on top of all others
	 * @param key
	 */
	public void setForegroundSeries(Object key){
		//first check if a series exists with the key
		if(!series.containsKey(key)){
			return;
		}
		//ok, now rip it out of the map and add it again. This should move it to the end of the list
		GraphSeries s = series.get(key);
		series.remove(key);
		series.put(key, s);
		repaint();
		
	}
	public void showCrossHairs(boolean b){
		m_showCrossHairs = b;
	}
	public void setGridColor(Color c){
		m_gridColor = c;
	}
	public void setTextColor(Color c){
		m_textColor = c;
	}
	public void setCursorColor(Color c){
		m_cursorColor  = c;
	}
	
	

}


