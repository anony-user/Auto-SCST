package net.sourceforge.jocular.gui.tables;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;

import net.sourceforge.jocular.Jocular;
import net.sourceforge.jocular.input_verification.PropertyInputVerifier;
import net.sourceforge.jocular.properties.AngleProperty;
import net.sourceforge.jocular.properties.BooleanProperty;
import net.sourceforge.jocular.properties.EnumArrayProperty;
import net.sourceforge.jocular.properties.EnumProperty;
import net.sourceforge.jocular.properties.EquationArrayProperty;
import net.sourceforge.jocular.properties.EquationProperty;
import net.sourceforge.jocular.properties.FileProperty;
import net.sourceforge.jocular.properties.ImageProperty;
import net.sourceforge.jocular.properties.IntegerProperty;
import net.sourceforge.jocular.properties.LengthProperty;
import net.sourceforge.jocular.properties.MaterialProperty;
import net.sourceforge.jocular.properties.MaterialProperty.MaterialKey;
import net.sourceforge.jocular.properties.Property;
import net.sourceforge.jocular.properties.PropertyVisitor;
import net.sourceforge.jocular.properties.StringArrayProperty;
import net.sourceforge.jocular.properties.StringProperty;

@SuppressWarnings("serial")
public class PropertyCellEditorFactory implements PropertyVisitor{

	TableCellEditor m_editor;
	PropertyInputVerifier m_verifier;
	
	public TableCellEditor getEditor(Property<?> property, PropertyInputVerifier verifier){
		
		m_verifier = verifier;
		
		property.accept(this);
		
		return m_editor;
	}
	
	public static TableCellEditor getDefaultEditor(){
		
		PropertyCellEditorFactory factory = new PropertyCellEditorFactory();
		return factory.new PropertyCellEditor();
	}

	
	@Override
	public void visit(AngleProperty v) {
		
		m_editor = new PropertyCellEditor();
		//((PropertyCellEditor)m_editor).setInputVerifier(m_verifier);
		
	}

	@Override
	public void visit(BooleanProperty v) {
		
		m_editor = new BooleanPropertyCellEditor();
	}

	@Override
	public void visit(EnumProperty v) {
		m_editor = new EnumPropertyCellEditor();
	}

	@Override
	public void visit(ImageProperty v) {
		m_editor = new ImagePropertyCellEditor();
	}

	@Override
	public void visit(IntegerProperty v) {
		m_editor = new PropertyCellEditor();
		//((PropertyCellEditor)m_editor).setInputVerifier(m_verifier);
		
	}
	@Override
	public void visit(EquationProperty v) {
		m_editor = new PropertyCellEditor();
		//((PropertyCellEditor)m_editor).setInputVerifier(m_verifier);		
		
	}
	@Override
	public void visit(EquationArrayProperty v) {
		m_editor = new PropertyCellEditor();
		//((PropertyCellEditor)m_editor).setInputVerifier(m_verifier);		
		
	}
	@Override
	public void visit(LengthProperty v) {
		m_editor = new PropertyCellEditor();
		//((PropertyCellEditor)m_editor).setInputVerifier(m_verifier);		
	}

	@Override
	public void visit(MaterialProperty v) {
		m_editor = new MaterialPropertyCellEditor();
		
	}

	@Override
	public void visit(StringProperty v) {
		m_editor = new StringPropertyCellEditor();		
	}
	@Override
	public void visit(FileProperty v) {
		m_editor = new FilePropertyCellEditor();		
	}
	@Override
	public void visit(StringArrayProperty v) {
		m_editor = new StringPropertyCellEditor();		
	}
	@Override
	public void visit(EnumArrayProperty v) {
		m_editor = new StringPropertyCellEditor();		
	}
	
	protected class PropertyCellEditor extends DefaultCellEditor {
		

		private final JTextField m_textField;
		
		public PropertyCellEditor(){
					
			super(new JTextField());
			
			m_textField = (JTextField) editorComponent;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column) {
			Property<?> p = (Property<?>)value;
			m_textField.setBorder(new LineBorder(Color.black));
			return super.getTableCellEditorComponent(table, p.getDefiningString(), isSelected, row, column);
		}
		
//		@Override
//		public boolean stopCellEditing() {
//			
//			VerificationResult result = verifier.verify(editorComponent);
//			 
//			if (result.isValid() == false){
//				m_textField.setBorder(new LineBorder(Color.red));
//				m_textField.setToolTipText(result.getMessage());
//				return false;
//			} else{
//				m_textField.setBorder(new LineBorder(Color.black));
//				m_textField.setToolTipText("");
//			}
//			
//			return super.stopCellEditing();		 
//		}
		
//		public void setInputVerifier(PropertyInputVerifier inputVerifier){
//			verifier = inputVerifier;
//		}
	}
	protected class ImagePropertyCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener{
		JButton m_button = new JButton("...");
		ImageProperty m_property = null;
		JTable m_table;
		public ImagePropertyCellEditor(){
			m_button.addActionListener(this);
		}
		@Override
		public Object getCellEditorValue() {
			return m_property;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			m_property = Jocular.defineImage(m_table);
			fireEditingStopped();
			
		}
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			m_table = table;
			return m_button;
		}				
	}
	protected class EnumPropertyCellEditor extends DefaultCellEditor {
		private JComboBox<Enum<?>> m_combo;
		
		// bam 131102 - This is creted with an enum combobox so we know it will be 
		//  the correct type
		@SuppressWarnings("unchecked")
		public EnumPropertyCellEditor(){
			super(new JComboBox<Enum<?>>());
			m_combo = (JComboBox<Enum<?>>)editorComponent;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column) {
			EnumProperty ep = (EnumProperty)value;
			m_combo.removeAllItems();
			for(Enum<?> e : ep.getValue().getClass().getEnumConstants()){
				m_combo.addItem(e);
			}
			return super.getTableCellEditorComponent(table, ep.getValue(), isSelected, row, column);
		}
	}
	protected class MaterialPropertyCellEditor extends DefaultCellEditor {
		private JComboBox<Enum<?>> m_combo;
		
		// bam 131102 - This is created with an enum combobox so we know it will be 
		//  the correct type
		@SuppressWarnings("unchecked")
		public MaterialPropertyCellEditor(){
			super(new JComboBox<Enum<?>>());
			m_combo = (JComboBox<Enum<?>>)editorComponent;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column) {
			MaterialProperty ep = (MaterialProperty)value;
			m_combo.removeAllItems();
			for(Enum<?> e : MaterialKey.values()){
				m_combo.addItem(e);
			}
			m_combo.setSelectedItem(ep.getKey());
			return super.getTableCellEditorComponent(table, ep.getKey(), isSelected, row, column);
		}
		
	}
	protected class BooleanPropertyCellEditor extends DefaultCellEditor {
		private JComboBox<String> m_combo;
		
		// bam 131102 - This is created with an string combobox so we know it will be 
		//  the correct type
		@SuppressWarnings("unchecked")
		public BooleanPropertyCellEditor(){
			super(new JComboBox<String>());
			m_combo = (JComboBox<String>)editorComponent;
			m_combo.addItem("false");
			m_combo.addItem("true");
			
		}
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column) {
			BooleanProperty ep = (BooleanProperty)value;
			int i = ep.getValue()?1:0;
			m_combo.setSelectedIndex(i);
			return super.getTableCellEditorComponent(table, ep.getDefiningString(), isSelected, row, column);
		}
		
	}
	protected class StringPropertyCellEditor extends DefaultCellEditor {
		
		private final JTextField m_textField;
		
		public StringPropertyCellEditor(){
					
			super(new JTextField());
			
			m_textField = (JTextField) editorComponent;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column) {
			StringProperty sp = (StringProperty)value;
			m_textField.setBorder(new LineBorder(Color.black));
			return super.getTableCellEditorComponent(table, sp.getDefiningString(), isSelected, row, column);
		}
	}
	protected class FilePropertyCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener{
		private final JButton m_button;
		private FileProperty m_property = null;
		
		public FilePropertyCellEditor(){
			m_button = new JButton();
			m_button.addActionListener(this);
			m_button.setBorderPainted(false);
			m_button.setContentAreaFilled(false);
			m_button.setHorizontalAlignment(SwingConstants.LEFT);
		}
		@Override
		public Object getCellEditorValue() {
			return m_property.getDefiningString();
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser jfc = new JFileChooser();
			jfc.setSelectedFile(m_property.getValue());
			if(jfc.showDialog(null, "Select File") == JFileChooser.APPROVE_OPTION){
				m_property = new FileProperty(m_property.getOwner(), jfc.getSelectedFile());
				fireEditingStopped();
			} else {
				fireEditingCanceled();
			}
			
			
		}
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean selected, int row, int column) {
			m_property = (FileProperty)value;
			m_button.setText(m_property.getValue().getName());
			return m_button;
		}
		
		
	}
}
