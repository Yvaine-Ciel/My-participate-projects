(function () {
    const STORAGE_KEY = "coviewFloatingControl";
    const status = document.getElementById("status");
    const details = document.getElementById("details");
    const toggle = document.getElementById("toggle");
    const clear = document.getElementById("clear");
    let current = null;

    function render() {
        if (!current || !current.roomId || !current.participantId) {
            status.textContent = "未配对";
            details.innerHTML = "<div>请先进入 CoView 房主房间。</div>";
            toggle.disabled = true;
            toggle.textContent = "启用";
            return;
        }

        status.textContent = current.enabled ? "已启用" : "已暂停";
        details.innerHTML = ""
            + "<div><strong>房间</strong><span>" + escapeHtml(current.roomId) + "</span></div>"
            + "<div><strong>服务</strong><span>" + escapeHtml(current.origin) + "</span></div>";
        toggle.disabled = false;
        toggle.textContent = current.enabled ? "暂停" : "启用";
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function save(next) {
        current = next;
        chrome.storage.local.set({[STORAGE_KEY]: current}, render);
    }

    chrome.storage.local.get(STORAGE_KEY, result => {
        current = result[STORAGE_KEY] || null;
        render();
    });

    toggle.addEventListener("click", () => {
        if (!current) {
            return;
        }
        save({...current, enabled: !current.enabled});
    });

    clear.addEventListener("click", () => {
        chrome.storage.local.remove(STORAGE_KEY, () => {
            current = null;
            render();
        });
    });
})();
