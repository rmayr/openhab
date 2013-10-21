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
package org.openhab.binding.yamahareceiver;


import java.util.Map;

import org.openhab.binding.yamahareceiver.internal.YamahaReceiverBindingConfig;
import org.openhab.core.binding.BindingProvider;

/**
 * Yamaha Receiver BindingProvider interface
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public interface YamahaReceiverBindingProvider extends BindingProvider {

	/**
	 * Returns the Item Configuration identified by {@code itemName}.
	 * 
	 * @param itemName
	 *            the name of the Item.
	 * @return The Item Configuration identified by {@code itemName}.
	 * 
	 */
	public YamahaReceiverBindingConfig getItemConfig(String itemName);

	/**
	 * Gets all of the configurations registered to a particular device
	 * identified by {@code deviceUid}
	 * 
	 * @param deviceUid
	 *            the uid of the device
	 * @param items
	 *            the map of configurations to populate. key=itemName,
	 *            value=configuration
	 */
	public void getDeviceConfigs(String deviceUid,
			Map<String, YamahaReceiverBindingConfig> configs);

}
