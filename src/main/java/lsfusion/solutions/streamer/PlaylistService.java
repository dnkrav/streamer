package lsfusion.solutions.streamer;

import lsfusion.base.ExceptionUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PlaylistService extends InternalAction {
    // Used to transfer parameters from the LSFusion module
    private final ClassPropertyInterface playlistInterface;
    private final ClassPropertyInterface modeInterface;

    // Initialization at server start
    public PlaylistService(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        playlistInterface = i.next();
        modeInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context)
            throws SQLException, SQLHandledException {
        try {
            DataObject commandArgs = context.getDataKeyValue(playlistInterface);
            String modeSet = (String) context.getDataKeyValue(modeInterface).getValue();
            switch (modeSet) {
                case "Start" :
                    this.triggerPlaylist(context, commandArgs, true);
                    break;
                case "Stop" :
                    this.triggerPlaylist(context, commandArgs, false);
                    break;
                default:
                    throw new StringIndexOutOfBoundsException("Unknown action for the Streamer");
            }
        }
        catch (Exception e) {
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        }
    }

    private boolean updateRunnerLink(String updateTarget, String fileLink) {
        try {
            ServerLoggers.systemLogger.info("Update streamer link " + fileLink + " to playlist: " + updateTarget);
            List<String> paramLink = Arrays.asList("ln",
                    "-sf",
                    updateTarget,
                    fileLink);

            ProcessBuilder linkUpdate = new ProcessBuilder(paramLink);
            linkUpdate.redirectErrorStream(true);
            Process processLink = linkUpdate.start();
            boolean updateResult = processLink.waitFor(1000, TimeUnit.MILLISECONDS);

            BufferedReader updateLog = new BufferedReader(new InputStreamReader(processLink.getInputStream()));
            String info;
            while ((info = updateLog.readLine()) != null) {
                ServerLoggers.systemLogger.info(info);
            }

            return updateResult;
        } catch (IOException e) {
            ServerLoggers.systemLogger.error("Cannot update link to the actual playlist: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            ServerLoggers.systemLogger.error("Cannot update link to the actual playlist: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void triggerPlaylist(ExecutionContext<ClassPropertyInterface> context, DataObject commandArgs, boolean mode)
            throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        try {
            // Initialise command arguments to control the systemd service
            // The start | status | stop options will be subject to control by a state machine
            List<String> paramService = Arrays.asList("sudo",
                    // Privilege to run this command for certain service has to be enabled in sudoers.d file in advance
                    // Refer to conf folder for it
                    // Note, that desired location for systemctl is /usr/bin:
                    // https://www.freedesktop.org/wiki/Software/systemd/TheCaseForTheUsrMerge/
                    // https://lists.freedesktop.org/archives/systemd-devel/2019-August/043225.html
                    "/usr/bin/systemctl",
                    // Running option, space here to be replaced by exact command later
                    " ",
                    // name of the service from the conf subfolder of the repo
                    "streamer.service");

            // Prepare link to proper playlist for the service using simple bash command
            String filePL = (String) findProperty("path[Playlist]").readClasses(context, commandArgs).getValue();
            String linkPL = (String) findProperty("playlistLinkService[]").readClasses(context).getValue();

            // State Machine on the ffmpeg related systemd service
            String nextCommand = "status";
            for (int i = 0; i < 2; i++) {
                paramService.set(2, nextCommand);
                switch (nextCommand) {
                    case "status" :
                        // @ToDo Handle statuses and exceptions properly
                        if(mode) {
                            nextCommand = "start";
                        } else {
                            nextCommand = "stop";
                        }
                        break;
                    case "start" :
                        if(!updateRunnerLink(filePL, linkPL)) {
                            throw new IOException("Cannot update streamer playlist link");
                        }

                        ProcessBuilder servicePL = new ProcessBuilder(paramService);
                        Process processPlay = servicePL.start();
                        processPlay.waitFor();

                        findProperty("isRunning[Playlist]").change(true, context, commandArgs);
                        break;
                    case "stop" :
                        ProcessBuilder serviceStop = new ProcessBuilder(paramService);
                        Process processStop = serviceStop.start();
                        processStop.waitFor();
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            ServerLoggers.systemLogger.error("Cannot start the operating system process: " + e.getMessage());
        } catch (InterruptedException e) {
            ServerLoggers.systemLogger.error("Unable to complete the operating system process : " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
