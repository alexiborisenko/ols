/*
 * OpenBench LogicSniffer / SUMP project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * 
 * Copyright (C) 2010-2011 - J.W. Janssen, http://www.lxtreme.nl
 */
package nl.lxtreme.ols.client;


import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.event.*;

import nl.lxtreme.ols.api.*;
import nl.lxtreme.ols.client.about.*;
import nl.lxtreme.ols.client.action.*;
import nl.lxtreme.ols.client.data.project.*;
import nl.lxtreme.ols.client.diagram.*;
import nl.lxtreme.ols.client.icons.*;
import nl.lxtreme.ols.util.*;
import nl.lxtreme.ols.util.swing.*;
import nl.lxtreme.ols.util.swing.StandardActionFactory.CloseAction.Closeable;
import nl.lxtreme.ols.util.swing.component.*;


/**
 * Denotes the main UI.
 */
public final class MainFrame extends JFrame implements Closeable, PropertyChangeListener, Configurable
{
  // INNER TYPES

  /**
   * Provides an adapter class for {@link MenuListener} allowing a menu to be
   * recreated each time it is selected.
   */
  static abstract class AbstractMenuBuilder implements MenuListener
  {
    // CONSTANTS

    private static final Logger LOG = Logger.getLogger( AbstractMenuBuilder.class.getName() );

    // VARIABLES

    protected final ClientController controller;

    private final ButtonGroup group = new ButtonGroup();

    // CONSTRUCTORS

    /**
     * Creates a new MainFrame.AbstractMenuBuilder instance.
     */
    public AbstractMenuBuilder( final ClientController aController )
    {
      this.controller = aController;
    }

    // METHODS

    /**
     * {@inheritDoc}
     */
    @Override
    public void menuCanceled( final MenuEvent aEvent )
    {
      // No-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void menuDeselected( final MenuEvent aEvent )
    {
      // No-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void menuSelected( final MenuEvent aEvent )
    {
      // Build the menu dynamically...
      final JMenu menu = ( JMenu )aEvent.getSource();

      String[] names = getMenuItemNames();
      if ( names.length == 0 )
      {
        for ( int i = 0, size = menu.getItemCount(); i < size; i++ )
        {
          final JMenuItem item = menu.getItem( i );
          if ( item instanceof AbstractButton )
          {
            this.group.remove( item );
          }
          menu.remove( item );
        }

        JMenuItem noDevicesItem = new JMenuItem( getNoItemsName() );
        noDevicesItem.setEnabled( false );

        menu.add( noDevicesItem );
      }
      else
      {
        names = removeObsoleteMenuItems( menu, names );
        for ( String name : names )
        {
          try
          {
            final JMenuItem menuItem = createMenuItem( name );

            this.group.add( menuItem );
            menu.add( menuItem );
          }
          catch ( Exception exception )
          {
            LOG.log( Level.FINE, "Exception thrown while creating menu item!", exception );
          }
        }
      }

      // Make sure the action reflect the current situation...
      this.controller.updateActionsOnEDT();
    }

    /**
     * Factory method for creating a menu item for the given name.
     * 
     * @param aName
     *          the name of the menu item, never <code>null</code>.
     * @return a new menu item instance, never <code>null</code>.
     */
    protected abstract JMenuItem createMenuItem( String aName );

    /**
     * Returns all names of menu items.
     * 
     * @return an array of menu item names, never <code>null</code>.
     */
    protected abstract String[] getMenuItemNames();

    /**
     * Returns the name to display in case no other menu items are available.
     * 
     * @return a 'no items' menu item name, never <code>null</code>.
     */
    protected abstract String getNoItemsName();

    /**
     * Returns whether or not the given menu item is "persistent", i.e., it
     * should not be removed automagically from the menu.
     * 
     * @param aMenuItem
     *          the menu item to test, cannot be <code>null</code>.
     * @return <code>true</code> if the menu item is persistent,
     *         <code>false</code> otherwise.
     */
    private boolean isPersistentMenuItem( final JMenuItem aMenuItem )
    {
      final Object isPersistent = aMenuItem.getClientProperty( PERSISTENT_MENU_ITEM_KEY );
      return Boolean.TRUE.equals( isPersistent );
    }

    /**
     * Removes all obsolete menu items from the given menu, meaning that all
     * items that are not persistent and are not contained in the given list of
     * menu items are removed.
     * 
     * @param aMenu
     *          the menu to remove the obsolete items from;
     * @param aMenuItems
     *          the menu items that should either remain or be added to the
     *          menu.
     * @return an array of menu items that are to be added to the given menu.
     */
    private String[] removeObsoleteMenuItems( final JMenu aMenu, final String[] aMenuItems )
    {
      List<String> result = new ArrayList<String>( Arrays.asList( aMenuItems ) );
      // Remove all obsolete menu items from the menu...
      for ( int i = aMenu.getItemCount() - 1; i >= 0; i-- )
      {
        final JMenuItem menuItem = aMenu.getItem( i );
        if ( menuItem == null )
        {
          // Not a menu item; simply ignore it and continue...
          continue;
        }

        final String itemText = menuItem.getText();
        if ( !result.contains( itemText ) && !isPersistentMenuItem( menuItem ) )
        {
          // Remove this menu item from the menu; it is obsolete...
          aMenu.remove( i );
        }
        else
        {
          // Remove the checked item; it should not be (re)added to the menu...
          result.remove( itemText );
        }
      }

      return result.toArray( new String[result.size()] );
    }
  }

  /**
   * Provides a builder for building the devices menu upon selection of the
   * menu.
   */
  static class DeviceMenuBuilder extends AbstractMenuBuilder
  {
    // VARIABLES

    private final MainFrame mainFrame;

    // CONSTRUCTORS

    /**
     * Creates a new MainFrame.DeviceMenuBuilder instance.
     */
    public DeviceMenuBuilder( final ClientController aController, final MainFrame aMainFrame )
    {
      super( aController );
      this.mainFrame = aMainFrame;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JMenuItem createMenuItem( final String aDeviceName )
    {
      final Action action = this.controller.getAction( SelectDeviceAction.getID( aDeviceName ) );
      action.putValue( Action.SELECTED_KEY, isDeviceToBeSelected( aDeviceName ) );
      return new JRadioButtonMenuItem( action );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getMenuItemNames()
    {
      return this.controller.getDeviceNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getNoItemsName()
    {
      return "No devices.";
    }

    /**
     * Returns whether or not the given device name is to be selected in the
     * menu.
     * 
     * @param aDeviceName
     *          the name of the device to test.
     * @return {@link Boolean#TRUE} if the device is to be selected,
     *         {@link Boolean#FALSE} otherwise.
     */
    private Boolean isDeviceToBeSelected( final String aDeviceName )
    {
      return Boolean.valueOf( aDeviceName.equals( this.mainFrame.lastSelectedDeviceName ) );
    }
  }

  /**
   * Provides a builder for building the export menu upon selection of the menu.
   */
  static class ExportMenuBuilder extends AbstractMenuBuilder
  {
    /**
     * Creates a new MainFrame.ExportMenuBuilder instance.
     */
    public ExportMenuBuilder( final ClientController aController )
    {
      super( aController );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JMenuItem createMenuItem( final String aExporterName )
    {
      return new JMenuItem( new ExportAction( this.controller, aExporterName ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getMenuItemNames()
    {
      return this.controller.getExporterNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getNoItemsName()
    {
      return "No exporters.";
    }
  }

  /**
   * Listens to window-close events for our main frame, explicitly invoking code
   * to close it on all platforms.
   */
  static final class MainFrameListener extends WindowAdapter
  {
    /**
     * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
     */
    @Override
    public void windowClosing( final WindowEvent aEvent )
    {
      final MainFrame mainFrame = ( MainFrame )aEvent.getSource();
      mainFrame.close();
    }
  }

  /**
   * Provides a builder for building the tools menu upon selection of the menu.
   */
  static class ToolMenuBuilder extends AbstractMenuBuilder
  {
    /**
     * Creates a new MainFrame.ToolMenuBuilder instance.
     */
    public ToolMenuBuilder( final ClientController aController )
    {
      super( aController );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JMenuItem createMenuItem( final String aToolName )
    {
      return new JMenuItem( this.controller.getAction( RunToolAction.getID( aToolName ) ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getMenuItemNames()
    {
      return this.controller.getToolNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getNoItemsName()
    {
      return "No tools.";
    }
  }

  /**
   * Provides a builder for building the window menu upon selection of the menu.
   */
  static class WindowMenuBuilder extends AbstractMenuBuilder
  {
    /**
     * Creates a new MainFrame.WindowMenuBuilder instance.
     */
    public WindowMenuBuilder( final ClientController aController )
    {
      super( aController );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JMenuItem createMenuItem( final String aWindowName )
    {
      return new JCheckBoxMenuItem( new FocusWindowAction( aWindowName ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getMenuItemNames()
    {
      final Window[] windows = Window.getWindows();
      final String[] titles = new String[windows.length];
      for ( int i = 0; i < titles.length; i++ )
      {
        titles[i] = FocusWindowAction.getTitle( windows[i] );
      }
      return titles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getNoItemsName()
    {
      return "No windows.";
    }
  }

  // CONSTANTS

  private static final long serialVersionUID = 1L;

  private static final String PERSISTENT_MENU_ITEM_KEY = "persistentMenuItem";

  // VARIABLES

  private final Diagram diagram;
  private final JTextStatusBar status;

  private JMenu deviceMenu;
  private JMenu toolsMenu;
  private JMenu windowMenu;
  private JMenu exportMenu;

  private volatile String lastSelectedDeviceName;

  private final ClientController controller;

  // CONSTRUCTORS

  /**
   * Creates a new MainFrame instance.
   * 
   * @param aController
   *          the client controller to use, cannot be <code>null</code>.
   */
  public MainFrame( final ClientController aController )
  {
    super();

    // Let the host platform determine where this diagram should be displayed;
    // gives it more or less a native feel...
    setLocationByPlatform( true );

    this.controller = aController;

    this.diagram = new Diagram( this.controller );
    this.status = new JTextStatusBar();

    setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
    setSize( 1200, 600 );

    final JToolBar tools = createMenuBars();

    // Create a scrollpane for the diagram...
    final JScrollPane scrollPane = new JScrollPane( this.diagram );

    final Container contentPane = getContentPane();
    contentPane.setLayout( new BorderLayout() );

    contentPane.add( tools, BorderLayout.PAGE_START );
    contentPane.add( scrollPane, BorderLayout.CENTER );
    contentPane.add( this.status, BorderLayout.PAGE_END );

    // Add the window icon...
    setIconImages( internalGetIconImages() );

    // Support closing of this window on Windows/Linux platforms...
    addWindowListener( new MainFrameListener() );
  }

  // METHODS

  /**
   * @see nl.lxtreme.ols.util.swing.StandardActionFactory.CloseAction.Closeable#close()
   */
  @Override
  public void close()
  {
    setVisible( false );
    dispose();

    // Make sure that if this frame is closed, the entire application is
    // shutdown as well...
    this.controller.exit();
  }

  /**
   * Converts a mouse position to a sample index.
   * 
   * @param aLocation
   *          the mouse position to convert, cannot be <code>null</code>.
   * @return the sample index of the sample under the mouse.
   */
  public long convertMousePositionToSampleIndex( final Point aLocation )
  {
    return this.diagram.convertPointToSampleIndex( aLocation );
  }

  /**
   * Returns the name of the current selected device in the devices menu.
   * 
   * @return the name of the current selected device, or <code>null</code> if no
   *         device is selected.
   */
  public final String getSelectedDeviceName()
  {
    return this.lastSelectedDeviceName;
  }

  /**
   * Returns the current zoom scale.
   * 
   * @return a zoom scale, > 0.0
   */
  public double getZoomScale()
  {
    return this.diagram.getScale();
  }

  /**
   * Sets the view to the position indicated by the given sample position.
   * 
   * @param aSamplePos
   *          the sample position, >= 0.
   */
  public void gotoPosition( final long aSamplePos )
  {
    this.diagram.gotoPosition( aSamplePos );
  }

  /**
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange( final PropertyChangeEvent aEvent )
  {
    final String propertyName = aEvent.getPropertyName();
    if ( ProjectProperties.PROPERTY_CHANGED.equals( propertyName ) )
    {
      final Boolean value = ( Boolean )aEvent.getNewValue();
      // Causes the window to be annotated with a dot on OSX...
      getRootPane().putClientProperty( "Window.documentModified", value );
    }
    else if ( ProjectProperties.PROPERTY_NAME.equals( propertyName ) )
    {
      // The project's name has changed; update the title bar to show this...
      final String value = ( String )aEvent.getNewValue();

      String title = this.controller.getHostProperties().getFullName();
      if ( !StringUtils.isEmpty( value ) )
      {
        // Denote the project file in the title of the main window...
        title = title.concat( " :: " ).concat( value );
      }
      setTitle( title );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void readPreferences( final UserSettings aSettings )
  {
    // Detour: make sure the controller does this, so the actions are correctly
    // synchronized...
    this.controller.selectDevice( aSettings.get( "selectedDevice", "" ) );
  }

  /**
   * Sets the channel labels to the given array.
   * 
   * @param aChannelLabels
   *          the changed channel labels.
   */
  public void setChannelLabels( final String[] aChannelLabels )
  {
    this.diagram.updatePreferredSize();
    this.diagram.revalidateAll();
  }

  /**
   * Updates the progress bar to the given percentage.
   * 
   * @param aPercentage
   *          the percentage to set, >= 0 && <= 100.
   */
  public void setProgress( final int aPercentage )
  {
    this.status.setProgress( aPercentage );
  }

  /**
   * Sets the status bar message to the message given.
   * 
   * @param aMessage
   *          the message to set as status text;
   * @param aMessageArgs
   *          the (optional) message arguments.
   */
  public void setStatus( final String aMessage, final Object... aMessageArgs )
  {
    String message = aMessage;
    if ( ( aMessageArgs != null ) && ( aMessageArgs.length > 0 ) )
    {
      message = MessageFormat.format( message, aMessageArgs );
    }
    this.status.setText( message );
    this.status.setProgress( 0 );
  }

  /**
   * Shows the main about box.
   */
  public void showAboutBox()
  {
    final HostProperties hostProperties = this.controller.getHostProperties();
    final AboutBox aboutDialog = new AboutBox( hostProperties.getShortName(), hostProperties.getVersion() );
    aboutDialog.showDialog();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writePreferences( final UserSettings aSettings )
  {
    // We cannot put null values into the settings!
    final String selectedDevice = this.lastSelectedDeviceName != null ? this.lastSelectedDeviceName : "";
    aSettings.put( "selectedDevice", selectedDevice );
  }

  /**
   * Zooms to the default level.
   */
  public void zoomDefault()
  {
    this.diagram.zoomDefault();
  }

  /**
   * Zooms in.
   */
  public void zoomIn()
  {
    this.diagram.zoomIn();
  }

  /**
   * Zooms out.
   */
  public void zoomOut()
  {
    this.diagram.zoomOut();
  }

  /**
   * Zooms so that the entire capture is visible.
   */
  public void zoomToFit()
  {
    this.diagram.zoomToFit();
  }

  /**
   * Should be called to apply new diagram settings.
   */
  final void diagramSettingsUpdated()
  {
    this.diagram.revalidateAll();
  }

  /**
   * Returns the current diagram instance.
   * 
   * @return a diagram instance, cannot be <code>null</code>.
   */
  final Diagram getDiagram()
  {
    return this.diagram;
  }

  /**
   * Returns the scroll pane of the current diagram instance.
   * 
   * @return a scroll pane instance, can be <code>null</code>.
   */
  final JComponent getDiagramScrollPane()
  {
    final Container viewport = getDiagram().getParent();
    return ( JComponent )viewport.getParent();
  }

  /**
   * Sets the name of the current selected device in the devices menu.
   * 
   * @param aSelectedDeviceName
   *          the name of the selected device, can be <code>null</code>.
   */
  final void setSelectedDeviceName( final String aSelectedDeviceName )
  {
    this.lastSelectedDeviceName = aSelectedDeviceName;
  }

  /**
   * Creates the menu bar with all menu's and the accompanying toolbar.
   * 
   * @return the toolbar, never <code>null</code>.
   */
  private JToolBar createMenuBars()
  {
    final JMenuBar bar = new JMenuBar();
    setJMenuBar( bar );

    this.exportMenu = new JMenu( "Export ..." );
    this.exportMenu.setMnemonic( 'e' );
    this.exportMenu.addMenuListener( new ExportMenuBuilder( this.controller ) );

    final JMenu fileMenu = new JMenu( "File" );
    fileMenu.setMnemonic( 'F' );
    bar.add( fileMenu );

    fileMenu.add( this.controller.getAction( NewProjectAction.ID ) );
    fileMenu.add( this.controller.getAction( OpenProjectAction.ID ) );
    fileMenu.add( this.controller.getAction( SaveProjectAction.ID ) );
    fileMenu.add( this.controller.getAction( SaveProjectAsAction.ID ) );
    fileMenu.addSeparator();
    fileMenu.add( this.controller.getAction( OpenDataFileAction.ID ) );
    fileMenu.add( this.controller.getAction( SaveDataFileAction.ID ) );
    fileMenu.addSeparator();
    fileMenu.add( this.exportMenu );

    final HostInfo hostInfo = HostUtils.getHostInfo();
    if ( hostInfo.needsExitMenuItem() )
    {
      fileMenu.add( new JSeparator() );
      fileMenu.add( this.controller.getAction( ExitAction.ID ) );
    }

    if ( hostInfo.needsPreferencesMenuItem() )
    {
      final JMenu editMenu = bar.add( new JMenu( "Edit" ) );
      editMenu.setMnemonic( 'E' );
      editMenu.add( this.controller.getAction( ShowPreferencesDialogAction.ID ) );
    }

    JMenu captureMenu = bar.add( new JMenu( "Capture" ) );
    captureMenu.setMnemonic( 'C' );

    this.deviceMenu = new JMenu( "Device" );
    this.deviceMenu.setMnemonic( 'D' );
    this.deviceMenu.addMenuListener( new DeviceMenuBuilder( this.controller, this ) );

    captureMenu.add( this.controller.getAction( CaptureAction.ID ) );
    captureMenu.add( this.controller.getAction( RepeatCaptureAction.ID ) );
    captureMenu.add( this.controller.getAction( CancelCaptureAction.ID ) );
    captureMenu.addSeparator();
    captureMenu.add( this.deviceMenu );

    final JMenu diagramMenu = bar.add( new JMenu( "Diagram" ) );
    diagramMenu.setMnemonic( 'D' );

    diagramMenu.add( this.controller.getAction( ZoomInAction.ID ) );
    diagramMenu.add( this.controller.getAction( ZoomOutAction.ID ) );
    diagramMenu.add( this.controller.getAction( ZoomDefaultAction.ID ) );
    diagramMenu.add( this.controller.getAction( ZoomFitAction.ID ) );
    diagramMenu.addSeparator();
    diagramMenu.add( this.controller.getAction( GotoTriggerAction.ID ) );
    diagramMenu.add( this.controller.getAction( GotoFirstCursorAction.ID ) );
    diagramMenu.add( this.controller.getAction( GotoLastCursorAction.ID ) );
    for ( int c = 0; c < Ols.MAX_CURSORS; c++ )
    {
      diagramMenu.add( this.controller.getAction( GotoNthCursorAction.getID( c ) ) );
    }
    diagramMenu.addSeparator();
    diagramMenu.add( new JCheckBoxMenuItem( this.controller.getAction( SetCursorModeAction.ID ) ) );
    diagramMenu.add( this.controller.getAction( ClearCursors.ID ) );
    diagramMenu.addSeparator();
    diagramMenu.add( this.controller.getAction( RemoveAnnotationsAction.ID ) );
    diagramMenu.addSeparator();
    diagramMenu.add( this.controller.getAction( ShowDiagramModeSettingsDialogAction.ID ) );
    diagramMenu.add( this.controller.getAction( ShowChannelLabelsDialogAction.ID ) );

    this.toolsMenu = bar.add( new JMenu( "Tools" ) );
    this.toolsMenu.setMnemonic( 'T' );
    this.toolsMenu.addMenuListener( new ToolMenuBuilder( this.controller ) );

    if ( hostInfo.isMacOS() )
    {
      this.windowMenu = bar.add( new JMenu( "Window" ) );
      this.windowMenu.setMnemonic( 'W' );

      // Add two items that remain constant for the remainder of the lifetime of
      // this client...
      this.windowMenu.add( new JMenuItem( StandardActionFactory.createCloseAction() ) ) //
          .putClientProperty( PERSISTENT_MENU_ITEM_KEY, Boolean.TRUE );
      this.windowMenu.add( new JMenuItem( new MinimizeWindowAction() ) ) //
          .putClientProperty( PERSISTENT_MENU_ITEM_KEY, Boolean.TRUE );

      this.windowMenu.addSeparator();

      this.windowMenu.addMenuListener( new WindowMenuBuilder( this.controller ) );
    }

    final JMenu helpMenu = bar.add( new JMenu( "Help" ) );
    helpMenu.setMnemonic( 'H' );
    helpMenu.add( this.controller.getAction( ShowBundlesAction.ID ) );

    if ( hostInfo.needsAboutMenuItem() )
    {
      helpMenu.addSeparator();
      helpMenu.add( this.controller.getAction( HelpAboutAction.ID ) );
    }

    final JToolBar toolbar = new JToolBar();
    toolbar.setRollover( true );

    toolbar.add( this.controller.getAction( OpenProjectAction.ID ) );
    toolbar.add( this.controller.getAction( SaveProjectAction.ID ) );
    toolbar.addSeparator();

    toolbar.add( this.controller.getAction( CaptureAction.ID ) );
    toolbar.add( this.controller.getAction( CancelCaptureAction.ID ) );
    toolbar.add( this.controller.getAction( RepeatCaptureAction.ID ) );
    toolbar.addSeparator();

    toolbar.add( this.controller.getAction( ZoomInAction.ID ) );
    toolbar.add( this.controller.getAction( ZoomOutAction.ID ) );
    toolbar.add( this.controller.getAction( ZoomDefaultAction.ID ) );
    toolbar.add( this.controller.getAction( ZoomFitAction.ID ) );
    toolbar.addSeparator();

    toolbar.add( this.controller.getAction( GotoTriggerAction.ID ) );
    for ( int c = 0; c < Ols.MAX_CURSORS; c++ )
    {
      toolbar.add( this.controller.getAction( GotoNthCursorAction.getID( c ) ) );
    }

    return toolbar;
  }

  /**
   * Creates a list of icon images that are used to decorate this frame.
   * 
   * @return a list of images, never <code>null</code>.
   */
  private List<? extends Image> internalGetIconImages()
  {
    final Image windowIcon16x16 = IconFactory.createImage( IconLocator.WINDOW_ICON_16x16 );
    final Image windowIcon32x32 = IconFactory.createImage( IconLocator.WINDOW_ICON_32x32 );
    final Image windowIcon48x48 = IconFactory.createImage( IconLocator.WINDOW_ICON_48x48 );
    final Image windowIcon64x64 = IconFactory.createImage( IconLocator.WINDOW_ICON_64x64 );
    final Image windowIcon256x256 = IconFactory.createImage( IconLocator.WINDOW_ICON_256x256 );
    return Arrays.asList( windowIcon16x16, windowIcon32x32, windowIcon48x48, windowIcon64x64, windowIcon256x256 );
  }
}
