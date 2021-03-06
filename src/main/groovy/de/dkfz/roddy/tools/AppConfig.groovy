/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import de.dkfz.roddy.StringConstants

/**
 * Java basically provides an ini file loading class. When using this class, there were several problems:
 * - The entries order in ini files is not permanent. Upon load and write, the order changes.
 * - Comments cannot be stored in those ini files
 */
@groovy.transform.CompileStatic
public class AppConfig {

    private class Entry {

        private String line;

        private int lineNo;

        private String key;

        private List<String> values;

        private String value;

        private String comment;

        public Entry(int lineNumber, String line) {
            lineNo = lineNumber;
            this.line = line;

            if(!isContent())
                return;

            setLine(line);
        }

        public boolean isEmpty() {
            return line.trim().size() == 0 || isComment();
        }

        public boolean isHeader() {
            return line.trim().startsWith(StringConstants.SBRACKET_LEFT) && line.trim().endsWith(StringConstants.SBRACKET_RIGHT);
        }

        public boolean isComment() {
            return line.trim().startsWith(StringConstants.HASH);
        }

        public boolean isContent() {
            return !(isEmpty() || isHeader() || isComment());
        }

        public String getType() {
            return isContent() ? "CONTENT" : isHeader() ? "HEADER" : isComment() ? "COMMENT" : isEmpty() ? "EMPTY" : "UNKNOWN";
        }

        void setLine(String s) {
            this.line = s;
            def splitLineByHash = line.split(StringConstants.SPLIT_HASH) as ArrayList<String>
            if (splitLineByHash.size() > 1) {
                comment = splitLineByHash.drop(1).inject {
                    acc, val -> acc + StringConstants.SPLIT_HASH + val
                }.trim()

            };
            String[] splitline = splitLineByHash[0].trim().split(StringConstants.SPLIT_EQUALS, 2)
            key = splitline[0];

            if(splitline.size() > 1) {
                values = splitline[1].split("[,:]") as List;
                value = splitline[1];
            } else {
                values = [ "" ]
                value = ""
            }
        }
    }

    private File appIniFile;

    /**
     * A copy of the entries in the ini file.
     */
    private final Map<String, Entry> entriesByKey = [:];

    private List<Entry> allEntries = [];

    public AppConfig() {
        appIniFile = null;
    }

    public AppConfig(String file) {
        this(new File(file));
    }

    public AppConfig(Map<String, Entry> config) {
        this.entriesByKey = config
    }

    public AppConfig(File file) {
        this(file.readLines() as String[]);
        this.appIniFile = file;
    }

    public AppConfig(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Entry e = new Entry(i, line);
            allEntries << e;
            if(e.isContent())
                entriesByKey[e.key] = e;
        }
    }

    public String toString() {
        println(appIniFile);

        for (def entry in allEntries) {
            println entry.getType() + "\t" + entry.line;
        }
    }

    public boolean containsKey(String key) {
        return entriesByKey.containsKey(key);
    }

    public void setProperty(String key, String value) {
        if(!containsKey(key))
            entriesByKey[key] = new Entry(-1, key + "=" + value);
        else
            entriesByKey[key].setLine(key + "=" + value);
    }

    public String getProperty(String key, String defaultvalue) {
        if(!containsKey(key))
            return defaultvalue;
        return entriesByKey[key].value
    }
}
