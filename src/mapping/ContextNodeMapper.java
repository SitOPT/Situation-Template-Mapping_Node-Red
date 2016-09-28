package mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import constants.Nodes;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import constants.Properties;
import situationtemplate.model.*;
import utils.NodeREDUtils;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class maps context nodes to HTTP nodes in NodeRED
 */
public class ContextNodeMapper {

	/**
	 * constants
	 */
	public static final String TYPE = "http request";
	public static final String METHOD = "GET";

	/**
	 * Maps the context nodes to corresponding NodeRED nodes
	 *
	 * @param situationTemplate
	 *            the situation template JAXB node
	 * @param nodeREDModel
	 *            the Node-RED flow as JSON
	 * @param sensorMapping
	 *
	 * @return the mapped JSON model
	 */
	@SuppressWarnings({"unchecked", "Duplicates"})
	public JSONArray mapContextNodes(TSituationTemplate situationTemplate, JSONArray nodeREDModel,
									 ObjectIdSensorIdMapping sensorMapping, boolean debug) {

		int xCoordinate = 300;
		int yCoordinate = 50;

		String url;
		StringBuilder builder = new StringBuilder();
		builder.append(Properties.getResourceProtocol());
		builder.append("://");
		builder.append(Properties.getResourceServer());
		builder.append(':');
		builder.append(Properties.getResourcePort());
		builder.append(builder.charAt(builder.length() - 1) == '/' ? "" : '/');
		builder.append("rmp/sensordata/%s");
		url = builder.toString();

		for (TContextNode sensorNode : situationTemplate.getContextNode()) {
			Properties.getContextNodes().add(sensorNode);

			String sensorURL = url;
			if (sensorNode.getInputType().toLowerCase().equals("sensor")) {
				sensorURL += "/" + sensorNode.getName();
			}
			// create the corresponding NodeRED JSON node
			String[] objects = sensorMapping.getObjects(sensorNode);
			for (String object : objects) {
				String name = sensorNode.getName() == null ? sensorNode.getType() : sensorNode.getName();
				JSONObject nodeREDNode = NodeREDUtils.createNodeREDNode(
						object + "." + situationTemplate.getName() + "." + sensorNode.getName(),
						name + " for " + object, TYPE, Integer.toString(xCoordinate),
						Integer.toString(yCoordinate));
				nodeREDNode.put("method", METHOD);
				if (sensorNode.getInputType().toLowerCase().equals("static")) {
					nodeREDNode.put("url", sensorNode.getMeasureName() + "/rmp/sensordata/" + sensorNode.getType() + "/" + sensorNode.getName());
				} else if (sensorNode.getInputType().toLowerCase().equals("sensor")) {
					nodeREDNode.put("url", String.format(sensorURL, object));
				} else if (sensorNode.getInputType().toLowerCase().equals("situation")) {
					registerSituation(sensorNode.getMeasureName(), sensorNode.getType(), sensorNode.getName());
					nodeREDNode.put("url", sensorNode.getMeasureName() + "/rmp/situation/" + sensorNode.getType() + "/" + sensorNode.getName());
				}
				yCoordinate += 100;

				// now connect the node to the flow
				JSONArray connections = new JSONArray();

				if (debug) {
					// map the sensor node to a debug node
					// TODO X/Y coordinates
					JSONObject debugNode = NodeREDUtils.generateDebugNode("600", "500");
					debugNode.put("name", sensorNode.getName().isEmpty() ? sensorNode.getType() : sensorNode.getName());
					debugNode.put("console", "true");
					nodeREDModel.add(debugNode);
					connections.add(debugNode.get("id"));
				}
				// connect to the parents
				for (TParent parent : sensorNode.getParent()) {
					if (parent.getParentID() instanceof TConditionNode) {
						connections.add(object + "." + situationTemplate.getName() + "."
								+ ((TConditionNode) parent.getParentID()).getName());
					} else if (parent.getParentID() instanceof TOperationNode) {
						connections.add(object + "." + situationTemplate.getName() + "."
								+ ((TOperationNode) parent.getParentID()).getName());
					} else if (parent.getParentID() instanceof TSituationNode) {
						JSONObject nullNode = NodeREDUtils.createNodeREDNode(object + "." + situationTemplate.getName() + ".nullNode", "passthrough", "function", Integer.toString(900), Integer.toString(50));
						nullNode.put("func", Nodes.getNULLNode(object, situationTemplate.getName(), sensorMapping));
						nullNode.put("outputs", "1");

						connections.add(object + "." + situationTemplate.getName() + ".nullNode");

						JSONObject debugNode = NodeREDUtils.generateDebugNode("600", "500");
						debugNode.put("name", situationTemplate.getName());
						nodeREDModel.add(debugNode);

						// create the corresponding NodeRED JSON node
						JSONObject httpNode = NodeREDUtils.createNodeREDNode(NodeREDUtils.generateNodeREDId(),
								"situation", "http request", Integer.toString(200), Integer.toString(200));
						httpNode.put("method", "POST");

						StringBuilder b = new StringBuilder();
						b.append(Properties.getSituationProtocol());
						b.append("://");
						b.append(Properties.getSituationServer());
						b.append(":");
						b.append(Properties.getSituationPort());
						if (!Properties.getSituationPath().startsWith("/")) {
							b.append("/");
						}
						b.append(Properties.getSituationPath());

						httpNode.put("url", b.toString());

						JSONArray httpConn = new JSONArray();
						httpConn.add(debugNode.get("id"));
						httpNode.put("wires", httpConn);

						JSONArray cons = new JSONArray();
						cons.add(httpNode.get("id"));
						nullNode.put("wires", cons);
						nodeREDModel.add(nullNode);

						nodeREDModel.add(httpNode);
					}
				}

				nodeREDNode.put("wires", connections);
				nodeREDModel.add(nodeREDNode);
			}

		}

		return nodeREDModel;
	}

	private void registerSituation(String rmp, String thing, String template) {
		StringBuilder builder = new StringBuilder();
		builder.append(rmp);
		if (!rmp.endsWith("/")) {
			builder.append('/');
		}
		builder.append("situation/");
		builder.append(thing);
		builder.append('/');
		builder.append(template);

		try {
			URL url = new URL(builder.toString());
			if (rmp.toLowerCase().startsWith("https")) {
				registerHttps(url);
			} else {
				registerHttp(url);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void registerHttps(URL url) throws IOException {
		HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.addRequestProperty("Content-Type", "application/json");
		connection.setDoInput(true);
		connection.connect();
		if (connection.getInputStream() != null) {
			try (BufferedReader stdout = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = stdout.readLine()) != null) {
					System.out.println(line);
				}
			}
		}
		if (connection.getErrorStream() != null) {
			try (BufferedReader stderr = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
				String line;
				while ((line = stderr.readLine()) != null) {
					System.err.println(line);
				}
			}
		}
	}

	private void registerHttp(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.addRequestProperty("Content-Type", "application/json");
		connection.setDoInput(true);
		connection.connect();
		if (connection.getInputStream() != null) {
			try (BufferedReader stdout = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = stdout.readLine()) != null) {
					System.out.println(line);
				}
			}
		}
		if (connection.getErrorStream() != null) {
			try (BufferedReader stderr = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
				String line;
				while ((line = stderr.readLine()) != null) {
					System.err.println(line);
				}
			}
		}
	}
}
