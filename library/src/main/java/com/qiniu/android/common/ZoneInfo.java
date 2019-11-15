package com.qiniu.android.common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jemy on 17/04/2017.
 */

public class ZoneInfo {
    private static int DOMAIN_FROZEN_SECONDS = 10 * 60;
    //upHost
    public final List<String> upDomainsList;
    //upHost -> frozenTillTimestamp
    public final Map<String, Long> upDomainsMap;
    private final int ttl;
    public final List<Integer> listHosts;


    public ZoneInfo(int ttl, List<String> upDomainsList, Map<String, Long> upDomainsMap, List<Integer> listHosts) {
        this.ttl = ttl;
        this.upDomainsList = upDomainsList;
        this.upDomainsMap = upDomainsMap;
        this.listHosts = listHosts;
    }

    /**
     *
     * @param obj Not allowed to be null
     * @return
     * @throws JSONException
     */
    public static ZoneInfo buildFromJson(JSONObject obj) throws JSONException {
        JSONArray hostArray = obj.getJSONArray("hosts");
        List<String> domainsList = new ArrayList<>();
        ConcurrentHashMap<String, Long> domainsMap = new ConcurrentHashMap<>();
        List<Integer> listHost = new ArrayList<>();
        int ttl = 0;
        for (int j = 0; j < hostArray.length(); j++) {
            int hosts = 0;
            JSONObject arrObj = hostArray.getJSONObject(j);
            JSONObject upObj = arrObj.getJSONObject("up");
            ttl = arrObj.getInt("ttl");
            String[] upDomainTags = new String[]{"acc", "src", "old_acc", "old_src"};
            for (String tag : upDomainTags) {
                JSONObject tagRootObj = upObj.getJSONObject(tag);
                JSONArray tagMainObj = tagRootObj.getJSONArray("main");
                for (int i = 0; i < tagMainObj.length(); i++) {
                    String upDomain = tagMainObj.getString(i);
                    domainsList.add(upDomain);
                    domainsMap.put(upDomain, 0L);
                    hosts += 1;
                }
            }

            for (String tag : upDomainTags) {
                try {
                    JSONObject tagRootObj = upObj.getJSONObject(tag);
                    JSONArray tagBackupObj = tagRootObj.getJSONArray("backup");
                    if (tagBackupObj != null) {
                        //this backup tag is optional
                        for (int i = 0; i < tagBackupObj.length(); i++) {
                            String upHost = tagBackupObj.getString(i);
                            domainsList.add(upHost);
                            domainsMap.put(upHost, 0L);
                            hosts += 1;
                        }
                    }
                } catch (JSONException ex) {
                    //some zone has not backup domain, just ignore here
                }
            }
            listHost.add(hosts);
        }

        return new ZoneInfo(ttl, domainsList, domainsMap, listHost);
    }

    public void frozenDomain(String domain) {
        //frozen for 10 minutes
        upDomainsMap.put(domain, System.currentTimeMillis() / 1000 + DOMAIN_FROZEN_SECONDS);
    }

    @Override
    public String toString() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("ttl", this.ttl);
        m.put("upDomainList", this.upDomainsList);
        m.put("upDomainMap", this.upDomainsMap);
        m.put("listHosts", this.listHosts);
        return new JSONObject(m).toString();
    }
}
