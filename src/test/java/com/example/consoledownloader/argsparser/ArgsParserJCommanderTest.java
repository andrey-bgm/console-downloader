package com.example.consoledownloader.argsparser;

import com.example.consoledownloader.lib.Options;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArgsParserJCommanderTest {

    private ArgsParserJCommander parser;

    @Before
    public void setUp() throws Exception {
        parser = new ArgsParserJCommander();
    }

    @Test
    public void parseAllPresent() throws Exception {
        Options options = parser.parse(split("-n 4 -l 10 -o output -f links -h -v"));
        assertThat(options.getThreadNumber()).isEqualTo(4);
        assertThat(options.getLimit()).isEqualTo(10);
        assertThat(options.getOutputDir()).isEqualTo("output");
        assertThat(options.getLinksFile()).isEqualTo("links");
        assertThat(options.isVerbose()).isTrue();
        assertThat(options.isHelpNeeded()).isTrue();
    }

    @Test
    public void parseWithDefaultValues() throws Exception {
        Options options = parser.parse(split("-f links"));
        assertThat(options.getThreadNumber()).isEqualTo(1);
        assertThat(options.getLimit()).isEqualTo(0);
        assertThat(options.getOutputDir()).isEmpty();
        assertThat(options.getLinksFile()).isEqualTo("links");
        assertThat(options.isVerbose()).isFalse();
        assertThat(options.isHelpNeeded()).isFalse();
    }

    @Test
    public void parseOnlyHelp() throws Exception {
        Options options = parser.parse("-h");
        assertThat(options.isHelpNeeded()).isTrue();
    }

    @Test
    public void parseLimitInBytes() throws Exception {
        Options options = parser.parse(split("-n 4 -l 10 -o output -f links"));
        assertThat(options.getLimit()).isEqualTo(10);
    }

    @Test
    public void parseLimitInKilobytes() throws Exception {
        Options options = parser.parse(split("-n 4 -l 10k -o output -f links"));
        assertThat(options.getLimit()).isEqualTo(10 * 1024);
    }

    @Test
    public void parseLimitInMegabytes() throws Exception {
        Options options = parser.parse(split("-n 4 -l 10m -o output -f links"));
        assertThat(options.getLimit()).isEqualTo(10 * 1024 * 1024);
    }

    @Test(expected = ArgsParseException.class)
    public void parseWrongLimit() throws Exception {
        parser.parse(split("-n 4 -l xxx -o output -f links"));
    }

    @Test(expected = ArgsParseException.class)
    public void failedParseWhenRequiredOptionIsMissing() throws Exception {
        parser.parse("");
    }
    
    private String[] split(String args) {
        return args.split(" ");
    }
}
