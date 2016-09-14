package mapping;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import constants.Properties;
import situationtemplate.model.TConditionNode;
import situationtemplate.model.TContextNode;
import situationtemplate.model.TOperationNode;
import situationtemplate.model.TParent;
import situationtemplate.model.TSituationTemplate;
import utils.NodeREDUtils;

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
	@SuppressWarnings("unchecked")
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
					}
				}

				nodeREDNode.put("wires", connections);
				nodeREDModel.add(nodeREDNode);
			}

		}

		return nodeREDModel;
	}
}
