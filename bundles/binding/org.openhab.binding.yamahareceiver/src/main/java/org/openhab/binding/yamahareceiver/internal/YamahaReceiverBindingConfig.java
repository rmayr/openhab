package org.openhab.binding.yamahareceiver.internal;

import org.openhab.core.binding.BindingConfig;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
 * Yamaha Receiver item binding configuration
 * 
 * @author Eric Thill
 * @since 1.4.0
 */
public class YamahaReceiverBindingConfig implements BindingConfig {

	public static final String KEY_BINDING_TYPE = "bindingType";
	public static final String KEY_DEVICE_UID = "uid";

	public enum BindingType {
		power, volumePercent, volumeDb, mute, input, surroundProgram;
	}

	private final String deviceUid;
	private final BindingType bindingType;
	private final String itemName;

	public YamahaReceiverBindingConfig(String deviceUid, String bindingType,
			String itemName) throws BindingConfigParseException {
		this.deviceUid = deviceUid != null ? deviceUid
				: YamahaReceiverBinding.DEFAULT_DEVICE_UID;
		this.bindingType = parseBindingType(bindingType);
		this.itemName = itemName;
	}

	private static BindingType parseBindingType(String str)
			throws BindingConfigParseException {
		if (str == null) {
			throw new BindingConfigParseException(KEY_BINDING_TYPE);
		}
		try {
			return BindingType.valueOf(str);
		} catch (Exception e) {
			throw new BindingConfigParseException("error parsing "
					+ KEY_BINDING_TYPE);
		}
	}

	public String getDeviceUid() {
		return deviceUid;
	}

	public BindingType getBindingType() {
		return bindingType;
	}

	public String getItemName() {
		return itemName;
	}
}
