package com.github.searchservice.configuration;

import org.springframework.core.convert.converter.Converter;

import javax.annotation.Nullable;

import java.util.Date;

public class TimestampReadConverter implements Converter<Long, Date> {

    @Override
    public Date convert(@Nullable Long timestamp) {
        if (timestamp != null) {
            return new Date(timestamp);
        }
        return null;
    }
}
