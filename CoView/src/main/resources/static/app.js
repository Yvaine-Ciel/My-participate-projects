(function () {
    const {useCallback, useEffect, useMemo, useRef, useState} = React;
    const h = React.createElement;

    const NAME_KEY = "coview:displayName";

    function participantKey(roomId) {
        return "coview:participant:" + roomId;
    }

    function currentRoomFromHash() {
        const match = window.location.hash.match(/^#\/room\/([A-Za-z0-9-]+)$/);
        return match ? decodeURIComponent(match[1]) : null;
    }

    function normalizeRoomInput(value) {
        const clean = (value || "").trim();
        if (!clean) {
            return "";
        }
        return clean.toUpperCase().startsWith("LJX-") ? clean : "LJX-" + clean;
    }

    function navigateToRoom(roomId) {
        window.location.hash = "#/room/" + encodeURIComponent(roomId);
    }

    async function api(path, options) {
        const response = await fetch(path, {
            headers: {"Content-Type": "application/json"},
            ...options
        });
        const text = await response.text();
        const data = text ? JSON.parse(text) : null;
        if (!response.ok) {
            throw new Error(data && data.message ? data.message : "请求失败，请稍后再试。");
        }
        return data;
    }

    function wsUrl(roomId, participantId) {
        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        return protocol + "//" + window.location.host + "/ws/rooms/" + encodeURIComponent(roomId)
            + "?participantId=" + encodeURIComponent(participantId);
    }

    function modeLabel(mode) {
        return mode === "SYNC" ? "同步播放" : "屏幕共享";
    }

    function isHls(url) {
        return /\.m3u8($|\?)/i.test(url || "");
    }

    const SHARE_TEXT = Object.freeze({
        sourcePreviewTitle: "\u540e\u7aef\u5df2\u8bc6\u522b\u5230\u8be6\u60c5\u9875",
        sourcePreviewNote: "\u5982\u679c\u9884\u89c8\u88ab\u5e73\u53f0\u62e6\u622a\uff0c\u8bf7\u5728\u65b0\u6253\u5f00\u7684\u7f51\u9875\u4e2d\u64ad\u653e\uff0c\u518d\u5728\u6d4f\u89c8\u5668\u5171\u4eab\u7a97\u53e3\u91cc\u9009\u62e9\u5b83\u8fdb\u884c\u88c1\u526a\u3002",
        openSourcePage: "\u6253\u5f00\u8be6\u60c5\u9875",
        prepareCrop: "\u6253\u5f00\u8be6\u60c5\u9875\u5e76\u51c6\u5907\u88c1\u526a",
        chooseCaptureTarget: "\u9009\u62e9\u8be5\u8be6\u60c5\u9875\u8fdb\u884c\u88c1\u526a",
        popupBlocked: "\u6d4f\u89c8\u5668\u963b\u6b62\u4e86\u8be6\u60c5\u9875\u5f39\u51fa\uff0c\u8bf7\u624b\u52a8\u6253\u5f00\u94fe\u63a5\uff1a",
        captureFailed: "\u65e0\u6cd5\u622a\u53d6\u5171\u4eab\u753b\u9762\u3002\u8bf7\u5728\u6d4f\u89c8\u5668\u5f39\u7a97\u4e2d\u9009\u62e9\u5df2\u6253\u5f00\u7684\u8be6\u60c5\u9875\u3001\u7a97\u53e3\u6216\u5c4f\u5e55\u3002",
        cropRequired: "\u8bf7\u5148\u62d6\u52a8\u9009\u62e9\u8981\u5171\u4eab\u7684\u533a\u57df\uff0c\u6216\u9009\u62e9\u5171\u4eab\u5168\u753b\u9762\u3002",
        confirmCrop: "\u786e\u8ba4\u88c1\u526a\u5e76\u5f00\u59cb\u5b9e\u65f6\u5171\u4eab",
        sharingLive: "\u88c1\u526a\u533a\u57df\u6b63\u5728\u5b9e\u65f6\u5171\u4eab\uff0c\u4fee\u6539\u88c1\u526a\u6846\u4f1a\u7acb\u5373\u66f4\u65b0\u623f\u5ba2\u753b\u9762\u3002",
        livePreview: "\u88c1\u526a\u540e\u7684\u753b\u9762\u6b63\u5728\u5b9e\u65f6\u5171\u4eab\u3002",
        cropReady: "\u62d6\u52a8\u9009\u533a\u540e\u70b9\u51fb\u786e\u8ba4\uff0c\u623f\u5ba2\u7aef\u4f1a\u6309\u88c1\u526a\u6bd4\u4f8b\u7b49\u6bd4\u4f8b\u653e\u5927\uff0c\u5bbd\u6216\u9ad8\u81f3\u5c11\u4e00\u4e2a\u65b9\u5411\u94fa\u6ee1\u753b\u9762\u3002",
        adjustCrop: "\u8c03\u6574\u88c1\u526a",
        finishAdjust: "\u67e5\u770b\u5171\u4eab\u753b\u9762",
        receiveShare: "\u63a5\u6536\u5171\u4eab\u753b\u9762",
        enableAudio: "\u5f00\u542f\u5171\u4eab\u58f0\u97f3",
        remotePlayBlocked: "\u6d4f\u89c8\u5668\u9700\u8981\u4f60\u70b9\u51fb\u4e00\u6b21\u624d\u80fd\u64ad\u653e\u5171\u4eab\u753b\u9762\u3002",
        connectingShare: "\u6b63\u5728\u8fde\u63a5\u623f\u4e3b\u5171\u4eab\u753b\u9762",
        waitingHostShare: "\u7b49\u5f85\u623f\u4e3b\u5f00\u59cb\u5171\u4eab",
        receivingLive: "\u6b63\u5728\u63a5\u6536\u5171\u4eab\u753b\u9762",
        signalFailed: "\u5171\u4eab\u8fde\u63a5\u4fe1\u4ee4\u5904\u7406\u5931\u8d25\uff0c\u8bf7\u8ba9\u623f\u4e3b\u505c\u6b62\u540e\u91cd\u65b0\u5171\u4eab\u3002",
        fullFrame: "\u5171\u4eab\u5168\u753b\u9762",
        recapture: "\u91cd\u65b0\u9009\u62e9\u9875\u9762",
        stopShare: "\u505c\u6b62\u5171\u4eab",
        liveBadge: "\u6b63\u5728\u5171\u4eab",
        pendingCrop: "\u5f85\u786e\u8ba4",
        dragCrop: "\u62d6\u52a8\u9009\u62e9\u5171\u4eab\u533a\u57df"
    });

    const PLAYER_TEXT = Object.freeze({
        resumeSync: "\u7ee7\u7eed\u540c\u6b65\u64ad\u653e",
        autoplayBlocked: "\u6d4f\u89c8\u5668\u9700\u8981\u4f60\u70b9\u51fb\u4e00\u6b21\u624d\u80fd\u7ee7\u7eed\u540c\u6b65\u64ad\u653e\u3002"
    });

    function Shell({children, room, wsStatus, onLeave}) {
        return h("div", {className: "app-shell"},
            h("header", {className: "topbar"},
                h("button", {className: "brand-button", onClick: () => window.location.hash = ""},
                    h("span", {className: "brand-mark"}, "C"),
                    h("span", null, "CoView")
                ),
                room ? h("div", {className: "room-code-center mono"}, room.id) : null,
                h("div", {className: "topbar-actions"},
                    room ? h("span", {className: "mode-pill " + (room.source.mode === "SYNC" ? "sync" : "share")}, modeLabel(room.source.mode)) : null,
                    wsStatus ? h("span", {className: "status-pill " + (wsStatus === "已连接" ? "online" : "")}, wsStatus) : null,
                    room ? h("button", {className: "secondary", onClick: () => copyText(room.id, "房间号已复制。")}, "复制房间号") : null,
                    room && onLeave ? h("button", {className: "danger", onClick: onLeave}, "退出房间") : null
                )
            ),
            h("main", {className: "main"}, children)
        );
    }

    function App() {
        const [roomId, setRoomId] = useState(currentRoomFromHash());

        useEffect(() => {
            const onHashChange = () => setRoomId(currentRoomFromHash());
            window.addEventListener("hashchange", onHashChange);
            return () => window.removeEventListener("hashchange", onHashChange);
        }, []);

        return roomId ? h(RoomPage, {key: roomId, roomId}) : h(EntryPage);
    }

    function EntryPage() {
        const [role, setRole] = useState("");

        if (role === "owner") {
            return h(Shell, null, h(HostSetup, {onBack: () => setRole("")}));
        }

        if (role === "guest") {
            return h(Shell, null, h(GuestSetup, {onBack: () => setRole("")}));
        }

        return h(Shell, null,
            h("section", {className: "entry-layout"},
                h("div", {className: "intro-band"},
                    h("h1", null, "选择你的观影身份"),
                    h("p", null, "房主负责创建房间、设置密码和控制共享；房客输入房间号和密码进入。")
                ),
                h("div", {className: "entry-grid"},
                    h("button", {className: "identity-card", onClick: () => setRole("owner")},
                        h("strong", null, "我是房主"),
                        h("span", null, "创建临时放映厅，生成 LJX 房间号，设置房间密码")
                    ),
                    h("button", {className: "identity-card", onClick: () => setRole("guest")},
                        h("strong", null, "我是房客"),
                        h("span", null, "输入房间号和密码，等待房主确认后进入")
                    )
                )
            )
        );
    }

    function HostSetup({onBack}) {
        const [displayName, setDisplayName] = useState(() => sessionStorage.getItem(NAME_KEY) || "");
        const [sourceUrl, setSourceUrl] = useState("");
        const [roomId, setRoomId] = useState("");
        const [password, setPassword] = useState("");
        const [confirmPassword, setConfirmPassword] = useState("");
        const [loadingRoomId, setLoadingRoomId] = useState(false);
        const [loading, setLoading] = useState(false);
        const [error, setError] = useState("");

        const refreshRoomId = useCallback(async () => {
            setLoadingRoomId(true);
            setError("");
            try {
                const result = await api("/api/rooms/candidate-id");
                setRoomId(result.roomId);
            } catch (ex) {
                setError(ex.message);
            } finally {
                setLoadingRoomId(false);
            }
        }, []);

        useEffect(() => {
            refreshRoomId();
        }, [refreshRoomId]);

        async function createRoom(event) {
            event.preventDefault();
            setError("");
            if (password !== confirmPassword) {
                setError("两次输入的房间密码不一致。");
                return;
            }
            setLoading(true);
            try {
                const result = await api("/api/rooms", {
                    method: "POST",
                    body: JSON.stringify({roomId, displayName, sourceUrl, password, confirmPassword})
                });
                sessionStorage.setItem(NAME_KEY, displayName || "房主");
                sessionStorage.setItem(participantKey(result.roomId), result.participantId);
                navigateToRoom(result.roomId);
            } catch (ex) {
                setError(ex.message);
                if (ex.message.includes("重新生成")) {
                    refreshRoomId();
                }
            } finally {
                setLoading(false);
            }
        }

        return h("form", {className: "setup-grid", onSubmit: createRoom},
            h("section", {className: "panel"},
                h("div", {className: "panel-header"},
                    h("div", null,
                        h("h1", {className: "panel-title"}, "房主信息与视频链接"),
                        h("p", {className: "panel-subtitle"}, "用户名、视频地址和房间密码都在这里填写。分享文案里的网址会由后端自动提取。")
                    )
                ),
                h("div", {className: "panel-body form-grid"},
                    h("label", null, "用户名",
                        h("input", {
                            value: displayName,
                            maxLength: 40,
                            placeholder: "房主昵称",
                            onChange: event => setDisplayName(event.target.value)
                        })
                    ),
                    h("label", null, "视频页面或视频资源",
                        h("textarea", {
                            value: sourceUrl,
                            rows: 6,
                            placeholder: "可以粘贴完整分享文案，例如：我正在看这个视频 https://www.bilibili.com/video/xxxx",
                            onChange: event => setSourceUrl(event.target.value)
                        })
                    )
                )
            ),
            h("section", {className: "panel"},
                h("div", {className: "panel-header"},
                    h("div", null,
                        h("h2", {className: "panel-title"}, "房间号生成"),
                        h("p", {className: "panel-subtitle"}, "这里先显示随机结果，只有点击进入并创建成功后，这个房间号才会真正生效。")
                    )
                ),
                h("div", {className: "panel-body form-grid"},
                    h("div", {className: "generated-room-preview mono"}, loadingRoomId ? "生成中" : (roomId || "等待生成")),
                    h("button", {type: "button", className: "secondary", disabled: loadingRoomId, onClick: refreshRoomId}, "重新生成房间号"),
                    h("label", null, "房间密码",
                        h("input", {
                            type: "password",
                            value: password,
                            minLength: 4,
                            maxLength: 32,
                            placeholder: "4 到 32 个字符",
                            onChange: event => setPassword(event.target.value)
                        })
                    ),
                    h("label", null, "再次确认密码",
                        h("input", {
                            type: "password",
                            value: confirmPassword,
                            minLength: 4,
                            maxLength: 32,
                            placeholder: "再输入一次",
                            onChange: event => setConfirmPassword(event.target.value)
                        })
                    ),
                    error ? h("div", {className: "notice error"}, error) : null,
                    h("div", {className: "button-row"},
                        h("button", {type: "button", className: "secondary", onClick: onBack}, "返回身份选择"),
                        h("button", {className: "success", disabled: loading || !roomId || !sourceUrl.trim() || !password || !confirmPassword},
                            loading ? "创建中" : "创建房间并进入")
                    )
                )
            )
        );
    }

    function GuestSetup({onBack}) {
        const [displayName, setDisplayName] = useState(() => sessionStorage.getItem(NAME_KEY) || "");
        const [roomInput, setRoomInput] = useState("");
        const [password, setPassword] = useState("");
        const [confirming, setConfirming] = useState(false);
        const [pendingRequest, setPendingRequest] = useState(null);
        const [loading, setLoading] = useState(false);
        const [error, setError] = useState("");
        const roomId = normalizeRoomInput(roomInput);

        useEffect(() => {
            if (!pendingRequest) {
                return undefined;
            }
            const timer = window.setInterval(async () => {
                try {
                    const result = await api("/api/rooms/" + encodeURIComponent(pendingRequest.roomId)
                        + "/join-requests/" + encodeURIComponent(pendingRequest.id));
                    if (result.status === "APPROVED") {
                        sessionStorage.setItem(NAME_KEY, displayName);
                        sessionStorage.setItem(participantKey(result.roomId), result.participantId);
                        navigateToRoom(result.roomId);
                    }
                    if (result.status === "REJECTED") {
                        setError(result.message || "房主已拒绝本次加入申请。");
                        setPendingRequest(null);
                    }
                } catch (ex) {
                    setError(ex.message);
                    setPendingRequest(null);
                }
            }, 1500);
            return () => window.clearInterval(timer);
        }, [pendingRequest, displayName]);

        function requestConfirm(event) {
            event.preventDefault();
            setError("");
            if (!displayName.trim()) {
                setError("请填写用户名。");
                return;
            }
            if (!roomId) {
                setError("请输入房间号。");
                return;
            }
            setConfirming(true);
        }

        async function requestJoinRoom() {
            setError("");
            setLoading(true);
            try {
                const result = await api("/api/rooms/" + encodeURIComponent(roomId) + "/join-requests", {
                    method: "POST",
                    body: JSON.stringify({displayName, password})
                });
                sessionStorage.setItem(NAME_KEY, displayName);
                setPendingRequest(result);
                setConfirming(false);
            } catch (ex) {
                setError(ex.message);
            } finally {
                setLoading(false);
            }
        }

        return h("section", {className: "panel join-panel"},
            h("div", {className: "panel-header"},
                h("div", null,
                    h("h1", {className: "panel-title"}, "房客加入房间"),
                    h("p", {className: "panel-subtitle"}, "填写用户名和房间号，提交申请后需要房主同意才能进入。")
                )
            ),
            h("form", {className: "panel-body form-grid", onSubmit: requestConfirm},
                h("label", null, "用户名",
                    h("input", {
                        value: displayName,
                        maxLength: 40,
                        placeholder: "房客昵称",
                        onChange: event => setDisplayName(event.target.value)
                    })
                ),
                h("label", null, "房间号",
                    h("input", {
                        className: "mono",
                        value: roomInput,
                        placeholder: "LJX-a1B2c3",
                        onChange: event => setRoomInput(event.target.value.trim())
                    })
                ),
                error ? h("div", {className: "notice error"}, error) : null,
                h("div", {className: "button-row"},
                    h("button", {type: "button", className: "secondary", onClick: onBack}, "返回身份选择"),
                    h("button", {className: "success", disabled: !displayName.trim() || !roomId}, "申请进入房间")
                )
            ),
            confirming ? h(ConfirmJoinDialog, {
                roomId,
                password,
                setPassword,
                loading,
                error,
                onCancel: () => setConfirming(false),
                onConfirm: requestJoinRoom
            }) : null,
            pendingRequest ? h(WaitingJoinPanel, {request: pendingRequest, onCancel: () => setPendingRequest(null)}) : null
        );
    }

    function WaitingJoinPanel({request, onCancel}) {
        return h("div", {className: "modal-backdrop"},
            h("section", {className: "modal-panel"},
                h("h2", null, "等待房主确认"),
                h("p", null, "你的加入申请已发送给房主，房主同意后会自动进入房间。"),
                h("div", {className: "confirm-room mono"}, request.roomId),
                h("div", {className: "notice"}, request.message || "等待房主确认。"),
                h("div", {className: "button-row"},
                    h("button", {type: "button", className: "secondary", onClick: onCancel}, "取消等待")
                )
            )
        );
    }

    function ConfirmJoinDialog({roomId, password, setPassword, loading, error, onCancel, onConfirm}) {
        return h("div", {className: "modal-backdrop"},
            h("section", {className: "modal-panel"},
                h("h2", null, "确认申请进入"),
                h("p", null, "请再次确认房间号是否正确，并输入房主设置的密码。房主同意后才能进入。"),
                h("div", {className: "confirm-room mono"}, roomId),
                h("label", null, "房间密码",
                    h("input", {
                        type: "password",
                        value: password,
                        autoFocus: true,
                        placeholder: "请输入房间密码",
                        onChange: event => setPassword(event.target.value)
                    })
                ),
                error ? h("div", {className: "notice error"}, error) : null,
                h("div", {className: "button-row"},
                    h("button", {type: "button", className: "secondary", onClick: onCancel}, "返回修改"),
                    h("button", {type: "button", className: "success", disabled: loading || !password, onClick: onConfirm},
                        loading ? "申请中" : "提交申请")
                )
            )
        );
    }

    function RoomPage({roomId}) {
        const [room, setRoom] = useState(null);
        const [participantId, setParticipantId] = useState(() => sessionStorage.getItem(participantKey(roomId)) || "");
        const [wsStatus, setWsStatus] = useState("");
        const [error, setError] = useState("");
        const [toast, setToast] = useState("");
        const [playbackEvent, setPlaybackEvent] = useState(null);
        const [signalEvents, setSignalEvents] = useState([]);
        const [chatMessages, setChatMessages] = useState([]);
        const [joinRequests, setJoinRequests] = useState([]);
        const wsRef = useRef(null);
        const wsRoomId = room ? room.id : "";

        useEffect(() => {
            if (!participantId) {
                return undefined;
            }
            let mounted = true;
            api("/api/rooms/" + encodeURIComponent(roomId))
                .then(data => mounted && setRoom(data))
                .catch(ex => mounted && setError(ex.message));
            return () => {
                mounted = false;
            };
        }, [roomId, participantId]);

        useEffect(() => {
            if (!participantId || !wsRoomId) {
                setWsStatus("");
                return undefined;
            }
            const socket = new WebSocket(wsUrl(wsRoomId, participantId));
            wsRef.current = socket;
            setWsStatus("连接中");

            socket.onopen = () => setWsStatus("已连接");
            socket.onclose = () => setWsStatus("已断开");
            socket.onerror = () => setWsStatus("连接异常");
            socket.onmessage = event => {
                const message = JSON.parse(event.data);
                if (message.type === "snapshot" || message.type === "presence" || message.type === "screen-share") {
                    setRoom(message.room);
                } else if (message.type === "playback") {
                    setPlaybackEvent({...message, receivedAt: Date.now()});
                } else if (message.type === "webrtc-signal") {
                    setSignalEvents(events => [
                        ...events.slice(-199),
                        {...message, signalId: Date.now() + ":" + Math.random()}
                    ]);
                } else if (message.type === "chat") {
                    setChatMessages(messages => [...messages.slice(-99), message]);
                } else if (message.type === "room-closed") {
                    window.alert(message.message || "房间已关闭。");
                    sessionStorage.removeItem(participantKey(wsRoomId));
                    window.location.hash = "";
                } else if (message.type === "error") {
                    setToast(message.message);
                }
            };

            return () => {
                socket.close();
            };
        }, [wsRoomId, participantId]);

        const sendWs = useCallback(payload => {
            const socket = wsRef.current;
            if (socket && socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify(payload));
            }
        }, []);

        const participant = useMemo(() => {
            if (!room || !participantId) {
                return null;
            }
            return room.participants.find(item => item.id === participantId) || null;
        }, [room, participantId]);

        const isOwner = Boolean(participant && participant.owner);

        useEffect(() => {
            if (!room || !participantId || !isOwner) {
                setJoinRequests([]);
                return undefined;
            }
            let stopped = false;
            async function loadJoinRequests() {
                try {
                    const result = await api("/api/rooms/" + encodeURIComponent(room.id)
                        + "/join-requests?participantId=" + encodeURIComponent(participantId));
                    if (!stopped) {
                        setJoinRequests(result);
                    }
                } catch (ex) {
                    if (!stopped) {
                        setToast(ex.message);
                    }
                }
            }
            loadJoinRequests();
            const timer = window.setInterval(loadJoinRequests, 2000);
            return () => {
                stopped = true;
                window.clearInterval(timer);
            };
        }, [room, participantId, isOwner]);

        function leaveRoom() {
            if (!room) {
                window.location.hash = "";
                return;
            }
            const message = isOwner
                ? "你是房主，退出后房间 " + room.id + " 会立即关闭，房间号将彻底作废。确定退出吗？"
                : "确定退出当前房间吗？";
            if (!window.confirm(message)) {
                return;
            }
            sendWs({type: "leave"});
            sessionStorage.removeItem(participantKey(room.id));
            window.setTimeout(() => {
                window.location.hash = "";
            }, 80);
        }

        async function decideJoinRequest(requestId, approved) {
            try {
                const result = await api("/api/rooms/" + encodeURIComponent(room.id)
                    + "/join-requests/" + encodeURIComponent(requestId) + "/decision", {
                    method: "POST",
                    body: JSON.stringify({participantId, approved})
                });
                setJoinRequests(requests => requests.filter(request => request.id !== requestId));
                if (result.room) {
                    setRoom(result.room);
                    sendWs({type: "room-refresh"});
                }
            } catch (ex) {
                setToast(ex.message);
            }
        }

        if (!participantId) {
            return h(Shell, null, h(DirectJoinPanel, {
                roomId: normalizeRoomInput(roomId),
                onApproved: result => {
                    setParticipantId(result.participantId);
                    setRoom(result.room);
                    navigateToRoom(result.roomId);
                }
            }));
        }

        if (error) {
            return h(Shell, null,
                h("section", {className: "panel join-panel"},
                    h("div", {className: "panel-body"},
                        h("div", {className: "notice error"}, error),
                        h("button", {className: "secondary", onClick: () => window.location.hash = ""}, "返回首页")
                    )
                )
            );
        }

        if (!room) {
            return h(Shell, null, h("div", {className: "notice"}, "正在进入房间"));
        }

        return h(Shell, {room, wsStatus, onLeave: leaveRoom},
            h("div", {className: "room-page-layout"},
                toast ? h("div", {className: "notice"}, toast) : null,
                h("div", {className: "watch-chat-row"},
                    h("section", {className: "watch-stage"},
                        h("div", {className: "stage-toolbar"},
                            h("div", {className: "stage-title"},
                                h("strong", null, modeLabel(room.source.mode)),
                                h("span", null, room.source.normalizedUrl)
                            ),
                            h("span", {className: "role-badge"}, isOwner ? "房主" : "房客")
                        ),
                        room.source.mode === "SYNC"
                            ? h(DirectPlayer, {room, participantId, isOwner, sendWs, playbackEvent})
                            : h(ScreenShare, {room, participantId, isOwner, sendWs, signalEvents})
                    ),
                    h(ChatPanel, {messages: chatMessages, participantId, sendWs})
                ),
                h("section", {className: "sidebar-panel full-row-panel"},
                    h("div", {className: "sidebar-title"},
                        h("strong", null, "当前来源"),
                        h("span", null, room.source.reason)
                    ),
                    isOwner ? h(SourceSwitcher, {room, participantId, setRoom, sendWs}) : null
                ),
                isOwner ? h(JoinRequestsPanel, {requests: joinRequests, onDecision: decideJoinRequest}) : null,
                h("section", {className: "sidebar-panel full-row-panel"},
                    h("div", {className: "sidebar-title"},
                        h("strong", null, "成员"),
                        h("span", null, room.participants.length + " 人")
                    ),
                    h(ParticipantList, {room})
                )
            )
        );
    }

    function JoinRequestsPanel({requests, onDecision}) {
        if (!requests.length) {
            return null;
        }
        return h("section", {className: "sidebar-panel full-row-panel join-requests-panel"},
            h("div", {className: "sidebar-title"},
                h("strong", null, "待确认申请"),
                h("span", null, requests.length + " 个房客等待进入")
            ),
            h("div", {className: "join-request-list"},
                requests.map(request => h("div", {className: "join-request-item", key: request.id},
                    h("div", null,
                        h("strong", null, request.displayName),
                        h("span", null, request.message || "等待房主确认。")
                    ),
                    h("div", {className: "button-row"},
                        h("button", {className: "success", onClick: () => onDecision(request.id, true)}, "同意"),
                        h("button", {className: "secondary", onClick: () => onDecision(request.id, false)}, "拒绝")
                    )
                ))
            )
        );
    }

    function DirectJoinPanel({roomId, onApproved}) {
        const [displayName, setDisplayName] = useState(() => sessionStorage.getItem(NAME_KEY) || "");
        const [password, setPassword] = useState("");
        const [pendingRequest, setPendingRequest] = useState(null);
        const [error, setError] = useState("");
        const [loading, setLoading] = useState(false);

        useEffect(() => {
            if (!pendingRequest) {
                return undefined;
            }
            const timer = window.setInterval(async () => {
                try {
                    const result = await api("/api/rooms/" + encodeURIComponent(pendingRequest.roomId)
                        + "/join-requests/" + encodeURIComponent(pendingRequest.id));
                    if (result.status === "APPROVED") {
                        sessionStorage.setItem(NAME_KEY, displayName || "房客");
                        sessionStorage.setItem(participantKey(result.roomId), result.participantId);
                        onApproved(result);
                    }
                    if (result.status === "REJECTED") {
                        setError(result.message || "房主已拒绝本次加入申请。");
                        setPendingRequest(null);
                    }
                } catch (ex) {
                    setError(ex.message);
                    setPendingRequest(null);
                }
            }, 1500);
            return () => window.clearInterval(timer);
        }, [pendingRequest, displayName]);

        async function submit(event) {
            event.preventDefault();
            setError("");
            setLoading(true);
            try {
                const result = await api("/api/rooms/" + encodeURIComponent(roomId) + "/join-requests", {
                    method: "POST",
                    body: JSON.stringify({displayName: displayName || "房客", password})
                });
                setPendingRequest(result);
            } catch (ex) {
                setError(ex.message);
            } finally {
                setLoading(false);
            }
        }

        return h("section", {className: "panel join-panel"},
            h("div", {className: "panel-header"},
                h("div", null,
                    h("h1", {className: "panel-title"}, "申请加入房间"),
                    h("p", {className: "panel-subtitle"}, "请确认房间号，并输入用户名和房主设置的密码。提交后等待房主同意。")
                )
            ),
            h("form", {className: "panel-body form-grid", onSubmit: submit},
                h("div", {className: "confirm-room mono"}, roomId),
                h("label", null, "用户名",
                    h("input", {
                        value: displayName,
                        maxLength: 40,
                        placeholder: "房客昵称",
                        onChange: event => setDisplayName(event.target.value)
                    })
                ),
                h("label", null, "房间密码",
                    h("input", {
                        type: "password",
                        value: password,
                        placeholder: "请输入房间密码",
                        onChange: event => setPassword(event.target.value)
                    })
                ),
                error ? h("div", {className: "notice error"}, error) : null,
                h("div", {className: "button-row"},
                    h("button", {className: "success", disabled: loading || !password}, loading ? "申请中" : "提交申请")
                )
            ),
            pendingRequest ? h(WaitingJoinPanel, {request: pendingRequest, onCancel: () => setPendingRequest(null)}) : null
        );
    }

    function DirectPlayer({room, participantId, isOwner, sendWs, playbackEvent}) {
        const videoRef = useRef(null);
        const hlsRef = useRef(null);
        const applyingRemoteRef = useRef(false);
        const lastStateSentRef = useRef(0);
        const pendingStateRef = useRef(null);
        const [playPrompt, setPlayPrompt] = useState("");

        const applyPlaybackState = useCallback((state, showPrompt = true) => {
            const video = videoRef.current;
            if (!video || !state) {
                return;
            }
            pendingStateRef.current = state;

            function applyNow() {
                applyingRemoteRef.current = true;
                let position = Number(state.positionSeconds || 0);
                if (state.playing && state.updatedAt) {
                    const updatedAt = Date.parse(state.updatedAt);
                    if (Number.isFinite(updatedAt)) {
                        position += Math.max(0, (Date.now() - updatedAt) / 1000);
                    }
                }
                if (Number.isFinite(position) && Math.abs(video.currentTime - position) > 0.75) {
                    try {
                        video.currentTime = position;
                    } catch (ignored) {
                    }
                }
                if (state.playing) {
                    video.play()
                        .then(() => setPlayPrompt(""))
                        .catch(() => {
                            if (showPrompt) {
                                setPlayPrompt(PLAYER_TEXT.autoplayBlocked);
                            }
                        });
                } else {
                    video.pause();
                    setPlayPrompt("");
                }
                window.setTimeout(() => {
                    applyingRemoteRef.current = false;
                }, 350);
            }

            if (video.readyState < 1) {
                video.addEventListener("loadedmetadata", applyNow, {once: true});
            } else {
                applyNow();
            }
        }, []);

        useEffect(() => {
            const video = videoRef.current;
            const sourceUrl = room.source.normalizedUrl;
            if (!video || !sourceUrl) {
                return undefined;
            }

            if (hlsRef.current) {
                hlsRef.current.destroy();
                hlsRef.current = null;
            }
            video.pause();
            video.removeAttribute("src");
            video.load();
            setPlayPrompt("");

            if (isHls(sourceUrl) && window.Hls && window.Hls.isSupported()) {
                const hlsInstance = new window.Hls({enableWorker: true});
                hlsInstance.loadSource(sourceUrl);
                hlsInstance.attachMedia(video);
                hlsRef.current = hlsInstance;
            } else {
                video.src = sourceUrl;
            }

            return () => {
                if (hlsRef.current) {
                    hlsRef.current.destroy();
                    hlsRef.current = null;
                }
            };
        }, [room.source.normalizedUrl]);

        useEffect(() => {
            if (room.playback) {
                applyPlaybackState(room.playback, true);
            }
        }, [room.playback && room.playback.updatedAt, room.source.normalizedUrl, applyPlaybackState]);

        useEffect(() => {
            const video = videoRef.current;
            if (!video || !playbackEvent || playbackEvent.senderId === participantId) {
                return;
            }
            applyPlaybackState(playbackEvent.state, true);
        }, [playbackEvent, participantId, applyPlaybackState]);

        const sendPlayback = useCallback((action) => {
            const video = videoRef.current;
            if (!video || applyingRemoteRef.current) {
                return;
            }
            sendWs({
                type: "playback",
                action,
                positionSeconds: Number(video.currentTime || 0)
            });
        }, [sendWs]);

        function handleTimeUpdate() {
            if (!isOwner || applyingRemoteRef.current) {
                return;
            }
            const now = Date.now();
            if (now - lastStateSentRef.current > 4000) {
                lastStateSentRef.current = now;
                sendPlayback("state");
            }
        }

        return h("div", {className: "video-wrap"},
            h("video", {
                ref: videoRef,
                controls: true,
                playsInline: true,
                preload: "metadata",
                onPlay: () => sendPlayback("play"),
                onPause: () => sendPlayback("pause"),
                onSeeked: () => sendPlayback("seek"),
                onTimeUpdate: handleTimeUpdate
            }),
            playPrompt ? h("button", {
                type: "button",
                className: "player-resume",
                onClick: () => applyPlaybackState(pendingStateRef.current || room.playback, true)
            }, PLAYER_TEXT.resumeSync) : null
        );
    }

    const FULL_CROP = Object.freeze({x: 0, y: 0, width: 100, height: 100});
    const EMPTY_IMAGE_BOX = Object.freeze({left: 0, top: 0, width: 0, height: 0});
    const SHARE_FRAME_RATE = 30;
    const SHARE_MAX_WIDTH = 1920;
    const SHARE_MAX_HEIGHT = 1080;
    const SHARE_VIDEO_BITRATE = 8_000_000;
    const SHARE_AUDIO_BITRATE = 160_000;

    function clampCrop(crop) {
        if (!crop) {
            return null;
        }
        const minSize = 2;
        let width = Math.max(minSize, Math.min(100, crop.width));
        let height = Math.max(minSize, Math.min(100, crop.height));
        let x = Math.max(0, Math.min(100 - width, crop.x));
        let y = Math.max(0, Math.min(100 - height, crop.y));
        return {x, y, width, height};
    }

    function cropFromPoints(start, end) {
        return clampCrop({
            x: Math.min(start.x, end.x),
            y: Math.min(start.y, end.y),
            width: Math.abs(end.x - start.x),
            height: Math.abs(end.y - start.y)
        });
    }

    function sharedOutputSize(width, height) {
        const safeWidth = Math.max(2, width);
        const safeHeight = Math.max(2, height);
        const scale = Math.min(SHARE_MAX_WIDTH / safeWidth, SHARE_MAX_HEIGHT / safeHeight);
        return {
            width: Math.max(2, Math.round(safeWidth * scale)),
            height: Math.max(2, Math.round(safeHeight * scale))
        };
    }

    async function tunePeerSender(sender, track) {
        if (!sender || !track || typeof sender.getParameters !== "function" || typeof sender.setParameters !== "function") {
            return;
        }
        try {
            const parameters = sender.getParameters();
            parameters.encodings = parameters.encodings && parameters.encodings.length ? parameters.encodings : [{}];
            parameters.encodings[0].maxBitrate = track.kind === "video" ? SHARE_VIDEO_BITRATE : SHARE_AUDIO_BITRATE;
            if (track.kind === "video") {
                parameters.encodings[0].maxFramerate = SHARE_FRAME_RATE;
                parameters.degradationPreference = "maintain-resolution";
            }
            await sender.setParameters(parameters);
        } catch (ignored) {
        }
    }

    function waitForVideoReady(video) {
        if (video.readyState >= 2 && video.videoWidth && video.videoHeight) {
            return Promise.resolve();
        }
        return new Promise(resolve => {
            const done = () => resolve();
            video.addEventListener("loadeddata", done, {once: true});
            window.setTimeout(done, 600);
        });
    }

    function captureVideoFrame(video) {
        const width = video.videoWidth || 1280;
        const height = video.videoHeight || 720;
        const canvas = document.createElement("canvas");
        const context = canvas.getContext("2d");
        canvas.width = width;
        canvas.height = height;
        if (context) {
            context.drawImage(video, 0, 0, width, height);
        }
        return {
            url: canvas.toDataURL("image/jpeg", 0.86),
            width,
            height
        };
    }

    function ScreenShare({room, participantId, isOwner, sendWs, signalEvents}) {
        const localVideoRef = useRef(null);
        const ownerPreviewVideoRef = useRef(null);
        const remoteVideoRef = useRef(null);
        const remoteStreamRef = useRef(null);
        const cropStageRef = useRef(null);
        const rawStreamRef = useRef(null);
        const localStreamRef = useRef(null);
        const canvasRef = useRef(null);
        const canvasTrackRef = useRef(null);
        const animationFrameRef = useRef(0);
        const videoFrameCallbackRef = useRef(0);
        const frameTimerRef = useRef(0);
        const peersRef = useRef(new Map());
        const pendingCandidatesRef = useRef(new Map());
        const processedSignalIdsRef = useRef(new Set());
        const remoteFrameStatsRef = useRef({frames: -1, unchangedTicks: 0});
        const cropRef = useRef(null);
        const [crop, setCrop] = useState(null);
        const [screenshot, setScreenshot] = useState(null);
        const [imageBox, setImageBox] = useState(EMPTY_IMAGE_BOX);
        const [sharing, setSharing] = useState(false);
        const [editingCrop, setEditingCrop] = useState(false);
        const [cropConfirmed, setCropConfirmed] = useState(false);
        const [remoteStreamReady, setRemoteStreamReady] = useState(false);
        const [remoteMuted, setRemoteMuted] = useState(true);
        const [remotePlayPrompt, setRemotePlayPrompt] = useState("");
        const [error, setError] = useState("");
        const [sourcePreviewOpen, setSourcePreviewOpen] = useState(false);
        const sourceUrl = room.source && room.source.normalizedUrl ? room.source.normalizedUrl : "";

        useEffect(() => {
            cropRef.current = crop;
            if (!sharing) {
                setCropConfirmed(false);
            }
        }, [crop]);

        const updateImageBox = useCallback(() => {
            const stage = cropStageRef.current;
            if (!stage || !screenshot) {
                setImageBox(EMPTY_IMAGE_BOX);
                return;
            }
            const box = stage.getBoundingClientRect();
            const aspect = screenshot.width / Math.max(1, screenshot.height);
            const stageAspect = box.width / Math.max(1, box.height);
            let width = box.width;
            let height = box.height;
            let left = 0;
            let top = 0;
            if (stageAspect > aspect) {
                height = box.height;
                width = height * aspect;
                left = (box.width - width) / 2;
            } else {
                width = box.width;
                height = width / aspect;
                top = (box.height - height) / 2;
            }
            setImageBox({left, top, width, height});
        }, [screenshot]);

        useEffect(() => {
            updateImageBox();
            if (!screenshot) {
                return undefined;
            }
            window.addEventListener("resize", updateImageBox);
            return () => window.removeEventListener("resize", updateImageBox);
        }, [screenshot, updateImageBox]);

        const sendSignal = useCallback((targetId, payload) => {
            sendWs({type: "webrtc-signal", targetId, payload});
        }, [sendWs]);

        const playRemoteVideo = useCallback((withAudio = false) => {
            const video = remoteVideoRef.current;
            if (!video || !video.srcObject) {
                return;
            }
            const muted = withAudio ? false : remoteMuted;
            video.muted = muted;
            setRemoteMuted(muted);
            setRemotePlayPrompt("");
            const playPromise = video.play();
            if (playPromise && typeof playPromise.catch === "function") {
                playPromise.catch(() => setRemotePlayPrompt(SHARE_TEXT.remotePlayBlocked));
            }
        }, [remoteMuted]);

        const attachRemoteTrack = useCallback((event) => {
            let stream = event.streams && event.streams[0] ? event.streams[0] : null;
            if (!stream) {
                stream = remoteStreamRef.current || new MediaStream();
                if (event.track && !stream.getTracks().some(track => track.id === event.track.id)) {
                    stream.addTrack(event.track);
                }
            }
            remoteStreamRef.current = stream;
            if (remoteVideoRef.current && remoteVideoRef.current.srcObject !== stream) {
                remoteVideoRef.current.srcObject = stream;
                remoteVideoRef.current.muted = remoteMuted;
            }
            remoteFrameStatsRef.current = {frames: -1, unchangedTicks: 0};
            setRemoteStreamReady(true);
            window.setTimeout(() => playRemoteVideo(false), 0);
        }, [playRemoteVideo, remoteMuted]);

        const closePeer = useCallback((peerId) => {
            const peer = peersRef.current.get(peerId);
            if (peer) {
                peer.close();
            }
            peersRef.current.delete(peerId);
            pendingCandidatesRef.current.delete(peerId);
        }, []);

        const createPeer = useCallback((peerId) => {
            if (peersRef.current.has(peerId)) {
                return peersRef.current.get(peerId);
            }
            const peer = new RTCPeerConnection({
                iceServers: [{urls: "stun:stun.l.google.com:19302"}]
            });
            peer.onicecandidate = event => {
                if (event.candidate) {
                    sendSignal(peerId, {kind: "candidate", candidate: event.candidate});
                }
            };
            peer.ontrack = attachRemoteTrack;
            peer.onconnectionstatechange = () => {
                const state = peer.connectionState;
                if (state === "connected") {
                    playRemoteVideo(false);
                }
                if (state === "failed" || state === "closed") {
                    closePeer(peerId);
                }
            };
            const stream = localStreamRef.current;
            if (stream) {
                stream.getTracks().forEach(track => {
                    const sender = peer.addTrack(track, stream);
                    tunePeerSender(sender, track);
                });
            }
            peersRef.current.set(peerId, peer);
            return peer;
        }, [attachRemoteTrack, closePeer, playRemoteVideo, sendSignal]);

        const closeAllPeers = useCallback(() => {
            peersRef.current.forEach(peer => peer.close());
            peersRef.current.clear();
            pendingCandidatesRef.current.clear();
        }, []);

        function queueIceCandidate(peerId, candidate) {
            const candidates = pendingCandidatesRef.current.get(peerId) || [];
            candidates.push(candidate);
            pendingCandidatesRef.current.set(peerId, candidates);
        }

        async function flushQueuedIceCandidates(peerId, peer) {
            if (!peer.remoteDescription) {
                return;
            }
            const candidates = pendingCandidatesRef.current.get(peerId) || [];
            if (!candidates.length) {
                return;
            }
            pendingCandidatesRef.current.delete(peerId);
            for (const candidate of candidates) {
                await peer.addIceCandidate(candidate);
            }
        }

        async function addIceCandidateWhenReady(peerId, peer, candidate) {
            if (!peer.remoteDescription) {
                queueIceCandidate(peerId, candidate);
                return;
            }
            await peer.addIceCandidate(candidate);
        }

        useEffect(() => {
            if (!isOwner || !sharing || editingCrop || !ownerPreviewVideoRef.current || !localStreamRef.current) {
                return undefined;
            }
            const video = ownerPreviewVideoRef.current;
            const stream = localStreamRef.current;
            video.srcObject = stream;
            video.muted = true;
            video.play().catch(() => {});
            return () => {
                if (video.srcObject === stream) {
                    video.srcObject = null;
                }
            };
        }, [isOwner, sharing, editingCrop]);

        useEffect(() => {
            if (isOwner || room.screenShareActive) {
                return;
            }
            if (remoteVideoRef.current) {
                remoteVideoRef.current.pause();
                remoteVideoRef.current.srcObject = null;
            }
            remoteStreamRef.current = null;
            setRemoteStreamReady(false);
            setRemoteMuted(true);
            remoteFrameStatsRef.current = {frames: -1, unchangedTicks: 0};
            setRemotePlayPrompt("");
            closeAllPeers();
        }, [isOwner, room.screenShareActive, closeAllPeers]);

        const stopShare = useCallback((notify = true) => {
            if (animationFrameRef.current) {
                cancelAnimationFrame(animationFrameRef.current);
                animationFrameRef.current = 0;
            }
            if (videoFrameCallbackRef.current && localVideoRef.current && typeof localVideoRef.current.cancelVideoFrameCallback === "function") {
                localVideoRef.current.cancelVideoFrameCallback(videoFrameCallbackRef.current);
                videoFrameCallbackRef.current = 0;
            }
            if (frameTimerRef.current) {
                window.clearInterval(frameTimerRef.current);
                frameTimerRef.current = 0;
            }
            if (rawStreamRef.current) {
                rawStreamRef.current.getTracks().forEach(track => track.stop());
            }
            if (localStreamRef.current) {
                localStreamRef.current.getVideoTracks().forEach(track => track.stop());
            }
            rawStreamRef.current = null;
            localStreamRef.current = null;
            canvasRef.current = null;
            canvasTrackRef.current = null;
            closeAllPeers();
            remoteStreamRef.current = null;
            setSharing(false);
            setEditingCrop(false);
            setCropConfirmed(false);
            setRemoteStreamReady(false);
            setRemoteMuted(true);
            remoteFrameStatsRef.current = {frames: -1, unchangedTicks: 0};
            setRemotePlayPrompt("");
            setScreenshot(null);
            setCrop(null);
            setSourcePreviewOpen(false);
            setImageBox(EMPTY_IMAGE_BOX);
            if (localVideoRef.current) {
                localVideoRef.current.srcObject = null;
            }
            if (notify) {
                sendWs({type: "screen-share", active: false});
            }
        }, [closeAllPeers, sendWs]);

        const offerTo = useCallback(async (targetId, options = {}) => {
            if (!localStreamRef.current) {
                return;
            }
            const existingPeer = peersRef.current.get(targetId);
            if (existingPeer) {
                const state = existingPeer.connectionState;
                if (options.force || state === "failed" || state === "closed" || state === "disconnected") {
                    closePeer(targetId);
                } else if (existingPeer.localDescription && existingPeer.localDescription.type === "offer") {
                    sendSignal(targetId, {kind: "offer", sdp: existingPeer.localDescription});
                    return;
                } else {
                    return;
                }
            }
            const peer = createPeer(targetId);
            const offer = await peer.createOffer();
            await peer.setLocalDescription(offer);
            sendSignal(targetId, {kind: "offer", sdp: peer.localDescription});
        }, [closePeer, createPeer, sendSignal]);

        const requestOfferFromOwner = useCallback(() => {
            if (isOwner || !room.screenShareActive || !room.ownerId || room.ownerId === participantId) {
                return;
            }
            sendSignal(room.ownerId, {kind: "request-offer"});
        }, [isOwner, room.screenShareActive, room.ownerId, participantId, sendSignal]);

        useEffect(() => {
            if (isOwner || !room.screenShareActive || remoteStreamReady || !room.ownerId || room.ownerId === participantId) {
                return undefined;
            }

            let attempts = 0;
            let retryTimer = 0;
            const requestOffer = () => {
                attempts += 1;
                requestOfferFromOwner();
                if (attempts >= 5 && retryTimer) {
                    window.clearInterval(retryTimer);
                }
            };

            requestOffer();
            retryTimer = window.setInterval(requestOffer, 4000);
            return () => window.clearInterval(retryTimer);
        }, [isOwner, room.screenShareActive, room.ownerId, participantId, remoteStreamReady, requestOfferFromOwner]);

        useEffect(() => {
            if (isOwner || !room.screenShareActive || !remoteStreamReady) {
                return undefined;
            }

            remoteFrameStatsRef.current = {frames: -1, unchangedTicks: 0};
            const timer = window.setInterval(() => {
                const video = remoteVideoRef.current;
                if (!video || !video.srcObject) {
                    requestOfferFromOwner();
                    return;
                }

                video.muted = remoteMuted;
                if (video.paused || video.ended) {
                    playRemoteVideo(false);
                }

                const quality = typeof video.getVideoPlaybackQuality === "function"
                    ? video.getVideoPlaybackQuality()
                    : null;
                const frames = quality ? quality.totalVideoFrames : Math.floor((video.currentTime || 0) * 10);
                const stats = remoteFrameStatsRef.current;
                if (frames > 0 && frames === stats.frames) {
                    stats.unchangedTicks += 1;
                } else {
                    stats.frames = frames;
                    stats.unchangedTicks = 0;
                }

                if (stats.unchangedTicks >= 4) {
                    stats.unchangedTicks = 0;
                    requestOfferFromOwner();
                }
            }, 1500);

            return () => window.clearInterval(timer);
        }, [isOwner, room.screenShareActive, remoteStreamReady, remoteMuted, playRemoteVideo, requestOfferFromOwner]);

        function openSourceDetailPage() {
            setSourcePreviewOpen(true);
            if (!sourceUrl) {
                return;
            }
            const opened = window.open(sourceUrl, "coview_source_detail", "width=1280,height=900");
            if (opened) {
                opened.opener = null;
                opened.focus();
            } else {
                setError(SHARE_TEXT.popupBlocked + " " + sourceUrl);
            }
        }

        function startCropDrawing(rawStream) {
            const video = localVideoRef.current;
            const canvas = document.createElement("canvas");
            const context = canvas.getContext("2d");
            canvasRef.current = canvas;

            function drawFrame() {
                if (video && video.readyState >= 2 && context) {
                    const sourceWidth = video.videoWidth || 1280;
                    const sourceHeight = video.videoHeight || 720;
                    const currentCrop = cropRef.current || FULL_CROP;
                    const sx = sourceWidth * currentCrop.x / 100;
                    const sy = sourceHeight * currentCrop.y / 100;
                    const sw = sourceWidth * currentCrop.width / 100;
                    const sh = sourceHeight * currentCrop.height / 100;
                    const output = sharedOutputSize(sw, sh);
                    if (canvas.width !== output.width || canvas.height !== output.height) {
                        canvas.width = output.width;
                        canvas.height = output.height;
                    }
                    context.imageSmoothingEnabled = true;
                    if ("imageSmoothingQuality" in context) {
                        context.imageSmoothingQuality = "high";
                    }
                    context.drawImage(video, sx, sy, sw, sh, 0, 0, canvas.width, canvas.height);
                    if (canvasTrackRef.current && typeof canvasTrackRef.current.requestFrame === "function") {
                        canvasTrackRef.current.requestFrame();
                    }
                }
            }

            function drawLoop() {
                if (video && typeof video.requestVideoFrameCallback === "function") {
                    videoFrameCallbackRef.current = video.requestVideoFrameCallback(() => {
                        drawFrame();
                        drawLoop();
                    });
                    return;
                }
                drawFrame();
                animationFrameRef.current = requestAnimationFrame(drawLoop);
            }

            drawFrame();
            const croppedStream = canvas.captureStream(SHARE_FRAME_RATE);
            canvasTrackRef.current = croppedStream.getVideoTracks()[0] || null;
            if (canvasTrackRef.current) {
                canvasTrackRef.current.contentHint = "detail";
            }
            rawStream.getAudioTracks().forEach(track => croppedStream.addTrack(track));
            localStreamRef.current = croppedStream;
            drawLoop();
            if (frameTimerRef.current) {
                window.clearInterval(frameTimerRef.current);
            }
            frameTimerRef.current = window.setInterval(drawFrame, Math.round(1000 / SHARE_FRAME_RATE));
        }

        async function captureForCrop() {
            setError("");
            if (!sourcePreviewOpen && sourceUrl) {
                openSourceDetailPage();
                return;
            }
            try {
                if (rawStreamRef.current) {
                    stopShare(sharing);
                }
                const rawStream = await navigator.mediaDevices.getDisplayMedia({
                    video: {
                        width: {ideal: SHARE_MAX_WIDTH},
                        height: {ideal: SHARE_MAX_HEIGHT},
                        frameRate: {ideal: SHARE_FRAME_RATE, max: SHARE_FRAME_RATE}
                    },
                    audio: true
                });
                rawStreamRef.current = rawStream;
                rawStream.getVideoTracks().forEach(track => {
                    track.contentHint = "detail";
                    if (typeof track.applyConstraints === "function") {
                        track.applyConstraints({
                            width: {ideal: SHARE_MAX_WIDTH},
                            height: {ideal: SHARE_MAX_HEIGHT},
                            frameRate: {ideal: SHARE_FRAME_RATE, max: SHARE_FRAME_RATE}
                        }).catch(() => {});
                    }
                });
                if (localVideoRef.current) {
                    localVideoRef.current.srcObject = rawStream;
                    localVideoRef.current.muted = true;
                    await localVideoRef.current.play();
                    await waitForVideoReady(localVideoRef.current);
                    setScreenshot(captureVideoFrame(localVideoRef.current));
                }
                startCropDrawing(rawStream);
                setCrop({...FULL_CROP});
                setEditingCrop(true);
                setCropConfirmed(false);
                rawStream.getTracks().forEach(track => {
                    track.addEventListener("ended", () => {
                        if (rawStreamRef.current === rawStream) {
                            stopShare(true);
                        }
                    }, {once: true});
                });
            } catch (ex) {
                setError(SHARE_TEXT.captureFailed);
                stopShare(false);
                setSourcePreviewOpen(true);
            }
        }

        async function startShare() {
            setError("");
            if (!localStreamRef.current || !rawStreamRef.current) {
                await captureForCrop();
                return;
            }
            if (!cropRef.current) {
                setError(SHARE_TEXT.cropRequired);
                return;
            }
            setCropConfirmed(true);
            setSharing(true);
            setEditingCrop(false);
            sendWs({type: "screen-share", active: true});
            await Promise.all(room.participants
                .filter(participant => participant.id !== participantId)
                .map(participant => offerTo(participant.id)));
        }

        function pointFromEvent(event) {
            const stage = cropStageRef.current;
            if (!stage || !imageBox.width || !imageBox.height) {
                return null;
            }
            const stageBox = stage.getBoundingClientRect();
            const x = (event.clientX - stageBox.left - imageBox.left) / imageBox.width * 100;
            const y = (event.clientY - stageBox.top - imageBox.top) / imageBox.height * 100;
            return {
                x: Math.max(0, Math.min(100, x)),
                y: Math.max(0, Math.min(100, y))
            };
        }

        function startDrawCrop(event) {
            if (!screenshot || (event.button !== undefined && event.button !== 0)) {
                return;
            }
            event.preventDefault();
            const startPoint = pointFromEvent(event);
            if (!startPoint) {
                return;
            }
            setCrop(clampCrop({x: startPoint.x, y: startPoint.y, width: 2, height: 2}));

            function move(moveEvent) {
                const nextPoint = pointFromEvent(moveEvent);
                if (nextPoint) {
                    setCrop(cropFromPoints(startPoint, nextPoint));
                }
            }

            function up() {
                window.removeEventListener("pointermove", move);
                window.removeEventListener("pointerup", up);
            }

            window.addEventListener("pointermove", move);
            window.addEventListener("pointerup", up);
        }

        function startCropPointer(event, mode) {
            if (!screenshot || !cropRef.current) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            const startPoint = pointFromEvent(event);
            if (!startPoint) {
                return;
            }
            const startCrop = cropRef.current;

            function move(moveEvent) {
                const nextPoint = pointFromEvent(moveEvent);
                if (!nextPoint) {
                    return;
                }
                const dx = nextPoint.x - startPoint.x;
                const dy = nextPoint.y - startPoint.y;
                let next = {...startCrop};
                if (mode === "move") {
                    next.x = startCrop.x + dx;
                    next.y = startCrop.y + dy;
                }
                if (mode.includes("w")) {
                    next.x = startCrop.x + dx;
                    next.width = startCrop.width - dx;
                }
                if (mode.includes("e")) {
                    next.width = startCrop.width + dx;
                }
                if (mode.includes("n")) {
                    next.y = startCrop.y + dy;
                    next.height = startCrop.height - dy;
                }
                if (mode.includes("s")) {
                    next.height = startCrop.height + dy;
                }
                setCrop(clampCrop(next));
            }

            function up() {
                window.removeEventListener("pointermove", move);
                window.removeEventListener("pointerup", up);
            }

            window.addEventListener("pointermove", move);
            window.addEventListener("pointerup", up);
        }

        function resetCrop() {
            setCrop({...FULL_CROP});
        }

        useEffect(() => {
            if (!isOwner || !sharing || !localStreamRef.current) {
                return;
            }
            room.participants
                .filter(participant => participant.id !== participantId)
                .forEach(participant => offerTo(participant.id));
        }, [room.participants, isOwner, participantId, offerTo, sharing]);

        useEffect(() => {
            if (!signalEvents || !signalEvents.length) {
                return;
            }

            async function handleSignal(signalEvent) {
                if (!signalEvent || signalEvent.targetId !== participantId) {
                    return;
                }
                const payload = signalEvent.payload;
                const fromId = signalEvent.fromId;

                if (payload.kind === "request-offer") {
                    if (isOwner && sharing && localStreamRef.current) {
                        await offerTo(fromId, {force: true});
                    }
                    return;
                }

                if (payload.kind === "offer") {
                    let peer = peersRef.current.get(fromId);
                    if (peer && (peer.remoteDescription || peer.localDescription || peer.connectionState !== "new")) {
                        closePeer(fromId);
                        peer = null;
                    }
                    peer = peer || createPeer(fromId);
                    await peer.setRemoteDescription(payload.sdp);
                    await flushQueuedIceCandidates(fromId, peer);
                    const answer = await peer.createAnswer();
                    await peer.setLocalDescription(answer);
                    sendSignal(fromId, {kind: "answer", sdp: peer.localDescription});
                } else if (payload.kind === "answer") {
                    const peer = createPeer(fromId);
                    await peer.setRemoteDescription(payload.sdp);
                    await flushQueuedIceCandidates(fromId, peer);
                } else if (payload.kind === "candidate" && payload.candidate) {
                    const peer = createPeer(fromId);
                    await addIceCandidateWhenReady(fromId, peer, payload.candidate);
                }
            }

            signalEvents.forEach(signalEvent => {
                const signalId = signalEvent.signalId || signalEvent.receivedAt;
                if (processedSignalIdsRef.current.has(signalId)) {
                    return;
                }
                processedSignalIdsRef.current.add(signalId);
                handleSignal(signalEvent).catch(() => setError(SHARE_TEXT.signalFailed));
            });
            if (processedSignalIdsRef.current.size > 400) {
                const activeIds = new Set(signalEvents.slice(-200).map(signalEvent => signalEvent.signalId || signalEvent.receivedAt));
                processedSignalIdsRef.current.forEach(signalId => {
                    if (!activeIds.has(signalId)) {
                        processedSignalIdsRef.current.delete(signalId);
                    }
                });
            }

        }, [signalEvents, participantId, closePeer, createPeer, sendSignal, isOwner, sharing, offerTo]);

        useEffect(() => () => stopShare(false), [stopShare]);

        if (isOwner) {
            const hasScreenshot = Boolean(screenshot);
            const showCropEditor = hasScreenshot && (!sharing || editingCrop);
            return h("div", {className: "share-panel"},
                h("div", {
                    className: "crop-stage " + (showCropEditor ? "selecting" : ""),
                    ref: cropStageRef,
                    onPointerDown: showCropEditor ? startDrawCrop : undefined
                },
                    h("video", {className: "capture-video-source", ref: localVideoRef, autoPlay: true, playsInline: true, muted: true}),
                    showCropEditor ? h("div", {
                        className: "capture-surface",
                        style: {
                            left: imageBox.left + "px",
                            top: imageBox.top + "px",
                            width: imageBox.width + "px",
                            height: imageBox.height + "px"
                        }
                    },
                        h("img", {src: screenshot.url, alt: "共享截图预览"}),
                        crop ? h("div", {
                            className: "crop-box",
                            style: {
                                left: crop.x + "%",
                                top: crop.y + "%",
                                width: crop.width + "%",
                                height: crop.height + "%"
                            },
                            onPointerDown: event => startCropPointer(event, "move")
                        },
                            h("span", {className: "crop-handle nw", onPointerDown: event => startCropPointer(event, "nw")}),
                            h("span", {className: "crop-handle ne", onPointerDown: event => startCropPointer(event, "ne")}),
                            h("span", {className: "crop-handle sw", onPointerDown: event => startCropPointer(event, "sw")}),
                            h("span", {className: "crop-handle se", onPointerDown: event => startCropPointer(event, "se")}),
                            h("span", {className: "crop-label"}, cropConfirmed ? SHARE_TEXT.liveBadge : SHARE_TEXT.pendingCrop)
                        ) : h("div", {className: "capture-hint"}, SHARE_TEXT.dragCrop)
                    ) : sharing ? h("video", {
                        className: "shared-output-video",
                        ref: ownerPreviewVideoRef,
                        autoPlay: true,
                        playsInline: true,
                        muted: true
                    }) : sourcePreviewOpen && sourceUrl ? h("div", {className: "source-preview"},
                        h("div", {className: "source-preview-bar"},
                            h("div", {className: "source-preview-title"},
                                h("strong", null, SHARE_TEXT.sourcePreviewTitle),
                                h("span", {className: "mono"}, sourceUrl)
                            ),
                            h("button", {type: "button", className: "secondary", onClick: openSourceDetailPage}, SHARE_TEXT.openSourcePage)
                        ),
                        h("div", {className: "source-preview-frame-wrap"},
                            h("iframe", {
                                className: "source-preview-frame",
                                src: sourceUrl,
                                title: SHARE_TEXT.sourcePreviewTitle,
                                referrerPolicy: "no-referrer",
                                allow: "autoplay; fullscreen; encrypted-media; picture-in-picture",
                                allowFullScreen: true
                            })
                        ),
                        h("div", {className: "source-preview-note"}, SHARE_TEXT.sourcePreviewNote)
                    ) : h("div", {className: "empty-share"}, SHARE_TEXT.chooseCaptureTarget)
                ),
                error ? h("div", {className: "notice error"}, error) : null,
                h("div", {className: "button-row"},
                    sharing
                        ? h("button", {className: "danger", onClick: () => stopShare(true)}, SHARE_TEXT.stopShare)
                        : hasScreenshot
                            ? h("button", {className: "success", disabled: !crop, onClick: startShare}, SHARE_TEXT.confirmCrop)
                            : h("button", {className: "success", onClick: captureForCrop}, sourcePreviewOpen ? SHARE_TEXT.chooseCaptureTarget : SHARE_TEXT.prepareCrop),
                    sharing && hasScreenshot ? h("button", {
                        type: "button",
                        className: "secondary",
                        onClick: () => setEditingCrop(value => !value)
                    }, editingCrop ? SHARE_TEXT.finishAdjust : SHARE_TEXT.adjustCrop) : null,
                    hasScreenshot && (!sharing || editingCrop) ? h("button", {type: "button", className: "secondary", onClick: resetCrop}, SHARE_TEXT.fullFrame) : null,
                    hasScreenshot ? h("button", {type: "button", className: "secondary", onClick: captureForCrop}, SHARE_TEXT.recapture) : null
                ),
                h("div", {className: "share-note"}, hasScreenshot
                    ? (sharing ? (editingCrop ? SHARE_TEXT.sharingLive : SHARE_TEXT.livePreview) : SHARE_TEXT.cropReady)
                    : SHARE_TEXT.sourcePreviewNote)
            );
        }

        return h("div", {className: "share-panel"},
            h("div", {className: "video-wrap"},
                h("video", {
                    className: "shared-output-video",
                    ref: remoteVideoRef,
                    autoPlay: true,
                    playsInline: true,
                    controls: true,
                    muted: remoteMuted,
                    onLoadedMetadata: () => playRemoteVideo(false),
                    onCanPlay: () => playRemoteVideo(false),
                    onPlaying: () => setRemotePlayPrompt("")
                }),
                room.screenShareActive && !remoteStreamReady ? h("div", {className: "empty-share"}, SHARE_TEXT.connectingShare) : null,
                remotePlayPrompt ? h("button", {
                    type: "button",
                    className: "player-resume",
                    onClick: () => playRemoteVideo(false)
                }, SHARE_TEXT.receiveShare) : null,
                remoteStreamReady && remoteMuted ? h("button", {
                    type: "button",
                    className: "player-resume audio",
                    onClick: () => playRemoteVideo(true)
                }, SHARE_TEXT.enableAudio) : null
            ),
            error ? h("div", {className: "notice error"}, error) : null,
            h("div", {className: "share-note"}, room.screenShareActive ? "正在接收共享画面" : "等待房主开始共享")
        );
    }

    function ChatPanel({messages, participantId, sendWs}) {
        const [collapsed, setCollapsed] = useState(false);
        const [text, setText] = useState("");
        const listRef = useRef(null);

        useEffect(() => {
            if (listRef.current) {
                listRef.current.scrollTop = listRef.current.scrollHeight;
            }
        }, [messages, collapsed]);

        function submit(event) {
            event.preventDefault();
            const clean = text.trim();
            if (!clean) {
                return;
            }
            sendWs({type: "chat", text: clean});
            setText("");
        }

        return h("section", {className: "sidebar-panel chat-panel " + (collapsed ? "collapsed" : "")},
            h("button", {className: "chat-toggle", onClick: () => setCollapsed(!collapsed)},
                h("span", null, "交流对话"),
                h("span", {className: "chat-count"}, collapsed ? "展开" : (messages.length ? messages.length + " 条" : "收起"))
            ),
            collapsed ? null : h("div", {className: "chat-body"},
                h("div", {className: "chat-list", ref: listRef},
                    messages.length
                        ? messages.map(message => h("div", {
                            className: "chat-message " + (message.senderId === participantId ? "mine" : ""),
                            key: message.id || message.receivedAt
                        },
                            h("div", {className: "chat-meta"},
                                h("span", null, message.senderName || "成员"),
                                h("span", null, message.owner ? "房主" : "房客")
                            ),
                            h("div", {className: "chat-text"}, message.text)
                        ))
                        : h("div", {className: "chat-empty"}, "还没有消息")
                ),
                h("form", {className: "chat-form", onSubmit: submit},
                    h("input", {
                        value: text,
                        maxLength: 500,
                        placeholder: "输入消息",
                        onChange: event => setText(event.target.value)
                    }),
                    h("button", {className: "success", disabled: !text.trim()}, "发送")
                )
            )
        );
    }

    function SourceSwitcher({room, participantId, setRoom, sendWs}) {
        const [sourceUrl, setSourceUrl] = useState(room.source.normalizedUrl);
        const [loading, setLoading] = useState(false);
        const [error, setError] = useState("");

        useEffect(() => {
            setSourceUrl(room.source.normalizedUrl);
        }, [room.source.normalizedUrl]);

        async function submit(event) {
            event.preventDefault();
            setError("");
            setLoading(true);
            try {
                const nextRoom = await api("/api/rooms/" + encodeURIComponent(room.id) + "/source", {
                    method: "PUT",
                    body: JSON.stringify({participantId, sourceUrl})
                });
                setRoom(nextRoom);
                sendWs({type: "room-refresh"});
            } catch (ex) {
                setError(ex.message);
            } finally {
                setLoading(false);
            }
        }

        return h("form", {className: "source-form", onSubmit: submit},
            h("label", null, "更换地址",
                h("textarea", {
                    value: sourceUrl,
                    rows: 4,
                    onChange: event => setSourceUrl(event.target.value)
                })
            ),
            error ? h("div", {className: "notice error"}, error) : null,
            h("button", {className: "secondary", disabled: loading || !sourceUrl.trim()}, loading ? "判断中" : "更换")
        );
    }

    function ParticipantList({room}) {
        return h("div", {className: "member-list"},
            room.participants.map(participant =>
                h("div", {className: "member", key: participant.id},
                    h("span", {className: "member-name"}, participant.displayName),
                    h("span", {className: "member-role"}, participant.owner ? "房主" : "房客")
                )
            )
        );
    }

    function copyText(text, successMessage) {
        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(text)
                .then(() => window.alert(successMessage))
                .catch(() => window.prompt("请手动复制", text));
        } else {
            window.prompt("请手动复制", text);
        }
    }

    ReactDOM.createRoot(document.getElementById("root")).render(h(App));
})();
