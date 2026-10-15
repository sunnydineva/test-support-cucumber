package com.shop.testsupport.cucumber.steps;

public final class TestWorld {

    private static final ThreadLocal<SharedTestState> STATE = ThreadLocal.withInitial(SharedTestState::new);

    private TestWorld() {
    }

    public static SharedTestState state() {
        return STATE.get();
    }

    public static void clear() {
        STATE.remove();
    }
}
