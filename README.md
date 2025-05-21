!!THIS PROJECT IS CURRENTLY UNCOMPLETED.!!

# Lua Utils

Lua Utils is an advanced Lua script executor for Minecraft 1.8.9 that allows you to automate various in-game actions using Lua scripts.


### Commands

- `/lua execute <script>` - Execute a Lua script from the config/luautils/scripts directory
- `/lua stop` - Stop the currently running script
- `/lua list` - List all available scripts
- `/lua reload` - Force reload the Lua environment and clear all script caches
- `/lua resetrender` - Clear all rendering effects (highlights) created by scripts

### Creating Scripts

Create Lua scripts in the `.minecraft/config/luautils/scripts` directory. Use the `.lua` file extension.


### Script Caching

LuaUtils mod now **completely bypasses** all Lua script caching. Each time you execute a script using `/lua execute <script>`:

1. A fresh Lua environment is created
2. The script file is loaded directly from disk using a custom file reader
3. The script is parsed and executed without any caching

This guarantees that any changes you make to your scripts will take effect immediately, even if you just saved the file seconds ago.

If you still experience any issues with script changes not being applied, try:
1. Using the `/lua reload` command to force a complete reinitialization of the Lua environment
2. Checking file permissions and ensuring the file is not locked by another process
3. Verifying there are no syntax errors in your script


## Building from Source

1. Clone the repository
2. Run `./gradlew build` or `./gradlew clean build` to build the mod
3. The compiled jar will be in the `build/libs` directory





### Script Path

The mod will create a scripts folder in the `.minecraft/config/luautils/scripts/` directory. When you first start Minecraft with the mod, it will automatically create an example script in this directory.

### Mod Compatibility

This mod is only compatible with Minecraft 1.8.9 and Forge 11.15.1.2318. Using other versions may result in errors or failure to load. 
