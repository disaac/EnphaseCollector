package com.hz;

import com.hz.metrics.Metric;
import com.hz.models.envoy.json.System;
import com.hz.models.envoy.xml.EnvoyDevice;
import com.hz.models.envoy.xml.EnvoyInfo;
import com.hz.models.envoy.xml.EnvoyPackage;
import com.hz.services.EnphaseService;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 0,stubs="classpath:/stubs/D4.2.27")
@ActiveProfiles("testing")
public class EnphaseServiceRest_4_2_27_Test {

	@TestConfiguration
	static class EmployeeServiceImplTestContextConfiguration {

		@Autowired
		private Environment environment;

		@Autowired
		private RestTemplateBuilder restTemplateBuilder;

		@Bean
		public RestTemplate enphaseRestTemplate() {
			return restTemplateBuilder
					.rootUri("http://localhost:" + this.environment.getProperty("wiremock.server.port"))
					.setConnectTimeout(Duration.ofSeconds(5))
					.setReadTimeout(Duration.ofSeconds(30))
					.build();
		}

		@Bean
		public RestTemplate enphaseSecureRestTemplate() {
			return restTemplateBuilder
					.rootUri("http://localhost:" + this.environment.getProperty("wiremock.server.port"))
					.setConnectTimeout(Duration.ofSeconds(5))
					.setReadTimeout(Duration.ofSeconds(30))
					.build();
		}

		@Bean
		public Unmarshaller enphaseMarshaller() {
			Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
			marshaller.setClassesToBeBound(EnvoyInfo.class, EnvoyPackage.class, EnvoyDevice.class);
			return marshaller;
		}

		@Bean
		public EnphaseService enphaseService(RestTemplate enphaseRestTemplate, Unmarshaller enphaseMarshaller) {
			return new EnphaseService(enphaseRestTemplate, enphaseRestTemplate, enphaseMarshaller);
		}
	}

	@Autowired
	private EnphaseService enphaseService;

	@Test
	public void EnphaseNonProdServiceTest() {

		Optional<System> system = this.enphaseService.collectEnphaseData();
		Assert.assertTrue(system.isPresent());
		Assert.assertThat(this.enphaseService.getSoftwareVersion(), Matchers.equalTo("D4.2.27"));
		Assert.assertThat(this.enphaseService.getSerialNumber(), Matchers.equalTo("121617XXXXXX"));
		Assert.assertThat(system.get().getProduction().getInverter().get().getActiveCount(), Matchers.equalTo(20));
		Assert.assertThat(system.get().getProduction().getProductionEim().get().getWattsLifetime(), Matchers.comparesEqualTo(BigDecimal.valueOf(12605195.311)));
		Assert.assertThat(system.get().getProduction().getProductionEim().get().getWattsNow(), Matchers.comparesEqualTo(BigDecimal.valueOf(-1.707)));
		Assert.assertThat(system.get().getProduction().getBatteryList().size(), Matchers.equalTo(0));
		Assert.assertThat(this.enphaseService.isOk(), Matchers.equalTo(true));

		List<Metric> metrics = this.enphaseService.getMetrics(system.get());

		Assert.assertThat(metrics.size(), Matchers.equalTo(24));
	}

}