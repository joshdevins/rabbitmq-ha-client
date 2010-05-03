package net.joshdevins.spring.amqp.rabbit;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/META-INF/spring/applicationContext.xml")
public class RabbitTemplateTest {

	@Autowired
	private RabbitTemplate template;

	private String date;

	@Before
	public void before() {

		date = new Date().toString();
	}

	@Test
	public void testPublish() {

		// broker stop_app -- exception
		// broker stop -- exception
		// broker kill -- exception
		// broker stop_app; start_app -- no failure
		// broker stop; start -- no failure
		// broker kill; start -- no failure
		template.convertAndSend(date + " : 1");

		// broker stop_app -- exception
		// broker stop -- exception
		// broker kill -- exception
		template.convertAndSend(date + " : 2");

		// broker start_app -- exception
		// broker start -- exception
		// broker kill -- exception
		template.convertAndSend(date + " : 3");
	}
}