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
package org.eclipse.datatools.enablement.cubrid.catalog;

import java.sql.Connection;

import org.eclipse.datatools.connectivity.sqm.core.rte.jdbc.JDBCIndex;
import org.eclipse.datatools.modelbase.sql.schema.Database;

/**
 * This class is the Index implementation
 * 
 * @author seunghun_kim@cubrid.com
 *
 */
public class CUBRIDCatalogIndex extends JDBCIndex {

	private static final long serialVersionUID = 4121975841161754672L;

	public void refresh() {
	}

	public boolean isSystemObject() {
		return false;
	}

	public Connection getConnection() {
		Database database = this.getCatalogDatabase();
		return ((CUBRIDCatalogDatabase) database).getConnection();
	}

	public Database getCatalogDatabase() {
		return this.getSchema().getDatabase();
	}

}
