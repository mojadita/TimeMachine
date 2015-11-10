/**
 * $Id: $
 *
 * Copyright (C) 2015 BaseN.
 *
 * All rights reserved.
 */
package net.basen.timecontroller;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

public class TimeManager extends JFrame implements WindowListener {

    private static final String version = "TimeManager v1.1";
    private static final long serialVersionUID = -4888907548573283461L;
    private static final File f = new File(System.getProperty( "user.home" ), ".timemanager");

    Task                    updatingTask;
    Map<String, Task>       tasks           = new TreeMap<String, Task>();
    JMenu                   menuSelect;
    JMenu                   menuDelete;
    JList<Task>             list;
    TaskListModel           listModel       = new TaskListModel();
    JLabel                  totalTime;
    JCheckBoxMenuItem       alwaysOnTop;
            
    private final Action toggleAlwaysOnTop = new AbstractAction( "Always on top" ) {
        private static final long serialVersionUID = 1167217382426314444L;
        @Override
        public void actionPerformed( ActionEvent e ) {
            boolean value = alwaysOnTop.getState();
            setAlwaysOnTop( value );
        }
    };

    private final Action addAction = new AbstractAction( "Add new task..." ) {
        private static final long serialVersionUID = -3170654095467188869L;
        @Override
        public void actionPerformed( ActionEvent e ) {
            String name = JOptionPane.showInputDialog( TimeManager.this, "Enter name of task:" );
            if (name != null && tasks.get( name ) == null) {
                Task t = new Task(name);
                tasks.put( name, t );
            }
        }
    };
    
    private final Action resetSelected = new AbstractAction( "Reset seleted" ) {
        private static final long serialVersionUID = 8344533774239738481L;
        @Override
        public void actionPerformed( ActionEvent e ) {
            for (int i = 0; i < listModel.getSize(); i++) {
                if (list.isSelectedIndex( i )) {
                    Task t = listModel.getElementAt( i );
                    t.accum = 0;
                    t.lapFrom = e.getWhen();
                }
            }
            
        }
    };

    /* TODO: This method has been just copied from resetSelected(), and it must be
     * rewritten to delete all selected Tasks.
     */
    @SuppressWarnings( "unused" )
    private final Action deleteSelected = new AbstractAction( "Delete seleted" ) {
        private static final long serialVersionUID = -6555684243738386825L;
        @Override
        public void actionPerformed( ActionEvent e ) {
            for (int i = 0; i < listModel.getSize(); i++) {
                if (list.isSelectedIndex( i )) {
                    Task t = listModel.getElementAt( i );
                    t.accum = 0;
                    t.lapFrom = e.getWhen();
                }
            }
            
        }
    };

    private final Action quitAction = new AbstractAction("Quit"){
        private static final long serialVersionUID = 300324797016514635L;
        @Override
        public void actionPerformed( ActionEvent e ) {
            if (updatingTask != null) {
                updatingTask.finish( e.getWhen() );
            }
            for (Task n: tasks.values()) {
                System.out.println("" + e.getWhen() + ": Total " + n);
            }
            save();
            System.exit(0);
        }
    };

    private class TaskListModel extends AbstractListModel<Task> {
        
        private static final long serialVersionUID = 2354503374712648030L;
        ArrayList<Task> values = new ArrayList<Task> ();

        @Override
        public int getSize() {
            return values.size();
        }

        @Override
        public Task getElementAt( int index ) {
            return values.get( index );
        }
        
        void add(Task t) {
            fireIntervalAdded( this, getSize(), getSize() );
            values.add( t );
        }
        
        void remove(Task t) {
            int ix = values.indexOf( t );
            fireIntervalRemoved( this, ix, ix );
            values.remove( t );
        }
        
        void update(Task t) {
            int ix = values.indexOf( t );
            fireContentsChanged( this, ix, ix );
        }
        
    }

    public class Task implements Serializable {

        private static final long serialVersionUID = 6941666755400993443L;
        private String          name;
        private long            accum = 0, 
                                lapFrom;
        private JMenuItem       menuItemSelect;
        private JMenuItem       menuItemDelete;
        private AbstractAction  select;
        private AbstractAction  delete;

        public Task( String name ) {
            this.name = name;
            select = new AbstractAction(name) {
                private static final long serialVersionUID =
                        -9041317284352500581L;

                @Override
                public void actionPerformed( ActionEvent e ) {
                    if (updatingTask != null) {
                        updatingTask.finish( e.getWhen() );
                    }
                    start( e.getWhen() );
                }
            };
            delete = new AbstractAction(name) {

                private static final long serialVersionUID =
                        -1845149367713429875L;

                @Override
                public void actionPerformed( ActionEvent e ) {
                    if (updatingTask == Task.this) {
                        updatingTask = null;
                    }
                    menuDelete.remove( menuItemDelete );
                    menuSelect.remove( menuItemSelect );
                    listModel.remove( Task.this );
                    tasks.remove( name );
                }
            };
            menuSelect.add( menuItemSelect = new JMenuItem( this.select ) );
            menuDelete.add( menuItemDelete = new JMenuItem( this.delete ) );
            tasks.put( name, this );                
            listModel.add(this);
        }
        
        public void finish(long ts) {
            synchronized (TimeManager.this) {
                System.out.println( "" + ts + ": Finishing " + this );
                updatingTask.accum += ts - updatingTask.lapFrom;
                updatingTask.select.setEnabled( true );
                listModel.update( this );
                updatingTask = null;
            }
        }
        
        public void start(long ts) {
            synchronized (TimeManager.this) {
                updatingTask = this;
                select.setEnabled( false );
                updatingTask.lapFrom = ts;
                listModel.update( this );
                System.out.println( "" + ts + ": Beginning " + this );
            }
        }
        
        public String getName() {
            return name;
        }
        
        public long getValue() {
            long res = accum;
            if (this == updatingTask) {
                long now = System.currentTimeMillis();
                res += now - lapFrom;
            }
            return res;
        }
        
        @Override
        public String toString() {
            return String.format("[%s]:%s", TimeUnits.msecToString(getValue()), name);
        }
    }

    public Task newTask(String name) {
        Task result = tasks.get( name );
        if (result == null) {
            result = new Task(name);
        }
        return result;
    }
    
    public Task newTask(String name, long ini) {
        Task result = newTask(name);
        result.accum = ini;
        return result;
    }
    
    public synchronized void update() {
        if (updatingTask != null) {
            listModel.update( updatingTask );
        }
        long total = 0;
        for (int i = 0; i < listModel.getSize(); i++) {
            if (list.isSelectedIndex( i )) {
                total += listModel.getElementAt( i ).getValue();
            }
        }
        totalTime.setText( String.format( "TOTAL: %s", TimeUnits.msecToString( total ) ) );
        setTitle( version + " - " + TimeUnits.msecToString( total ) );
    }

    public TimeManager( String title ) {
        JMenuBar menu; 
        JMenu file, ops;
        menu = new JMenuBar();
        menu.add(  file = new JMenu( "File" ) );
        file.add(alwaysOnTop = new JCheckBoxMenuItem( toggleAlwaysOnTop ));
        file.addSeparator();
        file.add( new JMenuItem( quitAction ) );
        menu.add( menuSelect = new JMenu( "Tasks" ) );
        menu.add( ops = new JMenu( "Ops..." ) );
        ops.add( new JMenuItem( addAction ) );
        ops.add( new JMenuItem( resetSelected ) );
        ops.add( menuDelete = new JMenu( "Delete task" ) );
        menu.add( totalTime = new JLabel( "TOTAL" ) );
        setJMenuBar( menu );
        add( new JScrollPane( list = new JList<Task>(listModel) ) );
        addWindowListener( this );
        load();
    }
    
    public void load() {
        Properties props = new Properties();
        try {
            props.load( new BufferedInputStream( new FileInputStream( f ) ));
        } catch (FileNotFoundException e) {
            // nothing, file doesn't exist.
            System.err.println(f + " does not exist, will be written on exit.");
        } catch (Exception e) {
            System.err.println("File: " + f + ": " + e);
            System.exit( 1 );
        }
        int tn = Integer.parseInt( props.getProperty( "tasks.num", "0" ));
        for (int i = 0; i < tn; i++) {
            String name = props.getProperty( nameString(i), "task#" + i );
            String val = props.getProperty( valueString( i ) );
            if (val == null) {
                newTask(name);
            } else {
                newTask(name, Long.parseLong( val ));
            }
        }
    }
    
    private String nameString(int i) {
        return "task." + i + ".name";
    }
    
    private String valueString(int i) {
        return "task." + i + ".value";
    }
    
    public void save() {
        Properties props = new Properties();
        int i = 0;
        for (Task t: tasks.values()) {
            props.setProperty( nameString( i ), t.getName() );
            props.setProperty( valueString( i ), new Long(t.getValue()).toString() );
            i++;
        }
        props.setProperty( "tasks.num", new Integer(i).toString());
        try {
            props.store( new BufferedOutputStream( new FileOutputStream( f ) ), version );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        TimeManager tm = new TimeManager( version );

        tm.setDefaultCloseOperation( EXIT_ON_CLOSE );
        tm.pack();
        tm.setVisible( true );
        
        try {
            while (true) {
                Thread.sleep( 100 );
                tm.update();
            }
        } catch( InterruptedException e ) {
            System.out.println( "main method interrupted, finishing: " + e);
            tm.quitAction.actionPerformed( 
                    new ActionEvent(null, 
                                    ActionEvent.ACTION_PERFORMED, 
                                    "main interrupted",
                                    System.currentTimeMillis(), 
                                    0) );
        }
        
    }

    @Override
    public void windowOpened( WindowEvent e ) {
        // empty
    }

    @Override
    public void windowClosing( WindowEvent e ) {
        quitAction.actionPerformed( 
                    new ActionEvent(this, 
                                    ActionEvent.ACTION_PERFORMED, 
                                    "window Close",
                                    System.currentTimeMillis(),
                                    0) );
    }

    @Override
    public void windowClosed( WindowEvent e ) {
        // empty
    }

    @Override
    public void windowIconified( WindowEvent e ) {
        // empty
    }

    @Override
    public void windowDeiconified( WindowEvent e ) {
        // empty
    }

    @Override
    public void windowActivated( WindowEvent e ) {
        // empty
    }

    @Override
    public void windowDeactivated( WindowEvent e ) {
        // empty
    }
}
