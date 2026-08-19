package com.codeguard.backend.orchestration.graph;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codeguard.backend.orchestration.nodes.AggregatorNode;
import com.codeguard.backend.orchestration.nodes.DocumentationNode;
import com.codeguard.backend.orchestration.nodes.QualityNode;
import com.codeguard.backend.orchestration.nodes.SecurityNode;
import com.codeguard.backend.orchestration.nodes.SupervisorNode;
import com.codeguard.backend.orchestration.nodes.TestNode;
import com.codeguard.backend.orchestration.state.ReviewState;

@Configuration
public class ReviewGraph {
    private final SupervisorNode supervisorNode;
    private final DocumentationNode documentationNode;
    private final SecurityNode securityNode;
    private final QualityNode qualityNode;
    private final TestNode testNode;
    private final AggregatorNode aggregatorNode;

    public ReviewGraph(SupervisorNode supervisorNode, DocumentationNode documentationNode, SecurityNode securityNode,
            QualityNode qualityNode, TestNode testNode, AggregatorNode aggregatorNode) {
        this.supervisorNode = supervisorNode;
        this.documentationNode = documentationNode;
        this.securityNode = securityNode;
        this.qualityNode = qualityNode;
        this.testNode = testNode;
        this.aggregatorNode = aggregatorNode;
    }

    @Bean
    public CompiledGraph<ReviewState> startGraph() {
        StateGraph<ReviewState> graph = new StateGraph<>(ReviewState.SCHEMA, ReviewState::new);

        try {

            graph.addNode("supervisor", node_async(supervisorNode));

            graph.addNode("security", securityNode);

            graph.addNode("documentation", documentationNode);

            graph.addNode("quality", qualityNode);

            graph.addNode("test", testNode);

            graph.addNode("aggregator", node_async(aggregatorNode));

            // START -> supervisor

            graph.addEdge(StateGraph.START, "supervisor");

            // supervisor -> specialists (Fan-Out)

            graph.addEdge("supervisor", "security");

            graph.addEdge("supervisor", "documentation");

            graph.addEdge("supervisor", "quality");

            graph.addEdge("supervisor", "test");

            // Specialists -> aggregator (Fan-in)
            graph.addEdge("security", "aggregator");

            graph.addEdge("documentation", "aggregator");

            graph.addEdge("quality", "aggregator");

            graph.addEdge("test", "aggregator");

            // aggregator -> END

            graph.addEdge("aggregator", StateGraph.END);

            return graph.compile();

        } catch (GraphStateException e) {

            throw new RuntimeException("Failed to build the Review Graph", e);

        }

    }
}
