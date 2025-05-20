-- Example Lua Utils script
-- Use this as a template for your own scripts

-- Get and display player position
local pos = GetPlayerPos()
SendLog("Current position:", pos.x, pos.y, pos.z)
SendLog("Current rotation:", pos.pitch, pos.yaw)

-- Wait a moment
Sleep(1000)

-- Send a message to chat
SendLog("Hello from Lua Utils script!")
SendLog("T:",pos.yaw+180)
SimpleRotate(pos.pitch, pos.yaw+180, 8, 8, 10, 10)
SendChatMessage("I'm at "+pos.x+", "+pos.y+", "+pos.z)
