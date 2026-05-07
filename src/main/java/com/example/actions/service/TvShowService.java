package com.example.actions.service;

import com.example.actions.model.TvShow;
import com.example.actions.repository.TvShowRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TvShowService {

    private final TvShowRepository tvShowRepository;

    public TvShowService(TvShowRepository tvShowRepository) {
        this.tvShowRepository = tvShowRepository;
    }

    public List<TvShow> getAllTvShows() {
        return tvShowRepository.findAll();
    }
}
