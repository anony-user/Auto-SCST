/*******************************************************************************
 * Copyright (c) 2013,Kenneth MacCallum
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.jocular.photons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import net.sourceforge.jocular.autofocus.AutofocusSensor;
import net.sourceforge.jocular.materials.OpticalMaterial;
import net.sourceforge.jocular.objects.OpticsObject;
import net.sourceforge.jocular.objects.OpticsObjectGroup;
import net.sourceforge.jocular.objects.OpticsSurface;
import net.sourceforge.jocular.objects.ProjectRootGroup;
import net.sourceforge.jocular.settings.Settings;
import net.sourceforge.jocular.sources.LightSource;



public class PhotonWrangler {
	//private OpticsObject m_object;

	private List<PhotonTrajectory> m_trajectories;
	private final ArrayList<LightSource> m_lightSources = new ArrayList<LightSource>();
	
	private Collection<WranglerListener> m_WranglerListeners = new ArrayList<WranglerListener>();
	//private Collection<WranglingStartedListener> m_WranglingStartedListeners = new ArrayList<WranglingStartedListener>();
	private int m_numTraj;
	private int m_curTraj;
	private Thread wranglerThreads[];
	public static final int NUM_THREADS = 1;
	private boolean stopWranglerThread;
	//private boolean wranglerRunning;
	
	public PhotonWrangler() {
		super();		
		
		//wranglerRunning = false;
		
		m_numTraj = Settings.SETTINGS.getPhotonTrajectoryCount();
		m_trajectories = new LimitedQueue<PhotonTrajectory>(Settings.SETTINGS.getPhotonTrajectoryDisplayCount());
	}
	
	private List<LightSource> getLightSources(Collection<OpticsObject> os){
		ArrayList<LightSource> result = new ArrayList<LightSource>();
		
		for(OpticsObject ot : os){
			if(ot instanceof LightSource){
				result.add((LightSource)ot);
			}
		}
			
	
		return result;
	}
	public class LightSourceWrangler {
		private final List<LightSource> m_sources;
		private int m_sourceIndex;
		public LightSourceWrangler(Collection<OpticsObject> os){
			m_sources = getLightSources(os);
			m_sourceIndex = 0;
		}
		public LightSource getNextLightSource(){
			if(m_sources.size() == 0){
				return null;
			}
			
			LightSource temp = m_sources.get(m_sourceIndex);
			
			//TODO: An iterator would probably be a better solution here
			if(++m_sourceIndex >= m_sources.size())
				m_sourceIndex = 0;
			
			return temp;
		}
		public int size(){
			return m_sources.size();
		}
	}
	

	public Collection<PhotonTrajectory> getTrajectories(){
			
		List<PhotonTrajectory> result;
		synchronized(this){
			
			result = new ArrayList<PhotonTrajectory>(m_trajectories);
		
			//Collections.copy(result, m_trajectories);
		}
		return result;
	}
	
	private void clear(){
		if(isWrangling()){
			throw new RuntimeException("Can't clear trajectories while running.");
		}
		
		synchronized(this){
			m_trajectories = new LimitedQueue<PhotonTrajectory>(Settings.SETTINGS.getPhotonTrajectoryDisplayCount());
			// TODO: Not sure if we want to clear and then re-add light
			// sources each time.  Difficult to detect changes to the project
			// otherwise though.
		}
		
	}
	public boolean isWrangling(){
		boolean result = false;
		if(wranglerThreads != null){
			for(Thread t : wranglerThreads){
				if(t != null && t.isAlive()){
					result = true;
				}
			}
		}
		return result;
	}
	public void wrangle(final OpticsObjectGroup optics, int num){
		Collection<OpticsObject> objects = ((OpticsObjectGroup)optics).getFlattenedOpticsObjects(false);
		
		
		Collection<OpticsObject> os = optics.getFlattenedOpticsObjects(false);
		Collection<OpticsObject> group = new ArrayList<OpticsObject>();
		for(OpticsObject o : os){
			if(o instanceof AutofocusSensor){
				//don't bother including sensors when computing images
			} else {
				group.add(o);
			}
			
		}
		
		
		
		
		wrangle(group, optics, num);
	}
	public void wrangle(final Collection<OpticsObject> objects, final OpticsObject optics, int num){
				
		if(isWrangling()){
			throw new RuntimeException("Wrangler is already running.");
		}
		
		clear();
		
		//addLightSources(optics);
		
		stopWranglerThread = false;
		m_curTraj = 0;
		m_numTraj = num;
		int n = Settings.SETTINGS.getNumberWranglerThreads();
		if(n > 3){
			n = 3;
		}
		//System.out.println("PhotonWrangler.wrangle: Starting "+n+" wranglers.");
		wranglerThreads = new Thread[n];
		for(int i = 0; i < wranglerThreads.length; ++i){
			
		
			wranglerThreads[i] = new Thread(new Runnable(){			
				
				public void run(){
					
					nonThreadWrangle(objects, optics, m_numTraj);
				}
			});
			wranglerThreads[i].start();
		}
	}
	private void nonThreadWrangle(Collection<OpticsObject> objects, OpticsObject outerObject, int numTraj){
		

		
		//wranglerRunning = true;
		fireWranglerEvent(WranglerEvent.Type.STARTED);
		
		LightSourceWrangler sourceWrangler = new LightSourceWrangler(objects);
		if(sourceWrangler.size() == 0){
			System.out.println("PhotonWrangler.nonThreadWrangle no light sources specified in project.");
			return;
		}
		//InteractionSorter is = new InteractionSorter();
		for(m_curTraj = 0; m_curTraj < numTraj; ++m_curTraj){
			if(m_curTraj % 1000 == 0 && m_curTraj != 0){
				fireWranglerEvent(WranglerEvent.Type.ONGOING);
			}
			InteractionSorter is = new InteractionSorter();
			PhotonTrajectory pt = new PhotonTrajectory(outerObject);//
			sourceWrangler.getNextLightSource().getNextPhoton(pt);
			
			while(!pt.isPhotonLost() && pt.getNumberOfPhotons() < 50){
				is.clear();
				//optics.getPossibleInteraction(pt, is);
				for(OpticsObject o : objects){
					//if(!o.isSuppressed()){
					try{
						o.getPossibleInteraction(pt, is);
					} catch(RuntimeException e){
						e.printStackTrace();
					}
					//}
				}
				is.sort(pt.getInteraction());
				PhotonInteraction pi = is.getShortest();
				if(pi == null){
					System.out.println("PhotonWrangler.nonThreadWrangle no interaction found.");
				} else if(is.getDistanceBetweenShortests() < Settings.MIN_DISTANCE){
					
					//TODO: combine these two surfaces
					PhotonInteraction pi1 = is.getShortest();
					PhotonInteraction pi2 = is.getNextShortest();
					//System.out.println("PhotonWrangler.nonThreadWrangle next closest "+pi1);
					if(pi1.getInteractingObject() instanceof OpticsSurface && pi2.getInteractingObject() instanceof OpticsSurface){
						
						
						if(pi1.isFromInside() && !(pi2.isFromInside())){
							//String sm1 = pi1.getOpticsObject().getProperty(PropertyKey.OUTSIDE_MATERIAL).toString();
							//String sm2 = pi2.getOpticsObject().getProperty(PropertyKey.INSIDE_MATERIAL).toString();
							//System.out.println("PhotonWrangler.nonThreadWrangle choosing next closest, out to in "+is.getDistanceBetweenShortests()+","+sm1+", "+sm2);
							
							pi = pi1.combine(pi2);
							if(pi.getToObject() == pt.getOuterObject()){
								throw new RuntimeException("This should not happen.");
							}
						} else if(!(pi1.isFromInside()) && pi2.isFromInside()){
							//String sm2 = pi2.getOpticsObject().getProperty(PropertyKey.OUTSIDE_MATERIAL).toString();
							//String sm1 = pi1.getOpticsObject().getProperty(PropertyKey.INSIDE_MATERIAL).toString();
							//System.out.println("PhotonWrangler.nonThreadWrangle choosing next closest, in to out "+is.getDistanceBetweenShortests()+","+sm1+", "+sm2);
							pi = pi1.combine(pi2);
							if(pi.getToObject() == pt.getOuterObject()){
								throw new RuntimeException("This should not happen.");
							}
						} 
					}
				} else if(pi.getInteractingObject() != pt.getContainingObject() && !(pt.getContainingObject() instanceof ProjectRootGroup)){
					if(is.getFirstInteractionWithObject(pt.getInteraction()) == null){
						//somehow we're in an object but the present photon will not cross to exit this object
						//let's assume for now that we should exit here but somehow we missed the change
						pi = pi.leaveObject(pt.getContainingObject());
//						if(pi.getToObject() == pt.getContainingObject()){
//							throw new RuntimeException("This should not happen.");
//						}
					}
				}
				
				//propagate photon through material
				if(pi != null && pi.isValid()){
					
					
					OpticalMaterial m = pt.getMaterial();
					
					//System.out.println("PhotonWrangler last mat is "+MaterialKey.getKey(m));
					m.interact(pi, pt);
					if(!pt.failedToPropagate()){
//						if(exitObject != null){
//							pt.exitObject(exitObject);
//						}
						
						if(pi.getToObject() == pt.getOuterObject()){
							throw new RuntimeException("this also should not happen.");
						}
//						if(pt.getContainingObject() instanceof SphericalLens && pi.getFromObject() == null){
//							throw new RuntimeException("Debugging."+pi);
//						}
						pi.getInteractingObject().interact(pi, pt);
						
						//System.out.println("PhotonWrangler.nonThreadWrangle "+pt.getNumberOfPhotons() + ", "+pt.getPhoton().getPhotonSource()+ ", "+ pt.getPhoton().getOrigin() + " : "+pi.getOpticsObject()+", "+pi);
					}
				} else {
					pt.losePhoton();
				}
				synchronized(this){
					m_trajectories.add(pt);
				}
			}
			//System.out.println("Photon "+i+" interacted "+ count + " times.");
			if(stopWranglerThread == true){				
				break;
			}
			
				
		}
		
		//wranglerRunning = false;
		
		fireWranglerEvent(WranglerEvent.Type.FINISHED);
	}
	
	
	
	public void stop(){
		stopWranglerThread = true;
	}
	
	public int getTotalCounts(){
		return m_numTraj;
	}
	
	public int getCurrentCount(){
		return m_curTraj;
	}
	
	//Event Listeners
	public void addWranglerListener(WranglerListener listener){
		if(!m_WranglerListeners.contains(listener)){
			m_WranglerListeners.add(listener);
		}
	}
	public void removeWranglerListener(WranglerListener listener){
		if(!m_WranglerListeners.contains(listener)){
			m_WranglerListeners.remove(listener);	
		}
	}
	private void fireWranglerEvent(final WranglerEvent.Type type){
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				WranglerEvent e = new WranglerEvent(PhotonWrangler.this, type);
				for(WranglerListener listener : m_WranglerListeners){
					listener.wranglingUpdate(e);
				}
			}
			
		});
	}

	
	// Copied from:
	// http://stackoverflow.com/questions/5498865/size-limited-queue-that-holds-last-n-elements-in-java
	@SuppressWarnings("serial")
	public class LimitedQueue<E> extends LinkedList<E> {
	    
		private int limit;

		public LimitedQueue(int limit) {
			this.limit = limit;		    
		}		
		    
		@Override		
		public boolean add(E o) {		
			super.add(o);		      
		      while (size() > limit) { super.remove(); }		      
		      return true;		    
		}
	}

}
