package com.example.consoledownloader.argsparser;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.example.consoledownloader.lib.Options;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgsParserJCommander implements ArgsParser {

    @Parameter(names = {LINK_FILE_SHORT, LINK_FILE_LONG}, required = LINK_FILE_REQUIRED)
    private String input;

    @Parameter(names = { OUTPUT_DIR_SHORT, OUTPUT_DIR_LONG }, required = OUTPUT_DIR_REQUIRED)
    private String output;

    @Parameter(names = {THREAD_NUMBER_SHORT, THREAD_NUMBER_LONG}, required = THREAD_NUMBER_REQUIRED)
    private int threads;

    @Parameter(names = { SPEED_LIMIT_SHORT, SPEED_LIMIT_LONG }, required = SPEED_LIMIT_REQUIRED,
        converter = SpeedLimitConverter.class)
    private long limit;

    @Parameter(names = { VERBOSE_SHORT, VERBOSE_LONG })
    private boolean verbose;

    @Parameter(names = { HELP_SHORT, HELP_LONG }, help = true)
    private boolean help;

    @Override
    public Options parse(String... args) throws ArgsParseException {
        JCommander jCommander = new JCommander();
        jCommander.addObject(this);

        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            throw new ArgsParseException(e.getMessage(), e);
        }

        return new Options.Builder(this.input)
            .outputDir(this.output)
            .limit(this.limit)
            .threads(this.threads)
            .verbose(this.verbose)
            .help(this.help)
            .build();
    }

    public static class SpeedLimitConverter implements IStringConverter<Integer> {

        static final Pattern PATTERN = Pattern.compile("^(\\d+)([km]?)$");
        static final Map<String, Integer> MULTIPLIERS;

        static {
            MULTIPLIERS = new HashMap<>();
            MULTIPLIERS.put("", 1);
            MULTIPLIERS.put("k", 1024);
            MULTIPLIERS.put("m", 1024 * 1024);
        }

        @Override
        public Integer convert(String value) {
            Matcher matcher = PATTERN.matcher(value.toLowerCase());

            if (!matcher.matches()) {
                throw new ParameterException(String.format(
                    "Value \"%s\" doesn't match the speed limit pattern", value));
            }

            String rawLimit = matcher.group(1);
            String suffix = matcher.group(2);

            return Integer.parseInt(rawLimit) * MULTIPLIERS.get(suffix);
        }
    }
}
