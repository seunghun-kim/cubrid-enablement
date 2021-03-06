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
package org.eclipse.datatools.enablement.cubrid;

//import java.util.MissingResourceException;
//import java.util.ResourceBundle;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 * 
 * @author seunghun_kim@cubrid.com
 *
 */
public class CUBRIDPlugin extends Plugin {
	// The shared instance.
	private static CUBRIDPlugin plugin;
	
	// Resource bundle.
//	private ResourceBundle resourceBundle;
	
	public static final String ID = "org.eclipse.datatools.enablement.cubrid"; // $NON-NLS-1$
//	public static final String ID = "org.eclipse.wst.rdb.mysql";	//$NON-NLS-1$
	
	/**
	 * The constructor.
	 */
	public CUBRIDPlugin() {
		super();
	}
	
	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}
	
	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
//		resourceBundle = null;
	}
	
	/**
	 * Returns the shared instance.
	 *  
	 * @return the shared instance
	 */
	public static CUBRIDPlugin getDefault() {
		return plugin;
	}
//	
//	/**
//	 * Returns the string from the plugin's resource bundle, or 'key' if not
//	 * found.
//	 */
//	public static String getResourceString(String key) {
//		ResourceBundle bundle = CUBRIDPlugin.getDefault().getResourceBundle();
//		try {
//			return (bundle != null) ? bundle.getString(key) : key;
//		} catch (MissingResourceException e) {
//			return key;
//		}
//	}
//	
//	/**
//	 * Returns the plugin's resource bundle. 
//	 */
//	public ResourceBundle getResourceBundle() {
//		try {
//			if (resourceBundle == null) {
//				resourceBundle = ResourceBundle
//						.getBundle("org.eclipse.wst.rdb.cubrid.CUBRIDPluginResources");	// $NON-NLS-1$
//			}
//		} catch (MissingResourceException e) {
//			resourceBundle = null;
//		}
//		return resourceBundle;
//	}
}
