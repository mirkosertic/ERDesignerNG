package de.mogwai.erdesignerng.visual.editor.connection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;

import de.mogwai.binding.BindingInfo;
import de.mogwai.erdesignerng.dialect.Dialect;
import de.mogwai.erdesignerng.dialect.DialectFactory;
import de.mogwai.erdesignerng.model.Model;
import de.mogwai.erdesignerng.visual.editor.BaseEditor;
import de.mogwai.erdesignerng.visual.editor.DialogConstants;

/**
 * Editor for the database connection.
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2007-07-30 15:21:27 $
 */
public class DatabaseConnectionEditor extends BaseEditor {

	private DatabaseConnectionEditorView view = new DatabaseConnectionEditorView() {

		@Override
		protected void handleCancel() {
			commandCancel();
		}

		@Override
		protected void handleOk() {
			commandClose();
		}

		@Override
		public void handleDialectChange(Dialect aDialect) {
			commandChangeDialect(aDialect);
		}
	};

	private Model model;

	private BindingInfo<DatabaseConnectionDatamodel> bindingInfo = new BindingInfo<DatabaseConnectionDatamodel>();

	public DatabaseConnectionEditor(JFrame aParent, Model aModel) {
		super(aParent);

		model = aModel;

		initialize();

		view.getDialect().setModel(
				new DefaultComboBoxModel(DialectFactory.getInstance()
						.getSupportedDialects().toArray()));

		DatabaseConnectionDatamodel theDescriptor = new DatabaseConnectionDatamodel();
		theDescriptor.setDialect(model.getDialect());
		theDescriptor.setDriver(model.getProperties().getProperty(
				Model.PROPERTY_DRIVER));
		theDescriptor.setUrl(model.getProperties().getProperty(
				Model.PROPERTY_URL));
		theDescriptor.setUser(model.getProperties().getProperty(
				Model.PROPERTY_USER));
		theDescriptor.setPassword(model.getProperties().getProperty(
				Model.PROPERTY_PASSWORD));

		bindingInfo.setDefaultModel(theDescriptor);
		bindingInfo.addBinding("dialect", view.getDialect(), true);
		bindingInfo.addBinding("driver", view.getDriver(), true);
		bindingInfo.addBinding("url", view.getUrl(), true);
		bindingInfo.addBinding("user", view.getUser(), true);
		bindingInfo.addBinding("password", view.getPassword());

		bindingInfo.configure();

		bindingInfo.model2view();
	}

	private void initialize() {

		setContentPane(view);
		setTitle("Database connection");
		setResizable(false);
		pack();
	}

	@Override
	public void applyValues() throws Exception {

		DatabaseConnectionDatamodel theDescriptor = bindingInfo.getDefaultModel();

		model.setDialect(theDescriptor.getDialect());
		model.getProperties().setProperty(Model.PROPERTY_DRIVER,
				theDescriptor.getDriver());
		model.getProperties().setProperty(Model.PROPERTY_URL,
				theDescriptor.getUrl());
		model.getProperties().setProperty(Model.PROPERTY_USER,
				theDescriptor.getUser());
		model.getProperties().setProperty(Model.PROPERTY_PASSWORD,
				theDescriptor.getPassword());
	}

	private void commandClose() {

		if (bindingInfo.validate().size() == 0) {

			bindingInfo.view2model();

			setModalResult(DialogConstants.MODAL_RESULT_OK);
		}
	}

	private void commandCancel() {

		setModalResult(DialogConstants.MODAL_RESULT_CANCEL);
	}
	
	private void commandChangeDialect(Dialect aDialect) {
		
		DatabaseConnectionDatamodel theDescriptor = bindingInfo.getDefaultModel();
		theDescriptor.setDriver(aDialect.getDriverClassName());
		theDescriptor.setUrl(aDialect.getDriverURLTemplate());
		
		bindingInfo.model2view();
	}
}
