package mapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import constants.Nodes;
import constants.Properties;
import situationtemplate.model.TConditionNode;
import situationtemplate.model.TContextNode;
import situationtemplate.model.TOperationNode;
import situationtemplate.model.TParent;
import situationtemplate.model.TSituationNode;
import situationtemplate.model.TSituationTemplate;
import utils.NodeREDUtils;


/**
 * This class maps the operation nodes of the situation template to corresponding NodeRED implementations
 */
public class OperationNodeMapper {

    /**
     * This method processes the mapping of operation nodes
     * 
     * @param situationTemplate
     *            the situation template to be mapped
     * @param nodeREDModel
     *            the existing flow defined in NodeRed JSON
     * 
     * @return the mapped model
     */
    @SuppressWarnings("unchecked")
    public JSONArray mapOperationNodes(TSituationTemplate situationTemplate, JSONArray nodeREDModel,
            ObjectIdSensorIdMapping sensorMapping) {

        // TODO those are just random values, write style function!
        int xCoordinate = 900;
        int yCoordinate = 50;

        // get the number of children of the operation node
        int children = 0;
        for (TOperationNode logicNode : situationTemplate.getOperationNode()) {
            for (TConditionNode node : situationTemplate.getConditionNode()) {
                for (TParent parent : node.getParent()) {

                    if (parent.getParentID() instanceof TConditionNode) {
                        if (((TConditionNode) parent.getParentID()).getId().equals(logicNode.getId())) {
                            children++;
                        }
                    } else if (parent.getParentID() instanceof TOperationNode) {
                        if (((TOperationNode) parent.getParentID()).getId().equals(logicNode.getId())) {
                            for (TContextNode snode : situationTemplate.getContextNode()) {
                                for (TParent sparent : snode.getParent()) {
                                    if (((TConditionNode) sparent.getParentID()).getId().equals(node.getId())) {
                                        for (@SuppressWarnings("unused")
                                        String object : sensorMapping.getObjects(snode)) {
                                            children++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // create the comparison node in NodeRED
            JSONObject nodeREDNode = NodeREDUtils.createNodeREDNode(sensorMapping.getObjects() + "." + situationTemplate.getName() + "." + logicNode.getName(),
                    logicNode.getName(), "function", Integer.toString(xCoordinate), Integer.toString(yCoordinate));
            Method m;
            ArrayList<String> parentIds = new ArrayList<>();
            parentIds.add(logicNode.getId());
            ArrayList<TContextNode> sensors = new ArrayList<>();

            for (TOperationNode node : situationTemplate.getOperationNode()) {
                for (TParent parent : node.getParent()) {
                    if (parent.getParentID() instanceof TSituationNode) {
                        if (parentIds.contains(((TSituationNode) parent.getParentID()).getId())) {
                            parentIds.add(node.getId());
                        }
                    } else {
                        if (parentIds.contains(((TOperationNode) parent.getParentID()).getId())) {
                            parentIds.add(node.getId());
                        }
                    }
                }
            }
            for (TConditionNode node : situationTemplate.getConditionNode()) {
                for (TParent parent : node.getParent()) {
                    if (parentIds.contains(((TOperationNode) parent.getParentID()).getId())) {
                        parentIds.add(node.getId());
                    }
                }
            }
            for (TContextNode node : situationTemplate.getContextNode()) {
                for (TParent parent : node.getParent()) {
                    if (parent.getParentID() instanceof TConditionNode && parentIds.contains(((TConditionNode) parent.getParentID()).getId())) {
                        sensors.add(node);
                    }
                }
            }

            String sensorIdMapping = sensorMapping.map(sensors);

            try {
                m = Nodes.class.getMethod(
                        "get" + logicNode.getType().toUpperCase() + (logicNode.isNegated() ? "Not" : "") + "Node",
                        String.class, String.class, String.class, ObjectIdSensorIdMapping.class);
            } catch (NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            try {
                nodeREDNode.put("func", m.invoke(null, Integer.toString(children), sensorIdMapping,
                        situationTemplate.getId(), sensorMapping));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            // connect it to the parent(s)
            JSONArray connections = new JSONArray();

            if (!logicNode.getParent().isEmpty()) {
                for (TParent parent : logicNode.getParent()) {
                    if (parent.getParentID() instanceof TConditionNode) {
                        String parentId = ((TConditionNode) parent.getParentID()).getName();
                        connections.add(sensorMapping.getObjects() + "." + situationTemplate.getName() + "." + parentId);
                    } else if (parent.getParentID() instanceof TOperationNode) {
                        String parentId = ((TOperationNode) parent.getParentID()).getName();
                        connections.add(sensorMapping.getObjects() + "." + situationTemplate.getName() + "." + parentId);
                    } else if (parent.getParentID() instanceof TSituationNode) {

                        JSONObject debugNode = NodeREDUtils.generateDebugNode("600", "500");
                        debugNode.put("name", sensorMapping.getObjects() + "." + situationTemplate.getName());
                        nodeREDModel.add(debugNode);

                        // create the corresponding NodeRED JSON node
                        JSONObject httpNode = NodeREDUtils.createNodeREDNode(NodeREDUtils.generateNodeREDId(),
                                    "situation", "http request", Integer.toString(200), Integer.toString(200));
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
                        httpConn.add(debugNode.get("id"));
                        httpNode.put("wires", httpConn);

                        connections.add(httpNode.get("id"));

                        nodeREDModel.add(httpNode);
                    }
                }
            } else {
                JSONObject debugNode = NodeREDUtils.generateDebugNode("600", "500");
                debugNode.put("name", situationTemplate.getName());
                nodeREDModel.add(debugNode);
                connections.add(debugNode.get("id"));
            }
                        
            nodeREDNode.put("outputs", String.valueOf(1));
            nodeREDNode.put("wires", connections);
            nodeREDModel.add(nodeREDNode);

            yCoordinate += 100;
        }

        return nodeREDModel;
    }
}
