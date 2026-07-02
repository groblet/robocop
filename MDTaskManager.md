# MarkdownTaskManager: Core Summary & Actions

**BLUF:** Download `task-manager.html` from GitHub to manage local Markdown files as a browser-based Kanban board without servers, databases, or third-party tracking.

### 🛠️ Core Functionality
* **Zero Infrastructure:** Runs entirely inside a single standalone HTML file (`task-manager.html`) with no server dependencies or background processes.
* **Direct Storage:** Utilises browser File System Access API to read and write changes natively to your disk.
* **File Pair Architecture:** Automatically creates and updates `kanban.md` for active operations and `archive.md` for historical logs.
* **Browser Restrictions:** Requires Chromium engines (Chrome, Edge, Opera 86+); incompatible with Safari or Firefox due to API limitations.

### 🤖 AI Integration & Ecosystems
* **Assistant Automation:** Includes structured instruction templates (`CLAUDE.md`, `GEMINI.md`, `CHATGPT.md`) allowing AI tools to modify boards directly.
* **Claude Code Skill:** Contains a custom global command-line utility for automated tracking, updates, and task creation via terminal.
* **Git Native:** Simplifies tracking, merging, and versioning because all task states exist as standard human-readable text files inside repositories.

### 🚀 Immediate Action Required
1. **Download:** Obtain `task-manager.html` from the [MarkdownTaskManager Repository](https://github.com/ioniks/MarkdownTaskManager).
2. **Launch:** Double-click the file in a supported Chromium browser.
3. **Link:** Select your local project directory to initialise the workspace.
