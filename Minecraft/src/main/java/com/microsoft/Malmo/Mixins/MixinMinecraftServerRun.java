package com.microsoft.Malmo.Mixins;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.microsoft.Malmo.Utils.TimeHelper;
import com.microsoft.Malmo.Utils.TimeHelper.SyncManager;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.ReportedException;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.MinecraftForgeClient;


@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServerRun  {
    // /* Overrides methods within the MinecraftServer class.
    //  */

    @Shadow private long currentTime;
    @Shadow private ServerStatusResponse statusResponse;
    @Shadow private boolean serverRunning;
    @Shadow private String motd;
    @Shadow private long timeOfLastWarning;
    @Shadow private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger();
    @Shadow private boolean serverIsRunning;
    @Shadow private boolean serverStopped;
    @Shadow public WorldServer[] worlds;

    @Shadow public abstract boolean init() throws IOException;
    @Shadow public abstract void applyServerIconToResponse(ServerStatusResponse response);
    @Shadow public abstract void tick();
    @Shadow public abstract void finalTick(CrashReport report);
    @Shadow public abstract CrashReport addServerInfoToCrashReport(CrashReport report);
    @Shadow public abstract File getDataDirectory();
    @Shadow public abstract void stopServer();
    @Shadow public abstract void systemExitNow();
    @Shadow public abstract void initiateShutdown();

    public void run()
    {
        try
        {
            if (this.init())
            {
                net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStarted();
                this.currentTime = MinecraftServer.getCurrentTimeMillis();
                long i = 0L;
                TimeHelper.SyncManager.numTicks = 0;
                this.statusResponse.setServerDescription(new TextComponentString(this.motd));
                this.statusResponse.setVersion(new ServerStatusResponse.Version("1.11.2", 316));
                this.applyServerIconToResponse(this.statusResponse);

                while (this.serverRunning)
                {
                    TimeHelper.SyncManager.setServerRunning();
                    long k = MinecraftServer.getCurrentTimeMillis();
                    long j = k - this.currentTime;

                    if (j > 2000L && this.currentTime - this.timeOfLastWarning >= 15000L)
                    {
                        LOG.warn("Can\'t keep up! Did the system time change, or is the server overloaded? Running {}ms behind, skipping {} tick(s)", new Object[] {Long.valueOf(j), Long.valueOf(j / TimeHelper.serverTickLength)});
                        j = 2000L;
                        this.timeOfLastWarning = this.currentTime;
                    }

                    if (j < 0L)
                    {
                        LOG.warn("Time ran backwards! Did the system time change?");
                        j = 0L;
                    }

                    i += j;
                    this.currentTime = k;

                    if(i < 0){
                        i = 0L;
                    }

                    if (this.worlds[0].areAllPlayersAsleep())
                    {
                        this.tick();
                        i = 0L;
                    }
                    else
                    {

                        if (TimeHelper.SyncManager.isSynchronous() && TimeHelper.SyncManager.numTicks > 32){

                            if(TimeHelper.SyncManager.shouldServerTick() && 
                            (TimeHelper.SyncManager.numTicks > 32)
                            ){

                                // TimeHelper.SyncManager.debugLog("[SERVER] tick start." +Long.toString(SyncManager.numTicks));
                                this.tick();
                                // TimeHelper.SyncManager.debugLog("[SERVER] tick end." +Long.toString(SyncManager.numTicks));
                                TimeHelper.SyncManager.numTicks += 1;
                                TimeHelper.SyncManager.completeServerTick();
                            }
                        } else
                        {
                            // TimeHelper.SyncManager.debugLog("[SERVER] Regular ticking ! " +Long.toString(SyncManager.numTicks));
                            while (i > TimeHelper.serverTickLength )
                            {
                                i -= TimeHelper.serverTickLength;
                                if( !TimeHelper.isPaused()) {
                                    TimeHelper.SyncManager.numTicks += 1;
                                    this.tick();
                                } 
                            }


                            Thread.sleep(Math.max(1L, TimeHelper.serverTickLength - i));
                        }
                        
                    }
                    

                    this.serverIsRunning = true;
                }
                net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStopping();
                net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
            }
            else
            {
                net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
                this.finalTick((CrashReport)null);
            }
        }
        catch (net.minecraftforge.fml.common.StartupQuery.AbortedException e)
        {
            // ignore silently
            net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
        }
        catch (Throwable throwable1)
        {
            LOG.error("Encountered an unexpected exception", throwable1);
            CrashReport crashreport = null;

            if (throwable1 instanceof ReportedException)
            {
                crashreport = this.addServerInfoToCrashReport(((ReportedException)throwable1).getCrashReport());
            }
            else
            {
                crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable1));
            }

            File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (crashreport.saveToFile(file1))
            {
                LOG.error("This crash report has been saved to: {}", new Object[] {file1.getAbsolutePath()});
            }
            else
            {
                LOG.error("We were unable to save this crash report to disk.");
            }

            net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
            this.finalTick(crashreport);
        }
        finally
        {
            TimeHelper.SyncManager.serverFinished();
            try
            {
                this.stopServer();
                this.serverStopped = true;
            }
            catch (Throwable throwable)
            {
                LOG.error("Exception stopping the server", throwable);
            }
            finally
            {
                net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStopped();
                this.serverStopped = true;
                this.systemExitNow();
            }
        }
    }
}
