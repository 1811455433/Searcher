package com.seven.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Seven
 * @description 用来控制搜索界面的
 * @date 2019-10-18
 */
@Controller
public class SearchController {

    @GetMapping("/search")
    public String search(@RequestParam(name = "keyword") String keyword, Model model) {
        // 去掉字符串中的前导空白和后导空白
        keyword = keyword.trim();

        // 如果是空串，则返回到主页
        if (keyword.isEmpty()) {
            return "index";
        }

        model.addAttribute("keyword", keyword);
        return "search";
    }
}
