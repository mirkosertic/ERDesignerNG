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
package de.erdesignerng.dialect.postgres;

import de.erdesignerng.dialect.DataType;
import de.erdesignerng.dialect.GenericDataTypeImpl;
import de.erdesignerng.dialect.NameCastType;
import de.erdesignerng.dialect.Dialect;
import java.sql.Types;
import java.util.Map;

/**
 * @author $Author: mirkosertic $
 * @version $Date: 2008-11-15 17:04:23 $
 */
public final class PostgresDialect extends Dialect {

	public static final String ARRAY_INDICATOR = "[]";

	public PostgresDialect() {
		setSpacesAllowedInObjectNames(true);
		setCaseSensitive(false);
		setMaxObjectNameLength(255);
		setNullablePrimaryKeyAllowed(false);
		setCastType(NameCastType.NOTHING);
		setSupportsDomains(true);
		setSupportsCustomTypes(true);
		setSupportsSchemaInformation(true);
		setDefaultSchemaName("public");

		addSystemSchema("information_schema");
		addSystemSchema("pg_catalog");
		addSystemSchema("pg_toast");
		addSystemSchema("pg_toast_temp");
		addSystemSchema("pg_toast_temp_1");
		addSystemSchema("pg_temp_1");
		addSystemSchema("public");

		// Bug Fixing 3306174 [ERDesignerNG] Unknown datatype (2.7.0 release) (postgres + postgis)
		// http://postgis.refractions.net/docs/reference.html#PostGIS_Types
		registerType(createDataType("box2d", "", SPATIAL_COLUMN_TYPE));
		registerType(createDataType("box3d", "", SPATIAL_COLUMN_TYPE));
		registerType(createDataType("box3d_extent", "", SPATIAL_COLUMN_TYPE));
		registerType(createDataType("geometry", "", SPATIAL_COLUMN_TYPE));
		registerType(createDataType("geometry_dump", "", SPATIAL_COLUMN_TYPE));
		registerType(createDataType("geography", "", SPATIAL_COLUMN_TYPE));

		//official data types according to http://www.postgresql.org/docs/9.3/static/datatype.html#DATATYPE-TABLE
		registerType(createDataType("bigint", "", Types.BIGINT));
		registerType(createDataType("bigserial", "", true, Types.BIGINT));
		registerType(createDataType("bit", "[" + GenericDataTypeImpl.SIZE_TOKEN + "]", Types.BIT));
		registerType(createDataType("bit varying", "[" + GenericDataTypeImpl.SIZE_TOKEN + "]", Types.BIT));
		registerType(createDataType("boolean", "", Types.BOOLEAN));
		registerType(createDataType("box", "", Types.OTHER));
		registerType(createDataType("bytea", "", Types.BINARY, Types.VARBINARY));
		registerType(createDataType("character", "[" + GenericDataTypeImpl.SIZE_TOKEN + "]", Types.CHAR));
		registerType(createDataType("character varying", "[" + GenericDataTypeImpl.SIZE_TOKEN + "]", Types.VARCHAR));
		registerType(createDataType("cidr", "", Types.VARCHAR, Types.NVARCHAR));
		registerType(createDataType("circle", "", Types.OTHER));
		registerType(createDataType("date", "", Types.DATE));
		registerType(createDataType("double precision", "", Types.DOUBLE));
		registerType(createDataType("inet", "", Types.VARCHAR, Types.NVARCHAR));
		registerType(createDataType("integer", "", Types.INTEGER));
		registerType(createDataType("interval", "", Types.TIMESTAMP));
		registerType(createDataType("json", "", Types.VARCHAR, Types.NVARCHAR));
		registerType(createDataType("line", "", Types.OTHER));
		registerType(createDataType("lseg", "", Types.OTHER));
		registerType(createDataType("macaddr", "", Types.VARCHAR, Types.NVARCHAR));
		registerType(createDataType("money", "", Types.DOUBLE));
		registerType(createDataType("numeric", "[" + GenericDataTypeImpl.SIZE_TOKEN + "], [" + GenericDataTypeImpl.FRACTION_TOKEN + "]", Types.NUMERIC, Types.DECIMAL));
		registerType(createDataType("path", "", SPATIAL_COLUMN_TYPE));
		registerType(createDataType("point", "", SPATIAL_COLUMN_TYPE));
		registerType(createDataType("polygon", "", Types.OTHER));
		registerType(createDataType("real", "", Types.REAL));
		registerType(createDataType("smallint", "", Types.SMALLINT));
		registerType(createDataType("smallserial", "", true, Types.SMALLINT));
		registerType(createDataType("serial", "", true, Types.INTEGER));
		registerType(createDataType("text", "", Types.VARCHAR));
		registerType(createDataType("time", "[" + GenericDataTypeImpl.SIZE_TOKEN + "]", Types.TIME));
		registerType(createDataType("time() with time zone", "[" + GenericDataTypeImpl.SIZE_TOKEN + "]", Types.TIME));
		registerType(createDataType("timestamp", "[" + GenericDataTypeImpl.SIZE_TOKEN + "]", Types.TIMESTAMP));
		registerType(createDataType("timestamp() with time zone", "[" + GenericDataTypeImpl.SIZE_TOKEN + "]", Types.TIMESTAMP));
		registerType(createDataType("tsquery", "", Types.OTHER));
		registerType(createDataType("tsvector", "", Types.OTHER));
		registerType(createDataType("txid_snapshot", "", Types.OTHER));
		registerType(createDataType("uuid", "", Types.VARCHAR, Types.NVARCHAR));
		registerType(createDataType("xml", "", Types.SQLXML));

		//internal data types
		registerType(createDataType("name", "", Types.VARCHAR));
		registerType(createDataType("oid", "", Types.INTEGER));
		registerType(createDataType("clob", "", Types.CLOB, Types.LONGVARCHAR));

		//official aliases according to http://www.postgresql.org/docs/9.3/static/datatype.html#DATATYPE-TABLE
		addDataTypeAlias("int8", "bigint");
		addDataTypeAlias("serial8", "bigserial");
		addDataTypeAlias("varbit", "bit varying");
		addDataTypeAlias("bool", "boolean");
		addDataTypeAlias("char", "character");
		addDataTypeAlias("varchar", "character varying");
		addDataTypeAlias("float8", "double precision");
		addDataTypeAlias("int", "integer");
		addDataTypeAlias("int4", "integer");
		addDataTypeAlias("decimal", "numeric");
		addDataTypeAlias("float4", "real");
		addDataTypeAlias("int2", "smallint");
		addDataTypeAlias("serial2", "smallserial");
		addDataTypeAlias("serial4", "serial");
		addDataTypeAlias("timetz", "time with time zone");
		addDataTypeAlias("timestamptz", "timestamp with time zone");

		//internal aliases
		addDataTypeAlias("blob", "bytea");
		addDataTypeAlias("bpchar", "character");

		seal();
	}

	@Override
	public PostgresReverseEngineeringStrategy getReverseEngineeringStrategy() {
		return new PostgresReverseEngineeringStrategy(this);
	}

	@Override
	public String getUniqueName() {
		return "PostgresDialect";
	}

	@Override
	public String getDriverClassName() {
		return "org.postgresql.Driver";
	}

	@Override
	public String getDriverURLTemplate() {
		return "jdbc:postgresql://<host>:<port>/<db>";
	}

	@Override
	public PostgresSQLGenerator createSQLGenerator() {
		return new PostgresSQLGenerator(this);
	}

	@Override
	public Class getHibernateDialectClass() {
		return org.hibernate.dialect.PostgreSQLDialect.class;
	}

	@Override
	public DataType createDataType(String aName, String aDefinition, int... aJdbcType) {
		return new PostgresDataType(aName, aDefinition, aJdbcType);
	}

	@Override
	public DataType createDataType(String aName, String aDefinition, boolean aIdentity, int... aJdbcType) {
		return new PostgresDataType(aName, aDefinition, aIdentity, aJdbcType);
	}

	@Override
	public DataType createDataType(String aName, String aDefinition, boolean anIdentity, boolean anArray, int... aJdbcType) {
		return new PostgresDataType(aName, aDefinition, anIdentity, anArray, aJdbcType);
	}

	@Override
	public boolean supportsSpatialIndexes() {
		return true;
	}

	@Override
	protected void registerType(DataType aType) {
		String anArrayDataTypeName = aType.getName() + ARRAY_INDICATOR;
		String anArrayDataTypeNameAlias = "_" + aType.getName();

		//register a common type, e.g. "integer"
		super.registerType(aType);
		//also register a common type as array of common type, e.g. "integer[]"...
		super.registerType(createDataType(anArrayDataTypeName, aType.getDefinition(), aType.isIdentity(), true, aType.getJDBCType()));
		//... and the appropriate system alias of the array of common type, e.g. "_integer" for "integer[]"
		super.addDataTypeAlias(anArrayDataTypeNameAlias, anArrayDataTypeName);
	}

	@Override
	public void addDataTypeAlias(String aDataTypeAlias, String aBaseDataTypeName) {
		String aDataTypeAliasArrayName = aDataTypeAlias + ARRAY_INDICATOR;
		String aDataTypeAliasArrayAliasName = "_" + aDataTypeAlias;
		String aBaseDataTypeArrayName = aBaseDataTypeName + ARRAY_INDICATOR;

		//register an alias of a real data type, e.g. "int4" for "integer"
		super.addDataTypeAlias(aDataTypeAlias, aBaseDataTypeName);
		//also register an array of alias, e.g. "int4[]" for "integer[]"...
		super.addDataTypeAlias(aDataTypeAliasArrayName, aBaseDataTypeArrayName);
		//... and the appropriate system alias of the array of alias, e.g. "_int4" for "integer[]"...
		super.addDataTypeAlias(aDataTypeAliasArrayAliasName, aBaseDataTypeArrayName);
	}

	@Override
	protected String convertTypeNameToRealTypeName(String aTypeName) {
		for (Map.Entry<String, String> theAliasEntry : getDataTypeAliases().entrySet()) {
			if (theAliasEntry.getKey().equalsIgnoreCase(aTypeName)) {
				return theAliasEntry.getValue();
			}
		}

		return aTypeName;
	}

}