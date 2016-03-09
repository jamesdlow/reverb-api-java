package com.reverb.api;

import org.json.*;

public class ReverbAffiliates {
	//Constants
	public static final String AID = "_aid";
	public static final String HORIZONAL = "horizontal";
	public static final String VERTICAL = "vertical";
	
	public static String comparisonEmbed(JSONObject json, boolean horizontal, String affiliateid) {
		//Check if json is a result or 
		JSONObject item = ReverbApi.getFirstItem(json, ReverbApi.COMPARISON_SHOPPING_PAGES);
		JSONObject links = item.optJSONObject(ReverbApi.LINKS);
		JSONObject web = links.optJSONObject(ReverbApi.WEB);
		return comparisonEmbed(web.optString(ReverbApi.HREF), horizontal, affiliateid);
	}
	public static String comparisonEmbed(String weburl, boolean horizontal, String affiliateid) {
		//<iframe frameborder='0' id='iframe' scrolling='no' src='https://reverb.com/affiliates/comparison_shopping_embeds/bearfoot-fx-sea-blue-eq?_aid=geartube&orientation=horizontal' style='width: 100%; height: 300px;'></iframe>
		//?_aid=geartube&orientation=horizontal
		String suffix = weburl.substring(weburl.lastIndexOf('/')+1);
		return affiliateLink(ReverbApi.URL+"/affiliates/comparison_shopping_embeds/"+suffix+"?orientation="+(horizontal?HORIZONAL:VERTICAL), affiliateid);
	}
	public static String affiliateLink(String url, String affiliateid) {
		return url+(url.indexOf('?') < 0 ? '?' : '&')+AID+'='+HttpRequest.encode(affiliateid);
	}
}
