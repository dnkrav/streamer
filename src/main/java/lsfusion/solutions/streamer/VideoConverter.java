package lsfusion.solutions.streamer;

import lsfusion.base.ExceptionUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.data.value.DataObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

public class VideoConverter extends InternalAction {

    // Used to transfer parameters from the LSFusion module
    private final ClassPropertyInterface videoInterface;

    public VideoConverter(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        videoInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataObject commandArgs = context.getDataKeyValue(videoInterface);
            convertVideo(context, commandArgs);
        }
        catch (Exception e) {
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        }
    }

    private void convertVideo(ExecutionContext<ClassPropertyInterface> context, DataObject commandArgs) throws IOException {
        // Alternative is to run command line process in asynchronious mode
        // https://stackoverflow.com/questions/30706704/java-run-async-processes
        // Here a synchronized block is used in order to wait() the conversion result:
        // https://examples.javacodegeeks.com/java-basics/exceptions/java-lang-illegalmonitorstateexception-how-to-solve-illegalmonitorstateexception/
        // and eliminate the IllegalMonitorStateException exception:
        // https://stackoverflow.com/questions/1537116/illegalmonitorstateexception-on-wait-call
        try {
            // Initialise command arguments, hardcoded for conversion into flv format
            // Hardcode parameters for now from the example: https://trac.ffmpeg.org/wiki/EncodingForStreamingSites
            // ffmpeg -i input.mov -c:v libx264 -preset medium -b:v 3000k -maxrate 3000k -bufsize 6000k -g 50 -c:a aac -b:a 128k -ac 2 -ar 44100 output.flv
            List<String> params = Arrays.asList("ffmpeg",
                    // Input file
                    "-i", (String) findProperty("filelink[Video]").readClasses(context, commandArgs).getValue(),
                    // Select codec for video
                    "-c:v",
                    // FLV doesn't support H.265, which is faster than H.264 below: https://trac.ffmpeg.org/ticket/6389
                    "libx264",
                    // Set a bench of encoder options, a balance between encoding speed and file size: https://trac.ffmpeg.org/wiki/Encode/H.264#Overwritingdefaultpresetsettings
                    "-preset", "medium",
                    // Set the video bitrate with tolerance
                    "-b:v", "3000K", "-maxrate", "3000k", "-bufsize", "6000k",
                    // Set the group of picture (GOP) size (default value is 12)
                    "-g", "50",
                    // Select codec for audio
                    "-c:a", "aac",
                    // Set the audio bitrate
                    "-b:a", "128k",
                    // Set the number of audio channels
                    "-ac", "2",
                    // Set the audio sampling frequency
                    "-ar", "44100",
                    // overwrite output
                    "-y",
                    // Output file
                    (String) findProperty("filelinkOut[Video]").readClasses(context, commandArgs).getValue());

            // Initialize and run the ffmpeg command
            ProcessBuilder converter = new ProcessBuilder(params);
            converter.redirectErrorStream(true);

            Process process = converter.start();

            process.waitFor();

            BufferedReader convertLog = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String info;
            while ((info = convertLog.readLine()) != null) {
                ServerLoggers.systemLogger.info(info);
            }

            // Success operation flag
            findProperty("isConverted[Video]").change(true, context, commandArgs);
        }
        catch (Exception e)
        {
            String[] stopArgs = {"pkill","-f","ffmpeg"};
            Runtime.getRuntime().exec(stopArgs);
        }
    }
}
