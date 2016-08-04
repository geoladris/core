package org.fao.unredd.portal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.IOUtils;

public class LayersModuleConfigurationProvider
		implements
			ModuleConfigurationProvider {

	@Override
	public Map<String, JSONObject> getConfigurationMap(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		HashMap<String, JSONObject> ret = new HashMap<String, JSONObject>();
		fillConfigMap(configurationContext, request, ret);
		return ret;
	}

	@Override
	public Map<String, JSON> getConfigMap(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		HashMap<String, JSON> ret = new HashMap<String, JSON>();
		fillConfigMap(configurationContext, request, ret);
		return ret;
	}

	private <T extends JSON> void fillConfigMap(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request, Map<String, T> ret) throws IOException {
		String id = request.getParameter("mapId");
		if (id == null) {
			id = "";
		}
		String layersTemplate = IOUtils.toString(
				new File(configurationContext.getConfigurationDirectory(),
						"layers" + id + ".json").toURI(),
				"UTF-8");
		@SuppressWarnings("unchecked")
		T layersContent = (T) JSONSerializer
				.toJSON(configurationContext.localize(layersTemplate));

		ret.put("layers", layersContent);
	}

	@Override
	public boolean canBeCached() {
		return true;
	}
}
