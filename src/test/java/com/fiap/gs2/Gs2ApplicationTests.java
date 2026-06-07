package com.fiap.gs2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"conservation-area.import-on-startup=false",
		"prodes.import-on-startup=false"
})
class Gs2ApplicationTests {

	@Test
	void contextLoads() {
	}

}
