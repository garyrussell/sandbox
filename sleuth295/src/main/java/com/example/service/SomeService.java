/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.service;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import com.example.Bar;
import com.example.Baz;
import com.example.Foo;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
@MessagingGateway()
public interface SomeService {

	@Gateway(requestChannel = "toAmqp", replyTimeout = 10000,
			payloadExpression = "new Object[] { #args[0], #args[1] }")
	Baz process(Foo foo, Bar bar);

}
