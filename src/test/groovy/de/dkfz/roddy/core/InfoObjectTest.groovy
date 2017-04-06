/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import groovy.transform.CompileStatic;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by heinold on 04.04.17.
 */
@CompileStatic
class InfoObjectTest {
    @Test
    void parseTimestampString() {
        // Please note that 117 is equal to 2017 at the end! new Date takes the year 1900 as a base! So 1900 + 117 == 2017
        assert InfoObject.parseTimestampString("171102_1402") == new Date(117, 10, 02, 14, 02, 00)
        Date d1 = InfoObject.parseTimestampString("171102_14020322")
        Date d2 = new Date(117, 10, 02, 14, 02, 03)
        assert d1.year == d2.year
        assert d1.getMonth() == d2.getMonth()
        assert d1.day == d2.day
    }
}