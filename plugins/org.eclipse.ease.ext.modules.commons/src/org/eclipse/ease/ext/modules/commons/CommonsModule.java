package org.eclipse.ease.ext.modules.commons;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.WrapToScript;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CommonsModule extends AbstractScriptModule {
	
	@WrapToScript
	public String mapToJSON(Map<String, String> stringMap) throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(stringMap);
	}
	
	@WrapToScript
	public String prettyJSON(String json) throws JsonProcessingException, IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(json);
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
	}
	
	@WrapToScript
	public Map<String,String> jsonToMap(String json) throws JsonParseException, JsonMappingException, IOException {
		TypeReference<HashMap<String,String>> mapTypeRef = new TypeReference<HashMap<String,String>>() {
		};
		
		return new ObjectMapper().readValue(json, mapTypeRef);
	}

}
