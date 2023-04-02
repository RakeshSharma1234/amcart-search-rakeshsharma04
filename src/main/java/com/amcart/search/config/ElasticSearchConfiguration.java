package com.amcart.search.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties("es")
@Getter
@Setter
public class ElasticSearchConfiguration {
	private String hostname;
	private int port;
	private String username;
	private String password;
	private String scheme;
	
	@Bean
	public RestClient getRestClient() {
		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

		RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, port, scheme))
				.setHttpClientConfigCallback(
						httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
		return builder.build();
	}

	@Bean
	public ElasticsearchClient getElasticsearchClient() {
		ElasticsearchClient client = new ElasticsearchClient(new RestClientTransport(getRestClient(), new JacksonJsonpMapper()));
		return client;
	}
	
}