package com.seven.community.controller;

import com.seven.community.Model.PageInfo;
import com.seven.community.api.Query;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Seven
 * @description 用来控制搜索界面的
 * @date 2019-10-18
 */
@Controller
public class SearchController {
    /**
     * 每页显示的条目数目，
     */
    private static int LIMIT = 15;

    /**
     * 查询结果的总页数
     */
    private long totalPageNum;

    /**
     * 查询结果的总条目数
     */
    private long totalItemNum;

    /**
     * 查询时长，毫秒
     */
    private long totalTookMillis;

    /**
     * cs连接
     */
    @Autowired
    private TransportClient client;

    /**
     * 根据关键字获取查询数据
     *
     * @param keyword        关键字
     * @param pageNum        第几页，每<code>LIMIT</code>项作为一页
     * @param domain         域名，将结果限定在某个域名
     * @param isMatchTitle   是否匹配标题中的关键字
     * @param isMatchContent 是否匹配内容中的关键字
     * @return 查询到的结果
     */
    private List<PageInfo> Query(String keyword, String domain, int pageNum
            , boolean isMatchTitle, boolean isMatchContent) {
        QueryBuilder queryKeyword;
        String[] multiMatchItems;

        // 如果不两个都不匹配，则使用默认：都匹配
        // 如果两个都匹配，则都匹配
        boolean matchAll = (!isMatchContent && !isMatchTitle) || (isMatchContent && isMatchTitle);
        if (matchAll) {
            multiMatchItems = new String[]{"content", "title"};
        } else if (isMatchContent) {
            multiMatchItems = new String[]{"content"};
        } else {
            multiMatchItems = new String[]{"title"};
        }

        queryKeyword = QueryBuilders.multiMatchQuery(keyword, multiMatchItems);

        SearchResponse response;

        // 如果域名约束不为空，则启用域名约束，否则不启用
        if (!"".equals(domain)) {
            QueryBuilder queryUrl = QueryBuilders.matchQuery("url", domain);
            response = client.prepareSearch("page")
                    .setTypes("_doc")
                    .setQuery(QueryBuilders
                            .boolQuery()
                            .must(queryKeyword)
                            .must(queryUrl))
                    .setFrom((pageNum - 1) * LIMIT)
                    .setSize(LIMIT)
                    .get();
        } else {
            response = client.prepareSearch("page")
                    .setTypes("_doc")
                    .setQuery(queryKeyword)
                    .setFrom((pageNum - 1) * LIMIT)
                    .setSize(LIMIT)
                    .get();
        }

        totalTookMillis = response.getTook().getMillis();
        totalItemNum = response.getHits().totalHits;
        totalPageNum = totalItemNum / LIMIT + (totalItemNum % LIMIT == 0 ? 0 : 1);

        List<PageInfo> pageInfos = new ArrayList<>();
        for (SearchHit result :
                response.getHits().getHits()) {
            Map<String, Object> fields = result.getSourceAsMap();
            pageInfos.add(new PageInfo(
                    fields.get("title"),
                    fields.get("content"),
                    fields.get("url"),
                    fields.get("content_type"),
                    fields.get("update_date"),
                    fields.get("weight")
            ));
        }

        return pageInfos;
    }

    /**
     * 搜索界面的控制类
     *
     * @param keyword      查询关键字
     * @param domain       限制域名，默认不限制
     * @param model        向前端传数据
     * @param pageNumStr   页码每页显示<code>LIMIT</code>条数据
     * @param matchTitle   是否匹配标题中的关键字
     * @param matchContent 是否匹配内容中的关键字
     * @return 结果
     */
    @GetMapping("/search")
    public String search(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "domain", defaultValue = "") String domain,
            @RequestParam(name = "pageNum", defaultValue = "1") String pageNumStr,
            @RequestParam(name = "matchTitle", defaultValue = "true") String matchTitle,
            @RequestParam(name = "matchContent", defaultValue = "true") String matchContent,
            Model model) {

        totalPageNum = 0;
        totalItemNum = 0;
        totalTookMillis = 0;

        boolean isMatchTitle = Boolean.parseBoolean(matchTitle);
        boolean isMatchContent = Boolean.parseBoolean(matchContent);

        // 如果关键字为空，查询结果应该为空
        //  实际上，ElasticSearch关键字为空时，会列出所有结果，
        //  不因该让这件事发生，所以返回空结果
        if ("".equals(keyword)) {
            model.addAttribute("keyword", keyword)
                    .addAttribute("domain", domain)
                    .addAttribute("pageNum", pageNumStr)
                    .addAttribute("matchTitle", isMatchTitle)
                    .addAttribute("matchContent", isMatchContent)
                    // 返回空的结果
                    .addAttribute("pageInfos", new ArrayList<PageInfo>())
                    .addAttribute("totalPageNum", totalPageNum)
                    .addAttribute("totalItemNum", totalItemNum)
                    .addAttribute("totalTookMillis", totalTookMillis);
            return "search";
        }

        // 如果页码格式错误（页码为大于0的整数），则使用默认值1
        int pageNumInt = 1;
        try {
            pageNumInt = Integer.parseInt(pageNumStr);
            if (pageNumInt <= 0) {
                pageNumInt = 1;
            }
        } catch (Exception ignored) {
        }

        model.addAttribute("keyword", keyword)
                .addAttribute("domain", domain)
                .addAttribute("pageNum", pageNumInt)
                // 返回空的结果
                .addAttribute("pageInfos", Query(keyword, domain, pageNumInt, isMatchTitle, isMatchContent))
                .addAttribute("totalPageNum", totalPageNum)
                .addAttribute("totalItemNum", totalItemNum)
                .addAttribute("totalTookMillis", totalTookMillis);
        return "search";
    }
}
