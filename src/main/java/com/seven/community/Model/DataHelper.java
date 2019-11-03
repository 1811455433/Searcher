package com.seven.community.Model;

import com.seven.community.ESConfig;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.xml.crypto.Data;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Seven
 * @description 数据的工具类
 * @date 2019-10-25
 */
public class DataHelper {
    /**
     * ElasticSearch 中的数据总数
     */
    private long numOfAllData;

    private Logger logger = Logger.getLogger(DataHelper.class.toString());

    /**
     * 每隔 1 小时查询一下结果总数
     */
    private static long PERIOD = 1;

    private Date lastUpdateDate;

    /**
     * cs连接
     */
    private TransportClient client;

    public DataHelper() {

        Runnable work = new Runnable() {
            @Override
            public void run() {
                try {
                    if(client == null) {
                        client = new ESConfig().client();
                    }
                    QueryBuilder query = QueryBuilders.matchAllQuery();
                        SearchResponse response = client.prepareSearch("page")
                                .setTypes("_doc")
                                .setQuery(query)
                                .setFrom(0)
                                .setSize(0)
                                .get();
                    numOfAllData = response.getHits().totalHits;
                    lastUpdateDate = new Date();
                    logger.info("更新了数据总数：" + numOfAllData);
                } catch (Exception ignored) {
                }
            }
        };
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                1, new BasicThreadFactory.Builder().namingPattern(" timing Execution-%d").daemon(false).build());
        // 第一个参数是任务，第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间,第四个参数是时间单位
        scheduledThreadPoolExecutor.scheduleAtFixedRate(work, 0L, PERIOD, TimeUnit.HOURS);
    }

    public long getNumOfAllData() {
        return numOfAllData;
    }

    public String getLastUpdateDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastUpdateDate);
    }
}
