package com.drumigo.mobile.data.model.history;

import java.util.List;

public class PageResponse<T> {
    public List<T> content;
    public int totalElements;
    public int totalPages;
    public int size;
    public int number;
}
