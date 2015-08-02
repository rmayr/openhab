/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.internal;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ModbusSlave class instantiates physical Modbus slave. 
 * It is responsible for polling data from physical device using TCPConnection.
 * It is also responsible for updating physical devices according to OpenHAB commands  
 *
 * @author Dmitry Krasnov
 * @since 1.1.0
 */
public class ModbusSerialSlave extends ModbusSlave {

	private static final Logger logger = LoggerFactory.getLogger(ModbusSerialSlave.class);

	private String port = null;
	private int baud = 9600;
	private int receiveTimeout = 500; // milliseconds
	private int dataBits = 8;
	private String parity = "None"; // "none", "even" or "odd"
	private Double stopBits = 1.0;
	private String serialEncoding = Modbus.DEFAULT_SERIAL_ENCODING;
	
	public void setReceiveTimeout(int timeout) {
		this.receiveTimeout = timeout;
	}
	
	public void setPort(String port) {
		if ((port != null) && (this.port != port)) {
			logger.debug("overriding modbus port: " + this.port
					+ " by: " + port
					+ "but there is currently only one port supported");
		}
		this.port = port;
	}

	public void setBaud(int baud) {
		this.baud = baud;
	}

	public void setDatabits(int dataBits) {
		this.dataBits = dataBits;
	}
	
	// Parity string should be "none", "even" or "odd"
	public void setParity(String parity) {
		this.parity = parity;
	}
	
	public void setStopbits(Double stopBits) {
		this.stopBits = stopBits;
	}

	private boolean isEncodingValid(String serialEncoding) {
		for (String str : Modbus.validSerialEncodings) {
			if (str.trim().contains(serialEncoding))
				return true;
		}
		return false;
	}

	public void setEncoding(String serialEncoding) {
		serialEncoding = serialEncoding.toLowerCase();

		if ( isEncodingValid(serialEncoding) ) {
			this.serialEncoding = serialEncoding;
		} else {
			logger.info("Encoding '{}' is unknown", serialEncoding);
		}
	}

	private static SerialConnection connection = null;

	public ModbusSerialSlave(String slave) {
		super(slave);
		transaction = new ModbusSerialTransaction();
	}

	/**
	 * Performs physical write to device when slave type is "holding" using Modbus FC06 function
	 * @param command command received from OpenHAB
	 * @param readRegister reference to the register that stores current value
	 * @param writeRegister register reference to write data to
	 */

	public boolean isConnected() {
		return connection != null;
	}

	/**
	 * Establishes connection to the device
	 */
	public boolean connect() {
		try {
			//			Enumeration<CommPortIdentifier> portlist =  CommPortIdentifier.getPortIdentifiers();
			//			while (portlist.hasMoreElements()) {
			//				logger.debug(portlist.nextElement().toString());
			//			}

			if (connection == null) {
				logger.debug("connection was null, going to create a new one");
				SerialParameters params = new SerialParameters();
				params.setPortName(port);
				params.setBaudRate(baud);
				params.setDatabits(dataBits);
				params.setParity(parity);
				params.setStopbits(stopBits);
				params.setEncoding(serialEncoding);
				params.setEcho(false);
  		        params.setReceiveTimeout(receiveTimeout);
				connection = new SerialConnection(params);
			}
			if (!connection.isOpen()) {
				connection.open();
			}
			((ModbusSerialTransaction)transaction).setSerialConnection(connection);
		} catch (Exception e) {
			logger.error("ModbusSlave: Error connecting to master: {}", e.getMessage());
			return false;
		}
		return true;
	}

	public void resetConnection() {
		if (connection != null) {
			connection.close();
		}
		connection = null;
	}

}
