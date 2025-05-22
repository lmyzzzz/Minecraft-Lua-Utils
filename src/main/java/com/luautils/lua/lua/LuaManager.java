package com.luautils.lua.lua;

import com.luautils.lua.lua.functions.*;
import com.luautils.lua.lua.functions.player.*;
import com.luautils.lua.lua.functions.utils.*;
import com.luautils.lua.lua.functions.network.*;
import com.luautils.lua.lua.functions.world.*;
//import com.luautils.lua.lua.functions.entity.*;
import com.luautils.lua.lua.functions.render.*;
import com.luautils.lua.lua.functions.algo.*;
//import com.luautils.lua.lua.functions.events.*;
//import com.luautils.lua.lua.functions.misc.*;
//import com.luautils.lua.lua.functions.items.*;
//import com.luautils.lua.lua.functions.game.*;
import com.luautils.lua.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseOsLib;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.LoadState;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class LuaManager {
    private Globals globals;
    private Thread scriptThread;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private ConcurrentLinkedQueue<Runnable> mainThreadTasks = new ConcurrentLinkedQueue<>();
    private final Map<String, Long> scriptLastModifiedTimes = new HashMap<>();
    private static final Random random = new Random();
    
    // 跟踪全局的执行计数，确保每次执行都有不同的标识
    private static long executionCounter = 0;
    
    public LuaManager() {
        // 初始化环境不再缓存
    }
    
    /**
     * 每次执行脚本前都完全重新创建Lua环境
     * 这可以确保没有任何缓存
     */
    private Globals createFreshLuaEnvironment() {
        System.out.println("[LuaUtils] Creating fresh Lua environment...");
        
        // 每次创建全新的环境
        Globals newGlobals = new Globals();
        
        // 加载基本库
        newGlobals.load(new JseBaseLib());
        newGlobals.load(new PackageLib());
        newGlobals.load(new Bit32Lib());
        newGlobals.load(new TableLib());
        newGlobals.load(new StringLib());
        newGlobals.load(new CoroutineLib());
        newGlobals.load(new JseMathLib());
        newGlobals.load(new JseOsLib());
        
        // 设置编译器和加载器
        LoadState.install(newGlobals);
        LuaC.install(newGlobals);
        
        // 禁用所有可能的缓存机制
        // 我们不使用反射直接清除内部缓存，因为这可能导致不可预测的行为
        // 相反，我们通过创建全新环境并使用唯一的脚本内容来避免缓存
        
        // 为环境增加额外的随机性，确保每次都创建全新的编译环境
        newGlobals.set("__luautils_timestamp", LuaValue.valueOf(System.nanoTime()));
        newGlobals.set("__luautils_random", LuaValue.valueOf(random.nextLong()));
        
        // 使用唯一标识确保环境不会被复用
        executionCounter++;
        String cacheBuster = "luautils_v1.1_" + System.currentTimeMillis() + "_" + random.nextInt(1000000) + "_" + executionCounter;
        newGlobals.set("__LUAUTILS_ENV_ID", LuaValue.valueOf(cacheBuster));
        
        // 注册函数
        registerLuaFunctions(newGlobals);
        
        System.out.println("[LuaUtils] Fresh Lua environment created with ID: " + cacheBuster);
        return newGlobals;
    }
    
    /**
     * 强制重新初始化Lua环境，清除所有缓存
     */
    public void initializeLuaEnvironment() {
        if (isRunning.get()) {
            stopCurrentScript();
        }
        
        System.out.println("[LuaUtils] Reinitializing Lua environment");
        globals = createFreshLuaEnvironment();
        
        // 清除文件修改时间缓存
        scriptLastModifiedTimes.clear();
        
        System.out.println("[LuaUtils] Lua environment reinitialized");
        ChatUtil.sendMessage("Lua environment has been fully reinitialized. All caches cleared.");
    }
    
    private void registerLuaFunctions(Globals g) {
        // 注册Lua函数
        g.set("SimpleRotate", new SimpleRotateFunction());
        g.set("GetPlayerPos", new GetPlayerPosFunction());
        g.set("Sleep", new SleepFunction());
        g.set("SendLog", new SendLogFunction());
        g.set("SendChatMessage", new SendChatMessageFunction());
        
        // 渲染相关函数
        g.set("HighlightBlock", new HighlightBlockFunction());
        g.set("RenderLine", new RenderLineFunction());
        g.set("ClearRender", new ClearRenderFunction());
        
        // 世界相关函数
        g.set("GetBlock", new GetBlockFunction());
        
        // 算法相关函数
        g.set("CalcViewPos", new CalcViewPosFunction());
        g.set("tb", new TbFunction());
        
        // 添加其他必要的函数...
        
        // 添加环境标识符，防止缓存
        long timestamp = System.currentTimeMillis();
        int randomValue = random.nextInt(Integer.MAX_VALUE);
        
        g.set("__LUAUTILS_CACHE_BUSTER_TIME", LuaValue.valueOf(timestamp));
        g.set("__LUAUTILS_CACHE_BUSTER_RANDOM", LuaValue.valueOf(randomValue));
        g.set("__LUAUTILS_CACHE_EXECUTION_COUNT", LuaValue.valueOf(executionCounter));
    }
    
    public void executeScript(final File scriptFile) {
        if (isRunning.get()) {
            stopCurrentScript();
        }
        
        isRunning.set(true);
        
        scriptThread = new Thread(() -> {
            try {
                // 获取文件最后修改时间
                long lastModified = scriptFile.lastModified();
                String filePath = scriptFile.getAbsolutePath();
                
                // 检查文件是否被修改
                boolean fileChanged = true;
                if (scriptLastModifiedTimes.containsKey(filePath)) {
                    if (scriptLastModifiedTimes.get(filePath) == lastModified) {
                        fileChanged = false;
                    }
                }
                
                // 更新文件修改时间缓存
                scriptLastModifiedTimes.put(filePath, lastModified);
                
                System.out.println("[LuaUtils] Executing script: " + filePath);
                System.out.println("[LuaUtils] Last modified: " + new java.util.Date(lastModified));
                if (fileChanged) {
                    System.out.println("[LuaUtils] Script file has been modified since last execution");
                }
                
                // ======== 关键部分：每次执行都创建全新的环境 ========
                globals = createFreshLuaEnvironment();
                
                // 读取文件内容
                String scriptContent = readFileContent(scriptFile);
                
                // 添加随机注释使每次执行的脚本内容都不同
                long uniqueTime = System.currentTimeMillis();
                int uniqueRandom = random.nextInt(1000000);
                String headerComment = String.format(
                    "-- LuaUtils Script Execution: ID=%d, Time=%d, Random=%d\n" +
                    "-- Cache prevention comment (DO NOT REMOVE): %d_%d\n",
                    executionCounter, uniqueTime, uniqueRandom,
                    System.nanoTime(), random.nextInt(Integer.MAX_VALUE)
                );
                
                // 构建带有唯一标识的脚本内容
                String finalScriptContent = headerComment + scriptContent;
                
                // 加载并执行脚本
                System.out.println("[LuaUtils] Loading script into fresh environment...");
                LuaValue chunk = globals.load(finalScriptContent, "@" + scriptFile.getName() + "_" + uniqueTime);
                
                System.out.println("[LuaUtils] Executing script in fresh environment...");
                chunk.call();
                
                System.out.println("[LuaUtils] Script execution completed: " + scriptFile.getName());
            } catch (LuaError e) {
                runOnMainThread(() -> {
                    ChatUtil.sendMessage("Script error: " + e.getMessage());
                });
                System.err.println("[LuaUtils] Lua error: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                runOnMainThread(() -> {
                    ChatUtil.sendMessage("Error executing script: " + e.getMessage());
                });
                System.err.println("[LuaUtils] Error executing script: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isRunning.set(false);
                System.out.println("[LuaUtils] Script execution thread terminated");
            }
        }, "LuaUtils-Script-" + executionCounter);
        
        scriptThread.setDaemon(true);
        scriptThread.start();
    }
    
    // 手动读取文件内容
    private String readFileContent(File file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
        }
        return content.toString();
    }
    
    public void stopCurrentScript() {
        if (isRunning.get() && scriptThread != null) {
            scriptThread.interrupt();
            isRunning.set(false);
            System.out.println("[LuaUtils] Script execution stopped");
        }
    }
    
    public boolean isScriptRunning() {
        return isRunning.get();
    }
    
    public void runOnMainThread(Runnable task) {
        mainThreadTasks.add(task);
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Runnable task;
            while ((task = mainThreadTasks.poll()) != null) {
                task.run();
            }
        }
    }
    
    public Minecraft getMinecraft() {
        return Minecraft.getMinecraft();
    }
} 