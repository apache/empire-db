/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.db.eclipse.ui;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.empire.db.codegen.util.DBUtil;
import org.apache.empire.db.eclipse.CodeGenConfig;
import org.apache.empire.db.eclipse.Plugin;
import org.apache.empire.db.eclipse.PluginConsts;
import org.apache.empire.db.eclipse.util.SWTResourceManager;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBTablesDialog extends TitleAreaDialog
{
    private CheckboxTableViewer checkboxTableViewer;
    private Button              btnConnect;

    private final CodeGenConfig config;
    private Label               lblConnection;
    private Button              btnOk;
    private Button              btnSpaceholder;

    private String              selectedTables;

    public DBTablesDialog(Shell parentShell, CodeGenConfig config)
    {
        super(parentShell);
        this.config = config;
    }

    @Override
    public void create()
    {
        super.create();
        setTitle(Plugin.getInstance().getMessageService().resolveMessageKey("dialog.tables.discription"));
    }

    @Override
    protected Point getInitialSize()
    {
        return new Point(343, 473);
    }

    // overriding this methods allows you to set the
    // title of the custom dialog
    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dialog.tables.title"));
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        parent.setLayoutData(new GridData(SWT.FILL, SWT.RIGHT, true, true));

        this.btnSpaceholder = createButton(parent, PluginConsts.BUTTON_PREVIOUS_ID, "     ", true);
        this.btnSpaceholder.setText("################");
        this.btnSpaceholder.setVisible(false);

        // Update layout of the parent composite to count the spacer
        GridLayout layout = (GridLayout) parent.getLayout();
        layout.makeColumnsEqualWidth = false;

        this.btnOk = createButton(parent, PluginConsts.BUTTON_OK,
                                  Plugin.getInstance().getMessageService().resolveMessageKey("dialog.tables.finish"), false);
        this.btnOk.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    StringBuilder sb = new StringBuilder();
                    Object[] tables = DBTablesDialog.this.checkboxTableViewer.getCheckedElements();
                    for (Object table : tables)
                    {
                        sb.append(table).append(",");
                    }
                    DBTablesDialog.this.selectedTables = sb.toString().substring(0, sb.length() - 1);
                    setReturnCode(Window.OK);
                    close();
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });
        createButton(parent, Window.CANCEL, Plugin.getInstance().getMessageService().resolveMessageKey("dialog.close"), false);

    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        setTitleImage(SWTResourceManager.getImage(ConfigurationDialog.class, "/icons/logo.png"));
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(null);

        this.btnConnect = new Button(composite, SWT.NONE);
        this.btnConnect.setBounds(5, 5, 145, 25);
        this.btnConnect.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dialog.tables.connect"));
        this.btnConnect.setImage(SWTResourceManager.getImage(ConfigurationDialog.class, "/icons/testConnection.gif"));
        this.btnConnect.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    try
                    {
                        Connection con = Plugin.getInstance().getJDBCConnection(DBTablesDialog.this.config.getJdbcType(),
                                                                                DBTablesDialog.this.config.getJdbcServer(),
                                                                                DBTablesDialog.this.config.getJdbcPort(),
                                                                                DBTablesDialog.this.config.getJdbcSID(),
                                                                                DBTablesDialog.this.config.getJdbcUser(),
                                                                                DBTablesDialog.this.config.getJdbcPwd());
                        if (con != null)
                        {
                            DBTablesDialog.this.lblConnection.setText(Plugin.getInstance().getMessageService()
                                                                            .resolveMessageKey("connectionSuccess"));
                            DBTablesDialog.this.lblConnection.setForeground(new Color(getShell().getDisplay(), 0, 255, 0));
                            DBTablesDialog.this.checkboxTableViewer.setInput(con);
                            if (DBTablesDialog.this.config.getDbTablePattern() != null)
                            {
                                String[] tablePatterns = DBTablesDialog.this.config.getDbTablePattern().split(",");
                                for (String pattern : tablePatterns)
                                {
                                    DBTablesDialog.this.checkboxTableViewer.setChecked(pattern, true);
                                }
                            }
                        }
                        con.close();
                    }
                    catch (Exception e)
                    {
                        DBTablesDialog.this.lblConnection.setText(Plugin.getInstance().getMessageService()
                                                                        .resolveMessageKey("connectionFailed"));
                        DBTablesDialog.this.lblConnection.setForeground(new Color(getShell().getDisplay(), 255, 0, 0));
                    }
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });

        this.checkboxTableViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
        final Table table = this.checkboxTableViewer.getTable();
        table.setBounds(5, 35, 290, 231);
        table.setLinesVisible(true);

        final Button btnCheckAll = new Button(composite, SWT.NONE);
        btnCheckAll.setBounds(301, 35, 26, 26);
        btnCheckAll.setImage(SWTResourceManager.getImage(ConfigurationDialog.class, "/icons/check_all.gif"));
        btnCheckAll.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    Table table = DBTablesDialog.this.checkboxTableViewer.getTable();
                    for (int i = 0; i < table.getItemCount(); i++)
                    {
                        table.getItem(i).setChecked(true);
                    }
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });

        final Button btnUncheckAll = new Button(composite, SWT.NONE);
        btnUncheckAll.setBounds(301, 67, 26, 26);
        btnUncheckAll.setImage(SWTResourceManager.getImage(ConfigurationDialog.class, "/icons/uncheck_all.gif"));
        btnUncheckAll.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    Table table = DBTablesDialog.this.checkboxTableViewer.getTable();
                    for (int i = 0; i < table.getItemCount(); i++)
                    {
                        table.getItem(i).setChecked(false);
                    }
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });

        this.lblConnection = new Label(composite, SWT.NONE);
        this.lblConnection.setBounds(156, 10, 139, 15);
        this.checkboxTableViewer.setContentProvider(new DBTablesContentProvider(this.config));
        this.checkboxTableViewer.setLabelProvider(new DBTablesLabelProvider());

        setHelpAvailable(false);
        return composite;
    }

    public String getSelectedTables()
    {
        return this.selectedTables;
    }

    public void setSelectedTables(String selectedTables)
    {
        this.selectedTables = selectedTables;
    }
}

class DBTablesContentProvider implements IStructuredContentProvider
{
    private static final Logger   log   = LoggerFactory.getLogger(DBTablesContentProvider.class);

    private static final Object[] EMPTY = new Object[] {};

    private final CodeGenConfig   config;

    public DBTablesContentProvider(CodeGenConfig config)
    {
        this.config = config;
    }

    public Object[] getElements(Object arg0)
    {
        Connection con = (Connection) arg0;
        ResultSet tables = null;
        ArrayList<String> tableNames = new ArrayList<String>();
        try
        {
            DatabaseMetaData dbMeta = con.getMetaData();
            // Get table metadata
            tables = dbMeta.getTables(this.config.getDbCatalog(), this.config.getDbSchema(), "%", new String[] { "TABLE", "VIEW" });
            // Add all tables and views
            while (tables.next())
            {
                String tableName = tables.getString("TABLE_NAME");
                String tableType = tables.getString("TABLE_TYPE");
                // Ignore system tables containing a '$' symbol (required for
                // Oracle!)
                if (tableName.indexOf('$') >= 0)
                {
                    DBTablesContentProvider.log.info("Ignoring system table " + tableName);
                    continue;
                }
                DBTablesContentProvider.log.info(tableType + ": " + tableName);
                tableNames.add(tableName);
            }
            return tableNames.toArray();
        }
        catch (SQLException e)
        {
            DBTablesContentProvider.log.info("Error loading meta data: " + e.getMessage());
        }
        finally
        {
            DBUtil.close(tables, DBTablesContentProvider.log);
        }
        return DBTablesContentProvider.EMPTY;
    }

    public void dispose()
    {
        // Nothing to dispose
    }

    public void inputChanged(Viewer arg0, Object arg1, Object arg2)
    {
        // Nothing to do
    }
}

class DBTablesLabelProvider implements ILabelProvider
{
    public Image getImage(Object arg0)
    {
        return null;
    }

    /**
     * Returns the name of the file
     * 
     * @param arg0
     *            the name of the file
     * @return String
     */
    public String getText(Object arg0)
    {
        return (String) arg0;
    }

    public void addListener(ILabelProviderListener arg0)
    {
        // Throw it away
    }

    public void dispose()
    {
        // Nothing to dispose
    }

    public boolean isLabelProperty(Object arg0, String arg1)
    {
        return false;
    }

    public void removeListener(ILabelProviderListener arg0)
    {
        // Ignore
    }
}
