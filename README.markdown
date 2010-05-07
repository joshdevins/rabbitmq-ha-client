RabbitMQ HA Client
==================

Some AMQP brokers and specifically RabbitMQ do not support HA out of the box. Rationale for this varies as much as peoples' requirements do, so it's not super surprising that this is the case. However there are basic HA possibilities with RabbitMQ, specifically active-passive brokers using [Pacemaker](http://www.rabbitmq.com/pacemaker.html) or behind a plain old TCP load balancer. For a better description of the latter scenario, please read the [blog post](http://www.joshdevins.net/2010/04/16/rabbitmq-ha-testing-with-haproxy) that started this project. Suffice it to say that in order to make this and many HA topologies work, a client that can do automatic, graceful connection recovery and message redelivery is required. Bonus points of course if you can auto-magically de-duplicate messages in the consumer as is done in [Beetle](http://github.com/xing/beetle) (TBD in this project).

Work in Progress
----------------

This is a major work in progress still! Watch this project and this page for regular updates on what has been completed and is still to be done.

Completed:

* creating a new connection with no broker running (this blocks synchronously right now, so lazily create your connections!)
* publishing messages after broker has gone down
* publishing messages after broker has restarted
* callbacks to listeners on: connection, connection failure, reconnection, reconnection failure, disconnection

Working on:

* support for blocking consumers (yes, this is currently only tested from the publisher side!)
* always adding tests of course

Still to be done:

* tests, waaaay more tests :)
* documentation and examples, specifically what to do on connection and reconnection events (queue declaration, etc.)
* handling of ack's and transactions that need to happen after a reconnection
* hook in message receipt path to do message deduplication 
* more customizability and tuning for reconnection values

Usage
-----

Basically this is a drop-in replacement for the standard RabbitMQ ConnectionFactory. Anything that uses that should be able to use the HaConnectionFactory instead. Be certain to review the retry strategies (there are some built-in) if you want custom behaviour on channel failures.

More details to come soon...

License
-------

Copyright 2010 [Josh Devins](http://www.joshdevins.net)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. 

Resources
---------

This RabbitMQ HA client internally makes use of the standard [RabbitMQ AMQP client](http://www.rabbitmq.com/java-client.html) and has borrowed ideas and inspiration from the following sources. Please respect their licenses.

* [RabbitMQ Java messagepaterns library, v0.1.3](http://hg.rabbitmq.com/rabbitmq-java-messagepatterns)
* [Spring Framework v3.0.x](http://static.springsource.org/spring/docs/3.0.x/spring-framework-reference/html/jms.html)
