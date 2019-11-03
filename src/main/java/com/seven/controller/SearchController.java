package com.seven.controller;

import com.seven.model.PageInfo;
import com.seven.model.DataHelper;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
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

    private DataHelper dataHelper = new DataHelper();

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
     * @param usePr          是否使用PageRank排序
     * @return 查询到的结果
     */
    private List<PageInfo> query(String keyword, String domain, int pageNum
            , boolean isMatchTitle, boolean isMatchContent, boolean usePr) {
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

        // 关键字查询
        queryKeyword = QueryBuilders.multiMatchQuery(keyword, multiMatchItems);

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch("page")
                .setTypes("_doc")
                .setFrom((pageNum - 1) * LIMIT)
                .setSize(LIMIT);

        // 排序
        if(usePr) {
            // 使用降序排序，如果缺少值，则使用0代替
            SortBuilder sortBuilder = SortBuilders.fieldSort("weight").missing(0.0).order(SortOrder.DESC);
            searchRequestBuilder.addSort(sortBuilder);
        }

        // 如果域名约束不为空，则启用域名约束，否则不启用
        if (!"".equals(domain)) {
            QueryBuilder queryUrl = QueryBuilders.matchQuery("url", domain);
            searchRequestBuilder.setQuery(
                    QueryBuilders
                            .boolQuery()
                            .must(queryKeyword)
                            .must(queryUrl)
            );
        } else {
            searchRequestBuilder.setQuery(queryKeyword);
        }


        SearchResponse response = searchRequestBuilder.get();

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
     * @param keyword     查询关键字
     * @param domain      限制域名，默认不限制
     * @param model       向前端传数据
     * @param pageNumStr  页码每页显示<code>LIMIT</code>条数据
     * @param matchOption 匹配规则，00或11=匹配标题和内容，01=只匹配内容，10=只匹配标题
     * @param sortOrder   排序方式，1=使用PageRank值排序，0=使用默认排序（倒排索引）
     * @return 结果
     */
    @GetMapping("/search")
    public String search(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "domain", defaultValue = "") String domain,
            @RequestParam(name = "pageNum", defaultValue = "1") String pageNumStr,
            @RequestParam(name = "matchOption", defaultValue = "11") String matchOption,
            @RequestParam(name = "sortOrder", defaultValue = "0") String sortOrder,
            Model model) {

        // 是否使用PageRank排序
        boolean usePr = "1".equals(sortOrder);
        // 如果不是0或者1，就使用默认值0
        if (!"0".equals(sortOrder) && !"1".equals(sortOrder)) {
            sortOrder = "0";
        }

        boolean matchTitle = true, matchContent = true;
        // 判断长度，如果不是两个字符，则使用默认值
        if (matchOption.length() == 2) {
            matchContent = matchOption.charAt(0) == '1';
            matchTitle = matchOption.charAt(1) == '1';

            // 如果既不匹配标题，又不匹配内容，则使用默认值
            if (!matchTitle && !matchContent) {
                matchContent = true;
                matchTitle = true;
            }
        }


        totalPageNum = 0;
        totalItemNum = 0;
        totalTookMillis = 0;

        // 如果关键字为空，查询结果应该为空
        //  实际上，ElasticSearch关键字为空时，会列出所有结果，
        //  不因该让这件事发生，所以返回空结果
        if ("".equals(keyword)) {
            model.addAttribute("keyword", keyword)
                    .addAttribute("domain", domain)
                    .addAttribute("pageNum", pageNumStr)
                    .addAttribute("matchOption", matchOption)
                    .addAttribute("sortOrder", sortOrder)
                    // 返回空的结果
                    .addAttribute("pageInfos", new ArrayList<PageInfo>())
                    .addAttribute("totalPageNum", totalPageNum)
                    .addAttribute("totalItemNum", totalItemNum)
                    .addAttribute("totalTookMillis", totalTookMillis)
                    .addAttribute("dataHelper", dataHelper);
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
                .addAttribute("matchOption", matchOption)
                .addAttribute("sortOrder", sortOrder)
                // 返回空的结果
                .addAttribute("pageInfos", query(keyword, domain, pageNumInt, matchTitle, matchContent, usePr))
                .addAttribute("totalPageNum", totalPageNum)
                .addAttribute("totalItemNum", totalItemNum)
                .addAttribute("totalTookMillis", totalTookMillis)
                .addAttribute("dataHelper", dataHelper);
        return "search";
    }
}
