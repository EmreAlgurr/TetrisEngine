-- ==========================================================
--  FCEUX Bridge for Custom NES Tetris AI Engine (Zero-Dependency)
--  Uses ultra-fast local file IPC (No external DLLs required!)
-- ==========================================================

local BRIDGE_DIR = "C:\\Users\\user\\Desktop\\TetrisEngine-main\\"
local REQ_FILE = BRIDGE_DIR .. "bridge_request.txt"
local RES_FILE = BRIDGE_DIR .. "bridge_response.txt"

-- NES RAM Adresleri (NTSC Tetris)
local ADDR_BOARD = 0x0400
local ADDR_CURRENT_PIECE = 0x0042
local ADDR_NEXT_PIECE = 0x0019
local ADDR_LEVEL = 0x0044
local ADDR_LINES_LO = 0x0050
local ADDR_LINES_HI = 0x0051
local ADDR_GAME_STATE = 0x0048

local PIECE_NAMES = {
    [2] = "T",
    [7] = "J",
    [8] = "Z",
    [10] = "O",
    [11] = "S",
    [14] = "L",
    [18] = "I"
}

local currentInputs = ""
local inputFrameIndex = 1
local lastHandledPiece = -1
local lastRequestToken = 0

function getEncodedBoard()
    local str = ""
    for i = 0, 199 do
        local val = memory.readbyte(ADDR_BOARD + i)
        if val == 0xEF or val == 0xFF or val == 0x00 then
            str = str .. "0"
        else
            str = str .. "1"
        end
    end
    return str
end

function bcdToDecimal(val)
    return (math.floor(val / 16) * 10) + (val % 16)
end

function getLines()
    local lo = bcdToDecimal(memory.readbyte(ADDR_LINES_LO))
    local hi = bcdToDecimal(memory.readbyte(ADDR_LINES_HI))
    return hi * 100 + lo
end

-- Java AI Engine ile ultra-hızlı haberleşme
function requestMove(curPiece, nextPiece, level, lines, boardStr)
    lastRequestToken = lastRequestToken + 1
    local reqData = string.format("%d|%s|%s|%d|%d|%s\n", lastRequestToken, curPiece, nextPiece, level, lines, boardStr)

    -- İstek dosyasını yaz
    local f = io.open(REQ_FILE, "w")
    if f then
        f:write(reqData)
        f:close()
    end

    -- Yanıtı bekle (maksimum 40ms)
    local startWait = os.clock()
    while (os.clock() - startWait) < 0.05 do
        local rf = io.open(RES_FILE, "r")
        if rf then
            local line = rf:read("*line")
            rf:close()
            if line then
                -- Token kontrolü (token|inputs)
                local sep = string.find(line, "|")
                if sep then
                    local resToken = tonumber(string.sub(line, 1, sep - 1))
                    local inputs = string.sub(line, sep + 1)
                    if resToken == lastRequestToken then
                        return inputs
                    end
                end
            end
        end
    end
    return ""
end

print("==================================================")
print(" NES TETRIS AI BRIDGE ONLINE (Zero-Dependency)    ")
print("==================================================")

while true do
    local gameState = memory.readbyte(ADDR_GAME_STATE)
    local rawCur = memory.readbyte(ADDR_CURRENT_PIECE)
    local rawNext = memory.readbyte(ADDR_NEXT_PIECE)
    local curName = PIECE_NAMES[rawCur] or "T"
    local nextName = PIECE_NAMES[rawNext] or "I"
    local level = memory.readbyte(ADDR_LEVEL)
    local lines = getLines()

    -- Yeni parça geldiğinde AI'dan hamle al
    if rawCur ~= lastHandledPiece and rawCur ~= 0 and gameState == 1 then
        lastHandledPiece = rawCur
        local boardStr = getEncodedBoard()
        currentInputs = requestMove(curName, nextName, level, lines, boardStr)
        inputFrameIndex = 1
        print(string.format(" Taş: %s | Next: %s | Tuşlar: %s", curName, nextName, currentInputs))
    end

    -- Tuşları emülatör joypad'ine aktar
    local joy = {A=false, B=false, left=false, right=false, down=false, up=false, start=false, select=false}

    if currentInputs ~= "" and inputFrameIndex <= string.len(currentInputs) then
        local ch = string.sub(currentInputs, inputFrameIndex, inputFrameIndex)
        if ch == "A" then joy.A = true
        elseif ch == "B" then joy.B = true
        elseif ch == "L" then joy.left = true
        elseif ch == "R" then joy.right = true
        elseif ch == "D" then joy.down = true
        end
        inputFrameIndex = inputFrameIndex + 1
    end

    joypad.set(1, joy)

    -- Ekrana HUD yaz
    gui.text(8, 8, "AI ENGINE: CONNECTED", "green")
    gui.text(8, 18, string.format("PIECE: %s | NEXT: %s", curName, nextName), "white")
    if currentInputs ~= "" and inputFrameIndex <= string.len(currentInputs) then
        gui.text(8, 28, "INPUT: " .. string.sub(currentInputs, inputFrameIndex), "yellow")
    end

    emu.frameadvance()
end
