package com.codeguard.backend.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	/**
	 * Groq Http Client
	 */

	@Bean
	public PoolingHttpClientConnectionManager groqConnectionManager() {
		return PoolingHttpClientConnectionManagerBuilder
				.create()
				.setMaxConnTotal(20)
				.setMaxConnPerRoute(10)
				.setDefaultConnectionConfig(
						org.apache.hc.client5.http.config.ConnectionConfig.custom()
								.setConnectTimeout(Timeout.ofSeconds(2)) // How long to
																			// establish
																			// the
																			// connection
																			// with
																			// the llm
																			// provider
								.build())
				.build();

	}

	@Bean(destroyMethod = "close")
	public CloseableHttpClient groqHttpClient(
			@Qualifier("groqConnectionManager") PoolingHttpClientConnectionManager connectionManager) {

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofSeconds(3)) // How long a request to wait for a connection
																	// from the pool
				.setResponseTimeout(Timeout.ofSeconds(60)) // How long to wait for the llm provider
															// response
				.build();

		return HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(requestConfig)
				.build();
	}

	@Bean
	public RestClient groqRestClient(
			@Qualifier("groqHttpClient") CloseableHttpClient httpClient) {

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);

		return RestClient.builder()
				.requestFactory(requestFactory)
				.build();
	}

	/**
	 * GitHub Http Client
	 * 
	 */

	@Bean
	public PoolingHttpClientConnectionManager gitHubConnectionManager() {
		return PoolingHttpClientConnectionManagerBuilder
				.create()
				.setMaxConnTotal(20)
				.setMaxConnPerRoute(10)
				.setDefaultConnectionConfig(
						org.apache.hc.client5.http.config.ConnectionConfig.custom()
								.setConnectTimeout(Timeout.ofSeconds(3)) // How long to
																			// establish
																			// the
																			// connection
																			// with
																			// the gitHub
																			// api
								.build())
				.build();
	}

	@Bean(destroyMethod = "close")
	public CloseableHttpClient gitHubHttpClient(
			@Qualifier("gitHubConnectionManager") PoolingHttpClientConnectionManager connectionManager) {

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofSeconds(3)) // How long to wait for a connection
																	// from the pool
				.setResponseTimeout(Timeout.ofSeconds(5)) // How long to wait for the gitHub
															// response
				.build();

		return HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(requestConfig)
				.build();
	}

	@Bean
	public RestClient gitHubRestClient(
			@Qualifier("gitHubHttpClient") CloseableHttpClient httpClient) {
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

		return RestClient
				.builder()
				.requestFactory(factory)
				.build();

	}
}
