package com.verve.VerveSampleProject.api.controller;

import java.util.List;

public class PostRequestDTO {
    List<Integer> listOfIds;
    int uniqueRequestsCount;


    public PostRequestDTO(List<Integer> listOfIds, int uniqueRequestsCount) {
        this.listOfIds = listOfIds;
        this.uniqueRequestsCount = uniqueRequestsCount;
    }
}
