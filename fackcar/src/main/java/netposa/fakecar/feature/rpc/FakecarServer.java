package netposa.fakecar.feature.rpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 任务启动入口
 * @author hongs.yang
 *
 */
public class FakecarServer {
	private static final Logger LOG = LoggerFactory.getLogger(FakecarServer.class);
	
	public static final int DEFAULT_LISTEN_PORT = 30050;
	public static final int DEFAULT_LISTEN_HTTP_PORT = 30077;

	public static void main(String[] args) throws Exception {
		Map<String,String> conf = new HashMap<String,String>();
		TServer server = null;
	    Options options = getOptions();
	    
	    CommandLine cmd = parseArguments(options, args);
	    if (cmd.hasOption("help")) {
	      printUsage();
	      System.exit(1);
	    }
	    
	   // Get address to bind
	    String bindAddress;
	    if (cmd.hasOption("bind")) {
	      bindAddress = cmd.getOptionValue("bind");
	    } else {
	      bindAddress = "0.0.0.0";
	    }
	    conf.put("fakecar.thrift.info.bindAddress", bindAddress);
	    
	   // Get port to bind to
	    int listenPort = 0;
	    try {
	      if (cmd.hasOption("port")) {
	        listenPort = Integer.parseInt(cmd.getOptionValue("port"));
	      } else {
			  listenPort = DEFAULT_LISTEN_PORT;
	      }
	    } catch (NumberFormatException e) {
	      throw new RuntimeException("Could not parse the value provided for the port option", e);
	    }
	    
	    boolean nonblocking = cmd.hasOption("nonblocking");
	    boolean hsha = cmd.hasOption("hsha");
	    String implType = "threadpool";
	    if (nonblocking) {
	      implType = "nonblocking";
	    } else if (hsha) {
	      implType = "hsha";
	    }
	    
	    conf.put("fakecar.server.thrift.server.type", implType);
	    conf.put("fakecar.server.thrift.port", listenPort+"");
	    
	    // Construct correct ProtocolFactory
	    boolean compact = cmd.hasOption("compact");
	    TProtocolFactory protocolFactory = getTProtocolFactory(compact);
	    final FeatureCalculatorServiceImpl fakecarHandler = 
	    		new FakecarServerHandler();
	    
	    //dump处理器
	    DumpProcessor dumpProcessor = new DumpProcessor(fakecarHandler);
	    dumpProcessor.load();
	    
	    @SuppressWarnings({ "unchecked", "rawtypes" })
		final FeatureCalculatorService.Processor p = 
	    		new FeatureCalculatorService.Processor(fakecarHandler);
	    conf.put("fakecar.server.thrift.compact", compact+"");
	    TProcessor processor = p;
	    
	    boolean framed = cmd.hasOption("framed") || nonblocking || hsha;
	    TTransportFactory transportFactory = getTTransportFactory(framed, 2*1024*1024);
	    InetSocketAddress inetSocketAddress = bindToPort(bindAddress, listenPort);
	    conf.put("fakecar.server.thrift.framed", framed+"");
	    
	    if (nonblocking) {
	        server = getTNonBlockingServer(protocolFactory, processor, transportFactory, inetSocketAddress);
	    } else if (hsha) {
	        server = getTHsHaServer(protocolFactory, processor, transportFactory, inetSocketAddress);
	    } else {
	        server = getTThreadPoolServer(protocolFactory, processor, transportFactory, inetSocketAddress);
	    }
	    
	    //启动内存dump处理
	    dumpProcessor.startDump();
	    
	    Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				LOG.info("shutdown socket ........");
			    fakecarHandler.shutdown();
			}
	    });
	    
	    LOG.info("start socket ........");
	    final TServer tserver = server;
	    tserver.serve();
	    
	    LOG.info("shutdown socket ........");
	    fakecarHandler.shutdown();
	}
	
	private static TServer getTHsHaServer(TProtocolFactory protocolFactory,
		      TProcessor processor, TTransportFactory transportFactory,
		      InetSocketAddress inetSocketAddress) throws TTransportException {
        TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(inetSocketAddress);
		LOG.info("starting FAKECAR HsHA Thrift server on " + inetSocketAddress.toString());
		THsHaServer.Args serverArgs = new THsHaServer.Args(serverTransport);
		ExecutorService executorService = createExecutor(serverArgs.getWorkerThreads());
		serverArgs.executorService(executorService);
		serverArgs.processor(processor);
		serverArgs.transportFactory(transportFactory);
		serverArgs.protocolFactory(protocolFactory);
		return new THsHaServer(serverArgs);
	}
	
	private static ExecutorService createExecutor(int workerThreads) {
		CallQueue callQueue = new CallQueue(new LinkedBlockingQueue<CallQueue.Call>());
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setDaemon(true);
		tfb.setNameFormat("thrift2-worker-%d");
		return new ThreadPoolExecutor(workerThreads, workerThreads,
		            Long.MAX_VALUE, TimeUnit.SECONDS, callQueue,
		            tfb.build());
	}
	
	private static TServer getTNonBlockingServer(TProtocolFactory protocolFactory, TProcessor processor,
		      TTransportFactory transportFactory, InetSocketAddress inetSocketAddress) throws TTransportException {
	    TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(inetSocketAddress);
		LOG.info("starting fakecar Nonblocking Thrift server on " + inetSocketAddress.toString());
		TNonblockingServer.Args serverArgs = new TNonblockingServer.Args(serverTransport);
		serverArgs.processor(processor);
		serverArgs.transportFactory(transportFactory);
		serverArgs.protocolFactory(protocolFactory);
		return new TNonblockingServer(serverArgs);
	}
	
	private static TServer getTThreadPoolServer(TProtocolFactory protocolFactory, TProcessor processor,
		      TTransportFactory transportFactory, InetSocketAddress inetSocketAddress) throws TTransportException {
        TServerTransport serverTransport = new TServerSocket(inetSocketAddress);
		LOG.info("starting fakecar ThreadPool Thrift server on " + inetSocketAddress.toString());
		TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
		serverArgs.processor(processor);
		serverArgs.transportFactory(transportFactory);
		serverArgs.protocolFactory(protocolFactory);
		return new TThreadPoolServer(serverArgs);
	}
	
	private static InetSocketAddress bindToPort(String bindValue, int listenPort)
	      throws UnknownHostException {
	    try {
	      if (bindValue == null) {
	        return new InetSocketAddress(listenPort);
	      } else {
	        return new InetSocketAddress(InetAddress.getByName(bindValue), listenPort);
	      }
	    } catch (UnknownHostException e) {
	      throw new RuntimeException("Could not bind to provided ip address", e);
	    }
	}

	
	private static TTransportFactory getTTransportFactory(boolean framed, int frameSize) {
		if (framed) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Using framed transport");
			}
		    return new TFramedTransport.Factory(frameSize);
		}else {
			return new TTransportFactory();
		}
	}
	
	private static TProtocolFactory getTProtocolFactory(boolean isCompact) {
	    if (isCompact) {
	      LOG.debug("Using compact protocol");
	      return new TCompactProtocol.Factory();
	    } else {
	      LOG.debug("Using binary protocol");
	      return new TBinaryProtocol.Factory();
	    }
	}
	
	private static CommandLine parseArguments(Options options, String[] args)
		      throws ParseException, IOException {
		/*
        CommandLine commandLine = null;
	    
	    CommandLineParser parser = new GnuParser();
	    try {
	        commandLine = parser.parse(options, args, true);
	    } catch(ParseException e) {
	        LOG.warn("options parsing failed: "+e.getMessage());

	        HelpFormatter formatter = new HelpFormatter();
	        formatter.printHelp("general options are: ", options);
	    }
	    String[] remainingArgs = (commandLine == null) 
	    		? new String[]{} 
	    		: commandLine.getArgs();
	    */
	    CommandLineParser new_parser = new PosixParser();
	    return new_parser.parse(options, args);
	}
	
	
	private static void printUsage() {
	    HelpFormatter formatter = new HelpFormatter();
	    formatter.printHelp("Thrift", null, getOptions(),
	        "To start the Fakecar server run 'bin/start.sh'\n" +
	            " send a kill signal to the thrift server pid",
	        true);
	}
	
	private static Options getOptions() {
	    Options options = new Options();
	    options.addOption("b", "bind", true,
	        "Address to bind the fakecar server to. [default: 0.0.0.0]");
	    options.addOption("p", "port", true, "Port to bind to [default: " + DEFAULT_LISTEN_PORT + "]");
	    options.addOption("f", "framed", false, "Use framed transport");
	    options.addOption("c", "compact", false, "Use the compact protocol");
	    options.addOption("h", "help", false, "Print help information");
	    options.addOption(null, "infoport", true, "Port for web UI");

	    OptionGroup servers = new OptionGroup();
	    servers.addOption(
	        new Option("nonblocking", false, "Use the TNonblockingServer. This implies the framed transport."));
	    servers.addOption(new Option("hsha", false, "Use the THsHaServer. This implies the framed transport."));
	    servers.addOption(new Option("threadpool", false, "Use the TThreadPoolServer. This is the default."));
	    options.addOptionGroup(servers);
	    return options;
	}

}
