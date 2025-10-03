package com.example.test_framework_api.rabbitmq;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping(value = "/rabbit-test")
public class RabbitMQProducerController {
  @Autowired
  private AmqpTemplate rabbiTemplate;

  @GetMapping(value = "/produced")
  public String producer() {
    rabbiTemplate.convertAndSend(RabbitmqApplication.EXCHANGE, RabbitmqApplication.ROUTING_KEY,
        "Hello World example !!! through RabbitMQ");
    return "Data produced";
  }

}
