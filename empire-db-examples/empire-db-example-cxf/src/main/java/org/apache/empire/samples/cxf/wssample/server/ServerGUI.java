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

package org.apache.empire.samples.cxf.wssample.server;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;

public class ServerGUI extends JFrame
{

    private static final long serialVersionUID = 1L;
    private JPanel            jContentPane     = null; // @jve:decl-index=0:visual-constraint="10,10"
    private JButton           _btnShutdown     = null;
    private ServerControl     _sControl;
    private JPanel            pnlButtons       = null;
    private JButton           btnClear         = null;
    private JTextPane       txtLog      = null;
    private JScrollPane       scroller         = null;
    
    /**
     * This is the default constructor
     * @param sControl the server control
     */
    public ServerGUI(ServerControl sControl)
    {
        super();
        _sControl = sControl;
        initialize();
        initTextAreaLogger();
    }
    
    private void initTextAreaLogger()
    {
        Style def = StyleContext.getDefaultStyleContext().
        getStyle(StyleContext.DEFAULT_STYLE);
        
        StyledDocument doc = txtLog.getStyledDocument();
        
        Style inStyle = doc.addStyle("bold", def);
        StyleConstants.setBold(inStyle, true);
        
        Style outStyle = doc.addStyle("bold", def);
        StyleConstants.setItalic(outStyle, true);
        
        PrintWriter pwIN = new PrintWriter(new DocumentWriter(txtLog.getStyledDocument(),inStyle));
        PrintWriter pwOUT = new PrintWriter(new DocumentWriter(txtLog.getStyledDocument(),outStyle));
        LoggingOutInterceptor out = new LoggingOutInterceptor(pwIN);
        LoggingInInterceptor in = new LoggingInInterceptor(pwOUT);
        
        this._sControl.appendLogger(out, in);
    }

    /**
     * This method initializes this
     * 
     */
    private void initialize()
    {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(getJContentPane());
        this.setTitle("Employee Management Server Control");
        this.pack();
        this.setLocationRelativeTo(null);
        this.addWindowListener(new java.awt.event.WindowAdapter() {   
        	@Override
			public void windowClosing(java.awt.event.WindowEvent e) {    
        		_sControl.stop();
        	}
        });
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane()
    {
        if (jContentPane == null)
        {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(getPnlButtons(), BorderLayout.SOUTH);
            jContentPane.add(getScroller(), BorderLayout.CENTER);
        }
        return jContentPane;
    }

    /**
     * This method initializes _btnShutdown
     * 
     * @return javax.swing.JButton
     */
    private JButton get_btnShutdown()
    {
        if (_btnShutdown == null)
        {
            _btnShutdown = new JButton();
            _btnShutdown.setText("Shutdown!");
            _btnShutdown.setEnabled(true);
            _btnShutdown.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    _sControl.stop();
                    System.exit(0);
                }
            });
        }
        return _btnShutdown;
    }

    /**
     * This method initializes pnlButtons
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getPnlButtons()
    {
        if (pnlButtons == null)
        {
            pnlButtons = new JPanel();
            pnlButtons.setLayout(new BorderLayout());
            pnlButtons.add(get_btnShutdown(), BorderLayout.WEST);
            pnlButtons.add(getBtnClear(), BorderLayout.EAST);
        }
        return pnlButtons;
    }

    /**
     * This method initializes btnClear
     * 
     * @return javax.swing.JButton
     */
    private JButton getBtnClear()
    {
        if (btnClear == null)
        {
            btnClear = new JButton();
            btnClear.setText("clear Log");
            btnClear.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    txtLog.setText("");
                }
            });

        }
        return btnClear;
    }

    /**
     * This method initializes jEditorPane
     * 
     * @return javax.swing.JEditorPane
     */
    private JTextPane getJEditorPane()
    {
        if (txtLog == null)
        {
            txtLog = new JTextPane();
            StyledDocument doc = txtLog.getStyledDocument();
            
            Style def = StyleContext.getDefaultStyleContext().
            getStyle(StyleContext.DEFAULT_STYLE);

            Style b = doc.addStyle("bold", def);
            StyleConstants.setBold(b, true);

        }
        return txtLog;
    }

    /**
     * This method initializes scroller
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getScroller()
    {
        if (scroller == null)
        {
            scroller = new JScrollPane();
            scroller.setPreferredSize(new Dimension(500, 250));
            scroller.setViewportView(getJEditorPane());
        }
        return scroller;
    }
} // @jve:decl-index=0:visual-constraint="10,10"
