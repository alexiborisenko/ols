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
 * Copyright (C) 2006-2010 Michael Poppitz, www.sump.org
 * Copyright (C) 2010 J.W. Janssen, www.lxtreme.nl
 */
package org.sump.device.logicsniffer.protocol;


import java.io.*;
import java.util.logging.*;

import nl.lxtreme.ols.util.*;

import org.sump.device.logicsniffer.*;


/**
 * Wrapper to read SUMP-specific results from a normal {@link DataInputStream}.
 */
public class SumpResultReader implements Closeable, SumpProtocolConstants
{
  // CONSTANTS

  private static final Logger LOG = Logger.getLogger( SumpResultReader.class.getName() );

  // VARIABLES

  private final LogicSnifferConfig config;
  private final DataInputStream inputStream;

  // CONSTRUCTORS

  /**
   * Creates a new {@link SumpResultReader} instance.
   * 
   * @param aConfiguration
   *          the configuration to use, cannot be <code>null</code>;
   * @param aInputStream
   *          the {@link DataInputStream} to read from, cannot be
   *          <code>null</code>.
   */
  public SumpResultReader( final LogicSnifferConfig aConfiguration, final DataInputStream aInputStream )
  {
    this.config = aConfiguration;
    this.inputStream = aInputStream;
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException
  {
    this.inputStream.close();
  }

  /**
   * @throws IOException
   */
  public void flush() throws IOException
  {
    HostUtils.flushInputStream( this.inputStream );
  }

  /**
   * @return the found device ID, or -1 if no suitable device ID was found.
   * @throws IOException
   */
  public int readDeviceId() throws IOException
  {
    int id = this.inputStream.readInt();

    if ( id == SLA_V0 )
    {
      LOG.log( Level.INFO, "Found (unsupported!) Sump Logic Analyzer ...", Integer.toHexString( id ) );
    }
    else if ( id == SLA_V1 )
    {
      LOG.log( Level.INFO, "Found Sump Logic Analyzer/LogicSniffer compatible device ...", Integer.toHexString( id ) );
    }
    else
    {
      LOG.log( Level.INFO, "Found unknown device: 0x{0} ...", Integer.toHexString( id ) );
      id = -1;
    }
    return id;
  }

  /**
   * Reads a single sample (= 1..4 bytes) from the serial input stream.
   * <p>
   * This method will take the enabled channel groups into consideration, making
   * it possible that the returned value contains "gaps".
   * </p>
   * 
   * @return the integer sample value containing up to four read bytes, not
   *         aligned.
   * @throws IOException
   *           if stream reading fails.
   */
  public int readSample() throws IOException, InterruptedException
  {
    final int groupCount = this.config.getGroupCount();
    byte[] buf = new byte[groupCount];

    final int enabledGroupCount = this.config.getEnabledGroupCount();
    assert enabledGroupCount > 0 : "Internal error: enabled group count should be at least 1!";
    assert enabledGroupCount <= groupCount : "Internal error: enabled group count be at most " + groupCount;

    int read, offset = 0;
    do
    {
      // Issue #81: read the same amount of bytes as given in the enabled group
      // count; otherwise succeeding reads might fail and/or data offset errors
      // could occur...
      read = this.inputStream.read( buf, offset, enabledGroupCount - offset );
      if ( read < 0 )
      {
        throw new EOFException( "Data readout interrupted: EOF." );
      }
      if ( Thread.currentThread().isInterrupted() )
      {
        throw new InterruptedException( "Data readout interrupted." );
      }
      offset += read;
    }
    while ( offset < enabledGroupCount );

    // "Expand" the read sample-bytes into a single sample value...
    int value = 0;

    for ( int i = 0, j = 0; i < groupCount; i++ )
    {
      // in case the group is disabled, simply set it to zero...
      if ( this.config.isGroupEnabled( i ) )
      {
        value |= ( ( buf[j++] & 0xff ) << ( 8 * i ) );
      }
    }

    return value;
  }

}
