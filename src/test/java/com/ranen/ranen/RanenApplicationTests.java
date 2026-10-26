package com.ranen.ranen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "sms.provider=console")
class RanenApplicationTests {

	@Test
	void contextLoads() {
	}
}
