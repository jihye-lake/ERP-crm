package com.nexora.erp.web.dto;

import java.time.LocalDate;

/**
 * 원본 index.asp / monitor.asp의 curPage, page_size, searchProject, searchType1(연도),
 * searchType2(월), surType(검색모드: 1=연/월, 2=프로젝트코드/Kcode) 쿼리스트링을 대체하는 폼 객체.
 */
public class ProjectSearchForm {

    private int curPage = 1;
    private int pageSize = 100;
    private String searchProject = "";
    private String searchType1;   // 연도, 예: "2026"
    private String searchType2;   // 월, 예: "05"
    private String surType = "1"; // 1: 연/월 검색, 2: 프로젝트코드/Kcode 검색

    public static ProjectSearchForm withDefaults() {
        ProjectSearchForm form = new ProjectSearchForm();
        LocalDate now = LocalDate.now();
        form.searchType1 = String.valueOf(now.getYear());
        form.searchType2 = String.format("%02d", now.getMonthValue());
        return form;
    }

    public boolean isKeywordMode() {
        return "2".equals(surType) && searchProject != null && !searchProject.isBlank();
    }

    public int getCurPage() { return curPage; }
    public void setCurPage(int curPage) { this.curPage = curPage <= 0 ? 1 : curPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize <= 0 ? 100 : pageSize; }

    public String getSearchProject() { return searchProject; }
    public void setSearchProject(String searchProject) { this.searchProject = searchProject; }

    public String getSearchType1() { return searchType1; }
    public void setSearchType1(String searchType1) { this.searchType1 = searchType1; }

    public String getSearchType2() { return searchType2; }
    public void setSearchType2(String searchType2) { this.searchType2 = searchType2; }

    public String getSurType() { return surType; }
    public void setSurType(String surType) { this.surType = surType; }
}
