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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package de.erdesignerng.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.erdesignerng.ERDesignerBundle;
import de.erdesignerng.exception.ElementAlreadyExistsException;
import de.erdesignerng.exception.ReverseEngineeringException;
import de.erdesignerng.model.Attribute;
import de.erdesignerng.model.CascadeType;
import de.erdesignerng.model.Domain;
import de.erdesignerng.model.Index;
import de.erdesignerng.model.IndexExpression;
import de.erdesignerng.model.IndexType;
import de.erdesignerng.model.Model;
import de.erdesignerng.model.Relation;
import de.erdesignerng.model.Table;
import de.erdesignerng.model.View;
import de.erdesignerng.modificationtracker.VetoException;
import de.erdesignerng.util.SQLUtils;
import de.erdesignerng.visual.common.ERDesignerWorldConnector;

/**
 * @author $Author: mirkosertic $
 * @version $Date: 2009-03-13 15:40:33 $
 * @param <T>
 *            the dialect
 */
public abstract class JDBCReverseEngineeringStrategy<T extends Dialect> {

    private static final Logger LOGGER = Logger.getLogger(JDBCReverseEngineeringStrategy.class);

    public static final String TABLE_TABLE_TYPE = "TABLE";

    public static final String VIEW_TABLE_TYPE = "VIEW";

    protected T dialect;

    protected JDBCReverseEngineeringStrategy(T aDialect) {
        dialect = aDialect;
    }

    /**
     * Convert a JDBC Cascade Type to the Mogwai CascadeType.
     * 
     * Default is CASCADE.
     * 
     * @param aValue
     *            the JDBC type
     * @return the CascadeType
     */
    protected CascadeType getCascadeType(int aValue) {
        switch (aValue) {
        case DatabaseMetaData.importedKeyNoAction:
            return CascadeType.NOTHING;
        case DatabaseMetaData.importedKeySetNull:
            return CascadeType.SET_NULL;
        case DatabaseMetaData.importedKeyCascade:
            return CascadeType.CASCADE;
        case DatabaseMetaData.importedKeyRestrict:
            return CascadeType.RESTRICT;
        default:
            return CascadeType.CASCADE;
        }
    }

    protected String convertColumnTypeToRealType(String aTypeName) {
        return aTypeName;
    }

    protected void reverseEngineerAttribute(Model aModel, Attribute aAttribute, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier, TableEntry aTable, Connection aConnection) throws SQLException {
    }

    protected void reverseEngineerDomain(Model aModel, Domain aDomain, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier, Connection aConnection) throws SQLException {
    }

    /**
     * Reverse engineerer the sql statement for a view.
     * 
     * @param aViewEntry
     *            die view entry
     * @param aConnection
     *            the connection
     * @param aView
     *            the view
     * @return the sql statement
     * @throws SQLException
     *             is thrown in case of an exception
     * @throws ReverseEngineeringException
     *             is thrown in case of an exception
     */
    protected String reverseEngineerViewSQL(TableEntry aViewEntry, Connection aConnection, View aView)
            throws SQLException, ReverseEngineeringException {
        return null;
    }

    /**
     * Reverse enginner an existing view.
     * 
     * @param aModel
     *            the model
     * @param aOptions
     *            the options
     * @param aNotifier
     *            the notifier
     * @param aViewEntry
     *            the table
     * @param aConnection
     *            the connection
     * @throws SQLException
     *             is thrown in case of an error
     * @throws ReverseEngineeringException
     *             is thrown in case of an error
     */
    protected void reverseEngineerView(Model aModel, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier, TableEntry aViewEntry, Connection aConnection) throws SQLException,
            ReverseEngineeringException {

        aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGTABLE, aViewEntry.getTableName());

        DatabaseMetaData theMetaData = aConnection.getMetaData();

        String theTablePattern = getEscapedPattern(theMetaData, aViewEntry.getTableName());
        String theSchemaPattern = getEscapedPattern(theMetaData, aViewEntry.getSchemaName());

        ResultSet theViewsResultSet = theMetaData.getTables(aViewEntry.getCatalogName(), theSchemaPattern,
                theTablePattern, new String[] { aViewEntry.getTableType() });
        while (theViewsResultSet.next()) {

            String theViewRemarks = theViewsResultSet.getString("REMARKS");

            View theView = new View();

            theView.setName(dialect.getCastType().cast(aViewEntry.getTableName()));
            theView.setOriginalName(aViewEntry.getTableName());
            switch (aOptions.getTableNaming()) {
            case INCLUDE_SCHEMA:
                theView.setSchema(aViewEntry.getSchemaName());
                break;
            default:
            }

            if (!StringUtils.isEmpty(theViewRemarks)) {
                theView.setComment(theViewRemarks);
            }

            String theStatement = reverseEngineerViewSQL(aViewEntry, aConnection, theView);

            try {
                SQLUtils.updateViewAttributesFromSQL(theView, theStatement);
            } catch (Exception e) {
                LOGGER.warn("View " + theView.getName() + " has a strange SQL : " + theStatement);
            }

            theView.setSql(theStatement);

            // We are done here
            try {
                aModel.addView(theView);
            } catch (Exception e) {
                throw new ReverseEngineeringException(e.getMessage(), e);
            }

        }
        theViewsResultSet.close();
    }

    protected String getEscapedPattern(DatabaseMetaData aMetaData, String aValue) throws SQLException {
        String thePrefix = aMetaData.getSearchStringEscape();
        if (!StringUtils.isEmpty(thePrefix) && !StringUtils.isEmpty(aValue)) {
            aValue = aValue.replace("_", thePrefix + "_");
            aValue = aValue.replace("%", thePrefix + "_");
        }
        return aValue;
    }

    /**
     * Reverse enginner an existing table.
     * 
     * @param aModel
     *            the model
     * @param aOptions
     *            the options
     * @param aNotifier
     *            the notifier
     * @param aTableEntry
     *            the table
     * @param aConnection
     *            the connection
     * @throws SQLException
     *             is thrown in case of an error
     * @throws ReverseEngineeringException
     *             is thrown in case of an error
     */
    protected final void reverseEngineerTable(Model aModel, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier, TableEntry aTableEntry, Connection aConnection) throws SQLException,
            ReverseEngineeringException {

        aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGTABLE, aTableEntry.getTableName());

        DatabaseMetaData theMetaData = aConnection.getMetaData();

        String theTablePattern = getEscapedPattern(theMetaData, aTableEntry.getTableName());
        String theSchemaPattern = getEscapedPattern(theMetaData, aTableEntry.getSchemaName());

        ResultSet theTablesResultSet = theMetaData.getTables(aTableEntry.getCatalogName(), theSchemaPattern,
                theTablePattern, new String[] { aTableEntry.getTableType() });
        while (theTablesResultSet.next()) {

            String theTableRemarks = theTablesResultSet.getString("REMARKS");

            Table theNewTable = new Table();

            theNewTable.setName(dialect.getCastType().cast(aTableEntry.getTableName()));
            theNewTable.setOriginalName(aTableEntry.getTableName());
            switch (aOptions.getTableNaming()) {
            case INCLUDE_SCHEMA:
                theNewTable.setSchema(aTableEntry.getSchemaName());
                break;
            default:
            }

            if (!StringUtils.isEmpty(theTableRemarks)) {
                theNewTable.setComment(theTableRemarks);
            }

            // Reverse engineer attributes
            ResultSet theColumnsResultSet = theMetaData.getColumns(aTableEntry.getCatalogName(), theSchemaPattern,
                    theTablePattern, null);
            while (theColumnsResultSet.next()) {

                String theColumnName = null;
                String theTypeName = null;
                Integer theSize = null;
                int theFraction = 0;
                int theRadix = 0;
                int theNullable = 0;
                String theDefaultValue = null;
                String theColumnRemarks = null;

                try {
                    theColumnName = theColumnsResultSet.getString("COLUMN_NAME");
                } catch (Exception e) {
                }

                try {
                    theTypeName = theColumnsResultSet.getString("TYPE_NAME");
                } catch (Exception e) {
                }

                try {
                    theSize = theColumnsResultSet.getInt("COLUMN_SIZE");
                } catch (Exception e) {
                }

                try {
                    theFraction = theColumnsResultSet.getInt("DECIMAL_DIGITS");
                } catch (Exception e) {
                }

                try {
                    theRadix = theColumnsResultSet.getInt("NUM_PREC_RADIX");
                } catch (Exception e) {
                }

                try {
                    theNullable = theColumnsResultSet.getInt("NULLABLE");
                } catch (Exception e) {
                }

                try {
                    theDefaultValue = theColumnsResultSet.getString("COLUMN_DEF");
                    if (!StringUtils.isEmpty(theDefaultValue)) {
                        theDefaultValue = theDefaultValue.trim();
                    }
                } catch (Exception e) {
                }

                try {
                    theColumnRemarks = theColumnsResultSet.getString("REMARKS");
                } catch (Exception e) {
                }

                Attribute theAttribute = new Attribute();

                theAttribute.setName(dialect.getCastType().cast(theColumnName));
                if (!StringUtils.isEmpty(theColumnRemarks)) {
                    theAttribute.setComment(theColumnRemarks);
                }

                // Search for the datatype in the domains and the dialect
                // specific datatypes
                DataType theDataType = aModel.getAvailableDataTypes().findByName(
                        convertColumnTypeToRealType(theTypeName));
                if (theDataType == null) {
                    throw new ReverseEngineeringException("Unknown data type " + theTypeName + " for "
                            + aTableEntry.getTableName() + "." + theColumnName);
                }

                boolean isNullable = true;
                switch (theNullable) {
                case DatabaseMetaData.columnNoNulls:
                    isNullable = false;
                    break;
                case DatabaseMetaData.columnNullable:
                    isNullable = true;
                    break;
                default:
                    LOGGER.warn("Unknown nullability : " + theNullable + " for " + theColumnName + " of table "
                            + theNewTable.getName());
                }

                theAttribute.setDatatype(theDataType);
                theAttribute.setSize(theSize);
                theAttribute.setFraction(theFraction);
                theAttribute.setScale(theRadix);
                theAttribute.setDefaultValue(theDefaultValue);
                theAttribute.setNullable(isNullable);

                reverseEngineerAttribute(aModel, theAttribute, aOptions, aNotifier, aTableEntry, aConnection);

                try {
                    theNewTable.addAttribute(aModel, theAttribute);
                } catch (Exception e) {
                    throw new ReverseEngineeringException(e.getMessage(), e);
                }
            }
            theColumnsResultSet.close();

            // Reverse engineer primary keys
            reverseEngineerPrimaryKey(aModel, aTableEntry, theMetaData, theNewTable);

            // Reverse engineer indexes
            try {
                reverseEngineerIndexes(aModel, aTableEntry, theMetaData, theNewTable, aNotifier);
            } catch (SQLException e) {
                // if there is an sql exception, just ignore it
            }

            // We are done here
            try {
                aModel.addTable(theNewTable);
            } catch (Exception e) {
                throw new ReverseEngineeringException(e.getMessage(), e);
            }

        }
        theTablesResultSet.close();
    }

    protected void reverseEngineerPrimaryKey(Model aModel, TableEntry aTableEntry, DatabaseMetaData aMetaData,
            Table aTable) throws SQLException, ReverseEngineeringException {

        ResultSet thePrimaryKeyResultSet = aMetaData.getPrimaryKeys(aTableEntry.getCatalogName(), aTableEntry
                .getSchemaName(), aTableEntry.getTableName());
        Index thePrimaryKeyIndex = null;
        while (thePrimaryKeyResultSet.next()) {

            String thePKName = thePrimaryKeyResultSet.getString("PK_NAME");
            String theColumnName = thePrimaryKeyResultSet.getString("COLUMN_NAME");

            if (thePrimaryKeyIndex == null) {
                thePrimaryKeyIndex = new Index();
                thePrimaryKeyIndex.setIndexType(IndexType.PRIMARYKEY);
                thePrimaryKeyIndex.setName(convertIndexNameFor(aTable, thePKName));
                thePrimaryKeyIndex.setOriginalName(thePKName);
                if (StringUtils.isEmpty(thePrimaryKeyIndex.getName())) {
                    // Assume the default name is TABLE_NAME+"_PK"
                    thePrimaryKeyIndex.setName(aTableEntry.getTableName() + "_PK");
                }

                try {
                    aTable.addIndex(aModel, thePrimaryKeyIndex);
                } catch (Exception e) {
                    throw new ReverseEngineeringException(e.getMessage(), e);
                }
            }

            Attribute theIndexAttribute = aTable.getAttributes().findByName(dialect.getCastType().cast(theColumnName));
            if (theIndexAttribute == null) {
                throw new ReverseEngineeringException("Cannot find attribute " + theColumnName + " in table "
                        + aTable.getName());
            }

            try {
                thePrimaryKeyIndex.getExpressions().addExpressionFor(theIndexAttribute);
            } catch (ElementAlreadyExistsException e) {
                throw new ReverseEngineeringException("Error adding index attribute", e);
            }

        }
        thePrimaryKeyResultSet.close();
    }

    protected String convertIndexNameFor(Table aTable, String aIndexName) {
        return aIndexName;
    }

    protected void reverseEngineerIndexes(Model aModel, TableEntry aTableEntry, DatabaseMetaData aMetaData,
            Table aTable, ReverseEngineeringNotifier aNotifier) throws SQLException, ReverseEngineeringException {

        ResultSet theIndexResults = aMetaData.getIndexInfo(aTableEntry.getCatalogName(), aTableEntry.getSchemaName(),
                aTableEntry.getTableName(), false, true);
        Index theIndex = null;
        while (theIndexResults.next()) {

            String theIndexName = convertIndexNameFor(aTable, theIndexResults.getString("INDEX_NAME"));

            if ((theIndexName != null) && ((theIndex == null) || (!theIndex.getOriginalName().equals(theIndexName)))) {

                String theNewIndexName = dialect.getCastType().cast(theIndexName);

                if (aTable.getIndexes().findByName(theNewIndexName) == null) {
                    theIndex = new Index();
                    theIndex.setName(theNewIndexName);
                    theIndex.setOriginalName(theIndexName);

                    boolean isNonUnique = theIndexResults.getBoolean("NON_UNIQUE");
                    if (isNonUnique) {
                        theIndex.setIndexType(IndexType.NONUNIQUE);
                    } else {
                        theIndex.setIndexType(IndexType.UNIQUE);
                    }

                    aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGINDEX, theIndex.getName());

                    try {
                        aTable.addIndex(aModel, theIndex);
                    } catch (Exception e) {
                        throw new ReverseEngineeringException("Cannot add index " + theIndexName + " in table "
                                + aTable.getName() + " : " + e.getMessage(), e);
                    }
                } else {
                    theIndex = null;
                }
            }

            if (theIndex != null) {
                short aPosition = theIndexResults.getShort("ORDINAL_POSITION");

                String theColumnName = theIndexResults.getString("COLUMN_NAME");
                String theASCorDESC = theIndexResults.getString("ASC_OR_DESC");

                reverseEngineerIndexAttribute(aMetaData, aTableEntry, aTable, aNotifier, theIndex, theColumnName,
                        aPosition, theASCorDESC);
            }

        }
        theIndexResults.close();
    }

    /**
     * Reverse engineer an attribute within an index.
     * 
     * @param aMetaData
     *            the database meta data
     * @param aTableEntry
     *            the current table entry
     * @param aTable
     *            the table
     * @param aNotifier
     *            the notifier
     * @param aIndex
     *            the current index
     * @param aColumnName
     *            the column name
     * @param aPosition
     *            the column position
     * @param aASCorDESC
     *            "A" = Ascending, "D" = Descending, NULL = sort not supported
     * @throws SQLException
     *             in case of an error
     * @throws ReverseEngineeringException
     *             in case of an error
     */
    protected void reverseEngineerIndexAttribute(DatabaseMetaData aMetaData, TableEntry aTableEntry, Table aTable,
            ReverseEngineeringNotifier aNotifier, Index aIndex, String aColumnName, short aPosition, String aASCorDESC)
            throws SQLException, ReverseEngineeringException {
        Attribute theIndexAttribute = aTable.getAttributes().findByName(dialect.getCastType().cast(aColumnName));
        if (theIndexAttribute == null) {

            // It seems to be a function based index
            aIndex.getExpressions().addExpressionFor(aColumnName);

        } else {

            // It is a column based index
            try {
                aIndex.getExpressions().addExpressionFor(theIndexAttribute);
            } catch (ElementAlreadyExistsException e) {
                throw new ReverseEngineeringException("Error adding index attribute", e);
            }
        }
    }

    /**
     * Reverse engineer relations for a table.
     * 
     * @param aModel
     *            the model
     * @param aOptions
     *            the options
     * @param aNotifier
     *            the notifier
     * @param aTableEntry
     *            the tsble entry
     * @param aConnection
     *            the connection
     * @throws SQLException
     *             is thrown in case of an error
     * @throws ReverseEngineeringException
     *             is thrown in case of an error
     */
    protected void reverseEngineerRelations(Model aModel, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier, TableEntry aTableEntry, Connection aConnection) throws SQLException,
            ReverseEngineeringException {

        DatabaseMetaData theMetaData = aConnection.getMetaData();

        String theSchemaName = null;
        String theCatalogName = null;
        if (aTableEntry != null) {
            theSchemaName = aTableEntry.getSchemaName();
            theCatalogName = aTableEntry.getCatalogName();
        }

        int theSysCounter = 0;

        List<Relation> theNewRelations = new ArrayList<Relation>();

        String theImportingTableName = aModel.getDialect().getCastType().cast(aTableEntry.getTableName());
        aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGRELATION, theImportingTableName);

        Table theImportingTable = null;

        switch (aOptions.getTableNaming()) {
        case STANDARD:
            theImportingTable = aModel.getTables().findByName(theImportingTableName);
            break;
        case INCLUDE_SCHEMA:
            theImportingTable = aModel.getTables().findByNameAndSchema(theImportingTableName,
                    theSchemaName);
            break;
        default:
            throw new RuntimeException("Not supported naming type");
        }
        if (theImportingTable == null) {
            throw new ReverseEngineeringException("Cannot find table in model : " + theImportingTableName);
        }

        String theOldFKName = null;

        String theTablePattern = getEscapedPattern(theMetaData, aTableEntry.getTableName());
        String theSchemaPattern = getEscapedPattern(theMetaData, aTableEntry.getSchemaName());

        // Foreign keys
        Relation theNewRelation = null;
        ResultSet theForeignKeys = theMetaData.getImportedKeys(theCatalogName, theSchemaPattern, theTablePattern);
        while (theForeignKeys.next()) {
            String theFKName = theForeignKeys.getString("FK_NAME");

            if ((theNewRelation == null) || (!theFKName.equals(theOldFKName))) {

                theOldFKName = theFKName;

                String thePKTableName = theForeignKeys.getString("PKTABLE_NAME");
                String thePKTableSchema = theForeignKeys.getString("PKTABLE_SCHEM");

                String theUpdateRule = theForeignKeys.getString("UPDATE_RULE");
                String theDeleteRule = theForeignKeys.getString("DELETE_RULE");

                Table theExportingTable = null;
                switch (aOptions.getTableNaming()) {
                case INCLUDE_SCHEMA:
                    theExportingTable = aModel.getTables().findByNameAndSchema(
                            dialect.getCastType().cast(thePKTableName), thePKTableSchema);
                    break;
                case STANDARD:
                    theExportingTable = aModel.getTables().findByName(dialect.getCastType().cast(thePKTableName));
                    break;
                default:
                    throw new RuntimeException("Naming not supported : " + aOptions.getTableNaming());
                }
                if (theExportingTable != null) {

                    // The relation is only added to the model
                    // if the exporting table is also part of the model
                    String theRelationName = dialect.getCastType().cast(theFKName);
                    theNewRelation = aModel.getRelations().findByName(theRelationName);

                    boolean addNew = false;
                    if (theNewRelation == null) {
                        addNew = true;
                    } else {
                        if (!theNewRelation.getExportingTable().equals(theExportingTable)
                                || !theNewRelation.getImportingTable().equals(theImportingTable)) {
                            theRelationName = "ERRELSYS_" + theSysCounter++;
                            addNew = true;
                        }
                    }

                    if (addNew) {

                        theNewRelation = new Relation();
                        theNewRelation.setName(dialect.getCastType().cast(theRelationName));
                        theNewRelation.setOriginalName(theRelationName);

                        theNewRelation.setExportingTable(theExportingTable);
                        theNewRelation.setImportingTable(theImportingTable);

                        if (theUpdateRule != null) {
                            int theType = Integer.parseInt(theUpdateRule);

                            theNewRelation.setOnUpdate(getCascadeType(theType));
                        } else {

                            theNewRelation.setOnUpdate(CascadeType.NOTHING);
                        }

                        if (theDeleteRule != null) {
                            int theType = Integer.parseInt(theDeleteRule);

                            theNewRelation.setOnDelete(getCascadeType(theType));
                        } else {

                            theNewRelation.setOnDelete(CascadeType.NOTHING);
                        }

                        theNewRelations.add(theNewRelation);
                    }
                }
            }

            if ((theNewRelation != null) && (theNewRelation.getImportingTable() != null)
                    && (theNewRelation.getExportingTable() != null)) {
                String thePKColumnName = dialect.getCastType().cast(theForeignKeys.getString("PKCOLUMN_NAME"));
                String theFKColumnName = dialect.getCastType().cast(theForeignKeys.getString("FKCOLUMN_NAME"));

                Attribute theExportingAttribute = theNewRelation.getExportingTable().getAttributes().findByName(
                        dialect.getCastType().cast(thePKColumnName));
                if (theExportingAttribute == null) {
                    throw new ReverseEngineeringException("Cannot find column " + thePKColumnName + " in table "
                            + theNewRelation.getExportingTable().getName());
                }

                Index thePrimaryKey = theNewRelation.getExportingTable().getPrimarykey();
                if (thePrimaryKey == null) {
                    throw new ReverseEngineeringException("Table " + theNewRelation.getExportingTable().getName()
                            + " does not have a primary key");
                }
                IndexExpression theExpression = thePrimaryKey.getExpressions().findByAttributeName(thePKColumnName);
                if (theExpression == null) {
                    throw new RuntimeException("Cannot find attribute " + thePKColumnName
                            + " in primary key for table " + theNewRelation.getExportingTable().getName());
                }

                Attribute theImportingAttribute = theNewRelation.getImportingTable().getAttributes().findByName(
                        theFKColumnName);
                if (theImportingAttribute == null) {
                    throw new ReverseEngineeringException("Cannot find column " + theFKColumnName + " in table "
                            + theNewRelation.getImportingTable().getName());
                }

                theNewRelation.getMapping().put(theExpression, theImportingAttribute);
            }
        }
        theForeignKeys.close();

        try {
            for (Relation theRelation : theNewRelations) {
                try {
                    aModel.addRelation(theRelation);
                } catch (ElementAlreadyExistsException e) {
                    // This might happen for instance on DB2 databases
                    // We will try to generate a new name here!!!
                    int counter = 0;
                    String theNewName = null;
                    while (counter == 0
                            || aModel.getRelations().findByName(dialect.getCastType().cast(theNewName)) != null) {
                        counter++;
                        theNewName = theRelation.getExportingTable().getName() + "_" + theRelation.getImportingTable()
                                + "_FK" + counter;
                    }
                    LOGGER.warn("Relation " + theRelation.getName() + " exists. Renaming it to " + theNewName);
                    theRelation.setName(theNewName);
                    aModel.addRelation(theRelation);
                }
            }
        } catch (Exception e) {
            throw new ReverseEngineeringException(e.getMessage(), e);
        }
    }

    /**
     * Get the list of available table types that shall be reverse engineered.
     * Default is only "TABLE", but can be overridden by subclasses.
     * 
     * @return the l
     */
    protected String[] getReverseEngineeringTableTypes() {
        return new String[] { TABLE_TABLE_TYPE, VIEW_TABLE_TYPE };
    }

    /**
     * Test if a table type is a view.
     * 
     * @param aTableType
     *            the table type
     * @return true if yes, else false
     */
    protected boolean isTableTypeView(String aTableType) {
        return VIEW_TABLE_TYPE.equals(aTableType);
    }

    /**
     * Check if the table is a valid table for reverse engineering.
     * 
     * @param aTableName
     *            the table name
     * @param aTableType
     *            the table type
     * @return true if the table is valid, else false
     */
    protected boolean isValidTable(String aTableName, String aTableType) {
        return true;
    }

    /**
     * Check if the table is a valid view for reverse engineering.
     * 
     * @param aTableName
     *            the table name
     * @param aTableType
     *            the table type
     * @return true if the table is valid, else false
     */
    protected boolean isValidView(String aTableName, String aTableType) {
        return true;
    }

    public void updateModelFromConnection(Model aModel, ERDesignerWorldConnector aConnector, Connection aConnection,
            ReverseEngineeringOptions aOptions, ReverseEngineeringNotifier aNotifier) throws SQLException,
            ReverseEngineeringException {

        if (aModel.getDialect().isSupportsDomains()) {
            reverseEngineerDomains(aModel, aOptions, aNotifier, aConnection);
        }

        for (TableEntry theTable : aOptions.getTableEntries()) {
            if (isTableTypeView(theTable.getTableType())) {
                reverseEngineerView(aModel, aOptions, aNotifier, theTable, aConnection);
            } else {
                reverseEngineerTable(aModel, aOptions, aNotifier, theTable, aConnection);
            }
        }

        for (TableEntry theTableEntry : aOptions.getTableEntries()) {
            // Reverse engineer only relations for tables, not for views!
            if (TABLE_TABLE_TYPE.equals(theTableEntry.getTableType())) {
                reverseEngineerRelations(aModel, aOptions, aNotifier, theTableEntry, aConnection);
            }
        }

        aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGFINISHED, "");
    }

    /**
     * Reverse engineer the domains.
     * 
     * @param aModel
     * @param aOptions
     * @param aNotifier
     * @param aConnection
     * @throws SQLException
     * @throws ReverseEngineeringException
     */
    protected void reverseEngineerDomains(Model aModel, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier, Connection aConnection) throws SQLException,
            ReverseEngineeringException {

        PreparedStatement theStatement = aConnection
                .prepareStatement("SELECT * FROM information_schema.domains where DOMAIN_SCHEMA = ?");
        for (SchemaEntry theEntry : aOptions.getSchemaEntries()) {
            theStatement.setString(1, theEntry.getSchemaName());
            ResultSet theResult = theStatement.executeQuery();
            while (theResult.next()) {
                String theDomainName = theResult.getString("DOMAIN_NAME");
                String theDataType = theResult.getString("DATA_TYPE");
                Integer theSize = null;
                try {
                    Integer theTemp = theResult.getInt("NUMERIC_PRECISION");
                    if (theTemp != null) {
                        theSize = theTemp;
                    }
                } catch (Exception e) {
                }
                try {
                    Integer theTemp = theResult.getInt("CHARACTER_MAXIMUM_LENGTH");
                    if (theTemp != null) {
                        theSize = theTemp;
                    }
                } catch (Exception e) {
                }

                int theFraction = theResult.getInt("NUMERIC_PRECISION_RADIX");
                int theScale = theResult.getInt("NUMERIC_SCALE");

                Domain theDomain = aModel.getDomains().findByName(theDomainName);
                if (theDomain != null) {
                    throw new ReverseEngineeringException("Duplicate domain found : " + theDomainName);
                }

                DataType theType = aModel.getDialect().getDataTypes().findByName(
                        convertColumnTypeToRealType(theDataType));
                if (theType != null) {
                    theDomain = new Domain();
                    theDomain.setName(theDomainName);
                    theDomain.setConcreteType(theType);
                    theDomain.setSize(theSize);
                    theDomain.setFraction(theFraction);
                    theDomain.setScale(theScale);

                    try {
                        aModel.addDomain(theDomain);
                    } catch (VetoException e) {
                        throw new ReverseEngineeringException(e.getMessage(), e);
                    }
                } else {
                    throw new ReverseEngineeringException("Unknown data type " + theDataType + " for domain "
                            + theDomainName);
                }

                reverseEngineerDomain(aModel, theDomain, aOptions, aNotifier, aConnection);
            }
            theResult.close();
        }
        theStatement.close();
    }

    public List<SchemaEntry> getSchemaEntries(Connection aConnection) throws SQLException {

        List<SchemaEntry> theList = new ArrayList<SchemaEntry>();

        DatabaseMetaData theMetadata = aConnection.getMetaData();
        ResultSet theResult = theMetadata.getSchemas();

        while (theResult.next()) {
            String theSchemaName = theResult.getString("TABLE_SCHEM");
            String theCatalogName = theResult.getString("TABLE_CATALOG");

            theList.add(new SchemaEntry(theCatalogName, theSchemaName));
        }

        return theList;
    }

    protected List<TableEntry> getTablesForSchemaEntry(Connection aConnection, SchemaEntry aEntry) throws SQLException {

        List<TableEntry> theResult = new ArrayList<TableEntry>();

        DatabaseMetaData theMetaData = aConnection.getMetaData();

        // Reverse engineer tables
        ResultSet theTablesResultSet = null;
        String theCatalogName = null;
        String theSchemaName = null;
        if (aEntry != null) {
            theCatalogName = aEntry.getCatalogName();
            theSchemaName = aEntry.getSchemaName();
            theTablesResultSet = theMetaData.getTables(theCatalogName, theSchemaName, null,
                    getReverseEngineeringTableTypes());
        } else {
            theTablesResultSet = theMetaData.getTables(null, null, null, getReverseEngineeringTableTypes());
        }

        while (theTablesResultSet.next()) {

            String theTableType = theTablesResultSet.getString("TABLE_TYPE");
            String theTableName = theTablesResultSet.getString("TABLE_NAME");

            if (isTableTypeView(theTableType)) {
                if (isValidView(theTableName, theTableType)) {
                    TableEntry theEntry = new TableEntry(theCatalogName, theSchemaName, theTableName, theTableType);
                    theResult.add(theEntry);
                }
            } else {
                if (isValidTable(theTableName, theTableType)) {
                    TableEntry theEntry = new TableEntry(theCatalogName, theSchemaName, theTableName, theTableType);
                    theResult.add(theEntry);
                }
            }
        }
        theTablesResultSet.close();

        return theResult;
    }

    public List<TableEntry> getTablesForSchemas(Connection aConnection, List<SchemaEntry> aSchemaEntries)
            throws SQLException {
        List<TableEntry> theResult = new ArrayList<TableEntry>();

        if (dialect.isSupportsSchemaInformation()) {

            for (SchemaEntry theEntry : aSchemaEntries) {
                theResult.addAll(getTablesForSchemaEntry(aConnection, theEntry));
            }

        } else {
            theResult.addAll(getTablesForSchemaEntry(aConnection, null));
        }

        return theResult;
    }

    protected String extractSelectDDLFromViewDefinition(String theViewDefinition) {
        if (!StringUtils.isEmpty(theViewDefinition)) {
            String theViewDefinitionLower = theViewDefinition.toLowerCase();
            theViewDefinitionLower = theViewDefinitionLower.replace('\n', ' ');
            theViewDefinitionLower = theViewDefinitionLower.replace('\r', ' ');
            theViewDefinitionLower = theViewDefinitionLower.replace('\t', ' ');

            if (theViewDefinitionLower.startsWith("create view ")) {
                int p = theViewDefinitionLower.indexOf(" as ");
                if (p >= 0) {
                    theViewDefinition = theViewDefinition.substring(p + 4).trim();
                }
            }
            if (theViewDefinition.endsWith(";")) {
                theViewDefinition = theViewDefinition.substring(0, theViewDefinition.length() - 1);
            }
        }
        return theViewDefinition;
    }
}