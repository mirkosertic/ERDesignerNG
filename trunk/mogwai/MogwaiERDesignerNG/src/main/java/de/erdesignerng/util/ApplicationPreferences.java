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
package de.erdesignerng.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Class for handling application preferences, LRUfiles and so on.
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2008-11-16 14:22:01 $
 */
public class ApplicationPreferences {

    private static final String LRUPREFIX = "file_";

    private static final String DOT_PATH = "DOT_PATH";

    private static final String CLASSPATHPREFIX = "classpath_";

    private static final String LRCPREFIX = "lrc_";

    private static final String RPCPREFIX = "rpc_";

    private static final String GRIDSIZE = "gridsize";

    private int size;

    private List<File> recentlyUsedFiles = new ArrayList<File>();

    private List<File> classpathfiles = new ArrayList<File>();

    private List<ConnectionDescriptor> recentlyUsedConnections = new ArrayList<ConnectionDescriptor>();

    private Preferences preferences;

    private String dotPath;

    private int gridSize;

    private ConnectionDescriptor repositoryConnection;

    private static ApplicationPreferences me;

    public static ApplicationPreferences getInstance() {

        if (me == null) {
            try {
                me = new ApplicationPreferences(20);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return me;
    }

    protected ApplicationPreferences(int aSize) throws BackingStoreException {

        preferences = Preferences.userNodeForPackage(ApplicationPreferences.class);
        List<String> theNames = Arrays.asList(preferences.keys());
        for (String theName : theNames) {
            if (theName.startsWith(LRUPREFIX)) {
                File theFile = new File(preferences.get(theName, ""));
                if (theFile.exists()) {
                    recentlyUsedFiles.add(theFile);
                }
            }
            if (theName.startsWith(CLASSPATHPREFIX)) {
                File theFile = new File(preferences.get(theName, ""));
                if (theFile.exists()) {
                    classpathfiles.add(theFile);
                }
            }

        }

        if (theNames.contains(RPCPREFIX + "DIALECT")) {
            String theDialect = preferences.get(RPCPREFIX + "DIALECT", "");
            String theURL = preferences.get(RPCPREFIX + "URL", "");
            String theUser = preferences.get(RPCPREFIX + "USER", "");
            String theDriver = preferences.get(RPCPREFIX + "DRIVER", "");
            String thePass = preferences.get(RPCPREFIX + "PASS", "");

            repositoryConnection = new ConnectionDescriptor(null, theDialect, theURL, theUser, theDriver, thePass);
        }

        for (int i = 0; i < aSize; i++) {
            if (theNames.contains(LRCPREFIX + "DIALECT_" + i)) {
                String theAlias = preferences.get(LRCPREFIX + "ALIAS_" + i, "");
                String theDialect = preferences.get(LRCPREFIX + "DIALECT_" + i, "");
                String theURL = preferences.get(LRCPREFIX + "URL_" + i, "");
                String theUser = preferences.get(LRCPREFIX + "USER_" + i, "");
                String theDriver = preferences.get(LRCPREFIX + "DRIVER_" + i, "");
                String thePass = preferences.get(LRCPREFIX + "PASS_" + i, "");

                ConnectionDescriptor theConnection = new ConnectionDescriptor(theAlias, theDialect, theURL, theUser,
                        theDriver, thePass);
                recentlyUsedConnections.add(theConnection);
            }
        }

        size = aSize;
        gridSize = preferences.getInt(GRIDSIZE, 10);

        dotPath = preferences.get(DOT_PATH, "");

    }

    /**
     * Add a file to the recently used LRUfiles list.
     * 
     * @param aFile
     *                the file to add
     */
    public void addRecentlyUsedFile(File aFile) {

        if (!recentlyUsedFiles.contains(aFile)) {
            recentlyUsedFiles.add(aFile);
            if (recentlyUsedFiles.size() > size) {
                recentlyUsedFiles.remove(0);
            }
        } else {
            recentlyUsedFiles.remove(aFile);
            recentlyUsedFiles.add(0, aFile);
        }
    }

    /**
     * Add a last used connection to the list.
     * 
     * @param aConnection
     *                the connection
     */
    public void addRecentlyUsedConnection(ConnectionDescriptor aConnection) {
        if (!recentlyUsedConnections.contains(aConnection)) {
            recentlyUsedConnections.add(aConnection);
            if (recentlyUsedConnections.size() > size) {
                recentlyUsedConnections.remove(0);
            }
        } else {
            recentlyUsedConnections.remove(aConnection);
            recentlyUsedConnections.add(0, aConnection);
        }

    }

    public List<File> getRecentlyUsedFiles() {
        return recentlyUsedFiles;
    }

    public List<ConnectionDescriptor> getRecentlyUsedConnections() {
        return recentlyUsedConnections;
    }

    public List<File> getClasspathFiles() {
        return classpathfiles;
    }

    /**
     * @return the gridSize
     */
    public int getGridSize() {
        return gridSize;
    }

    /**
     * @param gridSize
     *                the gridSize to set
     */
    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    /**
     * Save the preferences.
     * 
     * @throws BackingStoreException
     *                 is thrown if the operation fails
     */
    public void store() throws BackingStoreException {

        String[] theNames = preferences.childrenNames();
        for (String theName : theNames) {
            if (theName.startsWith(LRUPREFIX)) {
                preferences.remove(theName);
            }
            if (theName.startsWith(CLASSPATHPREFIX)) {
                preferences.remove(theName);
            }
            if (theName.startsWith(RPCPREFIX)) {
                preferences.remove(theName);
            }
        }

        for (int i = 0; i < recentlyUsedFiles.size(); i++) {
            preferences.put(LRUPREFIX + i, recentlyUsedFiles.get(i).toString());
        }

        for (int i = 0; i < recentlyUsedConnections.size(); i++) {
            ConnectionDescriptor theConnection = recentlyUsedConnections.get(i);
            preferences.put(LRCPREFIX + "ALIAS_" + i, theConnection.getAlias());
            preferences.put(LRCPREFIX + "DIALECT_" + i, theConnection.getDialect());
            preferences.put(LRCPREFIX + "URL_" + i, theConnection.getUrl());
            preferences.put(LRCPREFIX + "USER_" + i, theConnection.getUsername());
            preferences.put(LRCPREFIX + "DRIVER_" + i, theConnection.getDriver());
            preferences.put(LRCPREFIX + "PASS_" + i, theConnection.getPassword());
        }

        for (int i = 0; i < classpathfiles.size(); i++) {
            preferences.put(CLASSPATHPREFIX + i, classpathfiles.get(i).toString());
        }

        preferences.put(DOT_PATH, dotPath);
        preferences.putInt(GRIDSIZE, gridSize);

        if (repositoryConnection != null) {
            preferences.put(RPCPREFIX + "DIALECT", repositoryConnection.getDialect());
            preferences.put(RPCPREFIX + "URL", repositoryConnection.getUrl());
            preferences.put(RPCPREFIX + "USER", repositoryConnection.getUsername());
            preferences.put(RPCPREFIX + "DRIVER", repositoryConnection.getDriver());
            preferences.put(RPCPREFIX + "PASS", repositoryConnection.getPassword());
        }

        preferences.flush();
    }

    public ClassLoader createDriverClassLoader() {

        final URL[] theUrls = new URL[classpathfiles.size()];
        for (int i = 0; i < classpathfiles.size(); i++) {
            try {
                theUrls[i] = classpathfiles.get(i).toURL();
            } catch (MalformedURLException e) {
                // This will never happen
            }
        }

        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            public ClassLoader run() {
                return new URLClassLoader(theUrls, Thread.currentThread().getContextClassLoader());
            }
        });
    }

    /**
     * @return the dotPath
     */
    public String getDotPath() {
        return dotPath;
    }

    /**
     * @param dotPath
     *                the dotPath to set
     */
    public void setDotPath(String dotPath) {
        this.dotPath = dotPath;
    }

    /**
     * @return the repositoryConnection
     */
    public ConnectionDescriptor getRepositoryConnection() {
        return repositoryConnection;
    }

    /**
     * @param repositoryConnection
     *                the repositoryConnection to set
     */
    public void setRepositoryConnection(ConnectionDescriptor repositoryConnection) {
        this.repositoryConnection = repositoryConnection;
    }
}