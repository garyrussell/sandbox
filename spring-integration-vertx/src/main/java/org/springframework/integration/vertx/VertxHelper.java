package org.springframework.integration.vertx;

import java.util.Optional;

import org.springframework.messaging.Message;

/**
 * 
 * @author fbalicchia
 *
 */
public class VertxHelper
{

    public static Optional<Object> getVertxBody(Message< ? > message)
    {
        return Optional.ofNullable(message.getPayload());

    }

}
