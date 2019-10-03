package org.actioncontroller.meta;

import org.actioncontroller.client.ApiClientExchange;

/**
 * Returned by {@link HttpParameterMapperFactory} to be used with
 * {@link org.actioncontroller.client.ApiClientProxy} to convert
 * method arguments into HTTP request information.
 */
@FunctionalInterface
public interface HttpClientParameterMapper {
    void apply(ApiClientExchange exchange, Object arg);
}
