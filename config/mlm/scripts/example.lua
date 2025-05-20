-- Example MLM script
-- Tests various functions provided by the Most Legit Macro mod

-- Get and display player position
local pos = GetPlayerPos()
SendLog("Current position:", pos.x, pos.y, pos.z)
SendLog("Current rotation:", pos.pitch, pos.yaw)

-- Wait a moment
Sleep(1000)

-- Send a chat message
SendLog("Sending test message to chat...")
SendChatMessage("Hello from MLM script!")

-- Rotate to look up
SendLog("Rotating to look up...")
SimpleRotate(
    -45,   -- pitch (look up)
    pos.yaw, -- maintain current yaw
    5.0,   -- pitch speed (degrees/tick)
    5.0,   -- yaw speed (degrees/tick)
    0.1,   -- pitch acceleration (degrees/tick^2)
    0.1    -- yaw acceleration (degrees/tick^2)
)

-- Wait a moment
Sleep(2000)

-- Rotate to look around
SendLog("Rotating to look around...")
for i = 1, 4 do
    local newYaw = pos.yaw + (i * 90)
    SimpleRotate(
        0,    -- pitch (look straight)
        newYaw, -- rotate 90 degrees each time
        10.0,  -- pitch speed
        10.0,  -- yaw speed
        0.2,   -- pitch acceleration
        0.2    -- yaw acceleration
    )
    Sleep(500)
end

-- Return to original position
Sleep(1000)
SendLog("Returning to original rotation...")
SimpleRotate(
    pos.pitch,
    pos.yaw,
    15.0,
    15.0,
    0.3,
    0.3
)

-- Script complete
SendLog("Script completed successfully!") 