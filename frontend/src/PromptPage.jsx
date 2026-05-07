import { useMemo, useState } from "react";

const apiBase = import.meta.env.VITE_API_BASE_URL || "";

export default function PromptPage() {
  const [prompt, setPrompt] = useState("");
  const [response, setResponse] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const canSubmit = useMemo(() => prompt.trim().length > 0 && !loading, [prompt, loading]);

  const onPromptKeyDown = (event) => {
    if (event.key !== "Enter") {
      return;
    }
    if (event.shiftKey) {
      return;
    }
    event.preventDefault();
    if (!canSubmit) {
      return;
    }
    void submitPrompt(event);
  };

  const submitPrompt = async (event) => {
    event.preventDefault();
    if (!canSubmit) {
      return;
    }

    setLoading(true);
    setError("");
    setResponse("");

    try {
      const res = await fetch(`${apiBase}/api/completion`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt: prompt.trim() })
      });

      if (!res.ok) {
        const body = await res.text();
        throw new Error(body || `Request failed with status ${res.status}`);
      }

      const data = await res.json();
      setResponse(data.response || "");
    } catch (err) {
      setError(err.message || "Request failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="page">
      <section className="card">
        <h1>Prompt</h1>
        <p>Single-shot completion: one prompt in, one response out (no conversation memory).</p>

        <form onSubmit={submitPrompt}>
          <label htmlFor="prompt">Prompt</label>
          <textarea
            id="prompt"
            className="textarea-one-line"
            rows={1}
            value={prompt}
            onChange={(event) => setPrompt(event.target.value)}
            onKeyDown={onPromptKeyDown}
            placeholder="Ask something… (Enter to send, Shift+Enter for new line)"
          />
          <button type="submit" disabled={!canSubmit}>
            {loading ? "Generating..." : "Send Prompt"}
          </button>
        </form>

        {error && <div className="error">{error}</div>}

        <div className="response">
          <h2>Response</h2>
          <pre>{response || "No response yet."}</pre>
        </div>
      </section>
    </main>
  );
}
