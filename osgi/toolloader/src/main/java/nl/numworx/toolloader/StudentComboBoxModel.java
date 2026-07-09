package nl.numworx.toolloader;

import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

import nl.uu.fi.dwo.rest.dom.entities.DomStudentModelContext;

public class StudentComboBoxModel 
	extends DefaultComboBoxModel<DomStudentModelContext>
	implements ComboBoxModel<DomStudentModelContext> {

	public StudentComboBoxModel(List<DomStudentModelContext> list) {
		super(list.toArray(new DomStudentModelContext[list.size()]));
	}

	public Object find(String s) {
		for(int i = 0; i < getSize(); i++) {
			DomStudentModelContext c = getElementAt(i);
			if (c == null) continue;
			if (c.getId().getIdString().equals(s)) return c;
		}
		return null; 
	}

}
