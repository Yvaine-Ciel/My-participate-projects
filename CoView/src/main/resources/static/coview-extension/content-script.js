(function () {
    const STORAGE_KEY = "coviewFloatingControl";
    const ROOM_HASH_PATTERN = /^#\/room\/([A-Za-z0-9-]+)$/;
    const CONTROL_ROOT_ID = "coview-extension-floating-control";

    function normalizeOrigin(origin) {
        return String(origin || "").replace(/\/$/, "");
    }

    function participantKey(roomId) {
        return "coview:participant:" + roomId;
    }

    function detectRoomSession() {
        const match = window.location.hash.match(ROOM_HASH_PATTERN);
        if (!match) {
            return null;
        }

        const roomId = decodeURIComponent(match[1]);
        let participantId = "";
        try {
            participantId = window.sessionStorage.getItem(participantKey(roomId)) || "";
        } catch (ignored) {
        }
        if (!roomId || !participantId) {
            return null;
        }

        return {
            enabled: true,
            origin: normalizeOrigin(window.location.origin),
            roomId,
            participantId,
            pairedAt: Date.now()
        };
    }

    function isCoViewRoomPage(config) {
        return config
            && normalizeOrigin(config.origin) === normalizeOrigin(window.location.origin)
            && ROOM_HASH_PATTERN.test(window.location.hash);
    }

    function removeInjectedControl() {
        const host = document.getElementById(CONTROL_ROOT_ID);
        if (host) {
            host.remove();
        }
        const script = document.getElementById("coview-extension-control-script");
        if (script) {
            script.remove();
        }
        const boot = document.getElementById("coview-extension-control-boot");
        if (boot) {
            boot.remove();
        }
    }

    function injectControl(config) {
        if (!config || !config.enabled || !config.origin || !config.roomId || !config.participantId) {
            removeInjectedControl();
            return;
        }
        if (isCoViewRoomPage(config)) {
            removeInjectedControl();
            return;
        }
        if (document.getElementById(CONTROL_ROOT_ID)) {
            return;
        }

        const host = document.createElement("div");
        host.id = CONTROL_ROOT_ID;
        document.documentElement.appendChild(host);

        const script = document.createElement("script");
        script.id = "coview-extension-control-script";
        script.src = normalizeOrigin(config.origin) + "/external-control.js?ts=" + Date.now();
        script.onload = () => {
            const boot = document.createElement("script");
            boot.id = "coview-extension-control-boot";
            boot.textContent = "window.CoViewExternalControl&&window.CoViewExternalControl.open("
                + JSON.stringify({
                    origin: normalizeOrigin(config.origin),
                    roomId: config.roomId,
                    participantId: config.participantId
                })
                + ");";
            document.documentElement.appendChild(boot);
            boot.remove();
        };
        script.onerror = () => {
            host.remove();
        };
        document.documentElement.appendChild(script);
    }

    function pairFromCoViewRoom() {
        const session = detectRoomSession();
        if (!session) {
            return;
        }
        chrome.storage.local.set({[STORAGE_KEY]: session});
    }

    function loadAndInject() {
        chrome.storage.local.get(STORAGE_KEY, result => {
            injectControl(result[STORAGE_KEY]);
        });
    }

    pairFromCoViewRoom();
    loadAndInject();

    window.addEventListener("hashchange", () => {
        pairFromCoViewRoom();
        loadAndInject();
    });

    chrome.storage.onChanged.addListener((changes, areaName) => {
        if (areaName !== "local" || !changes[STORAGE_KEY]) {
            return;
        }
        injectControl(changes[STORAGE_KEY].newValue);
    });
})();
