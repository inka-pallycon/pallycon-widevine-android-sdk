package com.pallycon.androidtvsample;

import java.util.ArrayList;
import java.util.List;

public class MovieGroup {

    public String title;
    public List<Movie> arrMovie;

    MovieGroup(String title) {
        this.title = title;
        this.arrMovie = new ArrayList<>();
    }
}
