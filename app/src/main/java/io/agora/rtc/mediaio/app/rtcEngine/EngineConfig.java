package io.agora.rtc.mediaio.app.rtcEngine;

public class EngineConfig {
    public int mClientRole;

    public int mUid;

    public String mChannel;

    public void reset() {
        mChannel = null;
    }

    EngineConfig() {
    }
}
