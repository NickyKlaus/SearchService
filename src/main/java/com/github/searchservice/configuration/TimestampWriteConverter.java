package com.github.searchservice.configuration;

import org.springframework.core.convert.converter.Converter;

import javax.annotation.Nullable;
import java.util.Date;

public class TimestampWriteConverter implements Converter<Date, Long> {

    @Override
    public Long convert(@Nullable Date date) {
        if (date != null) {
            return date.getTime();
        }
        return null;
    }
}
