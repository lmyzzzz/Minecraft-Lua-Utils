-- Example script showing how to use highlight references
-- This demonstrates how to create and manage block highlights using the object references

-- Get player position
local pos = GetPlayerPos()
SendLog("Current position:", pos.x, pos.y, pos.z)

-- Convert player position to block coordinates (integer)
local blockX = math.floor(pos.x)
local blockY = math.floor(pos.y)
local blockZ = math.floor(pos.z)

SendLog("Current block position:", blockX, blockY, blockZ)

-- Create highlight objects using the reference-based approach
-- Highlight the block player is standing on with blue
local floorHighlight = HighlightBlock(blockX, blockY-1, blockZ, "#0000FF", 0.5, "FILL")
SendLog("Created floor highlight with ID:", floorHighlight.id)

-- Highlight player's position with red lines
local playerHighlight = HighlightBlock(blockX, blockY, blockZ, "#FF0000", 0.8, "LINE")

-- Create a pattern of blocks around the player with individual references
local highlights = {}
for x = -2, 2 do
    for z = -2, 2 do
        if x == 0 and z == 0 then
            -- Skip center, already highlighted
        else
            -- Make a checkboard pattern
            if (x + z) % 2 == 0 then
                local h = HighlightBlock(blockX+x, blockY-1, blockZ+z, "#00FFFF", 0.3, "LINE")
                table.insert(highlights, h)
            end
        end
    end
end

SendLog("Created", #highlights, "additional highlights")

-- Wait to see the highlights
Sleep(2000)

-- Demonstrate how to change highlight properties
SendLog("Changing floor highlight color...")
floorHighlight.setColor("#FF00FF", 0.7) -- Change to purple with higher opacity
Sleep(1000)

-- Change the render type of player highlight
SendLog("Changing player highlight type...")
playerHighlight.setType("FILL") 
Sleep(1000)

-- Remove half of the pattern highlights
SendLog("Removing half of the pattern highlights...")
for i = 1, #highlights, 2 do
    highlights[i].remove()
end
Sleep(1000)

-- Create a new highlight at a distance
SendLog("Creating a distant highlight...")
local distantHighlight = HighlightBlock(blockX + 10, blockY, blockZ + 10, "#00FF00", 1.0, "LINE")
Sleep(1000)

-- Check if highlights are still active
SendLog("Checking highlight states:")
SendLog("Floor highlight active:", floorHighlight.isActive())
SendLog("Player highlight active:", playerHighlight.isActive())
if #highlights > 0 then
    SendLog("First pattern highlight active:", highlights[1].isActive())
end
SendLog("Distant highlight active:", distantHighlight.isActive())

-- Remove all remaining highlights
SendLog("Removing all highlights...")
floorHighlight.remove()
playerHighlight.remove()
distantHighlight.remove()

for _, h in pairs(highlights) do
    h.remove()
end

SendLog("Done! All highlights should be removed.") 