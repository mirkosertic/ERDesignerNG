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
package de.erdesignerng.model;

import java.util.Vector;

/**
 * @author $Author: mirkosertic $
 * @version $Date: 2008-01-17 19:34:29 $
 * @param <T>
 *            the type
 */
public class ModelItemVector<T extends ModelItem> extends Vector<T> {

    private static final long serialVersionUID = 5030067810497396582L;

    /**
     * Find an entry by a given system id.
     * 
     * @param aSystemId
     *            the system id
     * @return the found element
     */
    public T findBySystemId(String aSystemId) {
        for (T theItem : this) {
            if (aSystemId.equals(theItem.getSystemId())) {
                return theItem;
            }
        }
        return null;
    }

    /**
     * Check if a named element already exists in this list.
     * 
     * @param aName
     *            the name of the element
     * @param aCaseSensitive
     *            true if checking is case sensitive, else false
     * 
     * @return true if it exists, else false.
     */
    public boolean elementExists(String aName, boolean aCaseSensitive) {
        for (T theElement : this) {
            if (aCaseSensitive) {
                if (aName.equals(theElement.getName())) {
                    return true;
                }
            } else {
                if (aName.toLowerCase().equals(theElement.getName().toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find an attribute by a given name.
     * 
     * @param aName
     *            the name
     * @return the found element
     */
    public T findByName(String aName) {
        for (T theElement : this) {
            if (aName.equals(theElement.getName())) {
                return theElement;
            }
        }
        return null;
    }

    /**
     * Delete an attribute by a given name.
     * 
     * @param aName
     *            the name
     */
    public void removeByName(String aName) {
        remove(findByName(aName));
    }

    /**
     * Delete an attribute by a given id.
     * 
     * @param aSystemId
     *            the id
     */
    public void removeById(String aSystemId) {
        remove(findBySystemId(aSystemId));
    }
}
