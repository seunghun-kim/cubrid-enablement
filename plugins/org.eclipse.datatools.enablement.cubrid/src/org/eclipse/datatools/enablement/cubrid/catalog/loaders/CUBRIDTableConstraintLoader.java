/*******************************************************************************
 * Copyright (c) 2016 Seunghun
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Seunghun - initial API and implementation
 *******************************************************************************/
package org.eclipse.datatools.enablement.cubrid.catalog.loaders;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.datatools.connectivity.sqm.core.rte.ICatalogObject;
import org.eclipse.datatools.connectivity.sqm.core.rte.jdbc.JDBCColumn;
import org.eclipse.datatools.connectivity.sqm.loader.JDBCTableConstraintLoader;
import org.eclipse.datatools.connectivity.sqm.loader.Messages;
import org.eclipse.datatools.enablement.cubrid.CUBRIDPlugin;
import org.eclipse.datatools.enablement.cubrid.catalog.CUBRIDCatalogForeignKey;
import org.eclipse.datatools.enablement.cubrid.catalog.CUBRIDCatalogPrimaryKey;
import org.eclipse.datatools.enablement.cubrid.catalog.CUBRIDCatalogTable;
import org.eclipse.datatools.enablement.cubrid.log.PluginLogger;
import org.eclipse.datatools.modelbase.sql.constraints.ForeignKey;
import org.eclipse.datatools.modelbase.sql.constraints.Index;
import org.eclipse.datatools.modelbase.sql.constraints.PrimaryKey;
import org.eclipse.datatools.modelbase.sql.constraints.UniqueConstraint;
import org.eclipse.datatools.modelbase.sql.schema.ReferentialActionType;
import org.eclipse.datatools.modelbase.sql.tables.BaseTable;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.emf.common.util.EList;

import com.ibm.icu.text.MessageFormat;

public class CUBRIDTableConstraintLoader extends JDBCTableConstraintLoader {

	public CUBRIDTableConstraintLoader() {
		super(null, null);
	}

	protected ForeignKey createForeignKey() {
		return new CUBRIDCatalogForeignKey();
	}

	protected PrimaryKey createPrimaryKey() {
		return new CUBRIDCatalogPrimaryKey();
	}

	public PrimaryKey loadPrimaryKey(PrimaryKey existingPK) throws SQLException {
		ResultSet rs = null;
		try {
			Map columns = new HashMap();
			PrimaryKey pk = null;
			for (rs = createPrimaryKeyResultSet(); rs.next();) {
				int seq = -1;
				String columnName = null;
				if (pk == null) {
					String pkName = rs.getString(6);
					if (pkName == null) {
						return null;
					}
					columnName = rs.getString(4);
					seq = rs.getInt(5);

					if (existingPK != null
							&& pkName.equals(existingPK.getName())) {
						pk = existingPK;
						pk.getMembers().clear();
						if (existingPK instanceof ICatalogObject) {
							((ICatalogObject) pk).refresh();
						}
					}
					else {
						pk = createPrimaryKey();
						pk.setName(pkName);
					}
				}
				if (seq > -1) {
					columns.put(new Integer(seq),
							findColumn(columnName));
				}
				else {
					columns.put(new Integer(rs.getShort(COLUMN_KEY_SEQ)),
							findColumn(rs.getString(COLUMN_COLUMN_NAME)));
				}
			}
			for (Iterator it = columns.values().iterator(); it.hasNext();) {
				pk.getMembers().add(it.next());
			}
			return pk;
		}
		finally {
			if (rs != null) {
				closeResultSet(rs);
			}
		}
	}

	protected ResultSet createForeignKeyResultSet() throws SQLException {
		return getCatalogObject().getConnection().
				getMetaData().getImportedKeys(null, null, this.getTable().getName());
	}

	protected ResultSet createPrimaryKeyResultSet() throws SQLException {
		return getCatalogObject().getConnection().
				getMetaData().getPrimaryKeys(null, null, this.getTable().getName());
	}

	protected ResultSet createUniqueConstraintResultSet() throws SQLException {
		try {
			return getCatalogObject().getConnection().getMetaData()
					.getExportedKeys(null,
							null, this.getTable().getName());
		}
		catch (RuntimeException e) {
			SQLException error = new SQLException(
					MessageFormat
					.format(
							Messages.Error_Unsupported_DatabaseMetaData_Method,
							new Object[] { "java.sql.DatabaseMetaData.getExportedKeys()"})); //$NON-NLS-1$
			error.initCause(e);
			throw error;
		}
	}

	public void loadUniqueConstraints(PrimaryKey pk, List containmentList,
			Collection existingUCs) throws SQLException {
		ResultSet rs = null;
//		if (pk != null) {	// this block is needless for CUBRID because pk is not equal uk in CUBRID.
//			// Remove this guy from the list.
//			existingUCs.remove(pk);
//		}
		try {
			Map constraints = new HashMap();
			Map constraintColumns = new HashMap();
			for (rs = createUniqueConstraintResultSet(); rs.next();) {

				String ucName = rs.getString(COLUMN_PKCOLUMN_NAME);
				if (!constraints.containsKey(ucName)) {
					UniqueConstraint uc = (UniqueConstraint) getAndRemoveSQLObject(
							existingUCs, ucName);
					if (uc == null) {
						// create the next UC
						uc = createUniqueConstraint();
						uc.setName(ucName);
					}
					else {
						uc.getMembers().clear();
						if (uc instanceof ICatalogObject) {
							((ICatalogObject) uc).refresh();
						}
					}
					containmentList.add(uc);
					constraints.put(ucName, uc);
					constraintColumns.put(ucName, new TreeMap());
				}
				((Map) constraintColumns.get(ucName)).put(new Integer(rs
						.getShort(COLUMN_KEY_SEQ)), findColumn(rs
								.getString(COLUMN_PKCOLUMN_NAME)));
			}
			for (Iterator it = constraints.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				UniqueConstraint uc = (UniqueConstraint) entry.getValue();
				for (Iterator colIt = ((Map) constraintColumns
						.get(uc.getName())).values().iterator(); colIt
						.hasNext();) {
					uc.getMembers().add(colIt.next());
				}
			}
		}
		finally {
			if (rs != null) {
				closeResultSet(rs);
			}
		}
	}

	private Table findFKTable (String fkTable_Name) {
		EList tables = this.getTable().getSchema().getTables();
		ListIterator iterator = tables.listIterator();
		while (iterator.hasNext()) {
			Table table = (Table) iterator.next();
			if (table.getName().equals(fkTable_Name)) 
				return table;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.sqm.loader.JDBCTableConstraintLoader#loadForeignKeys(java.util.List, java.util.Collection)
	 */
	public void loadForeignKeys(List containmentList, Collection existingFKs)
			throws SQLException {

		ResultSet rs = null;
		try {
			Map constraints = new HashMap();
			Map constraintColumns = new HashMap();
			for (rs = createForeignKeyResultSet(); rs.next();) {
				final String fkTable_Name = rs.getString("FKTABLE_NAME"); //$NON-NLS-1$
				final String fkName = rs.getString("FK_NAME"); //$NON-NLS-1$	// may be null
				
				CUBRIDCatalogTable fkTable = (CUBRIDCatalogTable) findFKTable(fkTable_Name);
				
				if (fkTable == null || fkName == null)	// if can't find matched table, then continue.
					continue;

				if (!constraints.containsKey(fkName)) {
					ForeignKey fk = (ForeignKey) getAndRemoveSQLObject(
							existingFKs, fkName);
					if (fk == null) {
						// create the next FK
						fk = createForeignKey();
						fk.setName(fkName);
					}
					else {
						fk.getMembers().clear();
						if (fk instanceof ICatalogObject) {
							((ICatalogObject) fk).refresh();
						}
					}
					if (fkTable.getPrimaryKey() != null) {
						fk.setUniqueConstraint(fkTable.getPrimaryKey());
					} else {
						Index index = fkTable.findIndexWithColumnName(rs
								.getString("PKCOLUMN_NAME")); //$NON-NLS-1$
						if (index != null) {
							fk.setUniqueIndex(index);
						}
					}
					short updateRule = rs.getShort("UPDATE_RULE"); //$NON-NLS-1$
					switch (updateRule) {
					case DatabaseMetaData.importedKeyCascade:
						fk.setOnUpdate(ReferentialActionType.CASCADE_LITERAL);
						break;
					case DatabaseMetaData.importedKeyRestrict:
						fk.setOnUpdate(ReferentialActionType.RESTRICT_LITERAL);
						break;
					case DatabaseMetaData.importedKeySetDefault:
						fk.setOnUpdate(ReferentialActionType.SET_DEFAULT_LITERAL);
						break;
					case DatabaseMetaData.importedKeySetNull:
						fk.setOnUpdate(ReferentialActionType.SET_NULL_LITERAL);
						break;
					case DatabaseMetaData.importedKeyNoAction:
						fk.setOnUpdate(ReferentialActionType.NO_ACTION_LITERAL);
						break;
					default:
						fk.setOnUpdate(ReferentialActionType.CASCADE_LITERAL);
						break;
					}
					short deleteRule = rs.getShort("DELETE_RULE"); //$NON-NLS-1$
					switch (deleteRule) {
					case DatabaseMetaData.importedKeyCascade:
						fk.setOnDelete(ReferentialActionType.CASCADE_LITERAL);
						break;
					case DatabaseMetaData.importedKeyRestrict:
						fk.setOnDelete(ReferentialActionType.RESTRICT_LITERAL);
						break;
					case DatabaseMetaData.importedKeySetDefault:
						fk.setOnDelete(ReferentialActionType.SET_DEFAULT_LITERAL);
						break;
					case DatabaseMetaData.importedKeySetNull:
						fk.setOnDelete(ReferentialActionType.SET_NULL_LITERAL);
						break;
					case DatabaseMetaData.importedKeyNoAction:
						fk.setOnDelete(ReferentialActionType.NO_ACTION_LITERAL);
						break;
					default:
						fk.setOnDelete(ReferentialActionType.CASCADE_LITERAL);
						break;
					}
					short deferrability = rs.getShort("DEFERRABILITY"); //$NON-NLS-1$
					switch (deferrability) {
					case DatabaseMetaData.importedKeyInitiallyDeferred:
						fk.setDeferrable(true);
						fk.setInitiallyDeferred(true);
						break;
					case DatabaseMetaData.importedKeyInitiallyImmediate:
						fk.setDeferrable(true);
						fk.setInitiallyDeferred(false);
						break;
					case DatabaseMetaData.importedKeyNotDeferrable:
					default:
						fk.setDeferrable(false);
						break;
					}
					String columnName = rs.getString("FKCOLUMN_NAME"); //$NON-NLS-1$
					Column column = this.findColumn(columnName);
					fk.getMembers().add(column);
					UniqueConstraint uk = findUniqueConstraint(rs
							.getString(COLUMN_PKTABLE_CAT), rs
							.getString(COLUMN_PKTABLE_SCHEM), rs
							.getString(COLUMN_PKTABLE_NAME), rs
							.getString(COLUMN_PK_NAME));
					if (uk != null) {
						fk.setUniqueConstraint(uk);
					}

					containmentList.add(fk);
					constraints.put(fkName, fk);
					constraintColumns.put(fkName, new TreeMap());
				}
				((Map) constraintColumns.get(fkName)).put(new Integer(rs
						.getShort(COLUMN_KEY_SEQ)), findColumn(rs
								.getString(COLUMN_FKCOLUMN_NAME)));
			}
			for (Iterator it = constraints.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				ForeignKey fk = (ForeignKey) entry.getValue();
				for (Iterator colIt = ((Map) constraintColumns
						.get(fk.getName())).values().iterator(); colIt
						.hasNext();) {
					fk.getMembers().add(colIt.next());
				}
				initReferenceAnnotation(fk);
			}
		}
		finally {
			if (rs != null) {
				closeResultSet(rs);
			}
		}
	}

	protected UniqueConstraint findUniqueConstraint(String catalogName,
			String schemaName, String tableName, String keyName) {
		Table table = findTable(catalogName, schemaName, tableName);
		if (table == null || !(table instanceof BaseTable)) {
			return null;
		}
		for (Iterator it = ((BaseTable) table).getUniqueConstraints()
				.iterator(); it.hasNext();) {
			UniqueConstraint uc = (UniqueConstraint) it.next();
			if (uc != null || !"".equals(uc.getName())) {
				return uc;
			}
		}
		return null;
	}

	protected Table findTable(String catalogName, String schemaName,
			String tableName) {
		if (tableName == null) {
			return null;
		}

		// this loop is finding super table of fk owner.
		for (Iterator tableIt = getTable().getSchema().getTables().iterator(); tableIt
				.hasNext();) {
			Table table = (Table) tableIt.next();
			if (tableName.equals(table.getName())) {
				// found it
				return table;
			}
		}
		return null;
	}

}
