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
package nl.lxtreme.ols.api.data;


import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import nl.lxtreme.ols.api.*;
import nl.lxtreme.ols.api.acquisition.*;
import nl.lxtreme.ols.api.data.project.*;


/**
 * @author jawi
 */
public final class ProjectImpl implements Project
{
  // VARIABLES

  private AcquisitionResult capturedData;
  private boolean cursorsEnabled;
  private Long[] cursors;
  private String[] labels;
  private String sourceVersion;
  private final Map<String, UserSettings> settings = new HashMap<String, UserSettings>();
  private String name;
  private Date lastModified;
  private File file;
  private boolean changed;

  // METHODS

  /**
   * Asserts the given absolute lengths is defined in the captured data.
   * 
   * @param aTimestamps
   */
  public void assertAbsoluteLength( final long aAbsLength )
  {
    assertNotNull( this.capturedData );

    final long absLength = this.capturedData.getAbsoluteLength();
    assertEquals( aAbsLength, absLength );
  }

  /**
   * Asserts the channel group with the given index is disabled in the captured
   * data.
   * 
   * @param aGroupIdx
   *          the group index.
   */
  public void assertChannelGroupDisabled( final int aGroupIdx )
  {
    assertTrue( ( this.capturedData.getEnabledChannels() & ( 0xFFL << ( aGroupIdx * 8 ) ) ) == 0 );
  }

  /**
   * Asserts the channel group with the given index is enabled in the captured
   * data.
   * 
   * @param aGroupIdx
   *          the group index.
   */
  public void assertChannelGroupEnabled( final int aGroupIdx )
  {
    assertTrue( ( this.capturedData.getEnabledChannels() & ( 0xFFL << ( aGroupIdx * 8 ) ) ) != 0 );
  }

  /**
   * Asserts the cursor with the given index occur in the captured data.
   * 
   * @param aTimestamps
   */
  public void assertCursorSet( final int aCursorIdx, final long aCursorValue )
  {
    assertNotNull( this.cursors );
    assertTrue( this.cursors.length > aCursorIdx );
    assertEquals( Long.valueOf( aCursorValue ), this.cursors[aCursorIdx] );
  }

  /**
   * Asserts the cursor with the given index does NOT occur in the captured
   * data.
   * 
   * @param aCursorIdx
   *          the cursor index.
   */
  public void assertCursorUnset( final int aCursorIdx )
  {
    assertNotNull( this.cursors );
    assertTrue( this.cursors.length > aCursorIdx );
    assertNull( this.cursors[aCursorIdx] );
  }

  /**
   * Asserts the given timestamps occur in the captured data.
   * 
   * @param aTimestamps
   */
  public void assertTimeStamps( final long... aTimestamps )
  {
    assertNotNull( this.capturedData );

    final long[] timestamps = this.capturedData.getTimestamps();
    assertArrayEquals( aTimestamps, timestamps );
  }

  /**
   * Asserts the given values occur in the captured data.
   * 
   * @param aTimestamps
   */
  public void assertValues( final int... aValues )
  {
    assertNotNull( this.capturedData );

    final int[] values = this.capturedData.getValues();
    assertArrayEquals( aValues, values );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AcquisitionResult getCapturedData()
  {
    return this.capturedData;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#getChannelLabels()
   */
  @Override
  public String[] getChannelLabels()
  {
    return this.labels;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#getCursorPositions()
   */
  @Override
  public Long[] getCursorPositions()
  {
    return this.cursors;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#getFilename()
   */
  @Override
  public File getFilename()
  {
    return this.file;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#getLastModified()
   */
  @Override
  public Date getLastModified()
  {
    return this.lastModified;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#getName()
   */
  @Override
  public String getName()
  {
    return this.name;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#getSettings(java.lang.String)
   */
  @Override
  public UserSettings getSettings( final String aName )
  {
    return this.settings.get( aName );
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#getSourceVersion()
   */
  @Override
  public String getSourceVersion()
  {
    return this.sourceVersion;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#isChanged()
   */
  @Override
  public boolean isChanged()
  {
    return this.changed;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#isCursorsEnabled()
   */
  @Override
  public boolean isCursorsEnabled()
  {
    return this.cursorsEnabled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCapturedData( final AcquisitionResult aCapturedData )
  {
    this.capturedData = aCapturedData;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#setChanged(boolean)
   */
  @Override
  public void setChanged( final boolean aChanged )
  {
    this.changed = aChanged;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#setChannelLabels(java.lang.String[])
   */
  @Override
  public void setChannelLabels( final String... aChannelLabels )
  {
    this.labels = aChannelLabels;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#setCursorPositions(java.lang.Long[])
   */
  @Override
  public void setCursorPositions( final Long... aCursors )
  {
    this.cursors = aCursors;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#setCursorsEnabled(boolean)
   */
  @Override
  public void setCursorsEnabled( final boolean aEnabled )
  {
    this.cursorsEnabled = aEnabled;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#setFilename(java.io.File)
   */
  @Override
  public void setFilename( final File aFilename )
  {
    this.file = aFilename;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#setLastModified(java.util.Date)
   */
  @Override
  public void setLastModified( final Date aLastModified )
  {
    this.lastModified = aLastModified;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#setName(java.lang.String)
   */
  @Override
  public void setName( final String aName )
  {
    this.name = aName;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#setSettings(nl.lxtreme.ols.api.UserSettings)
   */
  @Override
  public void setSettings( final UserSettings aSettings )
  {
    this.settings.put( aSettings.getName(), aSettings );
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#setSourceVersion(java.lang.String)
   */
  @Override
  public void setSourceVersion( final String aSourceVersion )
  {
    this.sourceVersion = aSourceVersion;
  }

  /**
   * @see nl.lxtreme.ols.api.data.project.Project#visit(nl.lxtreme.ols.api.data.project.ProjectVisitor)
   */
  @Override
  public void visit( final ProjectVisitor aVisitor )
  {
    for ( UserSettings settings : this.settings.values() )
    {
      try
      {
        aVisitor.visit( settings );
      }
      catch ( Exception exception )
      {
        fail( exception.toString() );
      }
    }
  }
}
