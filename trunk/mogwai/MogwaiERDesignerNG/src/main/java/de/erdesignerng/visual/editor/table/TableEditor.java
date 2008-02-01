/**
 * Mogwai ERDesigner. Copyright (C) 2002 The Mogwai Project.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package de.erdesignerng.visual.editor.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.event.ChangeEvent;

import de.erdesignerng.ERDesignerBundle;
import de.erdesignerng.exception.ElementAlreadyExistsException;
import de.erdesignerng.exception.ElementInvalidNameException;
import de.erdesignerng.model.Attribute;
import de.erdesignerng.model.DefaultValue;
import de.erdesignerng.model.Index;
import de.erdesignerng.model.IndexType;
import de.erdesignerng.model.Model;
import de.erdesignerng.model.Table;
import de.erdesignerng.modificationtracker.VetoException;
import de.erdesignerng.visual.MessagesHelper;
import de.erdesignerng.visual.editor.BaseEditor;
import de.mogwai.common.client.binding.BindingInfo;
import de.mogwai.common.client.binding.adapter.RadioButtonAdapter;
import de.mogwai.common.client.looks.UIInitializer;
import de.mogwai.common.client.looks.components.DefaultCheckBoxListModel;
import de.mogwai.common.client.looks.components.action.ActionEventProcessor;
import de.mogwai.common.client.looks.components.action.DefaultAction;
import de.mogwai.common.client.looks.components.list.DefaultListModel;

/**
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2008-02-01 17:20:27 $
 */
public class TableEditor extends BaseEditor {

    private Model model;

    private TableEditorView editingView;

    private BindingInfo<Table> tableBindingInfo = new BindingInfo<Table>();

    private BindingInfo<Attribute> attributeBindingInfo = new BindingInfo<Attribute>();

    private BindingInfo<Index> indexBindingInfo = new BindingInfo<Index>();

    private DefaultListModel attributeListModel;

    private DefaultListModel domainListModel;

    private DefaultListModel indexListModel;

    private DefaultComboBoxModel defaultValuesListModel = new DefaultComboBoxModel();

    private Map<String, Attribute> knownAttributeValues = new HashMap<String, Attribute>();

    private Map<String, Index> knownIndexValues = new HashMap<String, Index>();

    private List<Attribute> removedAttributes = new ArrayList<Attribute>();

    private List<Index> removedIndexes = new ArrayList<Index>();

    private DefaultAction okAction = new DefaultAction(new ActionEventProcessor() {

        public void processActionEvent(ActionEvent e) {
            commandOk();
        }
    }, this, ERDesignerBundle.OK);

    private DefaultAction cancelAction = new DefaultAction(new ActionEventProcessor() {

        public void processActionEvent(ActionEvent e) {
            commandCancel();
        }
    }, this, ERDesignerBundle.CANCEL);

    private DefaultAction newAttributeAction = new DefaultAction(new ActionEventProcessor() {

        public void processActionEvent(ActionEvent e) {
            commandNewAttribute(e);
        }
    }, this, ERDesignerBundle.NEW);

    private DefaultAction deleteAttributeAction = new DefaultAction(new ActionEventProcessor() {

        public void processActionEvent(ActionEvent e) {
            commandDeleteAttribute(e);
        }
    }, this, ERDesignerBundle.DELETE);

    private DefaultAction updateAttribute = new DefaultAction(new ActionEventProcessor() {

        public void processActionEvent(ActionEvent e) {
            commandUpdateAttribute(e);
        }
    }, this, ERDesignerBundle.UPDATE);

    private DefaultAction newIndexAction = new DefaultAction(new ActionEventProcessor() {

        public void processActionEvent(ActionEvent e) {
            commandNewIndex();
        }
    }, this, ERDesignerBundle.NEW);

    private DefaultAction deleteIndexAction = new DefaultAction(new ActionEventProcessor() {

        public void processActionEvent(ActionEvent e) {
            commandDeleteIndex();
        }
    }, this, ERDesignerBundle.DELETE);

    private DefaultAction updateIndex = new DefaultAction(new ActionEventProcessor() {

        public void processActionEvent(ActionEvent e) {
            commandUpdateIndex();
        }
    }, this, ERDesignerBundle.UPDATE);

    public TableEditor(Model aModel, Component aParent) {
        super(aParent, ERDesignerBundle.ENTITYEDITOR);
        initialize();

        attributeListModel = editingView.getAttributeList().getModel();
        indexListModel = editingView.getIndexList().getModel();

        model = aModel;
        editingView.getAttributeList().setCellRenderer(new AttributeListCellRenderer(this));

        for (DefaultValue theValue : aModel.getDefaultValues()) {
            defaultValuesListModel.addElement(theValue);
        }
        editingView.getDefault().setModel(defaultValuesListModel);

        tableBindingInfo.addBinding("name", editingView.getEntityName(), true);
        tableBindingInfo.addBinding("comment", editingView.getEntityComment());
        tableBindingInfo.configure();

        attributeBindingInfo.addBinding("name", editingView.getAttributeName(), true);
        attributeBindingInfo.addBinding("comment", editingView.getAttributeComment());
        attributeBindingInfo.addBinding("nullable", editingView.getNullable());
        attributeBindingInfo.addBinding("defaultValue", editingView.getDefault());
        attributeBindingInfo.configure();

        indexBindingInfo.addBinding("name", editingView.getIndexName(), true);

        RadioButtonAdapter theAdapter = new RadioButtonAdapter();
        theAdapter.addMapping(IndexType.PRIMARYKEY, editingView.getPrimaryIndex());
        theAdapter.addMapping(IndexType.UNIQUE, editingView.getUniqueIndex());
        theAdapter.addMapping(IndexType.NONUNIQUE, editingView.getNotUniqueIndex());
        indexBindingInfo.addBinding("indexType", theAdapter);
        indexBindingInfo.configure();

        UIInitializer.getInstance().initialize(this);
    }

    /**
     * This method initializes this.
     */
    private void initialize() {

        editingView = new TableEditorView();
        editingView.getOkButton().setAction(okAction);
        editingView.getCancelButton().setAction(cancelAction);
        editingView.getMainTabbedPane().addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent e) {
                commandTabStateChange(e);
            }
        });
        editingView.getAttributeList().addListSelectionListener(new javax.swing.event.ListSelectionListener() {

            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                commandAttributeListValueChanged(e);
            }
        });
        editingView.getNewButton().setAction(newAttributeAction);
        editingView.getDeleteButton().setAction(deleteAttributeAction);
        editingView.getUpdateAttributeButton().setAction(updateAttribute);
        editingView.getUpdateIndexButton().setAction(updateIndex);
        editingView.getNewIndexButton().setAction(newIndexAction);
        editingView.getDeleteIndexButton().setAction(deleteIndexAction);
        editingView.getIndexList().addListSelectionListener(new javax.swing.event.ListSelectionListener() {

            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                commandIndexListValueChanged(e);
            }
        });

        setContentPane(editingView);
        pack();

        editingView.getUpdateIndexButton().setEnabled(false);
    }

    public void initializeFor(Table aTable) {

        tableBindingInfo.setDefaultModel(aTable);
        tableBindingInfo.model2view();

        editingView.getEntityName().setName(aTable.getName());
        for (Attribute theAttribute : aTable.getAttributes()) {

            Attribute theClone = theAttribute.clone();

            attributeListModel.add(theClone);
            knownAttributeValues.put(theClone.getSystemId(), theClone);
        }
        for (Index theIndex : aTable.getIndexes()) {

            Index theClone = theIndex.clone();

            indexListModel.add(theClone);
            knownIndexValues.put(theClone.getSystemId(), theClone);
        }

        updateAttributeEditFields();
    }

    private void commandOk() {
        if (tableBindingInfo.validate().size() == 0) {
            setModalResult(MODAL_RESULT_OK);
        }
    }

    private void commandTabStateChange(ChangeEvent e) {
        int theIndex = editingView.getMainTabbedPane().getSelectedIndex();
        if (theIndex == 0) {
            attributeBindingInfo.setDefaultModel(null);
            updateAttributeEditFields();
        }
        if (theIndex == 1) {
            indexBindingInfo.setDefaultModel(null);
            updateIndexEditFields();
        }
    }

    private void commandUpdateAttribute(java.awt.event.ActionEvent evt) {
        Attribute theModel = attributeBindingInfo.getDefaultModel();
        Vector theValidationResult = attributeBindingInfo.validate();
        if (theValidationResult.size() == 0) {
            attributeBindingInfo.view2model();

            if (!attributeListModel.contains(theModel)) {

                attributeListModel.add(theModel);
                knownAttributeValues.put(theModel.getSystemId(), theModel);
            }

            updateAttributeEditFields();
        }
    }

    private void updateAttributeEditFields() {

        Attribute theValue = attributeBindingInfo.getDefaultModel();

        if (theValue != null) {

            boolean isNew = !attributeListModel.contains(theValue);

            editingView.getNewButton().setEnabled(true);
            editingView.getDeleteButton().setEnabled(!isNew);
            editingView.getAttributeName().setEnabled(true);
            editingView.getNullable().setEnabled(true);
            editingView.getDefault().setEnabled(true);

        } else {
            editingView.getNewButton().setEnabled(true);
            editingView.getDeleteButton().setEnabled(false);
            editingView.getAttributeName().setEnabled(false);
            editingView.getNullable().setEnabled(false);
            editingView.getDefault().setEnabled(false);
        }

        attributeBindingInfo.model2view();

        editingView.getAttributeList().invalidate();
        editingView.getAttributeList().setSelectedValue(attributeBindingInfo.getDefaultModel(), true);

    }

    private void updateIndexEditFields() {
        Index theValue = indexBindingInfo.getDefaultModel();

        if (theValue != null) {

            boolean isNew = !indexListModel.contains(theValue);

            editingView.getNewIndexButton().setEnabled(true);
            editingView.getUpdateIndexButton().setEnabled(true);

            indexBindingInfo.setEnabled(true);

            editingView.getIndexFieldList().setEnabled(true);
            editingView.getPrimaryIndex().setEnabled(true);
            editingView.getUniqueIndex().setEnabled(true);
            editingView.getNotUniqueIndex().setEnabled(true);

            editingView.getDeleteIndexButton().setEnabled(!isNew);
        } else {

            editingView.getNewIndexButton().setEnabled(true);
            editingView.getDeleteIndexButton().setEnabled(false);
            editingView.getUpdateIndexButton().setEnabled(false);

            editingView.getIndexFieldList().setEnabled(false);

            indexBindingInfo.setEnabled(false);
        }

        indexBindingInfo.model2view();

        DefaultCheckBoxListModel<Attribute> theModel = editingView.getIndexFieldList().getModel();
        theModel.clear();

        List<Attribute> theSelectedAttributes = new ArrayList<Attribute>();

        for (int i = 0; i < attributeListModel.getSize(); i++) {
            Attribute theAttribute = (Attribute) attributeListModel.get(i);
            theModel.add(theAttribute);

            if (theValue != null) {
                if (theValue.getAttributes().findBySystemId(theAttribute.getSystemId()) != null) {
                    theSelectedAttributes.add(theAttribute);
                }
            }
        }
        editingView.getIndexFieldList().setSelectedItems(theSelectedAttributes);

        editingView.getIndexList().invalidate();
        editingView.getIndexList().setSelectedValue(indexBindingInfo.getDefaultModel(), true);
    }

    private void commandAttributeListValueChanged(javax.swing.event.ListSelectionEvent evt) {

        int index = editingView.getAttributeList().getSelectedIndex();
        if (index >= 0) {
            attributeBindingInfo.setDefaultModel((Attribute) attributeListModel.get(index));
        }

        updateAttributeEditFields();

    }

    private void commandIndexListValueChanged(javax.swing.event.ListSelectionEvent evt) {

        int index = editingView.getIndexList().getSelectedIndex();
        if (index >= 0) {
            indexBindingInfo.setDefaultModel((Index) indexListModel.get(index));
        }

        updateIndexEditFields();

    }

    private void commandDeleteAttribute(java.awt.event.ActionEvent aEvent) {

        Attribute theAttribute = attributeBindingInfo.getDefaultModel();

        if (!model.checkIfUsedAsForeignKey(tableBindingInfo.getDefaultModel(), theAttribute)) {

            if (MessagesHelper.displayQuestionMessage(this, ERDesignerBundle.DOYOUREALLYWANTTODELETE)) {
                knownAttributeValues.remove(theAttribute.getSystemId());
                attributeListModel.remove(theAttribute);

                removedAttributes.add(theAttribute);
            }
        } else {
            MessagesHelper.displayErrorMessage(this, getResourceHelper().getText(
                    ERDesignerBundle.ATTRIBUTEISUSEDINFOREIGNKEYS));
        }
    }

    private void commandNewAttribute(java.awt.event.ActionEvent evt) {
        attributeBindingInfo.setDefaultModel(new Attribute());
        updateAttributeEditFields();
    }

    private void commandDeleteIndex() {
        Index theAttribute = indexBindingInfo.getDefaultModel();

        if (MessagesHelper.displayQuestionMessage(this, ERDesignerBundle.DOYOUREALLYWANTTODELETE)) {
            knownIndexValues.remove(theAttribute.getSystemId());
            indexListModel.remove(theAttribute);

            removedIndexes.add(theAttribute);
        }
    }

    private void commandNewIndex() {
        indexBindingInfo.setDefaultModel(new Index());
        updateIndexEditFields();
    }

    private void commandUpdateIndex() {
        Index theModel = indexBindingInfo.getDefaultModel();
        Vector theValidationResult = indexBindingInfo.validate();
        if (theValidationResult.size() == 0) {
            indexBindingInfo.view2model();
            
            if (theModel.getIndexType().equals(IndexType.PRIMARYKEY)) {
                for (int i = 0; i < indexListModel.getSize(); i++) {
                    Index theIndex = (Index) indexListModel.get(i);
                    if ((theIndex.getIndexType().equals(IndexType.PRIMARYKEY) && (!theModel.equals(theIndex)))) {
                        MessagesHelper.displayErrorMessage(this, getResourceHelper().getText(
                                ERDesignerBundle.THEREISALREADYAPRIMARYKEY));
                        return;
                    }
                }
            }
            
            if (theModel.getAttributes().size() == 0) {
                MessagesHelper.displayErrorMessage(this, getResourceHelper().getText(
                        ERDesignerBundle.INDEXMUSTHAVEATLEASTONEATTRIBUTE));
                return;
            }

            if (!indexListModel.contains(theModel)) {
                indexListModel.add(theModel);
            }

            theModel.getAttributes().clear();
            theModel.getAttributes().addAll(editingView.getIndexFieldList().getSelectedItems());

            updateIndexEditFields();
        }

    }

    @Override
    public void applyValues() throws ElementAlreadyExistsException, ElementInvalidNameException, VetoException {

        Table theTable = tableBindingInfo.getDefaultModel();
        Table theTempTable = new Table();

        tableBindingInfo.setDefaultModel(theTempTable);
        tableBindingInfo.view2model();
        if (theTable.isRenamed(theTempTable.getName())) {
            model.renameTable(theTable, theTempTable.getName());
        }

        if (!theTable.getComment().equals(theTempTable.getComment())) {
            model.changeTableComment(theTable, theTempTable.getComment());
        }

        if (!model.getTables().contains(theTable)) {
            model.addTable(theTable);
        } else {
            for (Attribute theAttribute : removedAttributes) {
                model.removeAttributeFromTable(theTable, theAttribute);
            }
            for (Index theIndex : removedIndexes) {
                model.removeIndex(theTable, theIndex);
            }

        }

        for (String theKey : knownAttributeValues.keySet()) {
            Attribute theAttribute = knownAttributeValues.get(theKey);

            Attribute theExistantAttribute = theTable.getAttributes().findBySystemId(theKey);
            if (theExistantAttribute == null) {
                model.addAttributeToTable(theTable, theAttribute);
            } else {
                try {
                    if (theExistantAttribute.isRenamed(theAttribute)) {
                        model.renameAttribute(theExistantAttribute, theAttribute.getName());
                    } else {
                        if (theExistantAttribute.isModified(theAttribute)) {
                            model.changeAttribute(theExistantAttribute, theAttribute);
                        }
                    }
                } catch (VetoException e1) {
                    throw e1;
                } catch (Exception e) {
                    logFatalError(e);
                }
            }
        }

        for (String theKey : knownIndexValues.keySet()) {
            Index theIndex = knownIndexValues.get(theKey);

            Index theExistantIndex = theTable.getIndexes().findBySystemId(theKey);
            if (theExistantIndex == null) {
                model.addIndexToTable(theTable, theIndex);
            } else {
                try {
                    if (theExistantIndex.isModified(theIndex)) {
                        model.changeIndex(theExistantIndex, theIndex);
                    }
                } catch (VetoException e1) {
                    throw e1;
                } catch (Exception e) {
                    logFatalError(e);
                }
            }
        }

    }

    public boolean isPrimaryKey(Attribute aAttribute) {
        for (int i = 0; i < indexListModel.getSize(); i++) {
            Index theIndex = (Index) indexListModel.get(i);
            if (IndexType.PRIMARYKEY.equals(theIndex.getIndexType())) {
                return theIndex.getAttributes().contains(aAttribute);
            }
        }
        return false;
    }
}
