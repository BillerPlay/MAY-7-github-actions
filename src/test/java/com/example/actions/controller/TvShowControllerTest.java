package com.example.actions.controller;

import com.example.actions.model.TvShow;
import com.example.actions.service.TvShowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TvShowControllerTest {

    @Mock
    private TvShowService tvShowService;

    @InjectMocks
    private TvShowController tvShowController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tvShowController).build();
    }

    @Test
    void getAll_returnsOkWithShows() throws Exception {
        when(tvShowService.getAllTvShows()).thenReturn(List.of(
                new TvShow(1L, "Breaking Bad", "Drama", 2008),
                new TvShow(2L, "The Office", "Comedy", 2005)
        ));

        mockMvc.perform(get("/tvshows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Wrong Title"))
                .andExpect(jsonPath("$[1].title").value("The Office"));
    }

    @Test
    void getAll_returnsEmptyArray_whenNoShows() throws Exception {
        when(tvShowService.getAllTvShows()).thenReturn(List.of());

        mockMvc.perform(get("/tvshows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
