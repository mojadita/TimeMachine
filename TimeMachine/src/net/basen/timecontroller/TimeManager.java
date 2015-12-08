/**
 * $Id: $ Copyright (C) 2015 BaseN. All rights reserved.
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
import java.util.Date;
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

/**
 * TimeManager widget is a full window widget that implements the time managing
 * feature.
 * 
 * @author Luis Colorado <code>&lt;lcu@basen.net&gt;</code>
 */
public class TimeManager
    extends JFrame
    implements WindowListener {

    private static final long   serialVersionUID  = -4888907548573283461L;

    private static final String version           = "TimeManager v1.4";

    private static final File   f                 =
                                                      new File( System.getProperty( "user.home" ),
                                                                ".timemanager.properties" );

    private static final String CREATE            = ": CREATE ";

    private static final String TOTAL             = ": TOTAL  ";

    private static final String END               = ": END    ";

    private static final String START             = ": START  ";

    private static final String STOP              = ": STOP   ";

    private static final String RESET             = ": RESET  ";

    private static final String DELETE            = ": DELETE ";

    private static final String BEGIN             = ": BEGIN  ";

    private Task                updatingTask;

    private Map<String, Task>   tasksMap          = new TreeMap<String, Task>();

    private JMenu               menuSelect;

    private JMenu               menuDelete;

    private JList<Task>         list;

    private TaskListModel       listModel         = new TaskListModel();

    private JLabel              totalTime;

    private JCheckBoxMenuItem   alwaysOnTop;

    /*
     * ******************************************************************
     * NOW, THE ACTIONS:
     */
    private final Action        toggleAlwaysOnTop =
                                                      new AbstractAction( "Always on top" ) {

                                                          private static final long serialVersionUID =
                                                                                                         1167217382426314444L;

                                                          @Override
                                                          public void actionPerformed( ActionEvent e ) {
                                                              boolean value =
                                                                  alwaysOnTop.getState();
                                                              setAlwaysOnTop( value );
                                                          }
                                                      };

    private final Action        addNewTask        =
                                                      new AbstractAction( "Add new task..." ) {

                                                          private static final long serialVersionUID =
                                                                                                         -3170654095467188869L;

                                                          @Override
                                                          public void actionPerformed( ActionEvent e ) {
                                                              String name =
                                                                  JOptionPane.showInputDialog( TimeManager.this,
                                                                                               "Enter name of task:",
                                                                                               "Task #"
                                                                                                   + e.getWhen() );
                                                              if( name != null
                                                                  && tasksMap.get( name ) == null ) {
                                                                  newTask( name,
                                                                           e.getWhen() );
                                                              }
                                                          }
                                                      };

    private final Action        resetSelected     =
                                                      new AbstractAction( "Reset seleted" ) {

                                                          private static final long serialVersionUID =
                                                                                                         8344533774239738481L;

                                                          @Override
                                                          public void actionPerformed( ActionEvent e ) {
                                                              int ok =
                                                                  JOptionPane.showConfirmDialog( TimeManager.this,
                                                                                                 "Are you sure?" );
                                                              switch( ok ) {
                                                              case JOptionPane.YES_OPTION:
                                                                  for( int i =
                                                                      0; i < listModel.getSize(); i++ ) {
                                                                      if( list.isSelectedIndex( i ) ) {
                                                                          Task t =
                                                                              listModel.getElementAt( i );
                                                                          t.reset( e.getWhen() );
                                                                          listModel.update( t );
                                                                      }
                                                                  }
                                                              }

                                                          }
                                                      };

    private final Action        deleteSelected    =
                                                      new AbstractAction( "Delete seleted" ) {

                                                          private static final long serialVersionUID =
                                                                                                         -6555684243738386825L;

                                                          @Override
                                                          public void actionPerformed( ActionEvent e ) {
                                                              int ok =
                                                                  JOptionPane.showConfirmDialog( TimeManager.this,
                                                                                                 "Are you sure?" );
                                                              switch( ok ) {
                                                              case JOptionPane.YES_OPTION:
                                                                  for( int i =
                                                                      0; i < listModel.getSize(); i++ ) {
                                                                      if( list.isSelectedIndex( i ) ) {
                                                                          Task t =
                                                                              listModel.getElementAt( i );
                                                                          if( updatingTask == t )
                                                                              t.stop( e.getWhen() );
                                                                          t.delete( e.getWhen() );
                                                                          i--;
                                                                          // so we get to the right
                                                                          // index in the next
                                                                          // iteration.
                                                                      }
                                                                  }
                                                              }
                                                          }
                                                      };

    private final Action        quitAction        =
                                                      new AbstractAction( "Quit" ) {

                                                          private static final long serialVersionUID =
                                                                                                         300324797016514635L;

                                                          @Override
                                                          public void actionPerformed( ActionEvent e ) {
                                                              if( updatingTask != null ) {
                                                                  updatingTask.stop( e.getWhen() );
                                                              }
                                                              for( Task n: tasksMap.values() ) {
                                                                  System.out.println( ""
                                                                      + e.getWhen()
                                                                      + TOTAL
                                                                      + n );
                                                              }
                                                              save();
                                                              System.out.println( ""
                                                                  + e.getWhen()
                                                                  + END
                                                                  + new Date( e.getWhen() ) );
                                                              System.exit( 0 );
                                                          }
                                                      };

    private final Action        stopAction        =
                                                      new AbstractAction( "Stop" ) {

                                                          private static final long serialVersionUID =
                                                                                                         -4605298344933918339L;

                                                          {
                                                              setEnabled( false );
                                                          }

                                                          @Override
                                                          public void actionPerformed( ActionEvent e ) {
                                                              if( updatingTask != null ) {
                                                                  updatingTask.stop( e.getWhen() );
                                                              }
                                                          }
                                                      };

    /*
     * ***************************************************************
     * SUBCLASSES:
     */
    private class TaskListModel
        extends AbstractListModel<Task> {

        private static final long serialVersionUID = 2354503374712648030L;

        ArrayList<Task>           values           = new ArrayList<Task>();

        @Override
        public int getSize() {
            return values.size();
        }

        @Override
        public Task getElementAt( int index ) {
            return values.get( index );
        }

        void add( Task t ) {
            fireIntervalAdded( this, getSize(), getSize() );
            values.add( t );
        }

        void remove( Task t ) {
            int ix = values.indexOf( t );
            fireIntervalRemoved( this, ix, ix );
            values.remove( t );
        }

        void update( Task t ) {
            int ix = values.indexOf( t );
            fireContentsChanged( this, ix, ix );
        }

    }

    /**
     * This class represents any of the tasks to measure time on.
     * 
     * @author Luis Colorado <code>&lt;lcu@basen.net&gt;</code>
     */
    public class Task
        implements Serializable {

        private static final long serialVersionUID = 6941666755400993443L;

        private String            name;

        private long              accum            = 0, lapFrom;

        private JMenuItem         menuItemSelect;

        private JMenuItem         menuItemDelete;

        private AbstractAction    select;

        private AbstractAction    delete;

        /**
         * Convenience constructor for having no initialization data.
         * 
         * @param name
         *            name of task.
         * @param ts
         *            timestamp of creation time. Normally this comes from the
         *            event that triggered this object creation.
         */
        public Task( final String name, long ts ) {
            this( name, ts, 0 );
        }

        /**
         * Normal constructor for creating the object from stored data.
         * 
         * @param name
         *            name of task.
         * @param ts
         *            timestamp of creation time. Normally this comes from the
         *            event that triggered this object creation.
         * @param accum
         *            already accumulated data for this object from secondary
         *            storage.
         */
        public Task( final String name, long ts, long accum ) {
            this.name = name;
            this.select = new AbstractAction( name ) {

                private static final long serialVersionUID =
                                                               -9041317284352500581L;

                @Override
                public void actionPerformed( ActionEvent e ) {
                    String comment =
                            JOptionPane.showInputDialog( TimeManager.this,
                                    "Enter comment:" );
                    if( comment != null ) {
                        if( updatingTask != null )
                            updatingTask.stop( e.getWhen() );
                        if (!comment.equals( "" ))
                            System.out.println("# " + comment);
                        start( e.getWhen() );
                    }
                }
            };
            this.delete = new AbstractAction( name ) {

                private static final long serialVersionUID =
                                                               -1845149367713429875L;

                @Override
                public void actionPerformed( ActionEvent e ) {
                    if( updatingTask == Task.this )
                        stop( e.getWhen() );
                    delete( e.getWhen() );
                }
            };
            this.accum = accum;
            menuSelect.add( menuItemSelect = new JMenuItem( this.select ) );
            menuDelete.add( menuItemDelete = new JMenuItem( this.delete ) );
            tasksMap.put( name, this );
            listModel.add( this );
            System.out.println( "" + ts + CREATE + this );
        }

        /**
         * Method to start measuring this task.
         * 
         * @param ts
         *            timestamp of this event.
         */
        public void start( long ts ) {
            synchronized( TimeManager.this ) {
                updatingTask = this;
                select.setEnabled( false );
                updatingTask.lapFrom = ts;
                stopAction.setEnabled( true );
                listModel.update( this );
                System.out.println( "" + ts + START + this );
            }
        }

        /**
         * Method to stop measuring this task.
         * 
         * @param ts
         *            timestamp triggering this event.
         */
        public void stop( long ts ) {
            synchronized( TimeManager.this ) {
                System.out.println( "" + ts + STOP + this );
                updatingTask.accum += ts - updatingTask.lapFrom;
                updatingTask.select.setEnabled( true );
                listModel.update( this );
                updatingTask = null;
                stopAction.setEnabled( false );
            }
        }

        /**
         * Method to reset this task measurements.
         * 
         * @param ts
         *            timestamp of the event that triggered this event.
         */
        public void reset( long ts ) {
            synchronized( TimeManager.this ) {
                System.out.println( "" + ts + RESET + this );
                accum = 0;
                lapFrom = ts;
            }
        }

        /**
         * Method to delete this task.
         * 
         * @param ts
         *            is the timestamp of the event that triggered this event.
         *            Actually it is only used to output some event data to the
         *            standard output.
         */
        public void delete( long ts ) {
            synchronized( TimeManager.this ) {
                tasksMap.remove( getName() );
                menuSelect.remove( menuItemSelect );
                menuDelete.remove( menuItemDelete );
                listModel.remove( this );
                System.out.println( "" + ts + DELETE + this );
            }
        }

        /**
         * Method to get the <code>name</code> attribute.
         * 
         * @return a {@link String} with the name of this <code>Task</code>
         */
        public String getName() {
            return name;
        }

        /**
         * Method to get the actual time measurement of this timer.
         * 
         * @return the value in milliseconds accumulated on this
         *         <code>Task</code> timer.
         */
        public long getValue() {
            long res = accum;
            if( this == updatingTask ) {
                long now = System.currentTimeMillis();
                res += now - lapFrom;
            }
            return res;
        }

        /**
         * This method overrides the {@link Object}.{@link Object#toString()}
         * method. It generates the accumulated time in square brackets,
         * followed by the <code>Task</code>'s name.
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return String.format( "[%s]:%s",
                                  TimeUnits.msecToString( getValue() ),
                                  name );
        }
    }

    /**
     * Method to create a new {@link Task} and allow to make measurements on it.
     * 
     * @param name
     *            name of this task, it must not exist previously or you'll get
     *            the registered one.
     * @param ts
     *            timestamp for use in case we need to create a new {@link Task}
     *            .
     * @return the {@link Task} created or searched for in the database.
     */
    public Task newTask( String name, long ts ) {
        return newTask( name, ts, 0 );
    }

    /**
     * Method to create and initialize with accumulated time a task.
     * 
     * @param name
     *            name of the {@link Task}.
     * @param ts
     *            timestamp that triggered this action.
     * @param ini
     *            initial value for the accumulated time.
     * @return the {@link Task} created or searche for in the database.
     */
    public Task newTask( String name, long ts, long ini ) {
        Task res = tasksMap.get( name );
        if( res == null ) {
            res = new Task( name, ts, ini );
        }
        return res;
    }

    /**
     * Method to update things. This is called from the main thread to update
     * the title bar text and the label showing the accumulated time at the
     * right of the main menubar.
     */
    public synchronized void update() {
        if( updatingTask != null ) {
            listModel.update( updatingTask );
        }

        long total = 0;
        for( int i = 0; i < listModel.getSize(); i++ ) {
            if( list.isSelectedIndex( i ) ) {
                total += listModel.getElementAt( i ).getValue();
            }
        }
        totalTime.setText( String.format( "TOTAL: %s",
                                          TimeUnits.msecToString( total ) ) );
        setTitle( version + " - " + TimeUnits.msecToString( total ) );
    }

    /**
     * Constructor for the TimeManager widget.
     * 
     * @param title
     *            Title for the main window.
     * @param ts
     *            timestamp of creation time.
     */
    public TimeManager( String title, long ts ) {
        JMenuBar menu;
        JMenu file, ops;
        menu = new JMenuBar();
        menu.add( file = new JMenu( "File" ) );
        file.add( alwaysOnTop = new JCheckBoxMenuItem( toggleAlwaysOnTop ) );
        file.addSeparator();
        file.add( new JMenuItem( quitAction ) );
        menu.add( menuSelect = new JMenu( "Tasks" ) );
        menuSelect.add( stopAction );
        menuSelect.addSeparator();
        menu.add( ops = new JMenu( "Ops..." ) );
        ops.add( new JMenuItem( addNewTask ) );
        ops.add( new JMenuItem( resetSelected ) );
        ops.add( new JMenuItem( deleteSelected ) );
        ops.add( menuDelete = new JMenu( "Delete task" ) );
        menu.add( totalTime = new JLabel( "TOTAL" ) );
        setJMenuBar( menu );
        add( new JScrollPane( list = new JList<Task>( listModel ) ) );
        addWindowListener( this );
        load( ts );
    }

    private String nameString( int i ) {
        return "task." + i + ".name";
    }

    private String valueString( int i ) {
        return "task." + i + ".value";
    }

    /**
     * Method to load stored data from the {@link TimeManager#f} file.
     * 
     * @param ts
     *            timestamp that triggered this action.
     */
    public void load( long ts ) {
        Properties props = new Properties();
        try {
            props.load( new BufferedInputStream( new FileInputStream( f ) ) );
        } catch( FileNotFoundException e ) {
            // nothing, file doesn't exist.
            System.err.println( f + " does not exist, will be written on exit." );
        } catch( Exception e ) {
            System.err.println( "File: " + f + ": " + e );
            System.exit( 1 );
        }
        int tn = Integer.parseInt( props.getProperty( "tasks.num", "0" ) );
        for( int i = 0; i < tn; i++ ) {
            String name = props.getProperty( nameString( i ), "task#" + i );
            String val = props.getProperty( valueString( i ) );
            if( val == null ) {
                newTask( name, ts );
            } else {
                newTask( name, ts, Long.parseLong( val ) );
            }
        }
    }

    /**
     * Method to save data on {@link TimeManager#f} file.
     */
    public void save() {
        Properties props = new Properties();
        int i = 0;
        for( Task t: tasksMap.values() ) {
            props.setProperty( nameString( i ), t.getName() );
            props.setProperty( valueString( i ),
                               new Long( t.getValue() ).toString() );
            i++;
        }
        props.setProperty( "tasks.num", new Integer( i ).toString() );
        try {
            props.store( new BufferedOutputStream( new FileOutputStream( f ) ),
                         version );
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Main entry point to the program.
     * 
     * @param args
     *            not used.
     */
    public static void main( String[] args ) {

        long starttime = System.currentTimeMillis();
        System.out.println( "" + starttime + BEGIN + new Date( starttime ) );

        TimeManager tm = new TimeManager( version, starttime );

        tm.setDefaultCloseOperation( EXIT_ON_CLOSE );
        tm.pack();
        tm.setVisible( true );

        try {
            while( true ) {
                Thread.sleep( 100 );
                tm.update();
            }
        } catch( InterruptedException e ) {
            System.out.println( "main method interrupted, finishing: " + e );
            tm.quitAction.actionPerformed( new ActionEvent( null,
                                                            ActionEvent.ACTION_PERFORMED,
                                                            "main interrupted",
                                                            System.currentTimeMillis(),
                                                            0 ) );
        }

    }

    /*
     * ************************************************************
     * WINDOW OPERATIONS:
     */

    /**
     * Empty implementation to comply with {@link WindowListener} interface.
     * 
     * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
     */
    @Override
    public void windowOpened( WindowEvent e ) {
        // empty
    }

    /**
     * This method implements the {@link WindowListener} interface
     * {@link WindowListener#windowClosing(WindowEvent)}. It installs a listener
     * that saves data and closes properly the application.
     * 
     * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
     */
    @Override
    public void windowClosing( WindowEvent e ) {
        quitAction.actionPerformed( new ActionEvent( this,
                                                     ActionEvent.ACTION_PERFORMED,
                                                     "window Close",
                                                     System.currentTimeMillis(),
                                                     0 ) );
    }

    /**
     * Empty implementation of the windowClosed event.
     * 
     * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
     */
    @Override
    public void windowClosed( WindowEvent e ) {
        // empty
    }

    /**
     * Empty implementation of the windowIconified event.
     * 
     * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
     */
    @Override
    public void windowIconified( WindowEvent e ) {
        // empty
    }

    /**
     * Empty implementation of the windowDeiconified event.
     * 
     * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
     */
    @Override
    public void windowDeiconified( WindowEvent e ) {
        // empty
    }

    /**
     * Empty implementation of the windowActivated event.
     * 
     * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
     */
    @Override
    public void windowActivated( WindowEvent e ) {
        // empty
    }

    /**
     * Empty implementation of the windowDeactivated event.
     * 
     * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
     */
    @Override
    public void windowDeactivated( WindowEvent e ) {
        // empty
    }
}
