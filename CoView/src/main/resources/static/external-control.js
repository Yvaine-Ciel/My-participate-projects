(function () {
    const existing = window.CoViewExternalControl;
    if (existing && existing.close) {
        existing.close();
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function wsUrl(origin, roomId, participantId) {
        const url = new URL(origin);
        url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
        url.pathname = "/ws/rooms/" + encodeURIComponent(roomId);
        url.search = "?participantId=" + encodeURIComponent(participantId);
        return url.toString();
    }

    function api(origin, path, options) {
        return fetch(origin.replace(/\/$/, "") + path, {
            headers: {"Content-Type": "application/json"},
            ...options
        }).then(async response => {
            const text = await response.text();
            const data = text ? JSON.parse(text) : null;
            if (!response.ok) {
                throw new Error(data && data.message ? data.message : "CoView request failed");
            }
            return data;
        });
    }

    function createControl(config) {
        const state = {
            origin: config.origin.replace(/\/$/, ""),
            roomId: config.roomId,
            participantId: config.participantId,
            room: null,
            messages: [],
            joinRequests: [],
            collapsed: false,
            wsStatus: "连接中",
            notice: "",
            seenCount: 0,
            socket: null,
            pollTimer: 0
        };

        const host = document.createElement("div");
        host.id = "coview-external-control-root";
        const shadow = host.attachShadow({mode: "open"});
        document.documentElement.appendChild(host);

        const style = document.createElement("style");
        style.textContent = `
            :host { all: initial; }
            *, *::before, *::after { box-sizing: border-box; }
            .toggle, .panel {
                position: fixed;
                right: 22px;
                bottom: 22px;
                z-index: 2147483647;
                font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            }
            .toggle {
                display: grid;
                place-items: center;
                width: 58px;
                height: 58px;
                border: 1px solid rgba(255,255,255,.55);
                border-radius: 999px;
                color: white;
                background: rgba(45,108,223,.88);
                box-shadow: 0 18px 44px rgba(0,0,0,.28);
                backdrop-filter: blur(16px);
                cursor: pointer;
                font-size: 24px;
                font-weight: 900;
            }
            .dot {
                position: absolute;
                top: 8px;
                right: 8px;
                width: 12px;
                height: 12px;
                border: 2px solid white;
                border-radius: 999px;
                background: #e02424;
            }
            .panel {
                display: grid;
                grid-template-rows: auto auto auto minmax(0, 1fr) auto;
                gap: 10px;
                width: min(390px, calc(100vw - 28px));
                max-height: min(78vh, 720px);
                padding: 12px;
                border: 1px solid rgba(255,255,255,.58);
                border-radius: 10px;
                color: #182230;
                background: rgba(255,255,255,.76);
                box-shadow: 0 22px 56px rgba(0,0,0,.28);
                backdrop-filter: blur(18px);
            }
            .header, .status, .actions, .row {
                display: flex;
                align-items: center;
                gap: 8px;
            }
            .header { justify-content: space-between; }
            .title strong, .title span { display: block; }
            .title span, .status, .meta { color: #667085; font-size: 12px; }
            button {
                border: 1px solid rgba(152,162,179,.7);
                border-radius: 7px;
                padding: 7px 10px;
                color: #182230;
                background: rgba(255,255,255,.82);
                cursor: pointer;
                font: inherit;
            }
            button.primary {
                border-color: #2d6cdf;
                color: #fff;
                background: #2d6cdf;
            }
            button.danger { color: #b42318; }
            .notice {
                border: 1px solid rgba(234,179,8,.35);
                border-radius: 8px;
                padding: 8px;
                color: #854d0e;
                background: rgba(254,243,199,.72);
                font-size: 13px;
            }
            .requests, .members, .chat {
                border: 1px solid rgba(208,213,221,.75);
                border-radius: 8px;
                padding: 9px;
                background: rgba(255,255,255,.68);
            }
            .requests { display: grid; gap: 8px; }
            .request { display: grid; gap: 7px; }
            .request strong, .member strong { display: block; }
            .chat {
                display: grid;
                grid-template-rows: minmax(0, 1fr) auto;
                min-height: 250px;
            }
            .list {
                overflow: auto;
                display: grid;
                align-content: start;
                gap: 8px;
                padding-right: 2px;
            }
            .msg {
                border-radius: 8px;
                padding: 8px;
                background: rgba(242,244,247,.88);
                font-size: 13px;
            }
            .msg.mine { background: rgba(219,234,254,.92); }
            .text { margin-top: 3px; white-space: pre-wrap; word-break: break-word; }
            form {
                display: grid;
                grid-template-columns: minmax(0, 1fr) auto;
                gap: 7px;
                margin-top: 8px;
            }
            input {
                width: 100%;
                border: 1px solid rgba(152,162,179,.75);
                border-radius: 7px;
                padding: 8px;
                font: inherit;
            }
            .members {
                max-height: 150px;
                overflow: auto;
                display: grid;
                gap: 7px;
            }
            .member {
                display: flex;
                justify-content: space-between;
                gap: 8px;
                font-size: 13px;
            }
        `;
        shadow.appendChild(style);

        const root = document.createElement("div");
        shadow.appendChild(root);

        function alertCount() {
            return state.messages.length + state.joinRequests.length;
        }

        function send(payload) {
            if (state.socket && state.socket.readyState === WebSocket.OPEN) {
                state.socket.send(JSON.stringify(payload));
            }
        }

        function render() {
            const hasAlert = state.collapsed && alertCount() > state.seenCount;
            if (state.collapsed) {
                root.innerHTML = `<button class="toggle" title="展开 CoView 浮窗">C${hasAlert ? '<span class="dot"></span>' : ""}</button>`;
                root.querySelector(".toggle").addEventListener("click", () => {
                    state.collapsed = false;
                    state.seenCount = alertCount();
                    render();
                });
                return;
            }

            const room = state.room;
            const requests = state.joinRequests.map(request => `
                <div class="request" data-request-id="${escapeHtml(request.id)}">
                    <div>
                        <strong>${escapeHtml(request.displayName)}</strong>
                        <span class="meta">${escapeHtml(request.message || "等待房主确认")}</span>
                    </div>
                    <div class="row">
                        <button class="primary approve">同意</button>
                        <button class="reject">拒绝</button>
                    </div>
                </div>
            `).join("");
            const messages = state.messages.map(message => `
                <div class="msg ${message.senderId === state.participantId ? "mine" : ""}">
                    <div class="meta">${escapeHtml(message.senderName || "成员")} · ${message.owner ? "房主" : "房客"}</div>
                    <div class="text">${escapeHtml(message.text)}</div>
                </div>
            `).join("");
            const members = room ? room.participants.map(participant => `
                <div class="member">
                    <strong>${escapeHtml(participant.displayName)}</strong>
                    <span class="meta">${participant.owner ? "房主" : "房客"}</span>
                </div>
            `).join("") : "";

            root.innerHTML = `
                <aside class="panel">
                    <div class="header">
                        <div class="title">
                            <strong>CoView 房主浮窗</strong>
                            <span>${escapeHtml(state.roomId)}</span>
                        </div>
                        <div class="actions">
                            <button class="collapse">收起</button>
                            <button class="close">关闭</button>
                        </div>
                    </div>
                    <div class="status">
                        <span>${escapeHtml(state.wsStatus)}</span>
                        <span>${room ? room.participants.length : 0} 人</span>
                        <span>${room && room.screenShareActive ? "共享中" : "未共享"}</span>
                        ${state.joinRequests.length ? `<strong style="color:#b42318">${state.joinRequests.length} 个申请</strong>` : ""}
                    </div>
                    ${state.notice ? `<div class="notice">${escapeHtml(state.notice)}</div>` : ""}
                    ${state.joinRequests.length ? `<section class="requests">${requests}</section>` : ""}
                    <section class="chat">
                        <div class="list">${messages || '<div class="meta">还没有消息</div>'}</div>
                        <form>
                            <input maxlength="500" placeholder="输入消息">
                            <button class="primary">发送</button>
                        </form>
                    </section>
                    <section class="members">${members || '<div class="meta">正在读取成员</div>'}</section>
                </aside>
            `;

            root.querySelector(".collapse").addEventListener("click", () => {
                state.collapsed = true;
                state.seenCount = alertCount();
                render();
            });
            root.querySelector(".close").addEventListener("click", () => close());
            root.querySelectorAll(".request").forEach(item => {
                const requestId = item.getAttribute("data-request-id");
                item.querySelector(".approve").addEventListener("click", () => decide(requestId, true));
                item.querySelector(".reject").addEventListener("click", () => decide(requestId, false));
            });
            const form = root.querySelector("form");
            const input = root.querySelector("input");
            form.addEventListener("submit", event => {
                event.preventDefault();
                const text = input.value.trim();
                if (!text) {
                    return;
                }
                send({type: "chat", text});
                input.value = "";
            });
            const list = root.querySelector(".list");
            list.scrollTop = list.scrollHeight;
        }

        async function loadRoom() {
            try {
                state.room = await api(state.origin, "/api/rooms/" + encodeURIComponent(state.roomId));
                render();
            } catch (error) {
                state.notice = error.message;
                render();
            }
        }

        async function loadJoinRequests() {
            try {
                state.joinRequests = await api(state.origin, "/api/rooms/" + encodeURIComponent(state.roomId)
                    + "/join-requests?participantId=" + encodeURIComponent(state.participantId));
                render();
            } catch (error) {
                state.notice = error.message;
                render();
            }
        }

        async function decide(requestId, approved) {
            try {
                const result = await api(state.origin, "/api/rooms/" + encodeURIComponent(state.roomId)
                    + "/join-requests/" + encodeURIComponent(requestId) + "/decision", {
                    method: "POST",
                    body: JSON.stringify({participantId: state.participantId, approved})
                });
                state.joinRequests = state.joinRequests.filter(request => request.id !== requestId);
                if (result.room) {
                    state.room = result.room;
                    send({type: "room-refresh"});
                }
                render();
            } catch (error) {
                state.notice = error.message;
                render();
            }
        }

        function connect() {
            if (state.socket) {
                state.socket.close();
            }
            state.socket = new WebSocket(wsUrl(state.origin, state.roomId, state.participantId));
            state.wsStatus = "连接中";
            render();

            state.socket.onopen = () => {
                state.wsStatus = "已连接";
                render();
            };
            state.socket.onclose = () => {
                state.wsStatus = "已断开";
                render();
            };
            state.socket.onerror = () => {
                state.wsStatus = "连接异常";
                render();
            };
            state.socket.onmessage = event => {
                const message = JSON.parse(event.data);
                if (message.type === "snapshot" || message.type === "presence" || message.type === "screen-share") {
                    state.room = message.room;
                } else if (message.type === "chat") {
                    state.messages = [...state.messages.slice(-99), message];
                } else if (message.type === "room-closed") {
                    state.notice = message.message || "房间已关闭。";
                } else if (message.type === "error") {
                    state.notice = message.message;
                }
                render();
            };
        }

        function close() {
            if (state.socket) {
                state.socket.close();
            }
            if (state.pollTimer) {
                window.clearInterval(state.pollTimer);
            }
            host.remove();
            window.CoViewExternalControl = null;
        }

        loadRoom();
        connect();
        loadJoinRequests();
        state.pollTimer = window.setInterval(loadJoinRequests, 2000);
        render();

        return {close};
    }

    window.CoViewExternalControl = {
        open(config) {
            const control = createControl(config);
            window.CoViewExternalControl.close = control.close;
            return control;
        },
        close: null
    };
})();
