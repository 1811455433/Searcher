ACS_Design          // 主目录
    Data            // 数据
        Raw         // 未处理
            1.json
            2.json
        Cooked      // 已处理
            1.json
            2.json
    DataCollection  // 数据收集
        *.py
    DataProcess     // 数据处理
        *.py
    WebApp          // 网站
        *
# Java代码
## 如果修改了页面的信息
需要修改的项目如下：
- PageInfo
- search.html

# ElasticSearch
## 创建索引
```
"analyzer": "ik_max_word"

{
    "settings": {
        "number_of_shards": "5",
        "number_of_replicas": "0"
    },
    "mappings": {
        "properties": {
            "title": {
                "type": "text"
            },
            "weight": {
                "type": "double"
            },
            "content" : {
            	"type" : "text"	
            },
            "content_type": {
                "type": "text"
            },
            "url": {
                "type": "text"
            },
            "update_date": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
            }
        }
    }
}
```

## 查询

- term
```
GET 127.0.0.1/page/_search
{
    "from" : 0,     // 从第一个开始
    "size" : 5,     // 的5个结果
    "query" : {
        "term" : {
            "content" : "西文"
        }
    }
}

GET 127.0.0.1/page/_search
{
    "from" : 0,     // 从第一个开始
    "size" : 5,     // 的5个结果
    "query" : {
        "terms" : {
            "content" : ["西文", "中文"]
        }
    }
}

```

- match（使用分词器）
```
// 查询所有
GET 127.0.0.1/page/_search
{
    "query" : {
        "match_all" : {}
    }
}
```
```
// 短语匹配，严格匹配
GET 127.0.0.1/page/_search
{
    "from" : 0,     // 从第一个开始
    "size" : 5,     // 的5个结果
    "query" : {
        "match_parse" : {
            "content" : "西文"
        }
    }
}
```
```
// 短语匹配，严格匹配
GET 127.0.0.1/page/_search
{
    "from" : 0,     // 从第一个开始
    "size" : 5,     // 的5个结果
    "query" : {
        "match" : {
            "content" : "西文"
        }
    }
}
```