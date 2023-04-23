//package org.netbeans.modules.python.debugger.pdb;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.Socket;
//import java.nio.file.Paths;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.eclipse.lsp4j.debug.Breakpoint;
//import org.eclipse.lsp4j.debug.BreakpointEventArguments;
//import org.eclipse.lsp4j.debug.Capabilities;
//import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
//import org.eclipse.lsp4j.debug.ContinueArguments;
//import org.eclipse.lsp4j.debug.ContinuedEventArguments;
//import org.eclipse.lsp4j.debug.DisconnectArguments;
//import org.eclipse.lsp4j.debug.InitializeRequestArguments;
//import org.eclipse.lsp4j.debug.NextArguments;
//import org.eclipse.lsp4j.debug.OutputEventArguments;
//import org.eclipse.lsp4j.debug.RestartArguments;
//import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
//import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
//import org.eclipse.lsp4j.debug.Source;
//import org.eclipse.lsp4j.debug.SourceBreakpoint;
//import org.eclipse.lsp4j.debug.StepInArguments;
//import org.eclipse.lsp4j.debug.StepOutArguments;
//import org.eclipse.lsp4j.debug.SteppingGranularity;
//import org.eclipse.lsp4j.debug.launch.DSPLauncher;
//import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
//import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
//import org.eclipse.lsp4j.jsonrpc.Launcher;
//import org.netbeans.modules.python.debugger.PythonDebuggerUtils;
//import org.netbeans.modules.python.debugger.breakpoints.PythonBreakpoint;
//import org.openide.cookies.LineCookie;
//import org.openide.filesystems.FileObject;
//import org.openide.filesystems.FileUtil;
//import org.openide.loaders.DataObject;
//import org.openide.loaders.DataObjectNotFoundException;
//import org.openide.text.Annotatable;
//import org.openide.text.Line;
//import org.openide.util.Exceptions;
//
///**
// *
// * @author albilu
// */
//public class DAPClient {
//
//    String host;
//    int port;
//    Socket process;
//    IDebugProtocolServer remoteProxy;
//    IDebugProtocolClient client;
//
//    public DAPClient(String host, int port) {
//        this.host = host;
//        this.port = port;
//    }
//
//    public void connect() {
//        try {
//            client = new IDebugProtocolClient() {
//                @Override
//                public void breakpoint(BreakpointEventArguments args) {
//                    IDebugProtocolClient.super.breakpoint(args); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
//                }
//
//                @Override
//                public void continued(ContinuedEventArguments args) {
//                    IDebugProtocolClient.super.continued(args); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
//                }
//
//                @Override
//                public void output(OutputEventArguments args) {
//                    IDebugProtocolClient.super.output(args); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
//                    System.out.println(args.getOutput());
//                    Integer line = args.getLine();
//                    String path = args.getSource().getPath();
//                    PythonDebuggerUtils.markCurrent(new Annotatable[]{getLine(path, line - 1)});
//                }
//
//            };
//            process = new Socket(host, port);
//            InputStream in = process.getInputStream();
//            OutputStream out = process.getOutputStream();
//
//            // Bootstrap the actual connection.
//            Launcher<IDebugProtocolServer> launcher = DSPLauncher.createClientLauncher(client, in, out);
//            launcher.startListening();
//
//            InitializeRequestArguments arguments = new InitializeRequestArguments();
//            arguments.setClientID("pythonDebugger");
//            arguments.setAdapterID("pythonAdatId");
//
//            // Configure initialization as needed.
//            arguments.setLinesStartAt1(true);
//            arguments.setColumnsStartAt1(true);
//            arguments.setSupportsRunInTerminalRequest(true);
//
//            remoteProxy = launcher.getRemoteProxy();
//            Capabilities capabilities = remoteProxy.initialize(arguments).get(10, TimeUnit.SECONDS);
//
//            // Configure launch as needed.
//            Map<String, Object> launchArgs = new HashMap<>();
//            Map<String, Object> launchArgs1 = new HashMap<>();
////            launchArgs.put("terminal", "none");
////            launchArgs.put("target", "/path/to/target");
//            launchArgs.put("noDebug", false);
////            launchArgs.put("__sessionId", "sessionId");
//            launchArgs.put("request", "attach");
//
//            launchArgs1.put("host", host);
//            launchArgs1.put("port", port);
//            launchArgs.put("connect", launchArgs1);
//
//            CompletableFuture<Void> launch = remoteProxy.attach(launchArgs);
////            CompletableFuture<Void> launch = remoteProxy.launch(launchArgs);
////            launch.get(10, TimeUnit.SECONDS);
//
//            // Signal that the configuration is finished
//            remoteProxy.configurationDone(new ConfigurationDoneArguments());
//        } catch (IOException | InterruptedException | ExecutionException | TimeoutException ex) {
//            Logger.getLogger(DAPClient.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    public void close() {
//        try {
//            DisconnectArguments disconnectArguments = new DisconnectArguments();
//            remoteProxy.disconnect(disconnectArguments);
//            //if launch
////            remoteProxy.terminate(new TerminateArguments());
//            process.close();
//        } catch (IOException ex) {
//            Exceptions.printStackTrace(ex);
//        }
//    }
//
//    public void sendCommand(String command) throws InterruptedException, ExecutionException {
//        switch (command) {
//            case "restart":
//                RestartArguments restartArguments = new RestartArguments();
//                remoteProxy.restart(restartArguments);
//                break;
//
//            case "continue":
//                ContinueArguments continueArguments = new ContinueArguments();
//                continueArguments.setSingleThread(Boolean.TRUE);
//                remoteProxy.continue_(continueArguments);
//
//                break;
//
//            case "next":
//                NextArguments nextArguments = new NextArguments();
//                nextArguments.setGranularity(SteppingGranularity.LINE);
//                nextArguments.setThreadId(1);
//                remoteProxy.next(nextArguments);
//
//                break;
//
//            case "step":
//                StepInArguments stepInArguments = new StepInArguments();
//                stepInArguments.setThreadId(1);
//                stepInArguments.setGranularity(SteppingGranularity.STATEMENT);
//                remoteProxy.stepIn(stepInArguments);
//
//                break;
//
//            case "return":
//                StepOutArguments stepOutArguments = new StepOutArguments();
//                stepOutArguments.setGranularity(SteppingGranularity.STATEMENT);
//                stepOutArguments.setThreadId(1);
//                remoteProxy.stepOut(stepOutArguments);
//                break;
//
//            default:
//                throw new AssertionError();
//        }
//    }
//
//    private Annotatable getLine(String filePath, int lineNumber) {
//        FileObject fobj = FileUtil.toFileObject(Paths.get(filePath));
//        DataObject dobj = null;
//        try {
//            dobj = DataObject.find(fobj);
//        } catch (DataObjectNotFoundException ex) {
//            ex.printStackTrace();
//        }
//        if (dobj != null) {
//            LineCookie lc = (LineCookie) dobj.getCookie(LineCookie.class);
//            if (lc == null) {
//                /* cannot do it */ return null;
//            }
//            Line l = lc.getLineSet().getOriginal(lineNumber);
//            return l;
//        }
//        return null;
//    }
//
//    public void addBreakpoints(PythonBreakpoint pb) {
//        try {
//            SetBreakpointsArguments breakpointArgs = new SetBreakpointsArguments();
//            Source source = new Source();
//            source.setName(pb.getGroupName());
//            source.setPath(pb.getFilePath());
//            breakpointArgs.setSource(source);
//
//            SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
//            sourceBreakpoint.setLine(pb.getLineNumber());
//
//            String condition = pb.getCondition();
//            if (condition != null && !condition.trim().isEmpty()) {
//                sourceBreakpoint.setCondition(condition);
//            }
//
//            SourceBreakpoint[] breakpoints = new SourceBreakpoint[]{sourceBreakpoint};
//
//            breakpointArgs.setBreakpoints(breakpoints);
//            CompletableFuture<SetBreakpointsResponse> future = remoteProxy.setBreakpoints(breakpointArgs);
//
//            SetBreakpointsResponse setBreakpointsResponse = future.get(10, TimeUnit.SECONDS);
//
//            Breakpoint[] breakpointsResponse = setBreakpointsResponse.getBreakpoints();
//            for (Breakpoint breakpoint : breakpointsResponse) {
////                pb.setID(breakpoint.getId());
//                pb.setDapBreakpoint(breakpoint);
//            }
//        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
//            Exceptions.printStackTrace(ex);
//        }
//    }
//
//    public void removeBreakpoints(PythonBreakpoint pb) {
//        BreakpointEventArguments breakpointEventArguments = new BreakpointEventArguments();
//        breakpointEventArguments.setReason("removed");
//        breakpointEventArguments.setBreakpoint(pb.getDapBreakpoint());
//        client.breakpoint(breakpointEventArguments);
//
//    }
//
//    public void updateBreakpoints(PythonBreakpoint pb) {
//        BreakpointEventArguments breakpointEventArguments = new BreakpointEventArguments();
//        breakpointEventArguments.setReason("changed");
//        breakpointEventArguments.setBreakpoint(pb.getDapBreakpoint());
//        client.breakpoint(breakpointEventArguments);
//
//    }
//
//}
