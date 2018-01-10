package net.sourceforge.jocular.properties;

import net.sourceforge.jocular.materials.OpticalMaterial;
import net.sourceforge.jocular.materials.SimpleOpticalMaterial;
import net.sourceforge.jocular.materials.nSf57;
import net.sourceforge.jocular.materials.nSk14;

public class MaterialProperty implements Property<OpticalMaterial> {
	public enum MaterialKey {VACUUM("Vacuum", SimpleOpticalMaterial.VACUUM),
							BOROSILICATE("Borosilicate", SimpleOpticalMaterial.BOROSILICATE),
							N_BK7("N-BK7", SimpleOpticalMaterial.N_BK7),
							SF10("SF10", SimpleOpticalMaterial.SF10),
							SF18("SF18", SimpleOpticalMaterial.SF18),
							F2("F2", SimpleOpticalMaterial.F2),
							N_SF2("N-SF2", SimpleOpticalMaterial.N_SF2),
							N_SF5("N-SF5", SimpleOpticalMaterial.N_SF5),
							N_SF57("N-SF57", new nSf57()),
							N_SK2("N-SK2", SimpleOpticalMaterial.N_SK2),
							N_SK14("N-SK14", new nSk14()),
							LBAL32("L-BAL35", SimpleOpticalMaterial.LBAL35),
							MAGNESIUM_FLUORIDE("Magnesium Fluoride", SimpleOpticalMaterial.MAGNESIUM_FLUORIDE),
							CALCITE("Calcite", SimpleOpticalMaterial.CALCITE),
							POLYCARBONATE("Polycarbonate", SimpleOpticalMaterial.POLYCARBONATE),
							TOPAS5013LS("Topas 5013LS-10", SimpleOpticalMaterial.TOPAS5013LS),
							SHINYMETAL("Shiny Metal", SimpleOpticalMaterial.SHINYMETAL)
							;
		private final String m_description;
		private final OpticalMaterial m_material;
		private MaterialKey(String description, OpticalMaterial m){
			m_description = description;
			m_material = m;
		}
		public String toString(){
			return m_description;
		}
		public OpticalMaterial getMaterial(){
			return m_material;
		}
		public static MaterialKey getKey(OpticalMaterial m){
			MaterialKey result = null;
			for(MaterialKey k : values()){
				if(k.getMaterial().getClass() == m.getClass()){
					result = k;
					break;
				}
			}
			return result;
		}
	}
	protected final MaterialKey m_key;
	protected final OpticalMaterial m_material;
	public MaterialProperty(String s){
		MaterialKey key = null;
		for(MaterialKey k : MaterialKey.values()){
			if(k.name().equals(s)){
				key = k;
				break;
			}
		}
		m_key = key;
		if(m_key == null){
			throw new RuntimeException("MaterialProperty: String \""+s+"\" does not match a MaterialKey.");
		}
		
//		switch(m_key){
//		default:
//		case VACUUM:
//			m_material = SimpleOpticalMaterial.VACUUM;
//			break;
//		case BOROSILICATE:
//			m_material = SimpleOpticalMaterial.BOROSILICATE;
//			break;
//		case SF10:
//			m_material = SimpleOpticalMaterial.SF10;
//			break;
//		case CALCITE:
//			m_material = SimpleOpticalMaterial.CALCITE;
//			break;
//		case TOPAS5013LS:
//			m_material = SimpleOpticalMaterial.TOPAS5013LS;
//			break;
//		}
		m_material = m_key.getMaterial();
	}

	@Override
	public String toString(){
		return m_key.toString();
	}
	@Override
	public String getDefiningString() {
		if(m_key != null){
			return m_key.name();
		} else {
			return null;
		}
	}

	@Override
	public void accept(PropertyVisitor v) {
		v.visit(this);
	}

	public MaterialKey getKey(){
		return m_key;
	}
	@Override
	public OpticalMaterial getValue() {
		return m_material;
	}
}
