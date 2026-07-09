package nl.numworx.toolloader;

import java.awt.Component;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import nl.uu.fi.dwo.rest.dom.entities.DomStudentModelContext;

public class StudentModelRenderer implements ListCellRenderer<DomStudentModelContext> {

	DefaultListCellRenderer delegate = new DefaultListCellRenderer();
	
	@Override
	public Component getListCellRendererComponent(JList<? extends DomStudentModelContext> arg0,
			DomStudentModelContext context, int arg2, boolean arg3, boolean arg4) {
		String locale = Locale.getDefault().getLanguage();
		String title = context == null ? null : context.getModelStructure().getInfo().getTitle().getOrDefault(locale, "Unknown");
		return delegate.getListCellRendererComponent(arg0, title, arg2, arg3, arg4);
	}

}
