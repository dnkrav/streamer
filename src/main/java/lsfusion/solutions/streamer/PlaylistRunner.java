package lsfusion.solutions.streamer;

import lsfusion.base.ExceptionUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.data.value.DataObject;

import java.io.IOException;
import java.lang.ProcessBuilder;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

public class PlaylistRunner extends InternalAction {

    // Used to transfer parameters from the LSFusion module
    private final ClassPropertyInterface playlistInterface;

    public PlaylistRunner(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        playlistInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataObject commandArgs = context.getDataKeyValue(playlistInterface);
            runPlaylist(context, commandArgs);
        }
        catch (Exception e) {
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        }
    }

    private void runPlaylist(ExecutionContext<ClassPropertyInterface> context, DataObject commandArgs) throws IOException {
        try {
            // Initialise command arguments, hardcoded for flv stream
            List<String> params = Arrays.asList("ffmpeg",
                    // Hardcode parameters for now
                    // Read input at native frame rate
                    "-re",
                    // infinity loop http://underpop.online.fr/f/ffmpeg/help/main-options.htm.gz
                    "-stream_loop", "-1",
                    // run playlist https://trac.ffmpeg.org/wiki/Concatenate#demuxer
                    "-f", "concat",
                    // use absolute paths https://stackoverflow.com/questions/38996925/ffmpeg-concat-unsafe-file-name
                    "-safe", "0",
                    // link to file generated from the Playlist module
                    "-i", (String) findProperty("path[Playlist]").readClasses(context, commandArgs).getValue(),
                    // omit the decoding and encoding step, so it does only demuxing and muxing, less consumption
                    "-c","copy",
                    // video format
                    "-f","flv",
                    // Adding the RTMP resource
                    // rtmp://a.rtmp.youtube.com/live2 + / + stream key
                    (String) findProperty("rtmp").read(context));

            // Initialize and run the ffmpeg command
            ProcessBuilder runner = new ProcessBuilder(params);
            runner.start();

            // Success operation flag
            findProperty("isRunning[Playlist]").change(true, context, commandArgs);
        }
        catch (Exception e)
        {
            String[] stopArgs = {"pkill","-f","ffmpeg"};
            Runtime.getRuntime().exec(stopArgs);
        }
    }

}
