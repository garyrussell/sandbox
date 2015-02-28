/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.vertx;

import io.vertx.core.Vertx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;


/**
 * @author Gary Russell
 */
public class DemoService
{
    
    
    @Autowired
    private Vertx vertxInstance;

    private static final Log logger = LogFactory.getLog(DemoService.class);

    private final Map<String, AtomicInteger> clients = new HashMap<>();

    private final Map<String, AtomicInteger> paused = new HashMap<>();

    public void startStop(String command, @Header(IpHeaders.CONNECTION_ID) String connectionId)
    {
        
        if(vertxInstance == null)
        {
            System.out.println("ciao mondo");
        }
        
        
        if ("stop".equalsIgnoreCase(command))
        {
            AtomicInteger clientInt = clients.remove(connectionId);
            if (clientInt != null)
            {
                paused.put(connectionId, clientInt);
            }
            logger.info("Connection " + connectionId + " stopped");
        }
        else if ("start".equalsIgnoreCase(command))
        {
            AtomicInteger clientInt = paused.remove(connectionId);
            clientInt = clientInt == null ? new AtomicInteger() : clientInt;
            clients.put(connectionId, clientInt);
            logger.info("Connection " + connectionId + " (re)started");
        }
    }

    public List<Message< ? >> getNext()
    {
        List<Message< ? >> messages = new ArrayList<>();
        for (Entry<String, AtomicInteger> entry : clients.entrySet())
        {
            Message<String> message = MessageBuilder
                .withPayload(Integer.toString(entry.getValue().incrementAndGet()))
                .setHeader(IpHeaders.CONNECTION_ID, entry.getKey())
                .build();
            messages.add(message);
            logger.info("Sending " + message.getPayload() + " to connection " + entry.getKey());
        }
        return messages;
    }

}
