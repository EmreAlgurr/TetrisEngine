# NES Tetris AI Engine

A Java-based artificial intelligence engine that plays the original Nintendo NES Tetris (1989) autonomously in real time on an NES emulator (FCEUX).

This project is not a game clone mimicking mechanics; it hooks into the RAM memory addresses of the FCEUX emulator, reads game state directly from emulator memory, computes the optimal placement and controller inputs, and feeds actions directly into the emulator via a virtual controller.

Algorithm design and decision heuristics are inspired by strategies from the competitive NES Tetris bot [StackRabbit](https://github.com/GICodeWarrior/StackRabbit).

---

## How It Works

The system operates via inter-process communication between two core components:

```
[ NES Tetris (FCEUX) ] 
         │ (RAM Read: 0x0400, 0x0042, 0x0019...)
         ▼
[ fceux_bridge.lua ] ──(IPC: bridge_request.txt)──► [ TetrisEngineServer.java ]
         ▲                                                    │ (2-Ply Lookahead Search)
         │ ◄──(IPC: bridge_response.txt / Token)───────────────┘ (Heuristic Evaluation)
         │
[ Key Inputs: A.R.R.D ] ──► Fed to Virtual Controller
```

1. **Memory Inspection (FCEUX Lua):** Every frame, `fceux_bridge.lua` inspects NES memory to extract the board matrix (`0x0400`), active tetromino (`0x0042`), next tetromino (`0x0019`), current level, and line count.
2. **IPC Bridge:** To avoid external socket libraries or dynamic DLL dependencies inside the FCEUX environment, a lightweight file-based (or HTTP) token synchronization mechanism is used. When a request is ready, it is dispatched to the Java server.
3. **2-Ply Decision Engine (`TetrisAI.java`):** Explores all valid rotation and column placements for the active piece. After each candidate placement, it simulates the "Next" piece, evaluating 2 moves ahead (lookahead search).
4. **Key Sequence Generation (`generateInputs`):** The chosen target rotation and column are translated into NES D-Pad keystrokes (`A.` rotate, `L.`/`R.` horizontal shift, `D` soft drop) and fed frame-by-frame into the emulator's controller input.

---

## Decision Engine & Heuristics

The board evaluation function (`evaluateBoard`) is built upon the following core heuristics:

- **Right-Well Tetris Strategy:** Column 9 (rightmost column) is kept open to clear 4 lines at once (Tetris) when an 'I' piece arrives. Accidentally covering the well incurs a heavy penalty.
- **Surface Flatness & Flat Spot:** Height disparities between adjacent columns are penalized exponentially. To ensure 'O' and 'L' pieces can be placed smoothly, the engine searches for at least one flat surface (`diff == 0`).
- **Built-Out Left:** Due to DAS (Delayed Auto Shift) mechanics in NES Tetris, shifting pieces to the left wall carries higher risk than shifting right. Consequently, maintaining slightly higher left columns relative to the right side is favored.
- **Hole & Blockage Penalties:** Empty cells beneath placed blocks (holes) and filled cells directly above holes (blockades/buries) are severely penalized.
- **Panic Mode:** When board height exceeds 10 rows, well-preservation is suspended; the engine prioritizes board clearing with singles, doubles, or triples to survive.

---

## Requirements

- **Java JDK 8 or higher** (No external libraries or third-party dependencies required)
- **FCEUX Emulator** (Tested on Windows)
- **Tetris (USA).nes** ROM file

---

## Setup & Running

### 1. Compile and Start the Engine

Open a terminal in the project directory, compile, and run the server:

```bash
javac *.java
java TetrisEngineServer
```

When started, the server activates both an HTTP listener on port `3000` and a local file-based IPC listener for FCEUX.

### 2. Run the Lua Bridge inside FCEUX

1. Open FCEUX and load the **Tetris (USA)** ROM.
2. Open `fceux_bridge.lua` in a text editor and ensure the `BRIDGE_DIR` variable at the top points to your project directory:
   ```lua
   local BRIDGE_DIR = "C:\\Users\\user\\Desktop\\TetrisEngine-main\\"
   ```
3. In the FCEUX menu, navigate to:
   **File > Lua > New Lua Script Window**.
4. Click **Browse**, select `fceux_bridge.lua`, and click **Run**.
5. Start a game in A-Type mode (Level 0, 9, or 18).

A green `AI ENGINE: ONLINE` overlay will appear on screen, and the bot will autonomously begin placing pieces. The console logs execution time and details for every placement:

```text
⚡ [Lvl 19 | Lines 042] Piece: T  | Next: I  | Decision: Rot=2, Col=5 | Keys: A.R.R.D | 2ms
```

---

## Headless Benchmark (Without Emulator)

To benchmark decision engine performance and Tetris Rate (TRT) without launching the emulator:

```bash
java TestBotBenchmark
```

This runs a 1,000-piece simulated game and outputs statistics including placed pieces, single/double/triple clears, total Tetris count, and Tetris Rate (TRT).

---

## File Structure

- `TetrisEngineServer.java` — Server bridging communication with the emulator via file-based IPC.
- `TetrisAI.java` — 2-ply search algorithm, board evaluation heuristics, and input sequence generator.
- `Tetromino.java` — Original NES Tetris piece definitions, rotation states, and coordinate offsets.
- `TestBotBenchmark.java` — Standalone benchmark simulation measuring algorithm speed and robustness.
- `fceux_bridge.lua` — Lua bridge script running inside FCEUX, inspecting RAM addresses and injecting controller inputs.
