/**
 * openHAB, the open Home Automation Bus.
 * Copyright (C) 2010-2013, openHAB.org <admin@openhab.org>
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with Eclipse (or a modified version of that library),
 * containing parts covered by the terms of the Eclipse Public License
 * (EPL), the licensors of this Program grant you additional permission
 * to convey the resulting work.
 */
package org.openhab.binding.yamahareceiver.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.yamahareceiver.YamahaReceiverBindingProvider;
import org.openhab.binding.yamahareceiver.internal.YamahaReceiverBindingConfig.BindingType;
import org.openhab.binding.yamahareceiver.internal.hardware.YamahaReceiverProxy;
import org.openhab.binding.yamahareceiver.internal.hardware.YamahaReceiverState;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yamaha Reciever binding. Handles all commands and polls configured devices to
 * process updates.
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class YamahaReceiverBinding extends
		AbstractActiveBinding<YamahaReceiverBindingProvider> implements
		ManagedService {

	public static final String CONFIG_KEY_HOST = "host";
	public static final long DEFAULT_REFRESH_INTERVAL = 60000;
	public static final String DEFAULT_DEVICE_UID = "default";

	private static final float VOLUME_DB_MIN = -80f;
	private static final float VOLUME_DB_MAX = 16f;
	private static final String BINDING_NAME = "YamahaReceiverBinding";

	private static final Logger logger = LoggerFactory
			.getLogger(YamahaReceiverBinding.class);

	private long refreshInterval = DEFAULT_REFRESH_INTERVAL;

	// Map of proxies. key=deviceUid, value=proxy
	// Used to keep track of proxies
	private final Map<String, YamahaReceiverProxy> proxies = new HashMap<String, YamahaReceiverProxy>();

	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	@Override
	protected String getName() {
		return "YamahaReceiver Refresh Service";
	}

	@Override
	protected void execute() {
		try {
			// Iterate through all proxies
			for (Map.Entry<String, YamahaReceiverProxy> entry : proxies
					.entrySet()) {
				String deviceUid = entry.getKey();
				YamahaReceiverProxy receiverProxy = entry.getValue();
				sendUpdates(receiverProxy, deviceUid);
			}
		} catch (Throwable t) {
			logger.error("Error polling devices for " + getName(), t);
		}
	}

	private void sendUpdates(YamahaReceiverProxy receiverProxy, String deviceUid) {
		// Get all item configurations belonging to this proxy
		Collection<YamahaReceiverBindingConfig> configs = getDeviceConfigs(deviceUid);
		try {
			// Poll the state from the device
			YamahaReceiverState state = receiverProxy.getState();

			// Create state updates
			State powerUpdate = state.isPower() ? OnOffType.ON : OnOffType.OFF;
			State muteUpdate = state.isMute() ? OnOffType.ON : OnOffType.OFF;
			State inputUpdate = new StringType(formatString(state.getInput()));
			State surroundUpdate = new StringType(
					formatString(state.getSurroundProgram()));
			State updateVolumeDb = new DecimalType(state.getVolume());
			State updateVolumePercent = new PercentType(
					(int) dbToPercent(state.getVolume()));

			// Send updates
			sendUpdate(configs, BindingType.power, powerUpdate);
			sendUpdate(configs, BindingType.mute, muteUpdate);
			sendUpdate(configs, BindingType.input, inputUpdate);
			sendUpdate(configs, BindingType.surroundProgram, surroundUpdate);
			sendUpdate(configs, BindingType.volumePercent, updateVolumePercent);
			sendUpdate(configs, BindingType.volumeDb, updateVolumeDb);
		} catch (IOException e) {
			logger.warn("Cannot communicate with " + receiverProxy.getHost());
		}
	}

	private Collection<YamahaReceiverBindingConfig> getDeviceConfigs(
			String deviceUid) {
		Map<String, YamahaReceiverBindingConfig> items = new HashMap<String, YamahaReceiverBindingConfig>();
		for (YamahaReceiverBindingProvider provider : this.providers) {
			provider.getDeviceConfigs(deviceUid, items);
		}
		return items.values();
	}

	private void sendUpdate(Collection<YamahaReceiverBindingConfig> configs,
			BindingType type, State state) {
		for (YamahaReceiverBindingConfig config : configs) {
			if (config.getBindingType() == type) {
				eventPublisher.postUpdate(config.getItemName(), state);
			}
		}
	}

	private static final int percentToDB(byte percentByte) {
		float percent = percentByte * .01f;
		float range = VOLUME_DB_MAX - VOLUME_DB_MIN;
		return (int) ((percent * range) + VOLUME_DB_MIN);
	}

	private static final float dbToPercent(float db) {
		float range = VOLUME_DB_MAX - VOLUME_DB_MIN;
		return 100 * (((db - VOLUME_DB_MIN) / range));
	}

	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		YamahaReceiverBindingConfig config = getConfigForItemName(itemName);
		if (config == null) {
			logger.error("Received command for unknown item '" + itemName + "'");
			return;
		}
		YamahaReceiverProxy proxy = proxies.get(config.getDeviceUid());
		if (proxy == null) {
			logger.error("Received command for unknown device uid '"
					+ config.getDeviceUid() + "'");
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug(BINDING_NAME + " processing command '" + command
					+ "' of type '" + command.getClass().getSimpleName()
					+ "' for item '" + itemName + "'");
		}

		try {
			BindingType type = config.getBindingType();
			if (type == BindingType.power) {
				if (command instanceof OnOffType) {
					proxy.setPower(command == OnOffType.ON);
				}
			} else if (type == BindingType.volumePercent
					|| type == BindingType.volumeDb) {
				if (command instanceof IncreaseDecreaseType
						|| command instanceof UpDownType) {
					// increase/decrease dB by .5 dB
					float db = proxy.getState().getVolume();
					float adjAmt;
					if (command == IncreaseDecreaseType.INCREASE
							|| command == UpDownType.UP) {
						adjAmt = .5f;
					} else {
						adjAmt = -.5f;
					}
					float newDb = db + adjAmt;
					proxy.setVolume(newDb);
					// send new value as update
					State newState = new DecimalType(newDb);
					eventPublisher.postUpdate(itemName, newState);
				} else if (command instanceof PercentType) {
					// set dB from percent
					byte percent = ((PercentType) command).byteValue();
					int db = percentToDB(percent);
					proxy.setVolume(db);
				} else {
					// set dB from value
					float db = Float.parseFloat(command.toString());
					proxy.setVolume(db);
				}
				// Volume updates multiple values => send update now
				sendUpdates(proxy, config.getDeviceUid());
			} else if (type == BindingType.mute) {
				if (command instanceof OnOffType) {
					proxy.setMute(command == OnOffType.ON);
				}
			} else if (type == BindingType.input) {
				proxy.setInput(parseString(command.toString()));
			} else if (type == BindingType.surroundProgram) {
				proxy.setSurroundProgram(parseString(command.toString()));
			}
		} catch (IOException e) {
			logger.warn("Cannot communicate with " + proxy.getHost()
					+ " (uid: " + config.getDeviceUid() + ")");
		} catch (Throwable t) {
			logger.error("Error processing command '" + command
					+ "' for item '" + itemName + "'", t);
		}
	}

	private String parseString(String str) {
		// This is annoying when we're dealing with spaces
		str = str.trim();
		if (str.startsWith("\"") && str.endsWith("\"")) {
			str = str.substring(1, str.length() - 1);
		}
		return str;
	}

	private String formatString(String str) {
		return "\"" + str + "\"";
	}

	private YamahaReceiverBindingConfig getConfigForItemName(String itemName) {
		for (YamahaReceiverBindingProvider provider : this.providers) {
			if (provider.getItemConfig(itemName) != null) {
				return provider.getItemConfig(itemName);
			}
		}
		return null;
	}

	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// ignore
	}

	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
		logger.debug(BINDING_NAME + " updated");
		try {
			// Process device configuration
			if (config != null) {
				String refreshIntervalString = (String) config.get("refresh");
				if (StringUtils.isNotBlank(refreshIntervalString)) {
					refreshInterval = Long.parseLong(refreshIntervalString);
				}
				// parse all configured receivers
				// ( yamahareceiver:<uid>.host=10.0.0.2 )
				Enumeration<String> keys = config.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					if (key.endsWith(CONFIG_KEY_HOST)) {
						// parse host
						String host = (String) config.get(key);
						int separatorIdx = key.indexOf('.');
						// no uid => one device => use default UID
						String uid = separatorIdx == -1 ? DEFAULT_DEVICE_UID
								: key.substring(0, separatorIdx);
						// proxy is stateless. keep them in a map in the
						// binding.
						proxies.put(uid, new YamahaReceiverProxy(host));
					}
				}
				setProperlyConfigured(true);
			}
		} catch (Throwable t) {
			logger.error("Error configuring " + getName(), t);
		}
	}

	@Override
	public void activate() {
		logger.debug(BINDING_NAME + " activated");
	}

	@Override
	public void deactivate() {
		logger.debug(BINDING_NAME + " deactivated");
	}

}
