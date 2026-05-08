import { useCallback, useEffect, useMemo, useRef, useState } from "react";

const apiBase = import.meta.env.VITE_API_BASE_URL || "";
const STORAGE_KEY = "springAiChatConversationId";

function loadOrCreateConversationId() {
  try {
    const existing = sessionStorage.getItem(STORAGE_KEY);
    if (existing) {
      return existing;
    }
  } catch {
    /* ignore */
  }
  const id = crypto.randomUUID();
  try {
    sessionStorage.setItem(STORAGE_KEY, id);
  } catch {
    /* ignore */
  }
  return id;
}

export default function ChatPage() {
  const [conversationId, setConversationId] = useState(loadOrCreateConversationId);
  const [turns, setTurns] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const turnsScrollRef = useRef(null);

  const canSend = useMemo(() => input.trim().length > 0 && !loading, [input, loading]);

  const loadHistory = useCallback(async (id) => {
    try {
      const res = await fetch(`${apiBase}/api/chat/${encodeURIComponent(id)}/messages`);
      if (!res.ok) {
        return;
      }
      const data = await res.json();
      const list = Array.isArray(data.messages) ? data.messages : [];
      setTurns(
        list.map((m) => ({
          role: m.role || "unknown",
          content: m.content || "",
          toolNames: Array.isArray(m.toolNames) ? m.toolNames : []
        }))
      );
    } catch {
      /* ignore hydrate errors */
    }
  }, []);

  useEffect(() => {
    loadHistory(conversationId);
  }, [conversationId, loadHistory]);

  useEffect(() => {
    const el = turnsScrollRef.current;
    if (!el) {
      return;
    }
    const id = requestAnimationFrame(() => {
      el.scrollTop = el.scrollHeight;
    });
    return () => cancelAnimationFrame(id);
  }, [turns, loading]);

  const newChat = () => {
    const id = crypto.randomUUID();
    try {
      sessionStorage.setItem(STORAGE_KEY, id);
    } catch {
      /* ignore */
    }
    setConversationId(id);
    setTurns([]);
    setError("");
    setInput("");
  };

  const sendMessage = async (event) => {
    event.preventDefault();
    if (!canSend) {
      return;
    }

    const text = input.trim();
    setInput("");
    setLoading(true);
    setError("");

    setTurns((prev) => [...prev, { role: "user", content: text }]);

    try {
      const res = await fetch(`${apiBase}/api/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ conversationId, message: text })
      });

      if (!res.ok) {
        const body = await res.text();
        throw new Error(body || `Request failed with status ${res.status}`);
      }

      const data = await res.json();
      const nextId = data.conversationId || conversationId;
      if (nextId !== conversationId) {
        try {
          sessionStorage.setItem(STORAGE_KEY, nextId);
        } catch {
          /* ignore */
        }
        setConversationId(nextId);
      }
      const assistantText = data.content || "";
      const toolsUsed = Array.isArray(data.toolsUsed) ? data.toolsUsed : [];
      setTurns((prev) => [
        ...prev,
        { role: "assistant", content: assistantText, toolNames: toolsUsed }
      ]);
    } catch (err) {
      setTurns((prev) => prev.slice(0, -1));
      setError(err.message || "Request failed");
    } finally {
      setLoading(false);
    }
  };

  const onMessageKeyDown = (event) => {
    if (event.key !== "Enter") {
      return;
    }
    if (event.shiftKey) {
      return;
    }
    event.preventDefault();
    if (!canSend) {
      return;
    }
    void sendMessage(event);
  };

  return (
    <main className="page">
      <section className="card chat-card">
        <div className="chat-header">
          <div>
            <h1>Chat (with memory)</h1>
            <p className="chat-sub">
              Server-side memory per conversation id. In-memory only — lost on backend restart. With{" "}
              <code>TAVILY_API_KEY</code>, include an explicit web search request in your message (e.g.{" "}
              <code>web search for …</code> or <code>search the web for …</code>) to enable the{" "}
              <code>web_search</code> tool on that turn.
            </p>
          </div>
          <button type="button" className="btn-secondary" onClick={newChat}>
            New chat
          </button>
        </div>

        <p className="conversation-id">
          <span className="muted">Conversation</span>{" "}
          <code>{conversationId}</code>
        </p>

        <div ref={turnsScrollRef} className="turns" aria-live="polite">
          {turns.length === 0 && <p className="muted">No messages yet. Say hello below.</p>}
          {turns.map((turn, index) => (
            <div key={`${index}-${turn.role}`} className={`turn turn-${turn.role}`}>
              {(turn.toolNames?.length ?? 0) > 0 && (
                <div className="turn-tools" aria-label="Tools used">
                  {(turn.toolNames ?? []).map((name) => (
                    <span key={name} className="tool-chip">
                      {name}
                    </span>
                  ))}
                </div>
              )}
              <pre className="turn-content">{turn.content}</pre>
            </div>
          ))}
        </div>

        {error && <div className="error">{error}</div>}

        <form className="chat-form" onSubmit={sendMessage}>
          <label htmlFor="chat-input">Message</label>
          <textarea
            id="chat-input"
            className="textarea-one-line"
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onMessageKeyDown}
            placeholder="Type a message… (Enter to send, Shift+Enter for new line)"
            disabled={loading}
          />
          <button type="submit" disabled={!canSend}>
            {loading ? "Sending..." : "Send"}
          </button>
        </form>
      </section>
    </main>
  );
}
