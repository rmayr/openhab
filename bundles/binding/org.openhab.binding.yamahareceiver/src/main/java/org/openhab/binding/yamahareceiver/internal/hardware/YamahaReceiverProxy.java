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
package org.openhab.binding.yamahareceiver.internal.hardware;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Yamaha Receiver Proxy used to control a yamaha receiver with HTTP/XML
 * 
 * @author Eric Thill
 */
public class YamahaReceiverProxy {

	public static final int VOLUME_MIN = -80;
	public static final int VOLUME_MAX = 16;

	public static final String[] SOUND_PROGRAMS_RX_V573 = new String[] {
			"Music Video", "Standard", "Spectacle", "Sci-Fi", "Adventure",
			"Drama", "Mono Movie", "Surrender Decoder", "2ch Stereo",
			"7ch Stereo", "Straight" };

	private final DocumentBuilderFactory dbf = DocumentBuilderFactory
			.newInstance();
	private final String host;

	public YamahaReceiverProxy(String host) {
		this.host = host;
	}
	
	public String getHost() {
		return host;
	}

	public void setPower(boolean on) throws IOException {
		if (on) {
			postAndGetResponse("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><Main_Zone><Power_Control><Power>On</Power></Power_Control></Main_Zone></YAMAHA_AV>");
		} else {
			postAndGetResponse("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><Main_Zone><Power_Control><Power>Standby</Power></Power_Control></Main_Zone></YAMAHA_AV>");
		}
	}

	public void setVolume(float volume) throws IOException {
		postAndGetResponse("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><Main_Zone><Volume><Lvl><Val>"
				+ (int) (volume * 10)
				+ "</Val><Exp>1</Exp><Unit>dB</Unit></Lvl></Volume></Main_Zone></YAMAHA_AV>");
	}

	public void setMute(boolean mute) throws IOException {
		if (mute) {
			postAndGetResponse("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><Main_Zone><Volume><Mute>On</Mute></Volume></Main_Zone></YAMAHA_AV>");
		} else {
			postAndGetResponse("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><Main_Zone><Volume><Mute>Off</Mute></Volume></Main_Zone></YAMAHA_AV>");
		}
	}

	public void setInput(String name) throws IOException {
		postAndGetResponse("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><Main_Zone><Input><Input_Sel>"
				+ name + "</Input_Sel></Input></Main_Zone></YAMAHA_AV>");
	}

	public void setSurroundProgram(String name) throws IOException {
		postAndGetResponse("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"PUT\"><Main_Zone><Surround><Program_Sel><Current><Sound_Program>"
				+ name
				+ "</Sound_Program></Current></Program_Sel></Surround></Main_Zone></YAMAHA_AV>");
	}

	public YamahaReceiverState getState() throws IOException {
		Document doc = postAndGetXmlResponse("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"GET\"><Main_Zone><Basic_Status>GetParam</Basic_Status></Main_Zone></YAMAHA_AV>");
		Node basicStatus = getNode(doc.getFirstChild(),
				"Main_Zone/Basic_Status");

		Node powerNode = getNode(basicStatus, "Power_Control/Power");
		boolean power = "On".equalsIgnoreCase(powerNode.getTextContent());
		Node inputNode = getNode(basicStatus, "Input/Input_Sel");
		String input = inputNode != null ? inputNode.getTextContent() : null;
		Node soundProgramNode = getNode(basicStatus,
				"Surround/Program_Sel/Current/Sound_Program");
		String soundProgram = soundProgramNode != null ? soundProgramNode
				.getTextContent() : null;
		Node volumeNode = getNode(basicStatus, "Volume/Lvl/Val");
		float volume = volumeNode != null ? Float.parseFloat(volumeNode
				.getTextContent()) * .1f : VOLUME_MIN;
		Node muteNode = getNode(basicStatus, "Volume/Mute");
		boolean mute ="On".equalsIgnoreCase(muteNode.getTextContent());

		return new YamahaReceiverState(power, input, soundProgram, volume, mute);
	}

	public List<String> getInputsList() throws IOException {
		List<String> names = new ArrayList<String>();
		Document doc = postAndGetXmlResponse("<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"GET\"><Main_Zone><Input><Input_Sel_Item>GetParam</Input_Sel_Item></Input></Main_Zone></YAMAHA_AV>");
		Node inputSelItem = getNode(doc.getFirstChild(),
				"Main_Zone/Input/Input_Sel_Item");
		NodeList items = inputSelItem.getChildNodes();
		for (int i = 0; i < items.getLength(); i++) {
			Element item = (Element) items.item(i);
			String name = item.getElementsByTagName("Param").item(0)
					.getTextContent();
			boolean writable = item.getElementsByTagName("RW").item(0)
					.getTextContent().contains("W");
			if (writable) {
				names.add(name);
			}
		}
		return names;
	}

	private static Node getNode(Node root, String nodePath) {
		String[] nodePathArr = nodePath.split("/");
		return getNode(root, nodePathArr, 0);
	}

	private static Node getNode(Node parent, String[] nodePath, int offset) {
		if (offset < nodePath.length - 1) {
			return getNode(
					((Element) parent).getElementsByTagName(nodePath[offset])
							.item(0), nodePath, offset + 1);
		} else {
			return ((Element) parent).getElementsByTagName(nodePath[offset])
					.item(0);
		}
	}

	private Document postAndGetXmlResponse(String message) throws IOException {
		String response = postAndGetResponse(message);
		String xml = response.toString();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(new InputSource(new StringReader(xml)));
		} catch (Exception e) {
			throw new IOException("Could not handle response", e);
		}
	}

	private String postAndGetResponse(String message) throws IOException {
		HttpURLConnection connection = null;
		try {
			URL url = new URL("http://" + host + "/YamahaRemoteControl/ctrl");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Length",
					"" + Integer.toString(message.length()));

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(message);
			wr.flush();
			wr.close();
			
			// Read response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(
					new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();
		} catch (Exception e) {
			throw new IOException("Could not handle http post", e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
