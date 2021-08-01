package lsfusion.streamer;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.data.value.DataObject;

import java.io.InputStream;
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

    private void runPlaylist(ExecutionContext<ClassPropertyInterface> context, DataObject commandArgs) throws IOException, SQLException {
        try (ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {

            // Initialise command
            List<String> params = Arrays.asList(new String[]{
                    "ffmpeg",
                    // Hardcode parameters for now
                    // Read input at native frame rate
                    "-re",
                    // infinity loop http://underpop.online.fr/f/ffmpeg/help/main-options.htm.gz
                    "-stream_loop", "-1",
                    // run playlist https://trac.ffmpeg.org/wiki/Concatenate#demuxer
                    "-f", "concat",
                    "-i", (String) findProperty("path[Playlist]").readClasses(context, commandArgs).getValue(),
                    // omit the decoding and encoding step, so it does only demuxing and muxing, less consumption
                    "-c","copy",
                    // video format
                    "-f","flv"
                    });

            // ToDo Gather parameters
            /*
            // Communicate with the table parameter of the CLASS Parameter directly
            KeyExpr paramExpr = new KeyExpr("parameter");
            ImRevMap<Object, KeyExpr> paramKeys = MapFact.singletonRev((Object) "parameter", paramExpr);
            QueryBuilder<Object, Object> paramQuery = new QueryBuilder<>(paramKeys);
            // select properties to be read from the database
            paramQuery.addProperty("name", LM.findProperty("name[Parameter]").getExpr(context.getModifier(), paramExpr));
            //paramQuery.addProperty("value", LM.findProperty("value[Parameter]").getExpr(paramExpr));
            // Execute query to the database
            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> paramResult = paramQuery.execute(context);
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> paramResult = paramQuery.execute(context);
            // Populate the parameters list
            for (int key = 0, size = paramResult.size(); key < size; key++) {
            //for (ImMap<Object, Object> key : paramResult.keyIt()) {
                ImMap<Object, Object> values = paramResult.getValue(key);
                params.add((String) values.get("name"));
                //params.add((String) paramResult.get(key).get("value").getValue());
            }
            */

            // Adding the RTMP resource
            params.add((String) findProperty("rtmp").read(context));
            ProcessBuilder runner = new ProcessBuilder(params);
            runner.start();
        }
        catch (Exception e)
        {
            String[] stopArgs = {"pkill","-f","ffmpeg"};
            Runtime.getRuntime().exec(stopArgs);
        }
    }

}
