package net.sourceforge.jocular.objects;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jocular.positioners.OffsetPositioner;
import net.sourceforge.jocular.project.OpticsObjectVisitor;
import net.sourceforge.jocular.project.OpticsProject;
import net.sourceforge.jocular.properties.FileProperty;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyKey;
import net.sourceforge.jocular.properties.PropertyManager;

public class OpticsPart extends OpticsObjectGroup {
	private OpticsProject m_project = null;
	private FileProperty m_file = new FileProperty(this, "");
	public OpticsPart(){
		super();
		setPositioner(new OffsetPositioner());
	}
	@Override
	public void setProperty(PropertyKey key, String s) {
		switch(key){
		case FILE_NAME:
			
			m_file = new FileProperty(this, s);
			setProject(m_file);
				
			firePropertyUpdated(key);
			
			break;
		default:
			super.setProperty(key, s);	
		}
		
	}

	
	@Override
	public Property<?> getProperty(PropertyKey key) {
		Property<?> result = null;
		switch(key){
		case FILE_NAME:
			//setProject(m_file);
			result = m_file;
			
				
			
			break;
		default:
			result = super.getProperty(key);
			break;
		}
		return result;
	}

	@Override
	public List<PropertyKey> getPropertyKeys() {
		ArrayList<PropertyKey> result = new ArrayList<PropertyKey>(asList(PropertyKey.NAME, PropertyKey.SUPPRESSED, PropertyKey.FILE_NAME));
		return result;
	}

	@Override
	public OpticsObject makeCopy() {
		OpticsPart result = new OpticsPart();
		result.copyEverything(this);
		return result;
	}
	public void setProject(FileProperty projectFile){
		
		
		
		//ObjectPositioner pos = getPositioner();
		if(projectFile.getValue() != null){
			OpticsProject project = OpticsProject.loadProject(projectFile.getValue());
			if(project != null){// && (m_project == null || !m_project.getFileName().equals(project.getFileName()))){
				m_project = project;
				PropertyManager.getInstance().addProject(project);
				
				OpticsObjectGroup g = project.getOpticsObject();
				
				getObjects().clear();
				add(g,0);
				g.setPositioner(new OffsetPositioner());
				//PropertyManager.getInstance().undeferParsing(m_project);
				PropertyManager.getInstance().updateDeferred(m_project);
				//g.copyProperties(project.getOpticsObject());
			}
			
		}
		//setPositioner(pos);
		
		
		
		
	}

	@Override
	public void accept(OpticsObjectVisitor visitor) {
		visitor.visit(this);
	}
	@Override
	public void doInternalCalcs() {
		setProject(m_file);
	}
	
	
	
	
	
	
}
