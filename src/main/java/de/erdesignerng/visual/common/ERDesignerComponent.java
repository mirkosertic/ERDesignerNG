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
package de.erdesignerng.visual.common;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;

import org.jdesktop.swingworker.SwingWorker;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphLayoutCacheEvent.GraphLayoutCacheChange;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.VertexView;

import de.erdesignerng.ERDesignerBundle;
import de.erdesignerng.dialect.Dialect;
import de.erdesignerng.dialect.DialectFactory;
import de.erdesignerng.dialect.GenericConnectionProvider;
import de.erdesignerng.dialect.ReverseEngineeringNotifier;
import de.erdesignerng.dialect.ReverseEngineeringOptions;
import de.erdesignerng.dialect.ReverseEngineeringStrategy;
import de.erdesignerng.dialect.SQLGenerator;
import de.erdesignerng.dialect.Statement;
import de.erdesignerng.dialect.StatementList;
import de.erdesignerng.io.GenericFileFilter;
import de.erdesignerng.io.ModelFileFilter;
import de.erdesignerng.model.Comment;
import de.erdesignerng.model.Model;
import de.erdesignerng.model.ModelBasedConnectionProvider;
import de.erdesignerng.model.ModelIOUtilities;
import de.erdesignerng.model.ModelItem;
import de.erdesignerng.model.Relation;
import de.erdesignerng.model.SubjectArea;
import de.erdesignerng.model.Table;
import de.erdesignerng.model.serializer.repository.DictionaryModelSerializer;
import de.erdesignerng.model.serializer.repository.RepositoryEntryDesciptor;
import de.erdesignerng.model.serializer.repository.entities.RepositoryEntity;
import de.erdesignerng.modificationtracker.HistoryModificationTracker;
import de.erdesignerng.modificationtracker.VetoException;
import de.erdesignerng.util.ApplicationPreferences;
import de.erdesignerng.util.ConnectionDescriptor;
import de.erdesignerng.visual.DisplayLevel;
import de.erdesignerng.visual.DisplayOrder;
import de.erdesignerng.visual.ERDesignerGraph;
import de.erdesignerng.visual.ExportType;
import de.erdesignerng.visual.MessagesHelper;
import de.erdesignerng.visual.cells.CommentCell;
import de.erdesignerng.visual.cells.ModelCell;
import de.erdesignerng.visual.cells.RelationEdge;
import de.erdesignerng.visual.cells.SubjectAreaCell;
import de.erdesignerng.visual.cells.TableCell;
import de.erdesignerng.visual.cells.views.CellViewFactory;
import de.erdesignerng.visual.cells.views.TableCellView;
import de.erdesignerng.visual.editor.DialogConstants;
import de.erdesignerng.visual.editor.classpath.ClasspathEditor;
import de.erdesignerng.visual.editor.comment.CommentEditor;
import de.erdesignerng.visual.editor.completecompare.CompleteCompareEditor;
import de.erdesignerng.visual.editor.connection.DatabaseConnectionEditor;
import de.erdesignerng.visual.editor.connection.RepositoryConnectionEditor;
import de.erdesignerng.visual.editor.domain.DomainEditor;
import de.erdesignerng.visual.editor.preferences.PreferencesEditor;
import de.erdesignerng.visual.editor.repository.LoadFromRepositoryEditor;
import de.erdesignerng.visual.editor.repository.MigrationScriptEditor;
import de.erdesignerng.visual.editor.repository.SaveToRepositoryEditor;
import de.erdesignerng.visual.editor.reverseengineer.ReverseEngineerEditor;
import de.erdesignerng.visual.editor.reverseengineer.TablesSelectEditor;
import de.erdesignerng.visual.editor.sql.SQLEditor;
import de.erdesignerng.visual.editor.table.TableEditor;
import de.erdesignerng.visual.export.Exporter;
import de.erdesignerng.visual.export.ImageExporter;
import de.erdesignerng.visual.export.SVGExporter;
import de.erdesignerng.visual.layout.Layouter;
import de.erdesignerng.visual.layout.LayouterFactory;
import de.erdesignerng.visual.layout.SizeableLayouter;
import de.erdesignerng.visual.plaf.basic.ERDesignerGraphUI;
import de.erdesignerng.visual.tools.CommentTool;
import de.erdesignerng.visual.tools.EntityTool;
import de.erdesignerng.visual.tools.HandTool;
import de.erdesignerng.visual.tools.RelationTool;
import de.erdesignerng.visual.tools.ToolEnum;
import de.mogwai.common.client.looks.UIInitializer;
import de.mogwai.common.client.looks.components.DefaultCheckboxMenuItem;
import de.mogwai.common.client.looks.components.DefaultComboBox;
import de.mogwai.common.client.looks.components.DefaultScrollPane;
import de.mogwai.common.client.looks.components.DefaultToggleButton;
import de.mogwai.common.client.looks.components.DefaultToolbar;
import de.mogwai.common.client.looks.components.action.ActionEventProcessor;
import de.mogwai.common.client.looks.components.action.DefaultAction;
import de.mogwai.common.client.looks.components.menu.DefaultMenu;
import de.mogwai.common.client.looks.components.menu.DefaultMenuItem;
import de.mogwai.common.client.looks.components.menu.DefaultRadioButtonMenuItem;
import de.mogwai.common.i18n.ResourceHelper;
import de.mogwai.common.i18n.ResourceHelperProvider;

/**
 * The ERDesigner Editing Component.
 * 
 * This is the heart of the system.
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2008-11-19 17:57:11 $
 */
public class ERDesignerComponent implements ResourceHelperProvider {

    private class ERDesignerGraphModelListener implements GraphModelListener {

        /**
         * {@inheritDoc}
         */
        public void graphChanged(GraphModelEvent aEvent) {
            GraphLayoutCacheChange theChange = aEvent.getChange();

            Object[] theChangedObjects = theChange.getChanged();
            Map theChangedAttributes = theChange.getPreviousAttributes();
            if (theChangedAttributes != null) {
                for (Object theChangedObject : theChangedObjects) {
                    Map theAttributes = (Map) theChangedAttributes.get(theChangedObject);

                    if (theChangedObject instanceof ModelCell) {

                        ModelCell theCell = (ModelCell) theChangedObject;
                        if (theAttributes != null) {
                            theCell.transferAttributesToProperties(theAttributes);
                        }
                    }

                    if (theChangedObject instanceof SubjectAreaCell) {

                        SubjectAreaCell theCell = (SubjectAreaCell) theChangedObject;
                        if (theCell.getChildCount() == 0) {
                            commandRemoveSubjectArea(theCell);
                        } else {
                            commandUpdateSubjectArea(theCell);
                        }
                    }
                }
            }

        }
    }

    private static class ReverseEngineeringResult {

        private Exception exception;

        private Model model;

        public ReverseEngineeringResult(Exception aException, Model aModel) {
            exception = aException;
            model = aModel;
        }

        public Exception getException() {
            return exception;
        }

        public Model getModel() {
            return model;
        }
    }

    private final class ReverseEngineerSwingWorker extends SwingWorker<ReverseEngineeringResult, String> {

        private Model tempModel;

        private Connection connection;

        private ReverseEngineeringOptions options;

        private ReverseEngineeringStrategy strategy;

        private ReverseEngineerSwingWorker(Model aModel, ReverseEngineeringOptions options,
                ReverseEngineeringStrategy strategy, Connection connection) {
            this.options = options;
            this.strategy = strategy;
            this.connection = connection;
            this.tempModel = aModel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ReverseEngineeringResult doInBackground() throws Exception {
            try {
                ReverseEngineeringNotifier theNotifier = new ReverseEngineeringNotifier() {

                    public void notifyMessage(String aResourceKey, String... aValues) {
                        String theMessage = MessageFormat.format(getResourceHelper().getText(aResourceKey),
                                (Object[]) aValues);
                        publish(new String[] { theMessage });
                    }

                };

                strategy.updateModelFromConnection(tempModel, worldConnector, connection, options, theNotifier);

                return new ReverseEngineeringResult(null, tempModel);

            } catch (Exception e) {
                return new ReverseEngineeringResult(e, null);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void process(List<String> aString) {
            for (String theMessage : aString) {
                worldConnector.setStatusText(theMessage);
            }
        }
    }

    private DefaultAction classpathAction;

    private File currentEditingFile;

    private RepositoryEntryDesciptor currentRepositoryEntry;

    private DefaultAction dbConnectionAction;

    private DefaultAction repositoryConnectionAction;

    private DefaultAction domainsAction;

    private DefaultAction entityAction;

    private JToggleButton entityButton;

    private DefaultAction exitAction;

    private DefaultAction exportAction;

    private DefaultAction exportSVGAction;

    private ERDesignerGraph graph;

    private GraphModel graphModel;

    private DefaultAction handAction;

    private JToggleButton handButton;

    private DefaultAction commentAction;

    private JToggleButton commentButton;

    private DefaultAction layoutAction;

    private GraphLayoutCache layoutCache;

    private DefaultAction layoutgraphvizAction;

    private DefaultAction layoutradialAction;

    private DefaultAction layoutspringAction;

    private DefaultAction layoutgridAction;

    private DefaultAction layouttreeAction;

    private DefaultAction layoutfrAction;

    private DefaultAction loadAction;

    private DefaultAction lruAction;

    private DefaultMenu lruMenu;

    private DefaultMenu storedConnections;

    private Model model;

    private DefaultAction newAction;

    private ApplicationPreferences preferences;

    private DefaultAction relationAction;

    private JToggleButton relationButton;

    private DefaultAction reverseEngineerAction;

    private DefaultAction completeCompareAction;

    private DefaultAction saveAction;

    private DefaultAction saveToRepository;

    private DefaultScrollPane scrollPane = new DefaultScrollPane();

    private ERDesignerWorldConnector worldConnector;

    private DefaultAction zoomAction;

    private DefaultComboBox zoomBox = new DefaultComboBox();

    private DefaultAction zoomInAction;

    private DefaultAction zoomOutAction;

    private DefaultAction preferencesAction;

    private DefaultAction generateSQL;

    private DefaultAction generateChangelog;

    private DefaultAction displayCommentsAction;

    private DefaultCheckboxMenuItem displayCommentsMenuItem;

    private DefaultAction displayGridAction;

    private DefaultCheckboxMenuItem displayGridMenuItem;

    private DefaultRadioButtonMenuItem displayAllMenuItem;

    private DefaultAction displayAllAction;

    private DefaultAction displayPKOnlyAction;

    private DefaultAction displayPKAndFK;

    private DefaultRadioButtonMenuItem displayNaturalOrderMenuItem;

    private DefaultAction displayNaturalOrderAction;

    private DefaultAction displayAscendingOrderAction;

    private DefaultAction displayDescendingOrderAction;

    private DefaultAction createMigrationScriptAction;

    private DefaultMenu repositoryUtilsMenu;

    private static final ZoomInfo ZOOMSCALE_HUNDREDPERCENT = new ZoomInfo("100%", 1);

    public ERDesignerComponent(ApplicationPreferences aPreferences, ERDesignerWorldConnector aConnector) {
        worldConnector = aConnector;
        preferences = aPreferences;
        initActions();
    }

    protected void initActions() {

        reverseEngineerAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent aEvent) {
                commandReverseEngineer();
            }

        }, this, ERDesignerBundle.REVERSEENGINEER);

        preferencesAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent aEvent) {
                commandPreferences();
            }

        }, this, ERDesignerBundle.PREFERENCES);

        saveAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent aEvent) {
                commandSaveFile();
            }

        }, this, ERDesignerBundle.SAVEMODEL);

        saveToRepository = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent aEvent) {
                commandSaveToRepository();
            }

        }, this, ERDesignerBundle.SAVEMODELTODB);

        relationAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetTool(ToolEnum.RELATION);
            }

        }, this, ERDesignerBundle.RELATION);

        newAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandNew();
            }
        }, this, ERDesignerBundle.NEWMODEL);

        lruAction = new DefaultAction(this, ERDesignerBundle.RECENTLYUSEDFILES);

        layoutgraphvizAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandLayout(LayouterFactory.getInstance().createGraphvizLayouter());
            }

        }, this, ERDesignerBundle.LAYOUTBYGRAPHVIZ);

        layoutradialAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandLayout(LayouterFactory.getInstance().createRadialLayouter());
            }

        }, this, ERDesignerBundle.LAYOUTBYRADIAL);

        layoutspringAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandLayout(LayouterFactory.getInstance().createSpringLayouter());
            }

        }, this, ERDesignerBundle.LAYOUTBYSPRING);

        layoutgridAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandLayout(LayouterFactory.getInstance().createGridLayouter());
            }

        }, this, ERDesignerBundle.LAYOUTBYGRID);

        layouttreeAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandLayout(LayouterFactory.getInstance().createTreeLayouter());
            }

        }, this, ERDesignerBundle.LAYOUTBYTREE);

        layoutfrAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandLayout(LayouterFactory.getInstance().createFRLayouter());
            }

        }, this, ERDesignerBundle.LAYOUTBYFR);

        loadAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent aEvent) {
                commandOpenFromFile();
            }

        }, this, ERDesignerBundle.LOADMODEL);

        handAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetTool(ToolEnum.HAND);
            }

        }, this, ERDesignerBundle.HAND);

        commentAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetTool(ToolEnum.COMMENT);
            }

        }, this, ERDesignerBundle.COMMENT);

        layoutAction = new DefaultAction(this, ERDesignerBundle.LAYOUT);

        exportSVGAction = new DefaultAction(this, ERDesignerBundle.ASSVG);

        entityAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetTool(ToolEnum.ENTITY);
            }

        }, this, ERDesignerBundle.ENTITY);

        exportAction = new DefaultAction(this, ERDesignerBundle.EXPORT);

        exitAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                worldConnector.exitApplication();
            }

        }, this, ERDesignerBundle.EXITPROGRAM);

        classpathAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandClasspath();
            }

        }, this, ERDesignerBundle.CLASSPATH);

        dbConnectionAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandDBConnection();
            }

        }, this, ERDesignerBundle.DBCONNECTION);

        repositoryConnectionAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandRepositoryConnection();
            }

        }, this, ERDesignerBundle.REPOSITORYCONNECTION);

        domainsAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandEditDomains();
            }

        }, this, ERDesignerBundle.DOMAINEDITOR);

        zoomAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent aEvent) {
                commandSetZoom((ZoomInfo) ((JComboBox) aEvent.getSource()).getSelectedItem());
            }
        }, this, ERDesignerBundle.ZOOM);

        zoomInAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandZoomIn();
            }

        }, this, ERDesignerBundle.ZOOMIN);

        zoomOutAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandZoomOut();
            }

        }, this, ERDesignerBundle.ZOOMOUT);

        generateSQL = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandGenerateSQL();
            }

        }, this, ERDesignerBundle.GENERATECREATEDBDDL);

        generateChangelog = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandGenerateChangelogSQL();
            }

        }, this, ERDesignerBundle.GENERATECHANGELOG);

        completeCompareAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandCompleteCompare();
            }

        }, this, ERDesignerBundle.COMPLETECOMPARE);

        createMigrationScriptAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent aEvent) {
                commandCreateMigrationScript();
            }

        }, this, ERDesignerBundle.CREATEMIGRATIONSCRIPT);

        lruMenu = new DefaultMenu(lruAction);

        DefaultAction theStoredConnectionsAction = new DefaultAction(this, ERDesignerBundle.STOREDDBCONNECTION);
        storedConnections = new DefaultMenu(theStoredConnectionsAction);

        ERDesignerToolbarEntry theFileMenu = new ERDesignerToolbarEntry(ERDesignerBundle.FILE);
        if (worldConnector.supportsPreferences()) {
            theFileMenu.add(preferencesAction);
            theFileMenu.addSeparator();
        }

        theFileMenu.add(newAction);
        theFileMenu.addSeparator();
        theFileMenu.add(saveAction);
        theFileMenu.add(loadAction);

        theFileMenu.addSeparator();
        theFileMenu.add(new DefaultMenuItem(repositoryConnectionAction));
        theFileMenu.add(saveToRepository);

        DefaultMenuItem theLoadFromDBMenu = new DefaultMenuItem(new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandOpenFromRepository();
            }

        }, this, ERDesignerBundle.LOADMODELFROMDB));

        theFileMenu.add(theLoadFromDBMenu);

        repositoryUtilsMenu = new DefaultMenu(this, ERDesignerBundle.REPOSITORYUTILS);
        repositoryUtilsMenu.add(new DefaultMenuItem(createMigrationScriptAction));

        UIInitializer.getInstance().initialize(repositoryUtilsMenu);

        theFileMenu.add(repositoryUtilsMenu);

        theFileMenu.addSeparator();

        DefaultMenu theExportMenu = new DefaultMenu(exportAction);

        List<String> theSupportedFormats = ImageExporter.getSupportedFormats();
        if (theSupportedFormats.contains("IMAGE/PNG")) {
            DefaultMenu theSingleExportMenu = new DefaultMenu(this, ERDesignerBundle.ASPNG);
            theExportMenu.add(theSingleExportMenu);

            addExportEntries(theSingleExportMenu, new ImageExporter("png"));
        }
        if (theSupportedFormats.contains("IMAGE/JPEG")) {
            DefaultMenu theSingleExportMenu = new DefaultMenu(this, ERDesignerBundle.ASJPEG);
            theExportMenu.add(theSingleExportMenu);

            addExportEntries(theSingleExportMenu, new ImageExporter("jpg"));
        }
        if (theSupportedFormats.contains("IMAGE/BMP")) {
            DefaultMenu theSingleExportMenu = new DefaultMenu(this, ERDesignerBundle.ASBMP);
            theExportMenu.add(theSingleExportMenu);

            addExportEntries(theSingleExportMenu, new ImageExporter("bmp"));
        }

        DefaultMenu theSVGExportMenu = new DefaultMenu(exportSVGAction);

        theExportMenu.add(theSVGExportMenu);
        addExportEntries(theSVGExportMenu, new SVGExporter());

        UIInitializer.getInstance().initialize(theExportMenu);

        theFileMenu.add(theExportMenu);

        theFileMenu.addSeparator();
        theFileMenu.add(lruMenu);

        if (worldConnector.supportsExitApplication()) {
            theFileMenu.addSeparator();
            theFileMenu.add(new DefaultMenuItem(exitAction));
        }

        ERDesignerToolbarEntry theDBMenu = new ERDesignerToolbarEntry(ERDesignerBundle.DATABASE);

        boolean addSeparator = false;
        if (worldConnector.supportsClasspathEditor()) {
            theDBMenu.add(new DefaultMenuItem(classpathAction));
            addSeparator = true;
        }

        if (worldConnector.supportsConnectionEditor()) {
            theDBMenu.add(new DefaultMenuItem(dbConnectionAction));
            theDBMenu.add(storedConnections);
            addSeparator = true;
        }

        if (addSeparator) {
            theDBMenu.addSeparator();
        }

        theDBMenu.add(domainsAction);
        theDBMenu.addSeparator();

        theDBMenu.add(new DefaultMenuItem(reverseEngineerAction));
        theDBMenu.addSeparator();
        theDBMenu.add(new DefaultMenuItem(generateSQL));
        theDBMenu.addSeparator();
        theDBMenu.add(new DefaultMenuItem(generateChangelog));
        theDBMenu.addSeparator();

        theDBMenu.add(new DefaultMenuItem(completeCompareAction));

        ERDesignerToolbarEntry theViewMenu = new ERDesignerToolbarEntry(ERDesignerBundle.VIEW);

        DefaultMenu theLayoutMenu = new DefaultMenu(layoutAction);
        theLayoutMenu.add(new DefaultMenuItem(layoutgraphvizAction));
        theLayoutMenu.add(new DefaultMenuItem(layoutradialAction));
        theLayoutMenu.add(new DefaultMenuItem(layoutspringAction));
        theLayoutMenu.add(new DefaultMenuItem(layouttreeAction));
        theLayoutMenu.add(new DefaultMenuItem(layoutgridAction));
        theLayoutMenu.add(new DefaultMenuItem(layoutfrAction));

        theViewMenu.add(theLayoutMenu);
        theViewMenu.addSeparator();

        displayCommentsAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                DefaultCheckboxMenuItem theItem = (DefaultCheckboxMenuItem) e.getSource();
                commandSetDisplayCommentsState(theItem.isSelected());
            }

        }, this, ERDesignerBundle.DISPLAYCOMMENTS);

        displayCommentsMenuItem = new DefaultCheckboxMenuItem(displayCommentsAction);
        displayCommentsMenuItem.setSelected(true);
        theViewMenu.add(displayCommentsMenuItem);

        displayGridAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                DefaultCheckboxMenuItem theItem = (DefaultCheckboxMenuItem) e.getSource();
                commandSetDisplayGridState(theItem.isSelected());
            }

        }, this, ERDesignerBundle.DISPLAYGRID);

        displayGridMenuItem = new DefaultCheckboxMenuItem(displayGridAction);
        theViewMenu.add(displayGridMenuItem);

        DefaultMenu theDisplayLevelMenu = new DefaultMenu(this, ERDesignerBundle.DISPLAYLEVEL);
        theViewMenu.add(theDisplayLevelMenu);

        displayAllAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetDisplayLevel(DisplayLevel.ALL);
            }

        }, this, ERDesignerBundle.DISPLAYALL);

        displayPKOnlyAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetDisplayLevel(DisplayLevel.PRIMARYKEYONLY);
            }

        }, this, ERDesignerBundle.DISPLAYPRIMARYKEY);

        displayPKAndFK = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetDisplayLevel(DisplayLevel.PRIMARYKEYSANDFOREIGNKEYS);
            }

        }, this, ERDesignerBundle.DISPLAYPRIMARYKEYANDFOREIGNKEY);

        displayAllMenuItem = new DefaultRadioButtonMenuItem(displayAllAction);
        DefaultRadioButtonMenuItem thePKOnlyItem = new DefaultRadioButtonMenuItem(displayPKOnlyAction);
        DefaultRadioButtonMenuItem thePKAndFKItem = new DefaultRadioButtonMenuItem(displayPKAndFK);

        ButtonGroup theDisplayLevelGroup = new ButtonGroup();
        theDisplayLevelGroup.add(displayAllMenuItem);
        theDisplayLevelGroup.add(thePKOnlyItem);
        theDisplayLevelGroup.add(thePKAndFKItem);

        theDisplayLevelMenu.add(displayAllMenuItem);
        theDisplayLevelMenu.add(thePKOnlyItem);
        theDisplayLevelMenu.add(thePKAndFKItem);

        UIInitializer.getInstance().initialize(theDisplayLevelMenu);

        DefaultMenu theDisplayOrderMenu = new DefaultMenu(this, ERDesignerBundle.DISPLAYORDER);
        theViewMenu.add(theDisplayOrderMenu);

        displayNaturalOrderAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetDisplayOrder(DisplayOrder.NATURAL);
            }

        }, this, ERDesignerBundle.DISPLAYNATURALORDER);

        displayAscendingOrderAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetDisplayOrder(DisplayOrder.ASCENDING);
            }

        }, this, ERDesignerBundle.DISPLAYASCENDING);

        displayDescendingOrderAction = new DefaultAction(new ActionEventProcessor() {

            public void processActionEvent(ActionEvent e) {
                commandSetDisplayOrder(DisplayOrder.DESCENDING);
            }

        }, this, ERDesignerBundle.DISPLAYDESCENDING);

        displayNaturalOrderMenuItem = new DefaultRadioButtonMenuItem(displayNaturalOrderAction);
        DefaultRadioButtonMenuItem theAscendingItem = new DefaultRadioButtonMenuItem(displayAscendingOrderAction);
        DefaultRadioButtonMenuItem theDescendingItem = new DefaultRadioButtonMenuItem(displayDescendingOrderAction);

        ButtonGroup theDisplayOrderGroup = new ButtonGroup();
        theDisplayOrderGroup.add(displayNaturalOrderMenuItem);
        theDisplayOrderGroup.add(theAscendingItem);
        theDisplayOrderGroup.add(theDescendingItem);

        theDisplayOrderMenu.add(displayNaturalOrderMenuItem);
        theDisplayOrderMenu.add(theAscendingItem);
        theDisplayOrderMenu.add(theDescendingItem);

        UIInitializer.getInstance().initialize(theDisplayOrderMenu);

        theViewMenu.addSeparator();

        theViewMenu.add(new DefaultMenuItem(zoomInAction));
        theViewMenu.add(new DefaultMenuItem(zoomOutAction));

        DefaultComboBoxModel theZoomModel = new DefaultComboBoxModel();
        theZoomModel.addElement(ZOOMSCALE_HUNDREDPERCENT);
        for (int i = 9; i > 0; i--) {
            theZoomModel.addElement(new ZoomInfo(i * 10 + " %", ((double) i) / (double) 10));
        }
        zoomBox.setPreferredSize(new Dimension(100, 21));
        zoomBox.setMaximumSize(new Dimension(100, 21));
        zoomBox.setAction(zoomAction);
        zoomBox.setModel(theZoomModel);

        DefaultToolbar theToolBar = worldConnector.getToolBar();

        theToolBar.add(theFileMenu);
        theToolBar.add(theDBMenu);
        theToolBar.add(theViewMenu);
        theToolBar.addSeparator();

        theToolBar.add(newAction);
        theToolBar.addSeparator();
        theToolBar.add(loadAction);
        theToolBar.add(saveAction);
        theToolBar.addSeparator();
        theToolBar.add(zoomBox);
        theToolBar.addSeparator();
        theToolBar.add(zoomInAction);
        theToolBar.add(zoomOutAction);
        theToolBar.addSeparator();

        handButton = new DefaultToggleButton(handAction);
        relationButton = new DefaultToggleButton(relationAction);
        entityButton = new DefaultToggleButton(entityAction);
        commentButton = new DefaultToggleButton(commentAction);

        ButtonGroup theGroup = new ButtonGroup();
        theGroup.add(handButton);
        theGroup.add(relationButton);
        theGroup.add(entityButton);
        theGroup.add(commentButton);

        theToolBar.add(handButton);
        theToolBar.add(entityButton);
        theToolBar.add(relationButton);
        theToolBar.add(commentButton);

        worldConnector.initTitle();

        updateRecentlyUsedMenuEntries();

        setupViewForNothing();

        UIInitializer.getInstance().initialize(scrollPane);
    }

    protected void commandAddTable(Point2D aPoint) {

        // if (model.getDialect() == null) {
        // MessagesHelper.displayErrorMessage(graph,
        // getResourceHelper().getText(
        // ERDesignerBundle.PLEASEDEFINEADATABASECONNECTIONFIRST));
        // return;
        // }

        Table theTable = new Table();
        TableEditor theEditor = new TableEditor(model, scrollPane);
        theEditor.initializeFor(theTable);
        if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {
            try {

                try {
                    theEditor.applyValues();
                } catch (VetoException e) {
                    worldConnector.notifyAboutException(e);
                }

                TableCell theCell = new TableCell(theTable);
                theCell.transferPropertiesToAttributes(theTable);

                GraphConstants.setBounds(theCell.getAttributes(), new Rectangle2D.Double(aPoint.getX(), aPoint.getY(),
                        -1, -1));

                layoutCache.insert(theCell);

                theCell.transferAttributesToProperties(theCell.getAttributes());

            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            }

            graph.doLayout();
        }
    }

    protected void commandClasspath() {
        ClasspathEditor theEditor = new ClasspathEditor(scrollPane, preferences);
        if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {
            try {
                theEditor.applyValues();
            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            }
        }

    }

    protected void commandPreferences() {
        PreferencesEditor theEditor = new PreferencesEditor(graph, preferences, this);
        if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {
            try {
                theEditor.applyValues();
            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            }
        }

    }

    protected void updateRecentlyUsedMenuEntries() {

        lruMenu.removeAll();
        storedConnections.removeAll();

        if (preferences != null) {

            List<File> theFiles = preferences.getRecentlyUsedFiles();
            for (final File theFile : theFiles) {
                JMenuItem theItem = new JMenuItem(theFile.toString());
                theItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        commandOpenFromFile(theFile);
                    }

                });

                UIInitializer.getInstance().initializeFontAndColors(theItem);

                lruMenu.add(theItem);
            }

            for (final ConnectionDescriptor theConnectionInfo : preferences.getRecentlyUsedConnections()) {
                JMenuItem theItem1 = new JMenuItem(theConnectionInfo.toString());
                theItem1.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        commandDBConnection(theConnectionInfo);
                    }

                });

                UIInitializer.getInstance().initializeFontAndColors(theItem1);
                storedConnections.add(theItem1);
            }
        }
    }

    protected void addCurrentConnectionToConnectionHistory() {

        ConnectionDescriptor theConnection = model.createConnectionHistoryEntry();
        addConnectionToConnectionHistory(theConnection);
    }

    protected void addConnectionToConnectionHistory(ConnectionDescriptor aConnection) {

        preferences.addRecentlyUsedConnection(aConnection);

        updateRecentlyUsedMenuEntries();
    }

    /**
     * Edit the database connection.
     */
    protected void commandDBConnection() {
        commandDBConnection(model.createConnectionHistoryEntry());
    }

    protected void commandDBConnection(ConnectionDescriptor aConnection) {
        DatabaseConnectionEditor theEditor = new DatabaseConnectionEditor(scrollPane, model, preferences, aConnection);
        if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {
            try {
                theEditor.applyValues();
                addCurrentConnectionToConnectionHistory();
            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            }
        }
    }

    /**
     * Edit the repository connection.
     */
    protected void commandRepositoryConnection() {
        RepositoryConnectionEditor theEditor = new RepositoryConnectionEditor(scrollPane, preferences);
        if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {
            try {
                theEditor.applyValues();
            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            }
        }
    }

    /**
     * Edit the domains.
     */
    protected void commandEditDomains() {
        DomainEditor theEditor = new DomainEditor(model, scrollPane);
        if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {
            try {
                theEditor.applyValues();
            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            }
        }
    }

    protected void commandExport(Exporter aExporter, ExportType aExportType) {

        if (aExportType.equals(ExportType.ONE_PER_FILE)) {

            JFileChooser theChooser = new JFileChooser();
            theChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (theChooser.showSaveDialog(scrollPane) == JFileChooser.APPROVE_OPTION) {
                File theBaseDirectory = theChooser.getSelectedFile();

                CellView[] theViews = layoutCache.getAllViews();
                for (CellView theView : theViews) {
                    if (theView instanceof TableCellView) {
                        VertexView theItemCellView = (VertexView) theView;
                        DefaultGraphCell theItemCell = (DefaultGraphCell) theItemCellView.getCell();
                        ModelItem theItem = (ModelItem) theItemCell.getUserObject();

                        File theOutputFile = new File(theBaseDirectory, theItem.getName()
                                + aExporter.getFileExtension());
                        try {
                            aExporter.exportToStream(theItemCellView.getRendererComponent(graph, false, false, false),
                                    new FileOutputStream(theOutputFile));
                        } catch (Exception e) {
                            worldConnector.notifyAboutException(e);
                        }
                    }
                }
            }

        } else {

            JFileChooser theChooser = new JFileChooser();
            GenericFileFilter theFilter = new GenericFileFilter(aExporter.getFileExtension(), aExporter
                    .getFileExtension()
                    + " File");
            theChooser.setFileFilter(theFilter);
            if (theChooser.showSaveDialog(scrollPane) == JFileChooser.APPROVE_OPTION) {

                File theFile = theFilter.getCompletedFile(theChooser.getSelectedFile());
                try {
                    aExporter.fullExportToStream(graph, new FileOutputStream(theFile));
                } catch (Exception e) {
                    worldConnector.notifyAboutException(e);
                }
            }

        }
    }

    protected void commandLayout(Layouter aLayouter) {

        if (model.getSubjectAreas().size() > 0) {
            MessagesHelper.displayErrorMessage(graph, getResourceHelper().getText(
                    ERDesignerBundle.MODELSWITHSUBJECTAREASARENOTSUPPORTED));
            return;
        }

        if (aLayouter instanceof SizeableLayouter) {
            SizeableLayouter theLayouter = (SizeableLayouter) aLayouter;

            String theSize = MessagesHelper.askForInput(scrollPane, ERDesignerBundle.INPUTLAYOUTSIZE, "1000,1000");
            if (theSize == null) {
                return;
            }

            try {

                int p = theSize.indexOf(",");
                int theWidth = Integer.parseInt(theSize.substring(0, p));
                int theHeight = Integer.parseInt(theSize.substring(p + 1));

                theLayouter.setSize(new Dimension(theWidth, theHeight));
            } catch (Exception e) {
                MessagesHelper.displayErrorMessage(scrollPane, getResourceHelper().getText(
                        ERDesignerBundle.INVALIDSIZESPECIFIED));
                return;
            }
        }

        try {
            aLayouter.applyLayout(preferences, graph, graph.getRoots());
            worldConnector.setStatusText(getResourceHelper().getText(ERDesignerBundle.LAYOUTFINISHED));
        } catch (Exception e) {
            worldConnector.notifyAboutException(e);
        }
    }

    protected void commandNew() {

        Model theModel = worldConnector.createNewModel();
        setModel(theModel);

        setupViewForNothing();

        worldConnector.setStatusText(getResourceHelper().getText(ERDesignerBundle.NEWMODELCREATED));
    }

    protected void commandOpenFromFile() {

        ModelFileFilter theFiler = new ModelFileFilter();

        JFileChooser theChooser = new JFileChooser();
        theChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        theChooser.setFileFilter(theFiler);
        if (theChooser.showOpenDialog(scrollPane) == JFileChooser.APPROVE_OPTION) {

            File theFile = theFiler.getCompletedFile(theChooser.getSelectedFile());

            commandOpenFromFile(theFile);
        }
    }

    protected void commandOpenFromFile(File aFile) {

        FileInputStream theStream = null;

        try {
            theStream = new FileInputStream(aFile);

            Model theModel = ModelIOUtilities.getInstance().deserializeModelFromXML(theStream);
            worldConnector.initializeLoadedModel(theModel);

            setModel(theModel);

            preferences.addRecentlyUsedFile(aFile);

            addCurrentConnectionToConnectionHistory();

            setupViewFor(aFile);
            worldConnector.setStatusText(getResourceHelper().getText(ERDesignerBundle.FILELOADED));

        } catch (Exception e) {

            MessagesHelper.displayErrorMessage(graph, getResourceHelper().getText(ERDesignerBundle.ERRORLOADINGFILE));

            worldConnector.notifyAboutException(e);
        } finally {
            if (theStream != null) {
                try {
                    theStream.close();
                } catch (IOException e) {
                    // Ignore this exception
                }
            }
        }
    }

    /**
     * Reverse engineer a model from a database connection.
     */
    public void commandReverseEngineer() {

        if (model.getDialect() == null) {
            MessagesHelper.displayErrorMessage(graph, getResourceHelper().getText(
                    ERDesignerBundle.PLEASEDEFINEADATABASECONNECTIONFIRST));
            return;
        }

        ReverseEngineerEditor theEditor = new ReverseEngineerEditor(model, scrollPane, preferences);
        if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {

            try {

                Connection theConnection = model.createConnection(preferences);
                ReverseEngineeringStrategy theStrategy = model.getDialect().getReverseEngineeringStrategy();

                ReverseEngineeringOptions theOptions = theEditor.createREOptions();
                theOptions.getTableEntries().addAll(
                        theStrategy.getTablesForSchemas(theConnection, theOptions.getSchemaEntries()));

                TablesSelectEditor theTablesEditor = new TablesSelectEditor(theOptions, scrollPane);
                if (theTablesEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {

                    // Try to detect the table names that should be reverse
                    // engineered
                    ReverseEngineerSwingWorker theWorker = new ReverseEngineerSwingWorker(model, theOptions,
                            theStrategy, theConnection);
                    theWorker.execute();

                    ReverseEngineeringResult theResult = theWorker.get();
                    Model theModel = theResult.getModel();
                    if (theModel != null) {
                        setModel(theModel);
                    } else {
                        worldConnector.notifyAboutException(theResult.getException());
                    }

                    if (!model.getDialect().generatesManagedConnection()) {
                        theConnection.close();
                    }
                }

            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            }

        }
    }

    /**
     * Save the current model to file.
     */
    protected void commandSaveFile() {

        DateFormat theFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date theNow = new Date();

        ModelFileFilter theFiler = new ModelFileFilter();

        JFileChooser theChooser = new JFileChooser();
        theChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        theChooser.setFileFilter(theFiler);
        theChooser.setSelectedFile(currentEditingFile);
        if (theChooser.showSaveDialog(scrollPane) == JFileChooser.APPROVE_OPTION) {

            File theFile = theFiler.getCompletedFile(theChooser.getSelectedFile());
            FileOutputStream theStream = null;
            PrintWriter theWriter = null;
            try {

                if (theFile.exists()) {
                    File theBakFile = new File(theFile.toString() + "_" + theFormat.format(theNow));
                    theFile.renameTo(theBakFile);
                }

                theStream = new FileOutputStream(theFile);

                ModelIOUtilities.getInstance().serializeModelToXML(model, theStream);

                worldConnector.initTitle();

                preferences.addRecentlyUsedFile(theFile);

                updateRecentlyUsedMenuEntries();

                if (model.getModificationTracker() instanceof HistoryModificationTracker) {
                    HistoryModificationTracker theTracker = (HistoryModificationTracker) model.getModificationTracker();
                    StatementList theStatements = theTracker.getNotSavedStatements();
                    if (theStatements.size() > 0) {
                        StringBuilder theFileName = new StringBuilder(theFile.toString());
                        int p = theFileName.lastIndexOf(".");
                        if (p > 0) {

                            SQLGenerator theGenerator = model.getDialect().createSQLGenerator();

                            theFileName = new StringBuilder(theFileName.substring(0, p));

                            theFileName.insert(p, "_" + theFormat.format(theNow));
                            theFileName.append(".sql");

                            theWriter = new PrintWriter(new File(theFileName.toString()));
                            for (Statement theStatement : theStatements) {
                                theWriter.print(theStatement.getSql());
                                theWriter.println(theGenerator.createScriptStatementSeparator());
                                theStatement.setSaved(true);

                            }
                        }
                    }
                }

                setupViewFor(theFile);
                worldConnector.setStatusText(getResourceHelper().getText(ERDesignerBundle.FILESAVED));

            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            } finally {
                if (theStream != null) {
                    try {
                        theStream.close();
                    } catch (IOException e) {
                        // Ignore this exception
                    }
                }
                if (theWriter != null) {
                    theWriter.close();
                }
            }

        }
    }

    /**
     * Save the current model to a repository.
     */
    protected void commandSaveToRepository() {

        ConnectionDescriptor theRepositoryConnection = preferences.getRepositoryConnection();
        if (theRepositoryConnection == null) {
            MessagesHelper.displayErrorMessage(scrollPane, getResourceHelper().getText(
                    ERDesignerBundle.ERRORINREPOSITORYCONNECTION));
            return;
        }
        Connection theConnection = null;
        Dialect theDialect = DialectFactory.getInstance().getDialect(theRepositoryConnection.getDialect());
        try {

            theConnection = theDialect.createConnection(preferences.createDriverClassLoader(), theRepositoryConnection
                    .getDriver(), theRepositoryConnection.getUrl(), theRepositoryConnection.getUsername(),
                    theRepositoryConnection.getPassword());

            List<RepositoryEntryDesciptor> theEntries = ModelIOUtilities.getInstance().getRepositoryEntries(theDialect,
                    theConnection);

            SaveToRepositoryEditor theEditor = new SaveToRepositoryEditor(scrollPane, theEntries,
                    currentRepositoryEntry);
            if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {
                try {

                    RepositoryEntryDesciptor theDesc = theEditor.getRepositoryDescriptor();

                    theDesc = ModelIOUtilities.getInstance().serializeModelToDB(theDesc, model, preferences);

                    setupViewFor(theDesc);
                    worldConnector.setStatusText(getResourceHelper().getText(ERDesignerBundle.FILESAVED));

                } catch (Exception e) {
                    worldConnector.notifyAboutException(e);
                }
            }
        } catch (Exception e) {
            worldConnector.notifyAboutException(e);
        } finally {
            if (theConnection != null && !theDialect.generatesManagedConnection()) {
                try {
                    theConnection.close();
                } catch (SQLException e) {
                    // Do nothing here
                }
            }
        }
    }

    /**
     * Create a migration script from repository.
     */
    protected void commandCreateMigrationScript() {

        ConnectionDescriptor theRepositoryConnection = preferences.getRepositoryConnection();
        if (theRepositoryConnection == null) {
            MessagesHelper.displayErrorMessage(scrollPane, getResourceHelper().getText(
                    ERDesignerBundle.ERRORINREPOSITORYCONNECTION));
            return;
        }
        Connection theConnection = null;
        Dialect theDialect = DialectFactory.getInstance().getDialect(theRepositoryConnection.getDialect());
        try {

            theConnection = theDialect.createConnection(preferences.createDriverClassLoader(), theRepositoryConnection
                    .getDriver(), theRepositoryConnection.getUrl(), theRepositoryConnection.getUsername(),
                    theRepositoryConnection.getPassword());

            RepositoryEntity theEntity = DictionaryModelSerializer.SERIALIZER.getRepositoryEntity(theDialect
                    .getHibernateDialectClass(), theConnection, currentRepositoryEntry);

            MigrationScriptEditor theEditor = new MigrationScriptEditor(scrollPane, theEntity,
                    new GenericConnectionProvider(theConnection, theDialect.createSQLGenerator()
                            .createScriptStatementSeparator()));

            theEditor.showModal();

        } catch (Exception e) {
            worldConnector.notifyAboutException(e);
        } finally {
            if (theConnection != null && !theDialect.generatesManagedConnection()) {
                try {
                    theConnection.close();
                } catch (SQLException e) {
                    // Do nothing here
                }
            }
        }
    }

    /**
     * Open the database model from an existing connection.
     */
    protected void commandOpenFromRepository() {

        ConnectionDescriptor theRepositoryConnection = preferences.getRepositoryConnection();
        if (theRepositoryConnection == null) {
            MessagesHelper.displayErrorMessage(scrollPane, getResourceHelper().getText(
                    ERDesignerBundle.ERRORINREPOSITORYCONNECTION));
            return;
        }
        Connection theConnection = null;
        Dialect theDialect = DialectFactory.getInstance().getDialect(theRepositoryConnection.getDialect());
        try {

            theConnection = theDialect.createConnection(preferences.createDriverClassLoader(), theRepositoryConnection
                    .getDriver(), theRepositoryConnection.getUrl(), theRepositoryConnection.getUsername(),
                    theRepositoryConnection.getPassword());

            List<RepositoryEntryDesciptor> theEntries = ModelIOUtilities.getInstance().getRepositoryEntries(theDialect,
                    theConnection);

            LoadFromRepositoryEditor theEditor = new LoadFromRepositoryEditor(scrollPane, preferences, theConnection,
                    theEntries);
            if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {

                RepositoryEntryDesciptor theDescriptor = theEditor.getModel().getEntry();

                Model theModel = ModelIOUtilities.getInstance().deserializeModelfromRepository(theDescriptor,
                        theDialect, theConnection, preferences);
                worldConnector.initializeLoadedModel(theModel);

                setupViewFor(theDescriptor);
                worldConnector.setStatusText(getResourceHelper().getText(ERDesignerBundle.FILELOADED));

                currentRepositoryEntry = theDescriptor;
                currentEditingFile = null;

                setModel(theModel);
            }

        } catch (Exception e) {
            worldConnector.notifyAboutException(e);
        } finally {
            if (theConnection != null && !theDialect.generatesManagedConnection()) {
                try {
                    theConnection.close();
                } catch (SQLException e) {
                    // Do nothing here
                }
            }
        }
    }

    /**
     * Setup the view for a model loaded from repository.
     * 
     * @param aDescriptor
     *                the entry descriptor
     */
    protected void setupViewFor(RepositoryEntryDesciptor aDescriptor) {

        currentEditingFile = null;
        currentRepositoryEntry = aDescriptor;
        worldConnector.initTitle(aDescriptor.getName());
        repositoryUtilsMenu.setEnabled(true);
    }

    /**
     * Setup the view for a model loaded from file.
     * 
     * @param aFile
     *                the file
     */
    protected void setupViewFor(File aFile) {

        currentEditingFile = aFile;
        currentRepositoryEntry = null;
        worldConnector.initTitle(aFile.toString());
        repositoryUtilsMenu.setEnabled(false);
    }

    /**
     * Setup the view for an empty model.
     */
    protected void setupViewForNothing() {

        currentEditingFile = null;
        currentRepositoryEntry = null;
        repositoryUtilsMenu.setEnabled(false);
        worldConnector.initTitle();
    }

    /**
     * Set the current editing tool.
     * 
     * @param aTool
     *                the tool
     */
    protected void commandSetTool(ToolEnum aTool) {
        if (aTool.equals(ToolEnum.HAND)) {

            if (!handButton.isSelected()) {
                handButton.setSelected(true);
            }

            graph.setTool(new HandTool(graph));
        }
        if (aTool.equals(ToolEnum.ENTITY)) {

            if (!entityButton.isSelected()) {
                entityButton.setSelected(true);
            }

            graph.setTool(new EntityTool(graph));
        }
        if (aTool.equals(ToolEnum.RELATION)) {

            if (!relationButton.isSelected()) {
                relationButton.setSelected(true);
            }

            graph.setTool(new RelationTool(graph));
        }
        if (aTool.equals(ToolEnum.COMMENT)) {

            if (!commentButton.isSelected()) {
                commentButton.setSelected(true);
            }

            graph.setTool(new CommentTool(graph));
        }
    }

    protected void commandSetZoom(ZoomInfo aZoomInfo) {
        graph.setScale(aZoomInfo.getValue());
        zoomBox.setSelectedItem(aZoomInfo);
    }

    protected void commandZoomIn() {
        int theIndex = zoomBox.getSelectedIndex();
        if (theIndex > 0) {
            theIndex--;
            zoomBox.setSelectedIndex(theIndex);
            commandSetZoom((ZoomInfo) zoomBox.getSelectedItem());
        }
    }

    protected void commandZoomOut() {
        int theIndex = zoomBox.getSelectedIndex();
        if (theIndex < zoomBox.getItemCount() - 1) {
            theIndex++;
            zoomBox.setSelectedIndex(theIndex);
            commandSetZoom((ZoomInfo) zoomBox.getSelectedItem());
        }
    }

    protected void commandGenerateSQL() {

        if (model.getDialect() == null) {
            MessagesHelper.displayErrorMessage(graph, getResourceHelper().getText(
                    ERDesignerBundle.PLEASEDEFINEADATABASECONNECTIONFIRST));
            return;
        }

        try {
            SQLGenerator theGenerator = model.getDialect().createSQLGenerator();
            StatementList theStatements = theGenerator.createCreateAllObjects(model);
            SQLEditor theEditor = new SQLEditor(scrollPane, new ModelBasedConnectionProvider(model), theStatements,
                    currentEditingFile, "schema.sql");
            theEditor.showModal();
        } catch (VetoException e) {
            worldConnector.notifyAboutException(e);
        }
    }

    protected String generateChangelogSQLFileName() {
        return "changelog.sql";
    }

    protected void commandGenerateChangelogSQL() {

        if (model.getDialect() == null) {
            MessagesHelper.displayErrorMessage(graph, getResourceHelper().getText(
                    ERDesignerBundle.PLEASEDEFINEADATABASECONNECTIONFIRST));
            return;
        }

        StatementList theStatements = ((HistoryModificationTracker) model.getModificationTracker()).getStatements();
        SQLEditor theEditor = new SQLEditor(scrollPane, new ModelBasedConnectionProvider(model), theStatements,
                currentEditingFile, generateChangelogSQLFileName());
        theEditor.showModal();
    }

    public ResourceHelper getResourceHelper() {
        return ResourceHelper.getResourceHelper(ERDesignerBundle.BUNDLE_NAME);
    }

    /**
     * Set the current editing model.
     * 
     * @param aModel
     *                the model
     */
    public void setModel(Model aModel) {
        model = aModel;

        graphModel = new DefaultGraphModel();
        layoutCache = new GraphLayoutCache(graphModel, new CellViewFactory());
        layoutCache.setAutoSizeOnValueChange(true);

        graphModel.addGraphModelListener(new ERDesignerGraphModelListener());

        graph = new ERDesignerGraph(model, graphModel, layoutCache) {

            @Override
            public void commandNewTable(Point2D aLocation) {
                ERDesignerComponent.this.commandAddTable(aLocation);
            }

            @Override
            public void commandNewComment(Point2D aLocation) {
                ERDesignerComponent.this.commandAddComment(aLocation);
            }
        };
        graph.setUI(new ERDesignerGraphUI(this));

        displayAllMenuItem.setSelected(true);
        displayNaturalOrderMenuItem.setSelected(true);
        displayCommentsMenuItem.setSelected(true);

        commandSetDisplayGridState(displayGridMenuItem.isSelected());
        commandSetDisplayCommentsState(true);
        commandSetDisplayLevel(DisplayLevel.ALL);
        commandSetDisplayOrder(DisplayOrder.NATURAL);

        refreshPreferences(preferences);

        scrollPane.getViewport().removeAll();
        scrollPane.getViewport().add(graph);

        Map<Table, TableCell> theModelTableCells = new HashMap<Table, TableCell>();
        Map<Comment, CommentCell> theModelCommentCells = new HashMap<Comment, CommentCell>();

        for (Table theTable : model.getTables()) {
            TableCell theCell = new TableCell(theTable);
            theCell.transferPropertiesToAttributes(theTable);

            layoutCache.insert(theCell);

            theModelTableCells.put(theTable, theCell);
        }

        for (Comment theComment : model.getComments()) {
            CommentCell theCell = new CommentCell(theComment);
            theCell.transferPropertiesToAttributes(theComment);

            layoutCache.insert(theCell);

            theModelCommentCells.put(theComment, theCell);
        }

        for (Relation theRelation : model.getRelations()) {

            TableCell theImportingCell = theModelTableCells.get(theRelation.getImportingTable());
            TableCell theExportingCell = theModelTableCells.get(theRelation.getExportingTable());

            RelationEdge theCell = new RelationEdge(theRelation, theImportingCell, theExportingCell);
            theCell.transferPropertiesToAttributes(theRelation);

            layoutCache.insert(theCell);
        }

        for (SubjectArea theSubjectArea : model.getSubjectAreas()) {

            SubjectAreaCell theSubjectAreaCell = new SubjectAreaCell(theSubjectArea);
            List<ModelCell> theTableCells = new ArrayList<ModelCell>();

            for (Table theTable : theSubjectArea.getTables()) {
                theTableCells.add(theModelTableCells.get(theTable));
            }

            for (Comment theComment : theSubjectArea.getComments()) {
                theTableCells.add(theModelCommentCells.get(theComment));
            }

            layoutCache.insertGroup(theSubjectAreaCell, theTableCells.toArray());
            layoutCache.toBack(new Object[] { theSubjectAreaCell });

        }

        commandSetZoom(ZOOMSCALE_HUNDREDPERCENT);
        commandSetTool(ToolEnum.HAND);
    }

    /**
     * Add a new comment to the model.
     * 
     * @param aLocation
     *                the location
     */
    protected void commandAddComment(Point2D aLocation) {
        Comment theComment = new Comment();
        CommentEditor theEditor = new CommentEditor(model, scrollPane);
        theEditor.initializeFor(theComment);
        if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {
            try {

                try {
                    theEditor.applyValues();
                } catch (VetoException e) {
                    worldConnector.notifyAboutException(e);
                }

                CommentCell theCell = new CommentCell(theComment);
                theCell.transferPropertiesToAttributes(theComment);

                GraphConstants.setBounds(theCell.getAttributes(), new Rectangle2D.Double(aLocation.getX(), aLocation
                        .getY(), -1, -1));

                layoutCache.insert(theCell);

                theCell.transferAttributesToProperties(theCell.getAttributes());

            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            }

            graph.doLayout();
        }
    }

    protected void addExportEntries(DefaultMenu aMenu, final Exporter aExporter) {

        DefaultAction theAllInOneAction = new DefaultAction(ERDesignerBundle.BUNDLE_NAME, ERDesignerBundle.ALLINONEFILE);
        DefaultMenuItem theAllInOneItem = new DefaultMenuItem(theAllInOneAction);
        theAllInOneAction.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                commandExport(aExporter, ExportType.ALL_IN_ONE);
            }
        });
        aMenu.add(theAllInOneItem);

        DefaultAction theOnePerTableAction = new DefaultAction(ERDesignerBundle.BUNDLE_NAME,
                ERDesignerBundle.ONEFILEPERTABLE);
        DefaultMenuItem theOnePerTable = new DefaultMenuItem(theOnePerTableAction);
        theOnePerTableAction.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                commandExport(aExporter, ExportType.ONE_PER_FILE);
            }
        });
        aMenu.add(theOnePerTable);
    }

    public JComponent getDetailComponent() {
        return scrollPane;
    }

    public File getCurrentFile() {
        return currentEditingFile;
    }

    /**
     * Save the preferences.
     */
    public void savePreferences() {
        try {
            preferences.store();
        } catch (Exception e) {
            worldConnector.notifyAboutException(e);
        }
    }

    public ERDesignerWorldConnector getWorldConnector() {
        return worldConnector;
    }

    protected void commandRemoveSubjectArea(SubjectAreaCell aCell) {
        graph.getGraphLayoutCache().remove(new Object[] { aCell });
        model.removeSubjectArea((SubjectArea) aCell.getUserObject());
    }

    protected void commandUpdateSubjectArea(SubjectAreaCell aCell) {

        SubjectArea theArea = (SubjectArea) aCell.getUserObject();
        theArea.getTables().clear();
        theArea.getComments().clear();
        for (Object theObject : aCell.getChildren()) {
            if (theObject instanceof TableCell) {
                theArea.getTables().add((Table) ((TableCell) theObject).getUserObject());
            }
            if (theObject instanceof CommentCell) {
                theArea.getComments().add((Comment) ((CommentCell) theObject).getUserObject());
            }
        }
    }

    protected void commandCompleteCompare() {
        if (model.getDialect() == null) {
            MessagesHelper.displayErrorMessage(graph, getResourceHelper().getText(
                    ERDesignerBundle.PLEASEDEFINEADATABASECONNECTIONFIRST));
            return;
        }

        ReverseEngineerEditor theEditor = new ReverseEngineerEditor(model, scrollPane, preferences);
        if (theEditor.showModal() == DialogConstants.MODAL_RESULT_OK) {

            try {
                Connection theConnection = model.createConnection(preferences);
                ReverseEngineeringStrategy theStrategy = model.getDialect().getReverseEngineeringStrategy();
                ReverseEngineeringOptions theOptions = theEditor.createREOptions();

                // Es werden alle Tabellen beim Vergleich ber�cksichtigt!
                theOptions.getTableEntries().addAll(
                        theStrategy.getTablesForSchemas(theConnection, theOptions.getSchemaEntries()));

                Model theDatabaseModel = worldConnector.createNewModel();
                theDatabaseModel.setDialect(model.getDialect());
                theDatabaseModel.getProperties().copyFrom(model);

                ReverseEngineerSwingWorker theWorker = new ReverseEngineerSwingWorker(theDatabaseModel, theOptions,
                        theStrategy, theConnection);
                theWorker.execute();

                ReverseEngineeringResult theResult = theWorker.get();
                theDatabaseModel = theResult.getModel();
                if (theDatabaseModel != null) {

                    addConnectionToConnectionHistory(theDatabaseModel.createConnectionHistoryEntry());

                    CompleteCompareEditor theCompare = new CompleteCompareEditor(scrollPane, model, theDatabaseModel);
                    theCompare.showModal();

                } else {
                    worldConnector.notifyAboutException(theResult.getException());
                }

                if (!model.getDialect().generatesManagedConnection()) {
                    theConnection.close();
                }

            } catch (Exception e) {
                worldConnector.notifyAboutException(e);
            }
        }
    }

    /**
     * Toggle the include comments view state.
     * 
     * @param aState
     *                true if comments shall be displayed, else false
     */
    protected void commandSetDisplayCommentsState(boolean aState) {
        graph.setDisplayComments(aState);
        repaintGraph();
    }

    /**
     * Toggle the include comments view state.
     * 
     * @param aState
     *                true if comments shall be displayed, else false
     */
    protected void commandSetDisplayGridState(boolean aState) {
        graph.setGridEnabled(aState);
        graph.setGridVisible(aState);
        repaintGraph();
    }

    /**
     * The preferences where changed, so they need to be reloaded.
     * 
     * @param aPreferences
     *                the preferences
     */
    public void refreshPreferences(ApplicationPreferences aPreferences) {
        graph.setGridSize(aPreferences.getGridSize());
        repaintGraph();
    }

    /**
     * Set the current display level.
     * 
     * @param aLevel
     *                the level
     */
    protected void commandSetDisplayLevel(DisplayLevel aLevel) {
        graph.setDisplayLevel(aLevel);
        repaintGraph();
    }

    /**
     * Set the current display order.
     * 
     * @param aOrder
     *                the display order
     */
    protected void commandSetDisplayOrder(DisplayOrder aOrder) {
        graph.setDisplayOrder(aOrder);
        repaintGraph();
    }

    /**
     * Repaint the current graph.
     */
    protected void repaintGraph() {
        for (CellView theView : layoutCache.getCellViews()) {
            graph.updateAutoSize(theView);
        }
        graph.getGraphLayoutCache().reload();
        graph.invalidate();
        graph.repaint();
    }
}