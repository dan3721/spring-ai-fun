import { BrowserRouter, Link, Route, Routes } from "react-router-dom";
import ChatPage from "./ChatPage";
import HomePage from "./HomePage";
import PromptPage from "./PromptPage";

export default function App() {
  return (
    <BrowserRouter>
      <header className="app-nav">
        <Link to="/" className="app-nav-brand-link">
          <span className="app-nav-brand">Spring AI + Ollama</span>
        </Link>
        <nav className="app-nav-links">
          <Link to="/">Home</Link>
          <Link to="/prompt">Prompt</Link>
          <Link to="/chat">Chat</Link>
        </nav>
      </header>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/prompt" element={<PromptPage />} />
        <Route path="/chat" element={<ChatPage />} />
      </Routes>
    </BrowserRouter>
  );
}
