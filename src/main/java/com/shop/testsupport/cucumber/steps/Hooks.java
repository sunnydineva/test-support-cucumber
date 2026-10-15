package com.shop.testsupport.cucumber.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;

public class Hooks {

    @Before
    public void beforeScenario() {
        TestWorld.state().http().resetRequest();
        TestWorld.state().mocks().resetAll();
    }

    @After
    public void afterScenario() {
        TestWorld.state().mocks().stopAll();
        TestWorld.clear();
    }
}
