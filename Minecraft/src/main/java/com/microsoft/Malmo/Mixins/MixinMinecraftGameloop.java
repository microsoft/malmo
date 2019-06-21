package com.microsoft.Malmo.Mixins;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.FutureTask;

import com.microsoft.Malmo.Utils.TimeHelper;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.glu.GLU;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import akka.actor.FSM.TimeoutMarker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.achievement.GuiAchievement;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.profiler.Profiler;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Snooper;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.Timer;
import net.minecraft.util.Util;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

@Mixin(Minecraft.class) 
public abstract class MixinMinecraftGameloop {
    @Shadow public  Profiler mcProfiler;
    @Shadow private SoundHandler mcSoundHandler;
    @Shadow public abstract void shutdown();
    @Shadow public boolean isGamePaused;
    @Shadow public WorldClient world;
    @Shadow public Timer timer;
    @Shadow public  Queue < FutureTask<? >> scheduledTasks;
    @Shadow public abstract void runTick() throws IOException;

    @Shadow public abstract void checkGLError(String message);
    @Shadow public abstract void displayDebugInfo(long elapsedTicksTime);
    @Shadow public EntityPlayerSP player;
    @Shadow private Framebuffer framebufferMc;
    @Shadow public boolean skipRenderWorld;
    @Shadow public EntityRenderer entityRenderer;
    @Shadow public GameSettings gameSettings;
    @Shadow long prevFrameTime;
    @Shadow public GuiAchievement guiAchievement;
    @Shadow int displayWidth;
    @Shadow public PlayerControllerMP playerController;
    @Shadow private NetworkManager myNetworkManager;


    
    @Shadow int displayHeight;
    @Shadow public abstract void updateDisplay();
    @Shadow private int fpsCounter;
    @Shadow public abstract boolean  isSingleplayer();
    @Shadow public GuiScreen currentScreen;
    @Shadow private IntegratedServer theIntegratedServer;
    @Shadow public  FrameTimer frameTimer;
    /** Time in nanoseconds of when the class is loaded */
    @Shadow long startNanoTime;
    @Shadow private long debugUpdateTime;
    @Shadow public String debug;
    @Shadow public Snooper usageSnooper;
    @Shadow public abstract int getLimitFramerate();
    @Shadow public abstract boolean isFramerateLimitBelowMax();
    private  int numTicksPassed = 0;
    long lastUpdateTime = -1;

    private static long MIN_TIME_TO_UPDATE = (long)1e9;

    private void checkUpdateDisplay(){
        long curTime = System.nanoTime();
        if(lastUpdateTime == -1)
            lastUpdateTime = curTime;
        if(curTime - lastUpdateTime > MIN_TIME_TO_UPDATE){
            this.updateDisplay();
            lastUpdateTime = curTime;
        }
    }

    private void runGameLoop() throws IOException
    {

        long i = System.nanoTime();
        this.mcProfiler.startSection("root");

        if (Display.isCreated() && Display.isCloseRequested())
        {
            this.shutdown();
        }


        float f = this.timer.renderPartialTicks;
        if (this.isGamePaused && this.world != null)
        {
            this.timer.updateTimer();
            this.timer.renderPartialTicks = f;
        }
        else
        {
            this.timer.updateTimer();
        }

        this.mcProfiler.startSection("scheduledExecutables");

        synchronized (this.scheduledTasks)
        {
            while (!this.scheduledTasks.isEmpty())
            {
                // TODO: MAke logger public
                Util.runTask((FutureTask)this.scheduledTasks.poll(), Minecraft.LOGGER);
            }
        }

        this.mcProfiler.endSection(); //scheduledExecutables
        long l = System.nanoTime();

        if(
            (TimeHelper.SyncManager.isSynchronous() && TimeHelper.SyncManager.isServerRunning() && !this.isGamePaused ) 
            || TimeHelper.SyncManager.shouldFlush()){
            this.mcProfiler.startSection("waitForTick");

            // TimeHelper.SyncManager.debugLog("[Client] Waiting for tick request!");

            // Wait for the shouldClientTick to be true!
            while(!TimeHelper.SyncManager.shouldClientTick()) {
                checkUpdateDisplay();
                Thread.yield();
            }
            this.mcProfiler.endSection();
            this.mcProfiler.startSection("syncTickEventPre");


            // TimeHelper.SyncManager.debugLog("[Client] Starting client tick.");

            MinecraftForge.EVENT_BUS.post(new TimeHelper.SyncTickEvent(Phase.START));
            this.mcProfiler.endSection();
            this.mcProfiler.startSection("clientTick");


            if(TimeHelper.SyncManager.shouldClientTick()){
                this.runTick();

                TimeHelper.SyncManager.completeClientTick();
            } 
            else{
                this.timer.renderPartialTicks = f;
            }
            
            this.mcProfiler.endSection(); //ClientTick
            this.mcProfiler.startSection("serverTick");

            // Wait for the server tick to finish.
            // TimeHelper.SyncManager.debugLog("[Client] Client tick end. Client Waiting for server to tick!");
            while(!TimeHelper.SyncManager.shouldRenderTick()) {
                Thread.yield();
            }

            this.mcProfiler.endSection(); //serverTick


         } else{
            for (int j = 0; j < this.timer.elapsedTicks; ++j)
            {
                this.runTick();
            }
        }


        this.mcProfiler.startSection("preRenderErrors");
        long i1 = System.nanoTime() - l;
        this.checkGLError("Pre render");
        this.mcProfiler.endSection();
        this.mcProfiler.startSection("sound");
        this.mcSoundHandler.setListener(this.player, this.timer.renderPartialTicks);
        // this.mcProfiler.endSection();
        this.mcProfiler.endSection();
        this.mcProfiler.startSection("render");
        GlStateManager.pushMatrix();
        GlStateManager.clear(16640);
        this.framebufferMc.bindFramebuffer(true);
        this.mcProfiler.startSection("display");
        GlStateManager.enableTexture2D();
        this.mcProfiler.endSection(); //display

        if (!this.skipRenderWorld)
        {
            net.minecraftforge.fml.common.FMLCommonHandler.instance().onRenderTickStart(this.timer.renderPartialTicks);
            this.mcProfiler.endStartSection("gameRenderer");
            this.entityRenderer.updateCameraAndRender(this.timer.renderPartialTicks, i);
            this.mcProfiler.endSection();
            net.minecraftforge.fml.common.FMLCommonHandler.instance().onRenderTickEnd(this.timer.renderPartialTicks);
        }

        this.mcProfiler.endSection(); ///root
        if (this.gameSettings.showDebugInfo && this.gameSettings.showDebugProfilerChart && !this.gameSettings.hideGUI)
        {
            if (!this.mcProfiler.profilingEnabled)
            {
                this.mcProfiler.clearProfiling();
            }

        this.mcProfiler.profilingEnabled = true;

            this.displayDebugInfo(i1);
        }
        else
        {
            this.mcProfiler.profilingEnabled = false;
            this.prevFrameTime = System.nanoTime();
        }

        this.guiAchievement.updateAchievementWindow();
        this.framebufferMc.unbindFramebuffer();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        this.framebufferMc.framebufferRender(this.displayWidth, this.displayHeight);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        this.entityRenderer.renderStreamIndicator(this.timer.renderPartialTicks);
        GlStateManager.popMatrix();

        this.mcProfiler.startSection("root");
        this.updateDisplay();
        lastUpdateTime = System.nanoTime();
        if(
            (TimeHelper.SyncManager.isSynchronous() && 
            TimeHelper.SyncManager.isServerRunning() && 
            TimeHelper.SyncManager.shouldRenderTick() &&
            TimeHelper.SyncManager.isTicking()) || TimeHelper.SyncManager.shouldFlush()){
            
            

            this.mcProfiler.startSection("syncTickEventPost");
            MinecraftForge.EVENT_BUS.post(new TimeHelper.SyncTickEvent(Phase.END));
            this.mcProfiler.endSection();
            
            // TimeHelper.SyncManager.debugLog("[Client] Tick fully complete..");
                
            TimeHelper.SyncManager.completeTick();


        }
        Thread.yield();
        this.checkGLError("Post render");
        ++this.fpsCounter;
        this.isGamePaused = this.isSingleplayer() && this.currentScreen != null && this.currentScreen.doesGuiPauseGame() && !this.theIntegratedServer.getPublic();
        long k = System.nanoTime();
        this.frameTimer.addFrame(k - this.startNanoTime);
        this.startNanoTime = k;

        while (Minecraft.getSystemTime() >= this.debugUpdateTime + 1000L)
        {
            // TODO: Add to CFG and make public.
            Minecraft.debugFPS = this.fpsCounter;
            this.debug = String.format("%d fps (%d chunk update%s) T: %s%s%s%s%s", new Object[] {Integer.valueOf(Minecraft.debugFPS), Integer.valueOf(RenderChunk.renderChunksUpdated), RenderChunk.renderChunksUpdated == 1 ? "" : "s", (float)this.gameSettings.limitFramerate == GameSettings.Options.FRAMERATE_LIMIT.getValueMax() ? "inf" : Integer.valueOf(this.gameSettings.limitFramerate), this.gameSettings.enableVsync ? " vsync" : "", this.gameSettings.fancyGraphics ? "" : " fast", this.gameSettings.clouds == 0 ? "" : (this.gameSettings.clouds == 1 ? " fast-clouds" : " fancy-clouds"), OpenGlHelper.useVbo() ? " vbo" : ""});
            RenderChunk.renderChunksUpdated = 0;
            this.debugUpdateTime += 1000L;
            this.fpsCounter = 0;
            this.usageSnooper.addMemoryStatsToSnooper();

            if (!this.usageSnooper.isSnooperRunning())
            {
                this.usageSnooper.startSnooper();
            }
        }

        if (this.isFramerateLimitBelowMax())
        {
            this.mcProfiler.startSection("fpslimit_wait");
            Display.sync(this.getLimitFramerate());
            this.mcProfiler.endSection();
        }

        this.mcProfiler.endSection(); //root
    }
}