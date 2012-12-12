package org.adbcj.h2.server.decoding;

import org.adbcj.h2.decoding.IoUtils;
import org.adbcj.h2.decoding.ResultOrWait;
import org.adbcj.h2.server.responses.QueryResponse;
import org.h2.command.Command;
import org.h2.result.ResultInterface;
import org.h2.value.Value;
import org.jboss.netty.channel.Channel;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author roman.stoffel@gamlor.info
 */
class ExecuteQuery implements DecoderState {
    private final AcceptCommands acceptCommands;

    public ExecuteQuery(AcceptCommands acceptCommands) {
        this.acceptCommands = acceptCommands;
    }

    @Override
    public ResultAndState decode(DataInputStream stream, Channel channel) throws IOException {
        final ResultOrWait<Integer> id = IoUtils.tryReadNextInt(stream, ResultOrWait.Start);
        final ResultOrWait<Integer> objectId = IoUtils.tryReadNextInt(stream, id);
        final ResultOrWait<Integer> maxRows = IoUtils.tryReadNextInt(stream, objectId);
        final ResultOrWait<Integer> fetchSize = IoUtils.tryReadNextInt(stream, maxRows);
        final ResultOrWait<Integer> paramSize = IoUtils.tryReadNextInt(stream, fetchSize);
        final ResultOrWait<List<Value>> paramsData = ReadUtils.tryReadParams(stream, paramSize);

        if(paramsData.couldReadResult){
            Command command = (Command) acceptCommands.cache().getObject(id.result, false);
            ResultInterface result;
            int old = acceptCommands.session().getModificationId();
            synchronized (acceptCommands.session()) {
                result = command.executeQuery(maxRows.result, false);
            }
            acceptCommands.cache().addObject(objectId.result, result);
            int state = acceptCommands.getState(old);
            int columnCount = result.getVisibleColumnCount();
            int rowCount = result.getRowCount();
            int fetch = Math.min(rowCount, fetchSize.result);
            channel.write(new QueryResponse(state,columnCount,rowCount,result,fetch));
            return ResultAndState.newState(acceptCommands);
        }else {
            return ResultAndState.waitForMoreInput(this);
        }
    }
}
