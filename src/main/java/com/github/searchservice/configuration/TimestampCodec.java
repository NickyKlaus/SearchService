package com.github.searchservice.configuration;

import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.driver.internal.core.util.Strings;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.netty.util.concurrent.FastThreadLocal;

import java.nio.ByteBuffer;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

public class TimestampCodec implements TypeCodec<Date> {
    private static final String[] DATE_STRING_PATTERNS =
            new String[] {
                    // 1) date-time patterns separated by 'T'
                    // (declared first because none of the others are ISO compliant, but some of these are)
                    // 1.a) without time zone
                    "yyyy-MM-dd'T'HH:mm",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    // 1.b) with ISO-8601 time zone
                    "yyyy-MM-dd'T'HH:mmX",
                    "yyyy-MM-dd'T'HH:mmXX",
                    "yyyy-MM-dd'T'HH:mmXXX",
                    "yyyy-MM-dd'T'HH:mm:ssX",
                    "yyyy-MM-dd'T'HH:mm:ssXX",
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    // 1.c) with generic time zone
                    "yyyy-MM-dd'T'HH:mm z",
                    "yyyy-MM-dd'T'HH:mm:ss z",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS z",
                    // 2) date-time patterns separated by whitespace
                    // 2.a) without time zone
                    "yyyy-MM-dd HH:mm",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    // 2.b) with ISO-8601 time zone
                    "yyyy-MM-dd HH:mmX",
                    "yyyy-MM-dd HH:mmXX",
                    "yyyy-MM-dd HH:mmXXX",
                    "yyyy-MM-dd HH:mm:ssX",
                    "yyyy-MM-dd HH:mm:ssXX",
                    "yyyy-MM-dd HH:mm:ssXXX",
                    "yyyy-MM-dd HH:mm:ss.SSSX",
                    "yyyy-MM-dd HH:mm:ss.SSSXX",
                    "yyyy-MM-dd HH:mm:ss.SSSXXX",
                    // 2.c) with generic time zone
                    "yyyy-MM-dd HH:mm z",
                    "yyyy-MM-dd HH:mm:ss z",
                    "yyyy-MM-dd HH:mm:ss.SSS z",
                    // 3) date patterns without time
                    // 3.a) without time zone
                    "yyyy-MM-dd",
                    // 3.b) with ISO-8601 time zone
                    "yyyy-MM-ddX",
                    "yyyy-MM-ddXX",
                    "yyyy-MM-ddXXX",
                    // 3.c) with generic time zone
                    "yyyy-MM-dd z"
            };

    private final FastThreadLocal<SimpleDateFormat> parser;

    private final FastThreadLocal<SimpleDateFormat> formatter;

    /**
     * Creates a new {@code TimestampCodec} that uses the system's {@linkplain ZoneId#systemDefault()
     * default time zone} to parse timestamp literals that do not include any time zone information.
     */
    public TimestampCodec() {
        this(ZoneId.systemDefault());
    }

    /**
     * Creates a new {@code TimestampCodec} that uses the given {@link ZoneId} to parse timestamp
     * literals that do not include any time zone information.
     */
    public TimestampCodec(ZoneId defaultZoneId) {
        parser =
                new FastThreadLocal<>() {
                    @Override
                    protected SimpleDateFormat initialValue() {
                        SimpleDateFormat parser = new SimpleDateFormat();
                        parser.setLenient(false);
                        parser.setTimeZone(TimeZone.getTimeZone(defaultZoneId));
                        return parser;
                    }
                };
        formatter =
                new FastThreadLocal<>() {
                    @Override
                    protected SimpleDateFormat initialValue() {
                        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                        parser.setTimeZone(TimeZone.getTimeZone(defaultZoneId));
                        return parser;
                    }
                };
    }

    @NonNull
    @Override
    public GenericType<Date> getJavaType() {
        return GenericType.of(Date.class);
    }

    @NonNull
    @Override
    public DataType getCqlType() {
        return DataTypes.TIMESTAMP;
    }

    @Override
    public boolean accepts(@NonNull Object value) {
        return value instanceof Date;
    }

    @Override
    public boolean accepts(@NonNull Class<?> javaClass) {
        return javaClass == Date.class;
    }

    @Nullable
    @Override
    public ByteBuffer encode(@Nullable Date value, @NonNull ProtocolVersion protocolVersion) {
        return (value == null)
                ? null
                : TypeCodecs.BIGINT.encodePrimitive(value.getTime(), protocolVersion);
    }

    @Nullable
    @Override
    public Date decode(@Nullable ByteBuffer bytes, @NonNull ProtocolVersion protocolVersion) {
        return (bytes == null || bytes.remaining() == 0)
                ? null
                : new Date(TypeCodecs.BIGINT.decodePrimitive(bytes, protocolVersion));
    }

    @NonNull
    @Override
    public String format(@Nullable Date value) {
        return (value == null) ? "NULL" : Strings.quote(formatter.get().format(value.toString()));
    }

    @Nullable
    @Override
    public Date parse(@Nullable String value) {
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("NULL")) {
            return null;
        }
        String unquoted = Strings.unquote(value);
        if (Strings.isLongLiteral(unquoted)) {
            // Numeric literals may be quoted or not
            try {
                return new Date(Long.parseLong(unquoted));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format("Cannot parse timestamp value from \"%s\"", value));
            }
        } else {
            // Alphanumeric literals must be quoted
            if (!Strings.isQuoted(value)) {
                throw new IllegalArgumentException(
                        String.format("Alphanumeric timestamp literal must be quoted: \"%s\"", value));
            }
            SimpleDateFormat parser = this.parser.get();
            TimeZone timeZone = parser.getTimeZone();
            ParsePosition pos = new ParsePosition(0);
            for (String pattern : DATE_STRING_PATTERNS) {
                parser.applyPattern(pattern);
                pos.setIndex(0);
                try {
                    Date date = parser.parse(unquoted, pos);
                    if (date != null && pos.getIndex() == unquoted.length()) {
                        return date;
                    }
                } finally {
                    // restore the parser's default time zone, it might have been modified by the call to
                    // parse()
                    parser.setTimeZone(timeZone);
                }
            }
            throw new IllegalArgumentException(
                    String.format("Cannot parse timestamp value from \"%s\"", value));
        }
    }
}
