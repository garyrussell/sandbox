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

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import com.example.Bar;
import com.example.Baz;
import com.example.Foo;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class SomeServiceImpl implements SomeService {

	@Override
	public Baz process(@Payload Foo foo, @Header("bar") Bar bar) {
		return new Baz(foo.getField() + bar.getField());
	}

}
