package utils;

import constants.Properties;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import mapping.ObjectIdSensorIdMapping;
import situationtemplate.model.TContextNode;
import situationtemplate.model.TSituationTemplate;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class contains utility methods to be used to generate the NodeRED JSON
 * model.
 */
public class NodeREDUtils {

    /**
     * Generates a NodeRED-conform ID.
     *
     * @return the id
     */
    public static String generateNodeREDId() {
        String id = "";
        for (int i = 0; i < 8; i++) {

            Random rdm = new Random();
            int rdmInt = rdm.nextInt(10);

            if (rdmInt < 5) {
                char c = (char) (rdm.nextInt(26) + 'a');
                id += c;
            } else {
                id += rdm.nextInt(9);
            }
        }
        id += ".";
        for (int i = 0; i < 5; i++) {
            Random rdm = new Random();
            int rdmInt = rdm.nextInt(10);

            if (rdmInt < 5) {
                char c = (char) (rdm.nextInt(26) + 'a');
                id += c;
            } else {
                id += rdm.nextInt(9);
            }
        }

        return id;
    }

    /**
     * Generates a NodeRED debug node that is used to display the output.
     *
     * @return the debug node
     */
    @SuppressWarnings("unchecked")
    public static JSONObject generateDebugNode(String x, String y) {

        JSONObject output = new JSONObject();
        String id = generateNodeREDId();
        output.put("id", id);
        output.put("type", "debug");
        output.put("name", "debug");
        output.put("active", true);
        output.put("console", "false");
        output.put("complete", "false");
        output.put("x", x);
        output.put("y", y);

        output.put("wires", new JSONArray());

        return output;
    }

    /**
     * Generates a NodeRED inject node that is used to display the output.
     *
     * @param situationTemplate
     * @param debugNode
     * @return the inject node
     */
    @SuppressWarnings("unchecked")
    public static JSONObject generateInputNode(TSituationTemplate situationTemplate,
                                               JSONObject debugNode, ObjectIdSensorIdMapping sensorMapping) {

        JSONObject input = new JSONObject();
        input.put("id", generateNodeREDId());
        input.put("type", "inject");
        input.put("name", "inject");
        input.put("topic", "");
        input.put("payload", "");
        input.put("payloadType", "date");
        input.put("repeat", "5");
        input.put("crontab", "");
        input.put("once", false);
        // TODO: this is hard coded
        input.put("x", "100");
        input.put("y", "75");

        JSONArray connections = new JSONArray();

        // TODO
        for (TContextNode sensorNode : situationTemplate.getContextNode()) {
            connections.add(sensorMapping.getObjects() + "." + situationTemplate.getName() + "." + sensorNode.getName());
        }

        connections.add(debugNode.get("id"));
        input.put("wires", connections);

        return input;
    }

    /**
     * Creates a NodeRED JSON node
     *
     * @param id
     *            the id of the node
     * @param name
     *            the name of the node
     * @param type
     *            the type of the node
     * @param x
     *            the x coordinate of the node
     * @param y
     *            the y coordinate of the node
     *
     * @return the node as JSONObject
     */
    @SuppressWarnings("unchecked")
    public static JSONObject createNodeREDNode(String id, String name, String type, String x, String y) {
        JSONObject nodeREDNode = new JSONObject();
        nodeREDNode.put("id", id);
        nodeREDNode.put("type", type);
        nodeREDNode.put("name", name);
        nodeREDNode.put("x", x);
        nodeREDNode.put("y", y);

        return nodeREDNode;
    }

    public static void deleteFlow(String id) {
        try {
            URL url = new URL(Properties.getProtocol() + "://" + Properties.getServer() + ":" + Properties.getPort() + "/flow/" + id);
            System.out.println(Properties.getProtocol() + "://" + Properties.getServer() + ":" + Properties.getPort() + "/flow/" + id);
            HttpURLConnection connection;
            if (Properties.getProtocol().equals("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setRequestMethod("DELETE");
            connection.connect();
            System.out.println(connection.getErrorStream() == null);
            System.out.println(connection.getInputStream() == null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
