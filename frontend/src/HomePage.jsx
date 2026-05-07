import { Link } from "react-router-dom";

export default function HomePage() {
  return (
    <main className="page">
      <section className="card home-card">
        <h1>Spring AI + Ollama</h1>
        <p className="home-lead">
          Play with a local Ollama model through Spring AI: single-shot prompts or multi-turn chat with
          in-memory conversation state.
        </p>
        <ul className="home-actions">
          <li>
            <Link to="/prompt" className="home-link">
              Prompt
            </Link>
            <span className="muted"> — one request to </span>
            <code>/api/completion</code>
            <span className="muted">, no server-side thread.</span>
          </li>
          <li>
            <Link to="/chat" className="home-link">
              Chat
            </Link>
            <span className="muted"> — multi-turn </span>
            <code>/api/chat</code>
            <span className="muted"> with memory (lost on backend restart).</span>
          </li>
        </ul>
      </section>
    </main>
  );
}
