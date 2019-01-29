package com.longcoding.undefined.services.internal;

import com.longcoding.undefined.exceptions.ExceptionType;
import com.longcoding.undefined.exceptions.GeneralException;
import com.longcoding.undefined.helpers.*;
import com.longcoding.undefined.models.cluster.AppSync;
import com.longcoding.undefined.models.cluster.WhitelistIpSync;
import com.longcoding.undefined.models.ehcache.AppInfo;
import com.longcoding.undefined.models.enumeration.SyncType;
import com.longcoding.undefined.models.internal.EnrollApp;
import com.longcoding.undefined.models.internal.EnrollWhitelistIp;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import redis.clients.jedis.Jedis;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by longcoding on 19. 1. 17..
 */

@Slf4j
@Service
public class AppService {

    @Autowired
    JedisFactory jedisFactory;

    @Autowired
    ClusterSyncUtil clusterSyncUtil;

    public AppInfo createApp(EnrollApp enrollApp) {
        AppInfo appInfo;
        try (Jedis jedis = jedisFactory.getInstance()) {
            Long totalApps = jedis.hlen(Constant.REDIS_KEY_INTERNAL_APP_INFO);

            appInfo = convertedEnrollAppToAppInfo(enrollApp);
            appInfo.setAppId(totalApps.toString());
            appInfo.setValid(true);
            appInfo.setApiKey(createUniqueApiKey().toString());

            AppSync appSync = new AppSync(SyncType.CREATE, appInfo);
            jedis.hset(Constant.REDIS_KEY_INTERNAL_APP_INFO, String.valueOf(totalApps), JsonUtil.fromJson(appInfo));

            clusterSyncUtil.setexInfoToHealthyNode(Constant.REDIS_KEY_APP_UPDATE, Constant.SECOND_OF_HOUR, JsonUtil.fromJson(appSync));
        }

        return appInfo;
    }

    public AppInfo getAppInfo(@PathVariable String appId) {
        try (Jedis jedis = jedisFactory.getInstance()) {
            String appInfo = jedis.hget(Constant.REDIS_KEY_INTERNAL_APP_INFO, appId);
            if (Strings.isNotEmpty(appInfo)) return JsonUtil.fromJson(appInfo, AppInfo.class);
            else throw new GeneralException(ExceptionType.E_1004_RESOURCE_NOT_FOUND);
        }
    }

    public boolean deleteApp(@PathVariable String appId) {
        try (Jedis jedis = jedisFactory.getInstance()) {
            AppInfo appInfo = new AppInfo();
            appInfo.setAppId(appId);

            clusterSyncUtil.setexInfoToHealthyNode(Constant.REDIS_KEY_APP_UPDATE, Constant.SECOND_OF_HOUR, JsonUtil.fromJson(new AppSync(SyncType.DELETE, appInfo)));
            Long id = jedis.hdel(Constant.REDIS_KEY_INTERNAL_APP_INFO, appId);
            return id == 1;
        }
    }

    public boolean expireApiKey(@PathVariable String appId) {
        try (Jedis jedis = jedisFactory.getInstance()) {
            String appInfoInString = jedis.hget(Constant.REDIS_KEY_INTERNAL_APP_INFO, appId);
            if (Strings.isNotEmpty(appInfoInString)) {
                AppInfo appInfo = JsonUtil.fromJson(appInfoInString, AppInfo.class);
                appInfo.setApiKey(Strings.EMPTY);

                clusterSyncUtil.setexInfoToHealthyNode(Constant.REDIS_KEY_APP_UPDATE, Constant.SECOND_OF_HOUR, JsonUtil.fromJson(new AppSync(SyncType.UPDATE, appInfo)));
                return jedis.hset(Constant.REDIS_KEY_INTERNAL_APP_INFO, String.valueOf(appInfo.getAppId()), JsonUtil.fromJson(appInfo)) == 1;
            } throw new GeneralException(ExceptionType.E_1004_RESOURCE_NOT_FOUND);
        }
    }


    public AppInfo refreshApiKey(@PathVariable String appId) {
        try (Jedis jedis = jedisFactory.getInstance()) {
            String appInfoInString = jedis.hget(Constant.REDIS_KEY_INTERNAL_APP_INFO, appId);
            if (Strings.isNotEmpty(appInfoInString)) {
                AppInfo appInfo = JsonUtil.fromJson(appInfoInString, AppInfo.class);
                appInfo.setApiKey(createUniqueApiKey().toString());
                jedis.hset(Constant.REDIS_KEY_INTERNAL_APP_INFO, String.valueOf(appInfo.getAppId()), JsonUtil.fromJson(appInfo));
                clusterSyncUtil.setexInfoToHealthyNode(Constant.REDIS_KEY_APP_UPDATE, Constant.SECOND_OF_HOUR, JsonUtil.fromJson(new AppSync(SyncType.UPDATE, appInfo)));
                return appInfo;
            } throw new GeneralException(ExceptionType.E_1004_RESOURCE_NOT_FOUND);
        }
    }
    //TODO
    public boolean removeWhiteIps(EnrollWhitelistIp enrollWhitelistIp) {
        WhitelistIpSync ipSync = new WhitelistIpSync(SyncType.DELETE, String.valueOf(enrollWhitelistIp.getAppId()), enrollWhitelistIp.getRequestIps());
        clusterSyncUtil.setexInfoToHealthyNode(Constant.REDIS_KEY_APP_WHITELIST_UPDATE, Constant.SECOND_OF_HOUR, JsonUtil.fromJson(ipSync));
        return true;
    }

    //TODO
    public boolean addWhiteIps(EnrollWhitelistIp enrollWhitelistIp) {
        WhitelistIpSync ipSync = new WhitelistIpSync(SyncType.UPDATE, String.valueOf(enrollWhitelistIp.getAppId()), enrollWhitelistIp.getRequestIps());
        clusterSyncUtil.setexInfoToHealthyNode(Constant.REDIS_KEY_APP_WHITELIST_UPDATE, Constant.SECOND_OF_HOUR, JsonUtil.fromJson(ipSync));
        return true;
    }

    private static UUID createUniqueApiKey() {
        byte[] uuidBySystemCurrentTimeMillis = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(System.currentTimeMillis()).array();
        return UUID.nameUUIDFromBytes(uuidBySystemCurrentTimeMillis);
    }

    private AppInfo convertedEnrollAppToAppInfo(EnrollApp enrollApp) {
        return AppInfo.builder()
                .appId(String.valueOf(enrollApp.getAppId()))
                .appName(enrollApp.getAppName())
                .appIpAcl(enrollApp.getAppIpAcl())
                .apiKey(enrollApp.getApiKey())
                .dailyRateLimit(String.valueOf(enrollApp.getAppDailyRateLimit()))
                .minutelyRateLimit(String.valueOf(enrollApp.getAppMinutelyRateLimit()))
                .valid(true)
                .build();
    }

}
