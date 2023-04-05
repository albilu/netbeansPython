package org.netbeans.modules.python.testrunner;

/**
 *
 * @author albilu
 */
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;

public class PythonTestParserTask extends ParserResultTask<Result> {

    @Override
    public void run(Result result, SchedulerEvent event) {
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Class getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
    }

}
