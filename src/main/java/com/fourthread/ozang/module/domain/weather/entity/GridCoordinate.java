package com.fourthread.ozang.module.domain.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GridCoordinate {
    @Column(name = "grid_x")
    private Integer x;

    @Column(name = "grid_y")
    private Integer y;

    protected GridCoordinate() {}

    public GridCoordinate(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() { return x; }
    public Integer getY() { return y; }
}
