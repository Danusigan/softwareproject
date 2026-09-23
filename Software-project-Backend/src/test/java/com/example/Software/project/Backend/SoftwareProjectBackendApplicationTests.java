package com.example.Software.project.Backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Explicit rather than relying on src/test/resources/application.properties alone: this
// property is set there too, but something about running in the same Surefire fork as other
// test classes (this suite runs last, after several @DataJpaTest classes with their own naming
// strategy - or lack of one) meant it wasn't reliably in effect by the time this context built,
// producing "missing table [StudentMark]" (unconverted) instead of the real table "studentmark"
// - a different symptom than the file being absent entirely (which produces the snake_cased
// "student_mark" instead). Setting it directly on the test guarantees it regardless.
@SpringBootTest(properties = {
		"spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"
})
class SoftwareProjectBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
