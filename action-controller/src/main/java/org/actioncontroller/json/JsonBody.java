package org.actioncontroller.json;

import org.actioncontroller.meta.HttpParameterMapping;
import org.actioncontroller.meta.HttpRequestParameterMapping;
import org.actioncontroller.meta.HttpRequestParameterMappingFactory;
import org.actioncontroller.meta.HttpReturnValueMapping;
import org.actioncontroller.meta.HttpReturnMapperFactory;
import org.actioncontroller.meta.HttpReturnMapping;
import org.jsonbuddy.JsonNode;
import org.jsonbuddy.parse.JsonParser;
import org.jsonbuddy.pojo.JsonGenerator;
import org.jsonbuddy.pojo.PojoMapper;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({PARAMETER, METHOD})
@HttpParameterMapping(JsonBody.JsonRequestMapper.class)
@HttpReturnMapping(JsonBody.JsonResponseMapper.class)
public @interface JsonBody {

    class ReturnMapperFactory implements HttpReturnMapperFactory<JsonBody> {
        private static HttpReturnValueMapping writeJsonNode = (o, resp, req) -> {
            resp.setContentType("application/json");
            ((JsonNode) o).toJson(resp.getWriter());
        };

        private static HttpReturnValueMapping writePojo = (o, resp, req) -> {
            resp.setContentType("application/json");
            JsonGenerator.generate(o).toJson(resp.getWriter());
        };

        @Override
        public HttpReturnValueMapping create(JsonBody annotation, Class<?> returnType) {
            if (!JsonNode.class.isAssignableFrom(returnType)) {
                return writePojo;
            } else {
                return writeJsonNode;
            }
        }
    }

    class JsonRequestMapper implements HttpRequestParameterMapping {

        private static HttpRequestParameterMapping readJsonNode = (req, pathParams) -> JsonParser.parse(req.getReader());

        private HttpRequestParameterMapping responseMapping;

        public JsonRequestMapper(JsonBody jsonBody, Parameter parameter) {
            if (JsonNode.class.isAssignableFrom(parameter.getType())) {
                responseMapping = readJsonNode;
            } else if (List.class.isAssignableFrom(parameter.getType())) {
                responseMapping = readJsonNode;
            } else {
                responseMapping = (req, u) -> PojoMapper.map(
                        JsonParser.parseToObject(req.getReader()),
                        parameter.getType()
                );
            }
        }

        @Override
        public Object apply(HttpServletRequest req, Map<String, String> u) throws IOException {
            return responseMapping.apply(req, u);
        }
    }

}

