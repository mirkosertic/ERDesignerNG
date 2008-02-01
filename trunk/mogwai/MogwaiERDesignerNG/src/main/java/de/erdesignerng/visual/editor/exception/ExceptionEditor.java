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
package de.erdesignerng.visual.editor.exception;

import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;

import de.erdesignerng.ERDesignerBundle;
import de.erdesignerng.visual.editor.BaseEditor;
import de.erdesignerng.visual.editor.DialogConstants;
import de.mogwai.common.client.looks.UIInitializer;

/**
 * Editor for exceptions.
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2008-02-01 17:20:26 $
 */
public class ExceptionEditor extends BaseEditor {

    private ExceptionEditorView view = new ExceptionEditorView();

    public ExceptionEditor(Component aParent, Exception aException) {
        super(aParent, ERDesignerBundle.CLASSPATHCONFIGURATION);

        initialize();

        StringWriter theWriter = new StringWriter();
        PrintWriter thePrintWriter = new PrintWriter(theWriter);
        aException.printStackTrace(thePrintWriter);
        thePrintWriter.flush();
        
        view.getExceptionText().setText(theWriter.toString());
    }

    private void initialize() {

        setContentPane(view);
        setResizable(false);
        pack();

        UIInitializer.getInstance().initialize(this);
    }

    @Override
    public void applyValues() throws Exception {
    }

    private void commandClose() {
        setModalResult(DialogConstants.MODAL_RESULT_OK);
    }
}