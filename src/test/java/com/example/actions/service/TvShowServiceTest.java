package com.example.actions.service;

import com.example.actions.model.TvShow;
import com.example.actions.repository.TvShowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TvShowServiceTest {

    @Mock
    private TvShowRepository tvShowRepository;

    @InjectMocks
    private TvShowService tvShowService;

    @Test
    void getAllTvShows_returnsAllShows() {
        List<TvShow> shows = List.of(
                new TvShow(1L, "Breaking Bad", "Drama", 2008),
                new TvShow(2L, "The Office", "Comedy", 2005)
        );
        when(tvShowRepository.findAll()).thenReturn(shows);

        List<TvShow> result = tvShowService.getAllTvShows();

        assertEquals(2, result.size());
        assertEquals("Breaking Bad", result.get(0).getTitle());
        assertEquals("The Office", result.get(1).getTitle());
    }

    @Test
    void getAllTvShows_returnsEmptyList_whenNoShows() {
        when(tvShowRepository.findAll()).thenReturn(List.of());

        List<TvShow> result = tvShowService.getAllTvShows();

        assertEquals(0, result.size());
    }
}
