package com.hz.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Created by David on 22-Oct-17.
 */
@Configuration
@RequiredArgsConstructor
@Log4j2
public class EnphaseRestClientConfig {

    public static final String SYSTEM = "/home.json";
    public static final String INVENTORY = "/inventory.json?deleted=1";
    public static final String PRODUCTION = "/production.json?details=1";
    public static final String CONTROLLER = "/info.xml";
    public static final String WIFI_INFO = "/admin/lib/wireless_display.json?site_info=0";
	public static final String WAN_INFO = "/admin/lib/network_display.json";
	public static final String METER_STREAM = "/stream/meter";  // needs installer user and password
	public static final String DEVICE_METERS = "/ivp/meters";
	public static final String POWER_METERS = "/ivp/meters/readings";

    private final EnphaseCollectorProperties config;

	@Bean
	public RestTemplate enphaseRestTemplate(RestTemplateBuilder builder) {

		log.info("Reading from insecure Envoy controller endpoint {}", config.getController().getUrl());

		HttpClient httpClient = HttpClients
				.custom()
				.useSystemProperties()
				.setRetryHandler(new StandardHttpRequestRetryHandler(3, true) {
					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {

						if (executionCount <= this.getRetryCount()) {
							// These exceptions appear in the logs occasionally, the envoy device probably needs some more time
							if (exception instanceof UnknownHostException || exception instanceof HttpHostConnectException || exception instanceof SocketTimeoutException) {
								try {
									TimeUnit.SECONDS.sleep(15);
								} catch (InterruptedException e) {
								}
								return true;
							}
						}
						return super.retryRequest(exception, executionCount, context);
					}
				})
				.build();
		
		return builder
				.rootUri(config.getController().getUrl())
				.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(30))
				.requestFactory(() -> new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient)))
				.build();
	}

	/**
	 * Needed for /ivp/meters and /ivp/meters/readings
	 * @return customer converter to handle json as application octect stream
	 */
	@Bean
	public HttpMessageConverters customConverters() {
		MappingJackson2HttpMessageConverter octetStreamConverter = new MappingJackson2HttpMessageConverter();
		octetStreamConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
		return new HttpMessageConverters(octetStreamConverter);
	}

}
