package com.seven.community.Model;

/**
 * @author Seven
 * @description f
 * @date 2019-10-21
 */
public class PageInfo {
    private String title;
    private String content;
    private String url;
    private String contentType;
    private String updateDate;
    private double weight;

    public PageInfo(Object title, Object content, Object url, Object contentType, Object updateDate, Object weight) {
        this.title = title == null ? "" : title.toString();
        this.content = content == null ? "" : content.toString();
        if (!"".equals(content)) {
            if (this.content.length() > 200) {
                this.content = this.content.substring(0, 200);
            }
        }
        this.url = url == null ? "" : url.toString();
        this.contentType = contentType == null ? "" : contentType.toString();
        this.updateDate = updateDate == null ? "" : updateDate.toString();

        try {
            this.weight = Double.parseDouble(weight.toString());
        } catch (Exception ignored) {
            this.weight = 0.0;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
