package io.github.notthatlonely.mechkeys.core;

import org.junit.Assert;
import org.junit.Test;

public class SelfTestJUnit {
    @Test public void allChecksPass() throws Exception { Assert.assertEquals("self-test failures (see output above)", 0, SelfTest.run()); }
}
