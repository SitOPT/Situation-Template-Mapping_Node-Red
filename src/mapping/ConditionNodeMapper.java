package mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import constants.Nodes;
import constants.Properties;
import situationtemplate.model.*;
import utils.NodeREDUtils;


/**
 * This class maps the operational nodes from the situation template to corresponding Node-RED nodes.
 */
public class ConditionNodeMapper {

    /**
     * This method processes the mapping of condition nodes.
     * 
     * @param situationTemplate
     *            the situation template to be mapped
     * @param nodeREDModel
     *            the nodeREDModel in JSON to be extended
     * 
     * @return the mapped model
     * 
     * @throws ParseException
     *             this exception occurs if the JSON can't be parsed
     */
    @SuppressWarnings("unchecked")
    public JSONArray mapConditionNodes(TSituationTemplate situationTemplate, JSONArray nodeREDModel, boolean debug,
            ObjectIdSensorIdMapping sensorMapping) throws ParseException {

        String xCoordinate = "600";
        int yCoordinate = 50;

        if (situationTemplate.getConditionNode().size() > 1 && situationTemplate.getOperationNode().size() == 0) {
            throw new Error("OperationNodes are only omittable if there is only one ConditionNode.");
        }

        for (TConditionNode node : situationTemplate.getConditionNode()) {
            ArrayList<String> objects = new ArrayList<>();
            for (TContextNode cnode : situationTemplate.getContextNode()) {
                for (TParent parent : cnode.getParent()) {
                    if ((parent.getParentID() instanceof TConditionNode)
                            && ((TConditionNode) parent.getParentID()).getId().equals(node.getId())) {
                        for (String o : sensorMapping.getObjects(cnode.getId())) {
                            objects.add(o);
                        }
                    }
                }
            }

            for (String object : objects) {
                List<String> conditionValues = node.getCondValues().getValue();
                JSONObject nodeREDNode = NodeREDUtils.createNodeREDNode(
                        situationTemplate.getId() + "." + node.getId() + object, node.getName() + " for " + object,
                        "function", xCoordinate, Integer.toString(yCoordinate), situationTemplate.getId());

                String sensorId = "";

                for (TContextNode cn : situationTemplate.getContextNode()) {
                    for (TParent p : cn.getParent()) {
                        if (p.getParentID().equals(node)) {
                            sensorId = cn.getId();
                        }
                    }
                }

                Object methodReturn;
                List<String> sensorOps = Arrays
                        .asList(new String[] { "SENSORLOWERTHAN", "SENSORGREATERTHAN", "SENSOREQUALS" });

                if (node instanceof TTimeNode) {
                    TTimeNode timeNode = (TTimeNode) node;
                    try {
                        methodReturn = Nodes.class.getMethod("get" + timeNode.getOpType().toUpperCase() + "Node",
                                String.class, String.class, String.class, String.class, ObjectIdSensorIdMapping.class).invoke(null,
                                        timeNode.getAmountIntervals().toString(), conditionValues.get(0), object,
                                        situationTemplate.getId(), sensorMapping);
                    } catch (NoSuchMethodException | SecurityException | InvocationTargetException
                            | IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                } else if (sensorOps.indexOf(node.getOpType().toUpperCase()) > -1) {
                    ArrayList<TContextNode> contextNodes = getSensorNodes(node);
                    if (contextNodes != null && contextNodes.size() > 0) {
                        try {
                            java.lang.reflect.Method m = Nodes.class.getMethod(
                                    "get" + node.getOpType().toUpperCase() + "Node", contextNodes.getClass(), String.class,
                                    String.class, String.class);
                            methodReturn = m.invoke(null, contextNodes, object, situationTemplate.getId(), sensorId);
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                                | IllegalArgumentException | InvocationTargetException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    } else {
                        continue;
                    }
                } else {
                    try {
                        methodReturn = Nodes.class
                                .getMethod("get" + node.getOpType().toUpperCase() + "Node", List.class, String.class,
                                        String.class, String.class, String.class, String.class)
                                .invoke(null, conditionValues, object, situationTemplate.getId(), sensorId, "0", "0");
                    } catch (NoSuchMethodException | SecurityException | InvocationTargetException
                            | IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }

                nodeREDNode.put("func", methodReturn);

                nodeREDNode.put("outputs", "1");

                // connect node to the existing flow or to a debug node
                JSONArray wiresNode = new JSONArray();
                JSONArray connections = new JSONArray();

                // add parents
                for (TParent parent : node.getParent()) {
                    if (parent.getParentID() instanceof TConditionNode) {
                        connections.add(situationTemplate.getId() + "." + ((TConditionNode) parent.getParentID()).getId());
                    } else if (parent.getParentID() instanceof TOperationNode) {
                        connections.add(situationTemplate.getId() + "." + ((TOperationNode) parent.getParentID()).getId());
                    } else if (parent.getParentID() instanceof TSituationNode) {
                        JSONObject nullNode = NodeREDUtils.createNodeREDNode(situationTemplate.getName() + ".nullNode", "passthrough", "function", Integer.toString(900), Integer.toString(50), situationTemplate.getId());
                        nullNode.put("func", Nodes.getNULLNode(object, situationTemplate.getId(), sensorMapping));
                        nullNode.put("outputs", "1");

                        connections.add(situationTemplate.getName() + ".nullNode");

                        JSONObject debugNode = NodeREDUtils.generateDebugNode("600", "500", situationTemplate.getId());
                        debugNode.put("name", situationTemplate.getName());
                        nodeREDModel.add(debugNode);

                        // create the corresponding NodeRED JSON node
                        JSONObject httpNode = NodeREDUtils.createNodeREDNode(NodeREDUtils.generateNodeREDId(),
                                "situation", "http request", Integer.toString(200), Integer.toString(200), situationTemplate.getId());
                        httpNode.put("method", "POST");

                        StringBuilder builder = new StringBuilder();
                        builder.append(Properties.getSituationProtocol());
                        builder.append("://");
                        builder.append(Properties.getSituationServer());
                        builder.append(":");
                        builder.append(Properties.getSituationPort());
                        if (!Properties.getSituationPath().startsWith("/")) {
                            builder.append("/");
                        }
                        builder.append(Properties.getSituationPath());

                        httpNode.put("url", builder.toString());

                        JSONArray httpConn = new JSONArray();
                        JSONArray httpWires = new JSONArray();
                        httpConn.add(debugNode.get("id"));
                        httpWires.add(httpConn);
                        httpNode.put("wires", httpWires);

                        JSONArray cons = new JSONArray();
                        cons.add(httpNode.get("id"));
                        JSONArray wires = new JSONArray();
                        wires.add(cons);
                        nullNode.put("wires", wires);
                        nodeREDModel.add(nullNode);

                        nodeREDModel.add(httpNode);
                    }
                }

                if (debug) {
                    // also connect to a debug node
                    JSONObject debugNode = NodeREDUtils.generateDebugNode("600", "500", situationTemplate.getId());
                    debugNode.put("name", node.getName().isEmpty() ? node.getOpType() : node.getName());
                    debugNode.put("console", "true");

                    nodeREDModel.add(debugNode);
                    connections.add(debugNode.get("id"));
                }

                wiresNode.add(connections);

                nodeREDNode.put("wires", wiresNode);

                nodeREDModel.add(nodeREDNode);
                yCoordinate += 100;
            }
        }

        return nodeREDModel;
    }

    /**
     * Returns the sensor nodes of a condition node
     * @param node the condition node, whose sensors should be found.
     * @return
     */
    private ArrayList<TContextNode> getSensorNodes(TConditionNode node) {
        String id = node.getId();
        ArrayList<TContextNode> returnValue = new ArrayList<>();
        Properties.getContextNodes().stream().forEach(c -> {
            if (c.getParent().stream().anyMatch(p -> ((TConditionNode) p.getParentID()).getId().equals(id))) {
                returnValue.add(c);
            }
        });
        return returnValue;
    }
}
