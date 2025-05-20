-- Example Lua script demonstrating block highlighting
-- This shows how to highlight blocks around the player

-- Get player position
local pos = GetPlayerPos()
SendLog("Current position:", pos.x, pos.y, pos.z)

-- Convert player position to block coordinates (integer)
local blockX = math.floor(pos.x)
local blockY = math.floor(pos.y)
local blockZ = math.floor(pos.z)

SendLog("Current block position:", blockX, blockY, blockZ)

-- Highlight the block player is standing on with a semi-transparent blue
HighlightBlock(blockX, blockY-1, blockZ, "#0000FF", 0.5, "FILL")

-- Highlight player's position with red lines
HighlightBlock(blockX, blockY, blockZ, "#FF0000", 0.8, "LINE")

-- Create a small highlight pattern around the player
-- Cyan blocks with outlines
for x = -2, 2 do
    for z = -2, 2 do
        if x == 0 and z == 0 then
            -- Skip center, already highlighted
        else
            -- Make a checkboard pattern
            if (x + z) % 2 == 0 then
                HighlightBlock(blockX+x, blockY-1, blockZ+z, "#00FFFF", 0.3, "LINE")
            end
        end
    end
end

SendLog("Block highlighting completed!")

-- Wait a bit to see the highlights
Sleep(5000)

-- Highlight a specific block in distance
local targetX = blockX + 10
local targetY = blockY
local targetZ = blockZ + 10

-- Highlight with a green box
HighlightBlock(targetX, targetY, targetZ, "#00FF00", 1.0, "LINE")
SendLog("Highlighted distant block at:", targetX, targetY, targetZ) 