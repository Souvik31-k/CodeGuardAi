package com.codeguard.backend.orchestration.graph;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codeguard.backend.orchestration.nodes.SupervisorNode;
import com.codeguard.backend.orchestration.state.ReviewState;

@Configuration
public class ReviewGraph {
    private final SupervisorNode supervisorNode;

    ReviewGraph(SupervisorNode supervisorNode) {
        this.supervisorNode = supervisorNode;
    }

    @Bean
    public CompiledGraph<ReviewState> startGraph() {
        StateGraph<ReviewState> graph = new StateGraph<>(ReviewState.SCHEMA, ReviewState::new);

        try {

            graph.addNode("supervisor", node_async(supervisorNode));

            graph.addEdge(StateGraph.START, "supervisor");

            graph.addEdge("supervisor", StateGraph.END);

            return graph.compile();

        } catch (GraphStateException e) {

            throw new RuntimeException(e.getMessage());

        }

    }
}
